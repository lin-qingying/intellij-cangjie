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

package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.resolve.constants.ConstantValue
import com.linqingying.cangjie.types.TypeSubstitutor

interface VariableDescriptor : ValueDescriptor ,MemberDescriptor/*,
   CallableMemberDescriptor, VariableSymbolMarker*/ {


    fun getCompileTimeInitializer(): ConstantValue<*>?  = null

    /**
     * ONLY FOR IDE USE! Please don't use the method inside the compiler
     */
    fun cleanCompileTimeInitializerCache()  {}


    /**
     * @return true if iff original declaration has appropriate flags and type, e.g. `const` modifier in CangJie.
     * It completely does not means that if isConst then `getCompileTimeInitializer` is not null
     */
    //    @Nullable
    //    FieldDescriptor getBackingField();
    //
    //    @Nullable
    //    FieldDescriptor getDelegateField();
    override fun substitute(substitutor: TypeSubstitutor): VariableDescriptor?
    val isConst: Boolean

    //    bool isActual();
    //
    //    bool isExternal();
    val isVar: Boolean
}
