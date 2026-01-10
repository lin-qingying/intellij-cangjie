/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi.stubs.elements

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.*
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubKindImpl
import java.io.IOException

open class StubIndexService protected constructor() {
    open fun indexFile(stub: CangJieFileStub, sink: IndexSink) {
    }

    open fun indexEnumConstructor(stub: CangJieEnumConstructorStub, sink: IndexSink) {
    }
    open fun indexScript(stub: CangJieScriptStub, sink: IndexSink) {
    }
    open fun indexEnum(stub: CangJieEnumStub, sink: IndexSink) {
    }

    open fun indexImports(stub: CangJieImportDirectiveStub, sink: IndexSink) {
    }

    open fun indexClass(stub: CangJieClassStub, sink: IndexSink) {
    }

    open fun indexExtend(stub: CangJieExtendStub, sink: IndexSink) {
    }

    open fun indexMacroFunction(stub: CangJieMacroStub, sink: IndexSink) {
    }

    open fun indexFunction(stub: CangJieNamedFunctionStub, sink: IndexSink) {
    }

    open fun indexMainFunction(stub: CangJieMainFunctionStub, sink: IndexSink) {
    }

    open fun indexTypeAlias(stub: CangJieTypeAliasStub, sink: IndexSink) {
    }

    open fun indexStruct(stub: CangJieStructStub, sink: IndexSink) {
    }

    open fun indexPatternVariable(stub: CangJieVariableStub, sink: IndexSink) {
    }

    open fun indexFieldVariable(stub: CangJieFieldStub, sink: IndexSink) {
    }

    open fun indexProperty(stub: CangJiePropertyStub, sink: IndexSink) {
    }

    open fun indexParameter(stub: CangJieParameterStubBase<*>, sink: IndexSink) {
    }

    open fun indexInterface(stub: CangJieInterfaceStub, sink: IndexSink) {
    }

    open fun indexAnnotation(stub: CangJieAnnotationStub, sink: IndexSink) {
    }

    open fun createFileStub(file: CjFile): CangJieFileStub {
        return CangJieFileStubImpl(file, file.packageFqNameByTree.asString())
    }

    @Throws(IOException::class)
    open fun serializeFileStub(stub: CangJieFileStub, dataStream: StubOutputStream) {
        CangJieFileStubKindImpl.serialize(stub.kind, dataStream)
    }

    @Throws(IOException::class)
    open fun deserializeFileStub(dataStream: StubInputStream): CangJieFileStub {
        val kind = CangJieFileStubKindImpl.deserialize(dataStream)
        return CangJieFileStubImpl(null, kind)
    }

    companion object {
        @JvmStatic
        fun getInstance(): StubIndexService {
            return ApplicationManager.getApplication().getService(StubIndexService::class.java) ?: NO_INDEX
        }

        private val NO_INDEX = StubIndexService()
    }
}
