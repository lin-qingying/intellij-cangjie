pluginManagement {

    repositories {

        mavenCentral()


        gradlePluginPortal()

    }
}





rootProject.name = "intellij-cangjie"
//include("plugin")
// Configure Gradle Build Cache. It is enabled in `gradle.properties` via `org.gradle.caching`.
//buildCache {
//    local {
//        isEnabled = System.getenv("CI") == null
//        directory = File(rootDir, "build/build-cache")
//        removeUnusedEntriesAfterDays = 30
//    }
//}

//pluginManagement {
//    repositories {
//        maven("https://oss.sonatype.org/content/repositories/snapshots/")
//        gradlePluginPortal()
//    }
//}
