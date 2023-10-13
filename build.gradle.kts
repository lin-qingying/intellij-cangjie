

//psiviewer插件



plugins {
    idea
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.15.0"
    id("org.jetbrains.grammarkit") version "2022.3.2"


}
sourceSets {
    main {
        java {
            srcDirs("src/main/gen")
        }
    }
}

group = "com.huawei.cangjie"
version = "1.0-SNAPSHOT"
val grammarKitFakePsiDeps = "grammar-kit-fake-psi-deps"
repositories {
    mavenCentral()
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


// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {

//    version.set("IU-2023.1")

//    updateSinceUntilBuild.set(true)
//    instrumentCode.set(false)
//    ideaDependencyCachePath.set(dependencyCachePath)



    version.set("2022.2.5")
    type.set("IC") // Target IDE Platform
////加载PsiViewer插件 grammar-kit
    plugins.set(listOf(


    ))


}





idea {
    module {
        // https://github.com/gradle/kotlin-dsl/issues/537/
        excludeDirs = excludeDirs + file("testData") + file("deps") + file("bin") +
                file("$grammarKitFakePsiDeps/src/main/kotlin")
    }
}
tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("232.*")
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
