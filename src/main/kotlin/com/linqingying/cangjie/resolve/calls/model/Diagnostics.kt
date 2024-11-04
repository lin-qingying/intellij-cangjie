package com.linqingying.cangjie.resolve.calls.model

import com.linqingying.cangjie.resolve.calls.inference.model.ConstraintSystemError
import com.linqingying.cangjie.resolve.calls.tower.CandidateApplicability
import io.github.classgraph.TypeArgument


abstract class CangJieCallDiagnostic(
    val candidateApplicability: CandidateApplicability
) {
    abstract fun report(reporter: DiagnosticReporter)
}

interface DiagnosticReporter {
    fun onExplicitReceiver(diagnostic: CangJieCallDiagnostic)

    fun onCall(diagnostic: CangJieCallDiagnostic)

    fun onTypeArguments(diagnostic: CangJieCallDiagnostic)

    fun onCallName(diagnostic: CangJieCallDiagnostic)

    fun onTypeArgument(typeArgument: TypeArgument, diagnostic: CangJieCallDiagnostic)
//
    fun onCallReceiver(callReceiver: SimpleCangJieCallArgument, diagnostic: CangJieCallDiagnostic)


    fun onCallArgument(callArgument: CangJieCallArgument, diagnostic: CangJieCallDiagnostic)
    fun onCallArgumentName(callArgument: CangJieCallArgument, diagnostic: CangJieCallDiagnostic)
    fun onCallArgumentSpread(callArgument: CangJieCallArgument, diagnostic: CangJieCallDiagnostic)

    fun constraintError(error: ConstraintSystemError)
}
