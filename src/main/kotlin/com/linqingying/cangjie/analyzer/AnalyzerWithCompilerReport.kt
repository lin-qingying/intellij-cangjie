package com.linqingying.cangjie.analyzer


import com.linqingying.cangjie.cli.messages.MessageCollector
import com.linqingying.cangjie.config.*
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.resolve.CompilerEnvironment
import com.linqingying.cangjie.resolve.TargetEnvironment
import com.linqingying.cangjie.serialization.builtins.languageVersionSettings


interface AbstractAnalyzerWithCompilerReport {
    val targetEnvironment: TargetEnvironment

    val analysisResult: AnalysisResult

    fun analyzeAndReport(files: Collection<CjFile>, analyze: () -> AnalysisResult)

    fun hasErrors(): Boolean
}
class AnalyzerWithCompilerReport(
    private val messageCollector: MessageCollector,
    private val languageVersionSettings: LanguageVersionSettings,
    private val renderDiagnosticName: Boolean
) : AbstractAnalyzerWithCompilerReport
{
    override val targetEnvironment: TargetEnvironment
        get() = CompilerEnvironment
    override lateinit var analysisResult: AnalysisResult

    override fun analyzeAndReport(files: Collection<CjFile>, analyze: () -> AnalysisResult) {
        analysisResult = analyze()
//        if (!analysisResult.isError()) {
//            OptInUsageChecker.checkCompilerArguments(
//                analysisResult.moduleDescriptor, languageVersionSettings,
//                reportError = { message -> messageCollector.report(ERROR, message) },
//                reportWarning = { message -> messageCollector.report(WARNING, message) }
//            )
//        }
//        reportSyntaxErrors(files)
//        reportDiagnostics(analysisResult.bindingContext.diagnostics, messageCollector, renderDiagnosticName)
//        reportIncompleteHierarchies()
//        reportAlternativeSignatureErrors()
    }
    constructor(configuration: CompilerConfiguration) : this(
        configuration.messageCollector,
        configuration.languageVersionSettings,
        configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    )
    override fun hasErrors(): Boolean =
        messageCollector.hasErrors()


}
