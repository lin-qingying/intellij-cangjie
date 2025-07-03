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

import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.TypeSubstitutor

interface ConstructorDescriptor : FunctionDescriptor {

    val isPrimary: Boolean
    val isEnd: Boolean

    override val returnType: CangJieType
    val constructedClass: ClassDescriptor

    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: DescriptorVisibility?,
        kind: CallableMemberDescriptor.Kind?,
        copyOverrides: Boolean
    ): ConstructorDescriptor

    //    @NotNull
    //    @Override
    //    CangJieType getReturnType();
    override fun substitute(substitutor: TypeSubstitutor): ConstructorDescriptor


    override val original: ConstructorDescriptor


    override val containingDeclaration: ClassifierDescriptorWithTypeParameters
}
