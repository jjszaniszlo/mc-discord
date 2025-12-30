# Discord Integration Mod

A Minecraft Fabric 1.21.11 server-side mod that requires players to link their Discord account before joining. Players must be members of a specific Discord server to play.

## Features

- **Discord Account Linking**: Players must link their Minecraft account to Discord before joining
- **Guild Verification**: Only members of a specified Discord server can play
- **Automatic Whitelist**: Database-driven whitelist
- **Bundled Discord Bot**: Bot runs inside the mod - no separate process needed

## How It Works

1. Player attempts to join the Minecraft server
2. Mod checks MySQL database for linked Discord account
3. If not linked:
   - Generates a 6-character verification code
   - Kicks player with instructions (in their language)
   - Code expires after 15 minutes (configurable)
4. Player runs `/link CODE` in Discord
5. Bot verifies player is in the required guild
6. Bot links accounts in database
7. Player can now join the server

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.18.2+
- Fabric API 0.140.2+1.21.11
- MySQL/MariaDB database
- Discord Bot (created in Discord Developer Portal)

## Setup
### 1. Create Discord Bot

1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Create new application
3. Go to **Bot** section, create bot
4. Enable **SERVER MEMBERS INTENT** under Privileged Gateway Intents
5. Go to **OAuth2 → URL Generator**
   - Scopes: `bot`, `applications.commands`
   - Permissions: Send Messages (optional)
6. Use generated URL to invite bot to your server
7. Copy the bot token

### 2. Configure the Mod

Edit `config/discord-integration.json`:

```json
{
  "mysql": {
    "host": "localhost",
    "port": 3306,
    "database": "mc_discord",
    "username": "minecraft",
    "password": "your_password"
  },
  "discord": {
    "botToken": "YOUR_BOT_TOKEN_HERE",
    "requiredGuildId": "YOUR_GUILD_ID",
    "chatChannelId": "YOUR_CHANNEL_ID",
    "chatWebhookUrl": "YOUR_WEBHOOK_URL"
  },
  "codeExpirationMinutes": 15,
  "language": "en"
}
```

### 3. Disable Vanilla Whitelist

In `server.properties`:
```
white-list=false
```
