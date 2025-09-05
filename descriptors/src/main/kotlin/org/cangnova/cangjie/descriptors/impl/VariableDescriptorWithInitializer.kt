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

package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.constants.ConstantValue
import org.cangnova.cangjie.storage.NullableLazyValue
import org.cangnova.cangjie.types.CangJieType

abstract class VariableDescriptorWithInitializerImpl(
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    outType: CangJieType?,
    override val isVar: Boolean,
    source: SourceElement
) : AbstractVariableDescriptor(containingDeclaration, annotations, name, outType, source) {
    private var compileTimeInitializer: NullableLazyValue<ConstantValue<*>>? =
        null
    private var compileTimeInitializerFactory: () -> NullableLazyValue<ConstantValue<*>>? =
        {
            null
        }

    fun setCompileTimeInitializerFactory(compileTimeInitializerFactory: () -> NullableLazyValue<ConstantValue<*>>) {
        assert(!isVar) { "Constant value for variable initializer should be recorded only for final variables: $name" }
        setCompileTimeInitializer(null, compileTimeInitializerFactory)
    }

    override fun cleanCompileTimeInitializerCache() {
        this.compileTimeInitializer = compileTimeInitializerFactory.invoke()
    }

    override fun getCompileTimeInitializer(): ConstantValue<*>? {
        if (compileTimeInitializer != null) {
            return compileTimeInitializer!!.invoke()
        }
        return null
    }



    fun setCompileTimeInitializer(
        compileTimeInitializer: NullableLazyValue<ConstantValue<*>>?,
        compileTimeInitializerFactory: () -> NullableLazyValue<ConstantValue<*>>
    ) {
        assert(!isVar) { "Constant value for variable initializer should be recorded only for final variables: " + name }
        this.compileTimeInitializerFactory = compileTimeInitializerFactory
        this.compileTimeInitializer =
            compileTimeInitializer ?: compileTimeInitializerFactory.invoke()
    }
}
