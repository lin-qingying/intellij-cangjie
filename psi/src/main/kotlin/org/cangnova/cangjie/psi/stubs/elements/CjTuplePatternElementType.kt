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

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.cangnova.cangjie.psi.CjTuplePattern
import org.cangnova.cangjie.psi.stubs.CangJieTuplePatternStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieTuplePatternStubImpl
import org.jetbrains.annotations.NonNls
import java.io.IOException

/**
 * 元组模式 ElementType
 *
 * 用于创建、序列化和反序列化 CjTuplePattern 的 Stub
 * 子模式通过子 stub 表示
 */
class CjTuplePatternElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieTuplePatternStub, CjTuplePattern>(
        debugName,
        CjTuplePattern::class.java,
        CangJieTuplePatternStub::class.java,
    ) {

    override fun createStub(psi: CjTuplePattern, parentStub: StubElement<*>?): CangJieTuplePatternStub {
        return CangJieTuplePatternStubImpl(parentStub)
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieTuplePatternStub, dataStream: StubOutputStream) {
        // 元组模式本身没有额外数据，子模式由子 stub 处理
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieTuplePatternStub {
        return CangJieTuplePatternStubImpl(parentStub)
    }

    override fun indexStub(stub: CangJieTuplePatternStub, sink: IndexSink) {
        // 元组模式的索引由变量索引处理
    }
}
