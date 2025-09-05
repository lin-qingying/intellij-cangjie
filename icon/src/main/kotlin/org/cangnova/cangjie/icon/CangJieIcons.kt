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

package org.cangnova.cangjie.icon

import com.intellij.icons.AllIcons
import com.intellij.ui.AnimatedIcon
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.Icon

object CangJieIcons {
    /** 16x16  */
//    val FILE: Icon = CangJieResourcesIcons.CangJie_file

    val CANGJIE_FILE = CangJieBaseResourcesIcons.CangJieFile

    /** 16x16  */
    @JvmField
    val FILE: Icon = CangJieBaseResourcesIcons.CangJie_file

    @JvmField
    val CANGJIE = CangJieBaseResourcesIcons.CangJie

    @JvmField
    val TOML = CangJieBaseResourcesIcons.Toml

    /** 16x16  */
    val ANNOTATION: Icon = CangJieBaseResourcesIcons.AnnotationCangJie

    /** 16x16  */
    val TYPE_ALIAS: Icon = CangJieBaseResourcesIcons.TypeAlias

    /** 16x16  */
    val CLASS: Icon = CangJieBaseResourcesIcons.ClassCangJie

    /** 16x16  */
    val INTERFACE: Icon = CangJieBaseResourcesIcons.InterfaceCangJie

    /** 16x16  */
    val ENUM = CangJieBaseResourcesIcons.EnumCangJie

    /** 16x16  */
    val STRUCT = CangJieBaseResourcesIcons.StructCangJie

    /** 16x16  */
    val ABSTRACT_CLASS: Icon = CangJieBaseResourcesIcons.AbstractClassCangJie

    val GEAR = CANGJIE_FILE
    val GEAR_OFF = CANGJIE_FILE

    val VAR: Icon = AllIcons.Nodes.Variable

    /** 16x16  */
    val FIELD_MPROP: Icon = CangJieBaseResourcesIcons.Field_property

    /** 16x16  */
    val FIELD_PROP: Icon = CangJieBaseResourcesIcons.Field_property_value

    /** 16x16  */
    val FIELD_VAR: Icon = CangJieBaseResourcesIcons.Field_variable

    /** 16x16  */
    val FIELD_LET: Icon = CangJieBaseResourcesIcons.Field_variable_value

    val FUNCTION: Icon = AllIcons.Nodes.Function

    /** 16x16  */
    val LET: Icon = CangJieBaseResourcesIcons.Value

    val GEAR_ANIMATED =
        AnimatedIcon(AnimatedIcon.Default.DELAY, GEAR, GEAR.rotated(15.0), GEAR.rotated(30.0), GEAR.rotated(45.0))
    val EXTENSION_FUNCTION: Icon = AllIcons.Nodes.Function

    /** 16x16  */
    val ABSTRACT_EXTENSION_FUNCTION: Icon = CangJieBaseResourcesIcons.Abstract_extension_function

    val PARAMETER: Icon = AllIcons.Nodes.Parameter

    /** 16x16  */
    val LAMBDA: Icon = CangJieBaseResourcesIcons.Lambda
}

/**
 * Rotates the icon by the given angle, in degrees.
 *
 * **Important**: Do ***not*** rotate the icon by ±90 degrees (or any sufficiently close amount)!
 * The implementation of rotation by that amount in AWT is broken, and results in erratic shifts for composed
 * transformations. In other words, the (final) transformation matrix as a function of rotation angle
 * is discontinuous at those points.
 */
fun Icon.rotated(angle: Double): Icon {
    val q = this
    return object : Icon by this {
        override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
            val g2d = g.create() as Graphics2D
            try {
                g2d.translate(x.toDouble(), y.toDouble())
                g2d.rotate(Math.toRadians(angle), iconWidth / 2.0, iconHeight / 2.0)
                q.paintIcon(c, g2d, 0, 0)
            } finally {
                g2d.dispose()
            }
        }
    }
}
