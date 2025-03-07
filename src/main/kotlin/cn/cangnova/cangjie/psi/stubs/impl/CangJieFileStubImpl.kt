/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.psi.stubs.impl

import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.Name

import cn.cangnova.cangjie.psi.stubs.CangJieFileStub
import cn.cangnova.cangjie.psi.stubs.elements.CjFileElementType


import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType

class CangJieFileStubImpl(
    CjFile: CjFile?,
    private val packageName: String,

    private val facadeFqNameString: String?,
    val partSimpleName: String?,
    val facadePartSimpleNames: List<String>?,
) : PsiFileStubImpl<CjFile>(CjFile), CangJieFileStub {

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
