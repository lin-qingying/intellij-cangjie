// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.linqingying.cangjie.ide.completion.turboComplete.ranking

import com.linqingying.cangjie.ide.completion.turboComplete.CompletionKind
import org.jetbrains.annotations.ApiStatus


data class RankedKind(
  val kind: CompletionKind,
  val relevance: Double?,
) {
  companion object {
    fun fromWeights(
      kindWeights: Iterable<Pair<CompletionKind, Double>>,
      negateWeight: Boolean,
    ): List<RankedKind> {
      return kindWeights
        .sortedBy { (_, weight) -> if (negateWeight) -weight else weight }
        .map { (kind, weight) -> RankedKind(kind, weight) }
    }
  }
}