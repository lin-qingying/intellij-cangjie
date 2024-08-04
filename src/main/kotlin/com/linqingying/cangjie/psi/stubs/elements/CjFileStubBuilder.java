package com.linqingying.cangjie.psi.stubs.elements;

import com.linqingying.cangjie.psi.CjFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.stubs.DefaultStubBuilder;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;


public class CjFileStubBuilder extends DefaultStubBuilder {
    @NotNull
    @Override
    protected StubElement createStubForFile(@NotNull PsiFile file) {
        if (!(file instanceof CjFile)) {
            return super.createStubForFile(file);
        }

        return StubIndexService.getInstance().createFileStub((CjFile) file);
    }
}
