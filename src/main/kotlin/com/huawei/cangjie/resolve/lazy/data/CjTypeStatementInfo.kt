package com.huawei.cangjie.resolve.lazy.data

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.*
import com.intellij.psi.PsiElement

abstract class CjTypeStatementInfo<E : CjTypeStatement>(
    protected open val element: E
) : CjClassLikeInfo {
    override val correspondingClass: CjTypeStatement
        get() = element


    val elementByE get() = element

    override val danglingAnnotations: List<CjAnnotationEntry>
        get() {
//            val body: CjAbstractClassBody? = element.body
//            return if (body == null) emptyList() else body.danglingAnnotations
            return emptyList()
        }
    val name get() = element.nameAsSafeName

    override val scopeAnchor: PsiElement
        get() = element


    override val containingPackageFqName: FqName
        get() {
            val file = element.containingFile
            if (file is CjFile) {

                return file.packageFqName
            }
            throw IllegalArgumentException("Not in a CjFile: $element")
        }
    override val modifierList: CjModifierList?
        get() = element.modifierList


    override val declarations: List<CjDeclaration>
        get() = element.declarations

    override fun toString(): String {
        return "info for " + element.text
    }

    override val primaryConstructorParameters: List<CjParameter>
        get() = element.primaryConstructorParameters
}
