package com.huawei.cangjie.resolve

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.Errors
import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjDeclarationWithBody
import com.huawei.cangjie.psi.CjModifierList
import com.huawei.cangjie.psi.CjModifierListOwner
import com.huawei.cangjie.psi.CjTypeStatement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

/**
 * 检查修饰符
 */
object ModifierCheckerCore {
    //    private fun checkModifierList(
//        list: CjModifierList,
//        trace: BindingTrace,
//        parentDescriptor: DeclarationDescriptor?,
//        actualTargets: List<CangJieTarget>,
//
//        ) {
//        if (list.stub != null) return
//
//        // It's a list of all nodes with error already reported
//        // General strategy: report no more than one error but any number of warnings
//        val incorrectNodes = hashSetOf<ASTNode>()
//
//        val children = list.node.getChildren(MODIFIER_KEYWORD_SET)
//        for (second in children) {
//            for (first in children) {
//                if (first == second) {
//                    break
//                }
//                checkCompatibility(trace, first, second, list.owner, incorrectNodes)
//            }
//            if (second !in incorrectNodes) {
//                when {
//                    !checkTarget(trace, second, actualTargets) -> incorrectNodes += second
//                    !checkParent(trace, second, parentDescriptor, languageVersionSettings) -> incorrectNodes += second
//                    !checkLanguageLevelSupport(trace, second, languageVersionSettings, actualTargets) -> incorrectNodes += second
//                }
//            }
//        }
//    }
    fun check(
        listOwner: CjModifierListOwner,
        trace: BindingTrace,
        descriptor: DeclarationDescriptor?,
        languageVersionSettings: LanguageVersionSettings

    ) {
        if (listOwner is CjDeclarationWithBody) {
            // CjFunction or CjPropertyAccessor
            for (parameter in listOwner.valueParameters) {
                if (!parameter.hasLetOrVar()) {
                    check(parameter, trace, trace[BindingContext.VALUE_PARAMETER, parameter], languageVersionSettings)
                }
            }
        }
        val actualTargets = AnnotationChecker.getDeclarationSiteActualTargetList(
            listOwner, descriptor as? ClassDescriptor, trace.bindingContext
        )
        val list = listOwner.modifierList ?: return
        checkModifierList(list, trace, descriptor?.containingDeclaration , actualTargets , languageVersionSettings)
    }

    private val MODIFIER_KEYWORD_SET = CjTokens.MODIFIER_KEYWORDS

    private fun checkCompatibility(
        trace: BindingTrace,
        firstNode: ASTNode,
        secondNode: ASTNode,
        owner: PsiElement,
        incorrectNodes: MutableSet<ASTNode>
    ) {
        val firstModifier = firstNode.elementType as CjModifierKeywordToken
        val secondModifier = secondNode.elementType as CjModifierKeywordToken
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
//        val actualParents: List<KotlinTarget> = when (parentDescriptor) {
//            is ClassDescriptor -> KotlinTarget.classActualTargets(
//                parentDescriptor.kind,
//                isInnerClass = parentDescriptor.isInner,
//                isCompanionObject = parentDescriptor.isCompanionObject,
//                isLocalClass = DescriptorUtils.isLocal(parentDescriptor)
//            )
//            is PropertySetterDescriptor -> KotlinTarget.PROPERTY_SETTER_LIST
//            is PropertyGetterDescriptor -> KotlinTarget.PROPERTY_GETTER_LIST
//            is FunctionDescriptor -> KotlinTarget.FUNCTION_LIST
//            else -> KotlinTarget.FILE_LIST
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


    private fun checkModifierList(
        list: CjModifierList,
        trace: BindingTrace,
        parentDescriptor: DeclarationDescriptor?,

        actualTargets: List<CangJieTarget>,
        languageVersionSettings: LanguageVersionSettings
    ) {
        if (list.stub != null) return


//        检查一些前置条件
//        1 如果是 sealed 则必须有 abstract
        checkSealed(list, trace)

        // It's a list of all nodes with error already reported
        // General strategy: report no more than one error but any number of warnings
        val incorrectNodes = hashSetOf<ASTNode>()

        val children = list.node.getChildren(MODIFIER_KEYWORD_SET)
        for (second in children) {
            for (first in children) {
                if (first == second) {
                    break
                }
                checkCompatibility(trace, first, second, list.owner, incorrectNodes)
            }
            if (second !in incorrectNodes) {
                when {
                    !checkTarget(trace, second, actualTargets) -> incorrectNodes += second
                    !checkParent(trace, second, parentDescriptor, languageVersionSettings) -> incorrectNodes += second
//                    !checkLanguageLevelSupport(trace, second, languageVersionSettings, actualTargets) -> incorrectNodes += second
                }
            }
        }
    }
    // Should return false if error is reported, true otherwise
    private fun checkTarget(trace: BindingTrace, node: ASTNode, actualTargets: List<CangJieTarget>): Boolean {
        val modifier = node.elementType as CjModifierKeywordToken

        val possibleTargets = possibleTargetMap[modifier] ?: emptySet()
        if (!actualTargets.any { it in possibleTargets }) {
            trace.report(Errors.WRONG_MODIFIER_TARGET.on(node.psi, modifier, actualTargets.firstOrNull()?.description ?: "this"))
            return false
        }

        val deprecatedTargets = deprecatedTargetMap[modifier] ?: emptySet()
        val redundantTargets = redundantTargetMap[modifier] ?: emptySet()
        when {

            actualTargets.any { it in deprecatedTargets } ->
                trace.report(
                    Errors.DEPRECATED_MODIFIER_FOR_TARGET.on(
                        node.psi,
                        modifier,
                        actualTargets.firstOrNull()?.description ?: "this"
                    )
                )
            actualTargets.any { it in redundantTargets } ->
                trace.report(
                    Errors.REDUNDANT_MODIFIER_FOR_TARGET.on(
                        node.psi,
                        modifier,
                        actualTargets.firstOrNull()?.description ?: "this"
                    )
                )
        }
        return true
    }
    private fun checkSealed(list: CjModifierList, trace: BindingTrace) {
        if (list.hasModifier(CjTokens.SEALED_KEYWORD)) {
            if (!list.hasModifier(CjTokens.ABSTRACT_KEYWORD)) {
                trace.report(Errors.SEALED_ABSTRACT.on(list))


            }
        }
    }
}
