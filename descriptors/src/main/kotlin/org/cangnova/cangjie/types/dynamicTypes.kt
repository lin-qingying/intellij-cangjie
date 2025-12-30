/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.container.DefaultImplementation
import org.cangnova.cangjie.container.PlatformSpecificExtension
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.renderer.DescriptorRendererOptions
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.model.DynamicTypeMarker


@DefaultImplementation(impl = DynamicTypesSettings::class)
open class DynamicTypesSettings : PlatformSpecificExtension<DynamicTypesSettings> {
    open val dynamicTypesAllowed: Boolean
        get() = false
}

class DynamicTypesAllowed : DynamicTypesSettings() {
    override val dynamicTypesAllowed: Boolean
        get() = true
}

fun CangJieType.isDynamic(): Boolean = unwrap() is DynamicType

fun createDynamicType(builtIns: CangJieBuiltIns) = DynamicType(builtIns, TypeAttributes.Empty)

class DynamicType(
    builtIns: CangJieBuiltIns,
    override val attributes: TypeAttributes
) : FlexibleType(builtIns.nothingType, builtIns.stdlibTypes.anyType), DynamicTypeMarker {
    override val delegate: SimpleType get() = upperBound


    override val isOption: Boolean get() = false

    override fun replaceAttributes(newAttributes: TypeAttributes): DynamicType =
        DynamicType(delegate.builtIns, newAttributes)


    // Option has no effect on dynamics
    override fun makeOptionAsSpecified(isOption: Boolean): UnwrappedType = this


    override fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions): String = "dynamic"

    
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) = this
}
