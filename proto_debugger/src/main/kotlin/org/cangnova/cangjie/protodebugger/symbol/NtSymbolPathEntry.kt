package org.cangnova.cangjie.protodebugger.symbol

import com.intellij.util.xmlb.annotations.Tag
import kotlin.jvm.internal.Intrinsics

/**
 * Windows NT符号路径条目类
 *
 * 该数据类表示Windows NT平台的符号路径配置条目，用于调试器查找和加载符号文件。
 * 符号文件（.pdb文件）包含了程序的调试信息，如函数名、变量名、行号信息等。
 *
 * 使用场景：
 * - 配置调试器的符号搜索路径
 * - 管理符号服务器的访问权限
 * - 支持本地和远程符号文件查找
 * - 调试器符号设置的持久化存储
 *
 * 主要功能：
 * - 封装符号路径的URL和启用状态
 * - 支持XML序列化和反序列化
 * - 提供符号路径的启用/禁用控制
 * - 管理符号搜索的优先级
 *
 * 技术特点：
 * - 支持Microsoft符号服务器
 * - 兼容本地符号文件路径
 * - 可被IntelliJ的XML序列化框架处理
 * - 提供灵活的符号查找配置
 *
 * @param url 符号路径的URL，可以是本地文件路径或网络地址
 * @param isEnabled 是否启用该符号路径条目
 */
@Tag
data class NtSymbolPathEntry(
    /**
     * 符号路径URL
     *
     * 指定符号文件的位置，可以是：
     * - 本地文件系统路径（如"C:\Symbols"）
     * - 网络共享路径（如"\\server\symbols"）
     * - Microsoft符号服务器URL（如"https://msdl.microsoft.com/download/symbols"）
     * - 其他符号服务器的网络地址
     */
    var url: String,

    /**
     * 启用状态标志
     *
     * 控制该符号路径条目是否被调试器使用。
     * 当设置为false时，调试器将跳过该路径的符号搜索。
     *
     * 使用场景：
     * - 临时禁用某个符号服务器
     * - 测试不同符号源的有效性
     * - 优化符号加载性能
     */
    var isEnabled: Boolean
) {
    /**
     * 比较两个符号路径条目是否相等
     *
     * 两个符号路径条目相等需要URL和启用状态都完全相同。
     *
     * @param other 要比较的对象
     * @return 如果条目相等返回true，否则返回false
     */
    override operator fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other !is NtSymbolPathEntry) {
            false
        } else {
            val (url1, isEnabled1) = other
            if (!Intrinsics.areEqual(url, url1)) {
                false
            } else {
                isEnabled == isEnabled1
            }
        }
    }

    /**
     * 返回符号路径条目的字符串表示
     *
     * 格式为"NtSymbolPathEntry(url=..., isEnabled=...)"，
     * 便于调试和日志记录。
     *
     * @return 格式化的字符串表示
     */
    override fun toString(): String {
        return "NtSymbolPathEntry(url=$url, isEnabled=$isEnabled)"
    }

    /**
     * 计算符号路径条目的哈希码
     *
     * 基于URL和启用状态计算哈希码，用于集合操作。
     *
     * @return 符号路径条目的哈希码
     */
    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + isEnabled.hashCode()
        return result
    }
}

