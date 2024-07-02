package com.huawei.cangjie.types.error

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.ClassDescriptorImpl
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.LockBasedStorageManager
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.TypeSubstitution

class ErrorClassDescriptor(name: Name) : ClassDescriptorImpl(
    ErrorUtils.errorModule, name, Modality.OPEN, ClassKind.CLASS, emptyList(), SourceElement.NO_SOURCE, false, LockBasedStorageManager.NO_LOCKS

) {
    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope {
        TODO("Not yet implemented")
    }


    override fun getStaticScope(): MemberScope {
        TODO("Not yet implemented")
    }

    override fun getConstructors(): MutableCollection<ClassConstructorDescriptor> {
        TODO("Not yet implemented")
    }

    override fun getCompanionObjectDescriptor(): ClassDescriptor? {
        TODO("Not yet implemented")
    }

    override fun getKind(): ClassKind {
        TODO("Not yet implemented")
    }

    override fun getTypeConstructor(): TypeConstructor {
        TODO("Not yet implemented")
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R {
        TODO("Not yet implemented")
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        TODO("Not yet implemented")
    }

    override val annotations: Annotations
        get() = TODO("Not yet implemented")
}