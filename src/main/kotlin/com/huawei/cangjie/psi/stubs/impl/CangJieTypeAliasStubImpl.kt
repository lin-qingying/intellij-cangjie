package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjTypeAlias
import com.huawei.cangjie.psi.stubs.CangJieTypeAliasStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef


class CangJieTypeAliasStubImpl(
     parent: StubElement<out PsiElement>?,
     private val name: StringRef?,
     private val qualifiedName: StringRef?,
     private val classId: ClassId?,

) :CangJieStubBaseImpl<CjTypeAlias>(parent, CjStubElementTypes.TYPEALIAS), CangJieTypeAliasStub {
     override fun getClassId(): ClassId?  =  classId

     override fun getFqName(): FqName? =     StringRef.toString(qualifiedName)?.let(::FqName)

     override fun getName(): String?  =     StringRef.toString(name)
 }
