package com.linqingying.cangjie.types.expressions.match

import com.linqingying.cangjie.types.expressions.ExpressionTypingContext

data class PatternContext(
  val  subject: Subject,
   val context: ExpressionTypingContext
) {
}
