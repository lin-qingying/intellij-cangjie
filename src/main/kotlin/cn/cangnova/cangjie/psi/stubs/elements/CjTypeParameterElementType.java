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

package cn.cangnova.cangjie.psi.stubs.elements;

import cn.cangnova.cangjie.psi.CjTypeParameter;
import cn.cangnova.cangjie.psi.stubs.CangJieTypeParameterStub;
import cn.cangnova.cangjie.psi.stubs.impl.CangJieTypeParameterStubImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


public class CjTypeParameterElementType extends CjStubElementType<CangJieTypeParameterStub, CjTypeParameter> {
    public CjTypeParameterElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjTypeParameter.class, CangJieTypeParameterStub.class);
    }

    @NotNull
    @Override
    public CangJieTypeParameterStub createStub(@NotNull CjTypeParameter psi, StubElement parentStub) {
        return new CangJieTypeParameterStubImpl(
                (StubElement<?>) parentStub, StringRef.fromString(psi.getName())
//                psi.getVariance() == Variance.IN_VARIANCE
        );
    }

    @Override
    public void serialize(@NotNull CangJieTypeParameterStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
//        dataStream.writeBoolean(stub.isInVariance());

    }

    @NotNull
    @Override
    public CangJieTypeParameterStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
//        bool isInVariance = dataStream.readBoolean();


        return new CangJieTypeParameterStubImpl((StubElement<?>) parentStub, name);
    }
}
