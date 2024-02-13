package com.huawei.cangjie

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey


abstract class AbstractCangJieBundle protected constructor(  pathToBundle: String) : DynamicBundle(pathToBundle) {
    @Nls
    protected fun String.withHtml(): String = "<html>$this</html>"




}
