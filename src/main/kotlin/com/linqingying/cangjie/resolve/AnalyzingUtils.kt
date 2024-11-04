package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjElementImplStub
import com.linqingying.cangjie.psi.CjPackageDirective
import com.linqingying.cangjie.psi.debugtext.getDebugText
import java.lang.StringBuilder


object AnalyzingUtils {
    private const val WRITE_DEBUG_TRACE_NAMES = false

    // --------------------------------------------------------------------------------------------------------------------------
    @JvmStatic
    fun formDebugNameForBindingTrace(debugName: String, resolutionSubjectForMessage: Any?): String {
        if (WRITE_DEBUG_TRACE_NAMES) {
            val debugInfo = StringBuilder(debugName)
            if (resolutionSubjectForMessage is CjElement) {
                debugInfo.append(" ").append(resolutionSubjectForMessage.getDebugText())
                //debugInfo.append(" in ").append(element.getContainingFile().getName());
                debugInfo.append(" in ").append(resolutionSubjectForMessage.getContainingCjFile().name).append(" ").append(
                    resolutionSubjectForMessage.textOffset
                )
            } else if (resolutionSubjectForMessage != null) {
                debugInfo.append(" ").append(resolutionSubjectForMessage)
            }
            return debugInfo.toString()
        }
        return ""
    }


}
