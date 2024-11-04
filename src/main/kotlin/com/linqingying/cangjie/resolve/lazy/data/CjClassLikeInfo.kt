package com.linqingying.cangjie.resolve.lazy.data

import com.linqingying.cangjie.descriptors.ClassKind
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.*
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
