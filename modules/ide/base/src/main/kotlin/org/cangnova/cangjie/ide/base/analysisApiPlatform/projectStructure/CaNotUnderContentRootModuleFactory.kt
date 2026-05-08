package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule

/**
 * 对位 Kotlin `KaNotUnderContentRootModuleFactory`。
 *
 * IDE project-structure 允许平台或子系统为“不在内容根下”的文件提供专门模块实现；
 * 若没有扩展命中，则回退到默认 IDE not-under-content-root 模块。
 */
interface CaNotUnderContentRootModuleFactory {
    fun create(project: Project, file: PsiFile?): CaNotUnderContentRootModule?

    companion object {
        val EP_NAME: ExtensionPointName<CaNotUnderContentRootModuleFactory> =
            ExtensionPointName.create("org.cangnova.cangjie.cangjieNotUnderContentRootModuleFactory")
    }
}
