package com.linqingying.cangjie.resolve

import com.intellij.psi.PsiElement
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.diagnostics.DiagnosticFactory3
import com.linqingying.cangjie.diagnostics.Errors.EXPOSED_FROM_PRIVATE_IN_FILE
import com.linqingying.cangjie.diagnostics.Errors.EXPOSED_TYPEALIAS_EXPANDED_TYPE
import com.linqingying.cangjie.psi.CjTypeAlias
import com.linqingying.cangjie.types.isError

// 用于检查所有七种 EXPOSED_* 错误的检查器
// 所有函数在一切正常时返回 true，如果存在任何错误则返回 false
class ExposedVisibilityChecker(
    private val languageVersionSettings: LanguageVersionSettings,
    private val trace: BindingTrace? = null
){

    private fun <E : PsiElement> reportExposure(
        diagnostic: DiagnosticFactory3<E, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility>,
        element: E,
        elementVisibility: EffectiveVisibility,
        restrictingDescriptor: DescriptorWithRelation
    ) {
        val trace = trace ?: return
        val restrictingVisibility = restrictingDescriptor.effectiveVisibility()

        if (!languageVersionSettings.supportsFeature(LanguageFeature.PrivateInFileEffectiveVisibility) &&
            elementVisibility == EffectiveVisibility.PrivateInFile
        ) {
            trace.report(EXPOSED_FROM_PRIVATE_IN_FILE.on(element, elementVisibility, restrictingDescriptor, restrictingVisibility))
        } else {
            trace.report(diagnostic.on(element, elementVisibility, restrictingDescriptor, restrictingVisibility))
        }
    }

    fun checkTypeAlias(typeAlias: CjTypeAlias, typeAliasDescriptor: TypeAliasDescriptor) {
        val expandedType = typeAliasDescriptor.expandedType
        if (expandedType.isError) return

        val typeAliasVisibility = typeAliasDescriptor.effectiveVisibility()
        val restricting = expandedType.leastPermissiveDescriptor(typeAliasVisibility)
        if (restricting != null) {
            reportExposure(EXPOSED_TYPEALIAS_EXPANDED_TYPE, typeAlias.nameIdentifier ?: typeAlias, typeAliasVisibility, restricting)
        }
    }

}
