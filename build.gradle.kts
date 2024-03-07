import groovy.xml.XmlParser
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PublishPluginTask
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

//import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
//
val kotlinVersion = "1.9.21"
val tomlPlugin = "org.toml.lang"
val psiViewerPlugin: String ="PsiViewer:233.2"

plugins {
    idea
//    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.15.0"
    id("org.jetbrains.grammarkit") version "2022.3.2"
    kotlin("plugin.serialization") version "1.9.21"
    id("org.gradle.test-retry") version "1.5.3"
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
//
////IDEA版本
//
//val ideaVersion = "2023.3.2"
val ideaVersion = "233-EAP-SNAPSHOT"
val ideaType = "IC" // Target IDE Platform
val nativeDebugPlugin: String = "com.intellij.nativeDebug:233.13763.5"
//val nativeDebugPlugin: String = "com.intellij.nativeDebug:233.13135.65"
idea {
    module {
        // https://github.com/gradle/kotlin-dsl/issues/537/
        excludeDirs = excludeDirs + file("testData") + file("deps") + file("bin") +
                file("$grammarKitFakePsiDeps/src/main/kotlin")
    }
}

val isCI = System.getenv("CI") != null
allprojects {
    apply {
        plugin("idea")
        plugin("kotlin")
        plugin("org.jetbrains.grammarkit")
        plugin("org.jetbrains.intellij")
//        plugin("plugin.serialization")
        plugin("org.gradle.test-retry")
    }

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
    intellij {
        version.set(ideaVersion)
        type.set(ideaType)

        downloadSources.set(!isCI)
        updateSinceUntilBuild.set(true)
        instrumentCode.set(false)
        ideaDependencyCachePath.set(dependencyCachePath)
//        sandboxDir.set("$buildDir/$ideaVersion-sandbox")
    }

    sourceSets {
        main {
            java {
                srcDirs("src/gen")
                srcDirs("src/main/kotlin")
            }
        }



    }

    tasks {
//        withType<JavaCompile> {
//            sourceCompatibility = "17"
//            targetCompatibility = "17"
//        }
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "17"
            kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
        }

        withType<PatchPluginXmlTask> {
            sinceBuild.set("223")
            untilBuild.set("233.*")
        }
        runIde { enabled = false }
        prepareSandbox { enabled = false }
        buildSearchableOptions { enabled = false }


        test {
            systemProperty("java.awt.headless", "true")
            testLogging {
                showStandardStreams = prop("showStandardStreams").toBoolean()
                afterSuite(
                    KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
                        if (desc.parent == null) { // will match the outermost suite
                            val output =
                                "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
                            println(output)
                        }
                    })
                )
            }
            if (isCI) {
                retry {
                    maxRetries.set(3)
                    maxFailures.set(5)
                }
            }
        }
//        signPlugin {
//            certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
//            privateKey.set(System.getenv("PRIVATE_KEY"))
//            password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
//        }
//        grammarKit {
//            jflexRelease.set("1.7.0-1")
//            grammarKitRelease.set("2021.1.2")
//            intellijRelease.set("203.7717.81")
//        }
//        publishPlugin {
//            token.set(System.getenv("PUBLISH_TOKEN"))
//        }
    }
    dependencies {
        compileOnly(kotlin("stdlib-jdk8"))
    }
}
val basePluginArchiveName = "intellij-cangjie"

val grammarKitFakePsiDeps = "grammar-kit-fake-psi-deps"

val pluginProjects: List<Project>
    get() = rootProject.allprojects.filter { it.name != grammarKitFakePsiDeps }


//moshi版本
val moshiVersion = "1.15.0"
//okio版本
val okioVersion = "2.10.0"
//toml4j版本
val toml4jVersion = "0.7.3"
//插件需要的依赖列表
val pluginDescriptors = arrayOf(
    "moshi-$moshiVersion.jar",
    "moshi-adapters-${moshiVersion}.jar",
    "moshi-kotlin-${moshiVersion}.jar",
    "okio-jvm-${okioVersion}.jar",
    "toml4j-${toml4jVersion}.jar"
)





project(":plugin") {
    intellij {
        pluginName.set("intellij-cangjie")
        plugins.set(listOf(       psiViewerPlugin))
    }
//    group = "com.huawei.cangjie"
    version = "beta-1.0.7"
    dependencies {
        implementation(project(":"))
//        implementation(project(":inspections"))
//        implementation(project(":highlighter"))
//        implementation(project(":descriptors"))
//        implementation(project(":lsp"))
//        implementation(project(":lsp4j"))

//        api("com.squareup.moshi:moshi-adapters:1.15.0")
//        api("com.squareup.moshi:moshi-kotlin:1.15.0")
//        implementation(project(":debugger"))
//        implementation(project(":debugger1"))


    }


    // Collects all jars produced by compilation of project modules and merges them into singe one.
    // We need to put all plugin manifest files into single jar to make new plugin mode        l work
    val mergePluginJarTask = task<Jar>("mergePluginJars") {
        dependsOn
        duplicatesStrategy = DuplicatesStrategy.FAIL
        archiveBaseName.set(basePluginArchiveName)

        exclude("META-INF/MANIFEST.MF")
        exclude("**/classpath.index")

        val pluginLibDir by lazy {
            val sandboxTask = tasks.prepareSandbox.get()
            sandboxTask.destinationDir.resolve("${sandboxTask.pluginName.get()}/lib")
        }

        val pluginJars by lazy {
            pluginLibDir.listFiles().orEmpty().filter {


                it.isPluginJar()
            }


        }

        destinationDirectory.set(project.layout.dir(provider { pluginLibDir }))

        doFirst {
            for (file in pluginJars) {
                from(zipTree(file))
            }

        }

        doLast {

            delete(pluginJars)
        }


    }
//    val createSourceJar = task<Jar>("createSourceJar") {
//        duplicatesStrategy = DuplicatesStrategy.WARN
//        for (prj in pluginProjects) {
//            from(prj.kotlin.sourceSets.main.get().kotlin) {
//                include("**/*.java")
//                include("**/*.kt")
//            }
//        }
//        destinationDirectory.set(layout.buildDirectory.dir("libs"))
//        archiveBaseName.set(basePluginArchiveName)
//        archiveClassifier.set("src")
//    }
    tasks {
        buildPlugin {
//            dependsOn(createSourceJar)
//            from(createSourceJar) { into("lib/src") }
            // Set proper name for final plugin zip.
            // Otherwise, base name is the same as gradle module name
            archiveBaseName.set(basePluginArchiveName)
        }
        runIde { enabled = true }
        prepareSandbox {
            finalizedBy(mergePluginJarTask)
            enabled = true
        }
        buildSearchableOptions {
            // Force `mergePluginJarTask` be executed before `buildSearchableOptions`
            // Otherwise, `buildSearchableOptions` task can't load the plugin and searchable options are not built.
            // Should be dropped when jar merging is implemented in `gradle-intellij-plugin` itself
            dependsOn(mergePluginJarTask)
            enabled = prop("enableBuildSearchableOptions").toBoolean()
        }
//        withType<PrepareSandboxTask> {
////            dependsOn(named(compileNativeCodeTaskName))
//
//            // Copy native binaries
//            from("${rootDir}/bin") {
//                into("${pluginName.get()}/bin")
//                include("**")
//            }
//            // Copy pretty printers
//            from("$rootDir/prettyPrinters") {
//                into("${pluginName.get()}/prettyPrinters")
//                include("**/*.py")
//            }
//        }
        withType<RunIdeTask> {
            dependsOn(mergePluginJarTask)
            // Default args for IDEA installation
            jvmArgs("-Xmx768m", "-XX:+UseG1GC", "-XX:SoftRefLRUPolicyMSPerMB=50")
            // Disable plugin auto reloading. See `com.intellij.ide.plugins.DynamicPluginVfsListener`
            jvmArgs("-Didea.auto.reload.plugins=false")
            // Don't show "Tip of the Day" at startup
            jvmArgs("-Dide.show.tips.on.startup.default.value=false")
            // uncomment if `unexpected exception ProcessCanceledException` prevents you from debugging a running IDE
            // jvmArgs("-Didea.ProcessCanceledException=disabled")

            // Uncomment to enable FUS testing mode
            // jvmArgs("-Dfus.internal.test.mode=true")

            // Uncomment to enable localization testing mode
            // jvmArgs("-Didea.l10n=true")
        }

        withType<PatchPluginXmlTask> {
            pluginDescription.set(provider { file("description.html").readText() })
        }

        withType<PublishPluginTask> {
            token.set(token)
            channels.set(listOf("dev"))
        }
    }

    task<RunIdeTask>("buildEventsScheme") {
        dependsOn(tasks.prepareSandbox)
        args(
            "buildEventsScheme",
            "--outputFile=${buildDir.resolve("eventScheme.json").absolutePath}",
            "--pluginId=com.huawei.cangjie"
        )
        // BACKCOMPAT: 2023.1. Update value to 232 and this comment
        // `IDEA_BUILD_NUMBER` variable is used by `buildEventsScheme` task to write `buildNumber` to output json.
        // It will be used by TeamCity automation to set minimal IDE version for new events
//        environment("IDEA_BUILD_NUMBER", "231")
    }
}

project(":") {
    intellij {
        plugins.set(listOf(tomlPlugin))
    }
    dependencies {
//        implementation("com.alibaba:fastjson:2.0.46")
        implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.22.0")


//        implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.debug:0.22.0")
        implementation("com.squareup.moshi:moshi-adapters:${moshiVersion}")
        implementation("com.squareup.moshi:moshi-kotlin:${moshiVersion}")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
//        implementation("org.jetbrains.kotlinx:kotlinx-serialization-toml:1.6.2")
//        implementation("com.akuleshov7:ktoml-file:0.5.1")
        // https://mvnrepository.com/artifact/com.akuleshov7/ktoml-core
//        implementation("com.akuleshov7:ktoml-core:0.5.1")
        implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
//        implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.15.2"){
//            exclude(group = "com.fasterxml.jackson.core", module = "jackson-core")
//            exclude(group = "com.fasterxml.jackson.core", module = "jackson-databind")
//
//            exclude(group = "com.fasterxml.jackson.core", module = "jackson-annotations")
//        }
        implementation("io.hotmoka:toml4j:0.7.3")
//        implementation(project(":dap"))
        implementation(project(":lsp"))

        implementation("org.antlr:antlr4-intellij-adaptor:0.1")
    }
    tasks {
        processTestResources {
//            dependsOn(named(compileNativeCodeTaskName))
            from("${rootDir}/bin") {
                into("bin")
                include("**")
            }
        }
    }
    task("resolveDependencies") {
        doLast {
            rootProject.allprojects
                .map { it.configurations }
                .flatMap { it.filter { c -> c.isCanBeResolved } }
                .forEach { it.resolve() }
        }
    }
}
//project(":highlighter"){
//    dependencies{
//        implementation(project(":"))
//        implementation(project(":descriptors"))
//
//    }
//}
//project(":inspections"){
//    dependencies{
//        implementation(project(":"))
//    }
//}
//project(":descriptors"){
//    dependencies{
//        implementation(project(":"))
//
//    }
//}
//
//project(":dap"){
//
//
//    dependencies{
//        implementation("com.squareup.moshi:moshi-adapters:${moshiVersion}")
//        implementation("com.squareup.moshi:moshi-kotlin:${moshiVersion}")
//        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
//        implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
//    }
//}
project(":lsp"){
    dependencies{
        implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.22.0")

//        implementation(project(":"))
    }
}



project(":grammar"){
    apply {
        plugin("antlr")
    }

    // Kotlin Gradle support doesn't generate proper extensions if the plugin is not declared in `plugin` block.
// But if we do it, `antlr` plugin will be applied to root project as well that we want to avoid.
// So, let's define all necessary things manually
    val antlr by configurations

    dependencies{

        antlr("org.antlr:antlr4:4.13.1")
        implementation("org.antlr:antlr4-runtime:4.13.1")
    }
}

//project(":lsp4j") {
//
//}
//project(":back") {
//
//}


//project(":debugger") {
//    intellij {
//        plugins.set(listOf(nativeDebugPlugin))
//    }
//    dependencies {
//        implementation(project(":"))
//    }
//}

//project(":cidr") {
//    dependencies {
//        implementation(project(":"))
//
////        implementation("com.squareup.moshi:moshi-adapters:1.15.0")
////        implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
//    }
//}
//project(":debugger1") {
//    intellij {
////        plugins.set(listOf(nativeDebugPlugin))
//    }
//    dependencies {
//        implementation(project(":"))
//        implementation(project(":cidr"))
//    }
//}


fun File.isPluginJar(): Boolean {
    if ("buildPlugin" in gradle.startParameter.taskNames) {
        if (pluginDescriptors.contains(name)) {
            return true
        }
    }



    if (!isFile) return false
    if (extension != "jar") return false
    return zipTree(this).files.any { it.isManifestFile() }
}

fun File.isManifestFile(): Boolean {
    if (extension != "xml") return false
    val rootNode = try {
        val parser = XmlParser()
        parser.parse(this)
    } catch (e: Exception) {
        logger.error("Failed to parse $path", e)
        return false
    }
    return rootNode.name() == "idea-plugin"
}


fun <T : ModuleDependency> T.excludeKotlinDeps() {
    exclude(module = "kotlin-reflect")
    exclude(module = "kotlin-runtime")
    exclude(module = "kotlin-stdlib")
    exclude(module = "kotlin-stdlib-common")
    exclude(module = "kotlin-stdlib-jdk8")
    exclude(module = "kotlinx-serialization-core")
}

fun prop(name: String): String =
    extra.properties[name] as? String
        ?: error("Property `$name` is not defined in gradle.properties")
