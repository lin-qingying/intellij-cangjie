import Build_gradle.BuildType.*
import groovy.xml.XmlParser
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
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


val build_type: String by project

enum class BuildType {


    //   IDEA Ultimate Edition + nativeDebug本地调试插件  LLDB
    IU_NATIVE_DEBUG,

    // IDEA Community Edition + DAP调试插件
    IC_DAP,

    //    IDEA Community Edition + cidr本地代码调试   LLDB
    IC_CIDR_NATIVE_DEBUG;


    companion object {

        fun fromString(str: String): BuildType {
            return when (str) {
                "IU_NATIVE_DEBUG" -> IU_NATIVE_DEBUG
                "IC_DAP" -> IC_DAP
                "IC_CIDR_NATIVE_DEBUG" -> IC_CIDR_NATIVE_DEBUG
                else -> IC_DAP
            }
        }
    }
}


//构建方式
val buildType = BuildType.fromString(build_type)

//IDEA版本
val ideaVersion = "2024.1"
//插件版本
val cangjiePluginVersion = "3.0.0-beta-5"


val kotlinVersion = "1.9.21"
val tomlPlugin = "org.toml.lang"
val terminalPlugin = "org.jetbrains.plugins.terminal"
val nativeDebugPlugin: String = "com.intellij.nativeDebug:241.14494.73"
val psiViewerPlugin: String = "PsiViewer:241-SNAPSHOT"
val indexViewPlugin = "com.jetbrains.hackathon.indices.viewer:1.26"
val chinesePlugin = "com.intellij.zh:241.230"
val diagramPlugin = "com.intellij.diagram"
val basePluginArchiveName = "intellij-cangjie-analyzer"

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

plugins {
    idea
//    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.jetbrains.grammarkit") version "2022.3.2"
    kotlin("plugin.serialization") version "1.9.21"
    id("org.gradle.test-retry") version "1.5.3"

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

val ideaType = // Target IDE Platform
    when (buildType) {
        IU_NATIVE_DEBUG -> "IU"
        IC_DAP, IC_CIDR_NATIVE_DEBUG -> "IC"

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
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies") }

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

        downloadSources.set(/*!isCI*/true)
        updateSinceUntilBuild.set(false)
        instrumentCode.set(false)
        ideaDependencyCachePath.set(dependencyCachePath)
//        sandboxDir.set("$buildDir/$ideaVersion-sandbox")
    }

//    sourceSets {
//        main {
//            java {
//
//                srcDirs("src/main/gen")
//                srcDirs("src/main/kotlin")
//            }
//        }
//    }
    sourceSets {

        main {
            java.srcDirs("src/gen")
            java.srcDirs("src/main/kotlin")
//            resources.srcDirs("src/$platformVersion/main/resources")
        }
        test {
//            resources.srcDirs("src/$platformVersion/test/resources")
        }
    }

    tasks {
//        withType<JavaCompile> {
//            sourceCompatibility = "17"
//            targetCompatibility = "17"
//        }
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "17"
            kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all", "-Xcontext-receivers")
        }

        withType<PatchPluginXmlTask> {
            sinceBuild.set("241")
            untilBuild.set("243.*")
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
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")


        // https://mvnrepository.com/artifact/jakarta.inject/jakarta.inject-api
        implementation("jakarta.inject:jakarta.inject-api:2.0.1")
        compileOnly(kotlin("stdlib-jdk8"))
    }
}


val cangjie_plugin_project = project(":plugin") {
    intellij {
        pluginName.set("intellij-cangjie")
        plugins.set(
            if (isBuildPlugin()) {
                listOf()
            } else {
                listOf(psiViewerPlugin, indexViewPlugin, chinesePlugin)
            }
        )

    }
//    group = "com.huawei.cangjie"
    version = cangjiePluginVersion
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
    // We need to put all plugin manifest files into single jar to make new plugin model work
    val mergePluginJarTask = task<Jar>("mergePluginJars") {
        dependsOn
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE // 避免重复文件错误
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

val cangjie_src_project = project(":") {
    intellij {
        plugins.set(listOf(tomlPlugin/*,diagramPlugin*/))
    }
    dependencies {
        implementation("com.squareup.moshi:moshi-adapters:${moshiVersion}")
        implementation("com.squareup.moshi:moshi-kotlin:${moshiVersion}")
        implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")


        implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.22.0")

        implementation("io.hotmoka:toml4j:0.7.3")

        implementation(project(":lsp"))
//        implementation(project(":lsp1"))

//        implementation(files("lib/lsp.jar"))
        implementation(project(":utils"))
        implementation("io.javaslang:javaslang:2.1.0-alpha")


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
//
project(":lsp") {
    dependencies {
        implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.22.0")
        implementation(project(":utils"))

//        implementation(project(":"))
    }
}
//
//project(":lsp1") {
//    dependencies {
//        implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.22.0")
//        implementation(project(":utils"))
//
////        implementation(project(":"))
//    }
//}

project(":utils") {
    dependencies {

//        implementation("org.yaml:snakeyaml:2.2")
    }
}
//project(":deveco-dap-debugger") {
//    intellij {
//        plugins.set(listOf( ))
//    }
//
//    dependencies {
//        implementation(project(":"))
//// https://mvnrepository.com/artifact/org.eclipse.lsp4j/org.eclipse.lsp4j.debug
//        implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.debug:0.23.1")
//        implementation(files("lib/intellij-dap.jar","lib/dap4j.jar"))
//
//    }
//}


when (buildType) {
    IU_NATIVE_DEBUG -> {
        project(":native-debugger") {
            intellij {
                plugins.set(listOf(nativeDebugPlugin))
            }
            dependencies {
                implementation(project(":"))
            }
        }

        cangjie_plugin_project.intellij.plugins.add(nativeDebugPlugin)
        cangjie_plugin_project.dependencies {
            implementation(project(":native-debugger"))

        }


    }

    IC_DAP -> {

        val dap = project(":dap-debugger") {
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
        cangjie_plugin_project.dependencies {
            implementation(dap)
        }
        cangjie_plugin_project.intellij.plugins.add(terminalPlugin)


    }

    IC_CIDR_NATIVE_DEBUG -> TODO()
}


fun isBuildPlugin(): Boolean {
    return "buildPlugin" in gradle.startParameter.taskNames
}

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

            "com.huawei.cangjie.nativeDebug",
            "com.huawei.cangjie.debugger",
            "com.huawei.cangjie.dapDebugger"
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
            IU_NATIVE_DEBUG -> {
                var node = xmlDoc.createElement("module")
                node.setAttributeNode(xmlDoc.createAttribute("name")?.apply {
                    nodeValue = "com.huawei.cangjie.nativeDebug"
                })
                content.appendChild(node)

                node = xmlDoc.createElement("module")
                node.setAttributeNode(xmlDoc.createAttribute("name")?.apply {
                    nodeValue = "com.huawei.cangjie.debugger"
                })

                content.appendChild(node)

            }

            IC_DAP -> {

                val node = xmlDoc.createElement("module")
                node.setAttributeNode(xmlDoc.createAttribute("name")?.apply {
                    nodeValue = "com.huawei.cangjie.dapDebugger"
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


