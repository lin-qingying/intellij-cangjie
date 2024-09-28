package com.huawei.cangjie.types

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.SupertypeLoopChecker
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import com.huawei.cangjie.descriptors.impl.basic.BuiltInTypeDescriptor
import com.huawei.cangjie.storage.StorageManager

class BuiltInTypeConstructor(

    override val classDescriptor: BuiltInTypeDescriptor,
    storageManager: StorageManager,
    private val parameters: MutableList<TypeParameterDescriptor> = mutableListOf()
) : BasicTypeConstructor(classDescriptor, storageManager) {

    fun addParameter(typeParameterDescriptor: TypeParameterDescriptor) {
        parameters.add(typeParameterDescriptor)
    }

    override fun getParameters(): List<TypeParameterDescriptor> = parameters
    override fun toString(): String {
        return "BuiltInType:" + classDescriptor.name
    }

    override fun isDenotable(): Boolean {
        return true
    }
}

open class BasicTypeConstructor(
    open val classDescriptor: BasicTypeDescriptor,
    storageManager: StorageManager
) : AbstractClassTypeConstructor(storageManager), TypeConstructor {

    override fun getBuiltIns(): CangJieBuiltIns {
        return classDescriptor.basicMemberScope.getBuiltIns()
    }

    override fun computeExtendSuperTypes(extendId: String?): Collection<CangJieType> {
        val result = mutableListOf<CangJieType>()

        classDescriptor.extendClass.forEach {
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
        return true
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
        return "Basic:" + classDescriptor.name
    }
}
