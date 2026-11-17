const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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

function pastTs() {
  return new Date(Date.now() - 2000);
}

test("Unauthenticated user cannot read or list consolidated reports", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("consolidated_reports").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("agent_insights").get()
  );
});

test("Authenticated owner can create, read, and list their consolidated reports", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("consolidated_reports")
    .doc("report_1");

  const now = pastTs();
  await assertSucceeds(
    docRef.set({
      userId: ALICE_UID,
      tickerFilter: "ITUB4",
      consensusConclusion: "Comprar",
      averageConfidence: 84,
      coveredAgentsCount: 3,
      analystSummary: "DY e P/VP atrativos.",
      gestorSummary: "HHI sob controle.",
      auditorSummary: "Fontes conferidas.",
      combinedRisks: "Risco macro.",
      totalEquityBrl: 50000,
      hhiScore: 1450,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("consolidated_reports").get()
  );
});

test("Cross-user access to consolidated reports and agent insights is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastTs();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("agent_insights")
      .doc("ins_1")
      .set({
        userId: ALICE_UID,
        agentRole: "ANALISTA",
        ticker: "HGLG11",
        conclusion: "Manter",
        confidence: 78,
        thesis: "Galpões logísticos resilientes.",
        usedPersonalData: false,
        createdAt: now,
        updatedAt: now,
      })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("agent_insights").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("agent_insights")
      .doc("ins_2")
      .set({
        userId: ALICE_UID,
        agentRole: "AUDITOR",
        ticker: "HGLG11",
        conclusion: "Manter",
        confidence: 80,
        thesis: "Tentativa de escrita cruzada.",
        usedPersonalData: false,
        createdAt: now,
        updatedAt: now,
      })
  );
});

test("Shadow update with ghost field and invalid enum are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastTs();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("consolidated_reports")
      .doc("ghost_1")
      .set({
        userId: ALICE_UID,
        tickerFilter: "GERAL",
        consensusConclusion: "Comprar",
        averageConfidence: 90,
        coveredAgentsCount: 3,
        totalEquityBrl: 10000,
        hhiScore: 1200,
        isAdminOverride: true,
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("agent_insights")
      .doc("bad_role")
      .set({
        userId: ALICE_UID,
        agentRole: "ADMIN",
        ticker: "ITUB4",
        conclusion: "Comprar",
        confidence: 80,
        thesis: "Papel inválido.",
        usedPersonalData: false,
        createdAt: now,
        updatedAt: now,
      })
  );
});
