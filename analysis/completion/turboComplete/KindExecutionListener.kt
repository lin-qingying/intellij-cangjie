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


/**
 * Listens how [SuggestionGenerator]s are executed
 * Be aware, that each listener is not reinitialized, but
 * the same instance used to convey information about generators'
 * execution each time.
 *
 * The functions are called in their declaration order.
 */

interface KindExecutionListener {
  /**
   * Code completion was just called
   */
  fun onInitialize(parameters: CompletionParameters) {}

  /**
   * Called before any [KindCollector] has collected any kinds,
   * so the collection process just began
   */
  fun onCollectionStarted() {}

  /**
   * A [SuggestionGenerator] had been collected.
   * There could be multiple suggestion generators, hence,
   * the function is called as many times.
   */
  fun onGeneratorCollected(suggestionGenerator: SuggestionGenerator) {}

  /**
   * All kinds have been collected
   */
  fun onCollectionFinished() {}

  /**
   * The generator started generating variants,
   * i.e. [SuggestionGenerator.generateCompletionVariants] was called
   */
  fun onGenerationStarted(suggestionGenerator: SuggestionGenerator) {}

  /**
   * The generator finished generating variants,
   * i.e. [SuggestionGenerator.generateCompletionVariants] finished
   * (either by an exception, or without)
   */
  fun onGenerationFinished(suggestionGenerator: SuggestionGenerator) {}
}