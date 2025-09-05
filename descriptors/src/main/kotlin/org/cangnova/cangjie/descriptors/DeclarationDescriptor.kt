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

import org.cangnova.cangjie.descriptors.annotations.Annotated

interface DeclarationDescriptor : Annotated,
    Named,
    ValidateableDescriptor {
    /**
     * @return The descriptor that corresponds to the original declaration of this element.
     * A descriptor can be obtained from its original by substituting type arguments (of the declaring class
     * or of the element itself).
     * returns `this` object if the current descriptor is original itself
     */
    val original: DeclarationDescriptor

    val containingDeclaration: DeclarationDescriptor?

    val visibility: DescriptorVisibility get() = DescriptorVisibilities.PUBLIC
    val isTopLevel: Boolean get() = false
    val isLocal get() = visibility == DescriptorVisibilities.LOCAL
    fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R?

    val isStatic: Boolean get() = false

    fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>)
}
