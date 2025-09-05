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

package org.cangnova.cangjie.buildsystem.impl.cjpm.runconfig

import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.ide.ui.LafManager

import com.intellij.ide.ui.laf.UIThemeLookAndFeelInfoImpl
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.math.sqrt


fun AnsiEscapeDecoder.removeEscapeSequences(text: String): String {
    val chunks = mutableListOf<String>()
    escapeText(text, ProcessOutputTypes.STDOUT) { chunk, _ ->
        chunks.add(chunk)
    }
    return chunks.joinToString("")
}
class CjAnsiEscapeDecoder: AnsiEscapeDecoder() {
    override fun escapeText(text: String, outputType: Key<*>, textAcceptor: ColoredTextAcceptor) {
        super.escapeText(quantizeAnsiColors(text), outputType, textAcceptor)
    }

    companion object {
        const val CSI: String = "\u001B[" // "Control Sequence Initiator"

        @JvmField
        val ANSI_SGR_RE: Regex = """${StringUtil.escapeToRegexp(CSI)}(\d+(;\d+)*)m""".toRegex()

        private const val ANSI_SET_FOREGROUND_ATTR: Int = 38
        private const val ANSI_SET_BACKGROUND_ATTR: Int = 48

        const val ANSI_24_BIT_COLOR_FORMAT: Int = 2
        const val ANSI_8_BIT_COLOR_FORMAT: Int = 5

        /**
         * Parses ANSI-value codes from text and replaces 8-bit and 24-bit colors with nearest (in Euclidean space)
         * 4-bit value.
         *
         * @param text a string with ANSI escape sequences
         */
        @NlsSafe
        fun quantizeAnsiColors(text: String): String = text
            .replace(ANSI_SGR_RE) {
                val rawAttributes = it.destructured.component1().split(";").iterator()
                val result = mutableListOf<Int>()
                while (rawAttributes.hasNext()) {
                    val attribute = rawAttributes.parseAttribute() ?: continue
                    if (attribute !in listOf(ANSI_SET_FOREGROUND_ATTR, ANSI_SET_BACKGROUND_ATTR)) {
                        result.add(attribute)
                        continue
                    }
                    val color = parseColor(rawAttributes) ?: continue
                    val ansiColor = getNearestAnsiColor(color) ?: continue
                    val colorAttribute = getColorAttribute(ansiColor, attribute == ANSI_SET_FOREGROUND_ATTR)
                    result.add(colorAttribute)
                }
                result.joinToString(separator = ";", prefix = CSI, postfix = "m") { attr -> attr.toString() }
            }

        private fun Iterator<String>.parseAttribute(): Int? = nextOrNull()?.toIntOrNull()

        private fun parseColor(rawAttributes: Iterator<String>): Color? {
            val format = rawAttributes.parseAttribute() ?: return null
            return when (format) {
                ANSI_24_BIT_COLOR_FORMAT -> parse24BitColor(rawAttributes)
                ANSI_8_BIT_COLOR_FORMAT -> parse8BitColor(rawAttributes)
                else -> null
            }
        }

        private fun parse24BitColor(rawAttributes: Iterator<String>): Color? {
            val red = rawAttributes.parseAttribute() ?: return null
            val green = rawAttributes.parseAttribute() ?: return null
            val blue = rawAttributes.parseAttribute() ?: return null
            return Color(red, green, blue)
        }

        private fun parse8BitColor(rawAttributes: Iterator<String>): Color? {
            val attribute = rawAttributes.parseAttribute() ?: return null
            return when (attribute) {
                // Standard colors or high intensity colors
                in 0..15 -> Ansi4BitColor[attribute]?.value

                // 6 × 6 × 6 cube (216 colors): 16 + 36 × r + 6 × g + b (0 ≤ r, g, b ≤ 5)
                in 16..231 -> {
                    val red = (attribute - 16) / 36 * 51
                    val green = (attribute - 16) % 36 / 6 * 51
                    val blue = (attribute - 16) % 6 * 51
                    Color(red, green, blue)
                }

                // Grayscale from black to white in 24 steps
                in 232..255 -> {
                    val value = (attribute - 232) * 10 + 8
                    Color(value, value, value)
                }

                else -> null
            }
        }

        private fun getNearestAnsiColor(color: Color): Ansi4BitColor? =
            Ansi4BitColor.values().minByOrNull { calcEuclideanDistance(it.value, color) }

        private fun calcEuclideanDistance(from: Color, to: Color): Int {
            val redDiff = from.red.toDouble() - to.red
            val greenDiff = from.green.toDouble() - to.green
            val blueDiff = from.blue.toDouble() - to.blue
            return sqrt(redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff).roundToInt()
        }

        private fun getColorAttribute(realAnsiColor: Ansi4BitColor, isForeground: Boolean): Int {
               val isForcedWhiteFontUnderLightTheme = realAnsiColor == Ansi4BitColor.BRIGHT_WHITE &&
                    isForeground && SystemInfo.isWindows && !isUnderDarkTheme
            val ansiColor = if (isForcedWhiteFontUnderLightTheme) {
                Ansi4BitColor.BLACK
            } else {
                realAnsiColor
            }
            val colorIndex = ansiColor.index
            return when {
                colorIndex in 0..7 && isForeground -> colorIndex + 30
                colorIndex in 0..7 && !isForeground -> colorIndex + 40
                colorIndex in 8..15 && isForeground -> colorIndex + 82
                colorIndex in 8..15 && !isForeground -> colorIndex + 92
                else -> error("impossible")
            }
        }

        private enum class Ansi4BitColor(val value: Color) {
            BLACK(Gray._0),
            RED(Color(128, 0, 0)),
            GREEN(Color(0, 128, 0)),
            YELLOW(Color(128, 128, 0)),
            BLUE(Color(0, 0, 128)),
            MAGENTA(Color(128, 0, 128)),
            CYAN(Color(0, 128, 128)),
            WHITE(Gray._192),
            BRIGHT_BLACK(Gray._128),
            BRIGHT_RED(Color(255, 0, 0)),
            BRIGHT_GREEN(Color(0, 255, 0)),
            BRIGHT_YELLOW(Color(255, 255, 0)),
            BRIGHT_BLUE(Color(0, 0, 255)),
            BRIGHT_MAGENTA(Color(255, 0, 255)),
            BRIGHT_CYAN(Color(0, 255, 255)),
            BRIGHT_WHITE(Gray._255);

            val index: Int get() = values().indexOf(this)

            companion object {
                operator fun get(index: Int): Ansi4BitColor? {
                    val values = values()
                    return if (index >= 0 && index < values.size) values[index] else null
                }
            }
        }
    }
}
fun <T : Any> Iterator<T>.nextOrNull(): T? =
    if (hasNext()) next() else null
val isUnderDarkTheme: Boolean
    get() {
        val lookAndFeel = LafManager.getInstance().currentUIThemeLookAndFeel as? UIThemeLookAndFeelInfoImpl
        return lookAndFeel?.theme?.isDark == true || JBColor.isBright()
    }
