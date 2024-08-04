//package com.linqingying.cangjie.psi;
//
//import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub;
//import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
//import com.intellij.lang.ASTNode;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//public class CjMacroAttrArgumentList extends CjElementImplStub<CangJiePlaceHolderStub<CjMacroAttrArgumentList>>{
//    public CjMacroAttrArgumentList(@NotNull ASTNode node) {
//        super(node);
//    }
//
//    public CjMacroAttrArgumentList(@NotNull CangJiePlaceHolderStub<CjMacroAttrArgumentList> stub) {
//        super(stub, CjStubElementTypes.MACRO_ARGUMENT_LIST);
//    }
//
//    @Override
//    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
//        return visitor.visitMacroArgumentList(this, data);
//    }
//
//}
