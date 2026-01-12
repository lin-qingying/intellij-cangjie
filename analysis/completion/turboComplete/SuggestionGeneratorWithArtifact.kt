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
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import org.cangnova.cangjie.completion.addingPolicy.PassDirectlyPolicy
import org.cangnova.cangjie.completion.addingPolicy.PolicyController
import org.cangnova.cangjie.completion.turboComplete.addingPolicy.CollectionFillingPolicy

/**
 * Generates completion suggestions for a certain type of completion kind and stores
 * a cached artifact for later use.
 *
 * For example, we have two [SuggestionGenerator]s A and B.
 * While working, A generates an artifact, that is used later by B.
 * And the ML model decided, that B must be executed earlier.
 *
 * If we execute B first, then we want to preserve the recommended order of the
 * generators, and we don't want A to add completion variants to the lookup first.
 * Instead, A could be a [SuggestionGeneratorWithArtifact]. Then
 * 1. B asks A to create the artifact - [getArtifact]
 * 2. B generates completion variants
 *   (it's A's order now to generate variants)
 * 3. A will only collect put cached lookup elements to the result set
 */

abstract class SuggestionGeneratorWithArtifact<T>(override val kind: CompletionKind,
                                                  override val result: CompletionResultSet,
                                                  private val resultPolicyController: PolicyController,
                                                  override val parameters: CompletionParameters) : SuggestionGenerator {

  private var cachedArtifact: T? = null
  private var cachedLookupElements: MutableList<LookupElement>? = null

  fun getArtifact(): T {
    return cachedArtifact ?: run {
      val generatedLookupElements = mutableListOf<LookupElement>()
      val generatedArtifact = resultPolicyController.invokeWithPolicy(CollectionFillingPolicy(generatedLookupElements)) {
        generateVariantsAndArtifact()
      }
      cachedLookupElements = generatedLookupElements
      cachedArtifact = generatedArtifact
      generatedArtifact
    }
  }

  override fun generateCompletionVariants() {
    cachedLookupElements?.let {
      if (it.isEmpty()) return
      resultPolicyController.invokeWithPolicy(PassDirectlyPolicy()) {
        result.addAllElements(it)
      }
      it.clear()
    } ?: run {
      cachedArtifact = resultPolicyController.invokeWithPolicy(PassDirectlyPolicy()) {
        generateVariantsAndArtifact()
      }
    }
  }

  abstract fun generateVariantsAndArtifact(): T
}