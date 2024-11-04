package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.builtins.BuiltInsPackageFragment
import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.ClassDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.descriptorUtil.builtIns
import com.linqingying.cangjie.resolve.scopes.GivenFunctionsMemberScope
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.storage.getValue

interface ClassDescriptorFactory {
    fun shouldCreateClass(packageFqName: FqName, name: Name): Boolean

    fun createClass(classId: ClassId): ClassDescriptor?

    // Note: do not rely on this function to return _all_ classes. Some factories can not enumerate all classes that they're able to create
    fun getAllContributedClassesIfPossible(packageFqName: FqName): Collection<ClassDescriptor>
}

class CloneableClassScope(
    storageManager: StorageManager,
    containingClass: ClassDescriptor
) : GivenFunctionsMemberScope(storageManager, containingClass) {
    override fun computeDeclaredFunctions(): List<FunctionDescriptor> = listOf(
        SimpleFunctionDescriptorImpl.create(containingClass, Annotations.EMPTY, CLONE_NAME, CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE).apply {
            initialize(
                null, containingClass.thisAsReceiverParameter, emptyList(), emptyList(), emptyList(), containingClass.builtIns.anyType,
                Modality.OPEN, DescriptorVisibilities.PROTECTED
            )
        }
    )

    companion object {
        val CLONE_NAME = Name.identifier("clone")
    }
}

class CangJieBuiltInClassDescriptorFactory(
    storageManager: StorageManager,
    private val moduleDescriptor: ModuleDescriptor,
    private val computeContainingDeclaration: (ModuleDescriptor) -> DeclarationDescriptor = { module ->
        module.getPackage(CANGJIE_FQ_NAME).fragments.filterIsInstance<BuiltInsPackageFragment>().first()
    }
) : ClassDescriptorFactory {
    private val cloneable by storageManager.createLazyValue {
        ClassDescriptorImpl(
            computeContainingDeclaration(moduleDescriptor),
            CLONEABLE_NAME, Modality.ABSTRACT, ClassKind.INTERFACE, listOf(moduleDescriptor.builtIns.anyType),
            SourceElement.NO_SOURCE, false, storageManager
        ).apply {
            initialize(CloneableClassScope(storageManager, this), emptySet(), null, emptySet())
        }
    }

    override fun shouldCreateClass(packageFqName: FqName, name: Name): Boolean =
        name == CLONEABLE_NAME && packageFqName == CANGJIE_FQ_NAME

    override fun createClass(classId: ClassId): ClassDescriptor? =
        when (classId) {
            CLONEABLE_CLASS_ID -> cloneable
            else -> null
        }

    override fun getAllContributedClassesIfPossible(packageFqName: FqName): Collection<ClassDescriptor> =
        when (packageFqName) {
            CANGJIE_FQ_NAME -> setOf(cloneable)
            else -> emptySet()
        }

    companion object {
        private val CANGJIE_FQ_NAME = StandardNames.BUILT_INS_PACKAGE_FQ_NAME
        private val CLONEABLE_NAME = StandardNames.FqNames.cloneable.shortName()
        val CLONEABLE_CLASS_ID = ClassId.topLevel(StandardNames.FqNames.cloneable.toSafe())
    }
}
