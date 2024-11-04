package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.ClassDescriptorBase
import com.linqingying.cangjie.descriptors.impl.EmptyPackageFragmentDescriptor
import com.linqingying.cangjie.descriptors.impl.TypeParameterDescriptorImpl
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.descriptorUtil.module
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.ClassTypeConstructorImpl
import com.linqingying.cangjie.types.Variance
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner

class NotFoundClasses(private val storageManager: StorageManager, private val module: ModuleDescriptor) {
    /**
     * @param typeParametersCount list of numbers of type parameters in this class and all its outer classes, starting from this class
     */
    private data class ClassRequest(val classId: ClassId, val typeParametersCount: List<Int>)

    private val packageFragments = storageManager.createMemoizedFunction<FqName, PackageFragmentDescriptor> { fqName ->
        EmptyPackageFragmentDescriptor(module, fqName)
    }

    private val classes = storageManager.createMemoizedFunction<ClassRequest, ClassDescriptor> { (classId, typeParametersCount) ->
        if (classId.isLocal) {
            throw UnsupportedOperationException("Unresolved local class: $classId")
        }

        val container = classId.outerClassId?.let { outerClassId ->
            getClass(outerClassId, typeParametersCount.drop(1))
        } ?: packageFragments(classId.packageFqName)

        // Treat a class with a nested ClassId as inner for simplicity, otherwise the outer type cannot have generic arguments
        val isInner = classId.isNestedClass

        MockClassDescriptor(storageManager, container, classId.shortClassName,   typeParametersCount.firstOrNull() ?: 0)
    }
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

    // We create different ClassDescriptor instances for types with the same ClassId but different number of type arguments.
    // (This may happen when a class with the same FQ name is instantiated with different type arguments in different modules.)
    // It's better than creating just one descriptor because otherwise would fail in multiple places where it's asserted that
    // the number of type arguments in a type must be equal to the number of the type parameters of the class
    fun getClass(classId: ClassId, typeParametersCount: List<Int>): ClassDescriptor {
        return classes(ClassRequest(classId, typeParametersCount))
    }
}
