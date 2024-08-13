import Build_gradle.BuildType.*
import groovy.xml.XmlParser
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.intellij.tasks.PublishPluginTask
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS

val build_type: String by project

enum class BuildType {


    //   IDEA Ultimate Edition + nativeDebug本地调试插件  LLDB
    IU_NATIVE_DEBUG,

    // IDEA Community Edition + DAP调试插件
    IC_DAP,

    //    IDEA Community Edition + cidr本地代码调试   LLDB
    IC_CIDR_NATIVE_DEBUG,


    //    CLION
    CLION_NATIVE_DEBUG,
    CLION_DAP;

    companion object {

        fun fromString(str: String): BuildType {
            return when (str) {
                "IU_NATIVE_DEBUG" -> IU_NATIVE_DEBUG
                "IC_DAP" -> IC_DAP
                "IC_CIDR_NATIVE_DEBUG" -> IC_CIDR_NATIVE_DEBUG

                "CLION_NATIVE_DEBUG" -> CLION_NATIVE_DEBUG

                "CLION_DAP" -> CLION_DAP

                else -> IC_DAP
            }
        }
    }
}


//构建方式
val buildType = BuildType.fromString(build_type)


val kotlinVersion = "1.9.21"
val tomlPlugin = "org.toml.lang"
val terminalPlugin = "org.jetbrains.plugins.terminal"


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
val pluginDescriptors = arrayOf<String>(
//    "moshi-$moshiVersion.jar",
//    "moshi-adapters-${moshiVersion}.jar",
//    "moshi-kotlin-${moshiVersion}.jar",
//    "okio-jvm-${okioVersion}.jar",
//    "toml4j-${toml4jVersion}.jar",
//    "utils.jar"
)


/********************属性**************************/
val platformVersion = prop("platformVersion").toInt()

//val sinceBuild = prop("sinceBuild")
//val untilBuild = prop("untilBuild")
val psiViewerPlugin: String = prop("psiViewerPlugin")
val basePluginArchiveName = prop("basePluginArchiveName")
//插件版本
val cangjiePluginVersion = prop("cangjiePluginVersion")
val nativeDebugPlugin: String = prop("nativeDebugPlugin")
val baseIDE = prop("baseIDE")
val ideToRun = prop("ideToRun").ifEmpty { baseIDE }

val ideaVersion = prop("ideaVersion")
val clionVersion = prop("clionVersion")
val compileNativeCodeTaskName = "compileNativeCode"
val baseVersion = versionForIde(baseIDE)
val baseVersionForRun = versionForIde(ideToRun)

/************************************************/


plugins {
    idea
//    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.15.0"
    id("org.jetbrains.grammarkit") version "2022.3.2"
    kotlin("plugin.serialization") version "1.9.21" apply false
    id("org.gradle.test-retry") version "1.5.3"
    id("net.saliman.properties") version "1.5.2"

//    id("antlr")

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
        version.set(baseVersion)


//        val ideaType = // Target IDE Platform
//            when (buildType) {
//                IU_NATIVE_DEBUG -> "IU"
//                IC_DAP, IC_CIDR_NATIVE_DEBUG -> "IC"
//                CLION_NATIVE_DEBUG, CLION_DAP -> "CL"
//
//            }
//        type.set(ideaType)
        downloadSources.set(!isCI)
        updateSinceUntilBuild.set(true)
        instrumentCode.set(false)
        ideaDependencyCachePath.set(dependencyCachePath)
        sandboxDir.set(layout.buildDirectory.dir("$ideToRun-sandbox-$platformVersion").map { it.asFile.absolutePath })

    }

    sourceSets {


        test {
            kotlin.srcDirs("src/$platformVersion/test/kotlin")

            resources.srcDirs("src/$platformVersion/test/resources")
        }

        main {
            kotlin.srcDirs("src/$platformVersion/main/kotlin")


            resources.srcDirs("src/$platformVersion/main/resources")
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
            sinceBuild.set(prop("sinceBuild"))
            untilBuild.set(prop("untilBuild"))
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

//         if (project.name in listOf("intellij-cangjie", "plugin")) {
//            task<Exec>(compileNativeCodeTaskName) {
//                workingDir = rootDir.resolve("native-helper")
//                executable = "cargo"
//
//                val hostPlatform = DefaultNativePlatform.host()
//                val archName = when (val archName = hostPlatform.architecture.name) {
//                    "arm-v8", "aarch64" -> "arm64"
//                    else -> archName
//                }
//                val outDir = "${rootDir}/bin/${hostPlatform.operatingSystem.toFamilyName()}/$archName"
//                args("build", "--release", "-Z", "unstable-options", "--out-dir", outDir)
//
//                // It may be useful to disable compilation of native code.
//                // For example, CI builds native code for each platform in separate tasks and puts it into `bin` dir manually
//                // so there is no need to do it again.
//                enabled = prop("compileNativeCode").toBoolean()
//            }
//        }
    }
    dependencies {
        compileOnly(kotlin("stdlib-jdk8"))
//        implementation(project(":utils"))

    }
}


val cangjie_plugin_project = project(":plugin") {
    intellij {
        pluginName.set("intellij-cangjie")
        plugins.set(
            listOf(
//                psiViewerPlugin,
                terminalPlugin, tomlPlugin,
//                "com.intellij.cidr.base", "com.intellij.clion"
//"com.intellij.java"
            )
        )
        version.set(baseVersionForRun)

    }
    group = "com.linqingying.cangjie"
//    version = cangjiePluginVersion


    val pluginVersion = System.getenv("BUILD_NUMBER") ?: "${prop("cangjiePluginVersion")}.${platformVersion}"
    version = pluginVersion
//    version =  if (pluginVersion.contains(".")) {
//        val split = pluginVersion.split(".").toMutableList()
//        split[0] = platformVersion.toString()
//
//        split[1] = (split[1].toIntOrNull()?.plus(10000))?.toString() ?: split[1]
//        split.joinToString(".")
//    } else {
//        pluginVersion
//    }
    dependencies {
        implementation(project(":"))

        implementation(project(":idea"))
        implementation(project(":clion"))
        implementation(project(":dap-debugger"))

//        implementation(project(":inspections"))
//        implementation(project(":highlighter"))
//        implementation(project(":descriptors"))
//        implementation(project(":lsp"))
//        implementation(project(":lsp4j"))
//        implementation(project(":dap"))
//        api("com.squareup.moshi:moshi-adapters:1.15.0")
//        api("com.squareup.moshi:moshi-kotlin:1.15.0")
//        implementation(project(":native-debugger"))
//        implementation(project(":debugger1"))
    }

    // Collects all jars produced by compilation of project modules and merges them into singe one.
    // We need to put all plugin manifest files into single jar to make new plugin model work
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
        withType<PrepareSandboxTask> {
//            dependsOn(named(compileNativeCodeTaskName))

            // Copy native binaries
//            from("${rootDir}/bin") {
//                into("${pluginName.get()}/bin")
//                include("**")
//            }
            // Copy shell
//            from("$rootDir/shell-integrations") {
//                into("${pluginName.get()}/shell-integrations")
//                include("**")
//            }
        }
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
        verifyPlugin {
            dependsOn(mergePluginJarTask)


        }
        runPluginVerifier {
            dependsOn(mergePluginJarTask)


        }


//        withType<RunPluginVerifierTask> {
//
//            dependsOn(mergePluginJarTask)
//            mustRunAfter(mergePluginJarTask)
////            distributionFile.set(
////                this@project.projectDir.resolve("build").resolve("idea-sandbox").resolve("plugins").resolve("lib")
////                    .resolve("$basePluginArchiveName-$cangjiePluginVersion.jar")
////            )
//        }

    }

    task<RunIdeTask>("buildEventsScheme") {
        dependsOn(tasks.prepareSandbox)
        args(
            "buildEventsScheme",
            "--outputFile=${buildDir.resolve("eventScheme.json").absolutePath}",
            "--pluginId=linqingying.cangjie"
        )
        // BACKCOMPAT: 2023.1. Update value to 232 and this comment
        // `IDEA_BUILD_NUMBER` variable is used by `buildEventsScheme` task to write `buildNumber` to output json.
        // It will be used by TeamCity automation to set minimal IDE version for new events
//        environment("IDEA_BUILD_NUMBER", "231")
    }
}


val cangjie_src_project = project(":") {

    intellij {
        plugins.set(
            listOf(
                tomlPlugin,
                terminalPlugin,
//                "com.intellij.cidr.base", "com.intellij.clion"
//            "com.intellij.java"
            )
        )
    }
    dependencies {
        implementation("com.squareup.moshi:moshi-adapters:${moshiVersion}")
        implementation("com.squareup.moshi:moshi-kotlin:${moshiVersion}")
        implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")


        implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.22.0")

        implementation("io.hotmoka:toml4j:0.7.3")
//        implementation(project(":dap"))
        implementation(project(":lsp"))

        implementation(project(":utils"))
//
//        antlr("org.antlr:antlr4:4.13.1") { // use ANTLR version 4
//
////            exclude(group = "com.ibm.icu", module = "icu4j")
//        }
//        implementation("org.antlr:antlr4-intellij-adaptor:0.1")
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


project(":idea") {
    intellij {
        version.set(ideaVersion)

    }
    dependencies {
        implementation(project(":"))

    }
}

project(":clion") {
    intellij {
        version.set(clionVersion)

//        plugins.set(listOf("com.intellij.clion"))
    }
    dependencies {
        implementation(project(":"))

    }
}

project(":lsp") {
    dependencies {
        implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.22.0")
        implementation(project(":utils"))


    }
}


project(":grammar") {
    apply {
//        plugin("antlr")
    }

    // Kotlin Gradle support doesn't generate proper extensions if the plugin is not declared in `plugin` block.
// But if we do it, `antlr` plugin will be applied to root project as well that we want to avoid.
// So, let's define all necessary things manually
//    val antlr by configurations
//
//    dependencies{
//
//        antlr("org.antlr:antlr4:4.13.1")
//        implementation("org.antlr:antlr4-runtime:4.13.1")
//    }
}



project(":utils") {
    dependencies {
// https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
        implementation("org.apache.commons:commons-lang3:3.15.0")

//        implementation("org.yaml:snakeyaml:2.2")
    }
}


//when (buildType) {
//    IU_NATIVE_DEBUG -> {
//        project(":native-debugger") {
//            intellij {
//                plugins.set(listOf(nativeDebugPlugin))
//            }
//            dependencies {
//                implementation(project(":"))
//            }
//        }
//
//        cangjie_plugin_project.intellij.plugins.add(nativeDebugPlugin)
//        cangjie_plugin_project.dependencies {
//            implementation(project(":native-debugger"))
//
//        }
//
//
//    }
//
//    CLION_NATIVE_DEBUG -> {
//        val clionPlugins = listOf("com.intellij.cidr.base", "com.intellij.clion", "com.intellij.nativeDebug")
//        project(":native-debugger") {
//            intellij {
//                plugins.set(clionPlugins)
//            }
//            dependencies {
//                implementation(project(":"))
//            }
//        }
//
//        cangjie_plugin_project.intellij.plugins.add(nativeDebugPlugin)
//        cangjie_plugin_project.dependencies {
//            implementation(project(":native-debugger"))
//
//        }
//    }
//
//    IC_DAP, CLION_DAP -> {
project(":dap-debugger") {
    intellij {
        plugins.set(listOf(terminalPlugin))
    }
    apply {
        plugin("org.jetbrains.kotlin.plugin.serialization")
    }
    dependencies {
        implementation(project(":"))
        implementation("com.squareup.moshi:moshi-adapters:${moshiVersion}")
        implementation("com.squareup.moshi:moshi-kotlin:${moshiVersion}")
        implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    }
}

//cangjie_plugin_project.intellij.plugins.add(terminalPlugin)
//cangjie_plugin_project.dependencies {
//    implementation(project(":dap-debugger"))
//
//}
//
//    }
//
//    IC_CIDR_NATIVE_DEBUG -> TODO()
//
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

fun versionForIde(ideName: String): String = when (ideName) {
    "idea" -> ideaVersion
    "clion" -> clionVersion

    else -> error("Unexpected IDE name: `$baseIDE`")
}
afterEvaluate {
    updatePluginXmlFile()
}

/**
 * 修改plugin.xml文件
 */
fun updatePluginXmlFile() {
    // Instantiate the Factory
    val dbf = DocumentBuilderFactory.newInstance()
    try {

        val pluginPath = this.cangjie_plugin_project.projectDir

        val pluginXmlFile = pluginPath.resolve("src/main/resources/META-INF/plugin.xml")
        val xmlDoc = dbf.newDocumentBuilder().parse(pluginXmlFile)
        xmlDoc.documentElement.normalize()

        val content = xmlDoc.getElementsByTagName("content").item(0) as Element
        val moduleList = content.getElementsByTagName("module")


//        需要删除节点属性的值
        val attsStrs = listOf(

            "com.linqingying.cangjie.nativeDebug",
            "com.linqingying.cangjie.debugger",
            "com.linqingying.cangjie.dapDebugger"
        )


// 需要删除的节点
        val removeNodeList = mutableListOf<Element>()
        for (i in 0 until moduleList.length) {
            val module = moduleList.item(i) as Element
            val attrName = module.getAttribute("name")
            if (attsStrs.contains(attrName)) {
                removeNodeList.add(module)
            }
        }
        removeNodeList.forEach {
            content.removeChild(it)
        }

        when (buildType) {
            IU_NATIVE_DEBUG, CLION_NATIVE_DEBUG -> {
                var node = xmlDoc.createElement("module")
                node.setAttributeNode(xmlDoc.createAttribute("name")?.apply {
                    nodeValue = "com.linqingying.cangjie.nativeDebug"
                })
                content.appendChild(node)

                node = xmlDoc.createElement("module")
                node.setAttributeNode(xmlDoc.createAttribute("name")?.apply {
                    nodeValue = "com.linqingying.cangjie.debugger"
                })

                content.appendChild(node)

            }

            IC_DAP, CLION_DAP -> {

                val node = xmlDoc.createElement("module")
                node.setAttributeNode(xmlDoc.createAttribute("name")?.apply {
                    nodeValue = "com.linqingying.cangjie.dapDebugger"
                })
                content.appendChild(node)
            }

            IC_CIDR_NATIVE_DEBUG -> {

            }
        }



        writeXmlToFile(xmlDoc, pluginXmlFile)

    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

/**
 * Document 转换为 String 并且进行了格式化缩进
 *
 * @param doc XML的Document对象
 * @return String
 */

fun docToString(doc: Document?): String {
    // XML转字符串
    var xmlStr = ""
    try {
        val tf = TransformerFactory.newInstance()
        val t = tf.newTransformer()
        t.setOutputProperty("encoding", "UTF-8") // 解决中文问题，试过用GBK不行
        val bos = ByteArrayOutputStream()
        t.transform(DOMSource(doc), StreamResult(bos))
        xmlStr = bos.toString()
        xmlStr = xmlStr.replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>", "")

    } catch (e: TransformerConfigurationException) {
        // TODO Auto-generated catch block
        e.printStackTrace()
    } catch (e: TransformerException) {
        // TODO Auto-generated catch block
        e.printStackTrace()
    }
    return xmlStr
}

/**
 * 将xml重新写入文件
 */
fun writeXmlToFile(doc: Document, file: File) {
    val xmlstr = docToString(doc)
//    清空文件
    file.writeText("")
    file.appendText(xmlstr)
}


fun generateXml(doc: Document, file: File) {
    // Instantiate the Transformer
    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer()

    // pretty print
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    val source = DOMSource(doc)
    val result = StreamResult(file)
    transformer.transform(source, result)
}
