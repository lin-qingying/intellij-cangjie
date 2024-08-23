package com.huawei.cangjie.ide

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.intellij.openapi.util.Iconable.IconFlags
import com.intellij.psi.PsiElement
import javax.swing.Icon

class CangJieDescriptorIconProvider {
    companion object {
        fun getIcon(
            descriptor: DeclarationDescriptor,
            declaration: PsiElement?,
            @IconFlags flags: Int
        ): Icon? {
            return null
//        if (declaration != null && declaration !is CjElement) {
//            return declaration.getIcon(flags)
//        }
//
//        var result: Icon? =  getBaseIcon(descriptor)
//        if ((flags and Iconable.ICON_FLAG_VISIBILITY) > 0) {
//            val rowIcon = RowIcon(2)
//            rowIcon.setIcon(result, 0)
//            rowIcon.setIcon( getVisibilityIcon(descriptor), 1)
//            result = rowIcon
//        }
//
//        return result
        }
    }
}
