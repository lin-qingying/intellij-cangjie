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

package cn.cangnova.cangjie.types

import cn.cangnova.cangjie.descriptors.annotations.Annotations
import kotlin.reflect.KClass
import cn.cangnova.cangjie.descriptors.annotations.composeAnnotations
val TypeAttributes.annotations: Annotations get() = annotationsAttribute?.annotations ?: Annotations.EMPTY
val TypeAttributes.annotationsAttribute: AnnotationsTypeAttribute? by TypeAttributes.attributeAccessor<AnnotationsTypeAttribute>()

class AnnotationsTypeAttribute(val annotations: Annotations) : TypeAttribute<AnnotationsTypeAttribute>() {
    override fun union(other: AnnotationsTypeAttribute?): AnnotationsTypeAttribute? =
        if (other == this) this else null

    override fun intersect(other: AnnotationsTypeAttribute?): AnnotationsTypeAttribute? =
        if (other == this) this else null

    override fun add(other: AnnotationsTypeAttribute?): AnnotationsTypeAttribute {
        if (other == null) return this
        return AnnotationsTypeAttribute(composeAnnotations(annotations, other.annotations))
    }

    override fun isSubtypeOf(other: AnnotationsTypeAttribute?): Boolean = true

    override val key: KClass<out AnnotationsTypeAttribute>
        get() = AnnotationsTypeAttribute::class

    override fun equals(other: Any?): Boolean {
        if (other !is AnnotationsTypeAttribute) return false
        return other.annotations == this.annotations
    }

    override fun hashCode(): Int = annotations.hashCode()
}
