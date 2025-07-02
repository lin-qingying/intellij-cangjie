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

package cn.cangnova.cangjie.descriptors

import cn.cangnova.cangjie.mpp.ValueParameterSymbolMarker
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.types.CangJieType

interface ValueParameterDescriptor : VariableDescriptor, ParameterDescriptor, ValueParameterSymbolMarker {
    override val original: ValueParameterDescriptor
//    val varargElementType: CangJieType?

    override val containingDeclaration: CallableDescriptor

    val varargElementType: CangJieType? get() = null

    val isNamed: Boolean

    /**
     * Returns the 0-based index of the value parameter in the parameter list of its containing function.

     * @return the parameter index
     */
    val index: Int

    /**
     * Parameter p1 overrides p2 iff
     * a) their respective owners (function declarations) f1 override f2
     * b) p1 and p2 have the same indices in the owners' parameter lists
     */
    override fun getOverriddenDescriptors(): Collection<ValueParameterDescriptor>

    /**
     * @return true iff this parameter belongs to a declared function (not a fake override) and declares the default value,
     * i.e. explicitly specifies it in the function signature. Also see 'hasDefaultValue' extension in DescriptorUtils.cj
     */
    fun declaresDefaultValue(): Boolean
    fun copy(newOwner: CallableDescriptor, newName: Name, newIndex: Int): ValueParameterDescriptor

}
