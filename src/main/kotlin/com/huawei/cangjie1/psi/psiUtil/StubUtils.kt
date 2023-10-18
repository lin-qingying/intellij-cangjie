package com.huawei.cangjie1.psi.psiUtil

import com.huawei.cangjie1.name.ClassId
import com.huawei.cangjie1.psi.CjClassLikeDeclaration
import com.huawei.cangjie1.psi.stubs.CangJieClassifierStub
import com.huawei.cangjie1.psi.stubs.CangJieFileStub
import com.huawei.cangjie1.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream



object StubUtils {
    @JvmStatic
    fun deserializeClassId(dataStream: StubInputStream): ClassId? {
        val classId = dataStream.readName() ?: return null
        return ClassId.fromString(classId.string)
    }

    @JvmStatic
    fun serializeClassId(dataStream: StubOutputStream, classId: ClassId?) {
        dataStream.writeName(classId?.asString())
    }

    @JvmStatic
    fun createNestedClassId(parentStub: StubElement<*>, currentDeclaration: CjClassLikeDeclaration): ClassId? = when {
        parentStub is CangJieFileStub -> ClassId(parentStub.getPackageFqName(), currentDeclaration.nameAsSafeName)

//        parentStub is CangJiePlaceHolderStub<*> && parentStub.stubType == CjStubElementTypes.CLASS_BODY -> {
//            val containingClassStub = parentStub.parentStub as? CangJieClassifierStub
//            if (containingClassStub != null && currentDeclaration !is CjEnumEntry) {
//                containingClassStub.getClassId()?.createNestedClassId(currentDeclaration.nameAsSafeName)
//            } else {
//                null
//            }
//        }
        else -> null
    }
}
