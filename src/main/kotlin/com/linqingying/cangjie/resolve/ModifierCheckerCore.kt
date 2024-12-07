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

package com.linqingying.cangjie.resolve

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.lexer.CjKeywordToken
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.calls.components.getDescriptorKind
import com.linqingying.cangjie.resolve.source.getPsi

/**
 * 检查修饰符
 */
object ModifierCheckerCore {

    /**
     * 检查给定的编程构造是否符合特定的规则或约束
     *
     * @param listOwner 编程构造的修饰符列表所有者，用于获取修饰符信息
     * @param trace 绑定追踪对象，用于记录绑定上下文信息
     * @param descriptor 声明描述符，提供了关于声明的元数据信息
     * @param languageVersionSettings 语言版本设置，用于确定特定语言特性的支持
     */
    fun check(
        listOwner: CjModifierListOwner,
        trace: BindingTrace,
        descriptor: DeclarationDescriptor?,
        languageVersionSettings: LanguageVersionSettings
    ) {
        // 检查修饰符的可见性规则
        checkModifierByVisibility(descriptor, trace)

        // 如果listOwner是声明带有主体的构造，则进一步检查其值参数
        if (listOwner is CjDeclarationWithBody) {
            // CjFunction or CjPropertyAccessor
            for (parameter in listOwner.valueParameters) {
                // 仅当参数没有let或var修饰符时，执行检查
                if (!parameter.hasLetOrVar()) {
                    check(parameter, trace, trace[BindingContext.VALUE_PARAMETER, parameter], languageVersionSettings)
                }
            }
        }
        // 获取实际的目标列表，用于后续的修饰符检查
        val actualTargets = AnnotationChecker.getDeclarationSiteActualTargetList(
            listOwner, descriptor as? ClassDescriptor, trace.bindingContext
        )
        // 获取修饰符列表，如果为空则直接返回
        val list = listOwner.modifierList ?: return
        // 检查修饰符列表的合法性
        checkModifierList(list, trace, descriptor?.containingDeclaration, actualTargets, languageVersionSettings)
    }

    /**
     * 根据修饰符检查可见性
     */
    private fun checkModifierByVisibility(descriptor: DeclarationDescriptor?, trace: BindingTrace) {
        descriptor ?: return
        val visibilitys = mutableListOf<DescriptorVisibility>()
        var modality: Modality? = null
        if (descriptor is CallableMemberDescriptor && descriptor.isMemberFunOrProperty()) {

            if (descriptor.modality == Modality.OPEN || descriptor.modality == Modality.ABSTRACT) {
                if (descriptor.visibility != DescriptorVisibilities.PUBLIC && descriptor.visibility != DescriptorVisibilities.PROTECTED) {
                    modality = descriptor.modality
                    visibilitys.addAll(listOf(DescriptorVisibilities.PUBLIC, DescriptorVisibilities.PROTECTED))
                }
            }
        }
        modality?.let { modality ->
            (descriptor as? DeclarationDescriptorWithSource)?.let { descriptorWithSource ->
                descriptorWithSource.source.getPsi()?.let { psi ->
                    psi.getNameElement()?.let {
                        trace.report(
                            Errors.ABSTRACT_MEMBER_VISIBILITY_ERROR.on(
                                it,
                                modality,
                                descriptor.getDescriptorKind(),
                                visibilitys
                            )
                        )

                    }


                }

            }
        }
    }

    fun PsiElement.getNameElement(): PsiElement? {
        return when (this) {
            is CjNamedDeclaration -> nameIdentifier

            else -> null
        }
    }

    private val MODIFIER_KEYWORD_SET = TokenSet.create(
        *CjTokens.MODIFIER_KEYWORDS_ARRAY,
        CjTokens.CONST_KEYWORD
    )

    private fun checkCompatibility(
        trace: BindingTrace,
        firstNode: ASTNode,
        secondNode: ASTNode,
        owner: PsiElement,
        incorrectNodes: MutableSet<ASTNode>
    ) {
        val firstModifier = firstNode.elementType as CjKeywordToken
        val secondModifier = secondNode.elementType as CjKeywordToken
        when (val compatibility = compatibility(firstModifier, secondModifier)) {
            Compatibility.COMPATIBLE -> {
            }

            Compatibility.REPEATED -> if (incorrectNodes.add(secondNode)) {
                trace.report(Errors.REPEATED_MODIFIER.on(secondNode.psi, firstModifier))
            }

            Compatibility.REDUNDANT ->
                trace.report(Errors.REDUNDANT_MODIFIER.on(secondNode.psi, secondModifier, firstModifier))

            Compatibility.REVERSE_REDUNDANT ->
                trace.report(Errors.REDUNDANT_MODIFIER.on(firstNode.psi, firstModifier, secondModifier))

            Compatibility.DEPRECATED -> {
                trace.report(Errors.DEPRECATED_MODIFIER_PAIR.on(firstNode.psi, firstModifier, secondModifier))
                trace.report(Errors.DEPRECATED_MODIFIER_PAIR.on(secondNode.psi, secondModifier, firstModifier))
            }

            Compatibility.COMPATIBLE_FOR_CLASSES_ONLY, Compatibility.INCOMPATIBLE -> {
                if (compatibility == Compatibility.COMPATIBLE_FOR_CLASSES_ONLY) {
                    if (owner is CjTypeStatement) return
                }
                if (incorrectNodes.add(firstNode)) {
                    trace.report(Errors.INCOMPATIBLE_MODIFIERS.on(firstNode.psi, firstModifier, secondModifier))
                }
                if (incorrectNodes.add(secondNode)) {
                    trace.report(Errors.INCOMPATIBLE_MODIFIERS.on(secondNode.psi, secondModifier, firstModifier))
                }
            }
        }
    }

    // Should return false if error is reported, true otherwise
    private fun checkParent(
        trace: BindingTrace,
        node: ASTNode,
        parentDescriptor: DeclarationDescriptor?,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        return false
//        val modifier = node.elementType as CjModifierKeywordToken
//
//        val actualParents: List<CangJieTarget> = when (parentDescriptor) {
//            is ClassDescriptor -> CangJieTarget.classActualTargets(
//                parentDescriptor.kind,
//                isInnerClass = parentDescriptor.isInner,
//                isCompanionObject = parentDescriptor.isCompanionObject,
//                isLocalClass = DescriptorUtils.isLocal(parentDescriptor)
//            )
//            is PropertySetterDescriptor -> CangJieTarget.PROPERTY_SETTER_LIST
//            is PropertyGetterDescriptor -> CangJieTarget.PROPERTY_GETTER_LIST
//            is FunctionDescriptor -> CangJieTarget.FUNCTION_LIST
//            else -> CangJieTarget.FILE_LIST
//        }
//        val deprecatedParents = deprecatedParentTargetMap[modifier]
//        if (deprecatedParents != null && actualParents.any { it in deprecatedParents }) {
//            trace.report(
//                Errors.DEPRECATED_MODIFIER_CONTAINING_DECLARATION.on(
//                    node.psi,
//                    modifier,
//                    actualParents.firstOrNull()?.description ?: "this scope"
//                )
//            )
//            return true
//        }
//        if (modifier == PROTECTED_KEYWORD && isFinalExpectClass(parentDescriptor)) {
//            trace.report(
//                Errors.WRONG_MODIFIER_CONTAINING_DECLARATION.on(
//                    node.psi,
//                    modifier,
//                    "final expect class"
//                )
//            )
//        }
//        val possibleParentPredicate = possibleParentTargetPredicateMap[modifier] ?: return true
//        if (actualParents.any { possibleParentPredicate.isAllowed(it, languageVersionSettings) }) return true
//        trace.report(
//            Errors.WRONG_MODIFIER_CONTAINING_DECLARATION.on(
//                node.psi,
//                modifier,
//                actualParents.firstOrNull()?.description ?: "this scope"
//            )
//        )
//        return false
    }


    /**
     * 检查修饰符列表的正确性
     *
     * 此函数负责分析给定的修饰符列表，确保其中的修饰符组合是合法的，并且与当前的语言版本设置兼容
     * 它还会根据实际的目标和父描述符来验证修饰符的适用性
     *
     * @param list 修饰符列表，包含了一系列的修饰符节点
     * @param trace 绑定追踪对象，用于记录解析过程中的绑定信息
     * @param parentDescriptor 父描述符，表示当前修饰符列表所属的声明的父级描述符
     * @param actualTargets 实际目标列表，表示当前修饰符列表所修饰的目标
     * @param languageVersionSettings 语言版本设置，表示当前Kotlin语言的版本配置
     */
    private fun checkModifierList(
        list: CjModifierList,
        trace: BindingTrace,
        parentDescriptor: DeclarationDescriptor?,

        actualTargets: List<CangJieTarget>,
        languageVersionSettings: LanguageVersionSettings
    ) {
        // 如果列表是存根，则直接返回，不进行检查
        if (list.stub != null) return

        // 检查一些前置条件
        // 1 如果是 sealed 则必须有 abstract
        checkSealed(list, trace)

        // 它是所有已报告错误的节点列表
        // 通用策略: 不报告超过一个错误，但可以报告任何数量的警告
        val incorrectNodes = hashSetOf<ASTNode>()

        // 获取修饰符列表中的所有子节点
        val children = list.node.getChildren(MODIFIER_KEYWORD_SET)
        // 双层循环遍历所有子节点，检查修饰符之间的兼容性
        for (second in children) {
            for (first in children) {
                // 如果两个节点相同，则跳过当前循环，避免重复检查
                if (first == second) {
                    break
                }
                // 检查两个修饰符节点是否兼容，如果不兼容且未报告错误，则记录错误
                checkCompatibility(trace, first, second, list.owner, incorrectNodes)
            }
            // 如果当前修饰符节点未被记录为错误，则进一步检查其目标和父级的适用性
            if (second !in incorrectNodes) {
                when {
                    // 检查修饰符是否适用于实际目标，如果不适用，则记录错误
                    !checkTarget(trace, second, actualTargets) -> incorrectNodes += second
                    // 检查修饰符是否与父描述符兼容，如果不兼容，则记录错误
                    !checkParent(trace, second, parentDescriptor, languageVersionSettings) -> incorrectNodes += second
                    // 检查修饰符是否得到当前语言版本的支持，如果不支持，则记录错误
                    // !checkLanguageLevelSupport(trace, second, languageVersionSettings, actualTargets) -> incorrectNodes += second
                }
            }
        }
    }


    /**
     * 检查修饰符的目标是否正确。
     *
     * @param trace 用于记录错误的跟踪对象。
     * @param node 当前检查的语法树节点。
     * @param actualTargets 实际的目标列表。
     * @return 如果报告了错误则返回 `false`，否则返回 `true`。
     */
    private fun checkTarget(trace: BindingTrace, node: ASTNode, actualTargets: List<CangJieTarget>): Boolean {
        // 获取修饰符关键字标记
        val modifier = node.elementType as CjKeywordToken

        // 获取修饰符可能的目标集合，如果不存在则默认为空集合
        val possibleTargets = possibleTargetMap[modifier] ?: emptySet()

        // 检查实际目标是否在可能的目标集合中
        if (!actualTargets.any { it in possibleTargets }) {
            trace.report(
                Errors.WRONG_MODIFIER_TARGET.on(
                    node.psi,
                    modifier,
                    actualTargets.firstOrNull()?.description ?: "this"
                )
            )
            return false
        }

        // 获取已废弃的目标集合
        val deprecatedTargets = deprecatedTargetMap[modifier] ?: emptySet()

        // 获取冗余的目标集合
        val redundantTargets = redundantTargetMap[modifier] ?: emptySet()

        // 检查实际目标是否包含已废弃或冗余的目标
        when {
            actualTargets.any { it in deprecatedTargets } -> {
                trace.report(
                    Errors.DEPRECATED_MODIFIER_FOR_TARGET.on(
                        node.psi,
                        modifier,
                        actualTargets.firstOrNull()?.description ?: "this"
                    )
                )
            }

            actualTargets.any { it in redundantTargets } -> {
                trace.report(
                    Errors.REDUNDANT_MODIFIER_FOR_TARGET.on(
                        node.psi,
                        modifier,
                        actualTargets.firstOrNull()?.description ?: "this"
                    )
                )
            }
        }
        return true
    }


    private fun checkSealed(list: CjModifierList, trace: BindingTrace) {
        if (list.hasModifier(CjTokens.SEALED_KEYWORD)) {
            if (!list.hasModifier(CjTokens.ABSTRACT_KEYWORD) && list.parent !is CjInterface) {
                trace.report(Errors.SEALED_ABSTRACT.on(list))


            }
        }
    }
}


/**
 * 是否为成员方法或者成员属性
 */
fun DeclarationDescriptor?.isMemberFunOrProperty(): Boolean {
    this ?: return false
    val source = (this as? DeclarationDescriptorWithSource)?.source?.getPsi() ?: return false
    if (this !is FunctionDescriptor && this !is PropertyDescriptor) return false
    if (source !is CjNamedFunction && source !is CjProperty) return false

    if (source is CjNamedFunction) {
        if (source.isTopLevel) return false
        if (source.parent !is CjAbstractClassBody) return false
    }



    return true
}
