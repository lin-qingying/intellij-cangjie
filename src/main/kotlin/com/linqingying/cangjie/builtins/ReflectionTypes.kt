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

import com.linqingying.cangjie.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.NotFoundClasses
import com.linqingying.cangjie.name.FqNameUnsafe
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeConstructor

class ReflectionTypes(module: ModuleDescriptor, private val notFoundClasses: NotFoundClasses)
{

  companion object{
    fun isNumberedTypeWithOneOrMoreNumber(type: CangJieType): Boolean {
      val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return false
      val fqName = DescriptorUtils.getFqName(descriptor)
      if (fqName.isRoot) return false

//      if (fqName.parent().toSafe() != CANGJIE_REFLECT_FQ_NAME) return false
      val shortName = descriptor.name.asString()

//      for (prefix in PREFIXES) {
//        if (shortName.startsWith(prefix)) {
//          val number = shortName.removePrefix(prefix)
//          return number.isNotEmpty() && number != "0"
//        }
//      }
      return false
    }
    fun isPossibleExpectedCallableType(typeConstructor: TypeConstructor): Boolean {
      val descriptor = typeConstructor.declarationDescriptor as? ClassDescriptor ?: return false
      if (CangJieBuiltIns.isAny(descriptor)) return true

      val fqName = DescriptorUtils.getFqName(descriptor)
      if (fqName.isRoot) return false




      return false
    }
  }
}
