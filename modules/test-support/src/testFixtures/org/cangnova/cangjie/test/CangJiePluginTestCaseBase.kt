package org.cangnova.cangjie.test

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.registerServiceInstance
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchain.api.CjSdk
import org.cangnova.cangjie.toolchain.api.CjSdkRegistry
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

/**
 * 对位 Kotlin `PluginTestCaseBase` 的插件测试工具入口。
 *
 * 仓颉插件当前最关键的运行时依赖不是 JDK，而是 toolchain 提供的 stdlib `.cjo`。
 * 因此这里集中负责：
 * 1. 从仓库 fixture 构造临时 toolchain；
 * 2. 向 `CjSdkRegistry` 注册测试 SDK；
 * 3. 绑定项目级 `CjProjectSdkConfig`，让 IDE 插件路径走真实 toolchain 解析链。
 */
object CangJiePluginTestCaseBase {
    /**
     * IntelliJ 模块级插件测试只加载当前模块沙箱，不能假设产品插件已经把 toolchain 服务完整装配好。
     * 这里显式补齐最小必要服务，使测试仍然走真实的 project SDK 配置入口。
     */
    private class TestCjSdkRegistry : CjSdkRegistry {
        private val sdkCache = linkedMapOf<String, CjSdk>()

        override fun getAllSdks(): List<CjSdk> = sdkCache.values.toList()

        override fun getSdk(id: String): CjSdk? = sdkCache[id]

        override fun getSdkByPath(homePath: Path): CjSdk? {
            val normalizedPath = homePath.toAbsolutePath().normalize()
            return sdkCache.values.firstOrNull { it.homePath.toAbsolutePath().normalize() == normalizedPath }
        }

        override fun registerSdk(sdk: CjSdk): Boolean {
            if (sdkCache.containsKey(sdk.id)) {
                return false
            }
            sdkCache[sdk.id] = sdk
            return true
        }

        override fun registerSdkPath(homePath: Path, customName: String?): CjSdk? {
            return getSdkByPath(homePath) ?: CjSdk(
                id = "cangjie-test-sdk-${UUID.randomUUID()}",
                name = customName ?: homePath.fileName.toString(),
                homePath = homePath,
                version = null,
                isValid = true,
            ).also(::registerSdk)
        }

        override fun unregisterSdk(id: String): Boolean = sdkCache.remove(id) != null

        override fun isSdkRegistered(id: String): Boolean = sdkCache.containsKey(id)

        override fun isSdkPathRegistered(homePath: Path): Boolean = getSdkByPath(homePath) != null

        override fun refreshAllSdks() = Unit

        override fun refreshSdk(id: String): CjSdk? = sdkCache[id]
    }

    private class TestCjProjectSdkConfig : CjProjectSdkConfig {
        private var sdkId: String? = null

        override fun getProjectSdkId(): String? = sdkId

        override fun setProjectSdkId(sdkId: String?) {
            this.sdkId = sdkId
        }

        override fun getProjectSdk(): CjSdk? {
            val currentSdkId = sdkId ?: return null
            return CjSdkRegistry.getInstance().getSdk(currentSdkId)
        }

        override fun hasProjectSdk(): Boolean = sdkId != null
    }

    private fun Path.containsStdlibFixtureRoot(): Boolean {
        return resolve("cfir")
            .resolve("cfir-serialization")
            .resolve("testResources")
            .resolve("cjo-sdk")
            .resolve("windows_x86_64_cjnative")
            .resolve("std.cjo")
            .isRegularFile()
    }

    fun locateRepositoryRoot(start: Path = Paths.get("").toAbsolutePath().normalize()): Path {
        return generateSequence(start) { current -> current.parent }
            .firstOrNull { candidate -> candidate.containsStdlibFixtureRoot() }
            ?: error("Cannot locate repository root from $start")
    }

    private fun ensureToolchainServices(project: Project, parentDisposable: Disposable) {
        val application = ApplicationManager.getApplication()
        if (application.getService(CjSdkRegistry::class.java) == null) {
            application.registerServiceInstance(CjSdkRegistry::class.java, TestCjSdkRegistry())
        }
        if (project.getService(CjProjectSdkConfig::class.java) == null) {
            project.registerServiceInstance(CjProjectSdkConfig::class.java, TestCjProjectSdkConfig())
        }
    }

    fun locateStdlibFixtureRoot(repositoryRoot: Path = locateRepositoryRoot()): Path {
        val fixtureRoot = repositoryRoot
            .resolve("cfir")
            .resolve("cfir-serialization")
            .resolve("testResources")
            .resolve("cjo-sdk")
            .resolve("windows_x86_64_cjnative")

        require(fixtureRoot.resolve("std.cjo").isRegularFile()) {
            "Cannot locate stdlib fixture root under $fixtureRoot"
        }
        return fixtureRoot
    }

    fun createSlimToolchainHome(
        vararg relativeStdlibPaths: String,
        repositoryRoot: Path = locateRepositoryRoot(),
    ): Path {
        val fixtureRoot = locateStdlibFixtureRoot(repositoryRoot)
        val sdkHome = Files.createTempDirectory("cangjie-plugin-sdk")
        val modulesRoot = sdkHome.resolve("modules").resolve("windows_x86_64_llvm")
        Files.createDirectories(modulesRoot)
        Files.createDirectories(sdkHome.resolve("bin"))

        relativeStdlibPaths.forEach { relativePath ->
            val sourceFile = fixtureRoot.resolve(relativePath)
            require(sourceFile.isRegularFile()) { "Missing stdlib fixture file: $sourceFile" }
            val targetFile = modulesRoot.resolve(relativePath)
            Files.createDirectories(targetFile.parent)
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
        }

        return sdkHome
    }

    fun registerProjectToolchain(
        project: Project,
        parentDisposable: Disposable,
        sdkHome: Path,
        displayName: String = "CangJie Test SDK",
    ): CjSdk {
        ensureToolchainServices(project, parentDisposable)

        val sdk = CjSdk(
            id = "cangjie-test-sdk-${UUID.randomUUID()}",
            name = displayName,
            homePath = sdkHome,
            version = null,
            isValid = true,
        )

        val registry = CjSdkRegistry.getInstance()
        check(registry.registerSdk(sdk)) { "Failed to register test SDK `${sdk.id}` at $sdkHome" }

        val sdkConfig = CjProjectSdkConfig.getInstance(project)
        val previousSdkId = sdkConfig.getProjectSdkId()
        sdkConfig.setProjectSdkId(sdk.id)

        Disposer.register(parentDisposable) {
            sdkConfig.setProjectSdkId(previousSdkId)
            registry.unregisterSdk(sdk.id)
            runCatching {
                if (sdkHome.exists()) {
                    sdkHome.toFile().deleteRecursively()
                }
            }
        }

        return sdk
    }
}
