package com.huawei.cangjie.debugger

import com.intellij.openapi.util.registry.Registry

val isNewGdbSetupEnabled: Boolean get() = Registry.`is`("org.rust.debugger.gdb.setup.v2", false)
