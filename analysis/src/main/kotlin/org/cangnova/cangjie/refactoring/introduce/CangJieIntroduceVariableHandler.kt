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

package org.cangnova.cangjie.refactoring.introduce


import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType
import org.cangnova.cangjie.psi.psiUtil.isFunctionalExpression
import org.cangnova.cangjie.psi.psiUtil.parents
import org.cangnova.cangjie.psi.psiUtil.parentsWithSelf
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.util.SmartList
import org.cangnova.cangjie.messages.CangJieRefactorBundle
import org.cangnova.cangjie.refactoring.CangJieRefactoringSettings
import org.cangnova.cangjie.refactoring.IntroduceRefactoringException
import org.cangnova.cangjie.refactoring.chooseContainer.selectContainerIfNeeded
import org.cangnova.cangjie.refactoring.selectElement


/**
 * 变量提取处理动作
 * 用于处理从代码中提取变量到新的作用域的重构操作。
 * 该类负责处理变量提取的核心逻辑，包括：
 * 1. 查找可提取的表达式
 * 2. 确定提取变量的目标作用域
 * 3. 执行变量提取和替换操作
 */
abstract class CangJieIntroduceVariableHandler : RefactoringActionHandler {
    object DEFAULT : CangJieIntroduceVariableHandler() {
        /**
         * 过滤包含lambda表达式的容器
         * 该方法用于分析并过滤包含lambda表达式的容器，确保变量提取在正确的作用域内进行。
         *
         * @param containersWithContainedLambdas 包含lambda的容器序列
         * @param physicalExpression 要提取的物理表达式
         * @param referencesFromExpressionToExtract 从表达式中提取的引用列表
         * @return 过滤后的容器序列，包含有效的提取目标容器
         */
        override fun filterContainersWithContainedLambdasByAnalyze(
            containersWithContainedLambdas: Sequence<ContainerWithContained>,
            physicalExpression: CjExpression,
            referencesFromExpressionToExtract: List<CjReferenceExpression>
        ): Sequence<ContainerWithContained> {
            return emptySequence()
        }

        /**
         * 在选定的目标容器中执行变量提取
         * 该方法负责在指定的容器中执行实际的变量提取操作。
         *
         * @param project 当前项目
         * @param editor 编辑器实例
         * @param expression 要提取的表达式
         * @param containers 目标容器信息
         * @param isVar 是否为可变变量
         * @param occurrencesToReplace 需要替换的表达式出现位置列表
         * @param onNonInteractiveFinish 非交互式完成时的回调函数
         */
        override fun doRefactoringWithSelectedTargetContainer(
            project: Project,
            editor: Editor?,
            expression: CjExpression,
            containers: Containers,
            isVar: Boolean,
            occurrencesToReplace: List<CjExpression>?,
            onNonInteractiveFinish: ((CjDeclaration) -> Unit)?
        ) {
        }
    }

    protected companion object {
        val INTRODUCE_VARIABLE get() = CangJieRefactorBundle.message("introduce.variable")

        /**
         * 显示错误提示
         * 在编辑器中显示变量提取过程中的错误信息。
         *
         * @param project 当前项目
         * @param editor 编辑器实例
         * @param message 要显示的错误消息
         */
        fun showErrorHint(project: Project, editor: Editor?, @NlsContexts.DialogMessage message: String) {
            CommonRefactoringUtil.showErrorHint(project, editor, message, INTRODUCE_VARIABLE,  "refactoring.introduceVariable")
        }

        /**
         * 判断元素是否为赋值语句的左值
         * 检查当前元素是否作为赋值语句的左操作数。
         *
         * @return 如果是赋值语句的左值则返回true，否则返回false
         */
        fun PsiElement.isAssignmentLHS(): Boolean = parents.any {
            CjPsiUtil.isAssignment(it) && (it as CjBinaryExpression).left == this
        }

        /**
         * 获取表达式的出现容器
         * 查找包含当前表达式的最近的有效容器。
         *
         * @return 包含表达式的容器元素，如果未找到则返回null
         */
        fun CjExpression.getOccurrenceContainer(): CjElement? {
            var result: CjElement? = null
            for ((place, parent) in parentsWithSelf.zip(parents)) {
                when {
                    parent is CjContainerNode && place !is CjBlockExpression && !parent.isBadContainerNode(place) -> result =
                        parent

                    parent is CjClassBody || parent is CjFile || parent is CjFunctionLiteral -> return result
                        ?: parent as? CjElement

                    parent is CjBlockExpression -> result = parent
                    parent is CjMatchEntry && place !is CjBlockExpression -> result = parent
                    parent is CjDeclarationWithBody && parent.bodyExpression == place && place !is CjBlockExpression -> result =
                        parent
                }
            }
            return null
        }

        /**
         * 获取函数表达式或lambda体的lambda表达式
         * 查找当前元素所在的外层lambda表达式。
         *
         * @return 包含当前元素的lambda表达式，如果不存在则返回null
         */
        fun PsiElement.getLambdaForFunExpressionOrLambdaBody(): CjExpression? {
            if (isFunctionalExpression()) return this as? CjFunction
            val parent = (parent as? CjFunction)?.takeIf { it.bodyExpression == this }
            return when {
                parent is CjFunctionLiteral -> parent.parent as? CjLambdaExpression
                parent?.isFunctionalExpression() == true -> parent
                else -> null
            }
        }

        /**
         * 获取元素的容器
         * 查找包含当前元素的最合适的容器。
         *
         * @return 包含该元素的容器，如果未找到则返回null
         */
        fun CjElement.getContainer(): CjElement? {
            if (this is CjBlockExpression || this is CjAbstractClassBody) return this
            return (parentsWithSelf.zip(parents)).firstOrNull {
                val (place, parent) = it
                when (parent) {
                    is CjContainerNode -> !parent.isBadContainerNode(place)
                    is CjBlockExpression -> true
                    is CjMatchEntry -> place == parent.expression
                    is CjDeclarationWithBody -> parent.bodyExpression == place
                    is CjAbstractClassBody -> place !is CjEnumConstructor
                    is CjFile -> true
                    else -> false
                }
            }?.second as? CjElement
        }

        /**
         * 判断容器节点是否为坏节点
         * 检查容器节点是否不适合作为变量提取的目标。
         *
         * @param place 位置元素
         * @return 如果是不适合的容器节点则返回true，否则返回false
         */
        fun CjContainerNode.isBadContainerNode(place: PsiElement): Boolean = when (val parent = parent) {
            is CjIfExpression -> parent.condition == place
            is CjLoopExpression -> parent.body != place
            is CjArrayAccessExpression -> true
            else -> false
        }
    }

    override fun invoke(
        project: Project,
        editor: Editor,
        file: PsiFile,
        dataContext: DataContext
    ) {
        if (file !is CjFile) return

        try {
            selectElement(editor, file, failOnEmptySuggestion = false, listOf(ElementKind.EXPRESSION)) { psi ->

                selectTargetContainerAndPerformIntroduce(
                    project = project,
                    editor = editor,
                    expressionToExtract = psi as CjExpression?,
                    isVar = CangJieRefactoringSettings.instance.INTRODUCE_DECLARE_WITH_VAR
                )

            }
        } catch (e: IntroduceRefactoringException) {
            showErrorHint(
                project,
                editor,
                e.message!!
            )
        }
    }

    override fun invoke(
        project: Project,
        elements: Array<out PsiElement?>,
        dataContext: DataContext?
    ) {
    }

    /**
     * 在选定的容器上执行重构
     * 根据提供的目标容器和候选容器列表执行变量提取操作。
     *
     * @param editor 编辑器实例
     * @param targetContainer 目标容器
     * @param candidateContainers 候选容器列表
     * @param doRefactoring 执行重构的回调函数
     */
    protected fun performRefactoringOnSelectedContainer(
        editor: Editor?,
        targetContainer: CjElement?,
        candidateContainers: List<Containers>,
        doRefactoring: (Containers) -> Unit,
    ) {
        if (targetContainer != null) {
            val foundPair = candidateContainers.find { it.targetContainer.textRange == targetContainer.textRange }
            if (foundPair != null) {
                doRefactoring(foundPair)
            }
        } else if (editor == null) {
            doRefactoring(candidateContainers.first())
        } else {
            selectContainerIfNeeded(
                candidateContainers, editor,
                CangJieRefactorBundle.message("text.select.target.code.block"), true, null, { it.targetContainer },
                doRefactoring
            )
        }
    }

    /**
     * 包含容器和内容的包装类
     * 用于存储容器元素和其中包含的表达式。
     *
     * @property container 容器元素
     * @property contained 包含的表达式
     */
    protected data class ContainerWithContained(val container: CjElement, val contained: CjExpression)

    /**
     * 过滤包含lambda表达式的容器
     * 分析并过滤包含lambda表达式的容器，确保变量提取在正确的作用域内进行。
     *
     * @param containersWithContainedLambdas 包含lambda的容器序列
     * @param physicalExpression 要提取的物理表达式
     * @param referencesFromExpressionToExtract 从表达式中提取的引用列表
     * @return 过滤后的容器序列
     */
    protected abstract fun filterContainersWithContainedLambdasByAnalyze(
        containersWithContainedLambdas: Sequence<ContainerWithContained>,
        physicalExpression: CjExpression,
        referencesFromExpressionToExtract: List<CjReferenceExpression>,
    ): Sequence<ContainerWithContained>

    /**
     * 查找表达式可以被提取变量的候选作用域容器
     * 分析表达式并找出所有可能作为变量提取目标的容器。
     *
     * @return 候选容器列表，包含所有可能的提取目标容器
     */
    fun CjExpression.getCandidateContainers(): List<Containers> {
        val physicalExpression = substringContextOrThis
        val firstContainer = physicalExpression.getContainer() ?: return emptyList()
        val firstOccurrenceContainer = physicalExpression.getOccurrenceContainer() ?: return emptyList()

        val lambda = firstContainer.getLambdaForFunExpressionOrLambdaBody()
        val lambdaContainer = lambda?.getContainer()

        if (lambda == null || lambdaContainer == null) return listOf(
            Containers(
                firstContainer,
                firstOccurrenceContainer
            )
        )

        val containersWithContainedLambdas =
            generateSequence(ContainerWithContained(lambdaContainer, lambda)) { (container, _) ->
                val nextLambda = container.getLambdaForFunExpressionOrLambdaBody() ?: return@generateSequence null
                val nextLambdaContainer = nextLambda.getContainer() ?: return@generateSequence null
                ContainerWithContained(nextLambdaContainer, nextLambda)
            }.let {
                val contentRange = extractableSubstringInfo?.contentRange
                val references = physicalExpression.collectDescendantsOfType<CjReferenceExpression> {
                    contentRange == null || contentRange.contains(it.textRange)
                }
                filterContainersWithContainedLambdasByAnalyze(it, physicalExpression, references)
            }.toList()

        val containers = SmartList(firstContainer)
        val occurrenceContainers = SmartList(firstOccurrenceContainer)

        containersWithContainedLambdas.mapTo(containers) { it.container }
        containersWithContainedLambdas.mapTo(occurrenceContainers) { it.contained.getOccurrenceContainer() }
        return ArrayList<Containers>().apply {
            for ((container, occurrenceContainer) in (containers zip occurrenceContainers)) {
                if (occurrenceContainer == null) continue
                add(Containers(container, occurrenceContainer))
            }
        }
    }

    /**
     * 收集候选目标容器并执行重构
     * 查找所有可能的提取目标容器，并在选定的容器中执行变量提取操作。
     *
     * @param project 当前项目
     * @param editor 编辑器实例
     * @param expressionToExtract 要提取的表达式
     * @param isVar 是否为可变变量
     * @param occurrencesToReplace 需要替换的表达式出现位置列表
     * @param targetContainer 目标容器
     * @param onNonInteractiveFinish 非交互式完成时的回调函数
     */
    fun selectTargetContainerAndPerformIntroduce(
        project: Project,
        editor: Editor?,
        expressionToExtract: CjExpression?,
        isVar: Boolean,
        occurrencesToReplace: List<CjExpression>? = null,
        targetContainer: CjElement? = null,
        onNonInteractiveFinish: ((CjDeclaration) -> Unit)? = null
    ) {
        val expression = expressionToExtract?.let { CjPsiUtil.safeDeparenthesize(it) }
            ?: return showErrorHint(project, editor, CangJieRefactorBundle.message("cannot.refactor.no.expression"))
        if (expression.isAssignmentLHS()) {
            return showErrorHint(
                project,
                editor,
                CangJieRefactorBundle.message("cannot.refactor.no.expression")
            )
        }
        if (!CommonRefactoringUtil.checkReadOnlyStatus(project, expression)) return
        val candidateContainers = expression.getCandidateContainers().ifEmpty {
            return showErrorHint(
                project,
                editor,
                CangJieRefactorBundle.message("cannot.refactor.no.container")
            )
        }
        performRefactoringOnSelectedContainer(editor, targetContainer, candidateContainers) { containers ->
            doRefactoringWithSelectedTargetContainer(
                project, editor, expression, containers,
                isVar, occurrencesToReplace, onNonInteractiveFinish
            )
        }
    }

    /**
     * 在选定的目标容器中执行变量提取
     * 在指定的容器中执行实际的变量提取操作。
     *
     * @param project 当前项目
     * @param editor 编辑器实例
     * @param expression 要提取的表达式
     * @param containers 目标容器信息
     * @param isVar 是否为可变变量
     * @param occurrencesToReplace 需要替换的表达式出现位置列表
     * @param onNonInteractiveFinish 非交互式完成时的回调函数
     */
    abstract fun doRefactoringWithSelectedTargetContainer(
        project: Project,
        editor: Editor?,
        expression: CjExpression,
        containers: Containers,
        isVar: Boolean,
        occurrencesToReplace: List<CjExpression>? = null,
        onNonInteractiveFinish: ((CjDeclaration) -> Unit)? = null,
    )
}

/**
 * 变量提取的候选容器信息
 * 存储变量提取过程中使用的容器信息。
 *
 * @property targetContainer 新变量定义将被插入的目标容器元素
 * @property occurrenceContainer 包含所有需要替换表达式的作用域容器
 */

data class Containers(val targetContainer: CjElement, val occurrenceContainer: CjElement)

