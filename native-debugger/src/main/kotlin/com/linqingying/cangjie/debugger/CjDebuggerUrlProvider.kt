package cn.cangnova.cangjie.debugger

import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import com.jetbrains.cidr.execution.debugger.backend.bin.UrlProvider
import java.net.URL

object CjDebuggerUrlProvider {
    fun lldbFrontend(os: OS, arch: CpuArch): URL? = UrlProvider.lldbFrontend(os, arch)
    fun lldb(os: OS, arch: CpuArch): URL? = UrlProvider.lldb(os, arch)
    fun gdb(os: OS, arch: CpuArch): URL? = UrlProvider.gdb(os, arch)
}
