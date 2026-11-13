const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const pastTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
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

test("Unauthenticated user cannot read or list memories", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/memories`).get());
});

test("User cannot read or write another user's memories", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/memories`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/memories/mem_1`).set({
      userId: ALICE_UID,
      title: "Hijack",
      content: "Should fail",
      category: "note",
      isApproved: true,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

test("Owner can create, list, update, and delete valid memory", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const memRef = aliceDb.doc(`users/${ALICE_UID}/memories/mem_1`);

  await assertSucceeds(
    memRef.set({
      userId: ALICE_UID,
      title: "Preferred Language",
      content: "User prefers Hinglish explanations.",
      category: "preference",
      isApproved: true,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/memories`).get());

  await assertSucceeds(
    memRef.update({
      content: "User prefers concise Hinglish explanations.",
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(memRef.delete());
});

test("Shadow update with ghost field on memory is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/memories/mem_ghost`).set({
      userId: ALICE_UID,
      title: "Ghost",
      content: "Body",
      category: "note",
      isApproved: true,
      isAdmin: true,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

test("Task terminal state locking blocks updates after completed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const taskRef = aliceDb.doc(`users/${ALICE_UID}/tasks/task_1`);

  await assertSucceeds(
    taskRef.set({
      userId: ALICE_UID,
      command: "Open YouTube",
      category: "media",
      stage: "planning",
      planSummary: "Launch YouTube app intent",
      verificationResult: "Pending",
      requiresInternet: true,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    taskRef.update({
      stage: "completed",
      verificationResult: "Verified YouTube launch intent resolved",
      updatedAt: pastTimestamp(),
    })
  );

  await assertFails(
    taskRef.update({
      stage: "executing",
      updatedAt: pastTimestamp(),
    })
  );
});

test("Messages require parent conversation and cannot be mutated", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const convRef = aliceDb.doc(`users/${ALICE_UID}/conversations/conv_1`);
  const msgRef = aliceDb.doc(`users/${ALICE_UID}/conversations/conv_1/messages/msg_1`);

  // Fails if parent conversation does not exist
  await assertFails(
    msgRef.set({
      userId: ALICE_UID,
      role: "user",
      text: "Hello Gwen",
      expression: "Happy",
      hasImage: false,
      createdAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    convRef.set({
      userId: ALICE_UID,
      title: "First Chat",
      mode: "general",
      lastMessagePreview: "Hello Gwen",
      messageCount: 1,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    msgRef.set({
      userId: ALICE_UID,
      role: "user",
      text: "Hello Gwen",
      expression: "Happy",
      hasImage: false,
      createdAt: pastTimestamp(),
    })
  );

  await assertFails(msgRef.update({ text: "Tampered" }));
});
