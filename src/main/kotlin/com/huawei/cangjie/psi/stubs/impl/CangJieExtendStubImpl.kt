package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjEnum
import com.huawei.cangjie.psi.CjExtend
import com.huawei.cangjie.psi.stubs.CangJieEnumStub
import com.huawei.cangjie.psi.stubs.CangJieExtendStub
import com.huawei.cangjie.psi.stubs.elements.CjEnumElementType
import com.huawei.cangjie.psi.stubs.elements.CjExtendElementType
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



) : CangJieStubBaseImpl<CjExtend>(parent, type), CangJieExtendStub {

    override fun getFqName(): FqName? {
        val stringRef = StringRef.toString(qualifiedName) ?: return null
        return FqName(stringRef)
    }

    override fun getName(): String? = StringRef.toString(name)

    override fun isLocal(): Boolean = true


    override fun getSuperNames(): List<String> {
        val result = ArrayList<String>()
        for (ref in superNames) {
            result.add(ref.toString())
        }
        return result
    }



    override fun getClassId(): ClassId? = classId


}
