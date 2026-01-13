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

package org.cangnova.cangjie.completion

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.indices.ExpectedInfo
import org.cangnova.cangjie.psi.psiUtil.getParentOfType
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.references.getCDocLinkMemberScope
import org.cangnova.cangjie.references.getCDocLinkResolutionScope
import org.cangnova.cangjie.references.getParamDescriptors
import org.cangnova.cangjie.references.resolveCDocLink
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.collectDescriptorsFiltered
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns
import com.intellij.util.ProcessingContext
import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocLink
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocName
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * CDoc 文档注释补全贡献者
 *
 * 为仓颉文档注释提供代码补全支持,包括:
 * - 标签名称补全(如 @param, @return 等)
 * - 文档链接中的名称补全
 * - 参数名称补全
 */
class CDocCompletionContributor : CompletionContributor() {

    init {
        // 注册文档链接中的名称补全
        extend(
            CompletionType.BASIC, psiElement().inside(CDocName::class.java),
            CDocNameCompletionProvider
        )

        // 注册标签名称补全(在 * 或文档开始后)
        extend(
            CompletionType.BASIC,
            psiElement().afterLeaf(
                StandardPatterns.or(psiElement(CDocTokens.LEADING_ASTERISK), psiElement(CDocTokens.START))
            ),
            CDocTagCompletionProvider
        )

        // 注册标签名称补全(在标签名称位置)
        extend(
            CompletionType.BASIC,
            psiElement(CDocTokens.TAG_NAME), CDocTagCompletionProvider
        )
    }
}

/**
 * CDoc 名称补全提供者
 *
 * 处理文档注释中的符号引用补全
 */
object CDocNameCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        CDocNameCompletionSession(parameters, result).complete()
    }
}

/**
 * CDoc 名称补全会话
 *
 * 专门处理文档注释中的代码引用补全,包括:
 * - @param 标签中的参数名称
 * - 文档链接中的符号引用(如 [MyClass.method])
 *
 * @property parameters 补全参数
 * @property resultSet 补全结果集
 */
class CDocNameCompletionSession(
    parameters: CompletionParameters,
    resultSet: CompletionResultSet
) :
    CompletionSession(CompletionSessionConfiguration(parameters), parameters, resultSet) {

    /** 不需要描述符类型过滤(文档注释可以引用任何声明) */
    override val descriptorKindFilter: DescriptorKindFilter? get() = null

    /** 文档注释中不使用预期类型信息 */
    override val expectedInfos: Collection<ExpectedInfo> get() = emptyList()

    /**
     * 执行文档注释补全
     *
     * 根据上下文判断是参数补全还是链接补全
     */
    override fun doComplete() {
        val position = parameters.position.getParentOfType<CDocName>(false) ?: return
        val declaration = position.getContainingDoc().getOwner() ?: return
        val cdocLink = position.getStrictParentOfType<CDocLink>()!!
        val declarationDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] ?: return
        withCollectRequiredContextVariableTypes { lookupFactory ->
            if (cdocLink.getTagIfSubject()?.knownTag == CDocKnownTag.PARAM) {
                // @param 标签: 补全参数名称
                addParamCompletions(position, declarationDescriptor, lookupFactory)
            } else {
                // 文档链接: 补全符号引用
                addLinkCompletions(declarationDescriptor, cdocLink, lookupFactory)
            }
        }
    }


    /**
     * 添加参数名称补全
     *
     * 为 @param 标签提供参数名称建议,排除已经文档化的参数
     *
     * @param position 当前位置
     * @param declarationDescriptor 声明描述符(函数或类)
     * @param lookupFactory LookupElement 工厂
     */
    private fun addParamCompletions(
        position: CDocName,
        declarationDescriptor: DeclarationDescriptor,
        lookupFactory: LookupElementFactory,
    ) {
        if (position.getQualifier() != null) return

        val section = position.getContainingSection()
        // 获取已经文档化的参数
        val documentedParameters = section.findTagsByName("param").map { it.getSubjectName() }.toSet()
        // 添加未文档化的参数
        getParamDescriptors(declarationDescriptor)
            .filter { it.name.asString() !in documentedParameters }
            .forEach {
                collector.addElement(
                    lookupFactory.createLookupElement(
                        it,
                        useReceiverTypes = false,
                        parametersAndTypeGrayed = true
                    )
                )
            }
    }

    /**
     * 收集文档链接补全的候选描述符
     *
     * @param declarationDescriptor 声明描述符
     * @param cDocLink 文档链接
     * @return 可用的声明描述符集合
     */
    private fun collectDescriptorsForLinkCompletion(
        declarationDescriptor: DeclarationDescriptor,
        cDocLink: CDocLink
    ): Collection<DeclarationDescriptor> {
        val contextScope = getCDocLinkResolutionScope(resolutionFacade, declarationDescriptor)

        val qualifier = cDocLink.qualifier
        val nameFilter = descriptorNameFilter.toNameFilter()
        return if (qualifier.isNotEmpty()) {
            // 有限定符: 解析限定符并从其成员中查找
            val parentDescriptors =
                resolveCDocLink(
                    bindingContext,
                    resolutionFacade,
                    declarationDescriptor,
                    cDocLink,
                    cDocLink.getTagIfSubject(),
                    qualifier
                )
            parentDescriptors.flatMap {
                val scope = getCDocLinkMemberScope(it, contextScope)
                scope.getContributedDescriptors(nameFilter = nameFilter)
            }
        } else {
            // 无限定符: 从上下文作用域中查找所有可用声明
            contextScope.collectDescriptorsFiltered(DescriptorKindFilter.ALL, nameFilter, changeNamesForAliased = true)
        }
    }

    /**
     * 添加文档链接补全
     *
     * 为文档链接提供符号引用建议
     *
     * @param declarationDescriptor 声明描述符
     * @param cDocLink 文档链接
     * @param lookupFactory LookupElement 工厂
     */
    private fun addLinkCompletions(
        declarationDescriptor: DeclarationDescriptor,
        cDocLink: CDocLink,
        lookupFactory: LookupElementFactory
    ) {
        collectDescriptorsForLinkCompletion(declarationDescriptor, cDocLink).forEach {
            val element = lookupFactory.createLookupElement(it, useReceiverTypes = true, parametersAndTypeGrayed = true)
            collector.addElement(
                // 仅插入简单名称,不包含限定符、括号等
                LookupElementDecorator.withDelegateInsertHandler(element, EmptyDeclarativeInsertHandler)
            )
        }
    }
}
