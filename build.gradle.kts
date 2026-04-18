import org.gradle.api.GradleException

group = "org.cangnova.cangjie"
version = providers.gradleProperty("pluginVersion")
    .zip(providers.gradleProperty("versionSuffix").orElse("")) { pluginVersion, versionSuffix ->
        "$pluginVersion$versionSuffix"
    }
    .get()

subprojects {
    group = rootProject.group
    version = rootProject.version
}




