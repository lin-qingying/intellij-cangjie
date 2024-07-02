

@file:JvmName("ResolutionUtils")
package com.huawei.cangjie.resolve.caches

import com.huawei.cangjie.analyzer.AnalysisResult
import com.huawei.cangjie.descriptors.Diagnostic
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.utils.actionUnderSafeAnalyzeBlock

//fun CjElement.getResolutionFacade(): ResolutionFacade =CangJieCacheService.getInstance(project).getResolutionFacade(this)
fun CjFile.analyzeWithAllCompilerChecks(vararg extraFiles: CjFile): AnalysisResult = this.analyzeWithAllCompilerChecks(null, *extraFiles)
fun CjFile.analyzeWithAllCompilerChecks(callback: ((Diagnostic) -> Unit)?, vararg extraFiles: CjFile): AnalysisResult {
    return if (extraFiles.isEmpty()) {
      CangJieCacheService.getInstance(project).getResolutionFacade(this)
            .analyzeWithAllCompilerChecks(this, callback)
    } else {
        CangJieCacheService.getInstance(project).getResolutionFacade(listOf(this) + extraFiles.toList())
            .analyzeWithAllCompilerChecks(this, callback)
    }
}

@JvmOverloads
fun CjElement.analyze(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): BindingContext = resolutionFacade.analyze(this, bodyResolveMode)

fun CjElement.safeAnalyzeNonSourceRootCode(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): BindingContext =
    actionUnderSafeAnalyzeBlock({ analyze(resolutionFacade, bodyResolveMode) }, { BindingContext.EMPTY })

fun CjElement.getResolutionFacade(): ResolutionFacade = CangJieCacheService.getInstance(project).getResolutionFacade(this)

@JvmOverloads
fun CjElement.safeAnalyzeNonSourceRootCode(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): BindingContext = safeAnalyzeNonSourceRootCode(getResolutionFacade(), bodyResolveMode)
