import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion = "1.9.21"

plugins {
    idea
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
//    kotlin("jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.15.0"
    id("org.jetbrains.grammarkit") version "2022.3.2"
    kotlin("plugin.serialization") version "1.9.21"

}
sourceSets {
    main {
        java {
            srcDirs("src/main/kotlin", "src/main/gen")
        }
        kotlin {
            srcDirs("testData/src/main/kotlin")
        }
    }
}

group = "com.huawei.cangjie"
version = "beta-1.0.3 linux"
val grammarKitFakePsiDeps = "grammar-kit-fake-psi-deps"

repositories {
    maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
    mavenCentral()
    maven {
        setUrl("https://jitpack.io")
//        credentials { username authToken }
//        credentials {
//
//        }
    }
}

val Project.dependencyCachePath
    get(): String {
        val cachePath = file("${rootProject.projectDir}/deps")
        // If cache path doesn't exist, we need to create it manually
        // because otherwise gradle-intellij-plugin will ignore it
        if (!cachePath.exists()) {
            cachePath.mkdirs()
        }
        return cachePath.absolutePath
    }

//IDEA版本

val nativeDebugPlugin: String by project
val ideaVersion = "233-EAP-SNAPSHOT"
val ideaType = "IC" // Target IDE Platform

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {

//    version.set("IU-2023.1")

//    updateSinceUntilBuild.set(true)
//    instrumentCode.set(false)
//    ideaDependencyCachePath.set(dependencyCachePath)


    version.set(ideaVersion)
    type.set(ideaType) // Target IDE Platform
////加载PsiViewer插件 grammar-kit
    plugins.set(
        listOf(
//"com.intellij.nativeDebug:232.8660.142"
        )
    )


}

kotlin {
    jvmToolchain(17)
    target {

        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
                freeCompilerArgs = listOf("-Xjvm-default=all")
            }

        }
    }
}


//val intellijVersion = "232.*"
dependencies {
//    api("com.jetbrains.intellij.java:java-psi-impl:$intellijVersion") { isTransitive = true }
//    api("com.jetbrains.intellij.java:java-psi:$intellijVersion") { isTransitive = true }


//    implementation(kotlin("stdlib"))
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.21.1")



    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.debug:0.21.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("com.squareup.moshi:moshi-adapters:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
}


//idea {
//    module {
//        // https://github.com/gradle/kotlin-dsl/issues/537/
//        excludeDirs = excludeDirs + file("testData") + file("deps") + file("bin") +
//                file("$grammarKitFakePsiDeps/src/main/kotlin")
//    }
//}
tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
        kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
    }

    patchPluginXml {
        sinceBuild.set("223")
        untilBuild.set("233.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }
    grammarKit {
        jflexRelease.set("1.7.0-1")
        grammarKitRelease.set("2021.1.2")
        intellijRelease.set("203.7717.81")
    }
    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }


}


