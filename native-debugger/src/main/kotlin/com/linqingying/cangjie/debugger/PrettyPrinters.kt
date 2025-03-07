/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package cn.cangnova.cangjie.debugger

import cn.cangnova.cangjie.openapiext.CjPathManager
import org.jetbrains.annotations.PropertyKey


val PP_PATH: String get() = CjPathManager.prettyPrintersDir().toString()
const val LLDB_LOOKUP: String = "lldb_lookup"
const val GDB_LOOKUP: String = "gdb_formatters.gdb_lookup"

enum class LLDBRenderers(@PropertyKey(resourceBundle = BUNDLE) private val descriptionKey: String) {
    NONE("cangjie.debugger.renderers.none.item"),
    COMPILER("cangjie.debugger.renderers.compiler.item"),
    BUNDLED("cangjie.debugger.renderers.bundled.item");

    override fun toString(): String = CjDebuggerBundle.message(descriptionKey)

    companion object {
        val DEFAULT: LLDBRenderers = BUNDLED
        fun fromIndex(index: Int): LLDBRenderers = values().getOrElse(index) { DEFAULT }
    }
}

