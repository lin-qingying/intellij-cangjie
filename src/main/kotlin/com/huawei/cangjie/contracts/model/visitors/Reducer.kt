//package com.huawei.cangjie.contracts.model.visitors
//
//import com.huawei.cangjie.builtins.CangJieBuiltIns
//import com.huawei.cangjie.contracts.model.ESEffect
//import com.huawei.cangjie.contracts.model.ESExpression
//import com.huawei.cangjie.contracts.model.ESExpressionVisitor
//import com.huawei.cangjie.contracts.model.structure.ESReceiver
//
///**
// * Reduces given list of effects by evaluating constant expressions,
// * throwing away senseless checks and infeasible clauses, etc.
// */
//class Reducer(private val builtIns: CangJieBuiltIns) : ESExpressionVisitor<ESExpression?> {
//    fun reduceEffects(schema: List<ESEffect>): List<ESEffect> =
//        schema.mapNotNull { reduceEffect(it) }
//
//    private fun reduceEffect(effect: ESEffect): ESEffect? {
//        when (effect) {
//            is ConditionalEffect -> {
//                // Reduce condition
//                val reducedCondition = effect.condition.accept(this) ?: return null
//
//                // Filter never executed conditions
//                if (reducedCondition.isFalse) return null
//
//                // Add always firing effects
//                if (reducedCondition.isTrue) return effect.simpleEffect
//
//                // Leave everything else as is
//                return effect
//            }
//            else -> return effect
//        }
//    }
//
//    override fun visitIs(isOperator: ESIs): ESExpression {
//        val reducedArg = isOperator.left.accept(this) as ESValue
//
//        val argType = reducedArg.type?.toCangJieType(builtIns)
//        val isType = isOperator.functor.type.toCangJieType(builtIns)
//
//        val result = when (reducedArg) {
//            is ESConstant -> argType!!.isSubtypeOf(isType)
//            is ESVariable, is ESReceiver -> if (argType?.isSubtypeOf(isType) == true) true else null
//            else -> throw IllegalStateException("Unknown ESValue: $reducedArg")
//        }
//
//        // Result is unknown, do not evaluate
//        result ?: return ESIs(reducedArg, isOperator.functor)
//
//        return ESConstants.booleanValue(result.xor(isOperator.functor.isNegated))
//    }
//
//    override fun visitEqual(equal: ESEqual): ESExpression? {
//        val reducedLeft = equal.left.accept(this) as ESValue? ?: return null
//        val reducedRight = equal.right
//
//        if (reducedLeft is ESConstant) return ESConstants.booleanValue((reducedLeft == reducedRight).xor(equal.functor.isNegated))
//
//        return ESEqual(reducedLeft, reducedRight, equal.functor.isNegated)
//    }
//
//    override fun visitAnd(and: ESAnd): ESExpression? {
//        val reducedLeft = and.left.accept(this) ?: return null
//        val reducedRight = and.right.accept(this) ?: return null
//
//        return when {
//            reducedLeft.isFalse || reducedRight.isFalse -> reducedLeft
//            reducedLeft.isTrue -> reducedRight
//            reducedRight.isTrue -> reducedLeft
//            else -> ESAnd(reducedLeft, reducedRight)
//        }
//    }
//
//    override fun visitOr(or: ESOr): ESExpression? {
//        val reducedLeft = or.left.accept(this) ?: return null
//        val reducedRight = or.right.accept(this) ?: return null
//
//        return when {
//            reducedLeft.isTrue || reducedRight.isTrue -> reducedLeft
//            reducedLeft.isFalse -> reducedRight
//            reducedRight.isFalse -> reducedLeft
//            else -> ESOr(reducedLeft, reducedRight)
//        }
//    }
//
//    override fun visitNot(not: ESNot): ESExpression? {
//        val reducedArg = not.arg.accept(this) ?: return null
//
//        return when {
//            reducedArg.isTrue -> ESConstants.falseValue
//            reducedArg.isFalse -> ESConstants.trueValue
//            else -> reducedArg
//        }
//    }
//
//    override fun visitVariable(esVariable: ESVariable): ESVariable = esVariable
//
//    override fun visitConstant(esConstant: ESConstant): ESConstant = esConstant
//
//    override fun visitReceiver(esReceiver: ESReceiver): ESReceiver = esReceiver
//
//    override fun visitLambda(lambda: ESValue): ESExpression? {
//        return null
//    }
//}
