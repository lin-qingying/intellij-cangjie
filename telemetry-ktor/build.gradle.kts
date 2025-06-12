val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "3.1.3"
    kotlin("plugin.serialization") version "2.1.10"
}

group = "cn.cangnova"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
    
    // 为测试数据生成添加第二个主类
    // 可以通过以下命令运行测试数据生成器：
    // ./gradlew run --args="--generate-test-data [options]"
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor核心依赖
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-swagger")
    implementation("io.ktor:ktor-server-openapi")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    
    // 添加FreeMarker模板引擎支持
    implementation("io.ktor:ktor-server-freemarker")
    implementation("io.ktor:ktor-server-html-builder")
    
    // 添加会话支持
    implementation("io.ktor:ktor-server-sessions")
    
    // 添加静态资源支持
    implementation("io.ktor:ktor-server-resources")
    implementation("io.ktor:ktor-server-host-common")
    implementation("io.ktor:ktor-server-auto-head-response")

    // Kotlin反射和序列化
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.10.1")

    // JWT依赖
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("com.auth0:jwks-rsa:0.22.1")

    // 配置库
    implementation("com.typesafe:config:1.4.2")

    // MongoDB依赖
    implementation("org.mongodb:mongodb-driver-kotlin-sync:4.10.1")
    implementation("org.litote.kmongo:kmongo:4.9.0")
    implementation("org.litote.kmongo:kmongo-coroutine:4.9.0")
    
    // MySQL和数据库连接依赖
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.zaxxer:HikariCP:5.0.1")
    
    // Exposed SQL库依赖
    implementation("org.jetbrains.exposed:exposed-core:0.46.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.46.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.46.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.46.0")
    
    // 图表库
    implementation("org.knowm.xchart:xchart:3.8.6")
    
    // 密码哈希库
    implementation("org.mindrot:jbcrypt:0.4")
    
    // Kotlin协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
    
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

// 添加测试数据生成任务
tasks.register<JavaExec>("generateTestData") {
    group = "application"
    description = "生成遥测测试数据"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "cn.cangnova.testdata.TestDataApplication"
    
    // 将命令行参数传递给应用程序
    if (project.hasProperty("appArgs")) {
        args = (project.property("appArgs") as String).split(" ")
    }
}
