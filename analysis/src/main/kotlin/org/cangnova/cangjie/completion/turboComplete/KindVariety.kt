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
 * Represents a family of completion kinds.
 * One [KindCollector] collects kinds from the same family (from Java, Kotlin K1, Kotlin K2, etc.)
 */

interface KindVariety {
  /**
   * Checks, if the kind variety can be collected withing the given parameters
   */
  fun kindsCorrespondToParameters(parameters: CompletionParameters): Boolean

  /**
   * Temporary workaround
   *
   * Currently, [SuggestionGenerator] is a "fixed"
   * version of a [com.intellij.codeInsight.completion.CompletionContributor].
   * We can't dynamically remove one contributor, so we need to filter it out, so we don't have duplicates
   * (suggestions from the contributor, and from the duplicating suggestion generator).
   * And to do this, we must know, what is the actual contributor, that we are filtering out.
   */
  val actualCompletionContributorClass: Class<*>
}