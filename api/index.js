require("dotenv").config();
const authRoutes = require("./routes/auth");
const habitsRoutes = require("./routes/habits");
const settingsRoutes = require("./routes/settings");
const tasksRoutes = require("./routes/tasks");
const notesRoutes = require("./routes/notes");
const express = require("express");
const cors = require("cors");
const db = require("./config/firebase");

const app = express();
app.use(cors());
app.use(express.json());
app.use("/auth", authRoutes);
app.use("/settings", settingsRoutes);
app.use("/habits", habitsRoutes);
app.use("/notes", notesRoutes);
app.use("/tasks", tasksRoutes);

app.get("/", (req, res) => {
  res.json({ message: "Routina API is running" });
});

app.get("/health", async (req, res) => {
  try {
    await db.collection("_health").limit(1).get();
    res.json({ status: "ok", firestore: "connected" });
  } catch (err) {
    res.status(500).json({ status: "error", message: err.message });
  }
});

const PORT = process.env.PORT || 5000;
app.listen(PORT, () => console.log(`Routina API listening on port ${PORT}`));
