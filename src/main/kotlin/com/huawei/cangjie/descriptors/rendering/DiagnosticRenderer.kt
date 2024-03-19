package com.huawei.cangjie.descriptors.rendering

import com.huawei.cangjie.descriptors.UnboundDiagnostic


interface DiagnosticRenderer<in D : UnboundDiagnostic> {
    fun render(diagnostic: D): String

    fun renderParameters(diagnostic: D): Array<out Any?>
}
