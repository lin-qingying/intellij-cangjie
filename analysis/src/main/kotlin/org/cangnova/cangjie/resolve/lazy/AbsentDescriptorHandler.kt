/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.resolve.lazy

import org.cangnova.cangjie.container.DefaultImplementation
import org.cangnova.cangjie.container.PlatformSpecificExtension
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import org.cangnova.cangjie.stubindex.resolve.PluginDeclarationProviderFactory
import org.cangnova.cangjie.stubindex.resolve.StubBasedPackageMemberDeclarationProvider
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachments
import org.cangnova.cangjie.utils.safeAs

@DefaultImplementation(BasicAbsentDescriptorHandler::class)
interface AbsentDescriptorHandler : PlatformSpecificExtension<AbsentDescriptorHandler> {
    fun diagnoseDescriptorNotFound(declaration: CjDeclaration): DeclarationDescriptor
}

class BasicAbsentDescriptorHandler : AbsentDescriptorHandler {
    override fun diagnoseDescriptorNotFound(declaration: CjDeclaration) =
        throw NoDescriptorForDeclarationException(declaration)
}

class NoDescriptorForDeclarationException @JvmOverloads constructor(
    declaration: CjDeclaration,
    additionalDetails: String? = null
) :
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
) : AbsentDescriptorHandler {
    override fun diagnoseDescriptorNotFound(declaration: CjDeclaration): DeclarationDescriptor {
        val exceptionWithAttachments =
            declarationProviderFactory.safeAs<PluginDeclarationProviderFactory>()?.let { factory ->
                var declarationException = NoDescriptorForDeclarationException(declaration)
                if (declaration is CjTypeStatement) {
                    declaration.fqName?.let { fqName ->
                        val parent = fqName.parent()
                        (factory.createPackageMemberDeclarationProvider(parent) as? StubBasedPackageMemberDeclarationProvider)?.let {
                            try {
                                it.checkClassOrStructDeclarations(declaration.nameAsSafeName)
                            } catch (e: Exception) {
                                declarationException =
                                    NoDescriptorForDeclarationException(declaration, e.message + "\n")
                            }
                        }
                    }
                }

                declarationException.withAttachment("declarationProviderFactory", factory.debugToString())
            } ?: NoDescriptorForDeclarationException(declaration, declarationProviderFactory.toString())
        throw exceptionWithAttachments
            .withPsiAttachment("CjDeclaration.kt", declaration)
            .withAttachment(
                "CjDeclaration location",
                kotlin.runCatching { declaration.containingFile.virtualFile }.getOrNull()
            )
    }

}
