# Codebase layout

The Java namespace is `dev.crits.tiertagger`. Packages follow their role; each class lives in the directory matching its package.

```text
src/
  main/
    java/dev/crits/tiertagger/
      config/             Validation and atomic config persistence
      identity/           Verified account and AfriTiers rename resolution
      network/
        http/             HTTP clients, limits, timeouts and response handling
        source/           Transport contracts
      provider/           Tierlist definitions and profile normalization
      ranking/            Tier, mode, profile and ranking models
      repository/         Cached state, refresh queues and retry scheduling
      waitlist/           Queue models and notification decisions
    resources/
      fabric.mod.json
      assets/afritiers_tagger/
        icon.png          Canonical mod logo, also used by the README
        font/             Outfit, font definitions and its license
        lang/             Translations
        mode_bounds.json  In-game icon crop windows
        textures/gui/sprites/
          mode/           Canonical mode artwork
          provider/       Canonical tierlist logos
  client/
    java/dev/crits/tiertagger/client/
      CritsTierTaggerClient.java   Client bootstrap and feature coordination
      ModMenuIntegration.java    Optional Mod Menu entrypoint
      TagFormatter.java          Player tier text composition
      ServerNameLabels.java      Server name-label compatibility
      command/                   Client commands
      gui/                       Screens, controls and menu typography
      mixin/                     Minecraft rendering/lifecycle hooks
      render/                    Glyph sources, sprites and icon metrics
    resources/
      afritiers_tagger.client.mixins.json
docs/                     Codebase and asset provenance
gradle/wrapper/           Reproducible Gradle bootstrap
releases/                 Latest production JAR and SHA-256 checksum
```

`main` contains code that can run independently of Minecraft's client classes. `client` contains native GUI and game integration. Fabric Loom combines both into the production JAR.

The internal mod ID, resource namespace, config filename, translation keys and keybind IDs remain `afritiers_tagger` for compatibility. Renaming Java classes does not reset existing settings or change username/UUID matching.

## Assets

Only runtime artwork is kept. The logo at `src/main/resources/assets/afritiers_tagger/icon.png` is the single source for mod branding. Mode and provider source URLs and ownership are recorded in [MODES.md](MODES.md) and [PROVIDERS.md](PROVIDERS.md). The Outfit license stays alongside the font.

## Local files and credentials

Build caches and local verification tools are ignored. `runClient` keeps its development runtime in `.local-dev/run-client/`. Account/config exports, environment files, private keys and signing stores must stay outside commits. The public APIs used by this mod require no embedded credentials. Keep licenses and asset provenance when removing files.
