package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.AppConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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

@JsonClass(generateAdapter = true)
data class GitHubRelease(
    @Json(name = "tag_name") val tagName: String,
    @Json(name = "name") val name: String,
    @Json(name = "body") val body: String,
    @Json(name = "assets") val assets: List<GitHubAsset>
)

@JsonClass(generateAdapter = true)
data class GitHubAsset(
    @Json(name = "name") val name: String,
    @Json(name = "browser_download_url") val browserDownloadUrl: String,
    @Json(name = "size") val size: Long
)

@JsonClass(generateAdapter = true)
data class UpdateMetadata(
    val versionName: String,
    val versionCode: Long,
    val minSupportedVersionCode: Long,
    val releaseNotesBn: String,
    val releaseNotesEn: String,
    val mandatory: Boolean,
    val apkUrl: String = ""
)

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

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    private val GITHUB_REPO_API = "https://api.github.com/repos/shahin0964/Mirpur-Blood-Donation/releases/latest"

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

    suspend fun checkGitHubUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(GITHUB_REPO_API).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            
            val body = response.body?.string() ?: return@withContext null
            val release = moshi.adapter(GitHubRelease::class.java).fromJson(body) ?: return@withContext null
            
            // Find metadata asset
            val metadataAsset = release.assets.find { it.name == "latest.json" } ?: return@withContext null
            val metadataRequest = Request.Builder().url(metadataAsset.browserDownloadUrl).build()
            val metadataResponse = client.newCall(metadataRequest).execute()
            if (!metadataResponse.isSuccessful) return@withContext null
            
            val metadataBody = metadataResponse.body?.string() ?: return@withContext null
            val metadata = moshi.adapter(UpdateMetadata::class.java).fromJson(metadataBody) ?: return@withContext null
            
            // Find APK asset
            val apkAsset = release.assets.find { it.name.endsWith(".apk") } ?: return@withContext null
            
            val currentCode = getCurrentVersionCode()
            val currentName = getCurrentVersionName()
            
            val requiresUpdate = metadata.versionCode > currentCode
            val isMandatory = metadata.mandatory || metadata.minSupportedVersionCode > currentCode

            UpdateInfo(
                currentVersionCode = currentCode,
                currentVersionName = currentName,
                latestVersionCode = metadata.versionCode,
                latestVersionName = metadata.versionName,
                minSupportedVersionCode = metadata.minSupportedVersionCode,
                downloadUrl = apkAsset.browserDownloadUrl,
                releaseNotesBn = metadata.releaseNotesBn,
                releaseNotesEn = metadata.releaseNotesEn,
                isMandatory = isMandatory,
                requiresUpdate = requiresUpdate
            )
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to check GitHub update", e)
            null
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

    fun openDownloadUrl(url: String) {
        val targetUrl = when {
            url.isBlank() -> "https://github.com/shahin0964/Mirpur-Blood-Donation/releases"
            !url.startsWith("http://") && !url.startsWith("https://") -> "https://$url"
            else -> url
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to open download URL", e)
        }
    }

    suspend fun downloadApk(url: String, fileName: String = "Mirpur-Blood-Donation-Update.apk") {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) {
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

                val request = Request.Builder().url(cleanUrl).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    _downloadState.value = DownloadState.Error("Server returned code ${response.code}")
                    return@withContext
                }

                val body = response.body ?: throw Exception("Empty response body")
                val fileLength = body.contentLength()
                val inputStream = body.byteStream()
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

                // Validate APK before allowing installation
                if (validateApk(apkFile)) {
                    val apkUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                    _downloadState.value = DownloadState.Completed(apkUri)
                    // Auto-launch installer
                    installApk(apkUri)
                } else {
                    apkFile.delete()
                    _downloadState.value = DownloadState.Error("Downloaded APK is invalid or corrupt")
                }

            } catch (e: Exception) {
                Log.e("UpdateManager", "Download failed", e)
                _downloadState.value = DownloadState.Error(e.localizedMessage ?: "Download failed")
            }
        }
    }

    private fun validateApk(file: File): Boolean {
        return try {
            val packageManager = context.packageManager
            val info = packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)
            if (info == null || info.packageName != context.packageName) return false

            // Simple signature check: match current app's first signature
            val currentInfo = packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            val currentSignature = currentInfo.signatures?.getOrNull(0)?.toCharsString()
            val newSignature = info.signatures?.getOrNull(0)?.toCharsString()

            currentSignature != null && currentSignature == newSignature
        } catch (e: Exception) {
            Log.e("UpdateManager", "APK validation failed", e)
            false
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
            _downloadState.value = DownloadState.Error("Package installation failed: ${e.message}")
        }
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }
}
