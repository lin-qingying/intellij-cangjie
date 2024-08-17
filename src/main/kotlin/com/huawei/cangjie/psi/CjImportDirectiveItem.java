package com.huawei.cangjie.psi;

import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.stubs.CangJieImportDirectiveStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.huawei.cangjie.psi.stubs.elements.CjTokenSets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes.DOT_QUALIFIED_EXPRESSION;
import static com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes.REFERENCE_EXPRESSION;
//
//public class CjImportDirectiveItem extends CjElementImplStub<CangJieImportDirectiveStub> {
//    //public class CjImportDirectiveItem extends CjExpressionImplStub<CangJiePlaceHolderStub<CjImportDirectiveItem>> {
//    //public class CjImportDirectiveItem extends CjElementImplStub<CangJieImportDirectiveItemStub>   {
//    public CjImportDirectiveItem(@NotNull ASTNode node) {
//        super(node);
//    }
//
//    public CjImportDirectiveItem(@NotNull CangJieImportDirectiveStub stub) {
//        super(stub, CjStubElementTypes.IMPORT_DIRECTIVE_ITEM);
//    }
////    public CjImportDirectiveItem(@NotNull CangJiePlaceHolderStub<CjImportDirectiveItem> stub) {
////        super(stub, CjStubElementTypes.IMPORT_DIRECTIVE_ITEM);
////    }
//
//    @Nullable
//    public CjImportAlias getAlias() {
//        return getStubOrPsiChild(CjStubElementTypes.IMPORT_ALIAS);
//    }
//
//    public boolean isValidImport() {
//        CangJieImportDirectiveStub stub = getStub();
//        if (stub != null) {
//            return stub.isValid();
//        }
//        return !PsiTreeUtil.hasErrorElements(this);
//    }
//
//    public boolean isAllUnder() {
//        CangJieImportDirectiveStub stub = getStub();
//        if (stub != null) {
//            return stub.isAllUnder();
//        }
//        return getNode().findChildByType(CjTokens.MUL) != null;
//    }
//
//    @Nullable
////    @Override
//    public CjImportInfo.ImportContent getImportContent() {
//        CjExpression[] reference = (CjExpression[]) getStubOrPsiChildren(TokenSet.create(REFERENCE_EXPRESSION, DOT_QUALIFIED_EXPRESSION
//        ), CjExpression.ARRAY_FACTORY);
//        if (reference.length < 1) return null;
//        return new CjImportInfo.ImportContent.ExpressionBased(reference[0]);
//
//    }
//
//    @Nullable
//    public String getAliasName() {
//        CjImportAlias alias = getAlias();
//        return alias != null ? alias.getName() : null;
//    }
//
//    /**
//     * 一条导入语句可能导入了多个包或者类
//     * 需要拆分
//     *
//     * @return
//     */
//    @Nullable
//    @IfNotParsed
//    public CjExpression[] getImportedReferences() {
//
//        return getStubOrPsiChildren(CjTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS, CjExpression.Companion.getARRAY_FACTORY());
//
//    }
//}
