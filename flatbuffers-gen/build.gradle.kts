import org.apache.tools.ant.taskdefs.condition.Os
import java.net.URI
import java.nio.file.Files

dependencies {
    implementation(libs.flatbuffers.java)
    implementation(project(":common"))
    implementation(project(":descriptors"))
}

val flatcVersion = "25.2.10"
val flatcExeName: String = "flatc" + (if (Os.isFamily(Os.FAMILY_WINDOWS)) ".exe" else "")
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
                val process = ProcessBuilder(exe.absolutePath, "--version")
                    .redirectErrorStream(true)
                    .start()
                val result = process.inputStream.bufferedReader().use { it.readText() }.trim()
                process.waitFor()
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
                val urlString = "https://github.com/google/flatbuffers/releases/download" +
                        "/v$flatcVersion/$assetName"
                val url = URI(urlString).toURL()
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
            }

            // 确认flatc文件存在且可执行
            if (!_flatcPath.exists()) {
                throw GradleException("flatc 文件解压失败")
            }
            // 设置可执行权限 (755)
            _flatcPath.setExecutable(true, false)
            _flatcPath.setReadable(true, false)
            _flatcPath.setWritable(true, false)

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

// FlatBuffers configuration - 使用我们确定的路径
val inputDir = file("flatbuffers")
val outputDir = file("$projectDir/gen")

tasks.register<Exec>("generateKotlinFlatBuffers") {
    dependsOn(ensureFlatc)

    inputs.dir(inputDir)
    outputs.dir(outputDir)

    doFirst {
        outputDir.mkdirs()
    }

    commandLine(
        _flatcPath.absolutePath,
        "--kotlin",
        "--gen-mutable",
        "--gen-object-api",
        "-o", outputDir.absolutePath,
        *fileTree(inputDir).filter { it.extension == "fbs" }.map { it.absolutePath }.toTypedArray()
    )
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