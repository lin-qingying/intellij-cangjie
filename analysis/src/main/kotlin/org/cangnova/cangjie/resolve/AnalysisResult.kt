/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.types.ErrorUtils
import java.io.File

class CompilationErrorException : RuntimeException {
    constructor() : super()
    constructor(message: String) : super(message)
}

class DelegateAnalysisResult(
    val result: AnalysisResult
) : AnalysisResult(
    result.bindingContext,
    result.moduleDescriptor,
    result.shouldGenerateCode
)

open class AnalysisResult protected constructor(
    val bindingContext: BindingContext,
    val moduleDescriptor: ModuleDescriptor,
    val shouldGenerateCode: Boolean = true
) {
    fun isError(): Boolean = this is InternalError || this is CompilationError
    private class CompilationError(bindingContext: BindingContext) :
        AnalysisResult(bindingContext, ErrorUtils.errorModule)

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
    ) : AnalysisResult(bindingContext, ErrorUtils.errorModule)

    val error: Throwable
        get() = if (this is InternalError) this.exception else throw IllegalStateException("Should only be called for error analysis result")

    companion object {
        val EMPTY: AnalysisResult = success(BindingContext.EMPTY, ErrorUtils.errorModule)


        @JvmStatic
        fun success(bindingContext: BindingContext, module: ModuleDescriptor): AnalysisResult {
            return AnalysisResult(bindingContext, module)
        }

        @JvmStatic
        fun success(
            bindingContext: BindingContext,
            module: ModuleDescriptor,
            shouldGenerateCode: Boolean
        ): AnalysisResult {
            return AnalysisResult(bindingContext, module, shouldGenerateCode)
        }

        @JvmStatic
        fun internalError(bindingContext: BindingContext, error: Throwable): AnalysisResult {
            return InternalError(bindingContext, error)
        }
    }
}
