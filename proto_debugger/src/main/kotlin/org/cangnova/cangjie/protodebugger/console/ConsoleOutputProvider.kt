package org.cangnova.cangjie.protodebugger.console

import com.intellij.openapi.util.Key

/**
 * 控制台输出提供者接口
 *
 * 用于向控制台输出内容的抽象接口，支持依赖注入
 * 这允许不同的组件向同一个控制台输出，而不需要直接依赖具体的控制台实现
 */
interface ConsoleOutputProvider {
    /**
     * 向控制台打印文本
     *
     * @param text 要打印的文本内容
     * @param outputType 输出类型键（stdout, stderr等）
     */
    fun printToConsole(text: String, outputType: Key<*>)
}

/**
 * 空实现 - 用于不需要输出的场景
 */
object NoOpConsoleOutputProvider : ConsoleOutputProvider {
    override fun printToConsole(text: String, outputType: Key<*>) {
        // 不做任何事
    }
}