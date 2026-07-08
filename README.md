# UnlockEnchantment

> Minecraft Paper/Folia 服务端插件 · 解除原版铁砧附魔等级上限并提供附魔简化合成
>
> Minecraft Paper/Folia server plugin · Unlocks the vanilla anvil enchantment level cap and provides enchantment simplification crafting.

## 简介 / Overview

UnlockEnchantment 是一个用 Kotlin 编写的 Minecraft Paper/Folia 插件，核心功能包括：

- **解除铁砧附魔上限**：突破原版 40 级"过于昂贵"限制，支持高级附魔合并
- **附魔简化合成**：附魔书 + 普通书 → 附魔书副本（仅消耗普通书），便于低成本复制
- **灵活配置**：黑名单、特殊附魔上限、最大花费、提示消息均可配置

UnlockEnchantment is a Minecraft Paper/Folia plugin written in Kotlin. Core features:

- **Unlock anvil enchantment cap**: breaks the vanilla "Too Expensive" 40-level limit, supports advanced enchantment merging
- **Enchantment simplification crafting**: enchanted book + normal book → enchanted book copy (consumes only the normal book), for low-cost duplication
- **Flexible configuration**: blacklist, special enchantment caps, max cost, and notice messages are all configurable

## 文档 / Documentation

完整的企业级文档提供中英双语，请选择您偏好的语言阅读：

Full enterprise-level documentation is available in both Chinese and English — please choose your preferred language:

- **中文文档**：[docs/README.zh-CN.md](docs/README.zh-CN.md)
- **English Documentation**: [docs/README.en-US.md](docs/README.en-US.md)

两份文档内容完全对齐，涵盖：项目背景与目标、环境配置指南、技术栈详解、目录结构说明、核心功能模块介绍、配置项参考文档、API / 事件接口文档、常见问题解决方案、部署流程、版本控制规范、代码风格指南、测试策略与方法、维护注意事项。

Both documents are fully aligned and cover: Project Background & Goals, Environment Setup, Tech Stack, Directory Structure, Core Modules, Configuration Reference, API & Event Reference, FAQ, Deployment, Version Control Conventions, Code Style Guide, Testing Strategy, and Maintenance Notes.

## 快速开始 / Quick Start

```bash
# 构建 / Build
./gradlew build

# 产物 / Output
# build/libs/UnlockEnchantment-0.1-all.jar

# 本地测试服 / Local test server
./gradlew runServer
```

将构建产物复制到服务端 `plugins/` 目录并重启即可。首次启动会自动生成 `plugins/UnlockEnchantment/config.yml`。

Copy the build output to your server's `plugins/` directory and restart. The first start auto-generates `plugins/UnlockEnchantment/config.yml`.

## 技术栈 / Tech Stack

| 组件 / Component | 版本 / Version | 用途 / Purpose |
|------------------|----------------|----------------|
| Kotlin | 2.4.20-Beta1 | 主开发语言 / Primary language |
| Folia API | 26.1.2 | 服务端 API（兼容 Paper）/ Server API (Paper compatible) |
| Gradle | 9.6.0 | 构建工具 / Build tool |
| Shadow | 9.4.3 | 打包 fat jar（含 Kotlin 运行时）/ Fat jar packaging (includes Kotlin runtime) |
| run-paper | 3.0.2 | 本地测试服 / Local test server |

## 许可证 / License

详见项目根目录许可证文件 / See the license file in the project root.
