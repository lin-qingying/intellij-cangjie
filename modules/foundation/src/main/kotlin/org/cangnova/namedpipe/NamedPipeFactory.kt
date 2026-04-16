package org.cangnova.namedpipe

/**
 * 命名管道工厂 —— 根据当前运行平台自动选择 Unix / Windows 实现。
 *
 * 用法：
 * ```kotlin
 * val factory = NamedPipeFactory.create()          // 自动检测平台
 * val server  = factory.createServer(PipeConfig("mypipe"))
 * val client  = factory.createClient(PipeConfig("mypipe"))
 * ```
 */
interface NamedPipeFactory {

    /** 创建服务端管道（同时完成底层管道文件/句柄的创建） */
    fun createServer(config: PipeConfig): ServerNamedPipe

    /** 创建客户端管道（尚未连接，需手动调用 connect() 或 connectAsync()） */
    fun createClient(config: PipeConfig): ClientNamedPipe

    /**
     * 构造该平台的完整管道路径：
     *   Unix:    /tmp/.namedpipes/<name>
     *   Windows: \\.\pipe\<name>
     */
    fun resolvePipePath(name: String): String

    companion object {

        /** 检测 os.name 并返回对应平台工厂 */
        fun create(): NamedPipeFactory {
            val os = System.getProperty("os.name", "").lowercase()
            return when {
                os.contains("win") -> WindowsNamedPipeFactory()
                os.contains("nix") || os.contains("nux") || os.contains("mac") ->
                    UnixNamedPipeFactory()
                else -> throw PipeException.UnsupportedPlatform(os)
            }
        }

        /** 强制使用 Unix 实现（测试 / 跨平台 CI 用） */
        fun createUnix(): NamedPipeFactory = UnixNamedPipeFactory()

        /** 强制使用 Windows 实现（测试用） */
        fun createWindows(): NamedPipeFactory = WindowsNamedPipeFactory()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 平台辅助
// ─────────────────────────────────────────────────────────────────────────────

enum class Platform { WINDOWS, UNIX, UNKNOWN }

internal fun detectPlatform(): Platform {
    val os = System.getProperty("os.name", "").lowercase()
    return when {
        os.contains("win")                                              -> Platform.WINDOWS
        os.contains("nix") || os.contains("nux") || os.contains("mac") -> Platform.UNIX
        else                                                            -> Platform.UNKNOWN
    }
}
