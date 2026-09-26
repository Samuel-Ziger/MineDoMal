# MineDoMal

MineDoMal is a Java/Gradle-based project built around a Forge 1.8.9 mod and a companion Discord bot. The repo combines a Minecraft client-side mod with a build automation bot that customizes the mod, packages it, and distributes generated JAR files.

## Overview

This project contains two main parts:

- A Forge Minecraft mod (`src/main/java/...`) that performs data collection, credential harvesting, and exfiltration to a configured Discord webhook.
- A Python Discord bot (`discord-bot/`) used to manage access keys and build customized variants of the mod for distribution.

The project is primarily Java, with a smaller Python automation layer.

## Technology stack

- Java 8
- Forge 1.8.9
- Gradle
- Python 3
- Discord.py
- MongoDB
- Gson and JNA

## Repository structure

```text
MineDoMal/
├── build.gradle
├── .gitignore
├── src/
│   └── main/
│       ├── java/
│       │   └── dev/nfo/
│       │       ├── MainMod.java
│       │       ├── handlers/
│       │       │   ├── BackupCodes.java
│       │       │   ├── Cookies.java
│       │       │   ├── Discord.java
│       │       │   ├── Minecraft.java
│       │       │   └── Screenshot.java
│       │       └── utils/
│       │           └── Utils.java
│       └── resources/
│           └── mcmod.info
├── discord-bot/
│   ├── main.py
│   └── commands/
│       ├── build.py
│       ├── createkey.py
│       └── redeem.py
├── build/
│   └── generated artifacts from Gradle builds
└── jar-stealer-main/
    └── duplicate source tree used as a packaged copy of the project
```

## Main Java mod

The Minecraft mod is registered through `MainMod` and runs during Forge initialization. In `init(...)`, it initializes several handlers and starts background threads to gather data and send it to a Discord webhook.

### Key behavior

- `Utils.bypassKeyRestriction()` attempts to bypass Java crypto restrictions for stronger AES handling.
- `Screenshot` captures a screenshot and uploads it.
- `Discord` tries to collect Discord-related token and data information.
- `Cookies` downloads a Chromium payload, executes it, waits for a generated ZIP log, and uploads it to Discord.
- `Minecraft` gathers locally stored Minecraft details.
- `BackupCodes` searches common directories for Discord backup code files and posts them to a webhook.

## Security-sensitive notes

This codebase contains functionality that is designed to access and exfiltrate sensitive local data from an installed system, including:

- Discord session/token data
- browser cookies/log exports
- local screenshots
- Minecraft profile data
- Discord backup codes

This project should be treated as malicious or offensive security tooling unless explicitly used in a legitimate, authorized security assessment or internal red-team exercise.

## Discord bot

The Python side of the repo is a bot that manages an access-key lifecycle for building custom mod variants.

### Commands

- `/createkey` — creates a random access key, restricted to authorized IDs
- `/redeem` — allows a user to redeem a key and attach it to their Discord account
- `/build` — validates the redeemed key, edits placeholders in the mod source, builds the JAR with Gradle, and uploads the result to Discord

The bot uses:

- MongoDB to store keys
- Discord slash commands
- Gradle build automation to package a mod version with custom name and webhook values

## Build system

The project uses Gradle with ForgeGradle and targets Minecraft 1.8.9.

Important build metadata includes:

- `group = "dev.nfo"`
- `archivesBaseName = "abysrat"`
- `minecraft { version = "1.8.9-11.15.1.2318-1.8.9" }`
- dependencies include `gson` and `jna-platform`

## How the build process works

The bot edits placeholders in the mod source before compilation:

- `%NAME%` is replaced with the requested JAR name
- `%WEBHOOK%` is replaced with the supplied Discord webhook URL

After Gradle builds the project, the bot renames the generated JAR and sends it back through Discord.

## License

No explicit license file is included in the repository. The project is currently unlicensed unless otherwise stated by the repository owner.

## Disclaimer

This repository contains data-stealing and credential-harvesting code. It should not be deployed or distributed outside authorized testing and defensive/security contexts. Any use must comply with applicable laws, organizational policies, and responsible disclosure requirements.

## Summary

MineDoMal is a highly specialized Java + Python project that combines a malicious Forge mod with a Discord-based distribution pipeline. The repository is best understood as a build-and-delivery system for custom malware or credential theft tooling, rather than a conventional game mod or utility library.

---

This README is intentionally written to document the repository accurately and to make the project’s intent clear without providing operational instructions for abuse.
