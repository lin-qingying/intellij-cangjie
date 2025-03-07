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

import cn.cangnova.cangjie.name.FqName;
import cn.cangnova.cangjie.psi.CjParameter;
import cn.cangnova.cangjie.psi.stubs.CangJieParameterStub;
import cn.cangnova.cangjie.psi.stubs.impl.CangJieParameterStubImpl;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


public class CjParameterElementType extends CjStubElementType<CangJieParameterStub, CjParameter> {
    public CjParameterElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjParameter.class, CangJieParameterStub.class);
    }

    @NotNull
    @Override
    public CangJieParameterStub createStub(@NotNull CjParameter psi, StubElement parentStub) {
        FqName fqName = psi.getFqName();
        StringRef fqNameRef = StringRef.fromString(fqName != null ? fqName.asString() : null);
        return new CangJieParameterStubImpl(
                (StubElement<?>) parentStub, fqNameRef, StringRef.fromString(psi.getName()),
                psi.isMutable(), psi.hasLetOrVar(), psi.hasDefaultValue(), null
        );
    }

    @Override
    public void serialize(@NotNull CangJieParameterStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isMutable());
        dataStream.writeBoolean(stub.hasValOrVar());
        dataStream.writeBoolean(stub.hasDefaultValue());
        FqName name = stub.getFqName();
        dataStream.writeName(name != null ? name.asString() : null);
        dataStream.writeName(stub instanceof CangJieParameterStubImpl ? ((CangJieParameterStubImpl) stub).getFunctionTypeParameterName() : null);
    }

    @NotNull
    @Override
    public CangJieParameterStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        boolean isMutable = dataStream.readBoolean();
        boolean hasValOrValNode = dataStream.readBoolean();
        boolean hasDefaultValue = dataStream.readBoolean();
        StringRef fqName = dataStream.readName();

        return new CangJieParameterStubImpl((StubElement<?>) parentStub, fqName, name, isMutable, hasValOrValNode, hasDefaultValue,
                dataStream.readNameString());
    }

    @Override
    public void indexStub(@NotNull CangJieParameterStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexParameter(stub, sink);
    }
}
