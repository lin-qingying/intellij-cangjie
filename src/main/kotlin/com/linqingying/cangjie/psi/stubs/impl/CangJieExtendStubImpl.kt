package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjEnum
import com.linqingying.cangjie.psi.CjExtend
import com.linqingying.cangjie.psi.stubs.CangJieEnumStub
import com.linqingying.cangjie.psi.stubs.CangJieExtendStub
import com.linqingying.cangjie.psi.stubs.elements.CjEnumElementType
import com.linqingying.cangjie.psi.stubs.elements.CjExtendElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import java.util.ArrayList


open class CangJieExtendStubImpl(
    type: CjExtendElementType,
    parent: StubElement<out PsiElement>?,
    private val qualifiedName: StringRef?,
    private val classId: ClassId?,
    private val name: StringRef?,
    private val superNames: Array<StringRef>,

    private val isLocal: Boolean,

) : CangJieStubBaseImpl<CjExtend>(parent, type), CangJieExtendStub {

    override fun getFqName(): FqName? {
        val stringRef = StringRef.toString(qualifiedName) ?: return null
        return FqName(stringRef)
    }


    override fun isLocal() = isLocal
    override fun getName() = StringRef.toString(name)

    override fun getSuperNames(): List<String> {
        val result = ArrayList<String>()
        for (ref in superNames) {
            result.add(ref.toString())
        }
        return result
    }

    override fun getClassId(): ClassId? = classId


}
