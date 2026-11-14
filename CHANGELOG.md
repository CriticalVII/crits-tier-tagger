# Changelog

## 2.3.7 � 2026-11-14

- Keep the last successful AfriTiers snapshot visible while reconnects, API failures and cooldowns delay refreshes.
- Retain cached MCTiers, PvPTiers and SubTiers profiles during failed refreshes, retry automatically after cooldown, and replace cached tiers only after a successful response. Successful empty/not-found responses still remove outdated tiers.
- Cached data stays in memory for the current client session, with the existing bounded provider caches and request limits.

## 2.3.6 — 2026-11-14

- Matched AfriTiers profile tier ordering to the website: current-tier points descending, with ties following the website’s mode order. Other providers retain their existing ordering.
- Verified that AfriTiers overall position follows current active-tier points in the live API. Confirmed an upstream discrepancy: the live website shows tiers missing from its public API. Overall ranks remain based on API data, without invented tiers or fixed rank overrides.

## 2.3.5 — 2026-11-14

- Reduced the gallery to six themes, mapping retired preset IDs to a nearby retained theme. Grouped HT/LT colour choices under Tier 1–Tier 5.
- Added theme-coloured tier hover cards with at least 94% opacity, and 256px GUI-only exports from original artwork for clearer provider/mode selectors. In-world sprite dimensions remain unchanged.
- Removed the Retired colour selector and legacy override; retired tiers inherit the underlying HT/LT colour without resetting other preferences.
- Added the live Sword/tier/username preview to colour pickers, using the selected colour and tier being edited.
- Removed dark outlines from GUI tier labels.
- Kept panel and navigation colours stable on hover while preserving selected states and keyboard focus outlines.
- Capped corner radius at 8 in both the slider and saved config validation, preserving marked snapping steps.

## 2.3.4 — 2026-11-14

- Fixed the signed-distance border mask at radius 1 so its centre no longer alters panel opacity. Added marked radius steps and one-step keyboard snapping.
- Added a shared swatch-and-hex colour picker for every colour control, and separate HT1–HT5, LT1–LT5 and Retired overrides with legacy colour fallbacks.
- Draw tier labels directly on the shared panel with contrast outlines instead of individual backing pills.
- Removed automatic hover popups from controls. Tier badges retain cursor-positioned mode, tier and points details; external providers without per-tier points display not supplied.

## 2.3.3 — 2026-11-14

- Replaced per-frame corner scanlines with reusable coverage textures and nine-slice drawing, capped at 18 quads per bordered panel. Masks are bounded by supported corner radii.
- Coalesced rapid GUI config writes after 350 ms of inactivity, while applying changes immediately. Pending settings flush on normal shutdown and failed writes retry. Commands retain synchronous persistence.
- Avoided redundant selected-tab/theme rebuilds and ranking reloads for appearance-only edits.
- Corrected shared widget hover tooltips to follow the cursor in this mod’s screens, preserving keyboard-focused placement and vanilla menus.
- Renamed Smooth font to Pixel Style and inverted the toggle: off uses smooth text and corners; on uses pixel rendering. Theme selection no longer overrides this independent preference.

## 2.3.2 — 2026-11-14

- Improved tier-label contrast on light and translucent themes with dark backing and text shadows.
- Positioned profile tier hover details at the cursor. Squared the sidebar’s inside corners while retaining the outer frame shape.
- Set default and preset opacity to 60%, and made content panels translucent over the blurred backdrop.
- Extended Smooth font to render rounded edges with framebuffer-resolution subpixel coverage; pixel mode remains available.

## 2.3.1 — 2026-11-14

- Removed theme-card subtitles and shortened hover text. Each card now uses its own preset background, border, text colours and corner style.
- Added an Interface tab for easy access to accent, font, blur, animation and corner settings. Compact screens wrap the settings tabs into two rows.
- Added an explicit Transparent GUI switch. Opacity is retained when switching to opaque panels, and selecting Glass enables transparency.
- Exposed the smooth/pixel font choice for every preset, including Minecraft.
- Removed colour controls from Tier Tags. Tier colours and the all-tags override remain in Settings → Tier colours; resetting tag controls leaves the palette intact.

## 2.3.0 — 2026-11-14

- Redesigned every mod screen around a shared, inset themed workspace with rounded surfaces, transparent backgrounds and consistent focus/hover states.
- Added a Settings tab with a searchable gallery of 14 dark, pastel, light and Minecraft-style palettes. Renamed the existing tag configuration tab to Tier Tags.
- Added accent swatches and hex input, opacity and corner sliders, blur and animation controls, and a smooth/pixel font option. Minecraft screen-effect preferences remain respected.
- Added independent T1–T5 colour controls, live tier previews, four colour sets and resets. Profiles, previews and in-game tags use the saved tier palette; the existing all-tags override stays available.
- Added custom surface/text palettes and bounded clipboard theme import/export containing appearance preferences only. All valid changes save immediately; unfinished hex colours leave the saved value intact.
- Kept existing matching, provider selection, waitlist subscriptions, icon sizes, world-transfer refreshes and network protections. Older configuration files migrate without resetting these settings.

## 2.2.4 — 2026-11-13

- Renamed the GitHub repository to `crits-tier-tagger` and updated the mod's website, source and issue links.

- Shortened source paths to `src/client/crits/` and `src/common/crits/`, with a single top-level `resources/` directory. Removed redundant source-root nesting and empty directories.
- Updated Java namespaces, Fabric entrypoints, mixin registration, build configuration and documentation to match. Existing mod/config IDs, assets and behaviour remain unchanged.

## 2.2.3 — 2026-11-13

- Reorganized Java under `dev.crits.tiertagger`, with dedicated rendering, identity, HTTP/source and repository packages. Renamed client/command entrypoints for the current brand.
- Consolidated artwork into canonical runtime assets, removed 23 unused source/duplicate images and moved source attribution into `docs/`. Simplified mode sprite paths and moved client mixin metadata into client resources.
- Removed obsolete navigation branches/status helpers and redundant imports. Development runtime files now stay under the ignored local directory.
- Preserved the mod ID, config/resource namespace, keybind IDs, artwork, licenses, network safeguards and existing matching behaviour. Added codebase documentation and expanded credential/dump ignores.

## 2.2.2 — 2026-11-13

- Replaced the previous brand icon with the approved charcoal HT1 tag, gold trim and mint-green loop. Mod Menu, Fabric metadata, the packaged mod and README share the same updated icon.
- Mode icons, tierlist logos and GUI behaviour are unchanged.

## 2.2.1 — 2026-11-13

- Smoothed Outfit menu text with 4× glyph oversampling and linear filtering scoped to this mod's UI. Labels, fields and tooltips use the same font; Minecraft's HUD and in-game tags remain unchanged.
- Moved the in-game tierlist selector into Settings with all four providers and their original logos. Profile source selection stays independent.
- Simplified navigation to Settings, Profiles and Waitlists at the side, including compact layouts. Removed sidebar category headings and the My account footer.
- Redesigned Profiles around a centred skin portrait and player identity, a position/points banner, and one tier panel inspired by the supplied reference. Original mode icons sit in circular wells above coloured tier pills, with per-mode hover details and wrapping rows inside the same panel. Columns fill its width and shorter final rows are centred.
- Centred profile search names, placeholders and native caret/selection baselines. Replaced the sidebar monogram with the full Crit's Tier Tagger wordmark.
- Removed the top-ten tag feature, its setting, rank-only prefixes, preview rank and associated Tab indentation. Overall rank remains available in Profiles. Older configs safely ignore the removed option.

## 2.2.0 — 2026-11-13

### Pandora-inspired GUI

- Replaced the framed workspace with an edge-to-edge black layout, slimmer grouped navigation, account footer and compact page headers.
- Added clean Outfit text throughout this mod's GUI, including text fields and tooltips. Minecraft's HUD font, in-game tags and live tag preview retain their original rendering.
- Restyled controls with subtle rounded corners, green/cyan actions, switches and blue-outlined selections.
- Converted mode/provider selection to image galleries and profile rankings to consistent mode tiles with coloured tier labels.
- Enlarged the profile portrait and simplified its surroundings to match the launcher reference. Smaller GUI sizes retain responsive top navigation and scrolling.
- Added restrained tile lift and portrait fades, respecting Minecraft's Screen Effects setting.

Includes 2.1.1's verified UUID-only lookup for MCTiers/PvPTiers/SubTiers and `RHT`/`RLT` retired labels. AfriTiers matching, original mode/provider artwork, automatic saving, main-menu access, waitlists and request safeguards remain intact.

## 2.1.1 — 2026-11-13

### UUID matching

- MCTiers, PvPTiers and SubTiers now use UUID profile endpoints exclusively. Usernames first resolve to verified account UUIDs through the official Minecraft account service, supporting offline-mode servers without trusting server-generated UUIDs.
- External profile caches and ranking keys are UUID-based. Responses must contain the requested UUID; old or missing display names cannot change identity matching. Tierlist username lookup fallbacks and external username aliases are disabled.
- Added a shared bounded identity cache, request spacing, priority searches, negative caching and Retry-After handling. Profile searches and status commands finish their UUID resolution asynchronously.
- AfriTiers username matching, verified paid rename recovery, aliases and bulk API behavior remain unchanged.

### Tier labels

- Retired tiers now display as `RHT1`, `RLT2`, etc. in Tab, nametags, profiles, hover labels and command output, replacing the `(retired)` suffix.
- Tier colours, tier ordering, retired-tier filtering and points rules are preserved.

## 2.1.0 — 2026-11-13

### GUI redesign

- Rebuilt the GUI around a charcoal workbench theme with thin neutral borders, muted blue accents, high-contrast light primary buttons and a subtle dotted backdrop.
- Added a fixed navigation sidebar on wide layouts and compact top navigation at smaller GUI scales.
- Added bundled Outfit Medium headings while retaining Minecraft text for controls, player names and tier labels. Font styling is scoped to this mod's headings.
- Redesigned Settings, Profiles, Tierlists, Waitlists, mode selection, profile-source selection and connection details with shared controls and surfaces.
- Added a framed live preview, grouped settings, a direct tierlist shortcut, framed player portraits, clearer profile rows and a selected-source status panel.
- Added consistent footer actions and responsive grids with full-width final rows when a mode list has an odd count.
- Added subtle hover transitions and heading reveals that respect Minecraft's Screen Effects setting.

Original tierlist mode icons, provider logos, tag sizes/colours, automatic saving, main-menu access, username matching, provider isolation, waitlist notifications and network safeguards are preserved.

## 2.0.2 — 2026-11-13

- Replaced all redrawn mode icons with the original MCTiers and SubTiers assets shown in the supplied references. Original SVG/PNG files and source links are retained; sprites only receive proportional resizing and transparent padding for Minecraft.
- Changed the main Tierlists tab to the reference's simple two-by-two button grid, with centred official logos and names and a highlighted selection. Removed the detailed provider cards.
- Preserved the profile source selector, approved Tab/nametag icon dimensions, main-menu updates, title-screen shortcut and API safeguards.

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
