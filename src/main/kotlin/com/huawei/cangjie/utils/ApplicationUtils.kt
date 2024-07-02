package com.huawei.cangjie.utils

import com.intellij.openapi.progress.impl.CancellationCheck

fun <T> runWithCancellationCheck(block: () -> T): T = CancellationCheck.runWithCancellationCheck(block)
