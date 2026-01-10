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
import org.cangnova.cangjie.psi.CjConstantPattern
import org.cangnova.cangjie.psi.stubs.CangJieConstantPatternStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieConstantPatternStubImpl
import org.jetbrains.annotations.NonNls
import java.io.IOException

/**
 * 常量模式 ElementType
 */
class CjConstantPatternElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieConstantPatternStub, CjConstantPattern>(
        debugName,
        CjConstantPattern::class.java,
        CangJieConstantPatternStub::class.java,
    ) {

    override fun createStub(psi: CjConstantPattern, parentStub: StubElement<*>?): CangJieConstantPatternStub {
        return CangJieConstantPatternStubImpl(parentStub)
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieConstantPatternStub, dataStream: StubOutputStream) {
        // 常量模式没有额外数据
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieConstantPatternStub {
        return CangJieConstantPatternStubImpl(parentStub)
    }

    override fun indexStub(stub: CangJieConstantPatternStub, sink: IndexSink) {
        // 常量模式不需要索引
    }
}
