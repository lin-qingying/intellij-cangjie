package icons

import org.cangnova.cangjie.icon.CangJieBaseResourcesIcons as BaseIcons
import javax.swing.Icon

/**
 * 对齐 IntelliJ/Kotlin 文档 icon 反射协议。
 *
 * QuickDoc 中 `<icon src="CangJieBaseResourcesIcons.*">` 会按约定反射加载
 * `icons.CangJieBaseResourcesIcons`。仓颉现有图标类位于
 * `org.cangnova.cangjie.icon` 包下，因此这里提供同名桥接入口。
 */
object CangJieBaseResourcesIcons {
    @JvmField
    val CangJie_file: Icon = BaseIcons.CangJie_file

    @JvmField
    val Lambda: Icon = BaseIcons.Lambda

    @JvmField
    val Abstract_extension_function: Icon = BaseIcons.Abstract_extension_function

    @JvmField
    val Field_property: Icon = BaseIcons.Field_property

    @JvmField
    val Field_property_value: Icon = BaseIcons.Field_property_value

    @JvmField
    val Field_variable: Icon = BaseIcons.Field_variable

    @JvmField
    val Field_variable_value: Icon = BaseIcons.Field_variable_value

    @JvmField
    val AnnotationCangJie: Icon = BaseIcons.AnnotationCangJie

    @JvmField
    val CangJieFile: Icon = BaseIcons.CangJieFile

    @JvmField
    val CangJie: Icon = BaseIcons.CangJie

    @JvmField
    val Toml: Icon = BaseIcons.Toml

    @JvmField
    val ClassCangJie: Icon = BaseIcons.ClassCangJie

    @JvmField
    val InterfaceCangJie: Icon = BaseIcons.InterfaceCangJie

    @JvmField
    val EnumCangJie: Icon = BaseIcons.EnumCangJie

    @JvmField
    val AbstractClassCangJie: Icon = BaseIcons.AbstractClassCangJie

    @JvmField
    val Value: Icon = BaseIcons.Value

    @JvmField
    val TypeAlias: Icon = BaseIcons.TypeAlias

    @JvmField
    val StructCangJie: Icon = BaseIcons.StructCangJie

    @JvmField
    val MacroCangJie: Icon = BaseIcons.MacroCangJie
}
