// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.linqingying.cangjie.ide.completion.turboComplete

import com.intellij.platform.ml.Tier
import com.linqingying.cangjie.ide.completion.turboComplete.KindVariety
import org.jetbrains.annotations.ApiStatus

/**
 * Data class representing a kind of [com.linqingying.cangjie.ide.completion.turboComplete.SuggestionGenerator]'s suggestions.
 *
 * Each completion kind belongs to a kind variety.
 * The completion kind's name is defined statically, it should be unique among
 * the corresponding [com.linqingying.cangjie.ide.completion.turboComplete.KindVariety].
 */

data class CompletionKind(val name: Enum<*>, val variety: KindVariety)


object TierCompletionKind : Tier<CompletionKind>()
