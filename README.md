# CrackpixelPitBots

CrackpixelPitBots is a Spigot 1.8.8 plugin focused on configurable combat bots for a Pit-style PvP environment.

The project is structured into separate areas for:

- bot state and profile management
- targeting and combat behavior
- packet-based fake player rendering
- NMS-backed entity handling
- TPS-aware performance throttling
- bridge logic into an existing Pit core plugin

## Main areas

- `ai/`: target selection and combat decision logic
- `bot/`: bot profiles, settings, scaling, skins, stats, feedback
- `nms/`: entity spawning and low-level bot entity integration
- `packet/`: PacketEvents-based fake player spawn, movement and visibility updates
- `performance/`: TPS monitoring and dynamic bot count reduction
- `pitcore/`: reflection bridge into an existing Pit plugin environment
- `task/`: bot tick and movement control

## Notes

- This repository reflects an active development state of the project.
- The plugin is built around Spigot 1.8.8 and PacketEvents.
- Some environments may require local server dependencies during packaging, depending on the exact setup.
