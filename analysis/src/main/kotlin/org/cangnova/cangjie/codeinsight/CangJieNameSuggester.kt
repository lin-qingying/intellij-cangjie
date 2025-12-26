/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.codeinsight

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.psi.psiUtil.getOutermostParenthesizerOrThis
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.calls.model.ArgumentMatch
import org.cangnova.cangjie.resolve.calls.util.getParentResolvedCall
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.utils.isDispatchThread
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CangJieCodeInsightBundle
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.isIdentifier
import org.cangnova.cangjie.psi.psiUtil.unquoteCangJieIdentifier
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.builtIns
import org.cangnova.cangjie.types.isFunctionType
import org.cangnova.cangjie.utils.decapitalizeSmart
import org.cangnova.cangjie.utils.executeInBackgroundWithProgress
import java.awt.EventQueue.isDispatchThread

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
        val myType = TypeUtils.makeNonOption(type) // wipe out '?'
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
            myType.isFunctionType -> addName("function", validator)
//            CangJieBuiltIns.isArray(myType) || CangJieBuiltIns.isPrimitiveArray(myType) -> {
//                addNamesForArray(builtIns, myType, validator, typeChecker)
//            }
            typeDescriptor != null && DescriptorUtils.isSubtypeOfClass(typeDescriptor.defaultType, builtIns.stdlibTypes.iterable.original)
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

    private val ACCESSOR_PREFIXES = arrayOf("get", "is", "set")
      const val MAX_NUMBER_OF_SUGGESTED_NAME_CHECKS = 1000
    fun suggestNamesByExpressionPSI(expression: CjExpression?, validator: (String) -> Boolean): Sequence<String> {
        val simpleExpressionName = getSimpleExpressionName(expression) ?: return emptySequence()
        return getCamelNames(simpleExpressionName, validator)
    }
    private fun getSimpleExpressionName(expression: CjExpression?): String? {
        if (expression == null) return null
        return when (val deparenthesized = CjPsiUtil.safeDeparenthesize(expression)) {
            is CjSimpleNameExpression -> deparenthesized.referencedName
            is CjQualifiedExpression -> getSimpleExpressionName(deparenthesized.selectorExpression)
            is CjCallExpression -> getSimpleExpressionName(deparenthesized.calleeExpression)
            is CjPostfixExpression -> getSimpleExpressionName(deparenthesized.baseExpression)
            else -> null
        }
    }
    private fun extractIdentifiers(s: String): String {
        return buildString {
            val lexer = CangJieLexer()
            lexer.start(s)
            while (lexer.tokenType != null) {
                if (lexer.tokenType == CjTokens.IDENTIFIER) {
                    append(lexer.tokenText)
                }
                lexer.advance()
            }
        }
    }

    private fun cutAccessorPrefix(name: String): String? {
        if (name === "" || !name.unquoteCangJieIdentifier().isIdentifier()) return null
        val s = extractIdentifiers(name)

        for (prefix in ACCESSOR_PREFIXES) {
            if (!s.startsWith(prefix)) continue

            val len = prefix.length
            if (len < s.length && Character.isUpperCase(s[len])) {
                return s.substring(len)
            }
        }

        return s
    }

    /**
     * Decapitalizes the passed [name] if [mustStartWithLowerCase] is `true`, checks whether the result is a valid identifier,
     * validates it using [validator], and improves it by adding a numeric suffix in case of conflicts.
     */
    fun suggestNameByValidIdentifierName(
        name: String?,
        validator: (String) -> Boolean,
        mustStartWithLowerCase: Boolean = true
    ): String? {
        if (name == null) return null
        if (mustStartWithLowerCase) return suggestNameByValidIdentifierName(
            name.decapitalizeSmart(),
            validator,
            false
        )
        val correctedName = when {
            name.isIdentifier() -> name
            name == "class" -> "clazz"
            else -> return null
        }
        return suggestNameByName(correctedName, validator)
    }



    fun getCamelNames(
        name: String,
        validator: (String) -> Boolean,
        startLowerCase: Boolean = true
    ): Sequence<String> {
        val s = cutAccessorPrefix(name) ?: return emptySequence()

        var upperCaseLetterBefore = false
        return sequence {
            for (i in s.indices) {
                val c = s[i]
                val upperCaseLetter = Character.isUpperCase(c)

                if (i == 0) {
                    suggestNameByValidIdentifierName(s, validator, startLowerCase)?.let { yield(it) }
                } else {
                    if (upperCaseLetter && !upperCaseLetterBefore) {
                        val substring = s.substring(i)
                        suggestNameByValidIdentifierName(substring, validator, startLowerCase)?.let { yield(it) }
                    }
                }

                upperCaseLetterBefore = upperCaseLetter
            }
        }
    }
}




