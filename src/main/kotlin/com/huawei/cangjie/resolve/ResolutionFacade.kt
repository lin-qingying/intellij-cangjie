package com.huawei.cangjie.resolve

import com.huawei.cangjie.idea.FrontendInternals
import com.huawei.cangjie.name.Name
import com.intellij.execution.target.TargetPlatform
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import java.lang.module.ModuleDescriptor


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
//    // get service for the module this resolution was created for
//    @FrontendInternals
//    fun <T : Any> getFrontendService(serviceClass: Class<T>): T
//
//    fun <T : Any> getIdeService(serviceClass: Class<T>): T
//
//    // get service for the module defined by PsiElement/ModuleDescriptor passed as parameter
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
//interface ModuleInfo {
//    val name: Name
//    val displayedName: String get() = name.asString()
//    fun dependencies(): List<ModuleInfo>
//    val expectedBy: List<ModuleInfo> get() = emptyList()
//    val platform: TargetPlatform
//    val analyzerServices: PlatformDependentAnalyzerServices
//    fun modulesWhoseInternalsAreVisible(): Collection<ModuleInfo> = listOf()
//    val capabilities: Map<ModuleCapability<*>, Any?>
//        get() = mapOf(Capability to this)
//    val stableName: Name?
//        get() = null
//
//    // For common modules, we add built-ins at the beginning of the dependencies list, after the SDK.
//    // This is needed because if a JVM module depends on the common module, we should use JVM built-ins for resolution of both modules.
//    // The common module usually depends on kotlin-stdlib-common which may or may not have its own (common, non-JVM) built-ins,
//    // but if they are present, they should come after JVM built-ins in the dependencies list, because JVM built-ins contain
//    // additional members dependent on the JDK
//    fun dependencyOnBuiltIns(): DependencyOnBuiltIns = analyzerServices.dependencyOnBuiltIns()
//
//    //TODO: (module refactoring) provide dependency on builtins after runtime in IDEA
//    enum class DependencyOnBuiltIns { NONE, AFTER_SDK, LAST }
//
//    companion object {
//        val Capability = ModuleCapability<ModuleInfo>("ModuleInfo")
//    }
//}
