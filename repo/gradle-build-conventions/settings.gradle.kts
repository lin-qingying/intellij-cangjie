
pluginManagement {
//    apply(from = "../scripts/cache-redirector.settings.gradle.kts")
//    apply(from = "../scripts/kotlin-bootstrap.settings.gradle.kts")
//
//    includeBuild("../gradle-settings-conventions")

    repositories {
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}


include(":buildsrc-compat")
