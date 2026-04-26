const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const express = require("express");
const {
  Client,
  GatewayIntentBits,
  REST,
  Routes,
  SlashCommandBuilder
} = require("discord.js");

loadEnv(path.join(__dirname, ".env"));

const BOT_TOKEN = process.env.DISCORD_BOT_TOKEN || "";
const CLIENT_ID = process.env.DISCORD_CLIENT_ID || "";
const GUILD_ID = process.env.DISCORD_GUILD_ID || "";
const ADMIN_USER_IDS = new Set(
  (process.env.DISCORD_ADMIN_USER_IDS || "")
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean)
);
const PORT = Number(process.env.VIP_API_PORT || 8787);
const DATA_FILE = path.resolve(__dirname, process.env.VIP_DATA_FILE || "./vip-data.json");

const state = loadState(DATA_FILE);

const commands = [
  new SlashCommandBuilder()
    .setName("createkey")
    .setDescription("Create a VIP key with a duration.")
    .addStringOption((option) =>
      option
        .setName("duration")
        .setDescription("How long the key should last.")
        .setRequired(true)
        .addChoices(
          { name: "1 day", value: "1d" },
          { name: "1 week", value: "1w" },
          { name: "1 month", value: "1m" },
          { name: "lifetime", value: "lifetime" }
        )
    )
    .addStringOption((option) =>
      option
        .setName("note")
        .setDescription("Optional label for the key.")
        .setRequired(false)
    ),
  new SlashCommandBuilder()
    .setName("revokekey")
    .setDescription("Revoke a VIP key.")
    .addStringOption((option) =>
      option.setName("key").setDescription("The key to revoke.").setRequired(true)
    ),
  new SlashCommandBuilder()
    .setName("keyinfo")
    .setDescription("Show info for a VIP key.")
    .addStringOption((option) =>
      option.setName("key").setDescription("The key to inspect.").setRequired(true)
    ),
  new SlashCommandBuilder()
    .setName("listkeys")
    .setDescription("List recent VIP keys.")
];

async function main() {
  ensureEnv(BOT_TOKEN, "DISCORD_BOT_TOKEN");
  ensureEnv(CLIENT_ID, "DISCORD_CLIENT_ID");

  await registerCommands();
  startApiServer();
  startDiscordBot();
}

async function registerCommands() {
  const rest = new REST({ version: "10" }).setToken(BOT_TOKEN);
  const body = commands.map((command) => command.toJSON());

  if (GUILD_ID) {
    await rest.put(Routes.applicationGuildCommands(CLIENT_ID, GUILD_ID), { body });
    console.log(`Registered guild slash commands for guild ${GUILD_ID}.`);
    return;
  }

  await rest.put(Routes.applicationCommands(CLIENT_ID), { body });
  console.log("Registered global slash commands.");
}

function startDiscordBot() {
  const client = new Client({ intents: [GatewayIntentBits.Guilds] });

  client.once("ready", () => {
    console.log(`Discord bot logged in as ${client.user.tag}.`);
  });

  client.on("interactionCreate", async (interaction) => {
    if (!interaction.isChatInputCommand()) {
      return;
    }

    if (!isAdmin(interaction.user.id)) {
      await interaction.reply({ content: "You are not allowed to use this bot.", ephemeral: true });
      return;
    }

    try {
      if (interaction.commandName === "createkey") {
        const duration = interaction.options.getString("duration", true);
        const note = interaction.options.getString("note", false) || "";
        const entry = createKey(duration, interaction.user.id, note);
        await interaction.reply({
          content:
            `Created key:\n` +
            `\`${entry.key}\`\n` +
            `Duration: ${entry.display}\n` +
            `Expires: ${entry.lifetime ? "lifetime" : new Date(entry.expiresAt).toLocaleString()}`,
          ephemeral: true
        });
        return;
      }

      if (interaction.commandName === "revokekey") {
        const key = interaction.options.getString("key", true).trim();
        const record = state.keys[key];
        if (!record) {
          await interaction.reply({ content: "Key not found.", ephemeral: true });
          return;
        }
        record.revoked = true;
        persistState();
        await interaction.reply({ content: `Revoked \`${key}\`.`, ephemeral: true });
        return;
      }

      if (interaction.commandName === "keyinfo") {
        const key = interaction.options.getString("key", true).trim();
        const record = state.keys[key];
        if (!record) {
          await interaction.reply({ content: "Key not found.", ephemeral: true });
          return;
        }
        await interaction.reply({ content: renderKeyInfo(record), ephemeral: true });
        return;
      }

      if (interaction.commandName === "listkeys") {
        const recent = Object.values(state.keys)
          .sort((a, b) => b.createdAt - a.createdAt)
          .slice(0, 15);

        if (!recent.length) {
          await interaction.reply({ content: "No keys created yet.", ephemeral: true });
          return;
        }

        const content = recent.map((record) =>
          `${record.key} | ${record.revoked ? "revoked" : record.claimedBy ? "claimed" : "unused"} | ${record.display}`
        ).join("\n");

        await interaction.reply({ content, ephemeral: true });
      }
    } catch (error) {
      console.error(error);
      const content = error && error.message ? error.message : "Unexpected bot error.";
      if (interaction.replied || interaction.deferred) {
        await interaction.followUp({ content, ephemeral: true });
      } else {
        await interaction.reply({ content, ephemeral: true });
      }
    }
  });

  client.login(BOT_TOKEN);
}

function startApiServer() {
  const app = express();
  app.use(express.json());

  app.get("/health", (req, res) => {
    res.json({ ok: true });
  });

  app.post("/vip/claim", (req, res) => {
    const key = `${req.body?.key || ""}`.trim();
    const clientId = `${req.body?.clientId || ""}`.trim();
    const username = `${req.body?.username || ""}`.trim();

    if (!key || !clientId) {
      res.status(400).json({ ok: false, message: "Missing key or clientId." });
      return;
    }

    const record = state.keys[key];
    if (!record) {
      res.status(404).json({ ok: false, message: "Key not found." });
      return;
    }
    if (record.revoked) {
      res.status(403).json({ ok: false, message: "Key revoked." });
      return;
    }
    if (isExpired(record)) {
      res.status(403).json({ ok: false, message: "Key expired." });
      return;
    }
    if (record.claimedBy && record.claimedBy !== clientId) {
      res.status(403).json({ ok: false, message: "Key already used." });
      return;
    }

    if (!record.sessionToken) {
      record.sessionToken = randomToken(32);
    }
    record.claimedBy = clientId;
    record.claimedAt = Date.now();
    record.claimedUsername = username;
    persistState();

    res.json({
      ok: true,
      message: "VIP unlocked.",
      sessionToken: record.sessionToken,
      expiresAt: record.lifetime ? -1 : record.expiresAt,
      display: record.display,
      lifetime: !!record.lifetime
    });
  });

  app.post("/vip/check", (req, res) => {
    const sessionToken = `${req.body?.sessionToken || ""}`.trim();
    const clientId = `${req.body?.clientId || ""}`.trim();
    if (!sessionToken || !clientId) {
      res.status(400).json({ ok: false, valid: false, message: "Missing sessionToken or clientId." });
      return;
    }

    const record = Object.values(state.keys).find((entry) => entry.sessionToken === sessionToken);
    if (!record) {
      res.status(404).json({ ok: false, valid: false, message: "Session not found." });
      return;
    }
    if (record.revoked) {
      res.status(403).json({ ok: false, valid: false, message: "Key revoked." });
      return;
    }
    if (isExpired(record)) {
      res.status(403).json({ ok: false, valid: false, message: "Key expired." });
      return;
    }
    if (record.claimedBy !== clientId) {
      res.status(403).json({ ok: false, valid: false, message: "Session belongs to another client." });
      return;
    }

    res.json({
      ok: true,
      valid: true,
      message: "VIP active.",
      expiresAt: record.lifetime ? -1 : record.expiresAt,
      display: record.display,
      lifetime: !!record.lifetime
    });
  });

  app.listen(PORT, () => {
    console.log(`VIP API listening on http://127.0.0.1:${PORT}`);
  });
}

function createKey(durationValue, createdBy, note) {
  const { lifetime, expiresAt, display } = parseDuration(durationValue);
  const key = makeHumanKey();
  const entry = {
    key,
    createdAt: Date.now(),
    createdBy,
    note,
    expiresAt,
    display,
    lifetime,
    claimedBy: "",
    claimedAt: 0,
    claimedUsername: "",
    sessionToken: "",
    revoked: false
  };
  state.keys[key] = entry;
  persistState();
  return entry;
}

function parseDuration(value) {
  const now = Date.now();
  switch (value) {
    case "1d":
      return { lifetime: false, expiresAt: now + 24 * 60 * 60 * 1000, display: "1 day" };
    case "1w":
      return { lifetime: false, expiresAt: now + 7 * 24 * 60 * 60 * 1000, display: "1 week" };
    case "1m":
      return { lifetime: false, expiresAt: now + 30 * 24 * 60 * 60 * 1000, display: "1 month" };
    case "lifetime":
      return { lifetime: true, expiresAt: -1, display: "lifetime" };
    default:
      throw new Error("Unsupported duration.");
  }
}

function isExpired(record) {
  return !record.lifetime && record.expiresAt > 0 && Date.now() > record.expiresAt;
}

function renderKeyInfo(record) {
  return [
    `Key: ${record.key}`,
    `Duration: ${record.display}`,
    `Expires: ${record.lifetime ? "lifetime" : new Date(record.expiresAt).toLocaleString()}`,
    `Claimed: ${record.claimedBy ? "yes" : "no"}`,
    `Claimed By: ${record.claimedBy || "-"}`,
    `Claimed User: ${record.claimedUsername || "-"}`,
    `Revoked: ${record.revoked ? "yes" : "no"}`,
    `Note: ${record.note || "-"}`
  ].join("\n");
}

function persistState() {
  fs.writeFileSync(DATA_FILE, JSON.stringify(state, null, 2));
}

function loadState(filePath) {
  if (!fs.existsSync(filePath)) {
    return { keys: {} };
  }

  try {
    const parsed = JSON.parse(fs.readFileSync(filePath, "utf8"));
    if (!parsed || typeof parsed !== "object") {
      return { keys: {} };
    }
    if (!parsed.keys || typeof parsed.keys !== "object") {
      parsed.keys = {};
    }
    return parsed;
  } catch (error) {
    console.error("Failed to read VIP data file, starting fresh.", error);
    return { keys: {} };
  }
}

function makeHumanKey() {
  return [
    randomWord(),
    randomWord(),
    randomWord()
  ].join("-").toUpperCase();
}

function randomWord() {
  return crypto.randomBytes(3).toString("hex");
}

function randomToken(length) {
  return crypto.randomBytes(length).toString("hex");
}

function isAdmin(userId) {
  return ADMIN_USER_IDS.has(userId);
}

function ensureEnv(value, name) {
  if (!value) {
    throw new Error(`Missing required environment variable ${name}.`);
  }
}

function loadEnv(filePath) {
  if (!fs.existsSync(filePath)) {
    return;
  }

  const lines = fs.readFileSync(filePath, "utf8").split(/\r?\n/);
  for (const line of lines) {
    if (!line || line.startsWith("#")) {
      continue;
    }
    const index = line.indexOf("=");
    if (index === -1) {
      continue;
    }
    const key = line.slice(0, index).trim();
    const value = line.slice(index + 1).trim();
    if (key && !process.env[key]) {
      process.env[key] = value;
    }
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
