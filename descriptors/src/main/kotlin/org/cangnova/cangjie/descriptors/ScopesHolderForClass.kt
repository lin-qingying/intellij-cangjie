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

package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.resolve.module
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.TypeRefinement
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.storage.getValue
import org.cangnova.cangjie.storage.StorageManager

class ScopesHolderForClass<T : MemberScope> private constructor(
    private val classDescriptor: ClassDescriptor,
    storageManager: StorageManager,
    private val scopeFactory: (CangJieTypeRefiner) -> T,
    private val cangjieTypeRefinerForOwnerModule: CangJieTypeRefiner
) {
    private val scopeForOwnerModule by storageManager.createLazyValue {
        scopeFactory(cangjieTypeRefinerForOwnerModule)
    }

    @OptIn(TypeRefinement::class)
    fun getScope(cangjieTypeRefiner: CangJieTypeRefiner): T {
        /*
         * That check doesn't break anything, because scopeForOwnerModule _will_ anyway refine supertypes from module of
         *   class descriptor.
         *
         * Without that fastpass there is problem wit recursion types such that:
         *
         *   interface A<T : A<T>>
         *
         *   interface B : B<T>
         *
         * In this case (without check) we start compute default type of class descriptor B, go to isRefinementNeededForTypeConstructor,
         *   ask for supertypes of B, see A<B>, ask default type of class B and fail with recursion problem
         */
        if (!cangjieTypeRefiner.isRefinementNeededForModule(classDescriptor.module)) return scopeForOwnerModule

        if (!cangjieTypeRefiner.isRefinementNeededForTypeConstructor(classDescriptor.typeConstructor)) return scopeForOwnerModule
        return cangjieTypeRefiner.getOrPutScopeForClass(classDescriptor) { scopeFactory(cangjieTypeRefiner) }
    }

    companion object {
        @JvmStatic
        fun <T : MemberScope> create(
            classDescriptor: ClassDescriptor,
            storageManager: StorageManager,
            cangjieTypeRefinerForOwnerModule: CangJieTypeRefiner,
            scopeFactory: (CangJieTypeRefiner) -> T
        ): ScopesHolderForClass<T> {
            return ScopesHolderForClass(classDescriptor, storageManager, scopeFactory, cangjieTypeRefinerForOwnerModule)
        }
    }
}
