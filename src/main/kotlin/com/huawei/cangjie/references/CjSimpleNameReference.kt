package com.huawei.cangjie.references

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjLabelReferenceExpression
import com.huawei.cangjie.psi.CjReferenceExpression
import com.huawei.cangjie.psi.CjSimpleNameExpression
import com.huawei.cangjie.psi.psiUtil.startOffset
import com.huawei.cangjie.resolve.BindingContext
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.util.SmartList


class CjSimpleNameReference(expression: CjSimpleNameExpression) : CjSimpleReference<CjSimpleNameExpression>(expression),
    CjReference {
    enum class ShorteningMode {
        NO_SHORTENING,
        DELAYED_SHORTENING,
        FORCED_SHORTENING
    }
    fun bindToElement(element: PsiElement, shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING): PsiElement =
        getCjReferenceMutateService().bindToElement(this, element, shorteningMode)

    override val resolvesByNames: Collection<Name>
        get() {

            val element = element




            return listOf(element.getReferencedNameAsName())

        }
    // It's a copy of function in BindingContextUtils supporting some special cases (labels, this)
    private fun CjExpression.getReferenceTargets(context: BindingContext): Collection<DeclarationDescriptor> {
        val descriptor = when (this) {
            is CjLabelReferenceExpression -> {
                val target = context[BindingContext.LABEL_TARGET, this]
                target?.let { context[BindingContext.DECLARATION_TO_DESCRIPTOR, it] }
            }
            is CjReferenceExpression -> {
                context[BindingContext.REFERENCE_TARGET, this]
            }
            else -> {
                null
            }
        }
        if (descriptor != null) return listOf(descriptor)
        return context[BindingContext.AMBIGUOUS_REFERENCE_TARGET, this].orEmpty()
    }
    override fun getTargetDescriptors(bindingContext: BindingContext): Collection<DeclarationDescriptor> {
        return SmartList<DeclarationDescriptor>().apply {
            // Replace Java property with its accessor(s)
            for (descriptor in expression.getReferenceTargets(bindingContext)) {
                val sizeBefore = size
//                if (descriptor !is JavaPropertyDescriptor) {
                    add(descriptor)
//                    continue
//                }


//                val readWriteAccess = expression.readWriteAccess(true)
//                descriptor.getter?.let {
//                    if (readWriteAccess.isRead) add(it)
//                }
//                descriptor.setter?.let {
//                    if (readWriteAccess.isWrite) add(it)
//                }

                if (size == sizeBefore) {
                    add(descriptor)
                }
            }
        }
    }


    override fun getRangeInElement(): TextRange {
        val element = element.getReferencedNameElement()
        val startOffset = getElement().startOffset
        return element.textRange.shiftRight(-startOffset)
    }
}


