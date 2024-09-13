package com.huawei.cangjie.resolve.calls.inference.components

enum class ConstraintSystemCompletionMode(val allLambdasShouldBeAnalyzed: Boolean) {
    FULL(true),
    PCLA_POSTPONED_CALL(true),


    PARTIAL(false),
    UNTIL_FIRST_LAMBDA(false),
}
