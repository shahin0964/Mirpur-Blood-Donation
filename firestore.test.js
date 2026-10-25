const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "restaurant-pos-99d57",
    firestore: {
      host: "127.0.0.1",
      port: 8085,
      rules: fs.readFileSync("firestore.rules", "utf8")
    }
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read user profiles", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("users").doc("user_123").get());
});

test("Authenticated user can read own profile", async () => {
  const db = testEnv.authenticatedContext("user_123", { email: "user@example.com" }).firestore();
  await assertSucceeds(db.collection("users").doc("user_123").set({
    userId: "user_123",
    fullName: "John Doe",
    email: "user@example.com",
    phoneNumber: "01700000000",
    bloodGroup: "A+"
  }));
  await assertSucceeds(db.collection("users").doc("user_123").get());
});

test("Non-admin user cannot read admin logs", async () => {
  const db = testEnv.authenticatedContext("user_123", { email: "user@example.com" }).firestore();
  await assertFails(db.collection("admin_logs").get());
});

test("Owner admin email can write to app_config", async () => {
  const db = testEnv.authenticatedContext("admin_uid", { email: "md.shahin0962@gmail.com" }).firestore();
  await assertSucceeds(db.collection("app_config").doc("main_config").set({
    supportEmail: "support@mirpurblooddonation.com"
  }));
});
