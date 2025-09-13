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

import org.cangnova.cangjie.descriptors.impl.PropertyAccessorDescriptor
import org.cangnova.cangjie.resolve.constants.ConstantValue
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeSubstitutor


interface PropertyDescriptor :EnumMember, PropertyDescriptorWithAccessors, CallableMemberDescriptor {

    override val getter: PropertyGetterDescriptor?


    override val setter: PropertySetterDescriptor?


    val accessors: List<PropertyAccessorDescriptor>

    /**
     * In the following case, the setter is projected out:
     *
     * trait Tr<T> { var v: T }
     * fun test(tr: Tr<out String>) {
     * tr.v = null!! // the assignment is illegal, although a read would be fine
     * }
    </out></T> */
    val isSetterProjectedOut: Boolean

    //    @Override
    //    @Nullable
    //    PropertySetterDescriptor getSetter();
    //    @NotNull
    //    List<PropertyAccessorDescriptor> getAccessors();
    override val original: PropertyDescriptor


    override val overriddenDescriptors: Collection<PropertyDescriptor>

    override fun getCompileTimeInitializer(): ConstantValue<*>?


    override fun substitute(substitutor: TypeSubstitutor): PropertyDescriptor

    override fun newCopyBuilder(): CallableMemberDescriptor.CopyBuilder<out PropertyDescriptor>

    val inType: CangJieType?
}
