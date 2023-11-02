package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjClass
import com.huawei.cangjie.psi.CjInterface
import com.huawei.cangjie.psi.stubs.CangJieClassStub
import com.huawei.cangjie.psi.stubs.CangJieInterfaceStub
import com.huawei.cangjie.psi.stubs.elements.CjClassElementType
import com.huawei.cangjie.psi.stubs.elements.CjInterfaceElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import java.util.ArrayList


open class CangJieInterfaceStubImpl(
    type: CjInterfaceElementType,
    parent: StubElement<out PsiElement>?,
    private val qualifiedName: StringRef?,
    private val classId: ClassId?,
    private val name: StringRef?,
    private val superNames: Array<StringRef>,


    private val isLocal: Boolean,
//    private val isTopLevel: Boolean,
) : CangJieStubBaseImpl<CjInterface>(parent, type), CangJieInterfaceStub {

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

//    override fun isTopLevel() = isTopLevel
}
