package com.huawei.cangjie1.psi.stubs.elements;

import com.huawei.cangjie1.lang.CangJieLanguage;
import com.huawei.cangjie1.parsing.CangJieParser;
import com.huawei.cangjie1.psi.stubs.CangJieFileStub;
import com.huawei.cangjie1.psi.stubs.CangJieStubVersions;
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
