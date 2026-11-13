# Changelog

## 2.0.1 — 2026-11-13

### Changed

- Remade all 22 mode icons with simpler flat silhouettes, restrained colours and objects matching the supplied tierlist references, including the Netherite Pot helmet, SMP pearl, Diamond SMP apple and Debuff carrot.
- Added official AfriTiers, MCTiers, PvPTiers and SubTiers logos beside their names in the tierlist cards, profile source picker and selected profile-source button.
- Preserved the accepted nametag and Tab icon metrics, nine-pixel Tab rows, tier colours and existing mod logo.

### Fixed

- Opening Settings starts the selected tierlist's data session from the main menu. Switching tierlists loads the new source without joining a server or world.
- The configured settings shortcut (K by default) now opens the GUI on the title screen, including without Mod Menu.
- The selected provider can load your own profile outside a world. Profiles also searches other usernames and refreshes without a world, including when tags are disabled.
- Removed misleading join-a-world requirements from connection and refresh messages. Unselected provider cards now say Select to load.
- Main-menu loading retains the existing request spacing, cache limits, Retry-After handling and refresh cooldowns.

## 2.0.0 — 2026-11-13

### Added

- MCTiers, PvPTiers and SubTiers alongside AfriTiers, using public username profile APIs.
- A Tierlists tab to select one source for Tab and nametags, with separate per-provider mode preferences.
- An independent tierlist selector in Profiles, preserving your account as the default with an empty search box.
- `/tiers <player> [tierlist]`, with provider suggestions, plus `/crittiers` and `/ctiers` aliases for the original management commands.
- Provider-specific username aliases, so name corrections never leak between tierlists.
- New Bow, Debuff, Elytra, Manhunt and Trident icons and the new providers' mode aliases.
- Background profile caches with request spacing, bounded queues, short missing-player caching and automatic rate-limit backoff.

### Changed

- Renamed the mod to **Crit's Tier Tagger**, with a new lavender C, diamond sword and gold rank-glint icon.
- Remade every mode icon as consistent smooth vector artwork, with small transparent sprites that preserve Tab row boundaries and nametag sizing.
- Mode menus show the selected provider's modes. Points and overall positions for external providers come directly from their APIs; AfriTiers scoring remains specific to AfriTiers.
- Top-ten gold `#<rank>` labels use the selected tierlist's overall position. Providers do not silently fall back to another source.
- Waitlists are explicitly labelled as AfriTiers and remain independent of both provider selectors.

### Compatibility

- Keeps Minecraft Java 26.2, Java 25, Fabric Loader 0.19.5+ and Fabric API 0.161.0+26.2; Mod Menu 20.0.3 remains optional.
- Preserves the internal mod ID, existing settings file, keybind, AfriTiers aliases and waitlist subscriptions. Replace the old JAR rather than installing both versions.
- Retains automatic saving, retired tiers enabled by default, purple T4/T5 colours, pipe-free tags, and refreshes after world/server transfers.
- Server/offline UUIDs remain excluded from tier matching. AfriTiers verified paid-account rename recovery is retained.

## 1.7.1 — 2026-11-01

Changes since 1.6.0.

### Added

- `/tiers <player>` opens Profiles with that player already searched. `/tiers` opens your own account.
- A Waitlists tab with per-mode subscriptions, All/None controls, a notification pause toggle and an optional sound.
- In-game chat alerts when a watched waitlist opens, with a clickable Discord Join link when available.
- Automatic queue checks about every 30 seconds while watching in game or browsing Waitlists. Alerts are deduplicated per opening/event; retries respect service cooldowns.

### Changed

- Profiles displays your own account with an empty search box. My profile returns to your account and clears the search box.
- Nametags and the preview use spaces between the icon, tier and username, without an added pipe separator.

### Fixed

- Tiers refresh after world/player replacements, respawns and server teleports, including hub-to-Practice transfers that retain the same connection.
- Valid tiers remain visible while transfer refreshes wait for the API cooldown. Rapid transfers share one pending refresh and reuse an in-flight ranking request.
- Changing settings in the Settings tab preserves waitlist subscriptions.
- Temporary Windows config-file locks are retried without deleting the original or weakening atomic saves.

Requires Minecraft Java 26.2, Java 25, Fabric Loader 0.19.5+ and Fabric API 0.161.0+26.2. Mod Menu 20.0.3 is optional.

[Full comparison](https://github.com/CriticalVII/afritiers-tagger/compare/v1.6.0...v1.7.1)
