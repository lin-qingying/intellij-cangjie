package org.cangnova.cangjie.test

import java.io.File
import java.nio.file.Path

/**
 * 对位 Kotlin `KotlinLightCodeInsightFixtureTestCase` 的仓颉插件 light fixture 基类。
 *
 * 当前先补齐插件运行时验证最需要的能力：
 * - 统一 testData 路径入口；
 * - toolchain stdlib fixture 注入；
 * - 让插件级 `.cjo` / builtins / highlighting 测试不再依赖手工 `runIde`。
 */
abstract class CangJieLightCodeInsightFixtureTestCase : CangJieLightCodeInsightFixtureTestCaseBase(), CangJieTestCase {
    open val testDataDirectory: File by lazy { File(testDataPath) }

    final override fun getTestDataPath(): String = TestMetadataUtil.getTestDataPath(javaClass)

    override fun fileName(): String =
        CangJieTestUtils.getTestDataFileName(javaClass, name) ?: super.fileName()

    protected fun dataFile(fileName: String): File = File(testDataDirectory, fileName)

    protected fun dataFilePath(fileName: String = fileName()): String = dataFile(fileName).path

    protected fun <T> withToolchainStdlibFixture(
        vararg relativeStdlibPaths: String,
        action: (sdkHome: Path) -> T,
    ): T {
        val sdkHome = CangJiePluginTestCaseBase.createSlimToolchainHome(*relativeStdlibPaths)
        CangJiePluginTestCaseBase.registerProjectToolchain(project, myFixture.testRootDisposable, sdkHome)
        return action(sdkHome)
    }
}
