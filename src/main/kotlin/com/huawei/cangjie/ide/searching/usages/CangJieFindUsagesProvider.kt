package com.huawei.cangjie.ide.searching.usages

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.ide.IdeDescriptorRenderers
import com.huawei.cangjie.psi.CjFunction
import com.huawei.cangjie.resolve.caches.unsafeResolveToDescriptor
import com.huawei.cangjie.types.checker.SimpleClassicTypeSystemContext.isUnit
import com.intellij.psi.PsiElement




class CangJieFindUsagesProvider : CangJieFindUsagesProviderBase() {

    override fun getDescriptiveName(element: PsiElement): String {

        if (element !is CjFunction) return super.getDescriptiveName(element)

        val name = element.name ?: ""
        val descriptor = element.unsafeResolveToDescriptor() as FunctionDescriptor
        val renderer = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS
        val paramsDescription =
            descriptor.valueParameters.joinToString(prefix = "(", postfix = ")") { renderer.renderType(it.type) }
        val returnType = descriptor.returnType
        val returnTypeDescription = if (returnType != null && !returnType.isUnit()) renderer.renderType(returnType) else null
        val funDescription = "$name$paramsDescription" + (returnTypeDescription?.let { ": $it" } ?: "")
        return element.containerDescription?.let { CangJieBundle.message("find.usage.provider.0.of.1", funDescription, it) }
            ?: CangJieBundle.message("find.usage.provider.0", funDescription)
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        getDescriptiveName(element)
}
