package com.huawei.cangjie.types

enum class EnrichedProjectionKind {
    IN, OUT, INV, STAR;

    companion object {
        @JvmStatic
        fun fromVariance(variance: Variance): EnrichedProjectionKind {
            return when (variance) {
                Variance.INVARIANT -> INV
                Variance.IN_VARIANCE -> IN
                Variance.OUT_VARIANCE -> OUT
            }
        }

        // If class C<out T> then C<T> and C<out T> mean the same
        // out * out = out
        // out * in  = *
        // out * inv = out
        //
        // in * out  = *
        // in * in   = in
        // in * inv  = in
        //
        // inv * out = out
        // inv * in  = out
        // inv * inv = inv
        fun getEffectiveProjectionKind(
            typeParameterVariance: Variance,
            typeArgumentVariance: Variance
        ): EnrichedProjectionKind {
            var a = typeParameterVariance
            var b = typeArgumentVariance

            // If they are not both invariant, let's make b not invariant for sure
            if (b === Variance.INVARIANT) {
                val t = a
                a = b
                b = t
            }

            // Opposites yield STAR
            if (a === Variance.IN_VARIANCE && b === Variance.OUT_VARIANCE) {
                return STAR
            }
            return if (a === Variance.OUT_VARIANCE && b === Variance.IN_VARIANCE) {
                STAR
            } else fromVariance(b)

            // If they are not opposite, return b, because b is either equal to a or b is in/out and a is inv
        }
    }
}


