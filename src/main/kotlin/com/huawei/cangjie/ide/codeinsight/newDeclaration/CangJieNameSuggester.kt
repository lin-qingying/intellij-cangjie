package com.huawei.cangjie.ide.codeinsight.newDeclaration

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.isFunctionType
import com.huawei.cangjie.ide.codeinsight.CangJieCodeInsightBundle
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjValueArgument
import com.huawei.cangjie.psi.psiUtil.getOutermostParenthesizerOrThis
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.calls.model.ArgumentMatch
import com.huawei.cangjie.resolve.calls.util.getParentResolvedCall
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.builtIns
import com.huawei.cangjie.utils.isDispatchThread
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.huawei.cangjie.utils.executeInBackgroundWithProgress

/**
 * 名称建议
 */
object  CangJieNameSuggester : AbstractCangJieNameSuggester() {
    fun suggestNamesByExpressionAndType(
        expression: CjExpression,
        type: CangJieType?,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean,
        defaultName: String?
    ): Collection<String> {
        return executeInBackgroundWithProgress(expression.project) {
            LinkedHashSet<String>().apply {
                addNamesByExpression(expression, bindingContext, validator)

                (type ?: bindingContext?.getType(expression))?.let {
                    addNamesByType(it, validator)
                }

                if (isEmpty()) {
                    addName(defaultName, validator)
                }
            }.toList()
        }
    }

    fun suggestNamesByType(type: CangJieType, validator: (String) -> Boolean, defaultName: String? = null): List<String> =
        executeInBackgroundWithProgress(null) {
            ArrayList<String>().apply {
                addNamesByType(type, validator)
                if (isEmpty()) {
                    ProgressManager.checkCanceled()
                    addName(defaultName, validator)
                }
            }
        }

    private fun executeInBackgroundWithProgress(project: Project?, blockToExecute: () -> List<String>): List<String> =
        if (isDispatchThread() && !ApplicationManager.getApplication().isWriteAccessAllowed) {
            executeInBackgroundWithProgress(
                project,
                CangJieCodeInsightBundle.message("progress.title.calculating.names")
            ) { runReadAction { blockToExecute() } }
        } else {
            blockToExecute()
        }

    fun suggestNamesByExpressionOnly(
        expression: CjExpression,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean, defaultName: String? = null
    ): List<String> {
        val result = ArrayList<String>()

        result.addNamesByExpression(expression, bindingContext, validator)

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    fun suggestIterationVariableNames(
        collection: CjExpression,
        elementType: CangJieType,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean, defaultName: String?
    ): Collection<String> {
        val result = LinkedHashSet<String>()

        suggestNamesByExpressionOnly(collection, bindingContext, { true })
            .mapNotNull { name -> StringUtil.unpluralize(name)}
            .filter { name -> !name.isKeyword() }
            .mapTo(result) { suggestNameByName(it, validator) }

        result.addNamesByType(elementType, validator)

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    private fun String?.isKeyword() = this in CjTokens.KEYWORDS.types.map { it.toString() }

    private fun MutableCollection<String>.addNamesByType(type: CangJieType, validator: (String) -> Boolean) {
        val myType = TypeUtils.makeNotNullable(type) // wipe out '?'
        val builtIns = myType.builtIns
        val typeChecker = CangJieTypeChecker.DEFAULT
        if (ErrorUtils.containsErrorType(myType)) return
        val typeDescriptor = myType.constructor.declarationDescriptor
        when {
            typeChecker.equalTypes(builtIns.boolType, myType) -> addName("i", validator)
            typeChecker.equalTypes(builtIns.int32Type, myType) -> addName("i", validator)
            typeChecker.equalTypes(builtIns.int8Type, myType) -> addName("i", validator)
            typeChecker.equalTypes(builtIns.int64Type, myType) -> addName("i", validator)
            typeChecker.equalTypes(builtIns.float32Type, myType) -> addName("fl", validator)
            typeChecker.equalTypes(builtIns.float64Type, myType) -> addName("fl", validator)
            typeChecker.equalTypes(builtIns.float16Type, myType) -> addName("fl", validator)

            typeChecker.equalTypes(builtIns.int16Type, myType) -> addName("i", validator)
            typeChecker.equalTypes(builtIns.runeType, myType) -> addName("r", validator)
            typeChecker.equalTypes(builtIns.stringType, myType) -> addName("s", validator)
            myType.isFunctionType -> addName("function", validator)
//            CangJieBuiltIns.isArray(myType) || CangJieBuiltIns.isPrimitiveArray(myType) -> {
//                addNamesForArray(builtIns, myType, validator, typeChecker)
//            }
            typeDescriptor != null && DescriptorUtils.isSubtypeOfClass(typeDescriptor.defaultType, builtIns.iterable.original)
                    && type.arguments.isNotEmpty() ->
                addNameForIterableInheritors(type, validator)
            else -> {
                val name = getTypeName(myType)
                if (name != null) {
                    addCamelNames(name, validator)
                }
                addNamesFromGenericParameters(myType, validator)
            }
        }
    }


    private fun MutableCollection<String>.addNameForIterableInheritors(type: CangJieType, validator: (String) -> Boolean) {
        val typeArgument = type.arguments.singleOrNull()?.type ?: return
        val name = getTypeName(typeArgument)
        if (name != null) {
            addCamelNames(StringUtil.pluralize(name), validator)
            val typeName = getTypeName(type)
            if (typeName != null) {
                addCamelNames(name + typeName, validator)
            }
        }
    }

    private fun MutableCollection<String>.addNamesFromGenericParameters(type: CangJieType, validator: (String) -> Boolean) {
        val typeName = getTypeName(type) ?: return
        val arguments = type.arguments
        val builder = StringBuilder()
        if (arguments.isEmpty()) return
        for (argument in arguments) {
            val name = getTypeName(argument.type)
            if (name != null) {
                builder.append(name)
            }
        }
        addCamelNames(builder.append(typeName).toString(), validator)
    }

    private fun getTypeName(type: CangJieType): String? {
        val descriptor = type.constructor.declarationDescriptor
        if (descriptor != null) {
            val className = descriptor.name
            if (!className.isSpecial) {
                return className.asString()
            }
        }
        return null
    }

    private fun MutableCollection<String>.addNamesByExpression(
        expression: CjExpression?,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean
    ) {
        if (expression == null) return

        addNamesByValueArgument(expression, bindingContext, validator)
        addNamesByExpressionPSI(expression, validator)
    }

    private fun MutableCollection<String>.addNamesByValueArgument(
        expression: CjExpression,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean
    ) {
        if (bindingContext == null) return
        val argumentExpression = expression.getOutermostParenthesizerOrThis()
        val valueArgument = argumentExpression.parent as? CjValueArgument ?: return
        val resolvedCall = argumentExpression.getParentResolvedCall(bindingContext) ?: return
        val argumentMatch = resolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: return
        val parameter = argumentMatch.valueParameter
        if (parameter.containingDeclaration.hasStableParameterNames()) {
            addName(parameter.name.asString(), validator)
        }
    }
}
