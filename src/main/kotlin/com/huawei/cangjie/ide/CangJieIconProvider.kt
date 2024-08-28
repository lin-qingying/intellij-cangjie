package com.huawei.cangjie.ide

import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjTypeAlias
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.psi.psiUtil.isPrivate
import com.intellij.ide.IconProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.text.StringUtil

abstract class CangJieIconProvider : IconProvider(), DumbAware {

    companion object{
        fun getSingleClass(file: CjFile): CjTypeStatement? {
            var targetDeclaration: CjDeclaration? = null
            for (declaration: CjDeclaration in file.declarations) {
                if (!declaration.isPrivate() && declaration !is CjTypeAlias) {
                    if (targetDeclaration != null) return null
                    targetDeclaration = declaration
                }
            }
            return targetDeclaration?.takeIf { it is CjTypeStatement && StringUtil.getPackageName(file.name) == it.name } as? CjTypeStatement
        }

    }
}
