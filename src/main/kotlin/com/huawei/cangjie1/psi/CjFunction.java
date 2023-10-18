package com.huawei.cangjie1.psi;


import com.huawei.cangjie1.psi.stubs.CangJieFunctionStub;
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifiableCodeBlock;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CjFunction extends CjDeclarationWithBody, CjCallableDeclaration {
    boolean isLocal();
}

