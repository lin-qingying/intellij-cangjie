package com.linqingying.cangjie.debugger

import com.intellij.openapi.util.registry.Registry

val isNewGdbSetupEnabled: Boolean get() = Registry.`is`("com.linqingying.cangjie.debugger.gdb.setup.v2", false)
