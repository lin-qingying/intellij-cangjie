package com.huawei.cangjie.icon


import com.intellij.openapi.util.IconLoader
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.IconManager
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.Icon

object CangJieIcons {
    /** 16x16  */
//    val FILE: Icon = CangJieResourcesIcons.CangJie_file

    val CANGJIE_FILE = load("/icons/cangjie_file.svg")
    val CANGJIE = load("/icons/cangjie_icon.png")
    val CANGJIE_16 = load("/icons/cangjie_icon_16_16.png")


    /** 16x16  */

    /** 16x16  */
    val Class: Icon =
        load("/icons/class.svg")
    val GEAR = CANGJIE_FILE
    val GEAR_OFF = CANGJIE_FILE

    //    val GEAR = load("/icons/gear.svg")
//    val GEAR_OFF = load("/icons/gearOff.svg")
    val GEAR_ANIMATED =
        AnimatedIcon(AnimatedIcon.Default.DELAY, GEAR, GEAR.rotated(15.0), GEAR.rotated(30.0), GEAR.rotated(45.0))

    private fun load(path: String): Icon = IconLoader.getIcon(path, CangJieIcons::class.java)

//    private fun load(path: String, cacheKey: Int, flags: Int): Icon {
//        return IconManager.getInstance()
//            .loadRasterizedIcon(path, CangJieIcons::class.java.classLoader, cacheKey, flags)
//    }

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
