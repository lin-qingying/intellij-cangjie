// Generated from D:/Code/intellij/intellij-cangjie/grammar/src/main/antlr/CangJie.g4 by ANTLR 4.13.1

package cangjie.antlr;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link CangJieParser}.
 */
public interface CangJieListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link CangJieParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(CangJieParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(CangJieParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#translationUnit}.
	 * @param ctx the parse tree
	 */
	void enterTranslationUnit(CangJieParser.TranslationUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#translationUnit}.
	 * @param ctx the parse tree
	 */
	void exitTranslationUnit(CangJieParser.TranslationUnitContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#end}.
	 * @param ctx the parse tree
	 */
	void enterEnd(CangJieParser.EndContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#end}.
	 * @param ctx the parse tree
	 */
	void exitEnd(CangJieParser.EndContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#preamble}.
	 * @param ctx the parse tree
	 */
	void enterPreamble(CangJieParser.PreambleContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#preamble}.
	 * @param ctx the parse tree
	 */
	void exitPreamble(CangJieParser.PreambleContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#packageHeader}.
	 * @param ctx the parse tree
	 */
	void enterPackageHeader(CangJieParser.PackageHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#packageHeader}.
	 * @param ctx the parse tree
	 */
	void exitPackageHeader(CangJieParser.PackageHeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#packageNameIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterPackageNameIdentifier(CangJieParser.PackageNameIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#packageNameIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitPackageNameIdentifier(CangJieParser.PackageNameIdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#importList}.
	 * @param ctx the parse tree
	 */
	void enterImportList(CangJieParser.ImportListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#importList}.
	 * @param ctx the parse tree
	 */
	void exitImportList(CangJieParser.ImportListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#importAllOrSpecified}.
	 * @param ctx the parse tree
	 */
	void enterImportAllOrSpecified(CangJieParser.ImportAllOrSpecifiedContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#importAllOrSpecified}.
	 * @param ctx the parse tree
	 */
	void exitImportAllOrSpecified(CangJieParser.ImportAllOrSpecifiedContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#importSpecified}.
	 * @param ctx the parse tree
	 */
	void enterImportSpecified(CangJieParser.ImportSpecifiedContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#importSpecified}.
	 * @param ctx the parse tree
	 */
	void exitImportSpecified(CangJieParser.ImportSpecifiedContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#importAll}.
	 * @param ctx the parse tree
	 */
	void enterImportAll(CangJieParser.ImportAllContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#importAll}.
	 * @param ctx the parse tree
	 */
	void exitImportAll(CangJieParser.ImportAllContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#importAlias}.
	 * @param ctx the parse tree
	 */
	void enterImportAlias(CangJieParser.ImportAliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#importAlias}.
	 * @param ctx the parse tree
	 */
	void exitImportAlias(CangJieParser.ImportAliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#topLevelObject}.
	 * @param ctx the parse tree
	 */
	void enterTopLevelObject(CangJieParser.TopLevelObjectContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#topLevelObject}.
	 * @param ctx the parse tree
	 */
	void exitTopLevelObject(CangJieParser.TopLevelObjectContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#classDefinition}.
	 * @param ctx the parse tree
	 */
	void enterClassDefinition(CangJieParser.ClassDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#classDefinition}.
	 * @param ctx the parse tree
	 */
	void exitClassDefinition(CangJieParser.ClassDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#superClassOrInterfaces}.
	 * @param ctx the parse tree
	 */
	void enterSuperClassOrInterfaces(CangJieParser.SuperClassOrInterfacesContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#superClassOrInterfaces}.
	 * @param ctx the parse tree
	 */
	void exitSuperClassOrInterfaces(CangJieParser.SuperClassOrInterfacesContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#classModifierList}.
	 * @param ctx the parse tree
	 */
	void enterClassModifierList(CangJieParser.ClassModifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#classModifierList}.
	 * @param ctx the parse tree
	 */
	void exitClassModifierList(CangJieParser.ClassModifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#classModifier}.
	 * @param ctx the parse tree
	 */
	void enterClassModifier(CangJieParser.ClassModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#classModifier}.
	 * @param ctx the parse tree
	 */
	void exitClassModifier(CangJieParser.ClassModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameters(CangJieParser.TypeParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameters(CangJieParser.TypeParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#superClass}.
	 * @param ctx the parse tree
	 */
	void enterSuperClass(CangJieParser.SuperClassContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#superClass}.
	 * @param ctx the parse tree
	 */
	void exitSuperClass(CangJieParser.SuperClassContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#classType}.
	 * @param ctx the parse tree
	 */
	void enterClassType(CangJieParser.ClassTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#classType}.
	 * @param ctx the parse tree
	 */
	void exitClassType(CangJieParser.ClassTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void enterTypeArguments(CangJieParser.TypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void exitTypeArguments(CangJieParser.TypeArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#superInterfaces}.
	 * @param ctx the parse tree
	 */
	void enterSuperInterfaces(CangJieParser.SuperInterfacesContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#superInterfaces}.
	 * @param ctx the parse tree
	 */
	void exitSuperInterfaces(CangJieParser.SuperInterfacesContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#interfaceType}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceType(CangJieParser.InterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#interfaceType}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceType(CangJieParser.InterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#genericConstraints}.
	 * @param ctx the parse tree
	 */
	void enterGenericConstraints(CangJieParser.GenericConstraintsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#genericConstraints}.
	 * @param ctx the parse tree
	 */
	void exitGenericConstraints(CangJieParser.GenericConstraintsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#upperBounds}.
	 * @param ctx the parse tree
	 */
	void enterUpperBounds(CangJieParser.UpperBoundsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#upperBounds}.
	 * @param ctx the parse tree
	 */
	void exitUpperBounds(CangJieParser.UpperBoundsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#classBody}.
	 * @param ctx the parse tree
	 */
	void enterClassBody(CangJieParser.ClassBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#classBody}.
	 * @param ctx the parse tree
	 */
	void exitClassBody(CangJieParser.ClassBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#classMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassMemberDeclaration(CangJieParser.ClassMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#classMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassMemberDeclaration(CangJieParser.ClassMemberDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#classInit}.
	 * @param ctx the parse tree
	 */
	void enterClassInit(CangJieParser.ClassInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#classInit}.
	 * @param ctx the parse tree
	 */
	void exitClassInit(CangJieParser.ClassInitContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#staticInit}.
	 * @param ctx the parse tree
	 */
	void enterStaticInit(CangJieParser.StaticInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#staticInit}.
	 * @param ctx the parse tree
	 */
	void exitStaticInit(CangJieParser.StaticInitContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#classPrimaryInit}.
	 * @param ctx the parse tree
	 */
	void enterClassPrimaryInit(CangJieParser.ClassPrimaryInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#classPrimaryInit}.
	 * @param ctx the parse tree
	 */
	void exitClassPrimaryInit(CangJieParser.ClassPrimaryInitContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#className}.
	 * @param ctx the parse tree
	 */
	void enterClassName(CangJieParser.ClassNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#className}.
	 * @param ctx the parse tree
	 */
	void exitClassName(CangJieParser.ClassNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#classPrimaryInitParamLists}.
	 * @param ctx the parse tree
	 */
	void enterClassPrimaryInitParamLists(CangJieParser.ClassPrimaryInitParamListsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#classPrimaryInitParamLists}.
	 * @param ctx the parse tree
	 */
	void exitClassPrimaryInitParamLists(CangJieParser.ClassPrimaryInitParamListsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#classUnnamedInitParamList}.
	 * @param ctx the parse tree
	 */
	void enterClassUnnamedInitParamList(CangJieParser.ClassUnnamedInitParamListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#classUnnamedInitParamList}.
	 * @param ctx the parse tree
	 */
	void exitClassUnnamedInitParamList(CangJieParser.ClassUnnamedInitParamListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#classNamedInitParamList}.
	 * @param ctx the parse tree
	 */
	void enterClassNamedInitParamList(CangJieParser.ClassNamedInitParamListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#classNamedInitParamList}.
	 * @param ctx the parse tree
	 */
	void exitClassNamedInitParamList(CangJieParser.ClassNamedInitParamListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#classUnnamedInitParam}.
	 * @param ctx the parse tree
	 */
	void enterClassUnnamedInitParam(CangJieParser.ClassUnnamedInitParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#classUnnamedInitParam}.
	 * @param ctx the parse tree
	 */
	void exitClassUnnamedInitParam(CangJieParser.ClassUnnamedInitParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#classNamedInitParam}.
	 * @param ctx the parse tree
	 */
	void enterClassNamedInitParam(CangJieParser.ClassNamedInitParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#classNamedInitParam}.
	 * @param ctx the parse tree
	 */
	void exitClassNamedInitParam(CangJieParser.ClassNamedInitParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#classNonStaticMemberModifier}.
	 * @param ctx the parse tree
	 */
	void enterClassNonStaticMemberModifier(CangJieParser.ClassNonStaticMemberModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#classNonStaticMemberModifier}.
	 * @param ctx the parse tree
	 */
	void exitClassNonStaticMemberModifier(CangJieParser.ClassNonStaticMemberModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#interfaceDefinition}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceDefinition(CangJieParser.InterfaceDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#interfaceDefinition}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceDefinition(CangJieParser.InterfaceDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceBody(CangJieParser.InterfaceBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceBody(CangJieParser.InterfaceBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMemberDeclaration(CangJieParser.InterfaceMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMemberDeclaration(CangJieParser.InterfaceMemberDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#interfaceModifierList}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceModifierList(CangJieParser.InterfaceModifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#interfaceModifierList}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceModifierList(CangJieParser.InterfaceModifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#interfaceModifier}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceModifier(CangJieParser.InterfaceModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#interfaceModifier}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceModifier(CangJieParser.InterfaceModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#functionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDefinition(CangJieParser.FunctionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#functionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDefinition(CangJieParser.FunctionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#operatorFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterOperatorFunctionDefinition(CangJieParser.OperatorFunctionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#operatorFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitOperatorFunctionDefinition(CangJieParser.OperatorFunctionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#functionParameters}.
	 * @param ctx the parse tree
	 */
	void enterFunctionParameters(CangJieParser.FunctionParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#functionParameters}.
	 * @param ctx the parse tree
	 */
	void exitFunctionParameters(CangJieParser.FunctionParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#nondefaultParameterList}.
	 * @param ctx the parse tree
	 */
	void enterNondefaultParameterList(CangJieParser.NondefaultParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#nondefaultParameterList}.
	 * @param ctx the parse tree
	 */
	void exitNondefaultParameterList(CangJieParser.NondefaultParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#unnamedParameterList}.
	 * @param ctx the parse tree
	 */
	void enterUnnamedParameterList(CangJieParser.UnnamedParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#unnamedParameterList}.
	 * @param ctx the parse tree
	 */
	void exitUnnamedParameterList(CangJieParser.UnnamedParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#unnamedParameter}.
	 * @param ctx the parse tree
	 */
	void enterUnnamedParameter(CangJieParser.UnnamedParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#unnamedParameter}.
	 * @param ctx the parse tree
	 */
	void exitUnnamedParameter(CangJieParser.UnnamedParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#namedParameterList}.
	 * @param ctx the parse tree
	 */
	void enterNamedParameterList(CangJieParser.NamedParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#namedParameterList}.
	 * @param ctx the parse tree
	 */
	void exitNamedParameterList(CangJieParser.NamedParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#namedParameter}.
	 * @param ctx the parse tree
	 */
	void enterNamedParameter(CangJieParser.NamedParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#namedParameter}.
	 * @param ctx the parse tree
	 */
	void exitNamedParameter(CangJieParser.NamedParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#defaultParameter}.
	 * @param ctx the parse tree
	 */
	void enterDefaultParameter(CangJieParser.DefaultParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#defaultParameter}.
	 * @param ctx the parse tree
	 */
	void exitDefaultParameter(CangJieParser.DefaultParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#functionModifierList}.
	 * @param ctx the parse tree
	 */
	void enterFunctionModifierList(CangJieParser.FunctionModifierListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#functionModifierList}.
	 * @param ctx the parse tree
	 */
	void exitFunctionModifierList(CangJieParser.FunctionModifierListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#functionModifier}.
	 * @param ctx the parse tree
	 */
	void enterFunctionModifier(CangJieParser.FunctionModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#functionModifier}.
	 * @param ctx the parse tree
	 */
	void exitFunctionModifier(CangJieParser.FunctionModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#variableDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclaration(CangJieParser.VariableDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#variableDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclaration(CangJieParser.VariableDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void enterVariableModifier(CangJieParser.VariableModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void exitVariableModifier(CangJieParser.VariableModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#enumDefinition}.
	 * @param ctx the parse tree
	 */
	void enterEnumDefinition(CangJieParser.EnumDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#enumDefinition}.
	 * @param ctx the parse tree
	 */
	void exitEnumDefinition(CangJieParser.EnumDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#enumBody}.
	 * @param ctx the parse tree
	 */
	void enterEnumBody(CangJieParser.EnumBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#enumBody}.
	 * @param ctx the parse tree
	 */
	void exitEnumBody(CangJieParser.EnumBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#caseBody}.
	 * @param ctx the parse tree
	 */
	void enterCaseBody(CangJieParser.CaseBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#caseBody}.
	 * @param ctx the parse tree
	 */
	void exitCaseBody(CangJieParser.CaseBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#enumModifier}.
	 * @param ctx the parse tree
	 */
	void enterEnumModifier(CangJieParser.EnumModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#enumModifier}.
	 * @param ctx the parse tree
	 */
	void exitEnumModifier(CangJieParser.EnumModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#structDefinition}.
	 * @param ctx the parse tree
	 */
	void enterStructDefinition(CangJieParser.StructDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#structDefinition}.
	 * @param ctx the parse tree
	 */
	void exitStructDefinition(CangJieParser.StructDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#structBody}.
	 * @param ctx the parse tree
	 */
	void enterStructBody(CangJieParser.StructBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#structBody}.
	 * @param ctx the parse tree
	 */
	void exitStructBody(CangJieParser.StructBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#structMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterStructMemberDeclaration(CangJieParser.StructMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#structMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitStructMemberDeclaration(CangJieParser.StructMemberDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#structInit}.
	 * @param ctx the parse tree
	 */
	void enterStructInit(CangJieParser.StructInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#structInit}.
	 * @param ctx the parse tree
	 */
	void exitStructInit(CangJieParser.StructInitContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#structPrimaryInit}.
	 * @param ctx the parse tree
	 */
	void enterStructPrimaryInit(CangJieParser.StructPrimaryInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#structPrimaryInit}.
	 * @param ctx the parse tree
	 */
	void exitStructPrimaryInit(CangJieParser.StructPrimaryInitContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#structName}.
	 * @param ctx the parse tree
	 */
	void enterStructName(CangJieParser.StructNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#structName}.
	 * @param ctx the parse tree
	 */
	void exitStructName(CangJieParser.StructNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#structPrimaryInitParamLists}.
	 * @param ctx the parse tree
	 */
	void enterStructPrimaryInitParamLists(CangJieParser.StructPrimaryInitParamListsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#structPrimaryInitParamLists}.
	 * @param ctx the parse tree
	 */
	void exitStructPrimaryInitParamLists(CangJieParser.StructPrimaryInitParamListsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#structUnnamedInitParamList}.
	 * @param ctx the parse tree
	 */
	void enterStructUnnamedInitParamList(CangJieParser.StructUnnamedInitParamListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#structUnnamedInitParamList}.
	 * @param ctx the parse tree
	 */
	void exitStructUnnamedInitParamList(CangJieParser.StructUnnamedInitParamListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#structNamedInitParamList}.
	 * @param ctx the parse tree
	 */
	void enterStructNamedInitParamList(CangJieParser.StructNamedInitParamListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#structNamedInitParamList}.
	 * @param ctx the parse tree
	 */
	void exitStructNamedInitParamList(CangJieParser.StructNamedInitParamListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#structUnnamedInitParam}.
	 * @param ctx the parse tree
	 */
	void enterStructUnnamedInitParam(CangJieParser.StructUnnamedInitParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#structUnnamedInitParam}.
	 * @param ctx the parse tree
	 */
	void exitStructUnnamedInitParam(CangJieParser.StructUnnamedInitParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#structNamedInitParam}.
	 * @param ctx the parse tree
	 */
	void enterStructNamedInitParam(CangJieParser.StructNamedInitParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#structNamedInitParam}.
	 * @param ctx the parse tree
	 */
	void exitStructNamedInitParam(CangJieParser.StructNamedInitParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#structModifier}.
	 * @param ctx the parse tree
	 */
	void enterStructModifier(CangJieParser.StructModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#structModifier}.
	 * @param ctx the parse tree
	 */
	void exitStructModifier(CangJieParser.StructModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#structNonStaticMemberModifier}.
	 * @param ctx the parse tree
	 */
	void enterStructNonStaticMemberModifier(CangJieParser.StructNonStaticMemberModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#structNonStaticMemberModifier}.
	 * @param ctx the parse tree
	 */
	void exitStructNonStaticMemberModifier(CangJieParser.StructNonStaticMemberModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#typeAlias}.
	 * @param ctx the parse tree
	 */
	void enterTypeAlias(CangJieParser.TypeAliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#typeAlias}.
	 * @param ctx the parse tree
	 */
	void exitTypeAlias(CangJieParser.TypeAliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#typeModifier}.
	 * @param ctx the parse tree
	 */
	void enterTypeModifier(CangJieParser.TypeModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#typeModifier}.
	 * @param ctx the parse tree
	 */
	void exitTypeModifier(CangJieParser.TypeModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#extendDefinition}.
	 * @param ctx the parse tree
	 */
	void enterExtendDefinition(CangJieParser.ExtendDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#extendDefinition}.
	 * @param ctx the parse tree
	 */
	void exitExtendDefinition(CangJieParser.ExtendDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#extendType}.
	 * @param ctx the parse tree
	 */
	void enterExtendType(CangJieParser.ExtendTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#extendType}.
	 * @param ctx the parse tree
	 */
	void exitExtendType(CangJieParser.ExtendTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#extendBody}.
	 * @param ctx the parse tree
	 */
	void enterExtendBody(CangJieParser.ExtendBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#extendBody}.
	 * @param ctx the parse tree
	 */
	void exitExtendBody(CangJieParser.ExtendBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#extendMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterExtendMemberDeclaration(CangJieParser.ExtendMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#extendMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitExtendMemberDeclaration(CangJieParser.ExtendMemberDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#foreignDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterForeignDeclaration(CangJieParser.ForeignDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#foreignDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitForeignDeclaration(CangJieParser.ForeignDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#foreignBody}.
	 * @param ctx the parse tree
	 */
	void enterForeignBody(CangJieParser.ForeignBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#foreignBody}.
	 * @param ctx the parse tree
	 */
	void exitForeignBody(CangJieParser.ForeignBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#foreignMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterForeignMemberDeclaration(CangJieParser.ForeignMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#foreignMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitForeignMemberDeclaration(CangJieParser.ForeignMemberDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#annotationList}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationList(CangJieParser.AnnotationListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#annotationList}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationList(CangJieParser.AnnotationListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#annotation}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation(CangJieParser.AnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#annotation}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation(CangJieParser.AnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#annotationArgumentList}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationArgumentList(CangJieParser.AnnotationArgumentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#annotationArgumentList}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationArgumentList(CangJieParser.AnnotationArgumentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#annotationArgument}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationArgument(CangJieParser.AnnotationArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#annotationArgument}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationArgument(CangJieParser.AnnotationArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#macroDefinition}.
	 * @param ctx the parse tree
	 */
	void enterMacroDefinition(CangJieParser.MacroDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#macroDefinition}.
	 * @param ctx the parse tree
	 */
	void exitMacroDefinition(CangJieParser.MacroDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#macroWithoutAttrParam}.
	 * @param ctx the parse tree
	 */
	void enterMacroWithoutAttrParam(CangJieParser.MacroWithoutAttrParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#macroWithoutAttrParam}.
	 * @param ctx the parse tree
	 */
	void exitMacroWithoutAttrParam(CangJieParser.MacroWithoutAttrParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#macroWithAttrParam}.
	 * @param ctx the parse tree
	 */
	void enterMacroWithAttrParam(CangJieParser.MacroWithAttrParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#macroWithAttrParam}.
	 * @param ctx the parse tree
	 */
	void exitMacroWithAttrParam(CangJieParser.MacroWithAttrParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#macroInputDecl}.
	 * @param ctx the parse tree
	 */
	void enterMacroInputDecl(CangJieParser.MacroInputDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#macroInputDecl}.
	 * @param ctx the parse tree
	 */
	void exitMacroInputDecl(CangJieParser.MacroInputDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#macroAttrDecl}.
	 * @param ctx the parse tree
	 */
	void enterMacroAttrDecl(CangJieParser.MacroAttrDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#macroAttrDecl}.
	 * @param ctx the parse tree
	 */
	void exitMacroAttrDecl(CangJieParser.MacroAttrDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#propertyDefinition}.
	 * @param ctx the parse tree
	 */
	void enterPropertyDefinition(CangJieParser.PropertyDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#propertyDefinition}.
	 * @param ctx the parse tree
	 */
	void exitPropertyDefinition(CangJieParser.PropertyDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#propertyBody}.
	 * @param ctx the parse tree
	 */
	void enterPropertyBody(CangJieParser.PropertyBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#propertyBody}.
	 * @param ctx the parse tree
	 */
	void exitPropertyBody(CangJieParser.PropertyBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#propertyMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterPropertyMemberDeclaration(CangJieParser.PropertyMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#propertyMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitPropertyMemberDeclaration(CangJieParser.PropertyMemberDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#propertyModifier}.
	 * @param ctx the parse tree
	 */
	void enterPropertyModifier(CangJieParser.PropertyModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#propertyModifier}.
	 * @param ctx the parse tree
	 */
	void exitPropertyModifier(CangJieParser.PropertyModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#mainDefinition}.
	 * @param ctx the parse tree
	 */
	void enterMainDefinition(CangJieParser.MainDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#mainDefinition}.
	 * @param ctx the parse tree
	 */
	void exitMainDefinition(CangJieParser.MainDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(CangJieParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(CangJieParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#arrowType}.
	 * @param ctx the parse tree
	 */
	void enterArrowType(CangJieParser.ArrowTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#arrowType}.
	 * @param ctx the parse tree
	 */
	void exitArrowType(CangJieParser.ArrowTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#arrowParameters}.
	 * @param ctx the parse tree
	 */
	void enterArrowParameters(CangJieParser.ArrowParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#arrowParameters}.
	 * @param ctx the parse tree
	 */
	void exitArrowParameters(CangJieParser.ArrowParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#tupleType}.
	 * @param ctx the parse tree
	 */
	void enterTupleType(CangJieParser.TupleTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#tupleType}.
	 * @param ctx the parse tree
	 */
	void exitTupleType(CangJieParser.TupleTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#prefixType}.
	 * @param ctx the parse tree
	 */
	void enterPrefixType(CangJieParser.PrefixTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#prefixType}.
	 * @param ctx the parse tree
	 */
	void exitPrefixType(CangJieParser.PrefixTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#prefixTypeOperator}.
	 * @param ctx the parse tree
	 */
	void enterPrefixTypeOperator(CangJieParser.PrefixTypeOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#prefixTypeOperator}.
	 * @param ctx the parse tree
	 */
	void exitPrefixTypeOperator(CangJieParser.PrefixTypeOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#atomicType}.
	 * @param ctx the parse tree
	 */
	void enterAtomicType(CangJieParser.AtomicTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#atomicType}.
	 * @param ctx the parse tree
	 */
	void exitAtomicType(CangJieParser.AtomicTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#charLangTypes}.
	 * @param ctx the parse tree
	 */
	void enterCharLangTypes(CangJieParser.CharLangTypesContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#charLangTypes}.
	 * @param ctx the parse tree
	 */
	void exitCharLangTypes(CangJieParser.CharLangTypesContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#numericTypes}.
	 * @param ctx the parse tree
	 */
	void enterNumericTypes(CangJieParser.NumericTypesContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#numericTypes}.
	 * @param ctx the parse tree
	 */
	void exitNumericTypes(CangJieParser.NumericTypesContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#userType}.
	 * @param ctx the parse tree
	 */
	void enterUserType(CangJieParser.UserTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#userType}.
	 * @param ctx the parse tree
	 */
	void exitUserType(CangJieParser.UserTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#parenthesizedType}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedType(CangJieParser.ParenthesizedTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#parenthesizedType}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedType(CangJieParser.ParenthesizedTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(CangJieParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(CangJieParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#assignmentExpression}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentExpression(CangJieParser.AssignmentExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#assignmentExpression}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentExpression(CangJieParser.AssignmentExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#tupleLeftValueExpression}.
	 * @param ctx the parse tree
	 */
	void enterTupleLeftValueExpression(CangJieParser.TupleLeftValueExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#tupleLeftValueExpression}.
	 * @param ctx the parse tree
	 */
	void exitTupleLeftValueExpression(CangJieParser.TupleLeftValueExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#leftValueExpression}.
	 * @param ctx the parse tree
	 */
	void enterLeftValueExpression(CangJieParser.LeftValueExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#leftValueExpression}.
	 * @param ctx the parse tree
	 */
	void exitLeftValueExpression(CangJieParser.LeftValueExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#leftValueExpressionWithoutWildCard}.
	 * @param ctx the parse tree
	 */
	void enterLeftValueExpressionWithoutWildCard(CangJieParser.LeftValueExpressionWithoutWildCardContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#leftValueExpressionWithoutWildCard}.
	 * @param ctx the parse tree
	 */
	void exitLeftValueExpressionWithoutWildCard(CangJieParser.LeftValueExpressionWithoutWildCardContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#leftAuxExpression}.
	 * @param ctx the parse tree
	 */
	void enterLeftAuxExpression(CangJieParser.LeftAuxExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#leftAuxExpression}.
	 * @param ctx the parse tree
	 */
	void exitLeftAuxExpression(CangJieParser.LeftAuxExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#assignableSuffix}.
	 * @param ctx the parse tree
	 */
	void enterAssignableSuffix(CangJieParser.AssignableSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#assignableSuffix}.
	 * @param ctx the parse tree
	 */
	void exitAssignableSuffix(CangJieParser.AssignableSuffixContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#fieldAccess}.
	 * @param ctx the parse tree
	 */
	void enterFieldAccess(CangJieParser.FieldAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#fieldAccess}.
	 * @param ctx the parse tree
	 */
	void exitFieldAccess(CangJieParser.FieldAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#flowExpression}.
	 * @param ctx the parse tree
	 */
	void enterFlowExpression(CangJieParser.FlowExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#flowExpression}.
	 * @param ctx the parse tree
	 */
	void exitFlowExpression(CangJieParser.FlowExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#coalescingExpression}.
	 * @param ctx the parse tree
	 */
	void enterCoalescingExpression(CangJieParser.CoalescingExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#coalescingExpression}.
	 * @param ctx the parse tree
	 */
	void exitCoalescingExpression(CangJieParser.CoalescingExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#logicDisjunctionExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicDisjunctionExpression(CangJieParser.LogicDisjunctionExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#logicDisjunctionExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicDisjunctionExpression(CangJieParser.LogicDisjunctionExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#logicConjunctionExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicConjunctionExpression(CangJieParser.LogicConjunctionExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#logicConjunctionExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicConjunctionExpression(CangJieParser.LogicConjunctionExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#rangeExpression}.
	 * @param ctx the parse tree
	 */
	void enterRangeExpression(CangJieParser.RangeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#rangeExpression}.
	 * @param ctx the parse tree
	 */
	void exitRangeExpression(CangJieParser.RangeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#bitwiseDisjunctionExpression}.
	 * @param ctx the parse tree
	 */
	void enterBitwiseDisjunctionExpression(CangJieParser.BitwiseDisjunctionExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#bitwiseDisjunctionExpression}.
	 * @param ctx the parse tree
	 */
	void exitBitwiseDisjunctionExpression(CangJieParser.BitwiseDisjunctionExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#bitwiseXorExpression}.
	 * @param ctx the parse tree
	 */
	void enterBitwiseXorExpression(CangJieParser.BitwiseXorExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#bitwiseXorExpression}.
	 * @param ctx the parse tree
	 */
	void exitBitwiseXorExpression(CangJieParser.BitwiseXorExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#bitwiseConjunctionExpression}.
	 * @param ctx the parse tree
	 */
	void enterBitwiseConjunctionExpression(CangJieParser.BitwiseConjunctionExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#bitwiseConjunctionExpression}.
	 * @param ctx the parse tree
	 */
	void exitBitwiseConjunctionExpression(CangJieParser.BitwiseConjunctionExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#equalityComparisonExpression}.
	 * @param ctx the parse tree
	 */
	void enterEqualityComparisonExpression(CangJieParser.EqualityComparisonExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#equalityComparisonExpression}.
	 * @param ctx the parse tree
	 */
	void exitEqualityComparisonExpression(CangJieParser.EqualityComparisonExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#comparisonOrTypeExpression}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOrTypeExpression(CangJieParser.ComparisonOrTypeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#comparisonOrTypeExpression}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOrTypeExpression(CangJieParser.ComparisonOrTypeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#shiftingExpression}.
	 * @param ctx the parse tree
	 */
	void enterShiftingExpression(CangJieParser.ShiftingExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#shiftingExpression}.
	 * @param ctx the parse tree
	 */
	void exitShiftingExpression(CangJieParser.ShiftingExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpression(CangJieParser.AdditiveExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpression(CangJieParser.AdditiveExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicativeExpression(CangJieParser.MultiplicativeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicativeExpression(CangJieParser.MultiplicativeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#exponentExpression}.
	 * @param ctx the parse tree
	 */
	void enterExponentExpression(CangJieParser.ExponentExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#exponentExpression}.
	 * @param ctx the parse tree
	 */
	void exitExponentExpression(CangJieParser.ExponentExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#prefixUnaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPrefixUnaryExpression(CangJieParser.PrefixUnaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#prefixUnaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPrefixUnaryExpression(CangJieParser.PrefixUnaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#incAndDecExpression}.
	 * @param ctx the parse tree
	 */
	void enterIncAndDecExpression(CangJieParser.IncAndDecExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#incAndDecExpression}.
	 * @param ctx the parse tree
	 */
	void exitIncAndDecExpression(CangJieParser.IncAndDecExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void enterPostfixExpression(CangJieParser.PostfixExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void exitPostfixExpression(CangJieParser.PostfixExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#questSeperatedItems}.
	 * @param ctx the parse tree
	 */
	void enterQuestSeperatedItems(CangJieParser.QuestSeperatedItemsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#questSeperatedItems}.
	 * @param ctx the parse tree
	 */
	void exitQuestSeperatedItems(CangJieParser.QuestSeperatedItemsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#questSeperatedItem}.
	 * @param ctx the parse tree
	 */
	void enterQuestSeperatedItem(CangJieParser.QuestSeperatedItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#questSeperatedItem}.
	 * @param ctx the parse tree
	 */
	void exitQuestSeperatedItem(CangJieParser.QuestSeperatedItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#itemAfterQuest}.
	 * @param ctx the parse tree
	 */
	void enterItemAfterQuest(CangJieParser.ItemAfterQuestContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#itemAfterQuest}.
	 * @param ctx the parse tree
	 */
	void exitItemAfterQuest(CangJieParser.ItemAfterQuestContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#callSuffix}.
	 * @param ctx the parse tree
	 */
	void enterCallSuffix(CangJieParser.CallSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#callSuffix}.
	 * @param ctx the parse tree
	 */
	void exitCallSuffix(CangJieParser.CallSuffixContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#valueArgument}.
	 * @param ctx the parse tree
	 */
	void enterValueArgument(CangJieParser.ValueArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#valueArgument}.
	 * @param ctx the parse tree
	 */
	void exitValueArgument(CangJieParser.ValueArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#refTransferExpression}.
	 * @param ctx the parse tree
	 */
	void enterRefTransferExpression(CangJieParser.RefTransferExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#refTransferExpression}.
	 * @param ctx the parse tree
	 */
	void exitRefTransferExpression(CangJieParser.RefTransferExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#indexAccess}.
	 * @param ctx the parse tree
	 */
	void enterIndexAccess(CangJieParser.IndexAccessContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#indexAccess}.
	 * @param ctx the parse tree
	 */
	void exitIndexAccess(CangJieParser.IndexAccessContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#rangeElement}.
	 * @param ctx the parse tree
	 */
	void enterRangeElement(CangJieParser.RangeElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#rangeElement}.
	 * @param ctx the parse tree
	 */
	void exitRangeElement(CangJieParser.RangeElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#atomicExpression}.
	 * @param ctx the parse tree
	 */
	void enterAtomicExpression(CangJieParser.AtomicExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#atomicExpression}.
	 * @param ctx the parse tree
	 */
	void exitAtomicExpression(CangJieParser.AtomicExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#literalConstant}.
	 * @param ctx the parse tree
	 */
	void enterLiteralConstant(CangJieParser.LiteralConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#literalConstant}.
	 * @param ctx the parse tree
	 */
	void exitLiteralConstant(CangJieParser.LiteralConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#booleanLiteral}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(CangJieParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#booleanLiteral}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(CangJieParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(CangJieParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(CangJieParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#lineStringContent}.
	 * @param ctx the parse tree
	 */
	void enterLineStringContent(CangJieParser.LineStringContentContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#lineStringContent}.
	 * @param ctx the parse tree
	 */
	void exitLineStringContent(CangJieParser.LineStringContentContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#lineStringLiteral}.
	 * @param ctx the parse tree
	 */
	void enterLineStringLiteral(CangJieParser.LineStringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#lineStringLiteral}.
	 * @param ctx the parse tree
	 */
	void exitLineStringLiteral(CangJieParser.LineStringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#lineStringExpression}.
	 * @param ctx the parse tree
	 */
	void enterLineStringExpression(CangJieParser.LineStringExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#lineStringExpression}.
	 * @param ctx the parse tree
	 */
	void exitLineStringExpression(CangJieParser.LineStringExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#multiLineStringContent}.
	 * @param ctx the parse tree
	 */
	void enterMultiLineStringContent(CangJieParser.MultiLineStringContentContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#multiLineStringContent}.
	 * @param ctx the parse tree
	 */
	void exitMultiLineStringContent(CangJieParser.MultiLineStringContentContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#multiLineStringLiteral}.
	 * @param ctx the parse tree
	 */
	void enterMultiLineStringLiteral(CangJieParser.MultiLineStringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#multiLineStringLiteral}.
	 * @param ctx the parse tree
	 */
	void exitMultiLineStringLiteral(CangJieParser.MultiLineStringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#multiLineStringExpression}.
	 * @param ctx the parse tree
	 */
	void enterMultiLineStringExpression(CangJieParser.MultiLineStringExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#multiLineStringExpression}.
	 * @param ctx the parse tree
	 */
	void exitMultiLineStringExpression(CangJieParser.MultiLineStringExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#collectionLiteral}.
	 * @param ctx the parse tree
	 */
	void enterCollectionLiteral(CangJieParser.CollectionLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#collectionLiteral}.
	 * @param ctx the parse tree
	 */
	void exitCollectionLiteral(CangJieParser.CollectionLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#arrayLiteral}.
	 * @param ctx the parse tree
	 */
	void enterArrayLiteral(CangJieParser.ArrayLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#arrayLiteral}.
	 * @param ctx the parse tree
	 */
	void exitArrayLiteral(CangJieParser.ArrayLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#elements}.
	 * @param ctx the parse tree
	 */
	void enterElements(CangJieParser.ElementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#elements}.
	 * @param ctx the parse tree
	 */
	void exitElements(CangJieParser.ElementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#element}.
	 * @param ctx the parse tree
	 */
	void enterElement(CangJieParser.ElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#element}.
	 * @param ctx the parse tree
	 */
	void exitElement(CangJieParser.ElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#expressionElement}.
	 * @param ctx the parse tree
	 */
	void enterExpressionElement(CangJieParser.ExpressionElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#expressionElement}.
	 * @param ctx the parse tree
	 */
	void exitExpressionElement(CangJieParser.ExpressionElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#spreadElement}.
	 * @param ctx the parse tree
	 */
	void enterSpreadElement(CangJieParser.SpreadElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#spreadElement}.
	 * @param ctx the parse tree
	 */
	void exitSpreadElement(CangJieParser.SpreadElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#tupleLiteral}.
	 * @param ctx the parse tree
	 */
	void enterTupleLiteral(CangJieParser.TupleLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#tupleLiteral}.
	 * @param ctx the parse tree
	 */
	void exitTupleLiteral(CangJieParser.TupleLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#unitLiteral}.
	 * @param ctx the parse tree
	 */
	void enterUnitLiteral(CangJieParser.UnitLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#unitLiteral}.
	 * @param ctx the parse tree
	 */
	void exitUnitLiteral(CangJieParser.UnitLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#ifExpression}.
	 * @param ctx the parse tree
	 */
	void enterIfExpression(CangJieParser.IfExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#ifExpression}.
	 * @param ctx the parse tree
	 */
	void exitIfExpression(CangJieParser.IfExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#deconstructPattern}.
	 * @param ctx the parse tree
	 */
	void enterDeconstructPattern(CangJieParser.DeconstructPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#deconstructPattern}.
	 * @param ctx the parse tree
	 */
	void exitDeconstructPattern(CangJieParser.DeconstructPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#matchExpression}.
	 * @param ctx the parse tree
	 */
	void enterMatchExpression(CangJieParser.MatchExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#matchExpression}.
	 * @param ctx the parse tree
	 */
	void exitMatchExpression(CangJieParser.MatchExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#matchCase}.
	 * @param ctx the parse tree
	 */
	void enterMatchCase(CangJieParser.MatchCaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#matchCase}.
	 * @param ctx the parse tree
	 */
	void exitMatchCase(CangJieParser.MatchCaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#patternGuard}.
	 * @param ctx the parse tree
	 */
	void enterPatternGuard(CangJieParser.PatternGuardContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#patternGuard}.
	 * @param ctx the parse tree
	 */
	void exitPatternGuard(CangJieParser.PatternGuardContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#pattern}.
	 * @param ctx the parse tree
	 */
	void enterPattern(CangJieParser.PatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#pattern}.
	 * @param ctx the parse tree
	 */
	void exitPattern(CangJieParser.PatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#constantPattern}.
	 * @param ctx the parse tree
	 */
	void enterConstantPattern(CangJieParser.ConstantPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#constantPattern}.
	 * @param ctx the parse tree
	 */
	void exitConstantPattern(CangJieParser.ConstantPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#wildcardPattern}.
	 * @param ctx the parse tree
	 */
	void enterWildcardPattern(CangJieParser.WildcardPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#wildcardPattern}.
	 * @param ctx the parse tree
	 */
	void exitWildcardPattern(CangJieParser.WildcardPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#varBindingPattern}.
	 * @param ctx the parse tree
	 */
	void enterVarBindingPattern(CangJieParser.VarBindingPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#varBindingPattern}.
	 * @param ctx the parse tree
	 */
	void exitVarBindingPattern(CangJieParser.VarBindingPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#tuplePattern}.
	 * @param ctx the parse tree
	 */
	void enterTuplePattern(CangJieParser.TuplePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#tuplePattern}.
	 * @param ctx the parse tree
	 */
	void exitTuplePattern(CangJieParser.TuplePatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#typePattern}.
	 * @param ctx the parse tree
	 */
	void enterTypePattern(CangJieParser.TypePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#typePattern}.
	 * @param ctx the parse tree
	 */
	void exitTypePattern(CangJieParser.TypePatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#enumPattern}.
	 * @param ctx the parse tree
	 */
	void enterEnumPattern(CangJieParser.EnumPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#enumPattern}.
	 * @param ctx the parse tree
	 */
	void exitEnumPattern(CangJieParser.EnumPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#enumPatternParameters}.
	 * @param ctx the parse tree
	 */
	void enterEnumPatternParameters(CangJieParser.EnumPatternParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#enumPatternParameters}.
	 * @param ctx the parse tree
	 */
	void exitEnumPatternParameters(CangJieParser.EnumPatternParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#loopExpression}.
	 * @param ctx the parse tree
	 */
	void enterLoopExpression(CangJieParser.LoopExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#loopExpression}.
	 * @param ctx the parse tree
	 */
	void exitLoopExpression(CangJieParser.LoopExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#forInExpression}.
	 * @param ctx the parse tree
	 */
	void enterForInExpression(CangJieParser.ForInExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#forInExpression}.
	 * @param ctx the parse tree
	 */
	void exitForInExpression(CangJieParser.ForInExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#patternsMaybeIrrefutable}.
	 * @param ctx the parse tree
	 */
	void enterPatternsMaybeIrrefutable(CangJieParser.PatternsMaybeIrrefutableContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#patternsMaybeIrrefutable}.
	 * @param ctx the parse tree
	 */
	void exitPatternsMaybeIrrefutable(CangJieParser.PatternsMaybeIrrefutableContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#whileExpression}.
	 * @param ctx the parse tree
	 */
	void enterWhileExpression(CangJieParser.WhileExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#whileExpression}.
	 * @param ctx the parse tree
	 */
	void exitWhileExpression(CangJieParser.WhileExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#doWhileExpression}.
	 * @param ctx the parse tree
	 */
	void enterDoWhileExpression(CangJieParser.DoWhileExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#doWhileExpression}.
	 * @param ctx the parse tree
	 */
	void exitDoWhileExpression(CangJieParser.DoWhileExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#tryExpression}.
	 * @param ctx the parse tree
	 */
	void enterTryExpression(CangJieParser.TryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#tryExpression}.
	 * @param ctx the parse tree
	 */
	void exitTryExpression(CangJieParser.TryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#catchPattern}.
	 * @param ctx the parse tree
	 */
	void enterCatchPattern(CangJieParser.CatchPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#catchPattern}.
	 * @param ctx the parse tree
	 */
	void exitCatchPattern(CangJieParser.CatchPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#exceptionTypePattern}.
	 * @param ctx the parse tree
	 */
	void enterExceptionTypePattern(CangJieParser.ExceptionTypePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#exceptionTypePattern}.
	 * @param ctx the parse tree
	 */
	void exitExceptionTypePattern(CangJieParser.ExceptionTypePatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#resourceSpecifications}.
	 * @param ctx the parse tree
	 */
	void enterResourceSpecifications(CangJieParser.ResourceSpecificationsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#resourceSpecifications}.
	 * @param ctx the parse tree
	 */
	void exitResourceSpecifications(CangJieParser.ResourceSpecificationsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void enterResourceSpecification(CangJieParser.ResourceSpecificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void exitResourceSpecification(CangJieParser.ResourceSpecificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#jumpExpression}.
	 * @param ctx the parse tree
	 */
	void enterJumpExpression(CangJieParser.JumpExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#jumpExpression}.
	 * @param ctx the parse tree
	 */
	void exitJumpExpression(CangJieParser.JumpExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#numericTypeConvExpr}.
	 * @param ctx the parse tree
	 */
	void enterNumericTypeConvExpr(CangJieParser.NumericTypeConvExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#numericTypeConvExpr}.
	 * @param ctx the parse tree
	 */
	void exitNumericTypeConvExpr(CangJieParser.NumericTypeConvExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#thisSuperExpression}.
	 * @param ctx the parse tree
	 */
	void enterThisSuperExpression(CangJieParser.ThisSuperExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#thisSuperExpression}.
	 * @param ctx the parse tree
	 */
	void exitThisSuperExpression(CangJieParser.ThisSuperExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#lambdaExpression}.
	 * @param ctx the parse tree
	 */
	void enterLambdaExpression(CangJieParser.LambdaExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#lambdaExpression}.
	 * @param ctx the parse tree
	 */
	void exitLambdaExpression(CangJieParser.LambdaExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#trailingLambdaExpression}.
	 * @param ctx the parse tree
	 */
	void enterTrailingLambdaExpression(CangJieParser.TrailingLambdaExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#trailingLambdaExpression}.
	 * @param ctx the parse tree
	 */
	void exitTrailingLambdaExpression(CangJieParser.TrailingLambdaExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#lambdaParameters}.
	 * @param ctx the parse tree
	 */
	void enterLambdaParameters(CangJieParser.LambdaParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#lambdaParameters}.
	 * @param ctx the parse tree
	 */
	void exitLambdaParameters(CangJieParser.LambdaParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#lambdaParameter}.
	 * @param ctx the parse tree
	 */
	void enterLambdaParameter(CangJieParser.LambdaParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#lambdaParameter}.
	 * @param ctx the parse tree
	 */
	void exitLambdaParameter(CangJieParser.LambdaParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#spawnExpression}.
	 * @param ctx the parse tree
	 */
	void enterSpawnExpression(CangJieParser.SpawnExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#spawnExpression}.
	 * @param ctx the parse tree
	 */
	void exitSpawnExpression(CangJieParser.SpawnExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#synchronizedExpression}.
	 * @param ctx the parse tree
	 */
	void enterSynchronizedExpression(CangJieParser.SynchronizedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#synchronizedExpression}.
	 * @param ctx the parse tree
	 */
	void exitSynchronizedExpression(CangJieParser.SynchronizedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#parenthesizedExpression}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedExpression(CangJieParser.ParenthesizedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#parenthesizedExpression}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedExpression(CangJieParser.ParenthesizedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(CangJieParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(CangJieParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#unsafeExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnsafeExpression(CangJieParser.UnsafeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#unsafeExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnsafeExpression(CangJieParser.UnsafeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#expressionOrDeclarations}.
	 * @param ctx the parse tree
	 */
	void enterExpressionOrDeclarations(CangJieParser.ExpressionOrDeclarationsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#expressionOrDeclarations}.
	 * @param ctx the parse tree
	 */
	void exitExpressionOrDeclarations(CangJieParser.ExpressionOrDeclarationsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#expressionOrDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterExpressionOrDeclaration(CangJieParser.ExpressionOrDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#expressionOrDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitExpressionOrDeclaration(CangJieParser.ExpressionOrDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#varOrfuncDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterVarOrfuncDeclaration(CangJieParser.VarOrfuncDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#varOrfuncDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitVarOrfuncDeclaration(CangJieParser.VarOrfuncDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#quoteExpression}.
	 * @param ctx the parse tree
	 */
	void enterQuoteExpression(CangJieParser.QuoteExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#quoteExpression}.
	 * @param ctx the parse tree
	 */
	void exitQuoteExpression(CangJieParser.QuoteExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#quoteExpr}.
	 * @param ctx the parse tree
	 */
	void enterQuoteExpr(CangJieParser.QuoteExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#quoteExpr}.
	 * @param ctx the parse tree
	 */
	void exitQuoteExpr(CangJieParser.QuoteExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#quoteParameters}.
	 * @param ctx the parse tree
	 */
	void enterQuoteParameters(CangJieParser.QuoteParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#quoteParameters}.
	 * @param ctx the parse tree
	 */
	void exitQuoteParameters(CangJieParser.QuoteParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#quoteToken}.
	 * @param ctx the parse tree
	 */
	void enterQuoteToken(CangJieParser.QuoteTokenContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#quoteToken}.
	 * @param ctx the parse tree
	 */
	void exitQuoteToken(CangJieParser.QuoteTokenContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#quoteInterpolate}.
	 * @param ctx the parse tree
	 */
	void enterQuoteInterpolate(CangJieParser.QuoteInterpolateContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#quoteInterpolate}.
	 * @param ctx the parse tree
	 */
	void exitQuoteInterpolate(CangJieParser.QuoteInterpolateContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#macroExpression}.
	 * @param ctx the parse tree
	 */
	void enterMacroExpression(CangJieParser.MacroExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#macroExpression}.
	 * @param ctx the parse tree
	 */
	void exitMacroExpression(CangJieParser.MacroExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#macroAttrExpr}.
	 * @param ctx the parse tree
	 */
	void enterMacroAttrExpr(CangJieParser.MacroAttrExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#macroAttrExpr}.
	 * @param ctx the parse tree
	 */
	void exitMacroAttrExpr(CangJieParser.MacroAttrExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#macroInputExprWithoutParens}.
	 * @param ctx the parse tree
	 */
	void enterMacroInputExprWithoutParens(CangJieParser.MacroInputExprWithoutParensContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#macroInputExprWithoutParens}.
	 * @param ctx the parse tree
	 */
	void exitMacroInputExprWithoutParens(CangJieParser.MacroInputExprWithoutParensContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#macroInputExprWithParens}.
	 * @param ctx the parse tree
	 */
	void enterMacroInputExprWithParens(CangJieParser.MacroInputExprWithParensContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#macroInputExprWithParens}.
	 * @param ctx the parse tree
	 */
	void exitMacroInputExprWithParens(CangJieParser.MacroInputExprWithParensContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#macroTokens}.
	 * @param ctx the parse tree
	 */
	void enterMacroTokens(CangJieParser.MacroTokensContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#macroTokens}.
	 * @param ctx the parse tree
	 */
	void exitMacroTokens(CangJieParser.MacroTokensContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#assignmentOperator}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentOperator(CangJieParser.AssignmentOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#assignmentOperator}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentOperator(CangJieParser.AssignmentOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#equalityOperator}.
	 * @param ctx the parse tree
	 */
	void enterEqualityOperator(CangJieParser.EqualityOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#equalityOperator}.
	 * @param ctx the parse tree
	 */
	void exitEqualityOperator(CangJieParser.EqualityOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOperator(CangJieParser.ComparisonOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOperator(CangJieParser.ComparisonOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#shiftingOperator}.
	 * @param ctx the parse tree
	 */
	void enterShiftingOperator(CangJieParser.ShiftingOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#shiftingOperator}.
	 * @param ctx the parse tree
	 */
	void exitShiftingOperator(CangJieParser.ShiftingOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#flowOperator}.
	 * @param ctx the parse tree
	 */
	void enterFlowOperator(CangJieParser.FlowOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#flowOperator}.
	 * @param ctx the parse tree
	 */
	void exitFlowOperator(CangJieParser.FlowOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#additiveOperator}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveOperator(CangJieParser.AdditiveOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#additiveOperator}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveOperator(CangJieParser.AdditiveOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#exponentOperator}.
	 * @param ctx the parse tree
	 */
	void enterExponentOperator(CangJieParser.ExponentOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#exponentOperator}.
	 * @param ctx the parse tree
	 */
	void exitExponentOperator(CangJieParser.ExponentOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#multiplicativeOperator}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicativeOperator(CangJieParser.MultiplicativeOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#multiplicativeOperator}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicativeOperator(CangJieParser.MultiplicativeOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#prefixUnaryOperator}.
	 * @param ctx the parse tree
	 */
	void enterPrefixUnaryOperator(CangJieParser.PrefixUnaryOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#prefixUnaryOperator}.
	 * @param ctx the parse tree
	 */
	void exitPrefixUnaryOperator(CangJieParser.PrefixUnaryOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CangJieParser#overloadedOperators}.
	 * @param ctx the parse tree
	 */
	void enterOverloadedOperators(CangJieParser.OverloadedOperatorsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CangJieParser#overloadedOperators}.
	 * @param ctx the parse tree
	 */
	void exitOverloadedOperators(CangJieParser.OverloadedOperatorsContext ctx);
}