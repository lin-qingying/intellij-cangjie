package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.container.PlatformSpecificExtension
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.ide.stubindex.resolve.PluginDeclarationProviderFactory
import com.huawei.cangjie.ide.stubindex.resolve.StubBasedPackageMemberDeclarationProvider
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import com.huawei.cangjie.utils.CangJieExceptionWithAttachments
import com.huawei.cangjie.utils.safeAs

interface AbsentDescriptorHandler : PlatformSpecificExtension<AbsentDescriptorHandler> {
    fun diagnoseDescriptorNotFound(declaration: CjDeclaration): DeclarationDescriptor
}

class BasicAbsentDescriptorHandler : AbsentDescriptorHandler {
    override fun diagnoseDescriptorNotFound(declaration: CjDeclaration) = throw NoDescriptorForDeclarationException(declaration)
}

class NoDescriptorForDeclarationException @JvmOverloads constructor(declaration: CjDeclaration, additionalDetails: String? = null) :
    CangJieExceptionWithAttachments(
        "Descriptor wasn't found for declaration $declaration"
                + (additionalDetails?.let { "\n---------------------------------------------------\n$it" } ?: "")
    ) {
    init {
        withPsiAttachment("declaration.cj", declaration)
    }
}



class IdeaAbsentDescriptorHandler(
    private val declarationProviderFactory: DeclarationProviderFactory
) : AbsentDescriptorHandler{
    override fun diagnoseDescriptorNotFound(declaration: CjDeclaration): DeclarationDescriptor {
        val exceptionWithAttachments =
            declarationProviderFactory.safeAs<PluginDeclarationProviderFactory>()?.let { factory ->
                var declarationException = NoDescriptorForDeclarationException(declaration)
                if (declaration is CjTypeStatement  ) {
                    declaration.fqName?.let { fqName ->
                        val parent = fqName.parent()
                        (factory.createPackageMemberDeclarationProvider(parent) as? StubBasedPackageMemberDeclarationProvider)?.let {
                            try {
                                it.checkClassOrStructDeclarations(declaration.nameAsSafeName)
                            } catch (e: Exception) {
                                declarationException = NoDescriptorForDeclarationException(declaration, e.message + "\n")
                            }
                        }
                    }
                }

                declarationException.withAttachment("declarationProviderFactory", factory.debugToString())
            } ?: NoDescriptorForDeclarationException(declaration, declarationProviderFactory.toString())
        throw exceptionWithAttachments
            .withPsiAttachment("KtDeclaration.kt", declaration)
            .withAttachment(
                "KtDeclaration location",
                kotlin.runCatching { declaration.containingFile.virtualFile }.getOrNull()
            )
    }

}
