# VIP Bot Setup

This folder contains the Discord bot and the small VIP key API used by the client.

## What it does

- Creates VIP keys from Discord slash commands
- Stores keys in a local JSON file
- Lets the mod claim a key one time per client
- Tracks expiration time
- Lets the mod re-check the saved VIP session token on startup

## 1. Create the Discord bot

1. Go to https://discord.com/developers/applications
2. Click `New Application`
3. Name it something like `Kuzays Secret VIP`
4. Open the `Bot` page
5. Click `Add Bot`
6. Turn on:
   - `MESSAGE CONTENT INTENT` is not needed
   - `SERVER MEMBERS INTENT` is not needed
7. Copy the bot token
8. Open `OAuth2` -> `URL Generator`
9. Select scopes:
   - `bot`
   - `applications.commands`
10. Select permissions:
   - `Send Messages`
   - `Use Slash Commands`
11. Open the generated URL in your browser and invite the bot to your server

## 2. Fill out `.env`

Copy `.env.example` to `.env`, then fill in:

- `DISCORD_BOT_TOKEN`
- `DISCORD_CLIENT_ID`
- `DISCORD_GUILD_ID`
- `DISCORD_ADMIN_USER_IDS`

`DISCORD_GUILD_ID` should be your server id while testing. Guild commands appear almost instantly.

`DISCORD_ADMIN_USER_IDS` should be your Discord user id, or multiple ids separated by commas.

## 3. Install and run

Python version:

```powershell
cd vip-bot
py -m pip install -r requirements.txt
py bot.py
```

If `py` is not available, use your Python path directly.

Node version:

```powershell
cd vip-bot
npm install
npm start
```

When it is running, the client can talk to:

`http://127.0.0.1:8787`

That already matches the client default `vipRegistryUrl`.

## 4. Commands

- `/createkey duration:1d`
- `/createkey duration:1w`
- `/createkey duration:1m`
- `/createkey duration:lifetime`
- `/revokekey key:...`
- `/keyinfo key:...`
- `/listkeys`

## 5. How the mod uses it

When someone enters a VIP key:

1. The mod sends the key to `POST /vip/claim`
2. The API marks the key as claimed by that client id
3. The API returns:
   - `sessionToken`
   - `expiresAt`
   - `display`
4. The mod saves that in its config
5. On later launches, the mod calls `POST /vip/check`
6. If the token is still valid, VIP stays unlocked

## 6. One-time keys

All generated keys are one-client keys.

That means:

- first client to claim it gets access
- another client using the same key will be rejected
- the original client can keep using its saved session token until expiry or revocation

## 7. Hosting somewhere else

If you want the VIP system to work for other people outside your PC:

1. Host this bot/API on a VPS or another always-on machine
2. Change `vipRegistryUrl` in the client's config file to your public URL

Example:

```json
"vipRegistryUrl": "https://your-domain.com"
```

## 8. Important note

The built-in admin password in the client still works locally:

`LIP-SINGING-COOKIE`

That password is separate from the Discord-created keys.
