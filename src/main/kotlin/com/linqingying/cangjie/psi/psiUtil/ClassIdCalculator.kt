package com.linqingying.cangjie.psi.psiUtil

import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.SpecialNames
import com.linqingying.cangjie.psi.CjClassLikeDeclaration
import com.linqingying.cangjie.psi.CjFile


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
