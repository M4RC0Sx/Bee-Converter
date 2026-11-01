# Bee Converter

**Stop right-clicking bees and chasing them when they fly away.**

In Productive Bees, converting a bee means holding the right item and clicking the bee again and again until the conversion chance finally hits, while the bee wanders off. Bee Converter is a powered machine that does it for you: put in a caged bee and the conversion item, and it keeps trying until the bee transforms.

## How it works

1. Place the Bee Converter and feed it Forge Energy (FE) from any generator or cable.
2. Put a **bee cage** with a bee in the top input slot.
3. Put the **conversion item** in the bottom input slot (for example a hopper, which turns a regular bee into a Collector Bee).
4. Each attempt spends one item and rolls the recipe's chance, exactly like converting by hand.
5. When the bee transforms, the cage with the new bee moves to the output slot.

The machine reads Productive Bees' own `bee_conversion` recipes, so every conversion works out of the box, including the ones added by other mods, modpacks or datapacks. There is nothing to configure.

## Upgrades

The machine is slow on purpose: one attempt takes **20 seconds** without upgrades. It has four upgrade slots that accept ProductiveLib's **Speed** and **Speedier** upgrades, the same ones used by the other Productive Bees machines. Install them through the GUI, or sneak + right-click the machine while holding one.

| Upgrades | Time per attempt |
|---|---|
| None | 20 s |
| 1 Speed | 12 s |
| 4 Speed | 2.6 s |
| 4 Speedier | 0.35 s |

Each Speed upgrade cuts the attempt time by 40%, and a Speedier counts as two. Energy is charged **per attempt** (2,000 FE by default), so a faster machine draws more FE per tick, but a conversion never gets cheaper or more expensive.

## Features

- Progress bar and live tooltips showing the progress and the current time per attempt.
- Works with hoppers and pipes: inputs go in from any side and converted bees come out, while inputs cannot be pulled back out by automation.
- Keeps its energy, progress, items and upgrades across world saves. Breaking it drops the items and upgrades.
- Listed in the Speed and Speedier upgrade tooltips alongside the other Productive Bees machines.
- Shows its contents and energy in Jade.

## Crafting

Iron ingots, a Sturdy Bee Cage, redstone and a hopper. Look it up in JEI or EMI.

## Configuration

`config/beeconverter-common.toml`:

| Option | Default | Description |
|---|---|---|
| `energyCapacity` | 10000 | Internal FE buffer |
| `energyPerAttempt` | 2000 | FE spent on each conversion attempt |
| `ticksPerAttempt` | 400 | Duration of an attempt without upgrades (20 ticks = 1 s) |
| `upgradeSpeedMultiplier` | 0.6 | Time multiplier applied per speed unit (Speed = 1 unit, Speedier = 2) |

## Requirements

- Minecraft **1.21.1** with **NeoForge**
- **Productive Bees** (it bundles ProductiveLib)

Install it on both the server and the clients.

## Building from source

The project uses Java 21 (`sdk env install` picks it up from `.sdkmanrc`) and the Gradle wrapper.

| Command | What it does |
|---|---|
| `./gradlew build` | Compiles, checks formatting and runs the unit tests; the jar lands in `build/libs/` |
| `./gradlew runGameTestServer` | Runs the in-game GameTests on a dedicated server |
| `./gradlew runClient` | Starts a development client with Productive Bees loaded |
| `./gradlew spotlessApply` | Formats the code (google-java-format) |

Productive Bees has no public API, so all access to its internals goes through `compat/PbBridge`. ProductiveLib is not published on a maven repository: the build extracts it from the Productive Bees jar for compiling only.

## Releases

Every push to `master` runs the tests and then [semantic-release](https://semantic-release.gitbook.io), which picks the version from the [Conventional Commits](https://www.conventionalcommits.org) since the last release, builds the jar and attaches it to a GitHub release with the changelog.

| Commit | Release |
|---|---|
| Any type with `!` (`feat!:`, `fix!:`…) or a `BREAKING CHANGE:` footer | Major |
| `feat:` | Minor |
| `fix:`, `perf:`, `revert:`, `refactor:`, `build(deps):` | Patch |
| `docs:`, `test:`, `ci:`, `style:`, `chore:`, other `build:` | No release |

Use a breaking change when an update can hurt existing worlds or setups: renamed blocks or config keys, machines losing their contents, or a new Minecraft version.

## License

Bee Converter is open source under the **LGPL-3.0** license. It is an independent addon and is not affiliated with Productive Bees.
