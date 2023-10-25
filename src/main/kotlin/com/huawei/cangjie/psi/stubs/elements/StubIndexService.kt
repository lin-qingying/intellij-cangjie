

package com.huawei.cangjie.psi.stubs.elements

import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.stubs.*
import com.huawei.cangjie.psi.stubs.impl.CangJieFileStubImpl
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
    open fun indexInterface(stub: CangJieInterfaceStub, sink: IndexSink) {
    }
    open fun indexFunction(stub: CangJieFunctionStub, sink: IndexSink) {
    }



    open fun indexVariable(stub: CangJieVariableStub, sink: IndexSink) {
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

  open  fun indexStruct(stub: CangJieStructStub, sink: IndexSink) {

    }

    companion object {
        @JvmStatic
        fun getInstance(): StubIndexService {
            return ApplicationManager.getApplication().getService(StubIndexService::class.java) ?: NO_INDEX
        }

        private val NO_INDEX = StubIndexService()
    }
}
