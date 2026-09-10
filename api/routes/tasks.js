const express = require("express");
const db = require("../config/firebase");
const verifyToken = require("../middleware/auth");

const router = express.Router();

router.get("/", verifyToken, async (req, res) => {
  try {
    const snapshot = await db.collection("tasks").where("userId", "==", req.userId).get();
    const tasks = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
    res.status(200).json({ tasks });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.post("/", verifyToken, async (req, res) => {
  try {
    const { title, dueDate, priority, recurring } = req.body;

    if (!title) {
      return res.status(400).json({ error: "Title is required" });
    }

    const newTask = {
      userId: req.userId,
      title,
      dueDate: dueDate || null,
      priority: priority || "medium",
      recurring: recurring || "none",
      completed: false,
      createdAt: new Date().toISOString(),
    };

    const docRef = await db.collection("tasks").add(newTask);
    res.status(201).json({ id: docRef.id, ...newTask });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.put("/:id", verifyToken, async (req, res) => {
  try {
    const taskRef = db.collection("tasks").doc(req.params.id);
    const taskDoc = await taskRef.get();

    if (!taskDoc.exists || taskDoc.data().userId !== req.userId) {
      return res.status(404).json({ error: "Task not found" });
    }

    await taskRef.update(req.body);
    const updated = await taskRef.get();
    res.status(200).json({ id: updated.id, ...updated.data() });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.delete("/:id", verifyToken, async (req, res) => {
  try {
    const taskRef = db.collection("tasks").doc(req.params.id);
    const taskDoc = await taskRef.get();

    if (!taskDoc.exists || taskDoc.data().userId !== req.userId) {
      return res.status(404).json({ error: "Task not found" });
    }

    await taskRef.delete();
    res.status(200).json({ message: "Task deleted" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
