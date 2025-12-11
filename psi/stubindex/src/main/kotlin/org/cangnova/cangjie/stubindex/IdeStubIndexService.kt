/*
 * Copyright 2025 LinQingYing. and contributors.
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
package org.cangnova.cangjie.stubindex


import org.cangnova.cangjie.name.*
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CangJiePsiHeuristics
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.stubs.*
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import java.io.IOException

internal class IdeStubIndexService : StubIndexService() {
    override fun indexVariable(stub: CangJieVariableStub, sink: IndexSink) {
        val name: String? = stub.name
        if (name != null) {
            sink.occurrence(CangJieVariableShortNameIndex.Helper.indexKey, name)


            val typeReference: CjTypeReference? = stub.psi.typeReference
            if (typeReference != null && CangJiePsiHeuristics.isProbablyNothing(typeReference)) {
                sink.occurrence(CangJieVariableNothingVariableShortNameIndex.Helper.indexKey, name)
            }
            indexPrime(stub, sink)
        }
        if (!stub.childNamesByPattern.isEmpty()) {
            for (childInfo in stub.childNamesByPattern) {
                if (childInfo.name != null) {
                    sink.occurrence(
                        CangJieVariableShortNameIndex.Helper.indexKey,
                        childInfo.name!!.getString()
                    )


                    val typeReference: CjTypeReference? = stub.psi.typeReference
                    if (typeReference != null && CangJiePsiHeuristics.isProbablyNothing(typeReference)) {
                        sink.occurrence(
                            CangJieVariableNothingVariableShortNameIndex.Helper.indexKey,
                            childInfo.name!!.getString()
                        )
                    }
                }
            }
            indexPrime(stub, sink)
        }

        if (stub.isTopLevel()) {
            val fqName: FqName? = stub.getFqName()

            if (fqName != null) {
                sink.occurrence(CangJieTopLevelVariableFqnNameIndex.Helper.indexKey, fqName.asString())
                sink.occurrence(
                    CangJieTopLevelVariableByPackageIndex.Helper.indexKey,
                    fqName.parent().asString()
                )
                indexTopLevelExtension(stub, sink)
            }

            if (!stub.childNamesByPattern.isEmpty()) {
                for (childInfo in stub.childNamesByPattern) {
                    if (childInfo.fqName != null) {
                        sink.occurrence(
                            CangJieTopLevelVariableFqnNameIndex.Helper.indexKey,
                            childInfo.fqName!!.asString()
                        )
                        sink.occurrence(
                            CangJieTopLevelVariableByPackageIndex.Helper.indexKey,
                            childInfo.fqName!!.parent().asString()
                        )
                        indexTopLevelExtension(stub, sink)
                    }
                }
                indexTopLevelExtension(stub, sink)
            }
        }

        indexInternals(stub, sink)
    }

    override fun indexProperty(stub: CangJiePropertyStub, sink: IndexSink) {
        val name: String? = stub.name
        if (name != null) {
            sink.occurrence(CangJiePropertyShortNameIndex.Helper.indexKey, name)

            val typeReference: CjTypeReference? = stub.psi.typeReference
            if (typeReference != null && CangJiePsiHeuristics.isProbablyNothing(typeReference)) {
                sink.occurrence(CangJieProbablyNothingPropertyShortNameIndex.Helper.indexKey, name)
            }
            indexPrime(stub, sink)
        }

        if (stub.isExtension()) {
            val fqName: FqName? = stub.getFqName()
            // can have special fq name in case of syntactically incorrect property with no name
            if (fqName != null) {
                sink.occurrence(CangJieTopLevelPropertyFqnNameIndex.Helper.indexKey, fqName.asString())
                sink.occurrence(
                    CangJieTopLevelPropertyByPackageIndex.Helper.indexKey,
                    fqName.parent().asString()
                )
                indexTopLevelExtension(stub, sink)
            }
        }

        indexInternals(stub, sink)
    }

    override fun indexFile(stub: CangJieFileStub, sink: IndexSink) {
        val packageFqName: FqName = stub.getPackageFqName()

        sink.occurrence(CangJieExactPackagesIndex.NAME, packageFqName.asString())


    }

    override fun indexTypeAlias(stub: CangJieTypeAliasStub, sink: IndexSink) {
        val name: String? = stub.name
        if (name != null) {
            sink.occurrence(CangJieTypeAliasShortNameIndex.Helper.indexKey, name)
            indexPrime(stub, sink)
        }

        indexTypeAliasExpansion(stub, sink)

        val fqName: FqName? = stub.getFqName()
        if (fqName != null) {
            sink.occurrence(CangJieTopLevelTypeAliasFqNameIndex.Helper.indexKey, fqName.asString())
            sink.occurrence(
                CangJieTopLevelTypeAliasByPackageIndex.Helper.indexKey,
                fqName.parent().asString()
            )
        }

    }

    override fun createFileStub(file: CjFile): CangJieFileStub {
        val packageFqName: String? = file.packageFqNameByTree.asString()


        return CangJieFileStubImpl(file, packageFqName ?: "", null, null, null)
    }

    @Throws(IOException::class)
    override fun serializeFileStub(
        stub: CangJieFileStub, dataStream: StubOutputStream
    ) {
        val fileStub: CangJieFileStubImpl = stub as CangJieFileStubImpl
        dataStream.writeName(fileStub.getPackageFqName().asString())

        val facadeFqName: FqName? = fileStub.facadeFqName
        dataStream.writeName(facadeFqName?.asString())
        dataStream.writeName(fileStub.partSimpleName)
        val facadePartNames = fileStub.facadePartSimpleNames
        if (facadePartNames == null) {
            dataStream.writeInt(0)
        } else {
            dataStream.writeInt(facadePartNames.size)
            for (partName in facadePartNames) {
                dataStream.writeName(partName)
            }
        }
    }

    override fun indexMainFunction(stub: CangJieFunctionStub, sink: IndexSink) {
        if (stub.isTopLevel()) {
            // can have special fq name in case of syntactically incorrect function with no name
            val fqName: FqName? = stub.getFqName()
            if (fqName != null) {
                sink.occurrence(CangJieMainFunctionFqnNameIndex.Helper.indexKey, fqName.asString())
                indexTopLevelExtension(stub, sink)
            }
        }

        indexInternals(stub, sink)
    }

    override fun indexMacroFunction(stub: CangJieFunctionStub, sink: IndexSink) {
        val name: String? = stub.name
        if (name != null) {
            sink.occurrence(CangJieMacroDeclarationShortNameIndex.Helper.indexKey, name)

            indexPrime(stub, sink)
        }
        val fqName: FqName? = stub.getFqName()
        if (fqName != null) {
            sink.occurrence(CangJieMacroDeclarationFqnNameIndex.Helper.indexKey, fqName.asString())
            sink.occurrence(
                CangJieMacroDeclarationByPackageIndex.Helper.indexKey,
                fqName.parent().asString()
            )
            indexTopLevelExtension(stub, sink)
        }
    }

    override fun indexFunction(stub: CangJieFunctionStub, sink: IndexSink) {
        val name: String? = stub.name
        if (name != null) {
            sink.occurrence(CangJieFunctionShortNameIndex.Helper.indexKey, name)

            val typeReference: CjTypeReference? = stub.psi.typeReference
            if (typeReference != null && CangJiePsiHeuristics.isProbablyNothing(typeReference)) {
                sink.occurrence(CangJieProbablyNothingFunctionShortNameIndex.Helper.indexKey, name)
            }

            indexPrime(stub, sink)
        }
        //如果该方法是顶层方法或有扩展接收器，则将其索引到顶层
        if (stub.isTopLevel() /*|| stub.isExtension()*/) {
            val fqName: FqName? = stub.getFqName()
            if (fqName != null) {
                sink.occurrence(CangJieTopLevelFunctionFqnNameIndex.Helper.indexKey, fqName.asString())
                sink.occurrence(
                    CangJieTopLevelFunctionByPackageIndex.Helper.indexKey,
                    fqName.parent().asString()
                )
                indexTopLevelExtension(stub, sink)
            }
        }
        if (stub.isExtension()) {
            val fqName: FqName? = stub.getFqName()
            if (fqName != null) {
                sink.occurrence(CangJieTopLevelFunctionFqnNameIndex.Helper.indexKey, fqName.asString())

                indexTopLevelExtension(stub, sink)
            }
        }

        indexInternals(stub, sink)
    }

    override fun indexExtend(stub: CangJieExtendStub, sink: IndexSink) {
        val fqName: FqName? = stub.getFqName()


        if (fqName != null) {
            sink.occurrence(CangJieExtendClassNameIndex.Helper.indexKey, fqName.asString())
        }

        indexSuperNames(stub, sink)
    }

    override fun indexClass(stub: CangJieClassStub, sink: IndexSink) {
        indexTypeStatementStub(stub, sink)
    }

    override fun indexInterface(stub: CangJieInterfaceStub, sink: IndexSink) {
        indexTypeStatementStub(stub, sink)
    }

    override fun indexImports(stub: CangJieImportDirectiveItemStub, sink: IndexSink) {

        val fqName: FqName? = stub.getPackageFqName()
        if (fqName != null) {
            sink.occurrence(CangJieImportFqNameForPackageNameIndex.Helper.indexKey, fqName.asString())

        }
    }

    override fun indexEnumEntry(stub: CangJieEnumEntryStub, sink: IndexSink) {
        processNames(sink, stub.name, stub.getFqName() /*, stub.isTopLevel()*/)
        indexSuperNames(stub, sink)

        indexPrime(stub, sink)
        sink.occurrence(CangJieEnumEntryShortNameIndex.Helper.indexKey, stub.name ?: "")
    }

    override fun indexEnum(stub: CangJieEnumStub, sink: IndexSink) {
        indexTypeStatementStub(stub, sink)
    }

    private fun indexTypeStatementStub(stub: CangJieTypeStatementStub<out CjTypeStatement>, sink: IndexSink) {
        processNames(sink, stub.name, stub.getFqName() /*, stub.isTopLevel()*/)

        if (stub is CangJieInterfaceStub) {
            sink.occurrence(CangJieClassShortNameIndex.Helper.indexKey, "DefaultImpls")
        }

        indexSuperNames(stub, sink)

        indexPrime(stub, sink)
    }

    override fun indexStruct(stub: CangJieStructStub, sink: IndexSink) {
        indexTypeStatementStub(stub, sink)
    }

    private fun processNames(sink: IndexSink, name: String?, fqName: FqName?) {
        processNames(sink, name, fqName, true)
    }

    @Throws(IOException::class)
    override fun deserializeFileStub(dataStream: StubInputStream): CangJieFileStub {
        val packageFqNameAsString: String? = dataStream.readNameString()
        checkNotNull(packageFqNameAsString) { "Can't read package fqname from stream" }


        val facadeString: String? = dataStream.readNameString()
        val partSimpleName: String? = dataStream.readNameString()
        val numPartNames: Int = dataStream.readInt()
        val facadePartNames: MutableList<String> = ArrayList<String>()
        for (i in 0..<numPartNames) {
            val partNameRef: String? = dataStream.readNameString()
            partNameRef?.let { facadePartNames.add(it) }
        }
        return CangJieFileStubImpl(null, packageFqNameAsString, facadeString, partSimpleName, facadePartNames)
    }

    companion object {
        private fun getModifierListStub(stub: CangJieStubWithFqName<*>): CangJieModifierListStub? {
            return stub.findChildStubByType(CjStubElementTypes.MODIFIER_LIST)
        }

        private fun indexSuperNames(stub: CangJieTypeStatementStub<out CjTypeStatement>, sink: IndexSink) {
            for (superName in stub.getSuperNames()) {
                sink.occurrence(CangJieSuperClassIndex.Helper.indexKey, superName)
            }

        }

        private fun processNames(
            sink: IndexSink,
            shortName: String?,
            fqName: FqName?,
            level: Boolean
        ) {
            if (shortName != null) {
                sink.occurrence(CangJieClassShortNameIndex.Helper.indexKey, shortName)
            }

            if (fqName != null) {
                sink.occurrence(CangJieFullClassNameIndex.Helper.indexKey, fqName.asString())

                if (level) {
                    sink.occurrence(
                        CangJieTopLevelClassByPackageIndex.Helper.indexKey,
                        fqName.parent().asString()
                    )
                }
            }
        }

        /**
         * 索引非私有的顶级符号或顶级对象及其伴生对象的成员。
         *
         * @param stub 包含完全限定名称的 CangJieStub 对象
         * @param sink 用于记录索引的 IndexSink 对象
         */
        private fun indexPrime(stub: CangJieStubWithFqName<*>, sink: IndexSink) {
            val name: String? = stub.name
            if (name == null) return

            val modifierList: CangJieModifierListStub? = getModifierListStub(stub)
            if (modifierList != null && modifierList.hasModifier(CjTokens.PRIVATE_KEYWORD)) return
            if (modifierList != null && modifierList.hasModifier(CjTokens.OVERRIDE_KEYWORD)) return

            val parent =
                stub.parentStub
            val prime = parent is CangJieFileStub

            if (prime) {
                sink.occurrence(CangJiePrimeSymbolNameIndex.Helper.indexKey, name)
            }
        }
    }
}
