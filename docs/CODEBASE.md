# Codebase layout

The Java namespace is `crits`. This single short namespace keeps classes unique without a long directory chain. Each class lives in the directory matching its package, relative to the source roots configured in Gradle.

```text
src/
  common/crits/
    config/             Validation, theme presets and atomic config persistence
    identity/           Verified account and AfriTiers rename resolution
    network/
      http/             HTTP clients, limits, timeouts and response handling
      source/           Transport contracts
    provider/           Tierlist definitions and profile normalization
    ranking/            Tier, mode, profile and ranking models
    repository/         Cached state, refresh queues and retry scheduling
    waitlist/           Queue models and notification decisions
  client/crits/
    CritsTierTaggerClient.java   Client bootstrap and feature coordination
    ModMenuIntegration.java    Optional Mod Menu entrypoint
    TagFormatter.java          Player tier text composition
    ServerNameLabels.java      Server name-label compatibility
    command/                   Client commands
    gui/                       Screens, controls and menu typography
    mixin/                     Minecraft rendering/lifecycle hooks
    render/                    Glyph sources, sprites and icon metrics
resources/
  fabric.mod.json
  afritiers_tagger.client.mixins.json
  assets/afritiers_tagger/
    icon.png              Canonical mod logo, also used by the README
    font/                 Outfit, font definitions and its license
    lang/                 Translations
    mode_bounds.json      In-game icon crop windows
    textures/gui/sprites/
      mode/               Canonical mode artwork
      provider/           Canonical tierlist logos
docs/                     Codebase and asset provenance
gradle/wrapper/           Reproducible Gradle bootstrap
releases/                 Latest production JAR and SHA-256 checksum
```

`src/common` contains code that can run independently of Minecraft's client classes. `src/client` contains native GUI and game integration. Fabric Loom combines both into the production JAR.

The internal mod ID, resource namespace, config filename, translation keys and keybind IDs remain `afritiers_tagger` for compatibility. Renaming Java classes does not reset existing settings or change username/UUID matching.

## GUI themes

`ThemePreset` and `AppearanceConfig` hold client-independent palettes and validated preferences. `GuiTheme` caches rendering tokens after a config load/save. `TaggerScreen` owns the single panorama/blur background pass; `TaggerFrame` and `TaggerWidgets` share those tokens. `AppearanceScreen` provides the gallery, custom palette editor and tier-colour controls. Theme thumbnails are drawn from each palette and need no additional artwork.

## Assets

Only runtime artwork is kept. The logo at `resources/assets/afritiers_tagger/icon.png` is the single source for mod branding. Mode and provider source URLs and ownership are recorded in [MODES.md](MODES.md) and [PROVIDERS.md](PROVIDERS.md). The Outfit license stays alongside the font.

## Local files and credentials

Build caches and local verification tools are ignored. `runClient` keeps its development runtime in `.local-dev/run-client/`. Account/config exports, environment files, private keys and signing stores must stay outside commits. The public APIs used by this mod require no embedded credentials. Keep licenses and asset provenance when removing files.
