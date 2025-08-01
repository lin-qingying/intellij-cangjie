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

package cn.cangnova.cangjie.descriptors.impl

import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.DeclarationDescriptorVisitor
import cn.cangnova.cangjie.descriptors.annotations.AnnotatedImpl
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.renderer.DescriptorRenderer

abstract class DeclarationDescriptorImpl(
    annotations: Annotations,
    override val  name: Name
):  AnnotatedImpl(annotations), DeclarationDescriptor {
    override val original: DeclarationDescriptor
        get() = this
    override fun toString(): String {
        return toString(this)
    }

    fun toString(descriptor: DeclarationDescriptor): String {
        return try {
        DescriptorRenderer.  DEBUG_TEXT.render(descriptor) +
                    "[" + descriptor.javaClass.simpleName + "@" + Integer.toHexString(
                System.identityHashCode(
                    descriptor
                )
            ) + "]"
        } catch (e: Throwable) {
            // DescriptionRenderer may throw if this is not yet completely initialized
            // It is very inconvenient while debugging
            descriptor.javaClass.getSimpleName() + " " + descriptor.name
        }
    }
    override fun acceptVoid(visitor:  DeclarationDescriptorVisitor<Void, Void>) {
        accept(visitor, null)
    }

}
