package com.huawei.cangjie.ide.statistics.compilationError

import com.huawei.cangjie.descriptors.Errors
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomValidationRule
import java.lang.reflect.Field
import java.lang.reflect.Modifier

class CangJieCompilationErrorIdValidationRule : CustomValidationRule() {
    override fun getRuleId(): String = "kotlin.compilation.error.id"
    override fun doValidate(data: String, context: EventContext): ValidationResultType =
        if (allowedCompilationErrorsIds.contains(data)) ValidationResultType.ACCEPTED else ValidationResultType.REJECTED
}

private val allowedCompilationErrorsIds: List<String> =
    listOf(Errors::class.java ).flatMap { clazz ->
        clazz.fields.filter { Modifier.isStatic(it.modifiers) }.map(Field::getName)
    }
