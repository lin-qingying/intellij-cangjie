package com.linqingying.cangjie.psi.stubs.elements;

import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.psi.CjFile;
import com.linqingying.cangjie.psi.CjForeignDirective;
import com.linqingying.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.linqingying.cangjie.psi.stubs.CangJieForeignDirectiveStub;
import com.linqingying.cangjie.psi.stubs.impl.CangJieForeignDirectiveStubImpl;
import com.linqingying.cangjie.psi.stubs.impl.CangJieFunctionStubImpl;
import com.linqingying.cangjie.psi.stubs.impl.CangJieStubOrigin;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CjForeignDirectiveElementType extends CjStubElementType<CangJieForeignDirectiveStub, CjForeignDirective> {


    public CjForeignDirectiveElementType(@NotNull @NonNls String debugName) {
        super(debugName, CjForeignDirective.class, CangJieForeignDirectiveStub.class);
    }

    @Override
    public @NotNull CangJieForeignDirectiveStub createStub(@NotNull CjForeignDirective cjForeign, StubElement<? extends PsiElement> parentStub) {

        return new CangJieForeignDirectiveStubImpl(
                (StubElement<?>) parentStub
        );
    }

    @Override
    public void serialize(@NotNull CangJieForeignDirectiveStub cangJieForeignStub, @NotNull StubOutputStream stubOutputStream) throws IOException {

    }

    @Override
    public @NotNull CangJieForeignDirectiveStub deserialize(@NotNull StubInputStream stubInputStream, StubElement parentStub) throws IOException {
        return new CangJieForeignDirectiveStubImpl(
                (StubElement<?>) parentStub
        );
    }
}
