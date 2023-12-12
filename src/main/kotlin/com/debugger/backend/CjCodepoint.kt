package com.debugger.backend

import com.intellij.xdebugger.breakpoints.XLineBreakpoint

open class CjCodepoint() {
}


//class CjBreakpoint( val origFile:String,val origLine:Int,val condition:String) : CjCodepoint(id) {
//}
/**
 * 断点对象  存储断点信息   filename  filepath   lines
 */

open class CjBreakpoint(
    val filename: String,
    val filepath: String,

    ) : CjCodepoint() {
    val lines: MutableMap<Int, XLineBreakpoint<*>> = mutableMapOf()

    fun addLine(map: MutableMap<Int, XLineBreakpoint<*>>) {
        lines.putAll(map)
    }

    fun removeLine(line: Int) {
        lines.remove(line)
    }

    fun addLines(lines: MutableMap<Int, XLineBreakpoint<*>>) {
//        for (line in lines) {
//            addLine(line)
//        }
        this.lines.putAll(lines)
    }

    fun removeLines(lines: MutableMap<Int, XLineBreakpoint<*>>) {
//        for (line in lines) {
//            removeLine(line)
//        }
        for (line in lines) {
            this.lines.remove(line.key)
        }
    }


}