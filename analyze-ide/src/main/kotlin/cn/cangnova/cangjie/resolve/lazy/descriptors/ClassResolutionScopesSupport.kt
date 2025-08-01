/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.resolve.lazy.descriptors

import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.psi.CjParameter
import cn.cangnova.cangjie.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import cn.cangnova.cangjie.resolve.scopes.*

class ClassResolutionScopesSupport(
    private val classDescriptor: ClassDescriptor,
    storageManager: StorageManager,
    private val languageVersionSettings: LanguageVersionSettings,
    private val getOuterScope: () -> LexicalScope
) {
    companion object {
        private val createErrorLexicalScope: (Boolean) -> LexicalScope = { ErrorLexicalScope() }
    }
    val scopeForClassHeaderResolution: () -> LexicalScope = storageManager.createLazyValue(onRecursion = createErrorLexicalScope) {
        scopeWithGenerics(getOuterScope())
    }

    private fun <T : Any> StorageManager.createLazyValue(onRecursion: ((Boolean) -> T), compute: () -> T) =
        createLazyValue(compute, onRecursion)

    private fun scopeWithGenerics(parent: LexicalScope): LexicalScopeImpl {
        return LexicalScopeImpl(parent, classDescriptor, false, null, emptyList(), LexicalScopeKind.CLASS_HEADER) {
            classDescriptor.declaredTypeParameters.forEach { addClassifierDescriptor(it) }
        }
    }

    private val inheritanceScopeWithMe: () -> LexicalScope =
        storageManager.createLazyValue(onRecursion = createErrorLexicalScope) {
            createInheritanceScope(
                parent = inheritanceScopeWithoutMe(),
                ownerDescriptor = classDescriptor,
                classDescriptor = classDescriptor
            )
        }
    private val inheritanceScopeWithoutMe: () -> LexicalScope =
        storageManager.createLazyValue(onRecursion = createErrorLexicalScope) {
            classDescriptor.getAllSuperclassesWithoutAny().asReversed().fold(getOuterScope()) { scope, currentClass ->
                createInheritanceScope(
                    parent = scope,
                    ownerDescriptor = classDescriptor,
                    classDescriptor = currentClass
                )
            }
        }

    val scopeForConstructorHeaderResolution: () -> LexicalScope = storageManager.createLazyValue {
        scopeWithGenerics(inheritanceScopeWithMe())
    }
    private fun createInheritanceScope(
        parent: LexicalScope,
        ownerDescriptor: DeclarationDescriptor,
        classDescriptor: ClassDescriptor,
        withCompanionObject: Boolean = true,
        isDeprecated: Boolean = false
    ): LexicalScope {

        val lexicalChainedScope = LexicalChainedScope.create(
            parent, ownerDescriptor,
            isOwnerDescriptorAccessibleByLabel = false,
            implicitReceiver = null,
            contextReceiversGroup = emptyList(),
            kind = LexicalScopeKind.CLASS_INHERITANCE,
            classDescriptor.staticScope,
            classDescriptor.unsubstitutedInnerClassesScope,
            null,
            isStaticScope = true
        )

        return if (isDeprecated) DeprecatedLexicalScope(lexicalChainedScope) else lexicalChainedScope
    }

    val scopeForMemberDeclarationResolution: () -> LexicalScope =
        storageManager.createLazyValue(onRecursion = createErrorLexicalScope) {
            val scopeWithGenerics = scopeWithGenerics(inheritanceScopeWithMe())
            LexicalScopeImpl(
                scopeWithGenerics,
                classDescriptor,
                true,
                classDescriptor.thisAsReceiverParameter,
                classDescriptor.contextReceivers,
                LexicalScopeKind.CLASS_MEMBER_SCOPE
            )
        }

}

fun scopeForInitializerResolution(
    classDescriptor: LazyClassDescriptor,
    parentDescriptor: DeclarationDescriptor,
    primaryConstructorParameters: List<CjParameter>
): LexicalScope {
    return LexicalScopeImpl(
        classDescriptor.scopeForMemberDeclarationResolution,
        parentDescriptor,
        false,
        null,
        emptyList(),
        LexicalScopeKind.CLASS_INITIALIZER
    ) {
        if (primaryConstructorParameters.isNotEmpty()) {
            val parameterDescriptors = classDescriptor.unsubstitutedPrimaryConstructor!!.valueParameters
            assert(parameterDescriptors.size == primaryConstructorParameters.size)
            for ((parameter, descriptor) in primaryConstructorParameters.zip(parameterDescriptors)) {
                if (!parameter.hasLetOrVar()) {
                    addVariableDescriptor(descriptor)
                }
            }
        }
    }
}
fun scopeForInitializerResolution(
    classDescriptor: LazyExtendClassDescriptor,
    parentDescriptor: DeclarationDescriptor,
    primaryConstructorParameters: List<CjParameter>
): LexicalScope {
    return LexicalScopeImpl(
        classDescriptor.scopeForMemberDeclarationResolution,
        parentDescriptor,
        false,
        null,
        emptyList(),
        LexicalScopeKind.CLASS_INITIALIZER
    ) {
        if (primaryConstructorParameters.isNotEmpty()) {
            val parameterDescriptors = classDescriptor.unsubstitutedPrimaryConstructor!!.valueParameters
            assert(parameterDescriptors.size == primaryConstructorParameters.size)
            for ((parameter, descriptor) in primaryConstructorParameters.zip(parameterDescriptors)) {
                if (!parameter.hasLetOrVar()) {
                    addVariableDescriptor(descriptor)
                }
            }
        }
    }
}
