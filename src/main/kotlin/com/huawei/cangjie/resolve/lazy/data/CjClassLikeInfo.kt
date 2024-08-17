package com.huawei.cangjie.resolve.lazy.data

import com.huawei.cangjie.descriptors.ClassKind
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.*
import com.intellij.psi.PsiElement


interface CjClassLikeInfo : CjDeclarationContainer {
    val containingPackageFqName: FqName

    val modifierList: CjModifierList?


    // This element is used to identify resolution scope for the class
    val scopeAnchor: PsiElement

    val correspondingClass: CjTypeStatement?

    val typeParameterList: CjTypeParameterList?


    val primaryConstructorParameters: List<CjParameter>

    val classKind: ClassKind

    val danglingAnnotations: List<CjAnnotationEntry>
}
