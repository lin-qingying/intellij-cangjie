package com.huawei.cangjie1.psi;

import com.huawei.cangjie1.name.ClassId;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;


//public class CjEnumEntry extends CjClass {
//    public CjEnumEntry(@NotNull ASTNode node) {
//        super(node);
//    }
//
//    public CjEnumEntry(@NotNull CangJieClassStub stub) {
//        super(stub);
//    }
//
//    @NotNull
//    @Override
//    public List<CjSuperTypeListEntry> getSuperTypeListEntries() {
//        CjInitializerList initializerList = getInitializerList();
//        if (initializerList == null) {
//            return Collections.emptyList();
//        }
//        return initializerList.getInitializers();
//    }
//
//    public boolean hasInitializer() {
//        return !getSuperTypeListEntries().isEmpty();
//    }
//
//    @Nullable
//    @Override
//    public ClassId getClassId() {
//        return null;
//    }
//
//    @Nullable
//    public CjInitializerList getInitializerList() {
//        return getStubOrPsiChild(CjStubElementTypes.INITIALIZER_LIST);
//    }
//
//    @Override
//    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
//        return visitor.visitEnumEntry(this, data);
//    }
//
//    @Override
//    public boolean isEquivalentTo(@Nullable PsiElement another) {
//        if (another instanceof PsiEnumConstant) {
//            PsiEnumConstant enumConstant = (PsiEnumConstant) another;
//            PsiClass containingClass = enumConstant.getContainingClass();
//            if (containingClass != null) {
//                String containingClassQName = containingClass.getQualifiedName();
//                if (containingClassQName != null && enumConstant.getName() != null) {
//                    String theirFQName = containingClassQName + "." + enumConstant.getName();
//                    if (theirFQName.equals(getQualifiedName())) {
//                        return true;
//                    }
//                }
//            }
//        }
//        return super.isEquivalentTo(another);
//    }
//}
