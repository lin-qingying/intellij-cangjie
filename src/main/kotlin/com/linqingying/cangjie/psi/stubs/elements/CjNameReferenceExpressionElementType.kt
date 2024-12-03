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

package com.linqingying.cangjie.psi.stubs.elements;

import com.linqingying.cangjie.psi.CjNameReferenceExpression;
import com.linqingying.cangjie.psi.stubs.CangJieNameReferenceExpressionStub;
import com.linqingying.cangjie.psi.stubs.impl.CangJieNameReferenceExpressionStubImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;



public class CjNameReferenceExpressionElementType extends CjStubElementType<CangJieNameReferenceExpressionStub, CjNameReferenceExpression> {
    public CjNameReferenceExpressionElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjNameReferenceExpression.class, CangJieNameReferenceExpressionStub.class);
    }

    @Override
    public CangJieNameReferenceExpressionStub createStub(@NotNull CjNameReferenceExpression psi, StubElement parentStub) {
        return new CangJieNameReferenceExpressionStubImpl(parentStub, StringRef.fromString(psi.getReferencedName()));
    }

    @Override
    public void serialize(@NotNull CangJieNameReferenceExpressionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getReferencedName());
        dataStream.writeBoolean(
                stub instanceof CangJieNameReferenceExpressionStubImpl && ((CangJieNameReferenceExpressionStubImpl) stub).isClassRef());
    }

    @NotNull
    @Override
    public CangJieNameReferenceExpressionStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef referencedName = dataStream.readName();
        boolean isClassRef = dataStream.readBoolean();
        return new CangJieNameReferenceExpressionStubImpl(parentStub, referencedName, isClassRef);
    }
}
