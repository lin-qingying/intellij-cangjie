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
import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.descriptors.ClassDescriptorWithResolutionScopes
import cn.cangnova.cangjie.descriptors.ClassKind
import cn.cangnova.cangjie.diagnostics.Errors
import cn.cangnova.cangjie.incremental.components.NoLookupLocation
import cn.cangnova.cangjie.psi.CjExtend
import cn.cangnova.cangjie.resolve.lazy.FileScopeProvider
import cn.cangnova.cangjie.resolve.lazy.LazyDeclarationResolver
import cn.cangnova.cangjie.resolve.lazy.declarations.AbstractLazyMemberScope
import cn.cangnova.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor

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


        val type  = scope.resolveTypeByExtend(cjExtend)

        return type
    }

    fun check(c: TopDownAnalysisContext, cjExtend: CjExtend) {
        val type = getExtendDescriptor(cjExtend)
        type
        type as LazyExtendClassDescriptor
        if (type.getSourceClassKind() == ClassKind.INTERFACE) {

            cjExtend.receiverTypeReceiver?.let { trace.report(Errors.EXTEND_CANNOT_INTERFACE.on(it)) }
            return
        }

        c.declaredClasses[cjExtend] = type

//        val classDescriptor = type.constructor.declarationDescriptor as? LazyClassDescriptor ?: return


//        classDescriptor.setExtendData(cjExtend,trace,lexicalScope)
//        classDescriptor as LazyClassDescriptor
//        c.declaredClasses[cjExtend] = classDescriptor


//        第二步，获取被扩展的类型


//

    }
}
