// Generated from D:/Code/intellij/intellij-cangjie/grammar/src/main/antlr/CangJie.g4 by ANTLR 4.13.1

package cangjie.antlr;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link CangJieParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface CangJieVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link CangJieParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(CangJieParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#translationUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTranslationUnit(CangJieParser.TranslationUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#end}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnd(CangJieParser.EndContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#preamble}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPreamble(CangJieParser.PreambleContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#packageHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackageHeader(CangJieParser.PackageHeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#packageNameIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackageNameIdentifier(CangJieParser.PackageNameIdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#importList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportList(CangJieParser.ImportListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#importAllOrSpecified}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportAllOrSpecified(CangJieParser.ImportAllOrSpecifiedContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#importSpecified}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportSpecified(CangJieParser.ImportSpecifiedContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#importAll}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportAll(CangJieParser.ImportAllContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#importAlias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportAlias(CangJieParser.ImportAliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#topLevelObject}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTopLevelObject(CangJieParser.TopLevelObjectContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#classDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassDefinition(CangJieParser.ClassDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#superClassOrInterfaces}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuperClassOrInterfaces(CangJieParser.SuperClassOrInterfacesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#classModifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassModifierList(CangJieParser.ClassModifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#classModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassModifier(CangJieParser.ClassModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#typeParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameters(CangJieParser.TypeParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#superClass}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuperClass(CangJieParser.SuperClassContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#classType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassType(CangJieParser.ClassTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#typeArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArguments(CangJieParser.TypeArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#superInterfaces}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuperInterfaces(CangJieParser.SuperInterfacesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#interfaceType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceType(CangJieParser.InterfaceTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#genericConstraints}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericConstraints(CangJieParser.GenericConstraintsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#upperBounds}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpperBounds(CangJieParser.UpperBoundsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#classBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassBody(CangJieParser.ClassBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#classMemberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassMemberDeclaration(CangJieParser.ClassMemberDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#classInit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassInit(CangJieParser.ClassInitContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#staticInit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStaticInit(CangJieParser.StaticInitContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#classPrimaryInit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassPrimaryInit(CangJieParser.ClassPrimaryInitContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#className}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassName(CangJieParser.ClassNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#classPrimaryInitParamLists}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassPrimaryInitParamLists(CangJieParser.ClassPrimaryInitParamListsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#classUnnamedInitParamList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassUnnamedInitParamList(CangJieParser.ClassUnnamedInitParamListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#classNamedInitParamList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassNamedInitParamList(CangJieParser.ClassNamedInitParamListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#classUnnamedInitParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassUnnamedInitParam(CangJieParser.ClassUnnamedInitParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#classNamedInitParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassNamedInitParam(CangJieParser.ClassNamedInitParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#classNonStaticMemberModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassNonStaticMemberModifier(CangJieParser.ClassNonStaticMemberModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#interfaceDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceDefinition(CangJieParser.InterfaceDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#interfaceBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceBody(CangJieParser.InterfaceBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceMemberDeclaration(CangJieParser.InterfaceMemberDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#interfaceModifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceModifierList(CangJieParser.InterfaceModifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#interfaceModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceModifier(CangJieParser.InterfaceModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#functionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDefinition(CangJieParser.FunctionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#operatorFunctionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperatorFunctionDefinition(CangJieParser.OperatorFunctionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#functionParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionParameters(CangJieParser.FunctionParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#nondefaultParameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNondefaultParameterList(CangJieParser.NondefaultParameterListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#unnamedParameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnnamedParameterList(CangJieParser.UnnamedParameterListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#unnamedParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnnamedParameter(CangJieParser.UnnamedParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#namedParameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedParameterList(CangJieParser.NamedParameterListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#namedParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedParameter(CangJieParser.NamedParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#defaultParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultParameter(CangJieParser.DefaultParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#functionModifierList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionModifierList(CangJieParser.FunctionModifierListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#functionModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionModifier(CangJieParser.FunctionModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#variableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclaration(CangJieParser.VariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#variableModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableModifier(CangJieParser.VariableModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#enumDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumDefinition(CangJieParser.EnumDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#enumBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumBody(CangJieParser.EnumBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#caseBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCaseBody(CangJieParser.CaseBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#enumModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumModifier(CangJieParser.EnumModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#structDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructDefinition(CangJieParser.StructDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#structBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructBody(CangJieParser.StructBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#structMemberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructMemberDeclaration(CangJieParser.StructMemberDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#structInit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructInit(CangJieParser.StructInitContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#structPrimaryInit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructPrimaryInit(CangJieParser.StructPrimaryInitContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#structName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructName(CangJieParser.StructNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#structPrimaryInitParamLists}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructPrimaryInitParamLists(CangJieParser.StructPrimaryInitParamListsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#structUnnamedInitParamList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructUnnamedInitParamList(CangJieParser.StructUnnamedInitParamListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#structNamedInitParamList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructNamedInitParamList(CangJieParser.StructNamedInitParamListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#structUnnamedInitParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructUnnamedInitParam(CangJieParser.StructUnnamedInitParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#structNamedInitParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructNamedInitParam(CangJieParser.StructNamedInitParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#structModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructModifier(CangJieParser.StructModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#structNonStaticMemberModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructNonStaticMemberModifier(CangJieParser.StructNonStaticMemberModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#typeAlias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeAlias(CangJieParser.TypeAliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#typeModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeModifier(CangJieParser.TypeModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#extendDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtendDefinition(CangJieParser.ExtendDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#extendType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtendType(CangJieParser.ExtendTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#extendBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtendBody(CangJieParser.ExtendBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#extendMemberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtendMemberDeclaration(CangJieParser.ExtendMemberDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#foreignDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeignDeclaration(CangJieParser.ForeignDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#foreignBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeignBody(CangJieParser.ForeignBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#foreignMemberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeignMemberDeclaration(CangJieParser.ForeignMemberDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#annotationList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationList(CangJieParser.AnnotationListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotation(CangJieParser.AnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#annotationArgumentList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationArgumentList(CangJieParser.AnnotationArgumentListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#annotationArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationArgument(CangJieParser.AnnotationArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#macroDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMacroDefinition(CangJieParser.MacroDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#macroWithoutAttrParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMacroWithoutAttrParam(CangJieParser.MacroWithoutAttrParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#macroWithAttrParam}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMacroWithAttrParam(CangJieParser.MacroWithAttrParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#macroInputDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMacroInputDecl(CangJieParser.MacroInputDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#macroAttrDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMacroAttrDecl(CangJieParser.MacroAttrDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#propertyDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyDefinition(CangJieParser.PropertyDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#propertyBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyBody(CangJieParser.PropertyBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#propertyMemberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyMemberDeclaration(CangJieParser.PropertyMemberDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#propertyModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyModifier(CangJieParser.PropertyModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#mainDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMainDefinition(CangJieParser.MainDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(CangJieParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#arrowType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrowType(CangJieParser.ArrowTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#arrowParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrowParameters(CangJieParser.ArrowParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#tupleType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTupleType(CangJieParser.TupleTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#prefixType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrefixType(CangJieParser.PrefixTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#prefixTypeOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrefixTypeOperator(CangJieParser.PrefixTypeOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#atomicType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtomicType(CangJieParser.AtomicTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#charLangTypes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharLangTypes(CangJieParser.CharLangTypesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#numericTypes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericTypes(CangJieParser.NumericTypesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#userType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUserType(CangJieParser.UserTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#parenthesizedType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedType(CangJieParser.ParenthesizedTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(CangJieParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#assignmentExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentExpression(CangJieParser.AssignmentExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#tupleLeftValueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTupleLeftValueExpression(CangJieParser.TupleLeftValueExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#leftValueExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLeftValueExpression(CangJieParser.LeftValueExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#leftValueExpressionWithoutWildCard}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLeftValueExpressionWithoutWildCard(CangJieParser.LeftValueExpressionWithoutWildCardContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#leftAuxExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLeftAuxExpression(CangJieParser.LeftAuxExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#assignableSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignableSuffix(CangJieParser.AssignableSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#fieldAccess}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldAccess(CangJieParser.FieldAccessContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#flowExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlowExpression(CangJieParser.FlowExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#coalescingExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoalescingExpression(CangJieParser.CoalescingExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#logicDisjunctionExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicDisjunctionExpression(CangJieParser.LogicDisjunctionExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#logicConjunctionExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicConjunctionExpression(CangJieParser.LogicConjunctionExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#rangeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRangeExpression(CangJieParser.RangeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#bitwiseDisjunctionExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitwiseDisjunctionExpression(CangJieParser.BitwiseDisjunctionExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#bitwiseXorExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitwiseXorExpression(CangJieParser.BitwiseXorExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#bitwiseConjunctionExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitwiseConjunctionExpression(CangJieParser.BitwiseConjunctionExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#equalityComparisonExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityComparisonExpression(CangJieParser.EqualityComparisonExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#comparisonOrTypeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonOrTypeExpression(CangJieParser.ComparisonOrTypeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#shiftingExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShiftingExpression(CangJieParser.ShiftingExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#additiveExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExpression(CangJieParser.AdditiveExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExpression(CangJieParser.MultiplicativeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#exponentExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExponentExpression(CangJieParser.ExponentExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#prefixUnaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrefixUnaryExpression(CangJieParser.PrefixUnaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#incAndDecExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIncAndDecExpression(CangJieParser.IncAndDecExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#postfixExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixExpression(CangJieParser.PostfixExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#questSeperatedItems}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuestSeperatedItems(CangJieParser.QuestSeperatedItemsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#questSeperatedItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuestSeperatedItem(CangJieParser.QuestSeperatedItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#itemAfterQuest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitItemAfterQuest(CangJieParser.ItemAfterQuestContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#callSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallSuffix(CangJieParser.CallSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#valueArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueArgument(CangJieParser.ValueArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#refTransferExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRefTransferExpression(CangJieParser.RefTransferExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#indexAccess}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndexAccess(CangJieParser.IndexAccessContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#rangeElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRangeElement(CangJieParser.RangeElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#atomicExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtomicExpression(CangJieParser.AtomicExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#literalConstant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralConstant(CangJieParser.LiteralConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#booleanLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanLiteral(CangJieParser.BooleanLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#stringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(CangJieParser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#lineStringContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLineStringContent(CangJieParser.LineStringContentContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#lineStringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLineStringLiteral(CangJieParser.LineStringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#lineStringExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLineStringExpression(CangJieParser.LineStringExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#multiLineStringContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiLineStringContent(CangJieParser.MultiLineStringContentContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#multiLineStringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiLineStringLiteral(CangJieParser.MultiLineStringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#multiLineStringExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiLineStringExpression(CangJieParser.MultiLineStringExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#collectionLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollectionLiteral(CangJieParser.CollectionLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#arrayLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayLiteral(CangJieParser.ArrayLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElements(CangJieParser.ElementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElement(CangJieParser.ElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#expressionElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionElement(CangJieParser.ExpressionElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#spreadElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpreadElement(CangJieParser.SpreadElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#tupleLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTupleLiteral(CangJieParser.TupleLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#unitLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnitLiteral(CangJieParser.UnitLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#ifExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfExpression(CangJieParser.IfExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#deconstructPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeconstructPattern(CangJieParser.DeconstructPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#matchExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatchExpression(CangJieParser.MatchExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#matchCase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatchCase(CangJieParser.MatchCaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#patternGuard}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPatternGuard(CangJieParser.PatternGuardContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPattern(CangJieParser.PatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#constantPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantPattern(CangJieParser.ConstantPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#wildcardPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWildcardPattern(CangJieParser.WildcardPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#varBindingPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarBindingPattern(CangJieParser.VarBindingPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#tuplePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTuplePattern(CangJieParser.TuplePatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#typePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypePattern(CangJieParser.TypePatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#enumPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumPattern(CangJieParser.EnumPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#enumPatternParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumPatternParameters(CangJieParser.EnumPatternParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#loopExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoopExpression(CangJieParser.LoopExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#forInExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForInExpression(CangJieParser.ForInExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#patternsMaybeIrrefutable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPatternsMaybeIrrefutable(CangJieParser.PatternsMaybeIrrefutableContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#whileExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileExpression(CangJieParser.WhileExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#doWhileExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoWhileExpression(CangJieParser.DoWhileExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#tryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTryExpression(CangJieParser.TryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#catchPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchPattern(CangJieParser.CatchPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#exceptionTypePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExceptionTypePattern(CangJieParser.ExceptionTypePatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#resourceSpecifications}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResourceSpecifications(CangJieParser.ResourceSpecificationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#resourceSpecification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResourceSpecification(CangJieParser.ResourceSpecificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#jumpExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJumpExpression(CangJieParser.JumpExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#numericTypeConvExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumericTypeConvExpr(CangJieParser.NumericTypeConvExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#thisSuperExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThisSuperExpression(CangJieParser.ThisSuperExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#lambdaExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaExpression(CangJieParser.LambdaExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#trailingLambdaExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrailingLambdaExpression(CangJieParser.TrailingLambdaExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#lambdaParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaParameters(CangJieParser.LambdaParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#lambdaParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaParameter(CangJieParser.LambdaParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#spawnExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpawnExpression(CangJieParser.SpawnExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#synchronizedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSynchronizedExpression(CangJieParser.SynchronizedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#parenthesizedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedExpression(CangJieParser.ParenthesizedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(CangJieParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#unsafeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnsafeExpression(CangJieParser.UnsafeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#expressionOrDeclarations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionOrDeclarations(CangJieParser.ExpressionOrDeclarationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#expressionOrDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionOrDeclaration(CangJieParser.ExpressionOrDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#varOrfuncDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarOrfuncDeclaration(CangJieParser.VarOrfuncDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#quoteExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuoteExpression(CangJieParser.QuoteExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#quoteExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuoteExpr(CangJieParser.QuoteExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#quoteParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuoteParameters(CangJieParser.QuoteParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#quoteToken}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuoteToken(CangJieParser.QuoteTokenContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#quoteInterpolate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuoteInterpolate(CangJieParser.QuoteInterpolateContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#macroExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMacroExpression(CangJieParser.MacroExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#macroAttrExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMacroAttrExpr(CangJieParser.MacroAttrExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#macroInputExprWithoutParens}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMacroInputExprWithoutParens(CangJieParser.MacroInputExprWithoutParensContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#macroInputExprWithParens}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMacroInputExprWithParens(CangJieParser.MacroInputExprWithParensContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#macroTokens}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMacroTokens(CangJieParser.MacroTokensContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#assignmentOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentOperator(CangJieParser.AssignmentOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#equalityOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityOperator(CangJieParser.EqualityOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#comparisonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonOperator(CangJieParser.ComparisonOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#shiftingOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShiftingOperator(CangJieParser.ShiftingOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#flowOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlowOperator(CangJieParser.FlowOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#additiveOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveOperator(CangJieParser.AdditiveOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#exponentOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExponentOperator(CangJieParser.ExponentOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#multiplicativeOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeOperator(CangJieParser.MultiplicativeOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#prefixUnaryOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrefixUnaryOperator(CangJieParser.PrefixUnaryOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CangJieParser#overloadedOperators}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOverloadedOperators(CangJieParser.OverloadedOperatorsContext ctx);
}