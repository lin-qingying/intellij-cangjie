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

package org.cangnova.cangjie.types.error

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.ClassConstructorDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.ClassDescriptorImpl
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.storage.LockBasedStorageManager
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner

class ErrorClassDescriptor(name: Name) : ClassDescriptorImpl(
    ErrorUtils.errorModule, name, Modality.OPEN, ClassKind.CLASS, emptyList(), SourceElement.NO_SOURCE, false, LockBasedStorageManager.NO_LOCKS

) {
    override fun getMemberScope(typeArguments: List<TypeProjection>, cangjieTypeRefiner: CangJieTypeRefiner): MemberScope =
        ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_CLASS, name.toString(), typeArguments.toString())

    override fun getMemberScope(typeSubstitution: TypeSubstitution, cangjieTypeRefiner: CangJieTypeRefiner): MemberScope =
        ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_CLASS, name.toString(), typeSubstitution.toString())






    init {
        val errorConstructor = ClassConstructorDescriptorImpl.create(this, Annotations.EMPTY, true, SourceElement.NO_SOURCE)
            .apply {
                initialize(
                    emptyList(),
                    DescriptorVisibilities.INTERNAL
                )
            }
        val memberScope = ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_CLASS, errorConstructor.name.toString(), "")
       errorConstructor.setReturnType(ErrorType(
           ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.ERROR_CLASS),
           memberScope,
           ErrorTypeKind.ERROR_CLASS
       ))

        initialize(memberScope, listOf(errorConstructor), errorConstructor, emptyList())
    }



    override fun substitute(substitutor: TypeSubstitutor): ClassifierDescriptorWithTypeParameters = this
    override fun toString(): String = name.asString()

}
