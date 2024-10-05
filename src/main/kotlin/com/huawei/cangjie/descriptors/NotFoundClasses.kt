package com.huawei.cangjie.descriptors

import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.ClassDescriptorBase
import com.huawei.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.descriptorUtil.module
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.ClassTypeConstructorImpl
import com.huawei.cangjie.types.Variance
import com.huawei.cangjie.types.checker.CangJieTypeRefiner

class NotFoundClasses(private val storageManager: StorageManager, private val module: ModuleDescriptor) {

    class MockClassDescriptor internal constructor(
        storageManager: StorageManager,
        container: DeclarationDescriptor,
        name: Name,

        numberOfDeclaredTypeParameters: Int
    ) : ClassDescriptorBase(storageManager, container, name, SourceElement.NO_SOURCE, /* isExternal = */ false) {
        private val declaredTypeParameters = (0 until numberOfDeclaredTypeParameters).map { index ->
            TypeParameterDescriptorImpl.createWithDefaultBound(
                this, Annotations.EMPTY, /*false,*/ Variance.INVARIANT, Name.identifier("T$index"), index, storageManager
            )
        }

        private val typeConstructor =
            ClassTypeConstructorImpl(
                this,
                computeConstructorTypeParameters(),
                setOf(module.builtIns.anyType),
                storageManager
            )

        override fun getKind() = ClassKind.CLASS
        override fun getModality() = Modality.FINAL

        override val visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC
        override fun getTypeConstructor() = typeConstructor
        override fun getDeclaredTypeParameters() = declaredTypeParameters


        override fun isFun() = false
        override fun isValue() = false
        override fun isExpect() = false

        override val annotations: Annotations get() = Annotations.EMPTY

        override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner) = MemberScope.Empty
        override fun getStaticScope() = MemberScope.Empty
        override fun getConstructors(): Collection<ClassConstructorDescriptor> = emptySet()
        override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? = null
        override fun getEndConstructors(): Collection<ClassConstructorDescriptor> =emptySet()
        override fun getSealedSubclasses(): Collection<ClassDescriptor> = emptyList()

        override fun toString() = "class $name (not found)"
    }

}
