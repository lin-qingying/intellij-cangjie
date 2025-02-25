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
package cn.cangnova.cangjie.psi.stubs.elements

import cn.cangnova.cangjie.psi.CjForeignDirective
import cn.cangnova.cangjie.psi.stubs.CangJieForeignDirectiveStub
import cn.cangnova.cangjie.psi.stubs.impl.CangJieForeignDirectiveStubImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CjForeignDirectiveElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieForeignDirectiveStub , CjForeignDirective >(
        debugName,
        CjForeignDirective::class.java,
        CangJieForeignDirectiveStub::class.java
    ) {
    override fun createStub(
        cjForeign: CjForeignDirective,
        parentStub: StubElement<out PsiElement?>
    ): CangJieForeignDirectiveStub {
        return CangJieForeignDirectiveStubImpl(
            parentStub
        )
    }

    @Throws(IOException::class)
    override fun serialize(cangJieForeignStub: CangJieForeignDirectiveStub, stubOutputStream: StubOutputStream) {
    }

    @Throws(IOException::class)
    override fun deserialize(
        stubInputStream: StubInputStream,
        parentStub: StubElement<*>
    ): CangJieForeignDirectiveStub {
        return CangJieForeignDirectiveStubImpl(
            parentStub
        )
    }
}
