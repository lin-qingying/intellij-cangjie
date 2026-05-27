@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.provider

import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.CangJieLightCodeInsightFixtureTestCase
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 这里必须跑在 `product/idea-plugin`，保证测试沙箱加载真实 `plugin.xml`，
 * 从而覆盖 IDE Analysis API project-structure 服务的实际装配链路。
 */
class CaIdeDanglingFileProjectStructureTest : CangJieLightCodeInsightFixtureTestCase() {
    fun testNonPhysicalCopyUsesIgnoreSelfResolutionMode() {
        val sourceFile = addSourceFile()
        val sourceModule = CangJieProjectStructureProvider.getModule(project, sourceFile, useSiteModule = null)
        val fakeFile = CjPsiFactory.contextual(sourceFile, markGenerated = true).createFile("copy.cj", sourceFile.text).apply {
            originalFile = sourceFile
        }

        assertTrue(!fakeFile.isPhysical, "dangling copy 必须是非物理 PSI。")
        assertTrue(!fakeFile.viewProvider.isEventSystemEnabled, "dangling copy 必须关闭事件系统。")

        val module = CangJieProjectStructureProvider.getModule(project, fakeFile, useSiteModule = null)
        val danglingModule = assertIs<CaDanglingFileModule>(module)
        assertEquals(CaDanglingFileResolutionMode.IGNORE_SELF, danglingModule.resolutionMode)
        assertEquals(sourceModule, danglingModule.contextModule)
    }

    fun testContextualInMemoryFileUsesElementContextModule() {
        val sourceFile = addSourceFile()
        val contextElement = sourceFile.declarations.single()
        val sourceModule = CangJieProjectStructureProvider.getModule(project, contextElement, useSiteModule = null)
        val fakeFile = CjPsiFactory.contextual(contextElement, markGenerated = true).createFile(
            "contextual.cj",
            """
            package sample.project

            func useHost(): Host {
                return Host()
            }
            """.trimIndent(),
        )

        assertTrue(!fakeFile.isPhysical, "contextual file 必须是非物理 PSI。")

        val module = CangJieProjectStructureProvider.getModule(project, fakeFile, useSiteModule = null)
        val danglingModule = assertIs<CaDanglingFileModule>(module)
        assertEquals(CaDanglingFileResolutionMode.PREFER_SELF, danglingModule.resolutionMode)
        assertEquals(sourceModule, danglingModule.contextModule)
    }

    private fun addSourceFile(): CjFile {
        return configureCangJieByText(
            """
            package sample.project

            class Host
            """.trimIndent(),
        ) as CjFile
    }
}
