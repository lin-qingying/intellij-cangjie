package com.huawei.cangjie1.psi.psiUtil

import com.huawei.cangjie1.name.ClassId
import com.huawei.cangjie1.name.FqName
import com.huawei.cangjie1.name.SpecialNames
import com.huawei.cangjie1.psi.CjClassLikeDeclaration
import com.huawei.cangjie1.psi.CjDeclaration
import com.huawei.cangjie1.psi.CjFile


internal object ClassIdCalculator {
    fun calculateClassId(declaration: CjClassLikeDeclaration): ClassId? {
        var CjFile: CjFile? = null
        val containingClasses = mutableListOf<CjClassLikeDeclaration>()

        for (element in declaration.parentsWithSelf) {
            when (element) {

                is CjClassLikeDeclaration -> {
                    containingClasses += element
                }
                is CjFile -> {
                    CjFile = element
                    break
                }


            }
        }

        if (CjFile == null) return null
        val relativeClassName = FqName.fromSegments(
            containingClasses.asReversed().map { containingClass ->
                containingClass.name ?: SpecialNames.NO_NAME_PROVIDED.asString()
            }
        )

        return ClassId(CjFile.packageFqName, relativeClassName, isLocal = false)
    }
}
