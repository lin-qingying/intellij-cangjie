/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.completion.turboComplete

import com.intellij.codeInsight.completion.CompletionParameters

import com.intellij.openapi.extensions.ExtensionPointName
import org.cangnova.cangjie.completion.addingPolicy.PolicyController


interface SuggestionGeneratorExecutorProvider {
  fun shouldBeCalled(parameters: CompletionParameters): Boolean

  fun createExecutor(
    parameters: CompletionParameters,
    policyController: PolicyController,
  ): SuggestionGeneratorExecutor

  companion object {
    val EP_NAME: ExtensionPointName<SuggestionGeneratorExecutorProvider> =
      ExtensionPointName("org.cangnova.cangjie.turboComplete.suggestionGeneratorExecutorProvider")

    fun hasAnyToCall(parameters: CompletionParameters): Boolean {
      return EP_NAME.extensionList.any { it.shouldBeCalled(parameters) }
    }

    fun findOneMatching(parameters: CompletionParameters): SuggestionGeneratorExecutorProvider {
      val allExecutorProviders = EP_NAME.extensionList.filter { it.shouldBeCalled(parameters) }
      if (allExecutorProviders.size > 1) {
        throw IllegalStateException(
          "Found more than one matching CompletionKindExecutorProvider: ${allExecutorProviders.map { it.javaClass.name }}")
      }
      return allExecutorProviders[0]
    }
  }
}