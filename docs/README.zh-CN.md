# UnlockEnchantment 中文文档

> **版本**：0.1  
> **适用服务端**：Paper / Folia 26.1.2  
> **主类**：`org.luminolcraft.unlockEnchantment.Main`  
> **作者/组织**：LuminolCraft (`org.luminolcraft`)

本仓库为 Minecraft Paper/Folia 服务端插件 **UnlockEnchantment（解锁附魔）** 的企业级中文技术文档，面向首次接手项目的开发与运维人员。文档涵盖项目背景、环境配置、技术栈、目录结构、核心模块、配置参考、事件接口、常见问题、部署流程、版本控制、代码风格、测试策略与维护注意事项共十三个章节。

---

## 1. 项目背景与目标

### 1.1 背景

Minecraft 原版铁砧（Anvil）在合并附魔或修复物品时，存在一个硬性上限：当本次操作所需的**经验等级花费 ≥ 40** 时，铁砧界面会显示"过于昂贵"（Too Expensive）并阻止玩家取出结果。这一限制由 `AnvilView.maximumRepairCost`（默认 40）控制，导致：

- 高等级附魔（如锋利 V + 锋利 V）无法继续合并升级；
- 装备修复与附魔合并混合使用时容易触发上限；
- 玩家无法获得超过原版最大等级的附魔（即使模组/数据包扩展了附魔等级范围）。

此外，原版附魔书合并成本较高，玩家在反复升级附魔时需要消耗大量附魔书，体验欠佳。

### 1.2 目标

UnlockEnchantment 旨在在不修改服务端核心、不依赖第三方模组（如 ProtocolLib、ItemsAdder）的前提下，通过纯 Bukkit 事件监听实现以下目标：

1. **解除铁砧附魔等级上限**：将 `maximumRepairCost` 提升至 `Int.MAX_VALUE`，并重新计算合并后的附魔等级，绕过原版 40 级拦截。
2. **提供附魔简化合成**：允许玩家用 1 本附魔书 + 1 本普通书在工作台合成附魔书副本，**仅消耗普通书**，附魔书原样保留，便于低成本复制附魔书进行后续合并。
3. **可配置化**：通过 `config.yml` 控制开关、黑名单、特殊附魔上限、最大花费、提示消息等，适配不同服务器的运营需求。
4. **Folia 兼容**：使用 Paper/Folia 提供的区域调度 API（`Player.scheduler.runDelayed`），保证在 Folia 多线程服务端下正常运行。

### 1.3 适用场景

- **生存/RPG 服务器**：希望提供更高等级附魔的成长线，提升玩家追求上限的动力。
- **休闲/创造服务器**：希望解除原版限制，让玩家自由实验各种附魔组合。
- **经济型服务器**：通过"附魔书复制"功能降低附魔获取成本，配合经济系统调控。
- **需要 Folia 多线程优化的高负载服务器**：本插件已声明 `folia-supported: true` 并使用区域调度 API。

---

## 2. 环境配置指南

本项目区分**开发环境**、**测试环境**、**生产环境**三套配置，下文分别说明。

### 2.1 开发环境

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 25 | 构建脚本通过 `jvmToolchain(25)` 强制要求；Gradle 会自动下载缺失的工具链 |
| Gradle | 9.6.0（Wrapper 自带） | 项目内置 `gradlew` / `gradlew.bat`，无需本机预装 Gradle |
| Kotlin | 2.4.20-Beta1 | 由 `build.gradle.kts` 的 `kotlin("jvm")` 插件提供 |
| IDE | IntelliJ IDEA 2025.x 及以上 | 推荐 Kotlin 插件支持 2.4.x；社区版即可 |
| Git | 任意现代版本 | 用于版本控制 |

**配置步骤**：

1. 安装 JDK 25（推荐 Eclipse Temurin / Oracle JDK），确认 `java -version` 输出 `25`。
2. 安装 IntelliJ IDEA，在 `File → Project Structure → SDK` 中指向 JDK 25。
3. 克隆仓库：`git clone <repo-url>`。
4. 在 IDEA 中 `Open` 项目根目录，等待 Gradle 同步完成（首次会下载 Folia API、Kotlin 运行时等依赖）。
5. 确认 `gradle/wrapper/gradle-wrapper.properties` 中 `distributionUrl` 指向 `gradle-9.6.0-bin.zip`。

```bash
# 验证 JDK 版本
java -version

# 验证 Gradle Wrapper 可用（Windows 用 gradlew.bat）
./gradlew --version
```

### 2.2 测试环境（本地 runServer）

项目内置 `xyz.jpenilla.run-paper` 插件，提供 `runServer` 任务，可一键下载并启动 Minecraft 26.1.2 测试服，自动将构建产物注入 `plugins/` 目录。

**配置步骤**：

1. 执行构建与启动：
   ```bash
   # Windows
   gradlew.bat runServer

   # Unix-like
   ./gradlew runServer
   ```
2. 首次启动会下载 Minecraft 26.1.2 服务端（Paper/Folia），分配 2G 内存（`-Xms2G -Xmx2G`，见 `build.gradle.kts`）。
3. 在控制台同意 EULA（若提示）。
4. 插件 jar 会自动放入测试服的 `plugins/` 目录。
5. 首次启动后，`plugins/UnlockEnchantment/config.yml` 会自动生成。
6. 用 Minecraft 客户端连接 `localhost`（默认端口 25565）进行手动测试。

**测试服配置建议**：

- 在 `server.properties` 中设置 `online-mode=false` 便于离线账号测试；
- 设置 `gamemode=creative`，方便获取附魔书与铁砧；
- 设置 `level-name=world_ue_test` 隔离测试世界。

### 2.3 生产环境（Paper/Folia 服务端部署）

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| 服务端 | Paper 26.1.2 或 Folia 26.1.2 | 必须与插件 API 版本一致 |
| Java | JDK 25 | 服务端运行时所需 |
| 操作系统 | Linux（推荐）/ Windows Server | Linux 下内存与并发表现更佳 |

**部署步骤**：

1. 从 [PaperMC 官网](https://papermc.io/) 下载对应版本的 Paper 或 Folia 服务端 jar。
2. 首次启动服务端生成 `eula.txt`，将其中的 `eula=false` 改为 `eula=true`。
3. 构建插件 jar（见第 9 章「部署流程」）。
4. 将 `UnlockEnchantment-0.1-all.jar` 复制到服务端 `plugins/` 目录。
5. 重启服务端，确认控制台输出 `[UnlockEnchantment] Loaded`。
6. 检查 `plugins/UnlockEnchantment/config.yml` 已生成，按需修改配置。
7. 修改配置后执行 `/reload confirm` 或重启服务端使其生效。

---

## 3. 技术栈详解

### 3.1 技术栈总览

| 技术 | 版本 | 作用 |
|------|------|------|
| Kotlin | 2.4.20-Beta1 | 主开发语言，提供空安全、协程、数据类等现代特性 |
| Folia API | 26.1.2（`dev.folia:folia-api`） | 提供与服务端交互的 Bukkit/Paper/Folia API，兼容 Paper |
| Gradle | 9.6.0（Wrapper） | 构建工具，管理依赖、任务、打包 |
| Shadow | 9.4.3（`com.gradleup.shadow`） | 生成 fat jar，将 Kotlin 标准库打包进最终产物 |
| run-paper | 3.0.2（`xyz.jpenilla.run-paper`） | 提供 `runServer` 任务，本地启动测试服 |
| MiniMessage | 随 Folia API 附带 | 用于格式化玩家提示消息（颜色、悬浮、占位符） |

### 3.2 各技术职责说明

#### Kotlin 2.4.20-Beta1
- **为何使用**：相比 Java，Kotlin 语法简洁（数据类、空安全、`when` 表达式），减少样板代码；与 Java 100% 互操作，可直接调用 Bukkit API。
- **版本说明**：2.4.20-Beta1 为 Beta 版本，对应较新的语言特性。如需升级到稳定版，需评估与 Folia API 的兼容性。
- **打包方式**：Kotlin 标准库（`kotlin-stdlib-jdk8`）通过 Shadow 插件打包进 fat jar，服务端无需单独安装 Kotlin。

#### Folia API 26.1.2
- **作用**：`compileOnly("dev.folia:folia-api:26.1.2.build.+")` 提供编译期类型，运行时由服务端提供实现，不打包进 jar。
- **Folia 与 Paper 的关系**：Folia 是 Paper 的多线程分支，将世界划分为多个区域独立 tick。本插件声明 `folia-supported: true`，并在合成事件中使用 `Player.scheduler.runDelayed`（区域调度 API）而非 `BukkitScheduler`，以兼容 Folia。
- **兼容性**：Folia API 兼容 Paper 服务端，因此在纯 Paper 服务端上也可正常运行。

#### Gradle 9.6.0 + Kotlin DSL
- **构建脚本语言**：使用 Kotlin DSL（`build.gradle.kts`），相比 Groovy 提供类型推导与 IDE 自动补全。
- **性能优化**：`gradle.properties` 中启用了配置缓存（`org.gradle.configuration-cache`）、并行构建（`org.gradle.parallel`）、构建缓存（`org.gradle.caching`），加速二次构建。

#### Shadow 9.4.3
- **作用**：将 `implementation` 依赖（如 `kotlin-stdlib-jdk8`）打包进最终 jar，生成 `UnlockEnchantment-0.1-all.jar`。
- **配置**：`tasks.build { dependsOn(shadowJar) }` 确保 `build` 任务产出 fat jar。

#### run-paper 3.0.2
- **作用**：提供 `runServer` 任务，自动下载 Minecraft 服务端并启动，便于本地调试。
- **配置**：`minecraftVersion("26.1.2")` 指定测试服版本；`jvmArgs("-Xms2G", "-Xmx2G")` 分配内存。

#### MiniMessage
- **作用**：用于 `expensive-enchant-message` 配置项，支持 `<RED>`、`<green>`、`<hover:show_item:...>` 等标签，比传统 `§` 颜色码更强大。
- **调用方式**：`MiniMessage.miniMessage().deserialize(str)` 将字符串解析为 Adventure 组件发送给玩家。

---

## 4. 目录结构说明

### 4.1 文件树

```
UnlockEnchantment/
├── build.gradle.kts                          # Gradle 构建脚本（Kotlin DSL）
├── gradle.properties                         # Gradle 属性（group/version/缓存开关）
├── settings.gradle.kts                       # Gradle 设置（项目名）
├── gradlew / gradlew.bat                     # Gradle Wrapper 启动脚本
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties         # Gradle 版本与下载地址
├── .gitignore                                # Git 忽略规则
├── .gitattributes                            # Git 属性
└── src/main/
    ├── kotlin/org/luminolcraft/unlockEnchantment/
    │   ├── Main.kt                           # 插件入口，注册事件监听器
    │   ├── AnvilListener.kt                  # 铁砧事件监听器（核心：解除附魔上限）
    │   ├── CraftListener.kt                  # 合成事件监听器（附魔简化合成）
    │   └── config/
    │       └── ConfigManager.kt              # 配置管理（读取 config.yml）
    └── resources/
        └── plugin.yml                        # Bukkit 插件描述文件
```

### 4.2 各文件职责

| 文件 | 职责 |
|------|------|
| `build.gradle.kts` | 声明插件（Kotlin JVM、Shadow、run-paper）、配置仓库与依赖、定义 build/runServer/processResources 任务 |
| `gradle.properties` | 存放项目元信息（`group=org.luminolcraft`、`version=0.1`）与 Gradle 性能开关 |
| `settings.gradle.kts` | 设定 `rootProject.name = "UnlockEnchantment"`，影响 jar 默认文件名 |
| `gradle/wrapper/gradle-wrapper.properties` | 指定 Gradle 9.6.0 下载地址，保证团队构建版本一致 |
| `Main.kt` | 插件入口类，继承 `JavaPlugin`；`onEnable` 中初始化 `ConfigManager`、注册 `AnvilListener` 与 `CraftListener`；通过 `companion object` 暴露全局 `configManager` |
| `AnvilListener.kt` | 监听 `PrepareAnvilEvent`，解除 `maximumRepairCost` 限制，按合并规则重算附魔，应用特殊上限，发送昂贵提示 |
| `CraftListener.kt` | 监听 `PrepareItemCraftEvent` 与 `InventoryClickEvent`，实现附魔书 + 普通书 = 附魔书副本（仅消耗普通书） |
| `ConfigManager.kt` | 管理 `config.yml` 的创建、默认值写入、读取、字段映射；提供 `reloadConfig()`；定义 `SpecialEnchantments` 数据类 |
| `plugin.yml` | Bukkit 插件描述：`name`、`main`、`version`（构建时注入）、`api-version: 26.1.2`、`load: POSTWORLD`、`folia-supported: true` |

### 4.3 包结构

```
org.luminolcraft.unlockEnchantment
├── Main                        # 主类
├── AnvilListener               # 铁砧监听器
├── CraftListener               # 合成监听器
└── config
    ├── ConfigManager           # 配置管理器
    └── SpecialEnchantments     # 特殊附魔数据类（同文件）
```

包命名遵循 Java 反向域名约定：`org.luminolcraft` 为组织命名空间，`unlockEnchantment` 为项目名。

---

## 5. 核心功能模块介绍

本项目包含三个核心模块：**铁砧解锁模块**、**附魔简化合成模块**、**配置管理模块**。

### 5.1 铁砧解锁模块（AnvilListener）

**文件**：`src/main/kotlin/org/luminolcraft/unlockEnchantment/AnvilListener.kt`

#### 5.1.1 模块职责

监听 `PrepareAnvilEvent`，在玩家打开铁砧界面、放入/移除物品时，重新计算两个物品合并后的附魔结果，绕过原版 40 级上限。

#### 5.1.2 工作流程

1. **解除上限**：将 `event.view.maximumRepairCost` 设为 `Int.MAX_VALUE`，使原版"过于昂贵"逻辑永不触发。
2. **读取附魔**：
   - 附魔书（`ENCHANTED_BOOK`）：通过 `EnchantmentStorageMeta.storedEnchants` 读取（`ItemStack.enchantments` 对附魔书返回空）。
   - 普通物品：通过 `ItemStack.enchantments` 直接读取。
3. **合并附魔**：遍历第二物品的每条附魔，按以下规则决定结果等级（详见 5.1.3）。
4. **补齐独有附魔**：将第二物品中存在、但第一物品没有的附魔直接加入结果。
5. **限制最大花费**：若配置了 `maximum-level-cost`（≠ -1）且当前 `repairCost` 已达到上限，则截断为上限。
6. **发送昂贵提示**：`repairCost > 39` 时向玩家发送 MiniMessage 提示。
7. **应用特殊上限**：若附魔在 `specialEnchantments` 中或结果等级超过自定义上限，则覆盖为自定义上限。
8. **写入结果**：用 `result.addUnsafeEnchantment` 写入合并后的附魔，绕过原版兼容性检查（`addEnchantment` 会因等级超过 `maxLevel` 抛异常）。

#### 5.1.3 附魔合并规则

遍历第二物品的每条附魔 `s`（key=附魔，value=等级）：

| 分支 | 条件 | 结果等级 |
|------|------|----------|
| A. 黑名单 | `s.key ∈ blackListEnchantments` | 保留 `s.value`（第二物品原值） |
| B. 单级附魔 | `s.key.maxLevel == 1`（如经验修补、无限、绑定诅咒） | `s.key.startLevel`（通常为 1） |
| C-1. 常规合并 | 第一物品有该附魔，且 `firstLevel ≤ 10` 或两物品类型相同 | `firstLevel < secondLevel` → 取 `secondLevel`；同级 → `secondLevel + 1`；否则保留 `firstLevel` |
| C-2. 跨级合并 | 第一物品为装备（剑/镐/斧/锹/锄/胸甲/靴子/护腿/头盔）+ 第二物品为附魔书 + `isEnchantmentSimplify` 开启 + `firstLevel > 10` | `firstLevel < secondLevel` → 取 `secondLevel`；`truncateOnes(firstLevel) == truncateOnes(secondLevel)` → `firstLevel + 1`；否则保留 `firstLevel` |

**`truncateOnes` 函数**：截断个位数，即 `number / 10 * 10`。例如 `13 → 10`、`25 → 20`、`9 → 0`。其作用是让等级接近的附魔被视为"同级"，允许跨级合并升级。

**示例**：
- 装备锋利 13 + 书锋利 12 → `truncateOnes(13)=10 == truncateOnes(12)=10` → 结果 14（跨级升级）。
- 装备锋利 13 + 书锋利 5 → `13 > 5` 且截断后 `10 ≠ 0` → 保留 13。

#### 5.1.4 特殊附魔上限覆盖

在写入结果前，对每条合并后的附魔检查：

- 若该附魔存在于 `specialEnchantments` 中，**或**结果等级超过其 `maximumLevels`，则将结果等级设为 `specialEnchantments[key].maximumLevels`。

例如配置 `SHARPNESS: { maximum-level: 5 }`，则锋利无论合并到多高，最终都不会超过 5 级。

### 5.2 附魔简化合成模块（CraftListener）

**文件**：`src/main/kotlin/org/luminolcraft/unlockEnchantment/CraftListener.kt`

#### 5.2.1 模块职责

玩家在工作台放入 **1 本附魔书（`ENCHANTED_BOOK`）+ 1 本普通书（`BOOK`）**，合成结果为附魔书的副本，但**仅消耗普通书**，附魔书原样保留。便于低成本复制附魔书进行后续合并。

#### 5.2.2 两阶段实现

由于原版合成会同时消耗两本书，本模块用两个事件接管流程：

| 阶段 | 事件 | 方法 | 作用 |
|------|------|------|------|
| 准备 | `PrepareItemCraftEvent` | `onPrepareCraft` | 把结果槽设为附魔书副本，用于 UI 展示 |
| 取结果 | `InventoryClickEvent` | `onCraft` | 取消原版合成，自定义扣减普通书、保留附魔书、把副本放到光标 |

#### 5.2.3 onPrepareCraft 校验逻辑

1. 合成矩阵中恰好 2 个非空物品 + `isEnchantmentSimplify` 开启。
2. 两物品必须分别是 1 本 `ENCHANTED_BOOK` 和 1 本 `BOOK`。
3. 克隆附魔书作为结果写入 `event.inventory.result`。

#### 5.2.4 onCraft 处理逻辑

1. 校验：`isEnchantmentSimplify` 开启 + 当前打开的是 `CraftingInventory` + 点击者为 `Player` + 点击的是结果槽（`slot == 0`）。
2. **取消原版合成**：`event.isCancelled = true`，否则原版会消耗两本书。
3. 校验玩家光标为空（否则结果放不下）。
4. 取出合成矩阵中的普通书和附魔书。
5. **延迟 1 tick**（使用 `Player.scheduler.runDelayed`，Folia 区域调度 API）：
   - 扣减普通书 1 本，放回原格。
   - 附魔书原样克隆放回原格（不消耗）。
6. **立即**把附魔书副本放到玩家光标上。

> **为何延迟 1 tick**：在事件处理中直接修改 `inventory` 会被原版合成逻辑覆盖，必须等当前 tick 事件处理结束、原版合成流程跑完后，再修改格子内容才能生效。

> **为何用 `Player.scheduler.runDelayed`**：Folia 没有全局 `BukkitScheduler`，必须使用基于实体/区域的调度 API，保证任务在玩家所在区域的线程执行。

### 5.3 配置管理模块（ConfigManager）

**文件**：`src/main/kotlin/org/luminolcraft/unlockEnchantment/config/ConfigManager.kt`

#### 5.3.1 模块职责

作为插件与 `config.yml` 交互的唯一入口，承担：

1. 首次启动时创建 `plugins/UnlockEnchantment/config.yml` 并写入默认配置与注释。
2. 将配置键值映射为内存字段（布尔开关、整数上限、附魔黑名单等）。
3. 提供 `reloadConfig()` 支持运行时刷新配置。
4. 暴露内存字段供事件监听器读取。

#### 5.3.2 核心方法

| 方法 | 职责 |
|------|------|
| `initConfig()` | 确保配置文件存在；逐项检查键是否缺失，缺失则写入默认值与注释；持久化回磁盘 |
| `loadConfig()` | 调用 `initConfig()` 后将配置读取到内存字段 |
| `reloadConfig()` | `initConfig()` → `config.load(configFile)` → `loadConfig()`，用于运行时刷新 |
| `getEnchantmentFromString(str)` | 通过 Paper 的 `RegistryAccess` 查找附魔，返回 `Enchantment?` |

#### 5.3.3 SpecialEnchantments 数据类

```kotlin
class SpecialEnchantments(val enchant: Enchantment, val maximumLevels: Int)
```

将一个附魔与其自定义最大等级绑定，对应配置 `special-enchantment-setting` 中每一项的解析结果。例如 `SHARPNESS: { maximum-level: 5 }` 被解析为 `SpecialEnchantments(SHARPNESS, 5)`。

---

## 6. 配置项参考文档

### 6.1 配置文件位置

```
<服务端根目录>/plugins/UnlockEnchantment/config.yml
```

首次启动时由 `ConfigManager.initConfig()` 自动生成，包含全部默认值与英文注释。

### 6.2 配置项全字段表

| 字段名 | 类型 | 默认值 | 取值范围 | 影响 |
|--------|------|--------|----------|------|
| `enabled` | boolean | `true` | `true` / `false` | 插件功能总开关。`false` 时铁砧解锁与简化合成都不会介入原版逻辑 |
| `simplify-enchantment` | boolean | `true` | `true` / `false` | 是否启用附魔简化合成（工作台复制附魔书）以及铁砧跨级合并（C-2 分支） |
| `maximum-level-cost` | int | `-1` | `-1` 或 ≥ 0 的整数 | 铁砧最大等级花费。`-1` 表示无限（解除原版 40 级上限）；填正数则超过该值的花费被截断为该值 |
| `blacklist` | list | `[]` | 附魔命名空间键字符串列表 | 忽略的附魔列表，黑名单内的附魔在铁砧合并时保留原值不做处理 |
| `special-enchantment-setting` | section | `{}` | 附魔键 → `maximum-level: int` | 为指定附魔单独设置最大等级上限，合并结果超过该上限时被覆盖 |
| `expensive-enchant-message` | string | `<hover:show_item:enchanted_book></hover><RED>超出原版附魔显示，附魔所需等级为 <green><level></green>` | 任意 MiniMessage 字符串 | 花费 ≥ 40 时发送给玩家的提示消息，支持 `{level}` 占位符替换为实际花费 |

> **关于 `expensive-enchant-message` 占位符**：代码中使用 `{level}` 作为占位符（见 `AnvilListener.onPrepareAnvil` 中的 `replace("{level}", ...)`）。请确保配置字符串中包含 `{level}` 而非 `<level>`，否则实际花费不会替换。`initConfig` 写入的默认值中存在 `<level>` 与 `{level}` 的不一致（详见第 12 章测试与第 8 章 FAQ），建议手动确认配置中使用 `{level}`。

> **关于 `blacklist` 与 `special-enchantment-setting` 默认值**：代码中默认写入 `listOf(null)`（含一个 null 元素的列表），目的是让 YAML 中出现可填写的占位。实际使用时应替换为有效的附魔键。

### 6.3 附魔命名空间键说明

配置中的附魔名称使用 Minecraft 命名空间键（小写，如 `sharpness`、`protection`、`unbreaking`）。`getEnchantmentFromString` 内部通过 `NamespacedKey.minecraft(str)` 构造为 `minecraft:str` 形式查找。

完整列表参考：[Folia Javadoc - Enchantment](https://jd.papermc.io/folia/26.1.2/org/bukkit/enchantments/Enchantment.html)

常用附魔键速查：

| 附魔 | 键 | 原版最大等级 |
|------|----|------------|
| 锋利 | `sharpness` | 5 |
| 保护 | `protection` | 4 |
| 耐久 | `unbreaking` | 3 |
| 效率 | `efficiency` | 5 |
| 经验修补 | `mending` | 1 |
| 无限 | `infinity` | 1 |
| 抢夺 | `looting` | 3 |
| 时运 | `fortune` | 3 |
| 火焰附加 | `fire_aspect` | 2 |
| 击退 | `knockback` | 2 |

### 6.4 示例配置

```yaml
# UnlockEnchantment 配置文件

# 插件功能总开关
enabled: true

# 是否启用附魔简化合成与跨级合并
simplify-enchantment: true

# 铁砧最大等级花费，-1 表示无限
maximum-level-cost: -1

# 附魔黑名单（这些附魔不被插件处理）
blacklist:
  - mending
  - infinity

# 特殊附魔上限设置
special-enchantment-setting:
  sharpness:
    maximum-level: 10
  protection:
    maximum-level: 8
  unbreaking:
    maximum-level: 5

# 花费 ≥ 40 时的提示消息（MiniMessage 格式）
expensive-enchant-message: "<hover:show_item:enchanted_book></hover><RED>超出原版附魔显示，附魔所需等级为 <green>{level}</green>"
```

### 6.5 配置加载与重载

- **加载时机**：插件 `onEnable` 时调用 `configManager.loadConfig()`。
- **重载方式**：修改 `config.yml` 后，调用 `Main.configManager.reloadConfig()`（当前版本未注册命令，需通过 `/reload confirm` 或重启服务端触发 `onEnable` 重新加载）。
- **兼容性**：`initConfig` 会逐项检查键是否存在，缺失则补写默认值，因此**旧版本配置文件可平滑升级**到新版本（新增的配置项会自动补全）。

---

## 7. API / 事件接口文档

### 7.1 监听的 Bukkit 事件

| 事件类 | 监听器方法 | 优先级 | 触发时机 | 用途 |
|--------|-----------|--------|----------|------|
| `PrepareAnvilEvent` | `AnvilListener.onPrepareAnvil` | `MONITOR` | 玩家在铁砧放入/移除物品时 | 重新计算合并后的附魔结果，解除上限 |
| `PrepareItemCraftEvent` | `CraftListener.onPrepareCraft` | `MONITOR` | 玩家在工作台摆好材料时 | 设置结果槽为附魔书副本（用于 UI 展示） |
| `InventoryClickEvent` | `CraftListener.onCraft` | `MONITOR` | 玩家点击合成台任意槽位时 | 点击结果槽时取消默认合成，手动处理物品扣减 |

> **关于 `MONITOR` 优先级**：`MONITOR` 是最低优先级（在其他插件之后执行），通常用于只读监听。本项目需要重写结果，因此在 `MONITOR` 中显式覆盖 `event.result` / `event.isCancelled`。若与其他插件冲突，可考虑调整为 `HIGHEST`。

### 7.2 配置访问入口

其他代码通过 `Main.configManager`（伴生对象，全局可访问）读取配置字段：

```kotlin
import org.luminolcraft.unlockEnchantment.Main

// 读取配置
val enabled = Main.configManager.isPluginEnabled
val simplify = Main.configManager.isEnchantmentSimplify
val maxCost = Main.configManager.maximumLevelCost
val blacklist = Main.configManager.blackListEnchantments
val special = Main.configManager.specialEnchantments
val msg = Main.configManager.expensiveEnchantMessage

// 重载配置
Main.configManager.reloadConfig()
```

### 7.3 ConfigManager 字段一览

| 字段名 | 类型 | 可见性 | 对应配置项 | 说明 |
|--------|------|--------|-----------|------|
| `configFile` | `File` | `val` 公开 | — | 配置文件路径（`plugins/UnlockEnchantment/config.yml`） |
| `config` | `FileConfiguration` | `val` 公开 | — | 由 `JavaPlugin` 提供的配置对象 |
| `javaPlugin` | `JavaPlugin` | `val` 公开 | — | 插件主类实例，用于调度器等 |
| `isPluginEnabled` | `Boolean` | `var` 公开 | `enabled` | 插件功能总开关 |
| `isEnchantmentSimplify` | `Boolean` | `var` 公开 | `simplify-enchantment` | 简化合成与跨级合并开关 |
| `maximumLevelCost` | `Int` | `var` 公开 | `maximum-level-cost` | 最大等级花费，`-1` 表示无限 |
| `blackListEnchantments` | `MutableList<Enchantment?>` | `var` 公开 | `blacklist` | 转换后的黑名单附魔对象列表 |
| `specialEnchantments` | `MutableMap<Enchantment, SpecialEnchantments>` | `var` 公开 | `special-enchantment-setting` | 特殊附魔上限映射表 |
| `expensiveEnchantMessage` | `String` | `var` 公开 | `expensive-enchant-message` | 昂贵提示消息（MiniMessage） |

### 7.4 SpecialEnchantments 数据结构

```kotlin
class SpecialEnchantments(val enchant: Enchantment, val maximumLevels: Int)
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `enchant` | `Enchantment` | 该条设置所针对的附魔对象 |
| `maximumLevels` | `Int` | 为该附魔单独设置的最大等级上限 |

**访问示例**：

```kotlin
val special: Map<Enchantment, SpecialEnchantments> = Main.configManager.specialEnchantments

// 查询某附魔是否有特殊上限
val sharpnessSetting = special[Enchantment.SHARPNESS]
if (sharpnessSetting != null) {
    val maxLevel = sharpnessSetting.maximumLevels
    // ...
}

// 判断附魔是否在特殊设置中
if (special.containsKey(someEnchant)) {
    // ...
}
```

### 7.5 plugin.yml 元信息

```yaml
name: UnlockEnchantment
version: '${version}'              # 构建时由 processResources 替换为 0.1
main: org.luminolcraft.unlockEnchantment.Main
api-version: '26.1.2'
load: POSTWORLD                    # 所有世界加载完成后加载
folia-supported: true              # 声明支持 Folia
```

| 字段 | 值 | 说明 |
|------|----|------|
| `name` | `UnlockEnchantment` | 插件名，须与 `plugins/` 下文件夹名一致 |
| `version` | `0.1`（构建时注入） | 来自 `gradle.properties` 的 `version` |
| `main` | `org.luminolcraft.unlockEnchantment.Main` | 主类全限定名 |
| `api-version` | `26.1.2` | 目标 Paper/Folia API 版本 |
| `load` | `POSTWORLD` | 世界加载后加载（另一选项 `STARTUP` 为启动时加载） |
| `folia-supported` | `true` | 声明兼容 Folia 多线程服务端 |

---

## 8. 常见问题解决方案

### Q1：修改了 config.yml 但配置不生效怎么办？

**原因**：配置仅在 `onEnable` 时加载一次，修改文件后未触发重载。

**解决方案**：
1. 在服务端控制台执行 `/reload confirm`（需安装 Paper 的 `/reload` 命令）；
2. 或重启服务端；
3. 若需要在插件内提供重载命令，可参考 `ConfigManager.reloadConfig()` 自行实现命令注册（当前版本未注册命令）。

**验证**：重载后用 `/minecraft:data` 或日志打印 `Main.configManager.maximumLevelCost` 确认值已更新。

### Q2：附魔书 + 普通书在工作台合成没反应？

**可能原因与排查**：

| 原因 | 排查方式 | 解决方案 |
|------|----------|----------|
| `simplify-enchantment` 设为 `false` | 检查 `config.yml` | 设为 `true` 并重载 |
| `enabled` 设为 `false` | 检查 `config.yml` | 设为 `true` 并重载 |
| 合成矩阵非恰好 2 个物品 | 确认只放了 1 附魔书 + 1 普通书 | 移除多余物品 |
| 物品类型不对 | 确认是 `ENCHANTED_BOOK` 与 `BOOK`（非 `WRITABLE_BOOK`） | 使用正确的书 |
| 玩家光标非空 | 点击结果槽前光标必须为空 | 清空光标后重试 |
| 插件未加载 | 控制台检查 `[UnlockEnchantment] Loaded` | 重新部署 jar |

### Q3：在 Folia 服务端上报错？

**典型错误**：`UnsupportedOperationException` / `Not implemented` / 调度相关异常。

**原因**：Folia 移除了全局 `BukkitScheduler`，必须使用区域/实体调度 API。

**排查**：
1. 确认 `plugin.yml` 中 `folia-supported: true`；
2. 确认 `api-version: 26.1.2` 与服务端版本一致；
3. 检查代码中是否有直接使用 `Bukkit.getScheduler()` 的地方——本项目 `CraftListener` 已使用 `Player.scheduler.runDelayed`，若你修改了代码请保持该模式；
4. Folia 下 `InventoryClickEvent` 必须在主线程（区域线程）处理，本项目通过 `Player.scheduler` 保证。

### Q4：构建后找不到 jar 文件？

**原因**：找错了输出目录或任务未执行 `shadowJar`。

**解决方案**：
1. 执行完整构建：`gradlew.bat build`（Windows）或 `./gradlew build`；
2. 产物位于 `build/libs/UnlockEnchantment-0.1-all.jar`（注意是 `-all` 后缀的 fat jar）；
3. 若只看到 `UnlockEnchantment-0.1.jar`（无 `-all`），说明 `shadowJar` 未执行，检查 `build.gradle.kts` 中 `build { dependsOn(shadowJar) }` 是否存在；
4. 若 `build/` 目录不存在，先执行 `gradlew.bat clean build`。

### Q5：Kotlin 版本冲突或编译报错？

**典型错误**：`Unresolved reference`、`Kotlin version mismatch`、`jvmTarget` 相关警告。

**原因**：JDK 工具链版本与 Kotlin 编译目标不匹配，或 IDE 的 Kotlin 插件版本过旧。

**解决方案**：
1. 确认 JDK 25 已安装：`java -version` 应输出 `25`；
2. 在 `build.gradle.kts` 中确认 `jvmToolchain(25)`；
3. IntelliJ IDEA 中 `File → Invalidate Caches / Restart`，重新同步 Gradle；
4. 升级 IDEA 的 Kotlin 插件至支持 2.4.x 的版本；
5. 若坚持使用稳定版 Kotlin，可将 `build.gradle.kts` 中的 `2.4.20-Beta1` 改为最近的稳定版（如 `2.0.x`），但需测试与 Folia API 的兼容性。

### Q6：超过 40 级没有收到提示消息？

**可能原因**：

| 原因 | 排查方式 | 解决方案 |
|------|----------|----------|
| `expensive-enchant-message` 占位符错误 | 检查配置中是否使用 `{level}` | 将 `<level>` 改为 `{level}`（代码用 `replace("{level}", ...)`） |
| 消息被其他插件拦截 | 临时禁用聊天类插件测试 | 调整插件加载顺序或优先级 |
| `repairCost` 未 > 39 | 在铁砧界面观察花费等级 | 确认实际花费 ≥ 40 |
| MiniMessage 语法错误 | 检查标签是否闭合（如 `<red>...</red>` 或 `<green>...</green>`） | 修正语法 |

> **注意**：`initConfig()` 写入的默认消息中使用 `<level>`，但 `loadConfig()` 的兜底默认值使用 `{level}`，而 `AnvilListener` 中替换的是 `{level}`。建议手动将配置中的 `<level>` 改为 `{level}` 以保证占位符生效。

### Q7：特殊附魔上限不生效？

**排查**：
1. 确认 `special-enchantment-setting` 下使用**小写**附魔键（如 `sharpness` 而非 `SHARPNESS`）；
2. 确认子键名为 `maximum-level`（带连字符）；
3. 确认附魔键能在 [Javadoc](https://jd.papermc.io/folia/26.1.2/org/bukkit/enchantments/Enchantment.html) 中查到；
4. 重载配置后测试。

---

## 9. 部署流程

### 9.1 构建产物

```bash
# Windows
gradlew.bat clean build

# Unix-like
./gradlew clean build
```

构建成功后，产物位于：

```
build/libs/UnlockEnchantment-0.1-all.jar
```

> **注意**：部署时应使用 `-all` 后缀的 fat jar（含 Kotlin 运行时），而非 `UnlockEnchantment-0.1.jar`（仅含插件类，运行时会因缺少 `kotlin-stdlib` 报 `NoClassDefFoundError`）。

### 9.2 完整部署步骤

1. **构建 jar**
   ```bash
   gradlew.bat build
   ```
   确认 `build/libs/UnlockEnchantment-0.1-all.jar` 已生成。

2. **上传到服务端**
   - 将 `UnlockEnchantment-0.1-all.jar` 复制到服务端 `plugins/` 目录；
   - 若旧版本已存在，先删除旧 jar 再上传新 jar。

3. **重启服务端**
   ```bash
   # Linux
   ./stop  # 或在控制台输入 stop
   ./start.sh

   # 或使用面板重启
   ```
   - 首次启动会自动生成 `plugins/UnlockEnchantment/config.yml`；
   - 控制台应输出 `[UnlockEnchantment] Loaded`。

4. **验证**
   - 检查控制台无报错；
   - 执行 `/plugins` 确认 UnlockEnchantment 显示为绿色（已启用）；
   - 检查 `plugins/UnlockEnchantment/config.yml` 已生成且内容完整；
   - 进入游戏测试铁砧合并与附魔书复制功能。

### 9.3 配置修改与生效

| 修改方式 | 操作 | 生效时机 |
|----------|------|----------|
| 重启服务端 | 修改 `config.yml` 后 `stop` + `start` | 立即 |
| `/reload confirm` | 修改 `config.yml` 后执行该命令 | 立即（触发 `onEnable`） |
| 插件重载命令 | 当前版本未注册命令，需自行实现 | — |

### 9.4 回滚

若新版本出现问题：
1. `stop` 服务端；
2. 删除新 jar，放回旧 jar；
3. `start` 服务端；
4. （可选）备份 `config.yml` 后回滚配置。

---

## 10. 版本控制规范

### 10.1 分支策略

采用简化的 Git Flow：

| 分支 | 用途 | 命名 |
|------|------|------|
| `main` | 生产分支，始终可部署的稳定版本 | `main` |
| `develop` | 开发集成分支，功能合并到此 | `develop` |
| `feature/*` | 功能开发分支，从 `develop` 拉出 | `feature/anvil-bugfix`、`feature/new-command` |
| `hotfix/*` | 紧急修复分支，从 `main` 拉出，合并回 `main` 与 `develop` | `hotfix/config-crash` |
| `release/*` | 发布准备分支 | `release/0.2` |

**工作流**：
1. 从 `develop` 拉出 `feature/xxx` 开发；
2. 开发完成后提 PR 合并回 `develop`；
3. 准备发版时从 `develop` 拉出 `release/x.x`，测试通过后合并到 `main` 并打 tag；
4. 紧急修复从 `main` 拉出 `hotfix/xxx`，修复后合并回 `main` 与 `develop`。

### 10.2 提交信息规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

| type | 含义 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档变更 |
| `style` | 代码格式（不影响功能） |
| `refactor` | 重构（非新功能、非修复） |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建/工具/依赖变更 |

**示例**：

```
feat(anvil): 支持配置特殊附魔上限覆盖

在 AnvilListener 写入结果前检查 specialEnchantments，超过自定义上限时覆盖。
新增 SpecialEnchantments 数据类。
```

```
fix(craft): 修复附魔书合成时光标非空时丢失物品

onCraft 中增加光标非空校验，避免结果物品被覆盖。
```

### 10.3 标签规范

使用语义化版本（Semantic Versioning）：`v<major>.<minor>.<patch>`

- `v0.1` — 首个可用版本（当前）
- `v0.1.1` — Bug 修复
- `v0.2.0` — 新增功能
- `v1.0.0` — 首个正式版

**打标签**：

```bash
git tag -a v0.1 -m "首个可用版本"
git push origin v0.1
```

### 10.4 .gitignore 要点

项目 `.gitignore` 已忽略：
- `.gradle` / `build` — Gradle 缓存与构建产物；
- `.idea` / `*.iml` / `out/` — IntelliJ 工程文件；
- `run` — run-paper 测试服工作目录；
- `.DS_Store` — macOS 系统文件；
- `hs_err_pid*` — JVM 崩溃日志。

**切勿提交**：`config.yml`（含服务器个性化配置）、`*.jar`（构建产物）、任何含密钥的文件。

---

## 11. 代码风格指南

### 11.1 Kotlin 编码规范

| 项目 | 规范 | 示例 |
|------|------|------|
| 包名 | 全小写，反向域名 | `org.luminolcraft.unlockEnchantment` |
| 类名 | PascalCase | `AnvilListener`、`ConfigManager` |
| 函数名 | camelCase | `onPrepareAnvil`、`truncateOnes` |
| 变量名 | camelCase | `firstItem`、`itemEnchants` |
| 常量 | UPPER_SNAKE_CASE | （本项目暂无顶层常量） |
| 缩进 | 4 空格，禁止 Tab | — |
| 行尾分号 | 不加分号 | `val x = 1` |
| 字符串 | 优先双引号；多行用 `"""` | `"hello"`、`"""..."""` |
| 空行 | 方法间 1 空行；类间 1-2 空行 | — |
| import | 按 字母序排列，不使用通配符 `*` | `import org.bukkit.Material` |

### 11.2 KDoc 注释规范

公开类与公开方法应编写 KDoc：

```kotlin
/**
 * 铁砧附魔合并监听器：解除原版铁砧"过于昂贵"限制的核心实现。
 *
 * ## 业务目标
 * 原版铁砧在合并花费 ≥ 40 级时会显示"过于昂贵"并阻止玩家取出结果。
 * 本监听器在玩家打开铁砧界面、放入物品时，重新计算两个物品合并后的附魔，
 * 绕过原版上限，让高等级附魔能够继续合并升级。
 *
 * @param event 由 Bukkit 触发的铁砧准备事件
 */
```

- 使用 `##` 分隔章节；
- `@param` / `@return` / `@throws` 标注参数与返回值；
- 行内引用用 `[ClassName]` 或 `[method()]`。

### 11.3 Bukkit 事件监听规范

```kotlin
class XxxListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onXxx(event: XxxEvent) {
        // 1. 先做校验（提前 return）
        if (!Main.configManager.isPluginEnabled) return

        // 2. 取出需要的数据
        val item = event.item ?: return

        // 3. 业务逻辑
        // ...

        // 4. 修改结果
        event.result = newItem
    }
}
```

**要点**：
- 实现 `Listener` 接口（无需继承 `JavaPlugin`）；
- 每个监听方法加 `@EventHandler` 注解，显式声明 `priority`；
- 校验逻辑放最前面，尽早 `return`，避免深层嵌套；
- 修改结果放最后；
- 监听器通过 `Bukkit.getPluginManager().registerEvents(XxxListener(), this)` 注册，无需手动注销（插件卸载时自动注销）。

### 11.4 Folia 兼容规范

- **禁止**使用 `Bukkit.getScheduler()` / `BukkitRunnable`（Folia 不支持）；
- **必须**使用 `Player.scheduler.runDelayed` / `runAtFixedRate`（基于实体区域）或 `regionScheduler` / `globalRegionScheduler`；
- 跨区域操作需通过 `RegionScheduler` 调度；
- 访问实体或方块状态时确保在对应区域线程执行。

---

## 12. 测试策略与方法

本项目当前**无自动化单元测试**，依赖手动测试。以下提供完整的手动测试清单。

### 12.1 测试环境准备

1. 执行 `gradlew.bat runServer` 启动本地测试服；
2. 用创造模式客户端连接 `localhost`；
3. 准备物品：铁砧、附魔书（各种等级）、普通书、武器、装备、经验球。

### 12.2 铁砧合并测试清单

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|----------|
| A1 | 常规同级合并 | 锋利 V 剑 + 锋利 V 书 | 结果锋利 VI（同级 +1） |
| A2 | 常规异级合并 | 锋利 V 剑 + 锋利 III 书 | 结果锋利 V（取较高者） |
| A3 | 单级附魔合并 | 经验修补书 + 经验修补书 | 结果经验修补 I（固定 startLevel） |
| A4 | 黑名单附魔 | 配置 `blacklist: [sharpness]`，锋利 V + 锋利 V | 锋利保留原值，不合并升级 |
| A5 | 超过 40 级合并 | 两本高等级附魔书合并 | 不显示"过于昂贵"，可正常取出 |
| A6 | 昂贵提示 | 触发花费 ≥ 40 的合并 | 收到 MiniMessage 提示消息 |
| A7 | 特殊上限 | 配置 `sharpness: { maximum-level: 5 }`，锋利 V + 锋利 V | 结果锋利 V（被上限覆盖，不超过 5） |
| A8 | 最大花费限制 | 配置 `maximum-level-cost: 30`，触发花费 50 的合并 | 花费被截断为 30 |
| A9 | 跨级合并（简化模式） | 装备锋利 13 + 书锋利 12，`simplify-enchantment: true` | 结果锋利 14（截断同级 +1） |
| A10 | 跨级合并不触发 | 装备锋利 13 + 书锋利 5，`simplify-enchantment: true` | 保留锋利 13 |
| A11 | 补齐独有附魔 | 锋利 V 剑 + 保护 IV 书（剑无保护） | 结果同时有锋利 V 与保护 IV |
| A12 | 插件关闭 | `enabled: false`，任意合并 | 原版行为（40 级上限生效） |

### 12.3 附魔简化合成测试清单

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|----------|
| B1 | 正常复制 | 附魔书（锋利 V）+ 普通书 | 结果为锋利 V 附魔书副本；普通书 -1，附魔书保留 |
| B2 | 多次复制 | 同一附魔书连续合成 5 次 | 每次只消耗 1 普通书，附魔书始终保留 |
| B3 | 普通书不足 | 附魔书 + 0 本普通书 | 无法合成（矩阵非 2 物品） |
| B4 | 光标非空 | 光标持有物品时点击结果 | 合成取消，无物品损失 |
| B5 | 错误配方 | 附魔书 + 可写书（WRITABLE_BOOK） | 不触发简化合成 |
| B6 | 功能关闭 | `simplify-enchantment: false` | 不触发简化合成，原版行为 |
| B7 | 附魔书 NBT 保留 | 复制后检查附魔书与副本 NBT | 完全一致 |

### 12.4 配置重载测试清单

| 编号 | 场景 | 操作 | 预期结果 |
|------|------|------|----------|
| C1 | 重载开关 | 修改 `enabled` 后 `/reload confirm` | 开关立即生效 |
| C2 | 重载黑名单 | 修改 `blacklist` 后重载 | 黑名单立即更新 |
| C3 | 重载特殊上限 | 修改 `special-enchantment-setting` 后重载 | 上限立即更新 |
| C4 | 缺失配置项 | 删除 `maximum-level-cost` 后重启 | 自动补全为默认 -1 |
| C5 | 非法值 | `maximum-level-cost: -5` | 自动回退为 -1 |

### 12.5 边界值测试清单

| 编号 | 场景 | 预期结果 |
|------|------|----------|
| D1 | 合并等级恰好 40 | 正常取出，发送提示 |
| D2 | 合并等级恰好 39 | 正常取出，不发送提示 |
| D3 | `maximum-level-cost: 0` | 花费被截断为 0 |
| D4 | 空附魔书合并 | 原版行为，不报错 |
| D5 | 一侧物品为空 | 监听器提前 return，不报错 |
| D6 | 附魔书 + 装备（非同类型） | 走跨级合并或补齐逻辑 |

### 12.6 测试记录建议

每次发版前应完成上述全部清单，并记录：
- 测试日期、测试人、测试服版本；
- 每个用例的实际结果（通过/失败 + 描述）；
- 失败用例的复现步骤与日志。

---

## 13. 维护注意事项

### 13.1 升级 Folia API 注意事项

1. **版本对应**：`build.gradle.kts` 中 `dev.folia:folia-api:26.1.2.build.+` 的版本号须与目标服务端版本一致。升级服务端时同步更新此处。
2. **API 变更**：关注 PaperMC 的 [Changelog](https://github.com/PaperMC/Paper)，特别注意：
   - `Enchantment` 注册表相关 API（本项目使用 `RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)`）；
   - `PrepareAnvilEvent` / `PrepareItemCraftEvent` 字段变更；
   - `AnvilView.maximumRepairCost` / `repairCost` 属性是否被重命名或废弃。
3. **`api-version`**：升级后同步更新 `plugin.yml` 中的 `api-version`。
4. **run-paper 版本**：`runServer` 的 `minecraftVersion` 须同步更新。
5. **回归测试**：升级后务必执行第 12 章的全部测试清单。

### 13.2 Kotlin 版本升级

1. **版本选择**：当前使用 `2.4.20-Beta1`。升级到稳定版前，确认该版本支持 `jvmToolchain(25)` 与 Folia API 依赖。
2. **stdlib 同步**：`kotlin-stdlib-jdk8` 会随 Kotlin 插件版本自动对齐，无需手动指定版本。
3. **兼容性测试**：升级后执行 `gradlew.bat clean build` 确认编译通过，并运行测试服验证功能。
4. **IDE 同步**：升级 IDEA 的 Kotlin 插件至对应版本。

### 13.3 配置兼容性

1. **向后兼容**：`ConfigManager.initConfig()` 会逐项检查键是否存在，缺失则补写默认值。新增配置项时无需担心旧配置文件报错。
2. **字段重命名**：若需重命名配置项，建议在 `initConfig` 中做迁移（读取旧键 → 写入新键 → 删除旧键），避免用户配置丢失。
3. **默认值变更**：修改默认值后，已存在的配置文件不会自动更新（因为键已存在）。可在版本号变更时通过 `config.set("config-version", x)` 机制触发迁移。
4. **类型变更**：避免改变已有配置项的类型（如 int → string），会导致 `ClassCastException`。

### 13.4 性能注意点

1. **事件监听频率**：
   - `PrepareAnvilEvent` 在玩家每次改动铁砧物品时触发，可能高频；
   - `PrepareItemCraftEvent` 同理；
   - `InventoryClickEvent` 在玩家点击合成台任意槽位时触发，频率更高。
   
   **建议**：监听器中已做提前 `return` 校验，但避免在监听器内做重计算（如大量 NBT 序列化）。当前实现的计算量为 O(附魔数量)，通常 < 10，可接受。

2. **MiniMessage 解析**：`MiniMessage.miniMessage().deserialize()` 每次调用都重新解析字符串。若 `expensive-enchant-message` 频繁触发，可考虑缓存解析结果（当前未做）。

3. **Folia 调度**：`Player.scheduler.runDelayed` 的延迟设为 1 tick，开销极小。但避免在循环中频繁调度。

4. **内存占用**：`specialEnchantments` 与 `blackListEnchantments` 在 `loadConfig` 时构建一次，后续只读，无内存泄漏风险。

5. **并发安全**：
   - `Main.configManager` 为 `lateinit var`，初始化后只读字段访问安全；
   - `ConfigManager` 的 `var` 字段（如 `isPluginEnabled`）在 `reloadConfig` 时可能被另一线程修改，但 Bukkit 事件在主线程/区域线程触发，`reload` 也在主线程执行，实际无竞争；
   - 若未来引入异步重载，需加 `@Volatile` 或锁。

### 13.5 已知问题与改进方向

| 问题 | 影响 | 建议改进 |
|------|------|----------|
| `expensive-enchant-message` 默认值占位符不一致（`initConfig` 用 `<level>`，`loadConfig` 兜底用 `{level}`） | 首次生成的配置中占位符可能不替换 | 统一为 `{level}` |
| `blacklist` / `special-enchantment-setting` 默认值为 `listOf(null)` | YAML 中出现 `null` 占位 | 改为空列表 `listOf()` |
| 未注册重载命令 | 必须用 `/reload confirm` 或重启 | 实现 `unlockenchantment reload` 命令 |
| 无自动化测试 | 每次发版需手动回归 | 引入 JUnit 5 + MockBukkit 编写单元测试 |
| `AnvilListener` 中 `firstItemEnchants!!` 多次非空断言 | 理论上可能 NPE（实际被前置校验保证） | 使用局部变量减少 `!!` |
| 未处理 `InventoryClickEvent` 中 `event.isCancelled` 已被其他插件设置的情况 | 可能与其他插件冲突 | 增加对 `event.isCancelled` 的检查 |

### 13.6 联系与反馈

- **组织**：LuminolCraft
- **包命名空间**：`org.luminolcraft`
- **问题反馈**：通过仓库的 Issue 系统提交，附上服务端版本、插件版本、报错日志、复现步骤。

---

> 本文档基于 UnlockEnchantment 0.1 版本源码编写，最后更新日期：2026-07-08。如发现文档与代码不符，以源码为准。
