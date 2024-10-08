package com.huawei.cangjie.highlighter

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.getParentOfTypeAndBranch
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.calls.util.FakeCallableDescriptorForObject
import com.huawei.cangjie.resolve.calls.util.getResolvedCall
import com.huawei.cangjie.resolve.unwrapIfTypeAlias
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.PsiTreeUtil

internal class TypeKindHighlightingVisitor(holder: HighlightInfoHolder, bindingContext: BindingContext) :
    AfterAnalysisHighlightingVisitor(holder, bindingContext) {

    override fun visitSimpleNameExpression(expression: CjSimpleNameExpression) {
        val parent = expression.parent
        if (parent is CjSuperExpression || parent is CjThisExpression) {
            // Do nothing: 'super' and 'this' are highlighted as a keyword
            return
        }

        // Prevent custom highlighting for a name that is a part of a definitely non-nullable type
        // (the only kind of intersection types that is currently supported by the compiler).
        // The type is highlighted as a whole in `visitIntersectionType`.
//        if (parent?.parent?.parent?.safeAs<CjIntersectionType>() != null) return

        val referenceTarget = computeReferencedDescriptor(expression) ?: return
        val textRange = computeHighlightingRangeForUsage(expression, referenceTarget)
        val key = attributeKeyForObjectAccess(expression) ?: calculateDeclarationReferenceAttributes(referenceTarget)
        ?: return
        highlightName(expression.project, textRange, key)
    }

    private fun attributeKeyForObjectAccess(expression: CjSimpleNameExpression): HighlightInfoType? {
        val resolvedCall = expression.getResolvedCall(bindingContext)
        return if (resolvedCall?.resultingDescriptor is FakeCallableDescriptorForObject)
            attributeKeyForCallFromExtensions(expression, resolvedCall)
        else null
    }

    private fun computeReferencedDescriptor(expression: CjSimpleNameExpression): DeclarationDescriptor? {
        val referenceTarget = bindingContext.get(BindingContext.REFERENCE_TARGET, expression)

        if (referenceTarget !is ConstructorDescriptor) return referenceTarget

        val callElement = expression.getParentOfTypeAndBranch<CjCallExpression>(true) { calleeExpression }
            ?: expression.getParentOfTypeAndBranch<CjSuperTypeCallEntry>(true) { calleeExpression }

        if (callElement != null) {
            return referenceTarget
        }

        return referenceTarget.containingDeclaration
    }


    private fun computeHighlightingRangeForUsage(
        expression: CjSimpleNameExpression,
        referenceTarget: DeclarationDescriptor
    ): TextRange {
        val target = referenceTarget.unwrapIfTypeAlias() as? ClassDescriptor
        if (target?.kind != ClassKind.ANNOTATION_CLASS) return expression.textRange

        // include '@' symbol if the reference is the first segment of CjAnnotationEntry
        // if "Deprecated" is highlighted then '@' should be highlighted too in "@Deprecated"
        val annotationEntry = PsiTreeUtil.getParentOfType(
            expression, CjAnnotationEntry::class.java, /* strict = */false, CjValueArgumentList::class.java
        )
        val atSymbol = annotationEntry?.atSymbol ?: return expression.textRange
        return TextRange(atSymbol.textRange.startOffset, expression.textRange.endOffset)
    }

    override fun visitTypeStatement(classOrObject: CjTypeStatement) {
        val identifier = classOrObject.nameIdentifier
        val classDescriptor = bindingContext.get(BindingContext.CLASS, classOrObject)
        if (identifier != null && classDescriptor != null) {
            highlightName(
                identifier,
                attributeKeyForDeclarationFromExtensions(classOrObject, classDescriptor)
                    ?: calculateClassReferenceAttributes(classDescriptor)
            )
        }
        super.visitTypeStatement(classOrObject)
    }

    override fun visitTypeAlias(typeAlias: CjTypeAlias) {
        val identifier = typeAlias.nameIdentifier
        val descriptor = bindingContext.get(BindingContext.TYPE_ALIAS, typeAlias)
        if (identifier != null && descriptor != null) {
            val key = attributeKeyForDeclarationFromExtensions(identifier, descriptor)
                ?: calculateTypeAliasReferenceAttributes(descriptor)

            highlightName(identifier, key)
        }
        super.visitTypeAlias(typeAlias)
    }

    private fun calculateClassReferenceAttributes(target: ClassDescriptor): HighlightInfoType {
        return when (target.kind) {
            ClassKind.ANNOTATION_CLASS -> CangJieHighlightInfoTypeSemanticNames.ANNOTATION
            ClassKind.INTERFACE -> CangJieHighlightInfoTypeSemanticNames.TRAIT

            ClassKind.ENUM -> CangJieHighlightInfoTypeSemanticNames.ENUM
            ClassKind.ENUM_ENTRY -> CangJieHighlightInfoTypeSemanticNames.ENUM_ENTRY
            else -> if (target.modality === Modality.ABSTRACT) CangJieHighlightInfoTypeSemanticNames.ABSTRACT_CLASS else CangJieHighlightInfoTypeSemanticNames.CLASS
        }
    }

    private fun calculateTypeAliasReferenceAttributes(target: TypeAliasDescriptor): HighlightInfoType {
        val aliasedTarget = target.expandedType.constructor.declarationDescriptor
        return if (aliasedTarget is ClassDescriptor && aliasedTarget.kind == ClassKind.ANNOTATION_CLASS) CangJieHighlightInfoTypeSemanticNames.ANNOTATION else CangJieHighlightInfoTypeSemanticNames.TYPE_ALIAS
    }

    private fun calculateDeclarationReferenceAttributes(target: DeclarationDescriptor): HighlightInfoType? {
        return when (target) {
            is TypeParameterDescriptor -> CangJieHighlightInfoTypeSemanticNames.TYPE_PARAMETER
            is TypeAliasDescriptor -> calculateTypeAliasReferenceAttributes(target)
            is ClassDescriptor -> calculateClassReferenceAttributes(target)
            else -> null
        }
    }
}
