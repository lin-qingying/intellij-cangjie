package com.huawei.cangjie.idea.projectStructure

import com.huawei.cangjie.idea.projectStructure.moduleInfo.IdeaModuleInfo
import com.huawei.cangjie.utils.SeqScope
import com.huawei.cangjie.utils.TransformingIterator
import com.huawei.cangjie.utils.seq
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus

@Service(Service.Level.PROJECT)
class ModuleInfoProvider(private val project: Project) {
    data class Configuration(
        val createSourceLibraryInfoForLibraryBinaries: Boolean = true,
        val preferModulesFromExtensions: Boolean = false,
        val contextualModuleInfo: IdeaModuleInfo? = null,
    ) {
        companion object {
            val Default = Configuration()
        }
    }

    companion object {
        internal val LOG = Logger.getInstance(ModuleInfoProvider::class.java)

        fun getInstance(project: Project): ModuleInfoProvider = project.service()

        fun findAnchorElement(element: PsiElement): PsiElement = when {
            element is PsiDirectory -> element
//            element !is CjLightElement<*, *> -> element.containingFile
            /**
             * We shouldn't unwrap decompiled classes
             * @see [ModuleInfoProvider.collectByLightElement]
             */
//            element.getNonStrictParentOfType<CjLightClassForDecompiledDeclaration>() != null -> null
//            element is CjLightClassForFacade -> element.files.first()
//            else -> element.cangjieOrigin?.let(::findAnchorElement)
            else -> {
                TODO()
            }
        }
    }

    private fun SeqScope<Result<IdeaModuleInfo>>.collectByElement(element: PsiElement, config: Configuration) {

    }

    fun collect(element: PsiElement, config: Configuration = Configuration.Default): Sequence<Result<IdeaModuleInfo>> {
        return seq {
            collectByElement(element, config)
        }
    }

}
@ApiStatus.Internal
fun Sequence<Result<IdeaModuleInfo>>.unwrap(
    errorHandler: (String, Throwable) -> Unit,
    stopOnErrors: Boolean = true
): Sequence<IdeaModuleInfo> {
    val originalSequence = this
    return object : Sequence<IdeaModuleInfo> {
        override fun iterator(): Iterator<IdeaModuleInfo> {
            return object : TransformingIterator<IdeaModuleInfo>() {
                private var iterator: Iterator<Result<IdeaModuleInfo>>? = originalSequence.iterator()
                override fun calculateHasNext(): Boolean = iterator?.hasNext() == true

                override fun calculateNext(): IdeaModuleInfo? {
                    val iter = iterator ?: return null
                    val result = iter.next()
                    result.getOrNull()?.let { return it }

                    val error = result.exceptionOrNull()
                    if (error != null) {
                        errorHandler("Could not find correct module information", error)
                        if (stopOnErrors) {
                            iterator = null
                        }
                    }
                    return null
                }
            }
        }
    }
}
