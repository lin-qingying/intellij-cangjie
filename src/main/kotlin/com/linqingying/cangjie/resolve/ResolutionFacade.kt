package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.analyzer.AnalysisResult
import com.linqingying.cangjie.analyzer.ModuleInfo
import com.linqingying.cangjie.analyzer.ResolverForProject
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.diagnostics.DiagnosticSink
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.ide.FrontendInternals
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

inline fun <reified T : Any> ResolutionFacade.ideService(): T = this.getIdeService(T::class.java)

interface ResolutionFacade{
    val project: Project

    fun fetchWithAllCompilerChecks(element: CjElement): AnalysisResult? = null
    val moduleDescriptor: ModuleDescriptor
    // get service for the module this resolution was created for
    @FrontendInternals
    fun <T : Any> getFrontendService(serviceClass: Class<T>): T
    fun <T : Any> getIdeService(serviceClass: Class<T>): T

    fun analyzeWithAllCompilerChecks(element: CjElement, callback: DiagnosticSink.DiagnosticsCallback? = null): AnalysisResult
            = analyzeWithAllCompilerChecks(listOf(element), callback)
    fun analyze(elements: Collection<CjElement>, bodyResolveMode: BodyResolveMode): BindingContext
    fun resolveToDescriptor(declaration: CjDeclaration, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): DeclarationDescriptor

    fun analyzeWithAllCompilerChecks(elements: Collection<CjElement>, callback: DiagnosticSink.DiagnosticsCallback? = null): AnalysisResult
    fun analyze(element: CjElement, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext
    // get service for the module defined by PsiElement/ModuleDescriptor passed as parameter
    @FrontendInternals
    fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T
    fun getResolverForProject(): ResolverForProject<out ModuleInfo>

}
@FrontendInternals
inline fun <reified T : Any> ResolutionFacade.frontendService(): T = this.getFrontendService(T::class.java)
val ResolutionFacade.languageVersionSettings: LanguageVersionSettings
    get() = @OptIn(FrontendInternals::class) frontendService()
val ResolutionFacade.dataFlowValueFactory: DataFlowValueFactory
    get() = @OptIn(FrontendInternals::class) frontendService()
