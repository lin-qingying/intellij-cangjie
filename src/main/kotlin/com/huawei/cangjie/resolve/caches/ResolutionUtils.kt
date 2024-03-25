

@file:JvmName("ResolutionUtils")
package com.huawei.cangjie.resolve.caches

import com.huawei.cangjie.analyzer.AnalysisResult
import com.huawei.cangjie.descriptors.Diagnostic
import com.huawei.cangjie.psi.CjFile

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