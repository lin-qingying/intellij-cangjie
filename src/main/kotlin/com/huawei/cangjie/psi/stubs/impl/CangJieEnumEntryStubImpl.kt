package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjEnumEntry
import com.huawei.cangjie.psi.stubs.CangJieEnumEntryStub
import com.huawei.cangjie.psi.stubs.elements.CjEnumEntryElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef


open class CangJieEnumEntryStubImpl(
    type: CjEnumEntryElementType,
    parent: StubElement<out PsiElement>?,
    private val qualifiedName: StringRef?,
    private val classId: ClassId?,
    private val name: StringRef?,

//    private val isInterface: Boolean,

    private val isLocal: Boolean,
//    private val isTopLevel: Boolean,
) : CangJieStubBaseImpl<CjEnumEntry>(parent, type), CangJieEnumEntryStub {

    override fun getFqName(): FqName? {
        val stringRef = StringRef.toString(qualifiedName) ?: return null
        return FqName(stringRef)
    }

//    override fun isInterface() = isInterface

    override fun isLocal() = isLocal
    override fun getName() = StringRef.toString(name)

    override fun getSuperNames(): List<String>  = emptyList()
    override fun getClassId(): ClassId? = classId

//    override fun isTopLevel() = isTopLevel
}
