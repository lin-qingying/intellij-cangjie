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

package com.linqingying.cangjie.ide.stubindex

import com.linqingying.cangjie.ide.stubindex.CangJieClassShortNameIndex.Helper.indexKey
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.CangJiePsiHeuristics.isProbablyNothing
import com.linqingying.cangjie.psi.stubs.*
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.linqingying.cangjie.psi.stubs.elements.StubIndexService
import com.linqingying.cangjie.psi.stubs.impl.CangJieFileStubImpl
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import java.io.IOException


class CangJieIdeStubIndexService : StubIndexService() {
    override fun createFileStub(file: CjFile): CangJieFileStub {
        val packageFqName = file.packageFqNameByTree.asString()


        return CangJieFileStubImpl(file, packageFqName, null, null, null)
    }

    @Throws(IOException::class)
    override fun serializeFileStub(
        stub: CangJieFileStub, dataStream: StubOutputStream
    ) {
        val fileStub = stub as CangJieFileStubImpl
        dataStream.writeName(fileStub.getPackageFqName().asString())

        val facadeFqName = fileStub.facadeFqName
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

    override fun indexFunction(stub: CangJieFunctionStub, sink: IndexSink) {
        val name = stub.name
        if (name != null) {
            sink.occurrence(CangJieFunctionShortNameIndex.indexKey, name)

            val typeReference = stub.psi.typeReference
            if (typeReference != null && isProbablyNothing(typeReference)) {
                sink.occurrence(CangJieProbablyNothingFunctionShortNameIndex.indexKey, name)
            }

            //
            /*     if (stub.mayHaveContract()) {
                sink.occurrence(CangJieProbablyContractedFunctionShortNameIndex.Helper.getIndexKey(), name);
            }*/
            indexPrime(stub, sink)
        }

        if (stub.isTopLevel()) {
            // can have special fq name in case of syntactically incorrect function with no name
            val fqName = stub.getFqName()
            if (fqName != null) {
                sink.occurrence(CangJieTopLevelFunctionFqnNameIndex.indexKey, fqName.asString())
                sink.occurrence(CangJieTopLevelFunctionByPackageIndex.indexKey, fqName.parent().asString())
                indexTopLevelExtension(stub, sink)
            }
        }

        indexInternals(stub, sink)
    }

    override fun indexClass(stub: CangJieClassStub, sink: IndexSink) {
        processNames(sink, stub.name, stub.getFqName()/*, stub.isTopLevel()*/)

//        if (stub.isInterface()) {
//            sink.occurrence<CjTypeStatement, String>(
//                CangJieClassShortNameIndex.indexKey,
//                JvmAbi.DEFAULT_IMPLS_CLASS_NAME
//            )
//        }

        indexSuperNames(stub, sink)

        indexPrime(stub, sink)
    }

    override fun indexInterface(stub: CangJieInterfaceStub, sink: IndexSink) {
        processNames(sink, stub.name, stub.getFqName()/*, stub.isTopLevel()*/)

//        if (stub.isInterface()) {
//            sink.occurrence<CjTypeStatement, String>(
//                CangJieClassShortNameIndex.indexKey,
//                JvmAbi.DEFAULT_IMPLS_CLASS_NAME
//            )
//        }
        sink.occurrence(indexKey, "DefaultImpls")

        indexSuperNames(stub, sink)

        indexPrime(stub, sink)
    }

    @Throws(IOException::class)
    override fun deserializeFileStub(dataStream: StubInputStream): CangJieFileStub {
        val packageFqNameAsString = dataStream.readNameString()
            ?: throw IllegalStateException("Can't read package fqname from stream")


        val facadeString = dataStream.readNameString()
        val partSimpleName = dataStream.readNameString()
        val numPartNames = dataStream.readInt()
        val facadePartNames: MutableList<String> = mutableListOf()
        for (i in 0 until numPartNames) {
            val partNameRef = dataStream.readNameString()
            if (partNameRef != null) {
                facadePartNames.add(partNameRef)
            }
        }
        return CangJieFileStubImpl(null, packageFqNameAsString, facadeString, partSimpleName, facadePartNames)
    }

    companion object {
        private fun getModifierListStub(stub: CangJieStubWithFqName<*>): CangJieModifierListStub? {
            return stub.findChildStubByType(CjStubElementTypes.MODIFIER_LIST)
        }


        private fun indexSuperNames(stub: CangJieTypeStatementStub<out CjTypeStatement>, sink: IndexSink) {
            for (superName in stub.getSuperNames()) {
                sink.occurrence(CangJieSuperClassIndex.indexKey, superName)
            }

            if (stub !is CangJieClassStub) {
                return
            }

//            val modifierListStub = getModifierListStub(stub)
//                ?: return

//                    if (modifierListStub.hasModifier(CjTokens.ENUM_KEYWORD)) {
//            sink.occurrence(CangJieSuperClassIndex.Helper.getIndexKey(), Enum.class.getSimpleName());
//        }
//        if (modifierListStub.hasModifier(CjTokens.ANNOTATION_KEYWORD)) {
//            sink.occurrence(CangJieSuperClassIndex.Helper.getIndexKey(), Annotation.class.getSimpleName());
//        }
        }

        private fun processNames(
            sink: IndexSink,
            shortName: String?,
            fqName: FqName?,
            level: Boolean = false
        ) {
            if (shortName != null) {
                sink.occurrence(indexKey, shortName)
            }

            if (fqName != null) {
                sink.occurrence(CangJieFullClassNameIndex.indexKey, fqName.asString())

                if (level) {
                    sink.occurrence(CangJieTopLevelClassByPackageIndex.indexKey, fqName.parent().asString())
                }
            }
        }

        /**
         * Indexes non-private top-level symbols or members of top-level objects and companion objects subject to this object serving as namespaces.
         */
        private fun indexPrime(stub: CangJieStubWithFqName<*>, sink: IndexSink) {
            val name = stub.name ?: return

            val modifierList = getModifierListStub(stub)
            if (modifierList != null && modifierList.hasModifier(CjTokens.PRIVATE_KEYWORD)) return
            if (modifierList != null && modifierList.hasModifier(CjTokens.OVERRIDE_KEYWORD)) return

            val parent = stub.parentStub
            var prime = false
            if (parent is CangJieFileStub) {
                prime = true
            }

            //        else if (parent instanceof CangJieStructStub) {
//            var grand = parent.getParentStub();
//            bool primeGrand = grand instanceof CangJieClassStub && ((CangJieClassStub) grand).isTopLevel();
//
//            prime = ((CangJieStructStub) parent).isTopLevel() ||
//                    primeGrand && ((CangJieStructStub) parent).isCompanion();
//        }
            if (prime) {
                sink.occurrence(CangJiePrimeSymbolNameIndex.indexKey, name)
            }
        }
    }
}
