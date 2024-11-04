package com.linqingying.cangjie.psi.psiUtil

import com.linqingying.cangjie.diagnostics.DiagnosticSink
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjSimpleNameExpression

fun checkReservedYield(expression: CjSimpleNameExpression?, sink: DiagnosticSink) {
    // do not force identifier calculation for elements from stubs.
    if (expression?.getReferencedName() != "yield") return

    val identifier = expression.getIdentifier() ?: return

    if (identifier.node.elementType == CjTokens.IDENTIFIER && "yield" == identifier.text) {
        sink.report(Errors.YIELD_IS_RESERVED.on(identifier, "Identifier 'yield' is reserved. Use backticks to call it: `yield`"))
    }
}
