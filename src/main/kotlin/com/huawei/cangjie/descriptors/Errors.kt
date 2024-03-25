package com.huawei.cangjie.descriptors

import com.huawei.cangjie.diagnostics.DiagnosticFactory1
import com.huawei.cangjie.diagnostics.PositioningStrategies
import com.huawei.cangjie.diagnostics.Severity
import com.huawei.cangjie.psi.CjReferenceExpression

object Errors {


    @JvmStatic
    @get:JvmName("UNRESOLVED_REFERENCE")
    val UNRESOLVED_REFERENCE =
        DiagnosticFactory1.create<CjReferenceExpression, CjReferenceExpression>(
            Severity.ERROR,
            PositioningStrategies.FOR_UNRESOLVED_REFERENCE
        )


}