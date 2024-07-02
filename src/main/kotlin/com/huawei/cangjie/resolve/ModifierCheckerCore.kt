package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.psi.CjDeclarationWithBody
import com.huawei.cangjie.psi.CjModifierList
import com.huawei.cangjie.psi.CjModifierListOwner
import com.intellij.lang.ASTNode

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

    ) {
//        if (listOwner is CjDeclarationWithBody) {
//            // CjFunction or CjPropertyAccessor
//            for (parameter in listOwner.valueParameters) {
//                if (!parameter.hasValOrVar()) {
//                    check(parameter, trace, trace[BindingContext.VALUE_PARAMETER, parameter])
//                }
//            }
//        }
//        val actualTargets = AnnotationChecker.getDeclarationSiteActualTargetList(
//            listOwner, descriptor as? ClassDescriptor, trace.bindingContext
//        )
//        val list = listOwner.modifierList ?: return
//        checkModifierList(list, trace, descriptor?.containingDeclaration, actualTargets)
    }
}
