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

package com.linqingying.cangjie.builtins

import com.linqingying.cangjie.descriptors.ClassConstructorDescriptor
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.SimpleFunctionDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.serialization.deserialization.AdditionalClassPartsProvider
import com.linqingying.cangjie.serialization.deserialization.PlatformDependentDeclarationFilter
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.CangJieType

class CangJieBuiltInsCustomizer(
    private val moduleDescriptor: ModuleDescriptor,
    storageManager: StorageManager,
): AdditionalClassPartsProvider, PlatformDependentDeclarationFilter {
    override fun getSupertypes(classDescriptor: ClassDescriptor): Collection<CangJieType> {
        val fqName = classDescriptor.fqNameUnsafe
        return when {

            else -> listOf()
        }
    }

    override fun getFunctions(name: Name, classDescriptor: ClassDescriptor): Collection<SimpleFunctionDescriptor> {
  return emptyList()
    }

    override fun getConstructors(classDescriptor: ClassDescriptor): Collection<ClassConstructorDescriptor> {
        return emptyList()

    }

    override fun getFunctionsNames(classDescriptor: ClassDescriptor): Collection<Name> {
        return emptyList()

    }

    override fun isFunctionAvailable(
        classDescriptor: ClassDescriptor,
        functionDescriptor: SimpleFunctionDescriptor
    ): Boolean {
        return false

    }
}
