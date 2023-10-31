package com.huawei.cangjie

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls


abstract class AbstractCangJieBundle protected constructor(pathToBundle: String) : DynamicBundle(pathToBundle) {
    @Nls
    protected fun String.withHtml(): String = "<html>$this</html>"
}
