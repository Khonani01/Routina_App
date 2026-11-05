const express = require("express");
const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const db = require("../config/firebase");
const { getAuth } = require("firebase-admin/auth");

const router = express.Router();

router.post("/register", async (req, res) => {
  try {
    const { name, email, password } = req.body;

    if (!name || !email || !password) {
      return res.status(400).json({ error: "Name, email, and password are required" });
    }

    const usersRef = db.collection("users");
    const existing = await usersRef.where("email", "==", email).get();

    if (!existing.empty) {
      return res.status(409).json({ error: "Email already registered" });
    }

    const hashedPassword = await bcrypt.hash(password, 10);

    const newUser = {
      name,
      email,
      password: hashedPassword,
      createdAt: new Date().toISOString(),
      settings: {
        darkMode: false,
        notificationsEnabled: true,
        language: "en",
      },
    };

    const docRef = await usersRef.add(newUser);

    const token = jwt.sign({ userId: docRef.id, email }, process.env.JWT_SECRET, { expiresIn: "7d" });

    res.status(201).json({
      user: { id: docRef.id, name, email },
      token,
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});
router.post("/login", async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: "Email and password are required" });
    }

    const usersRef = db.collection("users");
    const snapshot = await usersRef.where("email", "==", email).get();

    if (snapshot.empty) {
      return res.status(401).json({ error: "Invalid email or password" });
    }

    const userDoc = snapshot.docs[0];
    const userData = userDoc.data();

      if (!userData.password) {
      return res.status(401).json({ error: "Invalid email or password" });
    }

    const passwordMatch = await bcrypt.compare(password, userData.password);

    if (!passwordMatch) {
      return res.status(401).json({ error: "Invalid email or password" });
    }

    const token = jwt.sign({ userId: userDoc.id, email: userData.email }, process.env.JWT_SECRET, { expiresIn: "7d" });

    res.status(200).json({
      user: { id: userDoc.id, name: userData.name, email: userData.email },
      token,
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});
router.post("/sso", async (req, res) => {
  try {
    const { idToken } = req.body;

    if (!idToken) {
      return res.status(400).json({ error: "idToken is required" });
    }

    let decoded;
    try {
      decoded = await getAuth().verifyIdToken(idToken);
    } catch (err) {
      console.log("SSO token verification failed:", err.message);
      return res.status(401).json({ error: "Invalid Google token" });
    }

    const email = decoded.email;
    if (!email) {
      return res.status(400).json({ error: "Google account has no email" });
    }

    const name = decoded.name || email.split("@")[0];
    const usersRef = db.collection("users");
    const existing = await usersRef.where("email", "==", email).get();

    let userId, userName;

    if (existing.empty) {
      const newUser = {
        name,
        email,
        ssoProvider: "google",
        ssoUid: decoded.uid,
        createdAt: new Date().toISOString(),
        settings: {
          darkMode: false,
          notificationsEnabled: true,
          language: "en",
        },
      };
      const docRef = await usersRef.add(newUser);
      userId = docRef.id;
      userName = name;
    } else {
      const userDoc = existing.docs[0];
      userId = userDoc.id;
      userName = userDoc.data().name;
      if (!userDoc.data().ssoUid) {
        await userDoc.ref.update({ ssoProvider: "google", ssoUid: decoded.uid });
      }
    }

    const token = jwt.sign({ userId, email }, process.env.JWT_SECRET, { expiresIn: "7d" });

    res.status(200).json({
      user: { id: userId, name: userName, email },
      token,
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
