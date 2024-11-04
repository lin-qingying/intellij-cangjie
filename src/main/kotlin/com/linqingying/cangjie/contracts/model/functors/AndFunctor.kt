package com.linqingying.cangjie.contracts.model.functors

//
//class AndFunctor : AbstractBinaryFunctor() {
//    override fun invokeWithConstant(computation: Computation, constant: ESConstant): List<ESEffect> = when {
//        constant.isTrue -> computation.effects
//        constant.isFalse -> emptyList()
//
//        // This means that expression isn't typechecked properly
//        else -> computation.effects
//    }
//
//    override fun invokeWithReturningEffects(left: List<ConditionalEffect>, right: List<ConditionalEffect>): List<ConditionalEffect> {
//        /* Normally, `left` and `right` contain clauses that end with Returns(false/true), but if
//         expression wasn't properly typechecked, we could get some senseless clauses here, e.g.
//         with Returns(1) (note that they still *return* as guaranteed by AbstractSequentialBinaryFunctor).
//         We will just ignore such clauses in order to make smartcasting robust while typing */
//
//        val leftTrue = left.filter { it.simpleEffect.isReturns { value.isTrue } }
//        val leftFalse = left.filter { it.simpleEffect.isReturns { value.isFalse } }
//        val rightTrue = right.filter { it.simpleEffect.isReturns { value.isTrue } }
//        val rightFalse = right.filter { it.simpleEffect.isReturns { value.isFalse } }
//
//        val whenLeftReturnsTrue = foldConditionsWithOr(leftTrue)
//        val whenRightReturnsTrue = foldConditionsWithOr(rightTrue)
//        val whenLeftReturnsFalse = foldConditionsWithOr(leftFalse)
//        val whenRightReturnsFalse = foldConditionsWithOr(rightFalse)
//
//        // Even if one of 'Returns(true)' is missing, we still can argue that other condition
//        // *must* be true when whole functor returns true
//        val conditionWhenTrue = applyWithDefault(whenLeftReturnsTrue, whenRightReturnsTrue) { l, r -> ESAnd(l, r) }
//
//        // When whole And-functor returns false, we can only argue that one of arguments was false, and to do so we
//        // have to know *both* 'Returns(false)'-conditions
//        val conditionWhenFalse = applyIfBothNotNull(whenLeftReturnsFalse, whenRightReturnsFalse) { l, r -> ESOr(l, r) }
//
//        val result = mutableListOf<ConditionalEffect>()
//
//        if (conditionWhenTrue != null) {
//            result.add(ConditionalEffect(conditionWhenTrue, ESReturns(ESConstants.trueValue)))
//        }
//
//        if (conditionWhenFalse != null) {
//            result.add(ConditionalEffect(conditionWhenFalse, ESReturns(ESConstants.falseValue)))
//        }
//
//        return result
//    }
//}
