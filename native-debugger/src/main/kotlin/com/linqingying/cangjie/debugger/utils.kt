package cn.cangnova.cangjie.debugger

import com.intellij.openapi.util.registry.Registry

val isNewGdbSetupEnabled: Boolean get() = Registry.`is`("cn.cangnova.cangjie.debugger.gdb.setup.v2", false)
