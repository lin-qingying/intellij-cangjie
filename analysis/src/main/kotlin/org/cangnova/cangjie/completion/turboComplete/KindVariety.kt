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