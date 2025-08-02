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

package cn.cangnova.cangjie.types

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.descriptors.SupertypeLoopChecker
import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor
import cn.cangnova.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import cn.cangnova.cangjie.descriptors.impl.basic.BuiltInTypeDescriptor
import cn.cangnova.cangjie.storage.StorageManager

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
