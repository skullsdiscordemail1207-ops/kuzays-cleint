from __future__ import annotations

import json
import os
import secrets
import threading
import time
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Optional

import discord
from discord import app_commands
from discord.errors import Forbidden
from flask import Flask, jsonify, request


def load_env(path: Path) -> None:
    if not path.exists():
        return
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()
        if key and key not in os.environ:
            os.environ[key] = value


ROOT = Path(__file__).resolve().parent
load_env(ROOT / ".env")

BOT_TOKEN = os.environ.get("DISCORD_BOT_TOKEN", "").strip()
CLIENT_ID = os.environ.get("DISCORD_CLIENT_ID", "").strip()
GUILD_ID = os.environ.get("DISCORD_GUILD_ID", "").strip()
ADMIN_USER_IDS = {value.strip() for value in os.environ.get("DISCORD_ADMIN_USER_IDS", "").split(",") if value.strip()}
VIP_API_PORT = int(os.environ.get("VIP_API_PORT", "8787"))
VIP_API_HOST = os.environ.get("VIP_API_HOST", "0.0.0.0").strip() or "0.0.0.0"
PORT = int(os.environ.get("PORT", str(VIP_API_PORT)))
VIP_DATA_FILE = ROOT / os.environ.get("VIP_DATA_FILE", "./vip-data.json")


@dataclass
class VipKeyRecord:
    key: str
    created_at: int
    created_by: str
    note: str
    expires_at: int
    display: str
    lifetime: bool
    claimed_by: str = ""
    claimed_at: int = 0
    claimed_username: str = ""
    session_token: str = ""
    revoked: bool = False


class VipStore:
    def __init__(self, file_path: Path) -> None:
        self.file_path = file_path
        self.keys: dict[str, VipKeyRecord] = {}
        self._lock = threading.RLock()
        self.load()

    def load(self) -> None:
        if not self.file_path.exists():
            return
        try:
            payload = json.loads(self.file_path.read_text(encoding="utf-8"))
            loaded = {}
            for key, value in payload.get("keys", {}).items():
                loaded[key] = VipKeyRecord(**value)
            self.keys = loaded
        except Exception:
            self.keys = {}

    def save(self) -> None:
        with self._lock:
            payload = {"keys": {key: asdict(value) for key, value in self.keys.items()}}
            self.file_path.write_text(json.dumps(payload, indent=2), encoding="utf-8")

    def create_key(self, duration: str, created_by: str, note: str) -> VipKeyRecord:
        lifetime, expires_at, display = parse_duration(duration)
        key = make_human_key()
        record = VipKeyRecord(
            key=key,
            created_at=now_ms(),
            created_by=created_by,
            note=note,
            expires_at=expires_at,
            display=display,
            lifetime=lifetime,
        )
        with self._lock:
            self.keys[key] = record
            self.save()
        return record

    def get(self, key: str) -> Optional[VipKeyRecord]:
        return self.keys.get(key)

    def revoke(self, key: str) -> bool:
        with self._lock:
            record = self.keys.get(key)
            if not record:
                return False
            record.revoked = True
            self.save()
            return True

    def find_by_session(self, session_token: str) -> Optional[VipKeyRecord]:
        for record in self.keys.values():
            if record.session_token == session_token:
                return record
        return None


def parse_duration(value: str) -> tuple[bool, int, str]:
    now = now_ms()
    if value == "1d":
        return False, now + 24 * 60 * 60 * 1000, "1 day"
    if value == "1w":
        return False, now + 7 * 24 * 60 * 60 * 1000, "1 week"
    if value == "1m":
        return False, now + 30 * 24 * 60 * 60 * 1000, "1 month"
    if value == "lifetime":
        return True, -1, "lifetime"
    raise ValueError("Unsupported duration.")


def make_human_key() -> str:
    return "-".join(secrets.token_hex(3) for _ in range(3)).upper()


def now_ms() -> int:
    return int(time.time() * 1000)


def is_expired(record: VipKeyRecord) -> bool:
    return not record.lifetime and record.expires_at > 0 and now_ms() > record.expires_at


def render_key_info(record: VipKeyRecord) -> str:
    expires = "lifetime" if record.lifetime else time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(record.expires_at / 1000))
    return "\n".join([
        f"Key: {record.key}",
        f"Duration: {record.display}",
        f"Expires: {expires}",
        f"Claimed: {'yes' if record.claimed_by else 'no'}",
        f"Claimed By: {record.claimed_by or '-'}",
        f"Claimed User: {record.claimed_username or '-'}",
        f"Revoked: {'yes' if record.revoked else 'no'}",
        f"Note: {record.note or '-'}",
    ])


store = VipStore(VIP_DATA_FILE)
app = Flask(__name__)


@app.get("/health")
def health():
    return jsonify({"ok": True})


@app.post("/vip/claim")
def vip_claim():
    payload = request.get_json(silent=True) or {}
    key = str(payload.get("key", "")).strip()
    client_id = str(payload.get("clientId", "")).strip()
    username = str(payload.get("username", "")).strip()

    if not key or not client_id:
        return jsonify({"ok": False, "message": "Missing key or clientId."}), 400

    record = store.get(key)
    if not record:
        return jsonify({"ok": False, "message": "Key not found."}), 404
    if record.revoked:
        return jsonify({"ok": False, "message": "Key revoked."}), 403
    if is_expired(record):
        return jsonify({"ok": False, "message": "Key expired."}), 403
    if record.claimed_by and record.claimed_by != client_id:
        return jsonify({"ok": False, "message": "Key already used."}), 403

    if not record.session_token:
        record.session_token = secrets.token_hex(32)
    record.claimed_by = client_id
    record.claimed_at = now_ms()
    record.claimed_username = username
    store.save()

    return jsonify({
        "ok": True,
        "message": "VIP unlocked.",
        "sessionToken": record.session_token,
        "expiresAt": -1 if record.lifetime else record.expires_at,
        "display": record.display,
        "lifetime": record.lifetime,
    })


@app.post("/vip/check")
def vip_check():
    payload = request.get_json(silent=True) or {}
    session_token = str(payload.get("sessionToken", "")).strip()
    client_id = str(payload.get("clientId", "")).strip()

    if not session_token or not client_id:
        return jsonify({"ok": False, "valid": False, "message": "Missing sessionToken or clientId."}), 400

    record = store.find_by_session(session_token)
    if not record:
        return jsonify({"ok": False, "valid": False, "message": "Session not found."}), 404
    if record.revoked:
        return jsonify({"ok": False, "valid": False, "message": "Key revoked."}), 403
    if is_expired(record):
        return jsonify({"ok": False, "valid": False, "message": "Key expired."}), 403
    if record.claimed_by != client_id:
        return jsonify({"ok": False, "valid": False, "message": "Session belongs to another client."}), 403

    return jsonify({
        "ok": True,
        "valid": True,
        "message": "VIP active.",
        "expiresAt": -1 if record.lifetime else record.expires_at,
        "display": record.display,
        "lifetime": record.lifetime,
    })


class VipBotClient(discord.Client):
    def __init__(self) -> None:
        intents = discord.Intents.default()
        super().__init__(intents=intents)
        self.tree = app_commands.CommandTree(self)

    async def setup_hook(self) -> None:
        if GUILD_ID:
            try:
                guild = discord.Object(id=int(GUILD_ID))
                self.tree.copy_global_to(guild=guild)
                await self.tree.sync(guild=guild)
                print(f"Registered guild slash commands for guild {GUILD_ID}.")
                return
            except Forbidden:
                print(f"Warning: missing access to guild {GUILD_ID}. Falling back to global command sync.")
        await self.tree.sync()
        print("Registered global slash commands.")


client = VipBotClient()


def ensure_admin(interaction: discord.Interaction) -> bool:
    return str(interaction.user.id) in ADMIN_USER_IDS


@client.tree.command(name="createkey", description="Create a VIP key with a duration.")
@app_commands.describe(duration="How long the key should last.", note="Optional label for the key.")
@app_commands.choices(duration=[
    app_commands.Choice(name="1 day", value="1d"),
    app_commands.Choice(name="1 week", value="1w"),
    app_commands.Choice(name="1 month", value="1m"),
    app_commands.Choice(name="lifetime", value="lifetime"),
])
async def createkey(interaction: discord.Interaction, duration: app_commands.Choice[str], note: Optional[str] = None):
    if not ensure_admin(interaction):
        await interaction.response.send_message("You are not allowed to use this bot.", ephemeral=True)
        return
    record = store.create_key(duration.value, str(interaction.user.id), note or "")
    expires = "lifetime" if record.lifetime else time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(record.expires_at / 1000))
    await interaction.response.send_message(
        f"Created key:\n`{record.key}`\nDuration: {record.display}\nExpires: {expires}",
        ephemeral=True,
    )


@client.tree.command(name="revokekey", description="Revoke a VIP key.")
@app_commands.describe(key="The key to revoke.")
async def revokekey(interaction: discord.Interaction, key: str):
    if not ensure_admin(interaction):
        await interaction.response.send_message("You are not allowed to use this bot.", ephemeral=True)
        return
    if not store.revoke(key.strip()):
        await interaction.response.send_message("Key not found.", ephemeral=True)
        return
    await interaction.response.send_message(f"Revoked `{key.strip()}`.", ephemeral=True)


@client.tree.command(name="keyinfo", description="Show info for a VIP key.")
@app_commands.describe(key="The key to inspect.")
async def keyinfo(interaction: discord.Interaction, key: str):
    if not ensure_admin(interaction):
        await interaction.response.send_message("You are not allowed to use this bot.", ephemeral=True)
        return
    record = store.get(key.strip())
    if not record:
        await interaction.response.send_message("Key not found.", ephemeral=True)
        return
    await interaction.response.send_message(render_key_info(record), ephemeral=True)


@client.tree.command(name="listkeys", description="List recent VIP keys.")
async def listkeys(interaction: discord.Interaction):
    if not ensure_admin(interaction):
        await interaction.response.send_message("You are not allowed to use this bot.", ephemeral=True)
        return
    recent = sorted(store.keys.values(), key=lambda item: item.created_at, reverse=True)[:15]
    if not recent:
        await interaction.response.send_message("No keys created yet.", ephemeral=True)
        return
    content = "\n".join(
        f"{record.key} | {'revoked' if record.revoked else 'claimed' if record.claimed_by else 'unused'} | {record.display}"
        for record in recent
    )
    await interaction.response.send_message(content, ephemeral=True)


@client.event
async def on_ready():
    print(f"Discord bot logged in as {client.user}.")


def run_api_server() -> None:
    app.run(host=VIP_API_HOST, port=PORT, debug=False, use_reloader=False)


def main() -> None:
    if not BOT_TOKEN:
        raise RuntimeError("Missing DISCORD_BOT_TOKEN in .env")
    if not CLIENT_ID:
        raise RuntimeError("Missing DISCORD_CLIENT_ID in .env")
    if not ADMIN_USER_IDS:
        raise RuntimeError("Missing DISCORD_ADMIN_USER_IDS in .env")

    api_thread = threading.Thread(target=run_api_server, name="vip-api", daemon=True)
    api_thread.start()
    print(f"VIP API listening on http://{VIP_API_HOST}:{PORT}")
    client.run(BOT_TOKEN)


if __name__ == "__main__":
    main()
