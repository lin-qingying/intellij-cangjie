package com.huawei.cangjie.resolve.lazy.descriptors

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.psi.CjParameter
import com.huawei.cangjie.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import com.huawei.cangjie.resolve.scopes.*
import com.huawei.cangjie.storage.StorageManager

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
