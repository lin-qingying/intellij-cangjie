/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.PackageFragmentDescriptor
import org.cangnova.cangjie.descriptors.data.CjEnmuEntryInfo
import org.cangnova.cangjie.descriptors.impl.ClassConstructorDescriptorImpl
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjEnumEntry
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.lazy.LazyClassContext

import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.LexicalScopeKind
import org.cangnova.cangjie.resolve.scopes.LexicalWritableScope
import org.cangnova.cangjie.resolve.scopes.LocalRedeclarationChecker
import org.cangnova.cangjie.resolve.source.toSourceElement
import org.cangnova.cangjie.storage.StorageManager

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
        entry.typeReferences.map {
            typeResolver.resolveType(parameterScope, it, trace, true)
        }


        TODO("等待重构枚举构造器")

    }


}
