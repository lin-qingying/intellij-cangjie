package cn.cangnova.cangjie.utils

import com.intellij.openapi.application.ApplicationManager

val isUnitTestMode: Boolean get() = ApplicationManager.getApplication().isUnitTestMode
val isDispatchThread: Boolean get() = ApplicationManager.getApplication().isDispatchThread
val isInternal: Boolean get() = ApplicationManager.getApplication().isInternal
