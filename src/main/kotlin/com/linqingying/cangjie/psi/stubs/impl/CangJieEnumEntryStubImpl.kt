package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjEnumEntry
import com.linqingying.cangjie.psi.stubs.CangJieEnumEntryStub
import com.linqingying.cangjie.psi.stubs.elements.CjEnumEntryElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef


open class CangJieEnumEntryStubImpl(
    type: CjEnumEntryElementType,
    parent: StubElement<out PsiElement>?,
      val qualifiedNameByParent: StringRef?,  //枚举值对于enum声明的名称
      val qualifiedNameByPackage:StringRef?, //枚举值对于包声明的名称
    private val classId: ClassId?,
    private val name: StringRef?,

//    private val isInterface: Boolean,

    private val isLocal: Boolean,
//    private val isTopLevel: Boolean,
) : CangJieStubBaseImpl<CjEnumEntry>(parent, type), CangJieEnumEntryStub {

    override fun getFqName(): FqName? {
        val stringRef = StringRef.toString(qualifiedNameByParent) ?: return null
        return FqName(stringRef)
    }
    override val fqNameByPackage: FqName?get() {
        val stringRef = StringRef.toString(qualifiedNameByPackage) ?: return null
        return FqName(stringRef)
    }

//    override fun isInterface() = isInterface

    override fun isLocal() = isLocal
    override fun getName() = StringRef.toString(name)

    override fun getSuperNames(): List<String>  = emptyList()
    override fun getClassId(): ClassId? = classId

//    override fun isTopLevel() = isTopLevel
}
