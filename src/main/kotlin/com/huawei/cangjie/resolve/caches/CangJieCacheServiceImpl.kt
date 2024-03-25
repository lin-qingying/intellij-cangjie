package com.huawei.cangjie.resolve.caches

import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.utils.CangJieExceptionWithAttachments
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

internal val LOG = Logger.getInstance(CangJieCacheService::class.java)

class CangJieCacheServiceImpl(val project: Project) : CangJieCacheService {
    override fun getResolutionFacade(element: CjElement): ResolutionFacade {
        val file = element.fileForElement()

        return CachedValuesManager.getCachedValue(file) {

            CachedValueProvider.Result(
                getFacadeToAnalyzeFile(file),

                ProjectRootModificationTracker.getInstance(project),
            )
        }
    }

    private fun getFacadeToAnalyzeFile(file: CjFile): ResolutionFacade {
TODO()
    }

    override fun getResolutionFacade(elements: List<CjElement>): ResolutionFacade {
        TODO("Not yet implemented")
    }

    private fun getFilesForElements(elements: List<CjElement>): List<CjFile> {
        return elements.map {
            it.fileForElement()
        }.distinct()
    }


    private fun CjElement.fileForElement() = try {
        // in theory `containingKtFile` is `@NotNull` but in practice EA-114080
        @Suppress("USELESS_ELVIS")
        getContainingCjFile() ?: throw IllegalStateException("containingKtFile was null for $this of ${this.javaClass}")
    } catch (e: Exception) {
        if (e is ControlFlowException) throw e
        throw CangJieExceptionWithAttachments("Couldn't get containingKtFile for ktElement", e)
            .withPsiAttachment("element", this)
            .withPsiAttachment("file", this.containingFile)
            .withAttachment("original", e.message)
    }
}