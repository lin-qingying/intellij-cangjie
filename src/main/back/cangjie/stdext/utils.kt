package com.huawei.cangjie.stdext

import com.intellij.openapi.vfs.VirtualFile

inline fun <T> VirtualFile.applyWithSymlink(f: (VirtualFile) -> T?): T? {
    return f(this) ?: f(canonicalFile ?: return null)
}
