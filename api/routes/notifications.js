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

module.exports = router;