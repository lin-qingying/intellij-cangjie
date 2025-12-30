// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.cangnova.cangjie.completion.turboComplete.ranking

import org.cangnova.cangjie.completion.turboComplete.ranking.RankedKind


/**
 * Listens to the ranking process of [org.cangnova.cangjie.completion.turboComplete.SuggestionGenerator]
 *
 * The same instance of the listener used during all the application lifetime.
 * The callbacks are called in the order of their declaration
 */

interface KindRankingListener {
  /**
   * The ranking process started
   */
  fun onRankingStarted() {}

  /**
   * The kinds were ranked
   */
  fun onRanked(ranked: List<RankedKind>) {}

  /**
   * The ranking process finished
   */
  fun onRankingFinished() {}
}