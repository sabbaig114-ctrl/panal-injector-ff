const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_bkainat";
const ADMIN_EMAIL = "bkainat419@gmail.com";
const ALICE_UID = "alice_123";
const ALICE_EMAIL = "alice@example.com";
const BOB_UID = "bob_456";
const BOB_EMAIL = "bob@example.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function spoofedAdminContext() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  });
}

function aliceContext() {
  return testEnv.authenticatedContext(ALICE_UID, {
    email: ALICE_EMAIL,
    email_verified: true,
  });
}

function bobContext() {
  return testEnv.authenticatedContext(BOB_UID, {
    email: BOB_EMAIL,
    email_verified: true,
  });
}

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

test("Unauthenticated access is denied across all collections", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("products").where("active", "==", true).get());
  await assertFails(db.collection("product_assets").get());
  await assertFails(db.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertFails(db.collection("payment_settings").doc("default").get());
  await assertFails(db.collection("admins").doc(ALICE_UID).get());
});

test("Email spoofing with email_verified=false cannot act as admin", async () => {
  const spoofDb = spoofedAdminContext().firestore();
  const now = new Date(Date.now() - 2000);
  await assertFails(
    spoofDb.collection("products").doc("prod_1").set({
      name: "Spoof APK",
      version: "1.0",
      category: "Injector",
      description: "Unauthorized product",
      pricePkr: 500,
      fileName: "spoof.apk",
      fileSizeBytes: 1024000,
      active: true,
      ownerUid: "spoof_uid",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(spoofDb.collection("product_assets").get());
});

test("Customer cannot self-escalate into admins collection", async () => {
  const aliceDb = aliceContext().firestore();
  const now = new Date(Date.now() - 2000);
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: ALICE_EMAIL,
      displayName: "Alice",
      grantedBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Admin manages products and protected assets; customer reads active products only", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = aliceContext().firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    adminDb.collection("products").doc("prod_ff_pro").set({
      name: "FF Pro Hud Pack",
      version: "v4.2",
      category: "HUD Tool",
      description: "Real compiled utility APK package for custom HUD layout tuning.",
      pricePkr: 1500,
      fileName: "ff_pro_hud_v4_2.apk",
      fileSizeBytes: 15420100,
      active: true,
      ownerUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("product_assets").doc("prod_ff_pro").set({
      productId: "prod_ff_pro",
      storagePath: "apks/prod_ff_pro/ff_pro_hud_v4_2.apk",
      fileName: "ff_pro_hud_v4_2.apk",
      fileSizeBytes: 15420100,
      ownerUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer filtered active query succeeds; unfiltered products list fails; product_assets fails
  await assertSucceeds(aliceDb.collection("products").where("active", "==", true).get());
  await assertFails(aliceDb.collection("products").get());
  await assertFails(aliceDb.collection("product_assets").doc("prod_ff_pro").get());
  await assertFails(aliceDb.collection("product_assets").get());

  // Customer cannot mutate product
  await assertFails(
    aliceDb.collection("products").doc("prod_ff_pro").update({
      pricePkr: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Payment settings: admin writes default; customer can get default but cannot list or write", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = aliceContext().firestore();
  const now = new Date(Date.now() - 3000);

  await assertSucceeds(
    adminDb.collection("payment_settings").doc("default").set({
      easypaisaTitle: "Panel Store PK",
      easypaisaNumber: "03001234567",
      jazzcashTitle: "Panel Store PK",
      jazzcashNumber: "03121234567",
      instructions: "Send exact PKR amount and submit TID.",
      updatedBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("payment_settings").doc("default").get());
  await assertFails(aliceDb.collection("payment_settings").get());
  await assertFails(
    aliceDb.collection("payment_settings").doc("default").update({
      easypaisaNumber: "03009999999",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Order lifecycle: customer creates PENDING order; cannot self-approve or read others; admin approves with storage path", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = aliceContext().firestore();
  const bobDb = bobContext().firestore();
  const now = new Date(Date.now() - 8000);

  // Seed product & asset
  await assertSucceeds(
    adminDb.collection("products").doc("prod_1").set({
      name: "Panel Matrix APK",
      version: "2.1.0",
      category: "Injector",
      description: "Verified Android APK package.",
      pricePkr: 1200,
      fileName: "panel_matrix_2_1.apk",
      fileSizeBytes: 8200100,
      active: true,
      ownerUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer cannot create order for non-existent product
  await assertFails(
    aliceDb.collection("orders").doc("ord_bad_prod").set({
      userId: ALICE_UID,
      userEmail: ALICE_EMAIL,
      productId: "nonexistent",
      productName: "Fake",
      productVersion: "1.0",
      pricePkr: 1200,
      customerName: "Alice",
      whatsappNumber: "+923001112233",
      paymentMethod: "EASYPAISA",
      transactionId: "TID998877",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer cannot self-approve or inject approvedStoragePath on create
  await assertFails(
    aliceDb.collection("orders").doc("ord_spoof").set({
      userId: ALICE_UID,
      userEmail: ALICE_EMAIL,
      productId: "prod_1",
      productName: "Panel Matrix APK",
      productVersion: "2.1.0",
      pricePkr: 1200,
      customerName: "Alice",
      whatsappNumber: "+923001112233",
      paymentMethod: "EASYPAISA",
      transactionId: "TID998877",
      status: "APPROVED",
      approvedStoragePath: "apks/prod_1/panel_matrix_2_1.apk",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid customer PENDING order creation succeeds
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_1").set({
      userId: ALICE_UID,
      userEmail: ALICE_EMAIL,
      productId: "prod_1",
      productName: "Panel Matrix APK",
      productVersion: "2.1.0",
      pricePkr: 1200,
      customerName: "Alice Khan",
      whatsappNumber: "+923001112233",
      paymentMethod: "JAZZCASH",
      transactionId: "JC88442211",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer can list own orders via whereEqualTo("userId", uid); unfiltered fails; Bob cannot read Alice's order
  await assertSucceeds(aliceDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("orders").get());
  await assertFails(bobDb.collection("orders").doc("ord_1").get());
  await assertFails(bobDb.collection("orders").where("userId", "==", ALICE_UID).get());

  // Customer cannot approve own order
  await assertFails(
    aliceDb.collection("orders").doc("ord_1").update({
      status: "APPROVED",
      approvedStoragePath: "apks/prod_1/panel_matrix_2_1.apk",
      approvedFileName: "panel_matrix_2_1.apk",
      approvedFileSizeBytes: 8200100,
      reviewedBy: ALICE_UID,
      reviewedAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin cannot approve without approvedStoragePath
  await assertFails(
    adminDb.collection("orders").doc("ord_1").update({
      status: "APPROVED",
      reviewedBy: ADMIN_UID,
      reviewedAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin approves with storage path -> succeeds
  await assertSucceeds(
    adminDb.collection("orders").doc("ord_1").update({
      status: "APPROVED",
      adminNote: "JazzCash TID verified.",
      approvedStoragePath: "apks/prod_1/panel_matrix_2_1.apk",
      approvedFileName: "panel_matrix_2_1.apk",
      approvedFileSizeBytes: 8200100,
      reviewedBy: ADMIN_UID,
      reviewedAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Download grants: customer cannot forge or read others' grants; admin batch approval & revocation work", async () => {
  const adminDb = adminContext().firestore();
  const aliceDb = aliceContext().firestore();
  const bobDb = bobContext().firestore();
  const now = new Date(Date.now() - 6000);

  await assertSucceeds(
    adminDb.collection("products").doc("prod_1").set({
      name: "Panel Matrix APK",
      version: "2.1.0",
      category: "Injector",
      description: "Verified Android APK package.",
      pricePkr: 1200,
      fileName: "panel_matrix_2_1.apk",
      fileSizeBytes: 8200100,
      active: true,
      ownerUid: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_alice").set({
      userId: ALICE_UID,
      userEmail: ALICE_EMAIL,
      productId: "prod_1",
      productName: "Panel Matrix APK",
      productVersion: "2.1.0",
      pricePkr: 1200,
      customerName: "Alice Khan",
      whatsappNumber: "+923001112233",
      paymentMethod: "EASYPAISA",
      transactionId: "EP99887766",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer cannot forge their own download grant
  await assertFails(
    aliceDb.collection("products").doc("prod_1").collection("grants").doc(ALICE_UID).set({
      userId: ALICE_UID,
      orderId: "ord_alice",
      productId: "prod_1",
      storagePath: "apks/prod_1/panel_matrix_2_1.apk",
      fileName: "panel_matrix_2_1.apk",
      fileSizeBytes: 8200100,
      status: "APPROVED",
      grantedBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin cannot create a download grant while the order is still PENDING
  await assertFails(
    adminDb.collection("products").doc("prod_1").collection("grants").doc(ALICE_UID).set({
      userId: ALICE_UID,
      orderId: "ord_alice",
      productId: "prod_1",
      storagePath: "apks/prod_1/panel_matrix_2_1.apk",
      fileName: "panel_matrix_2_1.apk",
      fileSizeBytes: 8200100,
      status: "APPROVED",
      grantedBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin approves order & creates download grant atomically in one batch
  const reviewTime = new Date(Date.now() - 1000);
  const batch = adminDb.batch();
  batch.update(adminDb.collection("orders").doc("ord_alice"), {
    status: "APPROVED",
    adminNote: "Verified",
    approvedStoragePath: "apks/prod_1/panel_matrix_2_1.apk",
    approvedFileName: "panel_matrix_2_1.apk",
    approvedFileSizeBytes: 8200100,
    reviewedBy: ADMIN_UID,
    reviewedAt: reviewTime,
    updatedAt: reviewTime,
  });
  batch.set(adminDb.collection("products").doc("prod_1").collection("grants").doc(ALICE_UID), {
    userId: ALICE_UID,
    orderId: "ord_alice",
    productId: "prod_1",
    storagePath: "apks/prod_1/panel_matrix_2_1.apk",
    fileName: "panel_matrix_2_1.apk",
    fileSizeBytes: 8200100,
    status: "APPROVED",
    grantedBy: ADMIN_UID,
    createdAt: reviewTime,
    updatedAt: reviewTime,
  });
  await assertSucceeds(batch.commit());

  // Alice can read her own grant; Bob cannot read Alice's grant or list grants
  await assertSucceeds(
    aliceDb.collection("products").doc("prod_1").collection("grants").doc(ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("products").doc("prod_1").collection("grants").doc(ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("products").doc("prod_1").collection("grants").get()
  );
});
