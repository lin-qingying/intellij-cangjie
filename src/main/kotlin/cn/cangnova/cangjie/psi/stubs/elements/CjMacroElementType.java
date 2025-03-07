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
import cn.cangnova.cangjie.psi.CjFile;
import cn.cangnova.cangjie.psi.CjMacroDeclaration;
import cn.cangnova.cangjie.psi.psiUtil.CjPsiUtilKt;
import cn.cangnova.cangjie.psi.stubs.CangJieFunctionStub;
import cn.cangnova.cangjie.psi.stubs.impl.CangJieFunctionStubImpl;
import cn.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CjMacroElementType extends CjStubElementType<CangJieFunctionStub, CjMacroDeclaration>{

    public CjMacroElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjMacroDeclaration.class, CangJieFunctionStub.class);
    }
    @Override
    public @NotNull CangJieFunctionStub createStub(@NotNull CjMacroDeclaration psi, StubElement<? extends PsiElement> parentStub) {
        boolean isTopLevel = psi.getParent() instanceof CjFile;
        boolean isExtension = psi.getReceiverTypeReference() != null;
        FqName fqName = CjPsiUtilKt.safeFqNameForLazyResolve(psi);
        boolean hasBlockBody = psi.hasBlockBody();
        boolean hasBody = psi.hasBody();
        return new CangJieFunctionStubImpl(
                parentStub,CjStubElementTypes.MACRO, StringRef.fromString(psi.getName()), isTopLevel, fqName,
                isExtension, hasBlockBody, hasBody, psi.hasTypeParameterListBeforeFunctionName(),

null,
                null
        );
    }

    @Override
    public void serialize(@NotNull CangJieFunctionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isTopLevel());

        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName != null ? fqName.asString() : null);

        dataStream.writeBoolean(stub.isExtension());
        dataStream.writeBoolean(stub.hasBlockBody());
        dataStream.writeBoolean(stub.hasBody());
        dataStream.writeBoolean(stub.hasTypeParameterListBeforeFunctionName());
//        bool haveContract = stub.mayHaveContract();
//        dataStream.writeBoolean(haveContract);
        if (stub instanceof CangJieFunctionStubImpl stubImpl) {


            CangJieStubOrigin.serialize(stubImpl.getOrigin(), dataStream);
        }
    }

    @Override
    public void indexStub(@NotNull CangJieFunctionStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexMacroFunction(stub, sink);

    }

    @Override
    public @NotNull CangJieFunctionStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        boolean isTopLevel = dataStream.readBoolean();

        StringRef fqNameAsString = dataStream.readName();
        FqName fqName = fqNameAsString != null ? new FqName(fqNameAsString.toString()) : null;

        boolean isExtension = dataStream.readBoolean();
        boolean hasBlockBody = dataStream.readBoolean();
        boolean hasBody = dataStream.readBoolean();
        boolean hasTypeParameterListBeforeFunctionName = dataStream.readBoolean();
//        bool mayHaveContract = dataStream.readBoolean();
        return new CangJieFunctionStubImpl(
                (StubElement<?>) parentStub,CjStubElementTypes.MACRO, name, isTopLevel, fqName, isExtension, hasBlockBody, hasBody,
                hasTypeParameterListBeforeFunctionName,
null,
                CangJieStubOrigin.deserialize(dataStream)
        );
    }
}
