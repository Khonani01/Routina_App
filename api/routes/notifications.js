const express = require("express");
const db = require("../config/firebase");
const verifyToken = require("../middleware/auth");
const { FieldValue } = require("firebase-admin/firestore");

const router = express.Router();

// Saves the phone's push token on the logged-in user
router.post("/register-token", verifyToken, async (req, res) => {
  try {
    const { token } = req.body;

    if (!token) {
      return res.status(400).json({ error: "token is required" });
    }

    await db.collection("users").doc(req.userId).update({
      fcmTokens: FieldValue.arrayUnion(token),
    });

    res.status(200).json({ message: "Device token saved" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Sends a push notification to every device saved for the logged-in user
router.post("/send", verifyToken, async (req, res) => {
  try {
    const { title, body, data } = req.body;

    if (!title || !body) {
      return res.status(400).json({ error: "title and body are required" });
    }

    const userDoc = await db.collection("users").doc(req.userId).get();
    const tokens = (userDoc.exists && userDoc.data().fcmTokens) || [];

    if (tokens.length === 0) {
      return res.status(404).json({ error: "No device tokens saved for this user" });
    }

    const { getMessaging } = require("firebase-admin/messaging");
    const result = await getMessaging().sendEachForMulticast({
      tokens,
      notification: { title, body },
      data: data || {},
    });

    res.status(200).json({ sent: result.successCount, failed: result.failureCount });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;