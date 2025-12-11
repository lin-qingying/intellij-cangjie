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
import org.cangnova.cangjie.descriptors.ClassDescriptorWithResolutionScopes
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.lazy.FileScopeProvider
import org.cangnova.cangjie.resolve.lazy.LazyDeclarationResolver
import org.cangnova.cangjie.resolve.lazy.declarations.AbstractLazyMemberScope
import org.cangnova.cangjie.storage.StorageManager

class ExtendDescriptorResolver(
    private val trace: BindingTrace,
    private val lazyDeclarationResolver: LazyDeclarationResolver,
    private val fileScopeProvider: FileScopeProvider,
    private val descriptorResolver: DescriptorResolver,

    private val typeResolver: TypeResolver,

    private val overloadChecker: OverloadChecker,
    private val builtIns: CangJieBuiltIns,

    private val storageManager: StorageManager

) {

    fun getExtendDescriptor(cjExtend: CjExtend): ClassDescriptorWithResolutionScopes {
        //        val receiverTypeReceiver = cjExtend.receiverTypeReceiver ?: return
        val scope = lazyDeclarationResolver.getMemberScopeDeclaredIn(cjExtend, NoLookupLocation.FROM_BUILTINS)
//        val lexicalScope = fileScopeProvider.getFileResolutionScope(cjExtend.getContainingCjFile())


        scope as AbstractLazyMemberScope<*, *>


        val type = scope.resolveTypeByExtend(cjExtend)

        return type
    }


}
