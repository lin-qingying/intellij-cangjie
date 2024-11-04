package com.linqingying.cangjie.analyzer

import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.types.ErrorUtils
import java.io.File

class CompilationErrorException : RuntimeException {
    constructor() : super()
    constructor(message: String) : super(message)
}

open class AnalysisResult protected constructor(
    val bindingContext: BindingContext,
    val moduleDescriptor: ModuleDescriptor,
    val shouldGenerateCode: Boolean = true
) {
    fun isError(): Boolean = this is InternalError || this is CompilationError
    private class CompilationError(bindingContext: BindingContext) : AnalysisResult(bindingContext, ErrorUtils.errorModule )

    fun throwIfError() {
        when (this) {
            is InternalError -> throw IllegalStateException("failed to analyze: $error", error)
            is CompilationError -> throw CompilationErrorException()
        }
    }
    class RetryWithAdditionalRoots(
        bindingContext: BindingContext,
        moduleDescriptor: ModuleDescriptor,

        val additionalCangJieRoots: List<File>,
        val additionalClassPathRoots: List<File> = emptyList(),
        val addToEnvironment: Boolean = true
    ) : AnalysisResult(bindingContext, moduleDescriptor)

    private class InternalError(
        bindingContext: BindingContext,
        val exception: Throwable
    ) : AnalysisResult(bindingContext ,ErrorUtils.errorModule)
    val error: Throwable
        get() = if (this is InternalError) this.exception else throw IllegalStateException("Should only be called for error analysis result")

    companion object {
        val EMPTY: AnalysisResult = success(BindingContext.EMPTY,ErrorUtils.errorModule)


        @JvmStatic
        fun success(bindingContext: BindingContext, module: ModuleDescriptor): AnalysisResult {
            return AnalysisResult(bindingContext, module, true)
        }
        @JvmStatic
        fun success(bindingContext: BindingContext, module: ModuleDescriptor,  shouldGenerateCode: Boolean): AnalysisResult {
            return AnalysisResult(bindingContext,module, shouldGenerateCode)
        }
        @JvmStatic
        fun internalError(bindingContext: BindingContext, error: Throwable): AnalysisResult {
            return InternalError(bindingContext, error)
        }
    }
}
