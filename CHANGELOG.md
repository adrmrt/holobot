# Changelog

## [3.10.1](https://github.com/adrmrt/holobot/compare/v3.10.0...v3.10.1) (2026-09-11)


### Bug Fixes

* **countdown:** small patches ([085907e](https://github.com/adrmrt/holobot/commit/085907edfb73fe5c33d5b68472c74ce526235b10))


### Documentation

* add more deployment details ([17bf5c4](https://github.com/adrmrt/holobot/commit/17bf5c48ad3f86c5b595ebd2abef6f2c3a4dc86c))

## [3.10.0](https://github.com/adrmrt/holobot/compare/v3.9.2...v3.10.0) (2026-09-11)


### Features

* add timestamp, datecheck, and countdown commands ([#274](https://github.com/adrmrt/holobot/issues/274)) ([341a139](https://github.com/adrmrt/holobot/commit/341a13912aa5bb6deb1026543d1934f471fddb91))


### Documentation

* backfill changelog entries for pre-automation releases ([2c1c3ab](https://github.com/adrmrt/holobot/commit/2c1c3abdd991ac4b388c1d319df88fc33e7710e0))

## [3.9.2](https://github.com/adrmrt/holobot/compare/v3.9.1...v3.9.2) (2026-09-08)


### Bug Fixes

* **ci:** stop release-please from bypassing manifest and config files ([16faae5](https://github.com/adrmrt/holobot/commit/16faae5d0604bdf5d2f938dcbcfe67aa92a8f0d8))

## [3.9.1](https://github.com/adrmrt/holobot/compare/v3.9.0...v3.9.1) (2026-09-07)


### Bug Fixes

* **mce2:** stop hiding anonymous players from player list ([#264](https://github.com/adrmrt/holobot/issues/264)) ([397e312](https://github.com/adrmrt/holobot/commit/397e3125036f7e406c21560ebdd44947bdc5a03d))

---

> [!NOTE]
> Entries below this point predate release-please automation (bootstrapped at `v3.9.1`) and are reconstructed by hand from commit history. They're approximate and grouped loosely rather than following strict Conventional Commits categories.

## [3.9.0](https://github.com/adrmrt/holobot/compare/v3.8.2...v3.9.0) (2026-09-06)

### Features

* rework MAL anime/manga search to use Jikan API ([#261](https://github.com/adrmrt/holobot/pull/261))

### CI

* add build/test and commit-lint checks

## [3.8.2](https://github.com/adrmrt/holobot/compare/v3.8.1...v3.8.2) (2026-09-02)

### Features

* **games:** add deactivated mce2tps command for Spark TPS over RCON

## [3.8.1](https://github.com/adrmrt/holobot/compare/v3.8.0...v3.8.1) (2026-08-28)

### Bug Fixes

* **games:** filter players without id from mce2 player count

## [3.8.0](https://github.com/adrmrt/holobot/compare/v3.7.2...v3.8.0) (2026-08-28)

### Features

* **games:** add mce2 command for MC Eternal 2 server status ([#252](https://github.com/adrmrt/holobot/pull/252))
* add cat command (later deactivated due to API key issue)

### Refactor

* migrate command system to ExecutableCommand ([#248](https://github.com/adrmrt/holobot/pull/248))

## [3.7.2](https://github.com/adrmrt/holobot/compare/v3.7.1...v3.7.2) (2026-06-05)

### Bug Fixes

* closes [#231](https://github.com/adrmrt/holobot/issues/231)

### Refactor

* small improvements to dictionary command

## [3.7.1](https://github.com/adrmrt/holobot/compare/v3.7.0...v3.7.1) (2026-05-24)

### Bug Fixes

* fix Urban Dictionary pagination crash on special characters

## [3.7.0](https://github.com/adrmrt/holobot/compare/v3.6.0...v3.7.0) (2026-05-24)

### Features

* auto-leave voice channel on inactivity
* support additional music platforms (SoundCloud, Bandcamp)
* upgrade to Java 25

### Bug Fixes

* only join voice channel after validating video link
* display artwork URL correctly for YouTube videos

## [3.6.0](https://github.com/adrmrt/holobot/compare/v3.5.6...v3.6.0) (2026-05-20)

### Features

* add AniList as fallback provider for anime/manga search ([#212](https://github.com/adrmrt/holobot/pull/212))

## [3.5.6](https://github.com/adrmrt/holobot/compare/v3.5.5...v3.5.6) (2026-05-19)

### Bug Fixes

* Urban Dictionary links no longer broken

## [3.5.5](https://github.com/adrmrt/holobot/compare/v3.5.4...v3.5.5) (2026-05-19)

### Bug Fixes

* Urban Dictionary title and link now work again

### Chores

* normalize line endings to LF
* better user agent and add version information retrieval

## [3.5.4](https://github.com/adrmrt/holobot/compare/v3.5.3...v3.5.4) (2026-02-28)

### Bug Fixes

* show question count in final akinator victory embed

## [3.5.3](https://github.com/adrmrt/holobot/compare/v3.5.2...v3.5.3) (2026-02-27)

### Chores

* dependency and CI maintenance

## [3.5.2](https://github.com/adrmrt/holobot/compare/v3.5.1...v3.5.2) (2026-01-15)

### Bug Fixes

* added character/logo to similar-results and final embeds

### Refactor

* migrate some image commands to new command system

## [3.5.1](https://github.com/adrmrt/holobot/compare/v3.5.0...v3.5.1) (2026-01-06)

### Chores

* added Merriam-Webster logo

## [3.5.0](https://github.com/adrmrt/holobot/compare/v3.4.0...v3.5.0) (2026-01-06)

### Features

* introduce new context-based command system (foundation for later migrations)
* add akinator game, dictionary command, and purge command
* add blacklist service
* add xkcd full-text search and sync service
* add Flyway migrations
* start Merriam-Webster client integration

### Bug Fixes

* prevent missing guild configs causing NPE
* ignore delete error in DMs

## [3.4.0](https://github.com/adrmrt/holobot/compare/v3.3.9...v3.4.0) (2025-12-24)

### Features

* add Acheron content filter command

### Refactor

* centralize HTTP usage via HoloHttp
* introduce DAOs for emotes, guild config, and xkcd

### Bug Fixes

* unique emote names in same batch
* fixed SQL files in subfolders

## [3.3.9](https://github.com/adrmrt/holobot/compare/v3.3.8...v3.3.9) (2025-12-22)

### Bug Fixes

* fixed double jumps (maybe)

### Chores

* improvements to first startup

## [3.3.8](https://github.com/adrmrt/holobot/compare/v3.3.7...v3.3.8) (2025-12-22)

### Bug Fixes

* fixed font resource error
* hotfix for versioning

## [3.3.7](https://github.com/adrmrt/holobot/compare/v3.3.6...v3.3.7) (2025-12-22)

### Bug Fixes

* fixed 8ball reading resources
* fixed Docker build issue
* fixed [#197](https://github.com/adrmrt/holobot/issues/197)

### Chores

* added all settings to `.env.example`

## [3.3.6](https://github.com/adrmrt/holobot/compare/v3.3.5...v3.3.6) (2025-10-06)

### Chores

* changes to resource files

## [3.3.5](https://github.com/adrmrt/holobot/compare/v3.3.2...v3.3.5) (2025-10-06)

### Bug Fixes

* fixed bot initialization
* fixed packaging of resource files

### Chores

* added `docker-compose.yml`

## [3.3.2](https://github.com/adrmrt/holobot/commits/v3.3.2) (2025-10-06)

### Features

* better logging system

### Chores

* upgrade to JDK 21
* update JDA to 6.0.0
* build and push Docker image with Jib

---

Versions prior to `3.3.2` predate tagged releases (versioning was tracked only via ad hoc `pom.xml` bumps); see the [full commit history](https://github.com/adrmrt/holobot/commits/main) for that era.
