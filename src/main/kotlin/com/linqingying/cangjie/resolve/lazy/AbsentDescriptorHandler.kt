package com.linqingying.cangjie.resolve.lazy

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.container.PlatformSpecificExtension
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.ide.stubindex.resolve.PluginDeclarationProviderFactory
import com.linqingying.cangjie.ide.stubindex.resolve.StubBasedPackageMemberDeclarationProvider
import com.linqingying.cangjie.psi.CjTypeStatement
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import com.linqingying.cangjie.utils.CangJieExceptionWithAttachments
import com.linqingying.cangjie.utils.safeAs
@DefaultImplementation(BasicAbsentDescriptorHandler::class)
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
            .withPsiAttachment("CjDeclaration.cj", declaration)
            .withAttachment(
                "CjDeclaration location",
                kotlin.runCatching { declaration.containingFile.virtualFile }.getOrNull()
            )
    }

}
