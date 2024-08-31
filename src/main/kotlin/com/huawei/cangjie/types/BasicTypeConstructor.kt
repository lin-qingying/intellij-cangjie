package com.huawei.cangjie.types

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.SupertypeLoopChecker
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import com.huawei.cangjie.storage.StorageManager

class BasicTypeConstructor(
    val classDescriptor: BasicTypeDescriptor,
    storageManager: StorageManager
) : AbstractClassTypeConstructor(storageManager), TypeConstructor {

    override fun computeExtendSuperTypes(extendId: String?): Collection<CangJieType> {
        val result = mutableListOf<CangJieType>()
        classDescriptor.extendClassDescriptor.forEach {
            if (it.typeStatement.getExtendId() != extendId) {
                result.addAll(it.typeConstructor.supertypes)
            }
        }
        return result
    }

    override fun computeSupertypes(): Collection<CangJieType> {

        return emptyList()
    }

    override fun isDenotable(): Boolean {
        return false
    }

    override fun getDeclarationDescriptor(): ClassDescriptor {
        return classDescriptor
    }

    override fun getParameters(): List<TypeParameterDescriptor> {
        return emptyList()
    }

    override val supertypeLoopChecker: SupertypeLoopChecker
        get() = SupertypeLoopChecker.EMPTY

    override fun toString(): String {
        return "Basic:"+classDescriptor.name
    }
}
