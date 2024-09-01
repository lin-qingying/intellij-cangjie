package com.huawei.cangjie.psi


open class CjVisitorVoid : CjVisitor<Void?, Void?>() {

    open fun visitCjElement(element: CjElement) {
        super.visitCjElement(element, null)
    }

    open fun visitFunctionType(type: CjFunctionType) {
        super.visitFunctionType(type, null)
    }

    override fun visitOptionType(optionType: CjOptionType, data: Void?): Void? {
        visitOptionType(optionType)
        return null
    }

    open fun visitOptionType(optionType: CjOptionType) {
        super.visitOptionType(optionType, null)
    }

    open fun visitProperty(property: CjProperty) {
        super.visitProperty(property, null)
    }

    open fun visitVariable(variable: CjVariable) {
        super.visitVariable(variable, null)
    }

    open fun visitTypeStatement(typeStatement: CjTypeStatement) {
        super.visitTypeStatement(typeStatement, null)
    }

    open fun visitClass(cclass: CjClass) {
        super.visitClass(cclass, null)
    }

    override fun visitExtend(cjExtend: CjExtend, data: Void?): Void? {
        visitExtend(cjExtend)
        return null
    }

    open fun visitExtend(cjExtend: CjExtend) {
        super.visitExtend(cjExtend, null)
    }

    override fun visitMainFunction(cjMainFunction: CjMainFunction, data: Void?): Void? {
        visitMainFunction(cjMainFunction)
        return null
    }

    open fun visitMainFunction(cjMainFunction: CjMainFunction) {

    }

    open fun visitStruct(cstruct: CjStruct) {
        super.visitStruct(cstruct, null)
    }

    open fun visitEnum(cenum: CjEnum) {
        super.visitEnum(cenum, null)
    }

    open fun visitInterface(cinterface: CjInterface) {
        super.visitInterface(cinterface, null)
    }

    open fun visitDeclaration(dcl: CjDeclaration) {
        super.visitDeclaration(dcl, null)
    }

    open fun visitTypeAlias(typeAlias: CjTypeAlias) {
        super.visitTypeAlias(typeAlias, null)
    }


    open fun visitSecondaryConstructor(constructor: CjSecondaryConstructor) {
        super.visitSecondaryConstructor(constructor, null)
    }

    open fun visitPrimaryConstructor(constructor: CjPrimaryConstructor) {
        super.visitPrimaryConstructor(constructor, null)
    }

    open fun visitNamedFunction(function: CjNamedFunction) {
        super.visitNamedFunction(function, null)
    }


    open fun visitDestructuringDeclaration(destructuringDeclaration: CjDestructuringDeclaration) {
        super.visitDestructuringDeclaration(destructuringDeclaration, null)
    }

    fun visitDestructuringDeclarationEntry(multiDeclarationEntry: CjDestructuringDeclarationEntry) {
        super.visitDestructuringDeclarationEntry(multiDeclarationEntry, null)
    }

    open fun visitCjFile(file: CjFile) {
        super.visitCjFile(file, null)
    }


    open fun visitImportAlias(importAlias: CjImportAlias) {
        super.visitImportAlias(importAlias, null)
    }

    open fun visitImportDirective(importDirective: CjImportDirective) {
        super.visitImportDirective(importDirective, null)
    }

    open fun visitImportList(importList: CjImportList) {
        super.visitImportList(importList, null)
    }

    open fun visitClassBody(classBody: CjClassBody) {
        super.visitClassBody(classBody, null)
    }

    open fun visitModifierList(list: CjModifierList) {
        super.visitModifierList(list, null)
    }


    fun visitConstructorCalleeExpression(constructorCalleeExpression: CjConstructorCalleeExpression) {
        super.visitConstructorCalleeExpression(constructorCalleeExpression, null)
    }

    fun visitTypeParameterList(list: CjTypeParameterList) {
        super.visitTypeParameterList(list, null)
    }

    open fun visitTypeParameter(parameter: CjTypeParameter) {
        super.visitTypeParameter(parameter, null)
    }

    open fun visitEnumEntry(enumEntry: CjEnumEntry) {
        super.visitEnumEntry(enumEntry, null)
    }

    open fun visitParameterList(list: CjParameterList) {
        super.visitParameterList(list, null)
    }

    open fun visitParameter(parameter: CjParameter) {
        super.visitParameter(parameter, null)
    }

    open fun visitSuperTypeList(list: CjSuperTypeList) {
        super.visitSuperTypeList(list, null)
    }

    fun visitSuperTypeListEntry(specifier: CjSuperTypeListEntry) {
        super.visitSuperTypeListEntry(specifier, null)
    }


    open fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry) {
        super.visitSuperTypeCallEntry(call, null)
    }

    open fun visitSuperTypeEntry(specifier: CjSuperTypeEntry) {
        super.visitSuperTypeEntry(specifier, null)
    }

    fun visitContextReceiverList(contextReceiverList: CjContextReceiverList) {
        super.visitContextReceiverList(contextReceiverList, null)
    }

    open fun visitConstructorDelegationCall(call: CjConstructorDelegationCall) {
        super.visitConstructorDelegationCall(call, null)
    }


    fun visitTypeReference(typeReference: CjTypeReference) {
        super.visitTypeReference(typeReference, null)
    }

    fun visitValueArgumentList(list: CjValueArgumentList) {
        super.visitValueArgumentList(list, null)
    }


    open fun visitExpression(expression: CjExpression) {
        super.visitExpression(expression, null)
    }

    fun visitLoopExpression(loopExpression: CjLoopExpression) {
        super.visitLoopExpression(loopExpression, null)
    }

    open fun visitConstantExpression(expression: CjConstantExpression) {
        super.visitConstantExpression(expression, null)
    }

    open fun visitSimpleNameExpression(expression: CjSimpleNameExpression) {
        super.visitSimpleNameExpression(expression, null)
    }

    fun visitReferenceExpression(expression: CjReferenceExpression) {
        super.visitReferenceExpression(expression, null)
    }


    fun visitPrefixExpression(expression: CjPrefixExpression) {
        super.visitPrefixExpression(expression, null)
    }

    fun visitPostfixExpression(expression: CjPostfixExpression) {
        super.visitPostfixExpression(expression, null)
    }

    open fun visitUnaryExpression(expression: CjUnaryExpression) {
        super.visitUnaryExpression(expression, null)
    }

    open fun visitBinaryExpression(expression: CjBinaryExpression) {
        super.visitBinaryExpression(expression, null)
    }

    open fun visitReturnExpression(expression: CjReturnExpression) {
        super.visitReturnExpression(expression, null)
    }

    open fun visitExpressionWithLabel(expression: CjExpressionWithLabel) {
        super.visitExpressionWithLabel(expression, null)
    }

    open fun visitThrowExpression(expression: CjThrowExpression) {
        super.visitThrowExpression(expression, null)
    }

    open fun visitBreakExpression(expression: CjBreakExpression) {
        super.visitBreakExpression(expression, null)
    }

    open fun visitContinueExpression(expression: CjContinueExpression) {
        super.visitContinueExpression(expression, null)
    }

    open fun visitIfExpression(expression: CjIfExpression) {
        super.visitIfExpression(expression, null)
    }

    open fun visitMatchExpression(expression: CjMatchExpression) {
        super.visitMatchExpression(expression, null)
    }

    fun visitCollectionLiteralExpression(expression: CjCollectionLiteralExpression) {
        super.visitCollectionLiteralExpression(expression, null)
    }

    open fun visitTryExpression(expression: CjTryExpression) {
        super.visitTryExpression(expression, null)
    }

    open fun visitForExpression(expression: CjForExpression) {
        super.visitForExpression(expression, null)
    }

    open fun visitWhileExpression(expression: CjWhileExpression) {
        super.visitWhileExpression(expression, null)
    }

    open fun visitDoWhileExpression(expression: CjDoWhileExpression) {
        super.visitDoWhileExpression(expression, null)
    }


    open fun visitCallExpression(expression: CjCallExpression) {
        super.visitCallExpression(expression, null)
    }

    open fun visitArrayAccessExpression(expression: CjArrayAccessExpression) {
        super.visitArrayAccessExpression(expression, null)
    }

    open fun visitQualifiedExpression(expression: CjQualifiedExpression) {
        super.visitQualifiedExpression(expression, null)
    }


    open fun visitDotQualifiedExpression(expression: CjDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression, null)
    }


    open fun visitBlockExpression(expression: CjBlockExpression) {
        super.visitBlockExpression(expression, null)
    }

    fun visitCatchSection(catchClause: CjCatchClause) {
        super.visitCatchSection(catchClause, null)
    }

    fun visitFinallySection(finallySection: CjFinallySection) {
        super.visitFinallySection(finallySection, null)
    }

    fun visitTypeArgumentList(typeArgumentList: CjTypeArgumentList) {
        super.visitTypeArgumentList(typeArgumentList, null)
    }

    open fun visitThisExpression(expression: CjThisExpression) {
        super.visitThisExpression(expression, null)
    }

    fun visitSuperExpression(expression: CjSuperExpression) {
        super.visitSuperExpression(expression, null)
    }

    open fun visitParenthesizedExpression(expression: CjParenthesizedExpression) {
        super.visitParenthesizedExpression(expression, null)
    }


    open fun visitAnonymousInitializer(initializer: CjAnonymousInitializer) {
        super.visitAnonymousInitializer(initializer, null)
    }


    fun visitClassInitializer(initializer: CjClassInitializer) {
        super.visitClassInitializer(initializer, null)
    }


    fun visitTypeConstraintList(list: CjTypeConstraintList) {
        super.visitTypeConstraintList(list, null)
    }

    fun visitTypeConstraint(constraint: CjTypeConstraint) {
        super.visitTypeConstraint(constraint, null)
    }

    open fun visitUserType(type: CjUserType) {
        super.visitUserType(type, null)
    }

    open fun visitBasicType(type: CjBasicType) {
        super.visitBasicType(type, null)

    }


    open fun visitBinaryWithTypeRHSExpression(expression: CjBinaryExpressionWithTypeRHS) {
        super.visitBinaryWithTypeRHSExpression(expression, null)
    }

    open fun visitStringTemplateExpression(expression: CjStringTemplateExpression) {
        super.visitStringTemplateExpression(expression, null)
    }

    open fun visitNamedDeclaration(declaration: CjNamedDeclaration) {
        super.visitNamedDeclaration(declaration, null)
    }


    open fun visitTypeProjection(typeProjection: CjTypeProjection) {
        super.visitTypeProjection(typeProjection, null)
    }

    fun visitWhenEntry(CjWhenEntry: CjMatchEntry) {
        super.visitMatchEntry(CjWhenEntry, null)
    }

    open fun visitIsExpression(expression: CjIsExpression) {
        super.visitIsExpression(expression, null)
    }


    fun visitStringTemplateEntry(entry: CjStringTemplateEntry) {
        super.visitStringTemplateEntry(entry, null)
    }

    open fun visitStringTemplateEntryWithExpression(entry: CjStringTemplateEntryWithExpression) {
        super.visitStringTemplateEntryWithExpression(entry, null)
    }

    fun visitBlockStringTemplateEntry(entry: CjBlockStringTemplateEntry) {
        super.visitBlockStringTemplateEntry(entry, null)
    }

    fun visitSimpleNameStringTemplateEntry(entry: CjSimpleNameStringTemplateEntry) {
        super.visitSimpleNameStringTemplateEntry(entry, null)
    }

    open fun visitLiteralStringTemplateEntry(entry: CjLiteralStringTemplateEntry) {
        super.visitLiteralStringTemplateEntry(entry, null)
    }

    open fun visitEscapeStringTemplateEntry(entry: CjEscapeStringTemplateEntry) {
        super.visitEscapeStringTemplateEntry(entry, null)
    }

    open fun visitPackageDirective(directive: CjPackageDirective) {
        super.visitPackageDirective(directive, null)
    }

    // hidden methods
    override fun visitCjElement(element: CjElement, data: Void?): Void? {
        visitCjElement(element)
        return null
    }

    override fun visitDeclaration(dcl: CjDeclaration, data: Void?): Void? {
        visitDeclaration(dcl)
        return null
    }


    override fun visitProperty(property: CjProperty, data: Void?): Void? {
        visitProperty(property)
        return null
    }

    override fun visitVariable(variable: CjVariable, data: Void?): Void? {

        visitVariable(variable)
        return null
    }

    override fun visitSecondaryConstructor(constructor: CjSecondaryConstructor, data: Void?): Void? {
        visitSecondaryConstructor(constructor)
        return null
    }

    override fun visitPrimaryConstructor(constructor: CjPrimaryConstructor, data: Void?): Void? {
        visitPrimaryConstructor(constructor)
        return null
    }

    override fun visitNamedFunction(function: CjNamedFunction, data: Void?): Void? {
        visitNamedFunction(function)
        return null
    }

    override fun visitTypeAlias(typeAlias: CjTypeAlias, data: Void?): Void? {
        visitTypeAlias(typeAlias)
        return null
    }

    override fun visitDestructuringDeclaration(
        multiDeclaration: CjDestructuringDeclaration,
        data: Void?
    ): Void? {
        visitDestructuringDeclaration(multiDeclaration)
        return null
    }

    override fun visitDestructuringDeclarationEntry(
        multiDeclarationEntry: CjDestructuringDeclarationEntry,
        data: Void?
    ): Void? {
        visitDestructuringDeclarationEntry(multiDeclarationEntry)
        return null
    }

    override fun visitCjFile(file: CjFile, data: Void?): Void? {
        visitCjFile(file)
        return null
    }


    override fun visitImportDirective(importDirective: CjImportDirective, data: Void?): Void? {
        visitImportDirective(importDirective)
        return null
    }

    override fun visitImportList(importList: CjImportList, data: Void?): Void? {
        visitImportList(importList)
        return null
    }

    override fun visitClassBody(classBody: CjClassBody, data: Void?): Void? {
        visitClassBody(classBody)
        return null
    }

    override fun visitModifierList(list: CjModifierList, data: Void?): Void? {
        visitModifierList(list)
        return null
    }


    override fun visitConstructorCalleeExpression(
        constructorCalleeExpression: CjConstructorCalleeExpression,
        data: Void?
    ): Void? {
        visitConstructorCalleeExpression(constructorCalleeExpression)
        return null
    }

    override fun visitTypeParameterList(list: CjTypeParameterList, data: Void?): Void? {
        visitTypeParameterList(list)
        return null
    }

    override fun visitTypeParameter(parameter: CjTypeParameter, data: Void?): Void? {
        visitTypeParameter(parameter)
        return null
    }

    override fun visitEnumEntry(enumEntry: CjEnumEntry, data: Void?): Void? {
        visitEnumEntry(enumEntry)
        return null
    }

    override fun visitParameterList(list: CjParameterList, data: Void?): Void? {
        visitParameterList(list)
        return null
    }

    override fun visitParameter(parameter: CjParameter, data: Void?): Void? {
        visitParameter(parameter)
        return null
    }

    override fun visitSuperTypeList(list: CjSuperTypeList, data: Void?): Void? {
        visitSuperTypeList(list)
        return null
    }

    override fun visitSuperTypeListEntry(specifier: CjSuperTypeListEntry, data: Void?): Void? {
        visitSuperTypeListEntry(specifier)
        return null
    }


    override fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry, data: Void?): Void? {
        visitSuperTypeCallEntry(call)
        return null
    }

    override fun visitSuperTypeEntry(specifier: CjSuperTypeEntry, data: Void?): Void? {
        visitSuperTypeEntry(specifier)
        return null
    }

    override fun visitConstructorDelegationCall(
        call: CjConstructorDelegationCall,
        data: Void?
    ): Void? {
        visitConstructorDelegationCall(call)
        return null
    }


    override fun visitTypeReference(typeReference: CjTypeReference, data: Void?): Void? {
        visitTypeReference(typeReference)
        return null
    }

    override fun visitValueArgumentList(list: CjValueArgumentList, data: Void?): Void? {
        visitValueArgumentList(list)
        return null
    }


    override fun visitExpression(expression: CjExpression, data: Void?): Void? {
        visitExpression(expression)
        return null
    }

    override fun visitLoopExpression(loopExpression: CjLoopExpression, data: Void?): Void? {
        visitLoopExpression(loopExpression)
        return null
    }

    override fun visitConstantExpression(expression: CjConstantExpression, data: Void?): Void? {
        visitConstantExpression(expression)
        return null
    }

    override fun visitSimpleNameExpression(expression: CjSimpleNameExpression, data: Void?): Void? {
        visitSimpleNameExpression(expression)
        return null
    }

    override fun visitReferenceExpression(
        expression: CjReferenceExpression,
        data: Void?
    ): Void? {
        visitReferenceExpression(expression)
        return null
    }

    override fun visitPrefixExpression(expression: CjPrefixExpression, data: Void?): Void? {
        visitPrefixExpression(expression)
        return null
    }

    override fun visitPostfixExpression(expression: CjPostfixExpression, data: Void?): Void? {
        visitPostfixExpression(expression)
        return null
    }

    override fun visitUnaryExpression(expression: CjUnaryExpression, data: Void?): Void? {
        visitUnaryExpression(expression)
        return null
    }

    override fun visitBinaryExpression(expression: CjBinaryExpression, data: Void?): Void? {
        visitBinaryExpression(expression)
        return null
    }

    override fun visitReturnExpression(expression: CjReturnExpression, data: Void?): Void? {
        visitReturnExpression(expression)
        return null
    }

    override fun visitExpressionWithLabel(expression: CjExpressionWithLabel, data: Void?): Void? {
        visitExpressionWithLabel(expression)
        return null
    }

    override fun visitThrowExpression(expression: CjThrowExpression, data: Void?): Void? {
        visitThrowExpression(expression)
        return null
    }

    override fun visitBreakExpression(expression: CjBreakExpression, data: Void?): Void? {
        visitBreakExpression(expression)
        return null
    }

    override fun visitContinueExpression(expression: CjContinueExpression, data: Void?): Void? {
        visitContinueExpression(expression)
        return null
    }

    override fun visitIfExpression(expression: CjIfExpression, data: Void?): Void? {
        visitIfExpression(expression)
        return null
    }

    override fun visitMatchExpression(expression: CjMatchExpression, data: Void?): Void? {
        visitMatchExpression(expression)
        return null
    }

    override fun visitCollectionLiteralExpression(
        expression: CjCollectionLiteralExpression,
        data: Void?
    ): Void? {
        visitCollectionLiteralExpression(expression)
        return null
    }

    override fun visitTryExpression(expression: CjTryExpression, data: Void?): Void? {
        visitTryExpression(expression)
        return null
    }

    override fun visitForExpression(expression: CjForExpression, data: Void?): Void? {
        visitForExpression(expression)
        return null
    }

    override fun visitWhileExpression(expression: CjWhileExpression, data: Void?): Void? {
        visitWhileExpression(expression)
        return null
    }

    override fun visitDoWhileExpression(expression: CjDoWhileExpression, data: Void?): Void? {
        visitDoWhileExpression(expression)
        return null
    }


    override fun visitCallExpression(expression: CjCallExpression, data: Void?): Void? {
        visitCallExpression(expression)
        return null
    }

    override fun visitArrayAccessExpression(
        expression: CjArrayAccessExpression,
        data: Void?
    ): Void? {
        visitArrayAccessExpression(expression)
        return null
    }

    override fun visitQualifiedExpression(expression: CjQualifiedExpression, data: Void?): Void? {
        visitQualifiedExpression(expression)
        return null
    }


    override fun visitDotQualifiedExpression(expression: CjDotQualifiedExpression, data: Void?): Void? {
        visitDotQualifiedExpression(expression)
        return null
    }


    override fun visitBlockExpression(expression: CjBlockExpression, data: Void?): Void? {
        visitBlockExpression(expression)
        return null
    }

    override fun visitCatchSection(catchClause: CjCatchClause, data: Void?): Void? {
        visitCatchSection(catchClause)
        return null
    }

    override fun visitFinallySection(finallySection: CjFinallySection, data: Void?): Void? {
        visitFinallySection(finallySection)
        return null
    }

    override fun visitTypeArgumentList(
        typeArgumentList: CjTypeArgumentList,
        data: Void?
    ): Void? {
        visitTypeArgumentList(typeArgumentList)
        return null
    }

    override fun visitThisExpression(expression: CjThisExpression, data: Void?): Void? {
        visitThisExpression(expression)
        return null
    }

    override fun visitSuperExpression(expression: CjSuperExpression, data: Void?): Void? {
        visitSuperExpression(expression)
        return null
    }

    override fun visitParenthesizedExpression(
        expression: CjParenthesizedExpression,
        data: Void?
    ): Void? {
        visitParenthesizedExpression(expression)
        return null
    }


    override fun visitAnonymousInitializer(initializer: CjAnonymousInitializer, data: Void?): Void? {
        visitAnonymousInitializer(initializer)
        return null
    }


    override fun visitTypeConstraintList(list: CjTypeConstraintList, data: Void?): Void? {
        visitTypeConstraintList(list)
        return null
    }

    override fun visitTypeConstraint(constraint: CjTypeConstraint, data: Void?): Void? {
        visitTypeConstraint(constraint)
        return null
    }

    override fun visitUserType(type: CjUserType, data: Void?): Void? {
        visitUserType(type)
        return null
    }

    override fun visitBasicType(cjBasicType: CjBasicType, data: Void?): Void? {
        visitBasicType(cjBasicType)
        return null
    }


    override fun visitBinaryWithTypeRHSExpression(
        expression: CjBinaryExpressionWithTypeRHS,
        data: Void?
    ): Void? {
        visitBinaryWithTypeRHSExpression(expression)
        return null
    }

    override fun visitStringTemplateExpression(
        expression: CjStringTemplateExpression,
        data: Void?
    ): Void? {
        visitStringTemplateExpression(expression)
        return null
    }

    override fun visitNamedDeclaration(declaration: CjNamedDeclaration, data: Void?): Void? {
        visitNamedDeclaration(declaration)
        return null
    }


    override fun visitTypeProjection(typeProjection: CjTypeProjection, data: Void?): Void? {
        visitTypeProjection(typeProjection)
        return null
    }

    fun visitMatchEntry(cjMatchEntry: CjMatchEntry) {
        super.visitMatchEntry(cjMatchEntry, null)
    }

    override fun visitMatchEntry(cjMatchEntry: CjMatchEntry, data: Void?): Void? {
        visitMatchEntry(cjMatchEntry)
        return null
    }

    override fun visitIsExpression(expression: CjIsExpression, data: Void?): Void? {
        visitIsExpression(expression)
        return null
    }


    override fun visitStringTemplateEntry(entry: CjStringTemplateEntry, data: Void?): Void? {
        visitStringTemplateEntry(entry)
        return null
    }

    override fun visitStringTemplateEntryWithExpression(
        entry: CjStringTemplateEntryWithExpression,
        data: Void?
    ): Void? {
        visitStringTemplateEntryWithExpression(entry)
        return null
    }

    override fun visitBlockStringTemplateEntry(
        entry: CjBlockStringTemplateEntry,
        data: Void?
    ): Void? {
        visitBlockStringTemplateEntry(entry)
        return null
    }

    override fun visitSimpleNameStringTemplateEntry(
        entry: CjSimpleNameStringTemplateEntry,
        data: Void?
    ): Void? {
        visitSimpleNameStringTemplateEntry(entry)
        return null
    }

    override fun visitLiteralStringTemplateEntry(
        entry: CjLiteralStringTemplateEntry,
        data: Void?
    ): Void? {
        visitLiteralStringTemplateEntry(entry)
        return null
    }

    override fun visitEscapeStringTemplateEntry(
        entry: CjEscapeStringTemplateEntry,
        data: Void?
    ): Void? {
        visitEscapeStringTemplateEntry(entry)
        return null
    }

    override fun visitPackageDirective(directive: CjPackageDirective, data: Void?): Void? {
        visitPackageDirective(directive)
        return null
    }

    override fun visitTypeStatement(typeStatement: CjTypeStatement, data: Void?): Void? {
        visitTypeStatement(typeStatement)
        return null
    }

    override fun visitClass(cclass: CjClass, data: Void?): Void? {
        visitClass(cclass)
        return null

    }

    override fun visitStruct(cstruct: CjStruct, data: Void?): Void? {
        visitStruct(cstruct)
        return null

    }

    override fun visitEnum(cenum: CjEnum, data: Void?): Void? {
        visitEnum(cenum)
        return null

    }

    override fun visitInterface(cinterface: CjInterface, data: Void?): Void? {
        visitInterface(cinterface)
        return null

    }

    override fun visitClassInitializer(initializer: CjClassInitializer, data: Void?): Void? {
        visitClassInitializer(initializer)
        return null
    }
}
