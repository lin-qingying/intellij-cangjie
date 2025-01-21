package com.linqingying.cangjie.dapDebugger.backend

import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import java.util.concurrent.atomic.AtomicBoolean

open class CjCodepoint


//class CjBreakpoint( val origFile:String,val origLine:Int,val condition:String) : CjCodepoint(id) {
//}
/**
 * 断点对象  存储断点信息   filename  filepath   lines
 */

open class CjBreakpoint(
    val filename: String,
    val filepath: String,

    ) : CjCodepoint() {
    val lines: MutableMap<Int, XLineBreakpoint<*>?> = mutableMapOf()


    val isRunToCursor :MutableMap<Int, Boolean> = mutableMapOf()

    fun addLine(line: Int, breakpoint: XLineBreakpoint<*>?) {
        lines[line] = breakpoint
    }

    fun addLine(map: MutableMap<Int, XLineBreakpoint<*>>) {
        lines.putAll(map)
    }

    fun removeLine(line: Int) {
        lines.remove(line)
        isRunToCursor.remove(line)
    }

    fun addLines(lines: MutableMap<Int, XLineBreakpoint<*>?>) {
//        for (line in lines) {
//            addLine(line)
//        }
        this.lines.putAll(lines)
    }

    fun removeLines(lines: MutableMap<Int, XLineBreakpoint<*>?>) {
//        for (line in lines) {
//            removeLine(line)
//        }
        for (line in lines) {
            this.lines.remove(line.key)
            this.isRunToCursor.remove(line.key)
        }
    }


}
