package com.huawei.cangjie.resolve

import com.huawei.cangjie.analyzer.AnalysisResult
import com.huawei.cangjie.descriptors.DiagnosticSink
import com.huawei.cangjie.psi.CjElement


interface ResolutionFacade{



    fun analyzeWithAllCompilerChecks(element: CjElement, callback: DiagnosticSink.DiagnosticsCallback? = null): AnalysisResult
            = analyzeWithAllCompilerChecks(listOf(element), callback)

    fun analyzeWithAllCompilerChecks(elements: Collection<CjElement>, callback: DiagnosticSink.DiagnosticsCallback? = null): AnalysisResult

}