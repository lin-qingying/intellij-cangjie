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

import cn.cangnova.cangjie.psi.CjPackageDirective
import cn.cangnova.cangjie.psi.stubs.CangJiePackageDirectiveStub
import cn.cangnova.cangjie.psi.stubs.impl.CangJiePackageDirectiveStubImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CjPackageDirectiveElementType(debugName: @NonNls String) :
    CjStubElementType<CangJiePackageDirectiveStub , CjPackageDirective >(
        debugName,
        CjPackageDirective::class.java,
        CangJiePackageDirectiveStub::class.java
    ) {
    override fun createStub(
        psi: CjPackageDirective,
        parentStub: StubElement<out PsiElement?>
    ): CangJiePackageDirectiveStub {
        return CangJiePackageDirectiveStubImpl(parentStub)
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJiePackageDirectiveStub, dataStream: StubOutputStream) {
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJiePackageDirectiveStub {
        return CangJiePackageDirectiveStubImpl(parentStub)
    }
}
