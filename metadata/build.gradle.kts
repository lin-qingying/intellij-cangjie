import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.kotlin.dsl.register
import java.io.ByteArrayOutputStream
import java.net.URL
import java.nio.file.Files

plugins {
    kotlin("jvm")
    id("io.netifi.flatbuffers") version "1.0.7"
}

dependencies {
    implementation("com.google.flatbuffers:flatbuffers-java:25.2.10")
    implementation(project(":common"))
    implementation(project(":descriptors"))
}

val flatcVersion = "25.2.10"
val flatcExeName = "flatc" + (if (Os.isFamily(Os.FAMILY_WINDOWS)) ".exe" else "")
val cacheDir = layout.buildDirectory.dir("flatc").get().asFile

// 根据操作系统和架构确定文件名
val assetName by lazy{
    val os = when {
        Os.isFamily(Os.FAMILY_WINDOWS) -> "Windows"
        Os.isFamily(Os.FAMILY_MAC) && Os.isArch("arm64") -> "MacIntel"
        Os.isFamily(Os.FAMILY_MAC) -> "Mac"
        else -> "Linux"
    }
    
    val compilerSuffix = when{
        Os.isFamily(Os.FAMILY_UNIX) -> ".clang++-18"
        else -> ""
    }

    "$os.flatc.binary$compilerSuffix.zip"
}

val zipFile by lazy { cacheDir.resolve(assetName) }
val _flatcPath by lazy { cacheDir.resolve(flatcExeName) }

// 查找合适的flatc版本的任务
val locateFlatc = tasks.register<Task>("locateFlatc") {
    outputs.file(_flatcPath)
    
    doLast {
        // 检查系统是否有合适版本的flatc
        val candidates = listOfNotNull(
            System.getenv("FLATC_HOME")?.let { File(it, "bin${File.separator}$flatcExeName") },
            System.getenv("FLATC_HOME")?.let { File(it, flatcExeName) }
        ).plus(
            System.getenv("PATH")
                ?.split(File.pathSeparator)
                ?.map { File(it, flatcExeName) }
                ?: emptyList()
        ).filter { it.canExecute() }

        val systemFlatc = candidates.firstOrNull { exe ->
            try {
                val result = ByteArrayOutputStream().use { bos ->
                    project.exec {
                        commandLine(exe.absolutePath, "--version")
                        standardOutput = bos
                        isIgnoreExitValue = true
                    }
                    bos.toString().trim()
                }
                result.contains("flatc version") && result.contains(flatcVersion)
            } catch (e: Exception) {
                false
            }
        }

        if (systemFlatc != null && systemFlatc.exists()) {
            // 直接使用系统flatc
            logger.lifecycle("找到系统 flatc: ${systemFlatc.absolutePath}")
            if (!_flatcPath.parentFile.exists()) _flatcPath.parentFile.mkdirs()
            if (!_flatcPath.exists()) {
                systemFlatc.copyTo(_flatcPath, overwrite = true)
                _flatcPath.setExecutable(true)
            }
        } else {
            // 下载并解压到缓存目录
            logger.lifecycle("系统未找到匹配版本的 flatc，将从GitHub自动下载...")
            
            if (!cacheDir.exists()) cacheDir.mkdirs()

            if (!zipFile.exists()) {
                val url = URL(
                    "https://github.com/google/flatbuffers/releases/download" +
                            "/v$flatcVersion/$assetName"
                )
                logger.lifecycle("从网络下载 flatc: $url")
                try {
                    url.openStream().use { input ->
                        Files.newOutputStream(zipFile.toPath()).use { output ->
                            input.copyTo(output)
                        }
                    }
                    logger.lifecycle("下载完成")
                } catch (e: Exception) {
                    throw GradleException("下载 flatc 失败", e)
                }
            }

            // 解压文件
            logger.lifecycle("解压 flatc 到 ${cacheDir.absolutePath}")
            copy {
                from(zipTree(zipFile)) {
                    include { it.name == flatcExeName }
                }
                into(cacheDir)
                fileMode = 0b111101101 // 755 权限
            }
            
            // 确认flatc文件存在且可执行
            if (!_flatcPath.exists()) {
                throw GradleException("flatc 文件解压失败")
            }
            _flatcPath.setExecutable(true)
            
            logger.lifecycle("成功部署 flatc 到: ${_flatcPath.absolutePath}")
        }
    }
}

// 确保flatc存在的验证任务
val ensureFlatc = tasks.register<Task>("ensureFlatc") {
    dependsOn(locateFlatc)
    outputs.file(_flatcPath)
    
    doLast {
        if (!_flatcPath.exists() || !_flatcPath.canExecute()) {
            throw GradleException("flatc 不可访问: ${_flatcPath.absolutePath}")
        }
    }
}

// FlatBuffers配置 - 使用我们确定的路径
flatbuffers {
    flatcPath = _flatcPath.toString()
    language = "kotlin"
    flatBuffersVersion = flatcVersion
}

tasks.register<io.netifi.flatbuffers.plugin.tasks.FlatBuffers>("generateKotlinFlatBuffers") {
    dependsOn(ensureFlatc)
    
    inputDir = file("flatbuffers")
    outputDir = file("$projectDir/gen")
    language = "kotlin"
    extraArgs = "--gen-mutable --gen-object-api"
}

tasks.compileKotlin {
    dependsOn("generateKotlinFlatBuffers")
}

sourceSets {
    main {
        kotlin {
            srcDirs("gen")
        }
    }
}

// 确保构建目录存在
cacheDir.mkdirs()