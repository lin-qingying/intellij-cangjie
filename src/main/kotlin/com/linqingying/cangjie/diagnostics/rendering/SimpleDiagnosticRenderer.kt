package com.linqingying.cangjie.diagnostics.rendering

import com.linqingying.cangjie.diagnostics.Diagnostic
import java.text.MessageFormat

class SimpleDiagnosticRenderer(private val message: () -> String ) :
    DiagnosticRenderer<Diagnostic > {
    override fun render(diagnostic: Diagnostic): String {

//        val str = CangJieDiagnosisBundle.rawMessage(diagnostic.factory.name)
//        if(str == "!${diagnostic.factory.name}!"){
//            return message
//        }

        return message()
    }

    override fun renderParameters(diagnostic: Diagnostic): Array<Any?> {
        return arrayOfNulls(0)
    }
}
