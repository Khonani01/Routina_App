const express = require("express");
const db = require("../config/firebase");
const verifyToken = require("../middleware/auth");

const router = express.Router();

router.get("/", verifyToken, async (req, res) => {
  try {
    const snapshot = await db.collection("habits").where("userId", "==", req.userId).get();
    const habits = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
    res.status(200).json({ habits });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.post("/", verifyToken, async (req, res) => {
  try {
    const { title, frequency, goal } = req.body;

    if (!title) {
      return res.status(400).json({ error: "Title is required" });
    }

    const newHabit = {
      userId: req.userId,
      title,
      frequency: frequency || "daily",
      goal: goal || 1,
      streak: 0,
      createdAt: new Date().toISOString(),
    };

    const docRef = await db.collection("habits").add(newHabit);
    res.status(201).json({ id: docRef.id, ...newHabit });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.put("/:id", verifyToken, async (req, res) => {
  try {
    const habitRef = db.collection("habits").doc(req.params.id);
    const habitDoc = await habitRef.get();

    if (!habitDoc.exists || habitDoc.data().userId !== req.userId) {
      return res.status(404).json({ error: "Habit not found" });
    }

    await habitRef.update(req.body);
    const updated = await habitRef.get();
    res.status(200).json({ id: updated.id, ...updated.data() });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.delete("/:id", verifyToken, async (req, res) => {
  try {
    const habitRef = db.collection("habits").doc(req.params.id);
    const habitDoc = await habitRef.get();

    if (!habitDoc.exists || habitDoc.data().userId !== req.userId) {
      return res.status(404).json({ error: "Habit not found" });
    }

    await habitRef.delete();
    res.status(200).json({ message: "Habit deleted" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
