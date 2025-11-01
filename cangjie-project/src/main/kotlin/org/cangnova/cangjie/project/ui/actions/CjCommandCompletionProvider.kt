/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.project.ui.actions

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.externalSystem.service.execution.cmd.ParametersListLexer
import com.intellij.openapi.project.Project
import com.intellij.util.TextFieldCompletionProvider
import com.intellij.util.execution.ParametersListUtil
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.service.CjProjectBuildSystemService

/**
 * 命令基类
 */
abstract class CmdBase(
    val name: String
) {
    abstract val options: List<Opt>
    val lookupElement: LookupElement =
        LookupElementBuilder.create(name).withInsertHandler { ctx, _ ->
            ctx.addSuffix(" ")
        }
}

/**
 * 选项类
 */
data class Opt(
    val long: String,
    val short: String? = null,
    val description: String = "",
    val argCompleter: ((CompletionContext) -> List<LookupElement>)? = null
) {
    val lookupElement: LookupElement =
        LookupElementBuilder.create(long)
            .withTailText(if (description.isNotEmpty()) " $description" else "")
            .withInsertHandler { ctx, _ ->
                if (argCompleter != null) {
                    ctx.addSuffix(" ")
                }
            }
}

/**
 * 补全上下文
 */
data class CompletionContext(
    val allProjects: List<CjProject>,
    val currentProject: CjProject?,
    val args: List<String>
)

/**
 * 仓颉命令补全提供者
 *
 * 为 RunAnything 和其他命令输入场景提供自动补全功能
 */
abstract class CjCommandCompletionProvider(
    private val implicitTextPrefix: String = ""
) : TextFieldCompletionProvider() {

    fun splitContextPrefix(text: String): Pair<String, String> {
        val lexer = ParametersListLexer(text)
        var contextEnd = 0
        while (lexer.nextToken()) {
            if (lexer.tokenEnd == text.length) {
                return text.substring(0, contextEnd) to lexer.currentToken
            }
            contextEnd = lexer.tokenEnd
        }

        return text.substring(0, contextEnd) to ""
    }

    override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
        val (ctx, _) = splitContextPrefix(text)
        result.addAllElements(complete(ctx))
    }

    /**
     * 获取可用命令列表
     */
    protected abstract fun getCommands(): List<CmdBase>

    /**
     * 获取补全上下文
     */
    protected abstract fun getContext(args: List<String>): CompletionContext

    /**
     * 执行补全
     */
    fun complete(context: String): List<LookupElement> {
        val args = ParametersListUtil.parse(implicitTextPrefix + context)
        if ("--" in args) return emptyList()

        if (args.isEmpty()) {
            return getCommands().map { it.lookupElement }
        }

        val cmd = getCommands().find { it.name == args.firstOrNull() } ?: return emptyList()

        val argCompleter = cmd.options.find { it.long == args.lastOrNull() }?.argCompleter
        if (argCompleter != null) {
            return argCompleter(getContext(args))
        }

        return cmd.options
            .filter { it.long !in args }
            .map { it.lookupElement }
    }
}

/**
 * 基于命令提供者的补全实现
 *
 * 将 CjCommandProvider 的命令转换为补全建议
 */
class CommandProviderCompletionProvider(
    private val project: Project,
    private val projectProvider: () -> CjProject?
) : CjCommandCompletionProvider() {

    private fun getCommandProvider(): CjCommandProvider? {

        val buildSystemService = CjProjectBuildSystemService.getInstance()
        val buildSystemId = buildSystemService.getBuildSystemId()

        return CjCommandProvider.EP_NAME.extensionList.find { provider ->
            buildSystemId == null || provider.getBuildSystemId().id == buildSystemId
        }
    }

    override fun getCommands(): List<CmdBase> {
        val provider = getCommandProvider() ?: return emptyList()
        val cjProject = projectProvider()

        return provider.getAvailableCommands(project, cjProject).map { command ->
            object : CmdBase(command.id) {
                override val options: List<Opt> = command.getOptions().map { (name, description) ->
                    Opt(long = name, description = description)
                }
            }
        }
    }

    override fun getContext(args: List<String>): CompletionContext {
        val cjProject = projectProvider()
        return CompletionContext(
            allProjects = if (cjProject != null) listOf(cjProject) else emptyList(),
            currentProject = cjProject,
            args = args
        )
    }
}

fun InsertionContext.addSuffix(suffix: String) {
    document.insertString(selectionEndOffset, suffix)
    EditorModificationUtil.moveCaretRelatively(editor, suffix.length)
}
