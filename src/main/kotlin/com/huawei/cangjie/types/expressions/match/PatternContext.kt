package com.huawei.cangjie.types.expressions.match

import com.huawei.cangjie.types.expressions.ExpressionTypingContext

data class PatternContext(
  val  subject: Subject,
   val context: ExpressionTypingContext
) {
}
