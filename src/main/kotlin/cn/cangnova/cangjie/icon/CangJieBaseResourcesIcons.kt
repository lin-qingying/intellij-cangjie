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

package cn.cangnova.cangjie.icon

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object CangJieBaseResourcesIcons {

    private fun load(path: String): Icon = IconLoader.getIcon(path, CangJieBaseResourcesIcons::class.java)

    /** 16x16  */

    val CangJie_file: Icon =
        load("/icons/cangjie_file.svg")

    /** 16x16  */

    val Lambda: Icon = load("/icons/lambda.svg")

    /** 16x16  */

    val Abstract_extension_function: Icon = load(
        "/icons/abstract_extension_function.svg",

        )

    /** 16x16  */

    val Field_property: Icon =
        load("/icons/field_property.svg")

    /** 16x16  */

    val Field_property_value: Icon =
        load("/icons/field_property_value.svg")

    /** 16x16  */

    val Field_variable: Icon =
        load("/icons/field_variable.svg")

    /** 16x16  */

    val Field_variable_value: Icon =
        load("/icons/field_variable_value.svg")

    /** 16x16  */

    val AnnotationCangJie: Icon =
        load("/icons/macroCangJie.svg")

    /** 16x16 */
    @JvmField
    val CangJieFile: Icon =
        load("/icons/cangjie_file.svg")

    /** 16x16 */
    @JvmField
    val CangJie: Icon = load("/icons/cangjie_icon_16_16.png")

    @JvmField
    val Toml: Icon = load("/icons/toml.svg")

    /** 16x16 */
    @JvmField
    val ClassCangJie: Icon =
        load("/icons/classCangJie.svg")

    @JvmField
    val InterfaceCangJie: Icon =
        load("/icons/interfaceCangJie.svg")

    @JvmField
    val EnumCangJie: Icon =
        load("/icons/enumCangJie.svg")

    /** 16x16  */

    val AbstractClassCangJie: Icon =
        load("/icons/abstractClassCangJie.svg")

    /** 16x16  */


    val Value: Icon = load("/icons/value.svg")

    /** 16x16  */
    val TypeAlias: Icon =
        load("/icons/typeAlias.svg")

    @JvmField
    val StructCangJie: Icon =
        load("/icons/structCangjie.svg")

    /** 16x16 */
    @JvmField
    val MacroCangJie: Icon = load("/icons/macroCangJie.svg")

}
