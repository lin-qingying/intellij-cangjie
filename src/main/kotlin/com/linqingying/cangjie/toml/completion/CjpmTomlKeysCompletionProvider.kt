package com.linqingying.cangjie.toml.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import com.linqingying.cangjie.psi.psiUtil.ancestorStrict
import com.linqingying.cangjie.toml.isDependencyListHeader
import org.intellij.lang.annotations.Language
import org.toml.lang.psi.TomlArrayTable
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlHeaderOwner
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlKeyValueOwner
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.TomlTableHeader

private val TomlKeySegment.topLevelTable: TomlKeyValueOwner?
    get() {
        val table = ancestorStrict<TomlKeyValueOwner>() ?: return null
        if (table.parent !is TomlFile) return null
        return table
    }

class CjpmTomlKeysCompletionProvider : CompletionProvider<CompletionParameters>() {
    private var cachedSchema: TomlSchema? = null

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val schema = cachedSchema
            ?: TomlSchema.parse(parameters.position.project, EXAMPLE_CJPM_TOML).also { cachedSchema = it }

        val key = parameters.position.parent as? TomlKeySegment ?: return
        val table = key.topLevelTable ?: return
        val variants = when (val parent = key.parent?.parent) {
            is TomlTableHeader -> {
                if (key != parent.key?.segments?.firstOrNull()) return
                val isArray = when (table) {
                    is TomlArrayTable -> true
                    is TomlTable -> false
                    else -> return
                }
                schema.topLevelKeys(isArray)
            }

            is TomlKeyValue -> {
                if (table !is TomlHeaderOwner) return
                if (table.header.isDependencyListHeader) return
                schema.keysForTable(table.name ?: return)
            }

            else -> return
        }

        result.addAllElements(variants.map {
            LookupElementBuilder.create(it)
        })
    }

}

private val TomlHeaderOwner.name: String?
    get() = header.key?.segments?.firstOrNull()?.name


@Language("TOML")
private val EXAMPLE_CJPM_TOML = """
"""