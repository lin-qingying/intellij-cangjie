//package com.huawei.cangjie.contracts.model.structure
//
//import com.huawei.cangjie.contracts.model.ESExpression
//import com.huawei.cangjie.contracts.model.ESOperator
//
//
//class ESAnd(val left: ESExpression, val right: ESExpression) : ESOperator {
//    override val functor: AndFunctor = AndFunctor()
//    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitAnd(this)
//}
//
//class ESOr(val left: ESExpression, val right: ESExpression) : ESOperator {
//    override val functor: OrFunctor = OrFunctor()
//    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitOr(this)
//}
//
//class ESNot(val arg: ESExpression) : ESOperator {
//    override val functor = NotFunctor()
//    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitNot(this)
//}
//
//class ESIs(val left: ESValue, override val functor: IsFunctor) : ESOperator {
//    val type = functor.type
//    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitIs(this)
//}
//
//class ESEqual(val left: ESValue, val right: ESValue, isNegated: Boolean) : ESOperator {
//    override val functor: EqualsFunctor = EqualsFunctor(isNegated)
//    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitEqual(this)
//}
//
//fun ESExpression.and(other: ESExpression?): ESExpression =
//    if (other == null) this else ESAnd(this, other)
//
//fun ESExpression.or(other: ESExpression?): ESExpression =
//    if (other == null) this else ESOr(this, other)
