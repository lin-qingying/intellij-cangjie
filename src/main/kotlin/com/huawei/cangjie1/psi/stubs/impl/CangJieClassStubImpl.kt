package com.huawei.cangjie1.psi.stubs.impl

import com.huawei.cangjie1.name.ClassId
import com.huawei.cangjie1.name.FqName
import com.huawei.cangjie1.psi.CjClass
import com.huawei.cangjie1.psi.stubs.CangJieClassStub
import com.huawei.cangjie1.psi.stubs.elements.CjClassElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import java.util.ArrayList



class CangJieClassStubImpl(
    type: CjClassElementType,
    parent: StubElement<out PsiElement>?,
    private val qualifiedName: StringRef?,
    private val classId: ClassId?,
    private val name: StringRef?,
    private val superNames: Array<StringRef>,
    private val isInterface: Boolean,

    private val isLocal: Boolean,
    private val isTopLevel: Boolean,
) : CangJieStubBaseImpl<CjClass>(parent, type), CangJieClassStub {

    override fun getFqName(): FqName? {
        val stringRef = StringRef.toString(qualifiedName) ?: return null
        return FqName(stringRef)
    }

    override fun isInterface() = isInterface

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

    override fun isTopLevel() = isTopLevel
}
