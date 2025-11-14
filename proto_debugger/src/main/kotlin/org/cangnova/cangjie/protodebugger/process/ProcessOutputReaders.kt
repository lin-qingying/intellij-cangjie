package org.cangnova.cangjie.protodebugger.process


import com.intellij.execution.ExecutionException
import com.intellij.execution.Platform
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.io.BaseDataReader.SleepingPolicy
import com.intellij.util.io.BaseOutputReader
import com.intellij.util.system.OS
import com.pty4j.unix.Pty
import org.cangnova.cangjie.protodebugger.ipc.NamedPipe
import org.cangnova.cangjie.protodebugger.ipc.WinPipe
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
/**
 * 操作系统类型枚举
 *
 * 该枚举定义了调试器支持的三种主要操作系统类型：Windows、Linux和macOS。
 * 它提供了操作系统识别和平台转换的功能，用于跨平台的调试器实现。
 *
 * 使用场景：
 * - 根据操作系统选择不同的调试器实现
 * - 处理平台特定的文件路径和命令
 * - 选择合适的进程输出读取方式
 * - 配置平台特定的调试选项
 *
 * 主要功能：
 * - 操作系统类型的标准化定义
 * - 提供与IntelliJ平台OS枚举的转换
 * - 支持Platform枚举的转换
 * - 自动检测当前运行环境
 *
 * @param os 对应的IntelliJ平台OS枚举值
 */
enum class OSType(val os: OS) {
    /**
     * Windows操作系统
     */
    WIN(OS.Windows),

    /**
     * Linux操作系统
     */
    LINUX(OS.Linux),

    /**
     * macOS操作系统
     */
    MAC(OS.macOS);

    /**
     * 转换为IntelliJ平台的OS枚举
     *
     * @return 对应的IntelliJ OS枚举值
     */
    fun toOS(): OS = os

    /**
     * 转换为IntelliJ平台的Platform枚举
     *
     * @return 对应的Platform枚举值（WINDOWS或UNIX）
     */
    fun toPlatform(): Platform = if (this == WIN) Platform.WINDOWS else Platform.UNIX

    companion object {
        /**
         * 当前运行的操作系统类型
         *
         * 根据系统信息自动检测当前运行环境。
         */
        val current = if (SystemInfo.isWindows) WIN else if (SystemInfo.isMac) MAC else LINUX
    }
}

/**
 * 进程输出读取器抽象类
 *
 * 该抽象类负责管理和读取被调试进程的标准输出和标准错误流。
 * 它支持跨平台的输出读取方式，包括Windows命名管道、Unix PTY和临时文件等。
 *
 * 使用场景：
 * - 调试器中实时显示被调试程序的输出
 * - 捕获程序的标准输出和错误信息
 * - 支持终端模拟和交互式调试
 * - 处理远程调试的输出传输
 *
 * 主要功能：
 * - 跨平台的进程输出读取
 * - 支持PTY（伪终端）和管道通信
 * - 异步输出读取和文本处理
 * - 资源管理和错误处理
 *
 * 技术特点：
 * - 支持多种读取策略（阻塞、非阻塞）
 * - 自动检测和适配不同的操作系统
 * - 线程安全的输出读取
 * - 灵活的编码和文本格式处理
 *
 * @param host 主机机器信息，用于识别本地或远程环境
 * @param presentableName 可显示的名称，用于日志和调试
 * @param charset 字符编码，用于输出流的文本解码
 * @param usePtyOnUnix 在Unix系统上是否使用PTY
 * @param emulateTerminal 是否模拟终端环境
 */
abstract class ProcessOutputReaders(
    /**
     * 主机机器信息
     *
     * 包含目标主机的基本信息，用于判断是否为远程环境
     * 以及选择合适的输出读取方式。
     */
    val host: HostMachine,

    /**
     * 可显示的名称
     *
     * 用于日志记录、错误报告和调试信息显示。
     */
    val presentableName: String,

    /**
     * 字符编码
     *
     * 用于解码进程输出的字节流为文本。
     * 通常使用系统默认编码或UTF-8编码。
     */
    val charset: Charset,

    /**
     * 在Unix系统上使用PTY标志
     *
     * 当设置为true时，在Unix/Linux系统上使用伪终端(PTY)，
     * 可以提供更好的终端模拟和交互支持。
     */
    val usePtyOnUnix: Boolean,

    /**
     * 模拟终端标志
     *
     * 当设置为true时，模拟终端环境的行为，
     * 包括控制字符处理和终端特性支持。
     */
    val emulateTerminal: Boolean = false
) {
    companion object {
        /**
         * 日志记录器
         *
         * 用于记录输出读取过程中的调试信息和错误。
         */
        val LOG = Logger.getInstance(ProcessOutputReaders::class.java)
    }
    private val myReaders: AtomicReference<Array<MyOutputReader?>> = AtomicReference(arrayOfNulls(2))

    /**
     * 等待所有输出读取器完成
     *
     * 阻塞当前线程直到所有输出读取器（标准输出和标准错误）都完成工作。
     * 如果任何一个读取器被中断，则立即返回false。
     *
     * @return 如果所有读取器都正常完成返回true，否则返回false
     */
    fun waitFor(): Boolean {
        return this.doWaitFor(this.getReaders())
    }

    /**
     * 获取指定编号的输出读取器
     *
     * 根据编号获取对应的输出读取器实例。
     * 编号0通常表示标准输出，编号1通常表示标准错误。
     *
     * @param num 读取器编号（0=STDOUT，1=STDERR）
     * @return 对应的输出读取器实例
     * @throws ExecutionException 当读取器不可用时抛出
     */
    @Throws(ExecutionException::class)
    protected fun getReader(num: Int): MyOutputReader {
        val readers = getReaders()
        return if (ArrayUtil.isEmpty(readers)) {
            throw ExecutionException("Reader is closed")
        } else {
            readers[num]!!
        }
    }

    init {
        try {
            val osType: OSType = host.osType

            val readers = getReaders()
            if (osType === OSType.WIN) {
                readers[0] = MyWinPipeOutputReader(
                    ProcessOutputTypes.STDOUT
                )
                if (!emulateTerminal) {
                    readers[1] = MyWinPipeOutputReader(

                        ProcessOutputTypes.STDERR
                    )
                }
            } else if (usePtyOnUnix && host.isRemote) {
                readers[0] = MyRemoteUnixPipeOutputReader(

                    host,
                    ProcessOutputTypes.STDOUT
                )
                readers[1] = MyRemoteUnixPipeOutputReader(

                    host,
                    ProcessOutputTypes.STDERR
                )
            } else if (usePtyOnUnix) {
                readers[0] = MyUnixPtyOutputReader(

                    ProcessOutputTypes.STDOUT
                )
                readers[1] = MyUnixPtyOutputReader(

                    ProcessOutputTypes.STDERR
                )
            } else {
                readers[0] = MyFileOutputReader(

                    ProcessOutputTypes.STDOUT
                )
                readers[1] = MyFileOutputReader(

                    ProcessOutputTypes.STDERR
                )
            }
            readers[0]?.start()

            readers[1]?.start()


        } catch (ioEx: IOException) {
            throw ExecutionException("Cannot create output file", ioEx)
        }
    }

    @Throws(ExecutionException::class)
    fun getOutFileAbsolutePath(): String {
        return this.getReader(0).fileAbsolutePath
    }

    @Throws(ExecutionException::class)
    fun getErrFileAbsolutePath(): String {
        return getReader(1).fileAbsolutePath
    }

    protected fun doWaitFor(readers: Array<MyOutputReader?>): Boolean {
        for (each in readers) {
            try {
                each?.waitFor()
            } catch (e: InterruptedException) {
                Thread.interrupted()
                return false
            }
        }
        return true
    }

    fun close() {
        val readers = (myReaders.getAndSet(arrayOfNulls(1))) ?: emptyArray()

        for (each in readers) {
            each?.stop()
        }

        doWaitFor(readers)
    }

    protected fun getReaders(): Array<MyOutputReader?> {
        return myReaders.get()
    }

    fun waitFor(timeout: Long, unit: TimeUnit): Boolean {
        return this.doWaitFor(this.getReaders(), timeout, unit)
    }

    protected fun doWaitFor(readers: Array<MyOutputReader?>, timeout: Long, unit: TimeUnit): Boolean {
        for (each in readers) {
            try {
                each?.waitFor(timeout, unit)
            } catch (e: InterruptedException) {
                Thread.interrupted()
                return false
            } catch (e: TimeoutException) {
                return false
            }
        }
        return true
    }

    class ReaderOptions(private val myPolicy: SleepingPolicy) : BaseOutputReader.Options() {
        override fun policy(): SleepingPolicy {
            return myPolicy
        }

        override fun splitToLines(): Boolean {
            return false
        }

    }

    protected abstract fun onTextAvailable(text: String, key: Key<*>)

    protected open inner class MyOutputReader(

        val type: Key<*>,
        val fileAbsolutePath: String,
        stream: InputStream,
        sleepingPolicy: SleepingPolicy
    ) :
        BaseOutputReader(
            stream, this@ProcessOutputReaders.charset,
            ReaderOptions(sleepingPolicy)
        ) {


        fun start() {
            this.start(this@ProcessOutputReaders.presentableName)
        }

        override fun onTextAvailable(text: String) {
            var text = text

            if (!this@ProcessOutputReaders.emulateTerminal) {
                text = StringUtil.trimEnd(text, "\r")
                text = StringUtil.convertLineSeparators(text)
            }
            this@ProcessOutputReaders.onTextAvailable(text, type)
        }

        override fun executeOnPooledThread(runnable: Runnable): Future<*> {

            return ApplicationManager.getApplication().executeOnPooledThread(runnable)
        }

        override fun stop() {
            super.stop()
            if (mySleepingPolicy === SleepingPolicy.BLOCKING) {
                try {
                    this.waitFor(mySleepingPolicy.getTimeToSleep(false).toLong(), TimeUnit.MILLISECONDS)
                } catch (timeex: TimeoutException) {
                    try {
                        close()
                    } catch (ioex: IOException) {
                       LOG.error(ioex)
                    }
                } catch (iex: InterruptedException) {
                    try {
                        close()
                    } catch (ioex: IOException) {
                       LOG.error(ioex)
                    }
                }
            }
        }


    }


    protected open inner class MyPipeOutputReader<T : NamedPipe>(

        val pipe: NamedPipe,
        type: Key<*>
    ) : MyOutputReader(type, pipe.name, pipe.inputStream, SleepingPolicy.BLOCKING) {


        @Throws(IOException::class)
        override fun close() {
            try {
                super.close()
            } finally {
                pipe.close()
            }
        }
    }

    protected inner class MyWinPipeOutputReader(type: Key<*>) :
        MyPipeOutputReader<WinPipe>(WinPipe.createInboundPipe(type.toString()), type) {


        override fun doRun() {
            try {
                if (!(this.pipe as WinPipe).waitForConnection()) {
                  LOG.error(
                        java.lang.String.format(
                            "Stream reading can't be initiated: couldn't connect to pipe %s. ",
                            this.pipe.name
                        )
                    )
                    return
                }
            } catch (e: IOException) {
                LOG.debug(e)
                try {
                    this.close()
                } catch (ioe: IOException) {
                    LOG.error("Can't close stream", ioe)
                }
                return
            }
            super.doRun()
        }
    }

    protected inner class MyUnixPtyOutputReader(pty: Pty, type: Key<*>) : MyOutputReader(
        type,
        pty.slaveName,
        pty.inputStream,
        SleepingPolicy.BLOCKING
    ) {
        constructor(type: Key<*>) : this(Pty(true), type)


    }

    protected inner class MyRemoteUnixPipeOutputReader(
        pipe: HostMachine,
        type: Key<*>
    ) :
        MyPipeOutputReader<NamedPipe>(pipe.openNamedPipe(), type)

    protected inner class MyFileOutputReader(val file: File, type: Key<*>) :
        MyOutputReader(type, file.absolutePath, FileInputStream(file), SleepingPolicy.NON_BLOCKING) {

        constructor(type: Key<*>) : this(
            FileUtil.createTempFile(this@ProcessOutputReaders.javaClass.getSimpleName(), type.toString(), true),
            type
        )


        @Throws(IOException::class)
        override fun close() {
            try {
                super.close()
            } finally {
                FileUtil.delete(file)
            }
        }
    }


}
