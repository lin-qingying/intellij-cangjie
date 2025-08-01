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

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.PackageFragmentDescriptor
import cn.cangnova.cangjie.descriptors.enumd.EnumEntryDescriptor
import cn.cangnova.cangjie.descriptors.impl.ClassConstructorDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.EnumEntryConstructorDescriptor
import cn.cangnova.cangjie.incremental.components.NoLookupLocation
import cn.cangnova.cangjie.psi.CjEnum
import cn.cangnova.cangjie.psi.CjEnumEntry
import cn.cangnova.cangjie.resolve.calls.components.InferenceSession
import cn.cangnova.cangjie.resolve.lazy.LazyClassContext
import cn.cangnova.cangjie.resolve.lazy.data.CjEnmuEntryInfo

import cn.cangnova.cangjie.resolve.scopes.LexicalScope
import cn.cangnova.cangjie.resolve.scopes.LexicalScopeKind
import cn.cangnova.cangjie.resolve.scopes.LexicalWritableScope
import cn.cangnova.cangjie.resolve.scopes.LocalRedeclarationChecker
import cn.cangnova.cangjie.resolve.source.toSourceElement

class EnumDescriptorResolver(
    private val typeResolver: TypeResolver,
    private val builtIns: CangJieBuiltIns,
    private val storageManager: StorageManager

) {
    fun resolbeEnumEntryConstructorDescriptor(
        scope: LexicalScope,
        classDescriptor: ClassDescriptor,
        entry: CjEnumEntry,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
        inferenceSession: InferenceSession?,
    ): ClassConstructorDescriptorImpl {

        val parameterScope = LexicalWritableScope(
            scope,
            classDescriptor,
            false,
            LocalRedeclarationChecker.DO_NOTHING,
            LexicalScopeKind.CONSTRUCTOR_HEADER
        )
        val types = entry.typeReferences.map {
            typeResolver.resolveType(parameterScope, it, trace, true)
        }


        return EnumEntryConstructorDescriptor(classDescriptor, null, entry.toSourceElement()) {
            types
        }

    }

    fun resolveEnumEntryDescriptor(
        c: LazyClassContext,
        thisDescriptor: DeclarationDescriptor,
        name: Name,
        info: CjEnmuEntryInfo,
        external: Boolean
    ): EnumEntryDescriptor {
//        TODO 在这里校验还是在 LazyEnumEntryDescriptor的构造函数中校验？
        return when (thisDescriptor) {
            is PackageFragmentDescriptor -> {
                val descriptor = (info.correspondingClass.parent?.parent as? CjEnum)?.nameAsSafeName?.let {
                    thisDescriptor.getMemberScope().getContributedClassifier(
                        it,
                        NoLookupLocation.FROM_IDE
                    )
                }

                EnumEntryDescriptor(c, descriptor ?: thisDescriptor, name, info)

            }

            else -> {
                EnumEntryDescriptor(c, thisDescriptor, name, info)

            }
        }
    }

}
