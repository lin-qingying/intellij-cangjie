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

import com.linqingying.cangjie.descriptors.DescriptorVisibilities;
import com.linqingying.cangjie.descriptors.DescriptorVisibility;
import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.psi.CjImportDirective;
//import com.linqingying.cangjie.psi.CjImportDirectiveItem;
import com.linqingying.cangjie.psi.stubs.CangJieClassStub;
import com.linqingying.cangjie.psi.stubs.CangJieImportDirectiveStub;
import com.linqingying.cangjie.psi.stubs.impl.CangJieImportDirectiveStubImpl;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class CjImportDirectiveElementType extends CjStubElementType<CangJieImportDirectiveStub, CjImportDirective> {
    public CjImportDirectiveElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjImportDirective.class, CangJieImportDirectiveStub.class);
    }

    @NotNull
    @Override
    public CangJieImportDirectiveStub createStub(@NotNull CjImportDirective psi, StubElement parentStub) {
        FqName importedFqName = psi.getImportedFqName();
        StringRef fqName = StringRef.fromString(importedFqName == null ? null : importedFqName.asString());
        return new CangJieImportDirectiveStubImpl((StubElement<?>) parentStub, psi.isAllUnder(), fqName, psi.isValidImport(),psi.getModifierVisibility());
    }

    @Override
    public void serialize(@NotNull CangJieImportDirectiveStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeBoolean(stub.isAllUnder());
        FqName importedFqName = stub.getImportedFqName();
        dataStream.writeName(importedFqName != null ? importedFqName.asString() : null);
        dataStream.writeBoolean(stub.isValid());
        dataStream.writeName(stub.getModifierVisibility().getName());
    }

    @Override
    public void indexStub(@NotNull CangJieImportDirectiveStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexImports(stub, sink);
    }

    @NotNull
    @Override
    public CangJieImportDirectiveStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        boolean isAllUnder = dataStream.readBoolean();
        StringRef importedName = dataStream.readName();
        boolean isValid = dataStream.readBoolean();
        StringRef modifierVisibility = dataStream.readName();
        DescriptorVisibility visibility  = null;
        if (modifierVisibility != null) {
            visibility = DescriptorVisibilities.formName(modifierVisibility.getString());
        }else {
            visibility = DescriptorVisibilities.PRIVATE;
        }
        return new CangJieImportDirectiveStubImpl((StubElement<?>) parentStub, isAllUnder, importedName,isValid,visibility);
    }
}
