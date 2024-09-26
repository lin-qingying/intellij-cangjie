package com.huawei.cangjie.psi;

import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class CjVisitor<R, D> extends PsiElementVisitor {

    public R visitCjElement(@NotNull CjElement element, D data) {
        visitElement(element);
        return null;
    }

    public R visitSafeQualifiedExpression(@NotNull CjSafeQualifiedExpression expression, D data) {
        return visitQualifiedExpression(expression, data);
    }

    public R visitQuoteExpression(@NotNull CjQuoteExpression element, D data) {
        return visitExpression(element, data);


    }

    public R visitOptionType(@NotNull CjOptionType optionType, D data) {
        return visitTypeElement(optionType, data);
    }

    public R visitThisType(@NotNull CjThisType optionType, D data) {
        return visitTypeElement(optionType, data);
    }

    public R visitProperty(@NotNull CjProperty property, D data) {
        return visitNamedDeclaration(property, data);
    }

    public R visitVariable(@NotNull CjVariable variable, D data) {
        return visitNamedDeclaration(variable, data);
    }

    public R visitCallableReferenceExpression(@NotNull CjCallableReferenceExpression expression, D data) {
        return visitDoubleColonExpression(expression, data);
    }

    public R visitDoubleColonExpression(@NotNull CjDoubleColonExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitRangeExpression(@NotNull CjRangeExpression expression, D data) {
        return visitExpression(expression, data);

    }
    public R visitSpawnExpression(@NotNull CjSpawnExpression expression, D data) {
        return visitCallExpression(expression, data);
    }
    public R visitLambdaExpression(@NotNull CjLambdaExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitUnsafeExpression(@NotNull CjUnsafeExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitTupleExpression(@NotNull CjTupleExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitTryExpression(@NotNull CjTryExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitCatchSection(CjCatchClause cjCatchClause, D data) {

        return visitCjElement(cjCatchClause, data);
    }

    public R visitFinallySection(@NotNull CjFinallySection finallySection, D data) {
        return visitCjElement(finallySection, data);
    }

    public R visitMatchExpression(@NotNull CjMatchExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitWhileExpression(@NotNull CjWhileExpression expression, D data) {
        return visitLoopExpression(expression, data);
    }

    public R visitDoWhileExpression(@NotNull CjDoWhileExpression expression, D data) {
        return visitLoopExpression(expression, data);
    }

    public R visitLoopExpression(@NotNull CjLoopExpression loopExpression, D data) {
        return visitExpression(loopExpression, data);
    }

    public R visitForExpression(@NotNull CjForExpression expression, D data) {
        return visitLoopExpression(expression, data);
    }

    public R visitIfExpression(@NotNull CjIfExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitContinueExpression(@NotNull CjContinueExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitBreakExpression(@NotNull CjBreakExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitCollectionLiteralExpression(@NotNull CjCollectionLiteralExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitThrowExpression(@NotNull CjThrowExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitReturnExpression(@NotNull CjReturnExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitParenthesizedExpression(@NotNull CjParenthesizedExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitStringTemplateExpression(@NotNull CjStringTemplateExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitSuperExpression(@NotNull CjSuperExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitThisExpression(@NotNull CjThisExpression expression, D data) {
        return visitExpressionWithLabel(expression, data);
    }

    public R visitExpressionWithLabel(@NotNull CjExpressionWithLabel expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitLiteralStringTemplateEntry(@NotNull CjLiteralStringTemplateEntry entry, D data) {
        return visitStringTemplateEntry(entry, data);
    }

    public R visitEscapeStringTemplateEntry(@NotNull CjEscapeStringTemplateEntry entry, D data) {
        return visitStringTemplateEntry(entry, data);
    }

    public R visitStringTemplateEntry(@NotNull CjStringTemplateEntry entry, D data) {
        return visitCjElement(entry, data);
    }

    public R visitStringTemplateEntryWithExpression(@NotNull CjStringTemplateEntryWithExpression entry, D data) {
        return visitStringTemplateEntry(entry, data);
    }

    public R visitBlockStringTemplateEntry(@NotNull CjBlockStringTemplateEntry entry, D data) {
        return visitStringTemplateEntryWithExpression(entry, data);
    }

    public R visitPostfixExpression(@NotNull CjPostfixExpression expression, D data) {
        return visitUnaryExpression(expression, data);
    }

    public R visitConstantExpression(@NotNull CjConstantExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitValueArgumentList(@NotNull CjValueArgumentList list, D data) {
        return visitCjElement(list, data);
    }

    public R visitArrayAccessExpression(@NotNull CjArrayAccessExpression expression, D data) {
        return visitReferenceExpression(expression, data);
    }

    public R visitSuperTypeEntry(@NotNull CjSuperTypeEntry specifier, D data) {
        return visitSuperTypeListEntry(specifier, data);
    }

    public R visitPrefixExpression(@NotNull CjPrefixExpression expression, D data) {
        return visitUnaryExpression(expression, data);
    }

    public R visitUnaryExpression(@NotNull CjUnaryExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitIsExpression(@NotNull CjIsExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitBinaryExpression(@NotNull CjBinaryExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitSuperTypeCallEntry(@NotNull CjSuperTypeCallEntry call, D data) {
        return visitSuperTypeListEntry(call, data);
    }

    public R visitBinaryWithTypeRHSExpression(@NotNull CjBinaryExpressionWithTypeRHS expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitClassInitializer(@NotNull CjClassInitializer initializer, D data) {
        return visitAnonymousInitializer(initializer, data);
    }

    public R visitAnonymousInitializer(@NotNull CjAnonymousInitializer initializer, D data) {
        return visitDeclaration(initializer, data);
    }

    public R visitSuperTypeListEntry(@NotNull CjSuperTypeListEntry specifier, D data) {
        return visitCjElement(specifier, data);
    }

    public R visitTypeProjection(@NotNull CjTypeProjection typeProjection, D data) {
        return visitCjElement(typeProjection, data);
    }

    public R visitTypeParameterList(@NotNull CjTypeParameterList list, D data) {
        return visitCjElement(list, data);
    }

    public R visitTypeParameter(@NotNull CjTypeParameter parameter, D data) {
        return visitNamedDeclaration(parameter, data);
    }

    public R visitCjFile(@NotNull CjFile file, D data) {
        visitFile(file);
        return null;
    }

    public R visitBlockExpression(@NotNull CjBlockExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitExpression(@NotNull CjExpression expression, D data) {
        return visitCjElement(expression, data);
    }

    public R visitClassBody(@NotNull CjClassBody classBody, D data) {
        return visitCjElement(classBody, data);
    }

    public R visitEnumBody(@NotNull CjEnumBody enumBody, D data) {
        return visitCjElement(enumBody, data);
    }

    public R visitModifierList(@NotNull CjModifierList list, D data) {
        return visitCjElement(list, data);
    }


    public R visitContextReceiverList(@NotNull CjContextReceiverList contextReceiverList, D data) {
        return visitCjElement(contextReceiverList, data);
    }

    public R visitTypeReference(@NotNull CjTypeReference cjTypeReference, D data) {
        return visitCjElement(cjTypeReference, data);
    }


    public R visitSimpleNameExpression(@NotNull CjSimpleNameExpression expression, D data) {
        return visitReferenceExpression(expression, data);
    }

    public R visitReferenceExpression(@NotNull CjReferenceExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitTypeConstraintList(@NotNull CjTypeConstraintList cjTypeConstraintList, D data) {
        return visitCjElement(cjTypeConstraintList, data);
    }

    public R visitTypeConstraint(@NotNull CjTypeConstraint cjTypeConstraint, D data) {
        return visitCjElement(cjTypeConstraint, data);
    }

    public R visitParameterList(@NotNull CjParameterList cjParameterList, D data) {
        return visitCjElement(cjParameterList, data);
    }

    public R visitParameter(CjParameter cjParameter, D data) {
        return visitCjElement(cjParameter, data);
    }

    public R visitDestructuringDeclaration(CjDestructuringDeclaration cjDestructuringDeclaration, D data) {

        return visitCjElement(cjDestructuringDeclaration, data);
    }

    public R visitDestructuringDeclarationEntry(CjDestructuringDeclarationEntry cjDestructuringDeclarationEntry, D data) {
        return visitCjElement(cjDestructuringDeclarationEntry, data);
    }


    public R visitDeclaration(@NotNull CjDeclaration dcl, D data) {
        return visitExpression(dcl, data);
    }

    public R visitTypeAlias(@NotNull CjTypeAlias typeAlias, D data) {
        return visitNamedDeclaration(typeAlias, data);
    }

    public R visitNamedDeclaration(@NotNull CjNamedDeclaration declaration, D data) {
        return visitDeclaration(declaration, data);
    }

    public R visitPrimaryConstructor(@NotNull CjPrimaryConstructor constructor, D data) {
        return visitNamedDeclaration(constructor, data);
    }

    public R visitNamedFunction(CjNamedFunction cjNamedFunction, D data) {
        return visitNamedDeclaration(cjNamedFunction, data);
    }

    public R visitCallExpression(CjCallExpression cjCallExpression, D data) {
        return visitReferenceExpression(cjCallExpression, data);
    }

    public R visitSecondaryConstructor(@NotNull CjSecondaryConstructor constructor, D data) {
        return visitNamedDeclaration(constructor, data);
    }

    public R visitConstructorDelegationCall(CjConstructorDelegationCall cjConstructorDelegationCall, D data) {
        return visitCjElement(cjConstructorDelegationCall, data);
    }

    public R visitPackageDirective(CjPackageDirective cjPackageDirective, D data) {
        return visitCjElement(cjPackageDirective, data);
    }

    public R visitQualifiedExpression(@NotNull CjQualifiedExpression expression, D data) {
        return visitExpression(expression, data);
    }

    public R visitDotQualifiedExpression(@NotNull CjDotQualifiedExpression expression, D data) {
        return visitQualifiedExpression(expression, data);
    }

    public R visitImportDirective(CjImportDirective cjImportDirective, D data) {
        return visitCjElement(cjImportDirective, data);
    }

    public R visitUserType(CjUserType cjUserType, D data) {
        return visitCjElement(cjUserType, data);
    }

    public R visitTypeArgumentList(CjTypeArgumentList cjTypeArgumentList, D data) {
        return visitCjElement(cjTypeArgumentList, data);
    }

    //    public R visitMacroArgumentList(CjMacroAttrArgumentList psi, D data) {
//        return visitCjElement(psi, data);
//    }
    public R visitImportAlias(@NotNull CjImportAlias importAlias, D data) {
        return visitCjElement(importAlias, data);
    }

    public R visitImportList(CjImportList cjImportList, D data) {
        return visitCjElement(cjImportList, data);
    }

    public R visitSuperTypeList(@NotNull CjSuperTypeList list, D data) {
        return visitCjElement(list, data);
    }

    public R visitConstructorCalleeExpression(@NotNull CjConstructorCalleeExpression constructorCalleeExpression, D data) {
        return visitCjElement(constructorCalleeExpression, data);
    }

    public R visitBasicType(CjBasicType cjBasicType, D data) {
        return visitCjElement(cjBasicType, data);
    }

    public R visitKeyword(CjKeyword cjKeyword, D data) {
        return visitCjElement(cjKeyword, data);
    }

    public R visitSimpleNameStringTemplateEntry(@NotNull CjSimpleNameStringTemplateEntry entry, D data) {
        return visitStringTemplateEntryWithExpression(entry, data);
    }

    public R visitEnumEntry(CjEnumEntry cjEnumEntry, D data) {
        return visitTypeStatement(cjEnumEntry, data);

    }

    public R visitMatchEntry(CjMatchEntry cjMatchEntry, D data) {
        return visitCjElement(cjMatchEntry, data);
    }

    public R visitTupleType(@NotNull CjTupleType cjTupleType, D data) {
        return visitCjElement(cjTupleType, data);
    }

    public R visitParenthesizedType(@NotNull CjParenthesizedType cjParenthesizedType, D data) {
        return visitCjElement(cjParenthesizedType, data);
    }

    public R visitFunctionType(@NotNull CjFunctionType type, D data) {
        return visitTypeElement(type, data);
    }

    private R visitTypeElement(@NotNull CjTypeElement type, D data) {
        return visitCjElement(type, data);
    }

    public R visitPropertyAccessor(@NotNull CjPropertyAccessor accessor, D data) {
        return visitDeclaration(accessor, data);
    }

    public R visitExtend(@NotNull CjExtend cjExtend, D data) {
        return visitCjElement(cjExtend, data);
    }

    public R visitClass(@NotNull CjClass cclass, D data) {
        return visitTypeStatement(cclass, data);
    }

    public R visitEnum(@NotNull CjEnum cenum, D data) {
        return visitTypeStatement(cenum, data);
    }

    public R visitStruct(@NotNull CjStruct cstruct, D data) {
        return visitTypeStatement(cstruct, data);
    }

    public R visitInterface(@NotNull CjInterface cinterface, D data) {
        return visitTypeStatement(cinterface, data);
    }


    //    类型声明
    public R visitTypeStatement(@NotNull CjTypeStatement typeStatement, D data) {
        return visitNamedDeclaration(typeStatement, data);
    }

    public R visitAnnotation(@NotNull CjAnnotation annotation, D data) {
        return visitCjElement(annotation, data);
    }

    public R visitMainFunction(CjMainFunction cjMainFunction, D data) {
        return visitDeclaration(cjMainFunction, data);
    }

    public R visitClassInitFunction(@NotNull CjClassInit cjClassInit, D data) {
        return visitDeclaration(cjClassInit, data);
    }
}
