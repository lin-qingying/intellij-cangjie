package com.huawei.cangjie.resolve
//
//import com.huawei.cangjie.idea.FrontendInternals
//import com.huawei.cangjie.psi.CjDeclaration
//import com.huawei.cangjie.psi.CjElement
//import com.intellij.openapi.project.Project
//import com.intellij.psi.PsiElement
//import java.lang.module.ModuleDescriptor
//
//interface ResolutionFacade {
//    val project: Project
//
//    fun analyze(element: CjElement, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext
//    fun analyze(elements: Collection<CjElement>, bodyResolveMode: BodyResolveMode): BindingContext
//
//    fun analyzeWithAllCompilerChecks(element: CjElement, callback: DiagnosticSink.DiagnosticsCallback? = null): AnalysisResult
//            = analyzeWithAllCompilerChecks(listOf(element), callback)
//
//    fun analyzeWithAllCompilerChecks(elements: Collection<CjElement>, callback: DiagnosticSink.DiagnosticsCallback? = null): AnalysisResult
//
//    fun fetchWithAllCompilerChecks(element: CjElement): AnalysisResult? = null
//
//    fun resolveToDescriptor(declaration: CjDeclaration, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): DeclarationDescriptor
//
//    val moduleDescriptor: ModuleDescriptor
//
//
//    @FrontendInternals
//    fun <T : Any> getFrontendService(serviceClass: Class<T>): T
//
//    fun <T : Any> getIdeService(serviceClass: Class<T>): T
//
//    @FrontendInternals
//    fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T
//
//    @FrontendInternals
//    fun <T : Any> tryGetFrontendService(element: PsiElement, serviceClass: Class<T>): T?
//
//    @Deprecated("DO NOT USE IT AS IT IS A ROOT CAUSE OF CjIJ-17649")
//    @FrontendInternals
//    fun <T : Any> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T
//
//    fun getResolverForProject(): ResolverForProject<out ModuleInfo>
//}
//
//@FrontendInternals
//inline fun <reified T : Any> ResolutionFacade.frontendService(): T = this.getFrontendService(T::class.java)
//
//inline fun <reified T : Any> ResolutionFacade.ideService(): T = this.getIdeService(T::class.java)
//
//val ResolutionFacade.languageVersionSettings: LanguageVersionSettings
//    get() = @OptIn(FrontendInternals::class) frontendService()
//
//val ResolutionFacade.dataFlowValueFactory: DataFlowValueFactory
//    get() = @OptIn(FrontendInternals::class) frontendService()
