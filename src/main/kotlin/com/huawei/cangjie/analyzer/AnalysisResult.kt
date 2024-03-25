package com.huawei.cangjie.analyzer

import com.huawei.cangjie.resolve.BindingContext
class CompilationErrorException : RuntimeException {
    constructor() : super()
    constructor(message: String) : super(message)
}

open class AnalysisResult protected constructor(
    val bindingContext: BindingContext,
//    val moduleDescriptor: ModuleDescriptor,
    val shouldGenerateCode: Boolean = true
) {
    fun isError(): Boolean = this is InternalError || this is CompilationError
    private class CompilationError(bindingContext: BindingContext) : AnalysisResult(bindingContext )

    fun throwIfError() {
        when (this) {
            is InternalError -> throw IllegalStateException("failed to analyze: $error", error)
            is CompilationError -> throw CompilationErrorException()
        }
    }

    private class InternalError(
        bindingContext: BindingContext,
        val exception: Throwable
    ) : AnalysisResult(bindingContext )
    val error: Throwable
        get() = if (this is InternalError) this.exception else throw IllegalStateException("Should only be called for error analysis result")

    companion object {
        val EMPTY: AnalysisResult = success(BindingContext.EMPTY)

        @JvmStatic
        fun success(bindingContext: BindingContext): AnalysisResult {
            return AnalysisResult(bindingContext, true)
        }
        @JvmStatic
        fun success(bindingContext: BindingContext,  shouldGenerateCode: Boolean): AnalysisResult {
            return AnalysisResult(bindingContext, shouldGenerateCode)
        }
        @JvmStatic
        fun internalError(bindingContext: BindingContext, error: Throwable): AnalysisResult {
            return InternalError(bindingContext, error)
        }
    }
}