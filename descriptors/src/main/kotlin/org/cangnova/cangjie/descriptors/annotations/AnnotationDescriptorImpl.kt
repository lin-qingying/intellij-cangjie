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
package org.cangnova.cangjie.descriptors.annotations

import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.resolve.constants.ConstantValue
import org.cangnova.cangjie.types.CangJieType


class AnnotationDescriptorImpl(
    override val type: CangJieType,
    override val allValueArguments: Map<Name, ConstantValue<*>>,
    override val source: SourceElement
) : AnnotationDescriptor {
    override fun toString(): String {
        return DescriptorRenderer.FQ_NAMES_IN_TYPES.renderAnnotation(this, null)
    }
}
