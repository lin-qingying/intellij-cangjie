package com.huawei.cangjie.icon

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
