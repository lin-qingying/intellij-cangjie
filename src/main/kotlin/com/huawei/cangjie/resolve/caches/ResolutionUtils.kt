@file:JvmName("ResolutionUtils")

package com.huawei.cangjie.resolve.caches

import com.huawei.cangjie.analyzer.AnalysisResult
import com.huawei.cangjie.configurable.services.CangJieLanguageServerServices
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.diagnostics.Diagnostic
import com.huawei.cangjie.ide.FrontendInternals
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.*
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.huawei.cangjie.resolve.calls.util.getResolvedCall
import com.huawei.cangjie.resolve.calls.util.safeAnalyze
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.resolve.lazy.NoDescriptorForDeclarationException
import com.huawei.cangjie.utils.actionUnderSafeAnalyzeBlock

fun CjElement.analyzeWithAllCompilerChecks(): AnalysisResult = getResolutionFacade().analyzeWithAllCompilerChecks(this)

@JvmOverloads
fun CjElement.safeAnalyze(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): BindingContext = safeAnalyze(getResolutionFacade(), bodyResolveMode)

fun CjFile.resolveImportReference(fqName: FqName): Collection<DeclarationDescriptor> {
    val facade = getResolutionFacade()
    return facade.resolveImportReference(facade.moduleDescriptor, fqName)
}

// this method don't check visibility and collect all descriptors with given fqName
@OptIn(FrontendInternals::class)
fun ResolutionFacade.resolveImportReference(
    moduleDescriptor: ModuleDescriptor,
    fqName: FqName
): Collection<DeclarationDescriptor> {
    val importDirective = CjPsiFactory(project).createImportDirective(ImportPath(fqName, false))
    val qualifiedExpressionResolver = this.getFrontendService(QualifiedExpressionResolver::class.java)
    return qualifiedExpressionResolver.processImportReference(
        importDirective,
        moduleDescriptor,
        BindingTraceContext(),
        excludedImportNames = emptyList(),
        packageFragmentForVisibilityCheck = null
    )?.getContributedDescriptors() ?: emptyList()
}

/**
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun CjElement.resolveToCall(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL) =
    resolveToCall(getResolutionFacade(), bodyResolveMode)

fun CjElement.resolveToCall(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): ResolvedCall<out CallableDescriptor>? = getResolvedCall(safeAnalyze(resolutionFacade, bodyResolveMode))


//fun CjElement.getResolutionFacade(): ResolutionFacade =CangJieCacheService.getInstance(project).getResolutionFacade(this)
fun CjFile.analyzeWithAllCompilerChecks(vararg extraFiles: CjFile): AnalysisResult =
    this.analyzeWithAllCompilerChecks(null, *extraFiles)

fun CjFile.analyzeWithAllCompilerChecks(callback: ((Diagnostic) -> Unit)?, vararg extraFiles: CjFile): AnalysisResult {
    return if (extraFiles.isEmpty()) {
        CangJieCacheService.getInstance(project).getResolutionFacade(this)
            .analyzeWithAllCompilerChecks(this, callback)
    } else {
        CangJieCacheService.getInstance(project).getResolutionFacade(listOf(this) + extraFiles.toList())
            .analyzeWithAllCompilerChecks(this, callback)
    }
}

/**
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun CjTypeStatement.resolveToDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL) =
    resolveToDescriptorIfAny(getResolutionFacade(), bodyResolveMode)

fun CjTypeStatement.resolveToDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): ClassDescriptor? {
    return (this as CjDeclaration).resolveToDescriptorIfAny(resolutionFacade, bodyResolveMode) as? ClassDescriptor
}

/**
 * This function first uses declaration resolvers to resolve this declaration and/or additional declarations (e.g. its parent),
 * and then takes the relevant descriptor from binding context.
 * The exact set of declarations to resolve depends on bodyResolveMode
 *
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun CjDeclaration.resolveToDescriptorIfAny(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): DeclarationDescriptor? =
    resolveToDescriptorIfAny(getResolutionFacade(), bodyResolveMode)

/**
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun CjNamedFunction.resolveToDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL) =
    resolveToDescriptorIfAny(getResolutionFacade(), bodyResolveMode)

val CjDeclaration.descriptor: DeclarationDescriptor?
    get() = if (this is CjParameter) this.descriptor else this.resolveToDescriptorIfAny(BodyResolveMode.FULL)
val CjParameter.descriptor: ValueParameterDescriptor?
    get() = this.resolveToParameterDescriptorIfAny(BodyResolveMode.FULL)

/**
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun CjParameter.resolveToParameterDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL) =
    resolveToParameterDescriptorIfAny(getResolutionFacade(), bodyResolveMode)

fun CjParameter.resolveToParameterDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): ValueParameterDescriptor? {
    val context = safeAnalyze(resolutionFacade, bodyResolveMode)
    return context.get(BindingContext.VALUE_PARAMETER, this) as? ValueParameterDescriptor
}

/**
 * This function first uses declaration resolvers to resolve this declaration and/or additional declarations (e.g. its parent),
 * and then takes the relevant descriptor from binding context.
 * The exact set of declarations to resolve depends on bodyResolveMode
 */
fun CjDeclaration.resolveToDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): DeclarationDescriptor? {
    //TODO: BodyResolveMode.PARTIAL is not quite safe!
    val context = safeAnalyze(resolutionFacade, bodyResolveMode)
    return if (this is CjParameter && hasLetOrVar()) {
        context.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, this)
        // It is incorrect to have `val/var` parameters outside the primary constructor (e.g., `fun foo(val x: Int)`)
        // but we still want to try to resolve in such cases.
            ?: context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, this)
    } else {
        context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, this)
    }
}

fun CjNamedFunction.resolveToDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): FunctionDescriptor? {
    return (this as CjDeclaration).resolveToDescriptorIfAny(resolutionFacade, bodyResolveMode) as? FunctionDescriptor
}

@JvmOverloads
fun CjElement.analyze(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): BindingContext = resolutionFacade.analyze(this, bodyResolveMode)

@JvmOverloads
fun CjElement.analyze(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): BindingContext = analyze(getResolutionFacade(), bodyResolveMode)

fun CjElement.safeAnalyzeNonSourceRootCode(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): BindingContext =
    actionUnderSafeAnalyzeBlock({ analyze(resolutionFacade, bodyResolveMode) }, { BindingContext.EMPTY })

fun CjElement.getResolutionFacade(): ResolutionFacade =
    CangJieCacheService.getInstance(project).getResolutionFacade(this)

@JvmOverloads
fun CjElement.safeAnalyzeNonSourceRootCode(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): BindingContext {
    if (!CangJieLanguageServerServices.getInstance().astConfig.enabled) return BindingContext.EMPTY

    return safeAnalyzeNonSourceRootCode(getResolutionFacade(), bodyResolveMode)
}

/**
 * This function throws exception when resolveToDescriptorIfAny returns null, otherwise works equivalently.
 *
 * **Please, use overload with providing resolutionFacade for stable results of subsequent calls**
 */
fun CjDeclaration.unsafeResolveToDescriptor(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): DeclarationDescriptor =
    unsafeResolveToDescriptor(getResolutionFacade(), bodyResolveMode)

/**
 * This function throws exception when resolveToDescriptorIfAny returns null, otherwise works equivalently.
 */
fun CjDeclaration.unsafeResolveToDescriptor(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): DeclarationDescriptor =
    resolveToDescriptorIfAny(resolutionFacade, bodyResolveMode) ?: throw NoDescriptorForDeclarationException(this)

