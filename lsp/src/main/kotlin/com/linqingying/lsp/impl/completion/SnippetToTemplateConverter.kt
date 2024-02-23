package com.linqingying.lsp.impl.completion

import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.codeInsight.template.impl.Variable
import com.intellij.openapi.project.Project
import kotlin.jvm.internal.Intrinsics


internal class SnippetToTemplateConverter(
    val project: Project,
    val rawSnippetText: String
) {


    private fun addEndMarkerToTemplate(templateText: String): String {
        return "$templateText\$END$"

    }

    fun computeEffectiveLookup(): String {
        return TEMPLATE_VARIABLE_REGEX.replace(rawSnippetText as CharSequence, "")

    }

    internal fun computeTemplate(): Template {
        val variables = extractTemplateVariables()
        val template = TemplateManager.getInstance(project)
            .createTemplate("", "", generateSnippet(hasVariableWithIndexZero(variables)))

        for (variable in convertToVariables(variables)) {
            template.addVariable(variable)
        }

        template.setInline(false)

        return template
    }

    private fun generateSnippet(hasExplicitEndVariable: Boolean): String {
        val str: String = replaceTemplateVariables()
        return if (hasExplicitEndVariable) str else addEndMarkerToTemplate(str)
    }

    private fun extractTemplateVariable(match: MatchResult): TemplateVariable? {

        val intOrNull = extractGroupValue(match, *VARIABLE_NUMBER_GROUPS).toIntOrNull()
        return if (intOrNull != null) {


            TemplateVariable(intOrNull, rawPlaceHolderToStringList(extractGroupValue(match, 2)))
        } else {
            null
        }
    }

    private fun convertToVariables(parsedVariables: List<TemplateVariable>): List<Variable> {
        return parsedVariables.asSequence()
            .distinctBy { it }
            .sortedWith(compareBy { it.index })
            .map { templateVariableToVariable(it) }
            .toList()
    }

    private fun extractGroupValue(
        matchResult: MatchResult,
        vararg groupIndexes: Int
    ): String {
        val result = groupIndexes.asSequence().mapNotNull { groupIndex ->
            val groupValues = matchResult.groupValues
            val groupValueList = if (groupValues.size > groupIndex) groupValues else null
            if (groupValueList != null) {
                val group = groupValueList[groupIndex]
                if (group.isNotEmpty()) {
                    return@mapNotNull group
                }
            }
            null
        }.firstOrNull()
        return result ?: ""
    }

    private fun hasVariableWithIndexZero(variables: List<TemplateVariable>): Boolean {
        return variables.any { it.index == 0 }
    }

    private fun templateVariableToVariable(variable: TemplateVariable): Variable {
        val str = "$VARIABLE_NAME_PREFIX${variable.index}"
        val node = ConstantNode("").withLookupStrings(variable.completionVariants)
        return Variable(str, node as Expression, node as Expression, true, false)
    }

    private fun rawPlaceHolderToStringList(rawPlaceHolder: String): List<String> {
        return if (rawPlaceHolder.startsWith('|') && rawPlaceHolder.endsWith('|')) {
            val str = rawPlaceHolder.trim('|')
            val charArray = charArrayOf(',')
            str.split(*charArray)
        } else {
            emptyList()
        }
    }

    private fun extractTemplateVariables(): List<TemplateVariable> {
        return TEMPLATE_VARIABLE_REGEX.findAll(

            this.rawSnippetText

        ).mapNotNull {
            extractTemplateVariable(it)
        }.filter {
            it.index != 0
        }.toList()

    }

    private fun replaceTemplateVariables(): String {
        return TEMPLATE_VARIABLE_REGEX.replace(rawSnippetText) { matchResult ->
            val num = extractGroupValue(matchResult, *VARIABLE_NUMBER_GROUPS).toIntOrNull()
            if (num != null) {
                if (num == 0) {
                    END_VARIABLE
                } else {
                    "\$$VARIABLE_NAME_PREFIX$num$"
                }
            } else {
                ""
            }
        }
    }

    private data class TemplateVariable(
        val index: Int,
        val completionVariants: List<String>
    ) {


        override operator fun equals(other: Any?): Boolean { /* compiled code */
            if (this === other) {
                return true
            } else if (other !is TemplateVariable) {
                return false
            } else {
                val var2 = other
                return if (this.index != var2.index) {
                    false
                } else {
                    Intrinsics.areEqual(this.completionVariants, var2.completionVariants)
                }
            }
        }


        override fun toString(): String { /* compiled code */
            return "TemplateVariable(index=" + this.index + ", completionVariants=" + this.completionVariants + ")"

        }

        override fun hashCode(): Int {
            var result = index
            result = 31 * result + completionVariants.hashCode()
            return result
        }
    }
}

