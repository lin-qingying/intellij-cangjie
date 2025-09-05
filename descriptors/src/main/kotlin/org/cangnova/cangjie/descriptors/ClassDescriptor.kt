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



import org.cangnova.cangjie.psi.CjSuperTypeListEntry
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.types.TypeProjection
import org.cangnova.cangjie.types.TypeSubstitution

interface ClassDescriptor : ClassifierDescriptorWithTypeParameters, ClassOrPackageFragmentDescriptor
   {
    fun getMemberScope(typeArguments: List<TypeProjection>): MemberScope

    val thisAsReceiverParameter: ReceiverParameterDescriptor


    val contextReceivers: List<ReceiverParameterDescriptor>

    fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope

    val unsubstitutedMemberScope: MemberScope

//    val unsubstitutedInnerClassesScope: MemberScope

    val instanceScope: MemberScope
        get() = MemberScope.Empty

    val staticScope: MemberScope


    val constructors: Collection<ClassConstructorDescriptor>


    val endConstructors: Collection<ClassConstructorDescriptor>


    override val containingDeclaration: DeclarationDescriptor

    /**
     * @return type A&lt;T&gt; for the class A&lt;T&gt;
     */

    override val defaultType: SimpleType

    /**
     * @return nested object declared as 'companion' if one is present.
     */

    val kind: ClassKind


    override val modality: Modality

    override val visibility: DescriptorVisibility





    val  unsubstitutedPrimaryConstructor : ClassConstructorDescriptor?

    /**
     * It may differ from 'typeConstructor.parameters' in current class is inner, 'typeConstructor.parameters' contains
     * captured parameters from outer declaration.
     *
     * @return list of type parameters actually declared type parameters in current class
     */


    override val declaredTypeParameters: List<TypeParameterDescriptor>

    /**
     * @return direct subclasses of this class if it's a sealed class, empty list otherwise
     */

    val sealedSubclasses : Collection<ClassDescriptor>

    override val original: ClassifierDescriptor

    // Use SingleAbstractMethodUtils.getFunctionTypeForSamInterface() where possible. This is only a fallback
    val defaultFunctionTypeForSamInterface : SimpleType?

    /**
     * May return false even in case when the class is not SAM interface, but returns true only if it's definitely not a SAM.
     * But it should work much faster than the exact check.
     */
    val isDefinitelyNotSamInterface : Boolean

    /**
     * 获取所有SuperTypeListEntry 包含扩展
     */
    fun getSuperTypeListEntries(): MutableList<CjSuperTypeListEntry> {
        return mutableListOf<CjSuperTypeListEntry>()
    }

    /**
     * 是否具有const 构造函数
     */
    fun hasConstConstructor(): Boolean {
        val constructors =
            this.constructors
        for (constructor in constructors) {
            if (constructor.isConst) {
                return true
            }
        }


        return false
    }
}
