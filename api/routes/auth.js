const express = require("express");
const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const db = require("../config/firebase");

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
module.exports = router;
