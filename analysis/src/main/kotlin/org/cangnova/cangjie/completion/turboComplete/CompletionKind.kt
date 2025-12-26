// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.cangnova.cangjie.completion.turboComplete


import org.cangnova.cangjie.completion.turboComplete.KindVariety


/**
 * Data class representing a kind of [org.cangnova.cangjie.completion.turboComplete.SuggestionGenerator]'s suggestions.
 *
 * Each completion kind belongs to a kind variety.
 * The completion kind's name is defined statically, it should be unique among
 * the corresponding [org.cangnova.cangjie.completion.turboComplete.KindVariety].
 */

data class CompletionKind(val name: Enum<*>, val variety: KindVariety)


object TierCompletionKind : Tier<CompletionKind>()


abstract class Tier<T : Any> {
    /**
     * A unique name of a tier (among other tiers in your application).
     * Class name is used by default.
     */
    open val name: String
        get() = this.javaClass.simpleName

    override fun toString(): String = name
}