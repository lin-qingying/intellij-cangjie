/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.cangjie1.psi.stubs.elements

import com.huawei.cangjie1.psi.CjFile
import com.huawei.cangjie1.psi.stubs.*
import com.huawei.cangjie1.psi.stubs.impl.CangJieFileStubImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

import java.io.IOException

open class StubIndexService protected constructor() {
    open fun indexFile(stub: CangJieFileStub, sink: IndexSink) {
    }

    open fun indexClass(stub: CangJieClassStub, sink: IndexSink) {
    }

    open fun indexFunction(stub: CangJieFunctionStub, sink: IndexSink) {
    }



    open fun indexProperty(stub: CangJiePropertyStub, sink: IndexSink) {
    }

    open fun indexParameter(stub: CangJieParameterStub, sink: IndexSink) {
    }



    open fun createFileStub(file: CjFile): CangJieFileStub {
        return CangJieFileStubImpl(file, file.packageFqNameByTree.asString())
    }

    @Throws(IOException::class)
    open fun serializeFileStub(stub: CangJieFileStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getPackageFqName().asString())

    }

    @Throws(IOException::class)
    open fun deserializeFileStub(dataStream: StubInputStream): CangJieFileStub {
        val packageFqNameAsString = dataStream.readName()

        return CangJieFileStubImpl(null, packageFqNameAsString!!.string)
    }

    companion object {
        @JvmStatic
        fun getInstance(): StubIndexService {
            return ApplicationManager.getApplication().getService(StubIndexService::class.java) ?: NO_INDEX
        }

        private val NO_INDEX = StubIndexService()
    }
}
