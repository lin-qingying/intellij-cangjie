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

package com.linqingying.cangjie.descriptors.enumd

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.diagnostics.Errors.ENUM_REDECLARATION
import com.linqingying.cangjie.diagnostics.Errors.REDECLARATION
import com.linqingying.cangjie.diagnostics.reportOnDeclaration
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjEnumEntry
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.lazy.LazyClassContext
import com.linqingying.cangjie.resolve.lazy.data.CjClassLikeInfo
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyClassDescriptor

class LazyEnumDescriptor(
    c: LazyClassContext,
    classLikeInfo: CjClassLikeInfo,

    containingDeclaration: DeclarationDescriptor,
    name: Name,


    ) : LazyClassDescriptor(
    c,
    containingDeclaration, name,
    classLikeInfo, false
) {

init {
    checkArgumentBySize()
}
//    不能在构造器中调用，会引发循环调用，因为只能自底向上解析
    val entrys
        get() = unsubstitutedMemberScope.getContributedDescriptors { true }.filter { DescriptorUtils.isEnumEntry(it) }

    val entrysByNameGroup get() = entrys.groupBy { it.name }

    val entrysByPsi get() = classLikeInfo.declarations.filterIsInstance<CjEnumEntry>()
    val entrysByPsiByNameGroup get() = entrysByPsi.groupBy { it.name }
    fun checkArgumentBySize() {
        entrysByPsiByNameGroup .forEach { (name, entrys) ->

            val map =      entrys.groupBy {  it.typeReferences.size}
            map.values.forEach { values ->
                if (values.size > 1)

                  values.forEach {
                      c.trace.report(ENUM_REDECLARATION.on(it, name) )
                  }


            }
        }
//        entrysByNameGroup.forEach { (name, declarationDescriptors) ->
//
//            val map =      declarationDescriptors.groupBy {  (it as EnumEntryDescriptor).unsubstitutedPrimaryConstructor.valueParameters.size}
//            map.values.forEach { values ->
//                if (values.size > 1)
//                    reportOnDeclaration(c.trace, values.first()) {
//                        REDECLARATION.on(it, values)
//                    }
//
//            }
//        }


    }

}
