const express = require("express");
const db = require("../config/firebase");
const verifyToken = require("../middleware/auth");

const router = express.Router();

router.get("/", verifyToken, async (req, res) => {
  try {
    const userDoc = await db.collection("users").doc(req.userId).get();

    if (!userDoc.exists) {
      return res.status(404).json({ error: "User not found" });
    }

    const userData = userDoc.data();
    const settings = userData.settings || {
      darkMode: false,
      notificationsEnabled: true,
      language: "en",
    };

    res.status(200).json({ settings });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.put("/", verifyToken, async (req, res) => {
  try {
    const { darkMode, notificationsEnabled, language } = req.body;

    const updatedSettings = {};
    if (darkMode !== undefined) updatedSettings["settings.darkMode"] = darkMode;
    if (notificationsEnabled !== undefined) updatedSettings["settings.notificationsEnabled"] = notificationsEnabled;
    if (language !== undefined) updatedSettings["settings.language"] = language;

    await db.collection("users").doc(req.userId).update(updatedSettings);

    const userDoc = await db.collection("users").doc(req.userId).get();
    res.status(200).json({ settings: userDoc.data().settings });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
