/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

import com.google.protobuf.gradle.GenerateProtoTask
import com.google.protobuf.gradle.id
import groovy.xml.XmlParser
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask
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

plugins {
    idea

    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.intellij.platform") version "2.1.0"
//    id("org.jetbrains.intellij.platform.migration") version "2.1.0"
    id("org.jetbrains.grammarkit") version "2022.3.2"
    kotlin("plugin.serialization") version "1.9.21"
    id("org.gradle.test-retry") version "1.5.3"
    id("com.google.protobuf") version "0.9.3"
//    id("antlr")

}
//IDEA版本
val ideaVersion = "2024.3"
//插件版本
val cangjiePluginVersion = "3.0.0-beta-8"


val kotlinVersion = "1.9.21"
val tomlPlugin = "org.toml.lang"
val terminalPlugin = "org.jetbrains.plugins.terminal"
val nativeDebugPlugin: String = "com.intellij.nativeDebug:243.21565.23"
val psiViewerPlugin: String = "PsiViewer:243.7768"
val indexViewPlugin = "com.jetbrains.hackathon.indices.viewer:1.29"
val chinesePlugin = "com.intellij.zh:233.407"
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

)


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

val ideaType = "IU"
protobuf {
    protoc {
//        artifact = "com.google.protobuf:protoc:4.28.3"
        artifact = "com.google.protobuf:protoc:3.24.4"

    }
// 手动设置源集
    sourceSets {
        main {
            proto {
                srcDir("src/main/kotlin/com/linqingying/cangjie/metadata/proto") // 指定 Protobuf 文件目录
            }
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                // 配置生成选项

                java {


                }
            }
            // 可选：设置输出目录
            task.outputs.upToDateWhen { false } // 始终生成新的输出
        }
    }
}


val isCI = System.getenv("CI") != null
allprojects {
    apply {
        plugin("idea")
        plugin("kotlin")
        plugin("org.jetbrains.grammarkit")
        plugin("org.jetbrains.intellij.platform")
//        plugin("plugin.serialization")
        plugin("org.gradle.test-retry")
    }
    intellijPlatform {

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

        intellijPlatform {
            intellijDependencies()
            defaultRepositories()
        }
    }
    dependencies {

        intellijPlatform {

            create(IntelliJPlatformType.IntellijIdeaUltimate, ideaVersion)
            instrumentationTools()

//            local(dependencyCachePath)
        }


// https://mvnrepository.com/artifact/com.jetbrains.intellij.platform/test-framework-core
//        implementation("com.jetbrains.intellij.platform:test-framework-core:242.23726.103")

        testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
        implementation("com.google.protobuf:protobuf-java:3.24.4-jb.2")

        // https://mvnrepository.com/artifact/com.google.protobuf/protobuf-javalite
//        implementation("com.google.protobuf:protobuf-javalite:3.24.4-jb.2")

        // https://mvnrepository.com/artifact/jakarta.inject/jakarta.inject-api
        implementation("jakarta.inject:jakarta.inject-api:2.0.1")

        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        compileOnly(kotlin("stdlib-jdk8"))
    }


    sourceSets {

        main {
            java.srcDirs("src/gen")
            java.srcDirs("src/main/kotlin")

        }

    }

    tasks {

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

            useJUnitPlatform()
        }

    }

}


val cangjie_plugin_project = project(":plugin") {
    intellijPlatform {

        pluginConfiguration {
            name = "intellij-cangjie"

        }
        publishing {
            token.set(token)
            channels.set(listOf("dev"))
        }
    }

//    group = "com.linqingying.cangjie"
    version = cangjiePluginVersion
    dependencies {

        intellijPlatform {


            if (!isBuildPlugin()) {
                plugins(

                    psiViewerPlugin, indexViewPlugin, chinesePlugin,nativeDebugPlugin
                )
                bundledPlugins(tomlPlugin)
            }

        }
        implementation(project(":"))
//        implementation(project(":dap-debugger"))
        implementation(project(":native-debugger"))

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

//        withType<PublishPluginTask> {
//            token.set(token)
//            channels.set(listOf("dev"))
//        }
    }

    task<RunIdeTask>("buildEventsScheme") {
        dependsOn(tasks.prepareSandbox)
        args(
            "buildEventsScheme",
            "--outputFile=${buildDir.resolve("eventScheme.json").absolutePath}",
            "--pluginId=com.linqingying.cangjie"
        )
        // BACKCOMPAT: 2023.1. Update value to 232 and this comment
        // `IDEA_BUILD_NUMBER` variable is used by `buildEventsScheme` task to write `buildNumber` to output json.
        // It will be used by TeamCity automation to set minimal IDE version for new events
//        environment("IDEA_BUILD_NUMBER", "231")
    }
}

project(":") {

    dependencies {
        intellijPlatform {
            bundledPlugins(tomlPlugin)

        }
        implementation("com.squareup.moshi:moshi-adapters:${moshiVersion}")
        implementation("com.squareup.moshi:moshi-kotlin:${moshiVersion}")
        implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
        implementation("com.google.protobuf:protobuf-java:3.24.4-jb.2")

        implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.22.0")
// https://mvnrepository.com/artifact/org.fusesource.jansi/jansi
        implementation("org.fusesource.jansi:jansi:2.4.1")

        implementation("io.hotmoka:toml4j:0.7.3")

        implementation(project(":lsp"))
//        引入 lib目录下的jar包
//        implementation (files("lib/plsp.jar"))

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

project(":bnf") {
    dependencies {

        implementation(project(":"))

//        implementation(project(":"))
    }
}

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


    }
}

project(":native-debugger") {

    dependencies {
        intellijPlatform {
            plugin(nativeDebugPlugin)
        }
        implementation(project(":"))
    }
}
//project(":dap-debugger") {
//
//    apply {
//
//        plugin("org.jetbrains.kotlin.plugin.serialization")
//    }
//    dependencies {
//
//        intellijPlatform {
//            bundledPlugins(
//                terminalPlugin
//            )
//        }
//        implementation(project(":"))
//
//
//        implementation("com.squareup.moshi:moshi-adapters:${moshiVersion}")
//        implementation("com.squareup.moshi:moshi-kotlin:${moshiVersion}")
//        implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
//        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
//    }
//}


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

//afterEvaluate {
//    updatePluginXmlFile()
//}

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


        val node = xmlDoc.createElement("module")
        node.setAttributeNode(xmlDoc.createAttribute("name")?.apply {
            nodeValue = "com.linqingying.cangjie.dapDebugger"
        })
        content.appendChild(node)



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


