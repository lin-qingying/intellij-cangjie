//import org.gradle.jvm.tasks.Jar
//
//plugins {
//    kotlin("jvm")
////    id("jps-compatible")
//
//}
//
//sourceSets {
//    main { java.srcDirs("main") }
//    test {
//        java.srcDirs("src")
//
//        resources.srcDir("resources")
//    }
//}
//
//fun JavaExec.passClasspathInJar() {
//    val jarTask = project.task("${name}WriteClassPath", Jar::class) {
//        val classpath = classpath
//        val main = mainClass.get()
//        dependsOn(classpath)
//        inputs.files(classpath)
//        inputs.property("main", main)
//
//        archiveFileName.set("$main.${this@passClasspathInJar.name}.classpath.container.jar")
//        destinationDirectory.set(temporaryDir)
//
//        doFirst {
//            val classPathString = classpath.joinToString(" ") {
//                it.toURI().toString()
//            }
//            manifest {
//                attributes(
//                    mapOf(
//                        "Class-Path" to classPathString,
//                        "Main-Class" to main
//                    )
//                )
//            }
//        }
//    }
//
//    dependsOn(jarTask)
//
//    mainClass.set("-jar")
//    classpath = project.files()
//    args = listOf(jarTask.outputs.files.singleFile.path) + args.orEmpty()
//}
//
//fun Project.smartJavaExec(configure: JavaExec.() -> Unit) = tasks.creating(JavaExec::class) {
//    configure()
//    passClasspathInJar()
//}
//
//fun Project.javaPluginExtension(): JavaPluginExtension = extensions.getByType()
//
//val JavaPluginExtension.testSourceSet: SourceSet
//    get() = sourceSets.getByName("test")
//
//val Project.testSourceSet: SourceSet
//    get() = javaPluginExtension().testSourceSet
//
//fun Project.generator(fqName: String, sourceSet: SourceSet? = null, configure: JavaExec.() -> Unit = {}) =
//    smartJavaExec {
//        group = "Generate"
//        classpath = (sourceSet ?: testSourceSet).runtimeClasspath
//        mainClass.set(fqName)
//        workingDir = rootDir
//        systemProperty("line.separator", "\n")
//        systemProperty("idea.ignore.disabled.plugins", "true")
//        configure()
//    }
//
//val JavaPluginExtension.mainSourceSet: SourceSet
//    get() = sourceSets.getByName("main")
//val Project.mainSourceSet: SourceSet
//    get() = javaPluginExtension().mainSourceSet
//
//fun extraSourceSet(name: String, extendMain: Boolean = true, jpsKind: String? = null): Pair<SourceSet, Configuration> {
//    val sourceSet = sourceSets.create(name) {
//        java.srcDir(name)
//    }
////    val api = configurations[sourceSet.apiConfigurationName]
//
//    val api = configurations[sourceSet.implementationConfigurationName]
//    if (extendMain) {
//        dependencies { api(mainSourceSet.output) }
//        configurations[sourceSet.runtimeOnlyConfigurationName]
//            .extendsFrom(configurations.runtimeClasspath.get())
//    }
//    if (jpsKind != null) {
//        // For Pill
//        sourceSet.extra["jpsKind"] = jpsKind
//    }
//    return sourceSet to api
//}
////val (protobufCompareSourceSet, protobufCompareApi) = extraSourceSet("protobufCompare", jpsKind = SourceSet.TEST_SOURCE_SET_NAME)
//
//val (protobufSourceSet, protobufApi) = extraSourceSet("protobuf")
////val generateProtoBufCompare by generator("cn.cangnova.cangjie.generators.protobuf.GenerateProtoBufCompare", protobufCompareSourceSet)
//
//
//
//dependencies {
//    implementation(kotlin("stdlib", embeddedKotlinVersion))
//    protobufApi(kotlin("stdlib-jdk8"))
//
//
//}
//val generateProtoBuf by generator("cn.cangnova.cangjie.generators.protobuf.GenerateProtoBufKt", protobufSourceSet)
//
