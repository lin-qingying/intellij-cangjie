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

import com.linqingying.cangjie.name.ClassId;
import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.psi.CjClass;
import com.linqingying.cangjie.psi.CjInterface;
import com.linqingying.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.linqingying.cangjie.psi.psiUtil.StubUtils;
import com.linqingying.cangjie.psi.stubs.CangJieClassStub;
import com.linqingying.cangjie.psi.stubs.CangJieInterfaceStub;
import com.linqingying.cangjie.psi.stubs.impl.CangJieClassStubImpl;
import com.linqingying.cangjie.psi.stubs.impl.CangJieInterfaceStubImpl;
import com.linqingying.cangjie.psi.stubs.impl.Utils;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class CjInterfaceElementType extends CjStubElementType<CangJieInterfaceStub, CjInterface> {
    public CjInterfaceElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjInterface.class, CangJieInterfaceStub.class);
    }

    @Override
    public void indexStub(@NotNull CangJieInterfaceStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexInterface(stub, sink);
    }

    @NotNull
    @Override
    public CjInterface createPsi(@NotNull CangJieInterfaceStub stub) {
        return new CjInterface(stub);
    }

    public static CjInterfaceElementType getStubType() {
        return   CjStubElementTypes.INTERFACE;
    }
    @NotNull
    @Override
    public CjInterface createPsiFromAst(@NotNull ASTNode node) {
        return new CjInterface(node);
    }

    @Override
    public @NotNull CangJieInterfaceStub createStub(@NotNull CjInterface psi, StubElement<? extends PsiElement> parentStub) {
        FqName fqName = CjPsiUtilKt.safeFqNameForLazyResolve(psi);

        List<String> superNames = CjPsiUtilKt.getSuperNames(psi);
        ClassId classId = StubUtils.createNestedClassId(parentStub, psi);
        return new CangJieInterfaceStubImpl(
                CjStubElementTypes.INTERFACE, parentStub,
                StringRef.fromString(fqName != null ? fqName.asString() : null), classId,
                StringRef.fromString(psi.getName()),
                Utils.INSTANCE.wrapStrings(superNames),
                psi.isLocal()
        )  ;
    }

    @Override
    public void serialize(@NotNull CangJieInterfaceStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());

        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName == null ? null : fqName.asString());

        StubUtils.serializeClassId(dataStream, stub.getClassId());


        dataStream.writeBoolean(stub.isLocal());
//        dataStream.writeBoolean(stub.isTopLevel());

        List<String> superNames = stub.getSuperNames();
        dataStream.writeVarInt(superNames.size());
        for (String name : superNames) {
            dataStream.writeName(name);
        }
    }


    @Override
    public @NotNull CangJieInterfaceStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        StringRef qualifiedName = dataStream.readName();

        ClassId classId = StubUtils.deserializeClassId(dataStream);


        boolean isLocal = dataStream.readBoolean();
//        bool isTopLevel = dataStream.readBoolean();

        int superCount = dataStream.readVarInt();
        StringRef[] superNames = StringRef.createArray(superCount);
        for (int i = 0; i < superCount; i++) {
            superNames[i] = dataStream.readName();
        }

        return new CangJieInterfaceStubImpl(
                CjStubElementTypes.INTERFACE, (StubElement<?>) parentStub, qualifiedName, classId, name, superNames,
                isLocal
        );
    }
}
