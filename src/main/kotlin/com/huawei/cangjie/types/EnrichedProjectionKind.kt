package com.huawei.cangjie.types

enum class EnrichedProjectionKind {
    IN, OUT, INV, STAR;

    companion object {
        @JvmStatic
        fun fromVariance(variance: Variance): EnrichedProjectionKind {
            return when (variance) {
                Variance.INVARIANT -> INV

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


           return fromVariance(b)

            // If they are not opposite, return b, because b is either equal to a or b is in/out and a is inv
        }
    }
}


