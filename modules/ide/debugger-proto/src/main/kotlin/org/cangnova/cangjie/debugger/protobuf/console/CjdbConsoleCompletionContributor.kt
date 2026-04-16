/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.debugger.protobuf.console

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key

/**
 * UserData Key 用于在 PsiFile 中存储 executor
 */
val CJDB_EXECUTOR_KEY = Key.create<CjdbConsoleExecutor>("CJDB_CONSOLE_EXECUTOR")

/**
 * Cjdb 控制台补全贡献者
 *
 * 为 LLDB 语言提供命令补全功能
 * 通过 plugin.xml 注册到 completion.contributor 扩展点
 */
class CjdbConsoleCompletionContributor : CompletionContributor() {

    companion object {
        private val LOG = Logger.getInstance(CjdbConsoleCompletionContributor::class.java)
    }

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        // 检查语言是否匹配
        if (parameters.originalFile.language != CangJieDebugProtoLLDBLanguage) {
            super.fillCompletionVariants(parameters, result)
            return
        }

        LOG.info("fillCompletionVariants called for LLDB language")

        // 从 PsiFile 的 UserData 中获取 executor
        val executor = parameters.originalFile.getUserData(CJDB_EXECUTOR_KEY)

        if (executor == null) {
            LOG.warn("Executor not found in UserData")
            super.fillCompletionVariants(parameters, result)
            return
        }

        LOG.info("Executor found, getting completions")

        // 获取原始文本,移除 IntelliJ 的补全标记
        val originalText = parameters.originalFile.text
        val position = parameters.position
        val offset = parameters.offset

        // IntelliJ 会在光标位置插入 "IntellijIdeaRulezzz" 作为占位符
        // 我们需要移除这个标记来获取真实的命令文本
        val actualText = originalText.replace("IntellijIdeaRulezzz", "")

        // 计算实际的光标位置(需要考虑标记被移除后的偏移)
        val actualOffset = if (offset > actualText.length) actualText.length else offset

        LOG.info("Original text: '$originalText'")
        LOG.info("Actual text: '$actualText', offset: $actualOffset")

        try {
            // 获取补全建议
            val completions = executor.getCompletions(actualText, actualOffset)

            LOG.info("Got ${completions.size} completions")

            // 添加到结果集
            completions.forEach { completion ->
                result.addElement(
                    LookupElementBuilder.create(completion)
                        .withTypeText("LLDB command")
                        .withCaseSensitivity(true)
                )
            }
        } catch (e: Exception) {
            LOG.error("Failed to get completions", e)
        }

        // 调用父类方法以支持其他补全
        super.fillCompletionVariants(parameters, result)
    }
}