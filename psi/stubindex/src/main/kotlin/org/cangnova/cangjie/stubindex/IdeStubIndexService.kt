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
import org.cangnova.cangjie.psi.CjCasePatternElement
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjTuplePattern
import org.cangnova.cangjie.psi.CjEnumPattern
import org.cangnova.cangjie.psi.stubs.*
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubKindImpl
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import java.io.IOException

internal class IdeStubIndexService : StubIndexService() {
    override fun indexVariable(stub: CangJieVariableStub, sink: IndexSink) {
        // 变量声明的名称和 fqName 来自模式匹配中的绑定模式
        // 遍历子 stub 找到所有绑定模式
        val bindingPatternStubs = stub.childrenStubs
            .filterIsInstance<CangJieBindingPatternStub>()

        for (bindingStub in bindingPatternStubs) {
            val name = bindingStub.getName() ?: continue
            sink.occurrence(CangJieVariableShortNameIndex.indexKey, name)

            // fqName 现在存储在绑定模式中
            val fqName = bindingStub.fqName
            if (fqName != null && stub.isTopLevel()) {
                sink.occurrence(CangJieTopLevelVariableFqNameIndex.indexKey, fqName.asString())
                sink.occurrence(
                    CangJieTopLevelVariableByPackageIndex.indexKey,
                    fqName.parent().asString()
                )
            }
        }

        // 如果没有绑定模式 stub（可能是从 PSI 创建的），则从 PSI 获取
        if (bindingPatternStubs.isEmpty()) {
            val psi = stub.psi
            val allBindings = psi.pattern?.let { getAllBindingsFromPattern(it) } ?: emptyList()

            for (binding in allBindings) {
                val name = binding.name ?: continue
                sink.occurrence(CangJieVariableShortNameIndex.indexKey, name)

                val typeReference: CjTypeReference? = psi.typeReference
                if (typeReference != null && CangJiePsiHeuristics.isProbablyNothing(typeReference)) {
                    sink.occurrence(CangJieVariableNothingVariableShortNameIndex.indexKey, name)
                }
            }
        }
        // Variables don't have internal declarations, so no indexInternals call
    }

    /**
     * 从模式中获取所有绑定模式
     */
    private fun getAllBindingsFromPattern(pattern: CjCasePatternElement): List<CjBindingPattern> {
        return when (pattern) {
            is CjBindingPattern -> listOf(pattern)
            is CjTuplePattern -> pattern.patterns.flatMap { getAllBindingsFromPattern(it) }
            is CjEnumPattern -> pattern.patterns.flatMap { getAllBindingsFromPattern(it) }
            else -> emptyList()
        }
    }

    override fun indexProperty(stub: CangJiePropertyStub, sink: IndexSink) {
        val name: String? = stub.name
        if (name != null) {
            sink.occurrence(CangJiePropertyShortNameIndex.indexKey, name)

            val typeReference: CjTypeReference? = stub.psi.typeReference
            if (typeReference != null && CangJiePsiHeuristics.isProbablyNothing(typeReference)) {
                sink.occurrence(CangJieProbablyNothingPropertyShortNameIndex.indexKey, name)
            }
            indexPrime(stub, sink)
        }


        indexInternals(stub, sink)
    }

    override fun indexField(stub: CangJieFieldStub, sink: IndexSink) {
        val name: String? = stub.name
        if (name != null) {
            // 按字段名称索引
            sink.occurrence(CangJieFieldShortNameIndex.indexKey, name)
            indexPrime(stub, sink)
        }

        // 按所属类的 FqName 索引
        val fqName: FqName? = stub.getFqName()
        if (fqName != null) {
            // fqName 格式如 "pkg.ClassName.fieldName"，取其父级即为类的 FqName
            val parentFqName = fqName.parent()
            if (!parentFqName.isRoot) {
                sink.occurrence(CangJieFieldByClassIndex.indexKey, parentFqName.asString())
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
            sink.occurrence(CangJieTypeAliasShortNameIndex.indexKey, name)
            indexPrime(stub, sink)
        }

        indexTypeAliasExpansion(stub, sink)

        val fqName: FqName? = stub.getFqName()
        if (fqName != null) {
            sink.occurrence(CangJieTopLevelTypeAliasFqNameIndex.indexKey, fqName.asString())
            sink.occurrence(
                CangJieTopLevelTypeAliasByPackageIndex.indexKey,
                fqName.parent().asString()
            )
        }

    }

    override fun createFileStub(file: CjFile): CangJieFileStub {
        val packageFqName = file.packageFqNameByTree
        return CangJieFileStubImpl(file, CangJieFileStubKindImpl.File(packageFqName))
    }

    @Throws(IOException::class)
    override fun serializeFileStub(
        stub: CangJieFileStub, dataStream: StubOutputStream
    ) {
        CangJieFileStubKindImpl.serialize(stub.kind, dataStream)
    }

    override fun indexMainFunction(stub: CangJieMainFunctionStub, sink: IndexSink) {
        if (stub.isTopLevel()) {
            // can have special fq name in case of syntactically incorrect function with no name
            val fqName: FqName? = stub.getFqName()
            if (fqName != null) {
                sink.occurrence(CangJieMainFunctionFqNameIndex.indexKey, fqName.asString())
            }
        }

        indexInternals(stub, sink)
    }

    override fun indexMacroFunction(stub: CangJieMacroStub, sink: IndexSink) {
        val name: String? = stub.name
        if (name != null) {
            sink.occurrence(CangJieMacroDeclarationShortNameIndex.indexKey, name)

            indexPrime(stub, sink)
        }
        val fqName: FqName? = stub.getFqName()
        if (fqName != null) {
            sink.occurrence(CangJieMacroDeclarationFqNameIndex.indexKey, fqName.asString())
            sink.occurrence(
                CangJieMacroDeclarationByPackageIndex.indexKey,
                fqName.parent().asString()
            )
        }
    }

    override fun indexFunction(stub: CangJieNamedFunctionStub, sink: IndexSink) {
        val name: String? = stub.name
        if (name != null) {
            sink.occurrence(CangJieFunctionShortNameIndex.indexKey, name)

            val typeReference: CjTypeReference? = stub.psi.typeReference
            if (typeReference != null && CangJiePsiHeuristics.isProbablyNothing(typeReference)) {
                sink.occurrence(CangJieProbablyNothingFunctionShortNameIndex.indexKey, name)
            }

            indexPrime(stub, sink)
        }
        //如果该方法是顶层方法，则将其索引到顶层
        if (stub.isTopLevel()) {
            val fqName: FqName? = stub.getFqName()
            if (fqName != null) {
                sink.occurrence(CangJieTopLevelFunctionFqNameIndex.indexKey, fqName.asString())
                sink.occurrence(
                    CangJieTopLevelFunctionByPackageIndex.indexKey,
                    fqName.parent().asString()
                )
            }
        }


        indexInternals(stub, sink)
    }

    override fun indexExtend(stub: CangJieExtendStub, sink: IndexSink) {
        val fqName: FqName? = stub.getFqName()


        if (fqName != null) {
            sink.occurrence(CangJieExtendNameIndex.indexKey, fqName.asString())
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
            sink.occurrence(CangJieImportFqNameForPackageNameIndex.indexKey, fqName.asString())

        }
    }

    override fun indexEnumConstructor(stub: CangJieEnumConstructorStub, sink: IndexSink) {

        //  索引到枚举构造器专用索引
        val name = stub.name
        if (name != null) {
            sink.occurrence(CangJieEnumConstructorShortNameIndex.indexKey, name)
        }

        //  按枚举类型索引
        val parentEnumFqName = stub.getParentEnumFqName()
        if (parentEnumFqName != null) {
            sink.occurrence(CangJieEnumConstructorByEnumTypeIndex.indexKey, parentEnumFqName.asString())
        }

        //  prime 索引
        indexPrime(stub, sink)
    }

    override fun indexEnum(stub: CangJieEnumStub, sink: IndexSink) {
        indexTypeStatementStub(stub, sink)
    }

    private fun indexTypeStatementStub(stub: CangJieTypeStatementStub<out CjTypeStatement>, sink: IndexSink) {
        processNames(sink, stub.name, stub.getFqName() /*, stub.isTopLevel()*/)

        if (stub is CangJieInterfaceStub) {
            sink.occurrence(CangJieClassShortNameIndex.indexKey, "DefaultImpls")
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
        val kind = CangJieFileStubKindImpl.deserialize(dataStream)
        return CangJieFileStubImpl(null, kind)
    }

    companion object {
        private fun getModifierListStub(stub: CangJieStubWithFqName<*>): CangJieModifierListStub? {
            return stub.findChildStubByType(CjStubElementTypes.MODIFIER_LIST)
        }

        private fun indexSuperNames(stub: CangJieTypeStatementStub<out CjTypeStatement>, sink: IndexSink) {
            for (superName in stub.getSuperNames()) {
                sink.occurrence(CangJieSuperClassIndex.indexKey, superName)
            }

        }

        private fun processNames(
            sink: IndexSink,
            shortName: String?,
            fqName: FqName?,
            level: Boolean
        ) {
            if (shortName != null) {
                sink.occurrence(CangJieClassShortNameIndex.indexKey, shortName)
            }

            if (fqName != null) {
                sink.occurrence(CangJieFullClassNameIndex.indexKey, fqName.asString())

                if (level) {
                    sink.occurrence(
                        CangJieTopLevelClassByPackageIndex.indexKey,
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
                sink.occurrence(CangJiePrimeSymbolNameIndex.indexKey, name)
            }
        }
    }
}
