# UnlockEnchantment English Documentation

> **Version**: 0.1  
> **Target Server**: Paper / Folia 26.1.2  
> **Main Class**: `org.luminolcraft.unlockEnchantment.Main`  
> **Author/Organization**: LuminolCraft (`org.luminolcraft`)

This repository contains the enterprise-level English technical documentation for the Minecraft Paper/Folia server plugin **UnlockEnchantment**, intended for developers and operations staff taking over the project for the first time. The documentation covers thirteen sections: project background, environment setup, tech stack, directory structure, core modules, configuration reference, event API, FAQ, deployment, version control, code style, testing strategy, and maintenance notes.

---

## 1. Project Background & Goals

### 1.1 Background

The Minecraft vanilla Anvil has a hard cap when merging Enchantments or repairing items: when the required **experience level cost ≥ 40**, the Anvil UI displays "Too Expensive" and prevents the player from taking the result. This limit is controlled by `AnvilView.maximumRepairCost` (default 40), which causes:

- High-level Enchantments (e.g. Sharpness V + Sharpness V) cannot be merged to upgrade further;
- Mixed use of equipment repair and Enchantment merging easily triggers the cap;
- Players cannot obtain Enchantments beyond the vanilla maximum level (even if mods/datapacks extend the Enchantment level range).

In addition, the cost of merging enchanted books in vanilla is high; players consume large quantities of enchanted books when repeatedly upgrading Enchantments, resulting in a poor experience.

### 1.2 Goals

UnlockEnchantment aims to achieve the following goals through pure Bukkit event listening, without modifying the server core or relying on third-party mods (such as ProtocolLib, ItemsAdder):

1. **Unlock the Anvil Enchantment level cap**: Raise `maximumRepairCost` to `Int.MAX_VALUE` and recompute the merged Enchantment levels, bypassing the vanilla 40-level block.
2. **Provide Enchantment simplification crafting**: Allow players to craft an enchanted-book copy using 1 enchanted book + 1 normal book at a Crafting Table, **consuming only the normal book** while the enchanted book is preserved intact, enabling low-cost duplication of enchanted books for subsequent merging.
3. **Configurability**: Control toggles, Blacklist, Special Enchantment caps, maximum cost, notice messages, etc. via `config.yml` to suit the operational needs of different servers.
4. **Folia compatibility**: Use the region scheduler API provided by Paper/Folia (`Player.scheduler.runDelayed`) to ensure correct operation on the Folia multithreaded server.

### 1.3 Applicable Scenarios

- **Survival/RPG servers**: Wish to provide a growth path of higher-level Enchantments, increasing player motivation to pursue caps.
- **Casual/Creative servers**: Wish to remove vanilla restrictions so players can freely experiment with various Enchantment combinations.
- **Economy servers**: Lower the cost of acquiring Enchantments through the "enchanted-book duplication" feature, regulated via the economy system.
- **High-load servers requiring Folia multithreading optimization**: This plugin declares `folia-supported: true` and uses the region scheduler API.

---

## 2. Environment Setup

This project distinguishes three environments: **development**, **testing**, and **production**. Each is described below.

### 2.1 Development Environment

| Dependency | Version Requirement | Notes |
|------------|---------------------|-------|
| JDK | 25 | Enforced via `jvmToolchain(25)` in the build script; Gradle auto-downloads a missing toolchain |
| Gradle | 9.6.0 (bundled with Wrapper) | The project ships `gradlew` / `gradlew.bat`; no local Gradle install required |
| Kotlin | 2.4.20-Beta1 | Provided by the `kotlin("jvm")` plugin in `build.gradle.kts` |
| IDE | IntelliJ IDEA 2025.x or later | Kotlin plugin must support 2.4.x; Community Edition is sufficient |
| Git | any modern version | For version control |

**Setup Steps**:

1. Install JDK 25 (Eclipse Temurin / Oracle JDK recommended); confirm `java -version` outputs `25`.
2. Install IntelliJ IDEA; in `File → Project Structure → SDK` point to JDK 25.
3. Clone the repository: `git clone <repo-url>`.
4. In IDEA, `Open` the project root directory and wait for Gradle sync to complete (the first sync downloads Folia API, Kotlin runtime, and other dependencies).
5. Confirm `gradle/wrapper/gradle-wrapper.properties` points `distributionUrl` to `gradle-9.6.0-bin.zip`.

```bash
# Verify JDK version
java -version

# Verify Gradle Wrapper is available (use gradlew.bat on Windows)
./gradlew --version
```

### 2.2 Testing Environment (Local runServer)

The project bundles the `xyz.jpenilla.run-paper` plugin, which provides the `runServer` task. It downloads and launches a Minecraft 26.1.2 test server with one click and automatically injects the build artifact into the `plugins/` directory.

**Setup Steps**:

1. Build and launch:
   ```bash
   # Windows
   gradlew.bat runServer

   # Unix-like
   ./gradlew runServer
   ```
2. The first launch downloads the Minecraft 26.1.2 server (Paper/Folia) and allocates 2G of memory (`-Xms2G -Xmx2G`, see `build.gradle.kts`).
3. Accept the EULA in the console (if prompted).
4. The plugin jar is automatically placed in the test server's `plugins/` directory.
5. After the first start, `plugins/UnlockEnchantment/config.yml` is auto-generated.
6. Connect a Minecraft client to `localhost` (default port 25565) for manual testing.

**Test Server Configuration Recommendations**:

- Set `online-mode=false` in `server.properties` for offline-account testing;
- Set `gamemode=creative` for easy access to enchanted books and anvils;
- Set `level-name=world_ue_test` to isolate the test world.

### 2.3 Production Environment (Paper/Folia Server Deployment)

| Component | Version Requirement | Notes |
|-----------|---------------------|-------|
| Server | Paper 26.1.2 or Folia 26.1.2 | Must match the plugin API version |
| Java | JDK 25 | Required by the server runtime |
| OS | Linux (recommended) / Windows Server | Linux offers better memory and concurrency performance |

**Deployment Steps**:

1. Download the corresponding Paper or Folia server jar from the [PaperMC website](https://papermc.io/).
2. Start the server once to generate `eula.txt`; change `eula=false` to `eula=true`.
3. Build the plugin jar (see Section 9 "Deployment").
4. Copy `UnlockEnchantment-0.1-all.jar` to the server `plugins/` directory.
5. Restart the server; confirm the console outputs `[UnlockEnchantment] Loaded`.
6. Verify `plugins/UnlockEnchantment/config.yml` was generated and modify it as needed.
7. After editing the config, run `/reload confirm` or restart the server to apply changes.

---

## 3. Tech Stack

### 3.1 Tech Stack Overview

| Technology | Version | Role |
|------------|---------|------|
| Kotlin | 2.4.20-Beta1 | Primary development language; provides null safety, coroutines, data classes, and other modern features |
| Folia API | 26.1.2 (`dev.folia:folia-api`) | Provides the Bukkit/Paper/Folia API for interacting with the server; Paper compatible |
| Gradle | 9.6.0 (Wrapper) | Build tool; manages dependencies, tasks, and packaging |
| Shadow | 9.4.3 (`com.gradleup.shadow`) | Produces a fat jar bundling the Kotlin runtime |
| run-paper | 3.0.2 (`xyz.jpenilla.run-paper`) | Provides the `runServer` task for local test server launch |
| MiniMessage | bundled with Folia API | Formats player notice messages (color, hover, placeholders) |

### 3.2 Role of Each Technology

#### Kotlin 2.4.20-Beta1
- **Why use it**: Compared to Java, Kotlin has concise syntax (data classes, null safety, `when` expressions) and reduces boilerplate; it is 100% interoperable with Java and can call the Bukkit API directly.
- **Version notes**: 2.4.20-Beta1 is a Beta version corresponding to newer language features. Upgrading to a stable release requires evaluating compatibility with the Folia API.
- **Packaging**: The Kotlin standard library (`kotlin-stdlib-jdk8`) is bundled into the fat jar via the Shadow plugin; the server does not need Kotlin installed separately.

#### Folia API 26.1.2
- **Role**: `compileOnly("dev.folia:folia-api:26.1.2.build.+")` provides compile-time types; the implementation is supplied by the server at runtime and is not packaged into the jar.
- **Relationship between Folia and Paper**: Folia is a multithreaded fork of Paper that partitions the world into independent regions for ticking. This plugin declares `folia-supported: true` and uses `Player.scheduler.runDelayed` (the region scheduler API) instead of `BukkitScheduler` in crafting events, for Folia compatibility.
- **Compatibility**: The Folia API is compatible with Paper servers, so the plugin also runs correctly on a pure Paper server.

#### Gradle 9.6.0 + Kotlin DSL
- **Build script language**: Uses Kotlin DSL (`build.gradle.kts`), which provides type inference and IDE auto-completion compared to Groovy.
- **Performance optimization**: `gradle.properties` enables configuration cache (`org.gradle.configuration-cache`), parallel build (`org.gradle.parallel`), and build cache (`org.gradle.caching`) to speed up subsequent builds.

#### Shadow 9.4.3
- **Role**: Bundles `implementation` dependencies (e.g. `kotlin-stdlib-jdk8`) into the final jar, producing `UnlockEnchantment-0.1-all.jar`.
- **Configuration**: `tasks.build { dependsOn(shadowJar) }` ensures the `build` task produces a fat jar.

#### run-paper 3.0.2
- **Role**: Provides the `runServer` task, which auto-downloads and launches a Minecraft server for local debugging.
- **Configuration**: `minecraftVersion("26.1.2")` sets the test server version; `jvmArgs("-Xms2G", "-Xmx2G")` allocates memory.

#### MiniMessage
- **Role**: Used for the `expensive-enchant-message` config option; supports tags like `<RED>`, `<green>`, `<hover:show_item:...>`, more powerful than traditional `§` color codes.
- **Usage**: `MiniMessage.miniMessage().deserialize(str)` parses the string into an Adventure component sent to the player.

---

## 4. Directory Structure

### 4.1 File Tree

```
UnlockEnchantment/
├── build.gradle.kts                          # Gradle build script (Kotlin DSL)
├── gradle.properties                         # Gradle properties (group/version/cache flags)
├── settings.gradle.kts                       # Gradle settings (project name)
├── gradlew / gradlew.bat                     # Gradle Wrapper launch scripts
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties         # Gradle version and download URL
├── .gitignore                                # Git ignore rules
├── .gitattributes                            # Git attributes
└── src/main/
    ├── kotlin/org/luminolcraft/unlockEnchantment/
    │   ├── Main.kt                           # Plugin entry; registers event listeners
    │   ├── AnvilListener.kt                  # Anvil event listener (core: unlocks Enchantment cap)
    │   ├── CraftListener.kt                  # Craft event listener (Enchantment simplification crafting)
    │   └── config/
    │       └── ConfigManager.kt              # Config management (reads config.yml)
    └── resources/
        └── plugin.yml                        # Bukkit plugin descriptor
```

### 4.2 Responsibility of Each File

| File | Responsibility |
|------|----------------|
| `build.gradle.kts` | Declares plugins (Kotlin JVM, Shadow, run-paper); configures repositories and dependencies; defines build/runServer/processResources tasks |
| `gradle.properties` | Stores project metadata (`group=org.luminolcraft`, `version=0.1`) and Gradle performance toggles |
| `settings.gradle.kts` | Sets `rootProject.name = "UnlockEnchantment"`; affects the default jar filename |
| `gradle/wrapper/gradle-wrapper.properties` | Specifies the Gradle 9.6.0 download URL to ensure consistent team builds |
| `Main.kt` | Plugin entry class, extends `JavaPlugin`; `onEnable` initializes `ConfigManager` and registers `AnvilListener` and `CraftListener`; exposes a global `configManager` via `companion object` |
| `AnvilListener.kt` | Listens to `PrepareAnvilEvent`; lifts the `maximumRepairCost` limit; recomputes Enchantments per merge rules; applies Special Enchantment caps; sends expensive notices |
| `CraftListener.kt` | Listens to `PrepareItemCraftEvent` and `InventoryClickEvent`; implements enchanted book + normal book = enchanted-book copy (consuming only the normal book) |
| `ConfigManager.kt` | Manages creation, default-value writing, reading, and field mapping of `config.yml`; provides `reloadConfig()`; defines the `SpecialEnchantments` data class |
| `plugin.yml` | Bukkit plugin descriptor: `name`, `main`, `version` (injected at build time), `api-version: 26.1.2`, `load: POSTWORLD`, `folia-supported: true` |

### 4.3 Package Structure

```
org.luminolcraft.unlockEnchantment
├── Main                        # Main class
├── AnvilListener               # Anvil listener
├── CraftListener               # Craft listener
└── config
    ├── ConfigManager           # Config manager
    └── SpecialEnchantments     # Special Enchantment data class (same file)
```

Package naming follows the Java reverse-domain convention: `org.luminolcraft` is the organization namespace, and `unlockEnchantment` is the project name.

---

## 5. Core Modules

This project consists of three core modules: the **Anvil unlock module**, the **Enchantment simplification crafting module**, and the **config management module**.

### 5.1 Anvil Unlock Module (AnvilListener)

**File**: `src/main/kotlin/org/luminolcraft/unlockEnchantment/AnvilListener.kt`

#### 5.1.1 Module Responsibility

Listens to `PrepareAnvilEvent`. When a player opens the Anvil UI and places/removes items, it recomputes the merged Enchantment result of the two items, bypassing the vanilla 40-level cap.

#### 5.1.2 Workflow

1. **Lift the cap**: Set `event.view.maximumRepairCost` to `Int.MAX_VALUE` so the vanilla "Too Expensive" logic never triggers.
2. **Read Enchantments**:
   - Enchanted book (`ENCHANTED_BOOK`): read via `EnchantmentStorageMeta.storedEnchants` (`ItemStack.enchantments` returns empty for enchanted books).
   - Normal items: read directly via `ItemStack.enchantments`.
3. **Merge Enchantments**: Iterate each Enchantment of the second item and decide the resulting level per the rules (see 5.1.3).
4. **Fill exclusive Enchantments**: Add Enchantments present on the second item but absent on the first directly to the result.
5. **Limit maximum cost**: If `maximum-level-cost` is configured (≠ -1) and the current `repairCost` has reached the cap, truncate it to the cap.
6. **Send expensive notice**: When `repairCost > 39`, send a MiniMessage notice to the player.
7. **Apply Special Enchantment cap**: If the Enchantment is in `specialEnchantments` or the result level exceeds the custom cap, override it with the custom cap.
8. **Write result**: Use `result.addUnsafeEnchantment` to write the merged Enchantments, bypassing vanilla compatibility checks (`addEnchantment` throws when the level exceeds `maxLevel`).

#### 5.1.3 Enchantment Merge Rules

Iterate each Enchantment `s` of the second item (key = Enchantment, value = level):

| Branch | Condition | Result Level |
|--------|-----------|--------------|
| A. Blacklist | `s.key ∈ blackListEnchantments` | Keep `s.value` (the second item's original value) |
| B. Single-level Enchantment | `s.key.maxLevel == 1` (e.g. Mending, Infinity, Curse of Binding) | `s.key.startLevel` (usually 1) |
| C-1. Regular merge | First item has the Enchantment, and `firstLevel ≤ 10` or both items are the same type | `firstLevel < secondLevel` → take `secondLevel`; equal level → `secondLevel + 1`; otherwise keep `firstLevel` |
| C-2. Cross-level merge | First item is equipment (sword/pickaxe/axe/shovel/hoe/chestplate/boots/leggings/helmet) + second item is an enchanted book + `isEnchantmentSimplify` enabled + `firstLevel > 10` | `firstLevel < secondLevel` → take `secondLevel`; `truncateOnes(firstLevel) == truncateOnes(secondLevel)` → `firstLevel + 1`; otherwise keep `firstLevel` |

**`truncateOnes` function**: Truncates the ones digit, i.e. `number / 10 * 10`. For example `13 → 10`, `25 → 20`, `9 → 0`. Its purpose is to treat Enchantments of close levels as "same level", allowing cross-level merge upgrades.

**Examples**:
- Equipment Sharpness 13 + book Sharpness 12 → `truncateOnes(13)=10 == truncateOnes(12)=10` → result 14 (cross-level upgrade).
- Equipment Sharpness 13 + book Sharpness 5 → `13 > 5` and after truncation `10 ≠ 0` → keep 13.

#### 5.1.4 Special Enchantment Cap Override

Before writing the result, each merged Enchantment is checked:

- If the Enchantment exists in `specialEnchantments`, **or** the result level exceeds its `maximumLevels`, the result level is set to `specialEnchantments[key].maximumLevels`.

For example, configuring `SHARPNESS: { maximum-level: 5 }` means Sharpness will never exceed level 5, no matter how high it is merged.

### 5.2 Enchantment Simplification Crafting Module (CraftListener)

**File**: `src/main/kotlin/org/luminolcraft/unlockEnchantment/CraftListener.kt`

#### 5.2.1 Module Responsibility

When a player places **1 enchanted book (`ENCHANTED_BOOK`) + 1 normal book (`BOOK`)** in a Crafting Table, the result is a copy of the enchanted book, but **only the normal book is consumed**; the enchanted book is preserved intact. This enables low-cost duplication of enchanted books for subsequent merging.

#### 5.2.2 Two-Phase Implementation

Because vanilla crafting consumes both books, this module takes over the flow using two events:

| Phase | Event | Method | Role |
|-------|-------|--------|------|
| Prepare | `PrepareItemCraftEvent` | `onPrepareCraft` | Set the result slot to a copy of the enchanted book for UI display |
| Take result | `InventoryClickEvent` | `onCraft` | Cancel vanilla crafting; manually deduct the normal book, preserve the enchanted book, and place the copy on the cursor |

#### 5.2.3 onPrepareCraft Validation Logic

1. The crafting matrix contains exactly 2 non-empty items + `isEnchantmentSimplify` enabled.
2. The two items must be 1 `ENCHANTED_BOOK` and 1 `BOOK`.
3. Clone the enchanted book and write it to `event.inventory.result` as the result.

#### 5.2.4 onCraft Processing Logic

1. Validate: `isEnchantmentSimplify` enabled + the open inventory is a `CraftingInventory` + the clicker is a `Player` + the clicked slot is the result slot (`slot == 0`).
2. **Cancel vanilla crafting**: `event.isCancelled = true`; otherwise vanilla consumes both books.
3. Validate that the player's cursor is empty (otherwise the result cannot be placed).
4. Take out the normal book and enchanted book from the matrix.
5. **Delay 1 tick** (using `Player.scheduler.runDelayed`, the Folia region scheduler API):
   - Deduct 1 normal book and put it back in its original slot.
   - Clone the enchanted book as-is and put it back in its original slot (not consumed).
6. **Immediately** place a copy of the enchanted book on the player's cursor.

> **Why delay 1 tick**: Modifying `inventory` directly inside the event handler would be overwritten by vanilla crafting logic. You must wait until the current tick's event processing ends and vanilla crafting completes before modifying the slot contents for the change to take effect.

> **Why use `Player.scheduler.runDelayed`**: Folia has no global `BukkitScheduler`; you must use the entity/region-based scheduling API to ensure the task runs on the thread of the player's region.

### 5.3 Config Management Module (ConfigManager)

**File**: `src/main/kotlin/org/luminolcraft/unlockEnchantment/config/ConfigManager.kt`

#### 5.3.1 Module Responsibility

As the sole entry point between the plugin and `config.yml`, it is responsible for:

1. Creating `plugins/UnlockEnchantment/config.yml` on first start and writing default config and comments.
2. Mapping config keys to in-memory fields (boolean toggles, integer caps, Enchantment Blacklist, etc.).
3. Providing `reloadConfig()` to refresh config at runtime.
4. Exposing in-memory fields for event listeners to read.

#### 5.3.2 Core Methods

| Method | Responsibility |
|--------|----------------|
| `initConfig()` | Ensures the config file exists; checks each key for absence and writes the default value and comments if missing; persists back to disk |
| `loadConfig()` | Calls `initConfig()` then reads config into in-memory fields |
| `reloadConfig()` | `initConfig()` → `config.load(configFile)` → `loadConfig()`; used for runtime refresh |
| `getEnchantmentFromString(str)` | Looks up an Enchantment via Paper's `RegistryAccess`; returns `Enchantment?` |

#### 5.3.3 SpecialEnchantments Data Class

```kotlin
class SpecialEnchantments(val enchant: Enchantment, val maximumLevels: Int)
```

Binds an Enchantment with its custom maximum level, corresponding to the parsed result of each entry under the `special-enchantment-setting` config. For example `SHARPNESS: { maximum-level: 5 }` is parsed as `SpecialEnchantments(SHARPNESS, 5)`.

---

## 6. Configuration Reference

### 6.1 Config File Location

```
<server-root>/plugins/UnlockEnchantment/config.yml
```

Auto-generated on first start by `ConfigManager.initConfig()`, including all default values and English comments.

### 6.2 Full Config Field Table

| Field | Type | Default | Valid Range | Effect |
|-------|------|---------|-------------|--------|
| `enabled` | boolean | `true` | `true` / `false` | Master plugin toggle. When `false`, neither Anvil unlock nor simplification crafting intervenes in vanilla logic |
| `simplify-enchantment` | boolean | `true` | `true` / `false` | Whether to enable Enchantment simplification crafting (Crafting Table book duplication) and Anvil cross-level merging (branch C-2) |
| `maximum-level-cost` | int | `-1` | `-1` or an integer ≥ 0 | Anvil maximum level cost. `-1` means unlimited (unlocks the vanilla 40-level cap); a positive number truncates any cost exceeding it to that value |
| `blacklist` | list | `[]` | list of Enchantment namespace-key strings | Ignored Enchantments; Enchantments in the Blacklist retain their original value during Anvil merging without processing |
| `special-enchantment-setting` | section | `{}` | Enchantment key → `maximum-level: int` | Sets a custom maximum level cap for specific Enchantments; merged results exceeding the cap are overridden |
| `expensive-enchant-message` | string | `<hover:show_item:enchanted_book></hover><RED>Exceeds vanilla enchantment display, required level is <green><level></green>` | any MiniMessage string | Notice message sent to the player when cost ≥ 40; supports the `{level}` placeholder replaced with the actual cost |

> **About the `expensive-enchant-message` placeholder**: The code uses `{level}` as the placeholder (see `replace("{level}", ...)` in `AnvilListener.onPrepareAnvil`). Ensure your config string contains `{level}` rather than `<level>`, otherwise the actual cost will not be substituted. The default value written by `initConfig` has an inconsistency between `<level>` and `{level}` (see Section 12 Testing and Section 8 FAQ); manually confirm that `{level}` is used in your config.

> **About the `blacklist` and `special-enchantment-setting` defaults**: The code writes `listOf(null)` (a list containing a null element) so that a fillable placeholder appears in the YAML. In actual use, replace it with valid Enchantment keys.

### 6.3 Enchantment Namespace Keys

Enchantment names in the config use Minecraft namespace keys (lowercase, e.g. `sharpness`, `protection`, `unbreaking`). `getEnchantmentFromString` internally constructs a `minecraft:str` key via `NamespacedKey.minecraft(str)` for lookup.

Full list reference: [Folia Javadoc - Enchantment](https://jd.papermc.io/folia/26.1.2/org/bukkit/enchantments/Enchantment.html)

Common Enchantment key quick reference:

| Enchantment | Key | Vanilla Max Level |
|-------------|-----|-------------------|
| Sharpness | `sharpness` | 5 |
| Protection | `protection` | 4 |
| Unbreaking | `unbreaking` | 3 |
| Efficiency | `efficiency` | 5 |
| Mending | `mending` | 1 |
| Infinity | `infinity` | 1 |
| Looting | `looting` | 3 |
| Fortune | `fortune` | 3 |
| Fire Aspect | `fire_aspect` | 2 |
| Knockback | `knockback` | 2 |

### 6.4 Example Configuration

```yaml
# UnlockEnchantment configuration file

# Master plugin toggle
enabled: true

# Whether to enable Enchantment simplification crafting and cross-level merging
simplify-enchantment: true

# Anvil maximum level cost; -1 means unlimited
maximum-level-cost: -1

# Enchantment Blacklist (these Enchantments are not processed by the plugin)
blacklist:
  - mending
  - infinity

# Special Enchantment cap settings
special-enchantment-setting:
  sharpness:
    maximum-level: 10
  protection:
    maximum-level: 5
  unbreaking:
    maximum-level: 5

# Notice message when cost ≥ 40 (MiniMessage format)
expensive-enchant-message: "<hover:show_item:enchanted_book></hover><RED>Exceeds vanilla enchantment display, required level is <green>{level}</green>"
```

### 6.5 Config Loading and Reloading

- **Load timing**: `configManager.loadConfig()` is called during the plugin's `onEnable`.
- **Reload method**: After editing `config.yml`, call `Main.configManager.reloadConfig()` (the current version does not register a command; use `/reload confirm` or restart the server to trigger `onEnable` and reload).
- **Compatibility**: `initConfig` checks each key for existence and writes defaults for missing ones, so **old config files can be smoothly upgraded** to new versions (newly added config keys are auto-filled).

---

## 7. API & Event Reference

### 7.1 Listened Bukkit Events

| Event Class | Listener Method | Priority | Trigger Timing | Purpose |
|-------------|-----------------|----------|----------------|---------|
| `PrepareAnvilEvent` | `AnvilListener.onPrepareAnvil` | `MONITOR` | When a player places/removes items in the Anvil | Recompute the merged Enchantment result and lift the cap |
| `PrepareItemCraftEvent` | `CraftListener.onPrepareCraft` | `MONITOR` | When a player arranges materials in a Crafting Table | Set the result slot to an enchanted-book copy (for UI display) |
| `InventoryClickEvent` | `CraftListener.onCraft` | `MONITOR` | When a player clicks any slot in a Crafting Table | On clicking the result slot, cancel default crafting and manually handle item consumption |

> **About `MONITOR` priority**: `MONITOR` is the lowest priority (runs after other plugins) and is typically used for read-only listening. This project needs to rewrite results, so it explicitly overrides `event.result` / `event.isCancelled` at `MONITOR`. If conflicts with other plugins arise, consider switching to `HIGHEST`.

### 7.2 Config Access Entry

Other code reads config fields via `Main.configManager` (companion object, globally accessible):

```kotlin
import org.luminolcraft.unlockEnchantment.Main

// Read config
val enabled = Main.configManager.isPluginEnabled
val simplify = Main.configManager.isEnchantmentSimplify
val maxCost = Main.configManager.maximumLevelCost
val blacklist = Main.configManager.blackListEnchantments
val special = Main.configManager.specialEnchantments
val msg = Main.configManager.expensiveEnchantMessage

// Reload config
Main.configManager.reloadConfig()
```

### 7.3 ConfigManager Field Overview

| Field | Type | Visibility | Config Key | Description |
|-------|------|------------|------------|-------------|
| `configFile` | `File` | `val` public | — | Config file path (`plugins/UnlockEnchantment/config.yml`) |
| `config` | `FileConfiguration` | `val` public | — | Config object provided by `JavaPlugin` |
| `javaPlugin` | `JavaPlugin` | `val` public | — | Plugin main class instance; used for the scheduler etc. |
| `isPluginEnabled` | `Boolean` | `var` public | `enabled` | Master plugin toggle |
| `isEnchantmentSimplify` | `Boolean` | `var` public | `simplify-enchantment` | Simplification crafting and cross-level merging toggle |
| `maximumLevelCost` | `Int` | `var` public | `maximum-level-cost` | Maximum level cost; `-1` means unlimited |
| `blackListEnchantments` | `MutableList<Enchantment?>` | `var` public | `blacklist` | Converted Blacklist Enchantment object list |
| `specialEnchantments` | `MutableMap<Enchantment, SpecialEnchantments>` | `var` public | `special-enchantment-setting` | Special Enchantment cap map |
| `expensiveEnchantMessage` | `String` | `var` public | `expensive-enchant-message` | Expensive notice message (MiniMessage) |

### 7.4 SpecialEnchantments Data Structure

```kotlin
class SpecialEnchantments(val enchant: Enchantment, val maximumLevels: Int)
```

| Field | Type | Description |
|-------|------|-------------|
| `enchant` | `Enchantment` | The Enchantment this setting targets |
| `maximumLevels` | `Int` | The custom maximum level cap for this Enchantment |

**Access Example**:

```kotlin
val special: Map<Enchantment, SpecialEnchantments> = Main.configManager.specialEnchantments

// Query whether an Enchantment has a special cap
val sharpnessSetting = special[Enchantment.SHARPNESS]
if (sharpnessSetting != null) {
    val maxLevel = sharpnessSetting.maximumLevels
    // ...
}

// Check whether an Enchantment is in the special settings
if (special.containsKey(someEnchant)) {
    // ...
}
```

### 7.5 plugin.yml Metadata

```yaml
name: UnlockEnchantment
version: '${version}'              # Replaced with 0.1 at build time by processResources
main: org.luminolcraft.unlockEnchantment.Main
api-version: '26.1.2'
load: POSTWORLD                    # Loaded after all worlds are loaded
folia-supported: true              # Declares Folia support
```

| Field | Value | Description |
|-------|-------|-------------|
| `name` | `UnlockEnchantment` | Plugin name; must match the folder name under `plugins/` |
| `version` | `0.1` (injected at build) | From `version` in `gradle.properties` |
| `main` | `org.luminolcraft.unlockEnchantment.Main` | Fully qualified main class name |
| `api-version` | `26.1.2` | Target Paper/Folia API version |
| `load` | `POSTWORLD` | Loaded after worlds load (the alternative `STARTUP` loads at server startup) |
| `folia-supported` | `true` | Declares compatibility with the Folia multithreaded server |

---

## 8. FAQ

### Q1: I modified config.yml but the config does not take effect. What should I do?

**Cause**: The config is loaded only once during `onEnable`; modifying the file does not trigger a reload.

**Solution**:
1. Run `/reload confirm` in the server console (requires Paper's `/reload` command);
2. Or restart the server;
3. If you need an in-plugin reload command, refer to `ConfigManager.reloadConfig()` and implement command registration yourself (the current version does not register a command).

**Verification**: After reloading, use `/minecraft:data` or print `Main.configManager.maximumLevelCost` to the log to confirm the value has updated.

### Q2: Enchanted book + normal book does not respond in the Crafting Table?

**Possible causes and troubleshooting**:

| Cause | Troubleshooting | Solution |
|-------|-----------------|----------|
| `simplify-enchantment` is `false` | Check `config.yml` | Set to `true` and reload |
| `enabled` is `false` | Check `config.yml` | Set to `true` and reload |
| The matrix does not contain exactly 2 items | Confirm only 1 enchanted book + 1 normal book is placed | Remove extra items |
| Wrong item type | Confirm they are `ENCHANTED_BOOK` and `BOOK` (not `WRITABLE_BOOK`) | Use the correct books |
| Player cursor is not empty | The cursor must be empty before clicking the result slot | Clear the cursor and retry |
| Plugin not loaded | Check the console for `[UnlockEnchantment] Loaded` | Redeploy the jar |

### Q3: Errors on a Folia server?

**Typical errors**: `UnsupportedOperationException` / `Not implemented` / scheduling-related exceptions.

**Cause**: Folia removes the global `BukkitScheduler`; you must use the region/entity scheduling API.

**Troubleshooting**:
1. Confirm `folia-supported: true` in `plugin.yml`;
2. Confirm `api-version: 26.1.2` matches the server version;
3. Check the code for any direct use of `Bukkit.getScheduler()` — this project's `CraftListener` already uses `Player.scheduler.runDelayed`; if you modify the code, keep this pattern;
4. On Folia, `InventoryClickEvent` must be handled on the main (region) thread; this project guarantees that via `Player.scheduler`.

### Q4: Cannot find the jar file after building?

**Cause**: Looking in the wrong output directory or the `shadowJar` task did not run.

**Solution**:
1. Run a full build: `gradlew.bat build` (Windows) or `./gradlew build`;
2. The artifact is at `build/libs/UnlockEnchantment-0.1-all.jar` (note the `-all` suffix for the fat jar);
3. If you only see `UnlockEnchantment-0.1.jar` (without `-all`), `shadowJar` did not run; check that `build.gradle.kts` contains `build { dependsOn(shadowJar) }`;
4. If the `build/` directory does not exist, run `gradlew.bat clean build` first.

### Q5: Kotlin version conflict or compile error?

**Typical errors**: `Unresolved reference`, `Kotlin version mismatch`, `jvmTarget`-related warnings.

**Cause**: The JDK toolchain version does not match the Kotlin compile target, or the IDE's Kotlin plugin is too old.

**Solution**:
1. Confirm JDK 25 is installed: `java -version` should output `25`;
2. Confirm `jvmToolchain(25)` in `build.gradle.kts`;
3. In IntelliJ IDEA, `File → Invalidate Caches / Restart` and re-sync Gradle;
4. Upgrade IDEA's Kotlin plugin to a version that supports 2.4.x;
5. If you insist on a stable Kotlin, change `2.4.20-Beta1` in `build.gradle.kts` to a recent stable release (e.g. `2.0.x`), but test compatibility with the Folia API.

### Q6: No notice message received above level 40?

**Possible causes**:

| Cause | Troubleshooting | Solution |
|-------|-----------------|----------|
| Wrong placeholder in `expensive-enchant-message` | Check whether the config uses `{level}` | Change `<level>` to `{level}` (the code uses `replace("{level}", ...)` ) |
| Message intercepted by another plugin | Temporarily disable chat-related plugins to test | Adjust plugin load order or priority |
| `repairCost` is not > 39 | Observe the cost level in the Anvil UI | Confirm the actual cost ≥ 40 |
| MiniMessage syntax error | Check whether tags are closed (e.g. `<red>...</red>` or `<green>...</green>`) | Fix the syntax |

> **Note**: The default message written by `initConfig()` uses `<level>`, but the fallback default in `loadConfig()` uses `{level}`, and `AnvilListener` replaces `{level}`. It is recommended to manually change `<level>` in the config to `{level}` to ensure the placeholder works.

### Q7: Special Enchantment cap does not take effect?

**Troubleshooting**:
1. Confirm that the Enchantment key under `special-enchantment-setting` is **lowercase** (e.g. `sharpness`, not `SHARPNESS`);
2. Confirm the sub-key is named `maximum-level` (with a hyphen);
3. Confirm the Enchantment key can be found in the [Javadoc](https://jd.papermc.io/folia/26.1.2/org/bukkit/enchantments/Enchantment.html);
4. Reload the config and test.

---

## 9. Deployment

### 9.1 Build Artifact

```bash
# Windows
gradlew.bat clean build

# Unix-like
./gradlew clean build
```

After a successful build, the artifact is located at:

```
build/libs/UnlockEnchantment-0.1-all.jar
```

> **Note**: For deployment, use the fat jar with the `-all` suffix (includes the Kotlin runtime), not `UnlockEnchantment-0.1.jar` (contains only plugin classes and will throw `NoClassDefFoundError` at runtime due to the missing `kotlin-stdlib`).

### 9.2 Full Deployment Steps

1. **Build the jar**
   ```bash
   gradlew.bat build
   ```
   Confirm `build/libs/UnlockEnchantment-0.1-all.jar` was generated.

2. **Upload to the server**
   - Copy `UnlockEnchantment-0.1-all.jar` to the server's `plugins/` directory;
   - If an old version exists, delete the old jar before uploading the new one.

3. **Restart the server**
   ```bash
   # Linux
   ./stop  # or type stop in the console
   ./start.sh

   # Or restart via the panel
   ```
   - The first start auto-generates `plugins/UnlockEnchantment/config.yml`;
   - The console should output `[UnlockEnchantment] Loaded`.

4. **Verify**
   - Check the console for errors;
   - Run `/plugins` to confirm UnlockEnchantment appears green (enabled);
   - Check that `plugins/UnlockEnchantment/config.yml` was generated with complete content;
   - Enter the game and test Anvil merging and enchanted-book duplication.

### 9.3 Config Changes and Effective Timing

| Method | Operation | Effective Timing |
|--------|-----------|------------------|
| Restart server | After editing `config.yml`, run `stop` + `start` | Immediate |
| `/reload confirm` | After editing `config.yml`, run this command | Immediate (triggers `onEnable`) |
| Plugin reload command | Not registered in the current version; implement yourself | — |

### 9.4 Rollback

If a new version has issues:
1. `stop` the server;
2. Remove the new jar and put back the old jar;
3. `start` the server;
4. (Optional) Back up `config.yml` before rolling back the config.

---

## 10. Version Control Conventions

### 10.1 Branch Strategy

A simplified Git Flow is adopted:

| Branch | Purpose | Naming |
|--------|---------|--------|
| `main` | Production branch; always deployable stable version | `main` |
| `develop` | Development integration branch; features merge here | `develop` |
| `feature/*` | Feature development branch; cut from `develop` | `feature/anvil-bugfix`, `feature/new-command` |
| `hotfix/*` | Emergency fix branch; cut from `main`, merged back to `main` and `develop` | `hotfix/config-crash` |
| `release/*` | Release preparation branch | `release/0.2` |

**Workflow**:
1. Cut `feature/xxx` from `develop` for development;
2. After development, open a PR to merge back into `develop`;
3. When preparing a release, cut `release/x.x` from `develop`; after tests pass, merge into `main` and tag;
4. Emergency fixes cut `hotfix/xxx` from `main`; after fixing, merge back into `main` and `develop`.

### 10.2 Commit Message Conventions

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <subject>

<body>

<footer>
```

| type | Meaning |
|------|---------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation change |
| `style` | Code formatting (no functional impact) |
| `refactor` | Refactor (neither a new feature nor a fix) |
| `perf` | Performance optimization |
| `test` | Test-related |
| `chore` | Build/tooling/dependency change |

**Examples**:

```
feat(anvil): support configurable Special Enchantment cap override

Check specialEnchantments before writing the result in AnvilListener; override when exceeding the custom cap.
Add the SpecialEnchantments data class.
```

```
fix(craft): fix item loss when cursor is non-empty during enchanted-book crafting

Add a cursor-non-empty check in onCraft to avoid the result item being overwritten.
```

### 10.3 Tag Conventions

Use Semantic Versioning: `v<major>.<minor>.<patch>`

- `v0.1` — first usable version (current)
- `v0.1.1` — bug fix
- `v0.2.0` — new feature
- `v1.0.0` — first official release

**Tagging**:

```bash
git tag -a v0.1 -m "First usable version"
git push origin v0.1
```

### 10.4 .gitignore Notes

The project `.gitignore` already ignores:
- `.gradle` / `build` — Gradle cache and build artifacts;
- `.idea` / `*.iml` / `out/` — IntelliJ project files;
- `run` — run-paper test server working directory;
- `.DS_Store` — macOS system files;
- `hs_err_pid*` — JVM crash logs.

**Never commit**: `config.yml` (contains server-specific config), `*.jar` (build artifacts), any file containing secrets.

---

## 11. Code Style Guide

### 11.1 Kotlin Coding Conventions

| Item | Convention | Example |
|------|------------|---------|
| Package name | all lowercase, reverse domain | `org.luminolcraft.unlockEnchantment` |
| Class name | PascalCase | `AnvilListener`, `ConfigManager` |
| Function name | camelCase | `onPrepareAnvil`, `truncateOnes` |
| Variable name | camelCase | `firstItem`, `itemEnchants` |
| Constant | UPPER_SNAKE_CASE | (no top-level constants in this project yet) |
| Indentation | 4 spaces; tabs forbidden | — |
| Trailing semicolon | no semicolon | `val x = 1` |
| Strings | prefer double quotes; use `"""` for multiline | `"hello"`, `"""..."""` |
| Blank lines | 1 blank line between methods; 1–2 between classes | — |
| import | sort alphabetically; no wildcards `*` | `import org.bukkit.Material` |

### 11.2 KDoc Comment Conventions

Public classes and public methods should have KDoc:

```kotlin
/**
 * Anvil Enchantment merge listener: core implementation that lifts the vanilla Anvil "Too Expensive" restriction.
 *
 * ## Business Goal
 * The vanilla Anvil displays "Too Expensive" and blocks the player from taking the result when the merge cost ≥ 40 levels.
 * This listener recomputes the merged Enchantment result of two items when the player opens the Anvil UI and places items,
 * bypassing the vanilla cap so high-level Enchantments can continue to be merged and upgraded.
 *
 * @param event The Anvil prepare event triggered by Bukkit
 */
```

- Use `##` to separate sections;
- Use `@param` / `@return` / `@throws` to annotate parameters and return values;
- Use `[ClassName]` or `[method()]` for inline references.

### 11.3 Bukkit Event Listener Conventions

```kotlin
class XxxListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onXxx(event: XxxEvent) {
        // 1. Validate first (early return)
        if (!Main.configManager.isPluginEnabled) return

        // 2. Take out the data you need
        val item = event.item ?: return

        // 3. Business logic
        // ...

        // 4. Modify the result
        event.result = newItem
    }
}
```

**Key points**:
- Implement the `Listener` interface (no need to extend `JavaPlugin`);
- Add the `@EventHandler` annotation to each listener method and explicitly declare `priority`;
- Put validation logic at the very front and `return` early to avoid deep nesting;
- Put result modification at the end;
- Register listeners via `Bukkit.getPluginManager().registerEvents(XxxListener(), this)`; no manual unregistration is needed (auto-unregistered on plugin disable).

### 11.4 Folia Compatibility Conventions

- **Never** use `Bukkit.getScheduler()` / `BukkitRunnable` (unsupported on Folia);
- **Must** use `Player.scheduler.runDelayed` / `runAtFixedRate` (entity-region-based) or `regionScheduler` / `globalRegionScheduler`;
- Cross-region operations must be scheduled via `RegionScheduler`;
- When accessing entity or block state, ensure execution on the corresponding region thread.

---

## 12. Testing Strategy

This project currently has **no automated unit tests** and relies on manual testing. The following is a complete manual test checklist.

### 12.1 Test Environment Preparation

1. Run `gradlew.bat runServer` to start the local test server;
2. Connect a creative-mode client to `localhost`;
3. Prepare items: anvil, enchanted books (various levels), normal books, weapons, equipment, XP bottles.

### 12.2 Anvil Merge Test Checklist

| ID | Scenario | Operation | Expected Result |
|----|----------|-----------|-----------------|
| A1 | Regular equal-level merge | Sharpness V sword + Sharpness V book | Result Sharpness VI (equal level +1) |
| A2 | Regular unequal-level merge | Sharpness V sword + Sharpness III book | Result Sharpness V (take the higher) |
| A3 | Single-level Enchantment merge | Mending book + Mending book | Result Mending I (fixed startLevel) |
| A4 | Blacklisted Enchantment | Configure `blacklist: [sharpness]`, Sharpness V + Sharpness V | Sharpness retains original value; no merge upgrade |
| A5 | Merge above level 40 | Merge two high-level enchanted books | No "Too Expensive"; result can be taken normally |
| A6 | Expensive notice | Trigger a merge with cost ≥ 40 | Receive the MiniMessage notice |
| A7 | Special cap | Configure `sharpness: { maximum-level: 5 }`, Sharpness V + Sharpness V | Result Sharpness V (overridden by cap, does not exceed 5) |
| A8 | Maximum cost limit | Configure `maximum-level-cost: 30`, trigger a merge with cost 50 | Cost truncated to 30 |
| A9 | Cross-level merge (simplify mode) | Equipment Sharpness 13 + book Sharpness 12, `simplify-enchantment: true` | Result Sharpness 14 (truncated equal +1) |
| A10 | Cross-level merge not triggered | Equipment Sharpness 13 + book Sharpness 5, `simplify-enchantment: true` | Keep Sharpness 13 |
| A11 | Fill exclusive Enchantments | Sharpness V sword + Protection IV book (sword has no Protection) | Result has both Sharpness V and Protection IV |
| A12 | Plugin disabled | `enabled: false`, any merge | Vanilla behavior (40-level cap active) |

### 12.3 Enchantment Simplification Crafting Test Checklist

| ID | Scenario | Operation | Expected Result |
|----|----------|-----------|-----------------|
| B1 | Normal duplication | Enchanted book (Sharpness V) + normal book | Result is a Sharpness V enchanted-book copy; normal book -1, enchanted book preserved |
| B2 | Multiple duplications | Same enchanted book crafted 5 times in a row | Each time only 1 normal book is consumed; enchanted book always preserved |
| B3 | Insufficient normal books | Enchanted book + 0 normal books | Cannot craft (matrix is not 2 items) |
| B4 | Non-empty cursor | Click the result with an item on the cursor | Crafting canceled; no item lost |
| B5 | Wrong recipe | Enchanted book + writable book (WRITABLE_BOOK) | Simplification crafting not triggered |
| B6 | Feature disabled | `simplify-enchantment: false` | Simplification crafting not triggered; vanilla behavior |
| B7 | Enchanted book NBT preserved | Check the enchanted book and copy NBT after duplication | Identical |

### 12.4 Config Reload Test Checklist

| ID | Scenario | Operation | Expected Result |
|----|----------|-----------|-----------------|
| C1 | Reload toggle | Modify `enabled` then `/reload confirm` | Toggle takes effect immediately |
| C2 | Reload Blacklist | Modify `blacklist` then reload | Blacklist updates immediately |
| C3 | Reload Special cap | Modify `special-enchantment-setting` then reload | Cap updates immediately |
| C4 | Missing config key | Delete `maximum-level-cost` then restart | Auto-filled with default -1 |
| C5 | Invalid value | `maximum-level-cost: -5` | Auto-falls back to -1 |

### 12.5 Boundary Value Test Checklist

| ID | Scenario | Expected Result |
|----|----------|-----------------|
| D1 | Merge level exactly 40 | Take normally; notice sent |
| D2 | Merge level exactly 39 | Take normally; no notice sent |
| D3 | `maximum-level-cost: 0` | Cost truncated to 0 |
| D4 | Empty enchanted-book merge | Vanilla behavior; no error |
| D5 | One side item empty | Listener returns early; no error |
| D6 | Enchanted book + equipment (different types) | Goes through cross-level merge or fill logic |

### 12.6 Test Record Recommendations

Before each release, complete all the above checklists and record:
- Test date, tester, test server version;
- The actual result of each case (pass/fail + description);
- Reproduction steps and logs for failed cases.

---

## 13. Maintenance Notes

### 13.1 Upgrading the Folia API

1. **Version correspondence**: The version `dev.folia:folia-api:26.1.2.build.+` in `build.gradle.kts` must match the target server version. Update this when upgrading the server.
2. **API changes**: Follow the PaperMC [Changelog](https://github.com/PaperMC/Paper); pay special attention to:
   - `Enchantment` registry-related APIs (this project uses `RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)`);
   - Field changes in `PrepareAnvilEvent` / `PrepareItemCraftEvent`;
   - Whether `AnvilView.maximumRepairCost` / `repairCost` properties are renamed or deprecated.
3. **`api-version`**: After upgrading, update `api-version` in `plugin.yml` accordingly.
4. **run-paper version**: The `minecraftVersion` of `runServer` must be updated in sync.
5. **Regression testing**: After upgrading, be sure to execute the full test checklist in Section 12.

### 13.2 Upgrading the Kotlin Version

1. **Version selection**: Currently using `2.4.20-Beta1`. Before upgrading to a stable release, confirm the version supports `jvmToolchain(25)` and the Folia API dependency.
2. **stdlib sync**: `kotlin-stdlib-jdk8` is auto-aligned with the Kotlin plugin version; no manual version specification is needed.
3. **Compatibility testing**: After upgrading, run `gradlew.bat clean build` to confirm compilation passes, and launch the test server to verify functionality.
4. **IDE sync**: Upgrade IDEA's Kotlin plugin to the corresponding version.

### 13.3 Config Compatibility

1. **Backward compatibility**: `ConfigManager.initConfig()` checks each key for existence and writes defaults for missing ones. New config keys do not cause errors in old config files.
2. **Field renaming**: If you need to rename a config key, perform a migration in `initConfig` (read the old key → write the new key → delete the old key) to avoid losing user config.
3. **Default value changes**: After modifying a default, existing config files will not auto-update (because the key already exists). You can trigger a migration via a `config.set("config-version", x)` mechanism when the version number changes.
4. **Type changes**: Avoid changing the type of an existing config key (e.g. int → string); it will cause a `ClassCastException`.

### 13.4 Performance Notes

1. **Event listener frequency**:
   - `PrepareAnvilEvent` triggers on every change to the Anvil items by the player; can be high-frequency;
   - `PrepareItemCraftEvent` similarly;
   - `InventoryClickEvent` triggers on any Crafting Table slot click by the player; even higher frequency.

   **Recommendation**: The listeners already do early `return` validation, but avoid heavy computation inside listeners (e.g. large NBT serialization). The current implementation is O(number of Enchantments), usually < 10, which is acceptable.

2. **MiniMessage parsing**: `MiniMessage.miniMessage().deserialize()` re-parses the string on every call. If `expensive-enchant-message` triggers frequently, consider caching the parsed result (not done currently).

3. **Folia scheduling**: `Player.scheduler.runDelayed` is set to a 1-tick delay; the overhead is minimal. But avoid frequent scheduling in loops.

4. **Memory usage**: `specialEnchantments` and `blackListEnchantments` are built once during `loadConfig` and only read afterward; no memory leak risk.

5. **Concurrency safety**:
   - `Main.configManager` is a `lateinit var`; after initialization, read-only field access is safe;
   - The `var` fields of `ConfigManager` (e.g. `isPluginEnabled`) may be modified by another thread during `reloadConfig`, but Bukkit events fire on the main/region thread and `reload` also runs on the main thread; in practice there is no race;
   - If async reloading is introduced in the future, add `@Volatile` or a lock.

### 13.5 Known Issues and Improvement Directions

| Issue | Impact | Suggested Improvement |
|-------|--------|----------------------|
| `expensive-enchant-message` default placeholder inconsistency (`initConfig` uses `<level>`, `loadConfig` fallback uses `{level}`) | Placeholder may not be replaced in first-generated config | Unify to `{level}` |
| `blacklist` / `special-enchantment-setting` default is `listOf(null)` | A `null` placeholder appears in the YAML | Change to an empty list `listOf()` |
| No reload command registered | Must use `/reload confirm` or restart | Implement an `unlockenchantment reload` command |
| No automated tests | Each release requires manual regression | Introduce JUnit 5 + MockBukkit for unit tests |
| Multiple non-null assertions (`firstItemEnchants!!`) in `AnvilListener` | Theoretically possible NPE (guaranteed not to by prior validation) | Use local variables to reduce `!!` |
| `InventoryClickEvent` does not check whether `event.isCancelled` was already set by another plugin | May conflict with other plugins | Add a check for `event.isCancelled` |

### 13.6 Contact and Feedback

- **Organization**: LuminolCraft
- **Package namespace**: `org.luminolcraft`
- **Issue feedback**: Submit via the repository's Issue system, including the server version, plugin version, error logs, and reproduction steps.

---

> This documentation is written based on the UnlockEnchantment 0.1 source code; last updated: 2026-07-08. If the documentation conflicts with the code, the source code prevails.
