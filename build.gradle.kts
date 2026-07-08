/*
 * ============================================================================
 *  UnlockEnchantment 构建脚本 (Gradle Kotlin DSL)
 * ============================================================================
 *  本文件是项目的构建配置入口，使用 Kotlin DSL 语法编写（即用 Kotlin 代码
 *  代替传统的 Groovy build.gradle）。主要职责：
 *    1. 声明并应用所需插件（Kotlin 编译、Shadow 打包、run-paper 测试服）
 *    2. 配置依赖仓库地址与第三方库
 *    3. 定义构建任务（打 fat jar、本地启动测试服、资源占位符替换）
 *  新人阅读顺序建议：plugins → repositories → dependencies → kotlin → tasks
 * ============================================================================
 */

plugins {
    // Kotlin JVM 插件：启用 Kotlin 编译器，把 .kt 源码编译成 .class 字节码
    // 版本 2.4.20-Beta1 是当前项目所用的 Kotlin 编译器版本
    kotlin("jvm") version "2.4.20-Beta1"

    // Shadow 插件 (com.gradleup.shadow)：把 Kotlin 运行时库一起打包进最终 jar（即 fat jar）
    // 好处：服务端无需单独安装 Kotlin，丢一个 jar 即可运行
    id("com.gradleup.shadow") version "9.4.3"

    // run-paper 插件 (xyz.jpenilla.run-paper)：提供 runServer 任务
    // 作用：一键下载并本地启动 Minecraft 服务端用于插件测试，省去手动准备环境
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    // Maven 中央仓库：存放绝大多数开源 Java/Kotlin 库（如 kotlin-stdlib）
    mavenCentral()

    // PaperMC 官方仓库：Folia / Paper 服务端 API 的来源
    // 上方的 folia-api 依赖即从此处拉取
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // compileOnly：仅编译期依赖，不会被打包进最终 jar
    // folia-api 由服务端自身提供，插件运行时直接复用服务端的类，故无需打包
    compileOnly("dev.folia:folia-api:26.1.2.build.+")

    // implementation：运行时依赖，会被 Shadow 插件打包进 fat jar
    // kotlin-stdlib-jdk8 是 Kotlin 标准库（兼容 JDK8+），服务端没有，必须随插件一起分发
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

kotlin {
    // 指定使用 JDK 25 工具链进行编译
    // 即使本机安装的是其他版本的 JDK，Gradle 也会自动下载并使用 JDK 25
    jvmToolchain(25)
}

tasks {
    // build 任务依赖 shadowJar：执行 build 时会自动产出 fat jar（含 Kotlin 运行时）
    build {
        dependsOn(shadowJar)
    }

    // runServer 任务：本地启动 Minecraft 26.1.2 测试服来调试插件
    // 插件的 jar（若存在 shadowJar 则优先使用之）会自动注入服务端的 plugins 目录
    runServer {
        minecraftVersion("26.1.2")  // 测试服对应的 Minecraft / Folia 版本
        jvmArgs("-Xms2G", "-Xmx2G") // JVM 初始/最大堆内存均设为 2G，避免测试时 OOM
    }

    // processResources 任务：处理 src/main/resources 下的资源文件
    // 这里把 plugin.yml 中的 ${version} 占位符替换为项目的实际版本号
    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
