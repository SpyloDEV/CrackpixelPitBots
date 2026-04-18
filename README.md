# CrackpixelPitBots

CrackpixelPitBots is a Spigot 1.8.8 plugin for Pit-style combat bots.

The project is built around a few core pieces: bot profiles and settings, target selection, packet-based fake player rendering, NMS-backed entity handling, bot tick and movement control, and a bridge into an existing Pit core plugin.

## What is in the repository

- configurable bot profiles, names, skins and scaling
- bot combat and target selection logic
- PacketEvents-based fake player spawn, movement and visibility updates
- NMS-backed entity management for the bot side
- TPS-aware performance throttling
- stats and feedback handling
- reflection bridge into an existing Pit plugin environment

## Project layout

- `ai/` target selection and engagement logic
- `bot/` bot state, settings, profiles, skins, scaling and stats
- `combat/` combat-side listeners
- `listener/` player and bot interaction listeners
- `nms/` low-level entity integration
- `packet/` fake player packet handling through PacketEvents
- `performance/` TPS monitoring and bot count reduction
- `pitcore/` integration layer for an existing Pit core plugin
- `task/` bot tick and movement control

## Build

Requirements:

- Java 8
- Maven
- Spigot 1.8.8 API
- PacketEvents 2.12.0

Build command:

```bash
mvn clean package
```

The jar is written to:

```bash
target/CrackpixelPitBots.jar
```

## Important note about the current setup

The current `pom.xml` uses a local `server.jar` system dependency at:

```text
%USERPROFILE%\Downloads\server.jar
```

If that file is missing, Maven packaging will fail until the dependency setup is adjusted.

## Runtime notes

- `packetevents` is a hard dependency
- `CrackpixelCore` is configured as a soft dependency
- the plugin is meant for an existing Pit-style setup and is not documented as a general-purpose drop-in NPC plugin
