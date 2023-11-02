package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.stubs.CangJieFileStub
import com.huawei.cangjie.psi.stubs.elements.CjFileElementType


import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType

class CangJieFileStubImpl(
    CjFile: CjFile?,
    private val packageName: String,

    private val facadeFqNameString: String?,
    val partSimpleName: String?,
    val facadePartSimpleNames: List<String>?,
) : PsiFileStubImpl<CjFile>(CjFile), CangJieFileStub{

    constructor(CjFile: CjFile?, packageName: String,  ) : this(
        CjFile,
        packageName,

        facadeFqNameString = null,
        partSimpleName = null,
        facadePartSimpleNames = null
    )

    private fun String.relativeToPackage() = getPackageFqName().child(Name.identifier(this))

    val partFqName: FqName?
        get() = partSimpleName?.relativeToPackage()

    val facadeFqName: FqName?
        get() = facadeFqNameString?.let(::FqName)

    override fun getPackageFqName(): FqName = FqName(packageName)

    override fun getType(): IStubFileElementType<CangJieFileStub> = CjFileElementType.INSTANCE

    override fun toString(): String = "PsiJetFileStubImpl[" + "package=" + getPackageFqName().asString() + "]"

//    override fun getClasses(): Array<PsiClass> {
//        return childrenStubs.filterIsInstance<PsiClassStub<*>>().map { it.psi }.toTypedArray()
//    }



    companion object {
        fun forFile(packageFqName: FqName): CangJieFileStubImpl = CangJieFileStubImpl(
            CjFile = null,
            packageName = packageFqName.asString(),
            facadeFqNameString = null,
            partSimpleName = null,
            facadePartSimpleNames = null,

        )

        fun forFileFacadeStub(facadeFqName: FqName): CangJieFileStubImpl = CangJieFileStubImpl(
            CjFile = null,
            packageName = facadeFqName.parent().asString(),
            facadeFqNameString = facadeFqName.asString(),
            partSimpleName = facadeFqName.shortName().asString(),
            facadePartSimpleNames = null,

        )

        fun forMultifileClassStub(packageFqName: FqName, facadeFqName: FqName, partNames: List<String>?): CangJieFileStubImpl =
            CangJieFileStubImpl(
                CjFile = null,
                packageName = packageFqName.asString(),
                facadeFqNameString = facadeFqName.asString(),
                partSimpleName = null,
                facadePartSimpleNames = partNames,

            )
    }
}
