/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DescriptorVisibilities
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.DefaultTypeSubstitutor

open class LocalVariableDescriptor
    (
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    type: CangJieType?,
    mutable: Boolean,

    source: SourceElement
) : VariableDescriptorImpl(containingDeclaration, name, type, mutable, source, DescriptorVisibilities.LOCAL) {
    override fun substitute(substitutor: DefaultTypeSubstitutor): CallableDescriptor? {
        if (substitutor.isEmpty) return this
        throw UnsupportedOperationException() // TODO
    }


}
