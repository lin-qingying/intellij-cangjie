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

import com.linqingying.cangjie.lang.CangJieLanguage;
import com.linqingying.cangjie.parsing.CangJieParser;
import com.linqingying.cangjie.psi.stubs.CangJieFileStub;
import com.linqingying.cangjie.psi.stubs.CangJieStubVersions;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBuilder;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.tree.IStubFileElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;


public class CjFileElementType extends IStubFileElementType<CangJieFileStub> {
    private static final String NAME = "cangjie.FILE";

    public static final CjFileElementType INSTANCE = new CjFileElementType();

    private CjFileElementType() {
        super(NAME, CangJieLanguage.INSTANCE);
    }

    protected CjFileElementType(@NonNls String debugName) {
        super(debugName, CangJieLanguage.INSTANCE);
    }

    @Override
    public StubBuilder getBuilder() {
        return new CjFileStubBuilder();
    }

    @Override
    public int getStubVersion() {
        return CangJieStubVersions.SOURCE_STUB_VERSION;
    }

    @NotNull
    @Override
    public String getExternalId() {
        return NAME;
    }

    @Override
    public void serialize(@NotNull CangJieFileStub stub, @NotNull StubOutputStream dataStream)
            throws IOException {
        StubIndexService.getInstance().serializeFileStub(stub, dataStream);
    }

    @NotNull
    @Override
    public CangJieFileStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return StubIndexService.getInstance().deserializeFileStub(dataStream);
    }

    @Override
    protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement psi) {
        Project project = psi.getProject();
        Language languageForParser = getLanguageForParser(psi);
        PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, languageForParser, chameleon.getChars());
        return CangJieParser.parse(builder, psi.getContainingFile()).getFirstChildNode();
    }

    @Override
    public void indexStub(@NotNull CangJieFileStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexFile(stub, sink);
    }
}
