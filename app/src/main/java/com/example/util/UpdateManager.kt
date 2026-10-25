package com.example.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.model.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progressPercent: Int) : DownloadState()
    data class Completed(val fileUri: Uri) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

data class UpdateInfo(
    val currentVersionCode: Long,
    val currentVersionName: String,
    val latestVersionCode: Long,
    val latestVersionName: String,
    val minSupportedVersionCode: Long,
    val downloadUrl: String,
    val releaseNotesBn: String,
    val releaseNotesEn: String,
    val isMandatory: Boolean,
    val requiresUpdate: Boolean
)

class UpdateManager(private val context: Context) {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun getCurrentVersionCode(): Long {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun checkUpdate(appConfig: AppConfig): UpdateInfo {
        val currentCode = getCurrentVersionCode()
        val currentName = getCurrentVersionName()

        val latestCode = if (appConfig.latestVersionCode > 0) appConfig.latestVersionCode else parseVersionCode(appConfig.latestAppVersion)
        val minCode = if (appConfig.minSupportedVersionCode > 0) appConfig.minSupportedVersionCode else parseVersionCode(appConfig.minRequiredVersion)

        val isMandatory = appConfig.isUpdateMandatory || currentCode < minCode
        val requiresUpdate = currentCode < latestCode || currentCode < minCode

        return UpdateInfo(
            currentVersionCode = currentCode,
            currentVersionName = currentName,
            latestVersionCode = latestCode,
            latestVersionName = appConfig.latestAppVersion.ifBlank { "1.0.0" },
            minSupportedVersionCode = minCode,
            downloadUrl = appConfig.downloadUrl,
            releaseNotesBn = appConfig.releaseNotesBn,
            releaseNotesEn = appConfig.releaseNotesEn,
            isMandatory = isMandatory,
            requiresUpdate = requiresUpdate
        )
    }

    private fun parseVersionCode(versionName: String): Long {
        return try {
            val parts = versionName.split(".")
            var code = 0L
            for (part in parts) {
                val num = part.filter { it.isDigit() }.toLongOrNull() ?: 0L
                code = code * 100 + num
            }
            if (code == 0L) 1L else code
        } catch (e: Exception) {
            1L
        }
    }

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    suspend fun downloadApk(url: String, fileName: String = "app-update.apk") {
        if (url.isBlank()) {
            _downloadState.value = DownloadState.Error("Invalid download URL")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                _downloadState.value = DownloadState.Downloading(0)

                val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.cacheDir
                val apkFile = File(downloadDir, fileName)
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    _downloadState.value = DownloadState.Error("Server returned code ${connection.responseCode}")
                    return@withContext
                }

                val fileLength = connection.contentLength
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(apkFile)

                val data = ByteArray(8192)
                var total: Long = 0
                var count: Int
                var lastProgress = 0

                while (inputStream.read(data).also { count = it } != -1) {
                    total += count
                    outputStream.write(data, 0, count)
                    if (fileLength > 0) {
                        val progress = ((total * 100) / fileLength).toInt()
                        if (progress > lastProgress) {
                            lastProgress = progress
                            _downloadState.value = DownloadState.Downloading(progress)
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )

                _downloadState.value = DownloadState.Completed(apkUri)
                installApk(apkUri)

            } catch (e: Exception) {
                Log.e("UpdateManager", "Failed to download APK", e)
                _downloadState.value = DownloadState.Error(e.localizedMessage ?: "Download failed")
            }
        }
    }

    fun installApk(contentUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to launch package installer", e)
            _downloadState.value = DownloadState.Error("Package installation trigger failed: ${e.message}")
        }
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }
}
