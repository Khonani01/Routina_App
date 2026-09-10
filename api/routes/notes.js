const express = require("express");
const db = require("../config/firebase");
const verifyToken = require("../middleware/auth");

const router = express.Router();

router.get("/", verifyToken, async (req, res) => {
  try {
    const snapshot = await db.collection("notes").where("userId", "==", req.userId).get();
    const notes = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
    res.status(200).json({ notes });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.post("/", verifyToken, async (req, res) => {
  try {
    const { title, content, color, checklist } = req.body;

    if (!title) {
      return res.status(400).json({ error: "Title is required" });
    }

    const newNote = {
      userId: req.userId,
      title,
      content: content || "",
      color: color || "#FFFFFF",
      checklist: checklist || [],
      createdAt: new Date().toISOString(),
    };

    const docRef = await db.collection("notes").add(newNote);
    res.status(201).json({ id: docRef.id, ...newNote });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.put("/:id", verifyToken, async (req, res) => {
  try {
    const noteRef = db.collection("notes").doc(req.params.id);
    const noteDoc = await noteRef.get();

    if (!noteDoc.exists || noteDoc.data().userId !== req.userId) {
      return res.status(404).json({ error: "Note not found" });
    }

    await noteRef.update(req.body);
    const updated = await noteRef.get();
    res.status(200).json({ id: updated.id, ...updated.data() });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.delete("/:id", verifyToken, async (req, res) => {
  try {
    const noteRef = db.collection("notes").doc(req.params.id);
    const noteDoc = await noteRef.get();

    if (!noteDoc.exists || noteDoc.data().userId !== req.userId) {
      return res.status(404).json({ error: "Note not found" });
    }

    await noteRef.delete();
    res.status(200).json({ message: "Note deleted" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
