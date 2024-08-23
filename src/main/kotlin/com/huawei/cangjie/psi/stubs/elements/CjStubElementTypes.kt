//package com.huawei.cangjie.psi.stubs.elements
//
//import com.huawei.cangjie.psi.*
//import com.intellij.psi.tree.TokenSet
//
//interface CjStubElementTypes {
//
//
//    val CLASS: CjClassElementType = CjClassElementType("CLASS")
//
//    @JvmField
//    val FUNCTION: CjFunctionElementType = CjFunctionElementType("FUNC")
//
//    @JvmField
//    val VARIABLE: CjVariableElementType = CjVariableElementType("VARIABLE")
//
//    @JvmField
//    val PROPERTY: CjPropertyElementType = CjPropertyElementType("PROPERTY")
//
//    @JvmField
//    val PROPERTY_ACCESSOR: CjPropertyAccessorElementType = CjPropertyAccessorElementType("PROPERTY_ACCESSOR")
//
//    //    CjBackingFieldElementType BACKING_FIELD = new CjBackingFieldElementType("BACKING_FIELD");
//    @JvmField
//    val TYPEALIAS: CjTypeAliasElementType = CjTypeAliasElementType("TYPEALIAS")
//
//    @JvmField
//    val ENUM_ENTRY: CjClassElementType = CjClassElementType("ENUM_ENTRY")
//
//    //    CjObjectElementType OBJECT_DECLARATION = new CjObjectElementType("OBJECT_DECLARATION");
//    @JvmField
//    val CLASS_INITIALIZER: CjPlaceHolderStubElementType<CjClassInitializer> = CjPlaceHolderStubElementType(
//        "CLASS_INITIALIZER",
//        CjClassInitializer::class.java
//    )
//
//    @JvmField
//    val SECONDARY_CONSTRUCTOR: CjSecondaryConstructorElementType =
//        CjSecondaryConstructorElementType("SECONDARY_CONSTRUCTOR")
//
//    @JvmField
//    val PRIMARY_CONSTRUCTOR: CjPrimaryConstructorElementType = CjPrimaryConstructorElementType("PRIMARY_CONSTRUCTOR")
//
//    @JvmField
//    val VALUE_PARAMETER: CjParameterElementType = CjParameterElementType("VALUE_PARAMETER")
//
//    @JvmField
//    val VALUE_PARAMETER_LIST: CjPlaceHolderStubElementType<CjParameterList> = CjPlaceHolderStubElementType(
//        "VALUE_PARAMETER_LIST",
//        CjParameterList::class.java
//    )
//
//    @JvmField
//    val TYPE_PARAMETER: CjTypeParameterElementType = CjTypeParameterElementType("TYPE_PARAMETER")
//
//    @JvmField
//    val TYPE_PARAMETER_LIST: CjPlaceHolderStubElementType<CjTypeParameterList> = CjPlaceHolderStubElementType(
//        "TYPE_PARAMETER_LIST",
//        CjTypeParameterList::class.java
//    )
//
//    @JvmField
//    val ANNOTATION_ENTRY: CjAnnotationEntryElementType = CjAnnotationEntryElementType("ANNOTATION_ENTRY")
//
//    @JvmField
//    val ANNOTATION: CjPlaceHolderStubElementType<CjAnnotation> = CjPlaceHolderStubElementType(
//        "ANNOTATION",
//        CjAnnotation::class.java
//    )
//
//
//    //    CjAnnotationUseSiteTargetElementType ANNOTATION_TARGET = new CjAnnotationUseSiteTargetElementType("ANNOTATION_TARGET");
//    @JvmField
//    val CLASS_BODY: CjPlaceHolderStubElementType<CjClassBody> = CjPlaceHolderStubElementType(
//        "CLASS_BODY",
//        CjClassBody::class.java
//    )
//
//    @JvmField
//    val IMPORT_LIST: CjPlaceHolderStubElementType<CjImportList> = CjPlaceHolderStubElementType(
//        "IMPORT_LIST",
//        CjImportList::class.java
//    )
//
//
//    //    CjPlaceHolderStubElementType<CjFileAnnotationList> FILE_ANNOTATION_LIST =
//    //            new CjPlaceHolderStubElementType<>("FILE_ANNOTATION_LIST", CjFileAnnotationList.class);
//    @JvmField
//    val IMPORT_DIRECTIVE: CjImportDirectiveElementType = CjImportDirectiveElementType("IMPORT_DIRECTIVE")
//
//    @JvmField
//    val IMPORT_ALIAS: CjImportAliasElementType = CjImportAliasElementType("IMPORT_ALIAS")
//
//    @JvmField
//    val PACKAGE_DIRECTIVE: CjPlaceHolderStubElementType<CjPackageDirective> = CjPlaceHolderStubElementType(
//        "PACKAGE_DIRECTIVE",
//        CjPackageDirective::class.java
//    )
//
//    @JvmField
//    val MODIFIER_LIST: CjModifierListElementType<CjDeclarationModifierList> = CjModifierListElementType(
//        "MODIFIER_LIST",
//        CjDeclarationModifierList::class.java
//    )
//
//    @JvmField
//    val TYPE_CONSTRAINT_LIST: CjPlaceHolderStubElementType<CjTypeConstraintList> = CjPlaceHolderStubElementType(
//        "TYPE_CONSTRAINT_LIST",
//        CjTypeConstraintList::class.java
//    )
//
//    @JvmField
//    val TYPE_CONSTRAINT: CjPlaceHolderStubElementType<CjTypeConstraint> = CjPlaceHolderStubElementType(
//        "TYPE_CONSTRAINT",
//        CjTypeConstraint::class.java
//    )
//
//
//    //    CjPlaceHolderStubElementType<CjNullableType> NULLABLE_TYPE =
//    //            new CjPlaceHolderStubElementType<>("NULLABLE_TYPE", CjNullableType.class);
//    //    CjPlaceHolderStubElementType<CjIntersectionType> INTERSECTION_TYPE =
//    //            new CjPlaceHolderStubElementType<>("INTERSECTION_TYPE", CjIntersectionType.class);
//    @JvmField
//    val TYPE_REFERENCE: CjPlaceHolderStubElementType<CjTypeReference> = CjPlaceHolderStubElementType(
//        "TYPE_REFERENCE",
//        CjTypeReference::class.java
//    )
//
//    @JvmField
//    val USER_TYPE: CjUserTypeElementType = CjUserTypeElementType("USER_TYPE")
//
//
//    //    CjPlaceHolderStubElementType<CjDynamicType> DYNAMIC_TYPE =
//    //            new CjPlaceHolderStubElementType<>("DYNAMIC_TYPE", CjDynamicType.class);
//    @JvmField
//    val FUNCTION_TYPE: CjPlaceHolderStubElementType<CjFunctionType> = CjPlaceHolderStubElementType(
//        "FUNCTION_TYPE",
//        CjFunctionType::class.java
//    )
//
////    @JvmField
//@JvmStatic
//    val TYPE_CODE_FRAGMENT: CjTypeCodeFragmentType = CjTypeCodeFragmentType()
//
////    @JvmField
//@JvmStatic
//    val EXPRESSION_CODE_FRAGMENT: CjExpressionCodeFragmentType = CjExpressionCodeFragmentType()
//
////    @JvmField
//    @JvmStatic
//    val BLOCK_CODE_FRAGMENT: CjBlockCodeFragmentType = CjBlockCodeFragmentType()
//
//    @JvmField
//    val TYPE_PROJECTION: CjTypeProjectionElementType = CjTypeProjectionElementType("TYPE_PROJECTION")
//
//    @JvmField
//    val FUNCTION_TYPE_RECEIVER: CjPlaceHolderStubElementType<CjFunctionTypeReceiver> = CjPlaceHolderStubElementType(
//        "FUNCTION_TYPE_RECEIVER",
//        CjFunctionTypeReceiver::class.java
//    )
//
//    @JvmField
//    val REFERENCE_EXPRESSION: CjNameReferenceExpressionElementType =
//        CjNameReferenceExpressionElementType("REFERENCE_EXPRESSION")
//
//    @JvmField
//    val DOT_QUALIFIED_EXPRESSION: CjDotQualifiedExpressionElementType =
//        CjDotQualifiedExpressionElementType("DOT_QUALIFIED_EXPRESSION")
//
//    //    CjEnumEntrySuperClassReferenceExpressionElementType
//    //            ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION =
//    //            new CjEnumEntrySuperClassReferenceExpressionElementType("ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION");
//    @JvmField
//    val TYPE_ARGUMENT_LIST: CjPlaceHolderStubElementType<CjTypeArgumentList> = CjPlaceHolderStubElementType(
//        "TYPE_ARGUMENT_LIST",
//        CjTypeArgumentList::class.java
//    )
//
//    @JvmField
//    val VALUE_ARGUMENT_LIST: CjPlaceHolderStubElementType<CjValueArgumentList> =
//        CjValueArgumentListElementType("VALUE_ARGUMENT_LIST")
//
//    @JvmField
//    val VALUE_ARGUMENT: CjValueArgumentElementType<CjValueArgument> = CjValueArgumentElementType(
//        "VALUE_ARGUMENT",
//        CjValueArgument::class.java
//    )
//
//
//    //    CjPlaceHolderStubElementType<CjContractEffectList> CONTRACT_EFFECT_LIST =
//    //            new CjContractEffectListElementType("CONTRACT_EFFECT_LIST");
//    //    CjContractEffectElementType CONTRACT_EFFECT =
//    //            new CjContractEffectElementType("CONTRACT_EFFECT", CjContractEffect.class);
//    @JvmField
//    val LAMBDA_ARGUMENT: CjValueArgumentElementType<CjLambdaArgument> = CjValueArgumentElementType(
//        "LAMBDA_ARGUMENT",
//        CjLambdaArgument::class.java
//    )
//
//    @JvmField
//    val VALUE_ARGUMENT_NAME: CjPlaceHolderStubElementType<CjValueArgumentName> = CjPlaceHolderStubElementType(
//        "VALUE_ARGUMENT_NAME",
//        CjValueArgumentName::class.java
//    )
//
//    @JvmField
//    val SUPER_TYPE_LIST: CjPlaceHolderStubElementType<CjSuperTypeList> = CjPlaceHolderStubElementType(
//        "SUPER_TYPE_LIST",
//        CjSuperTypeList::class.java
//    )
//
//
//    @JvmField
//    val SUPER_TYPE_CALL_ENTRY: CjPlaceHolderStubElementType<CjSuperTypeCallEntry> = CjPlaceHolderStubElementType(
//        "SUPER_TYPE_CALL_ENTRY",
//        CjSuperTypeCallEntry::class.java
//    )
//
//    @JvmField
//    val SUPER_TYPE_ENTRY: CjPlaceHolderStubElementType<CjSuperTypeEntry> = CjPlaceHolderStubElementType(
//        "SUPER_TYPE_ENTRY",
//        CjSuperTypeEntry::class.java
//    )
//
//    @JvmField
//    val CONSTRUCTOR_CALLEE: CjPlaceHolderStubElementType<CjConstructorCalleeExpression> = CjPlaceHolderStubElementType(
//        "CONSTRUCTOR_CALLEE",
//        CjConstructorCalleeExpression::class.java
//    )
//
//    @JvmField
//    val CONTEXT_RECEIVER: CjContextReceiverElementType = CjContextReceiverElementType("CONTEXT_RECEIVER")
//
//    @JvmField
//    val CONTEXT_RECEIVER_LIST: CjPlaceHolderStubElementType<CjContextReceiverList> = CjPlaceHolderStubElementType(
//        "CONTEXT_RECEIVER_LIST",
//        CjContextReceiverList::class.java
//    )
//
//    @JvmField
//    val NULL: CjConstantExpressionElementType = CjConstantExpressionElementType("NULL")
//
//    @JvmField
//    val BOOLEAN_CONSTANT: CjConstantExpressionElementType = CjConstantExpressionElementType("BOOLEAN_CONSTANT")
//
//    @JvmField
//    val FLOAT_CONSTANT: CjConstantExpressionElementType = CjConstantExpressionElementType("FLOAT_CONSTANT")
//
//    @JvmField
//    val CHARACTER_CONSTANT: CjConstantExpressionElementType = CjConstantExpressionElementType("CHARACTER_CONSTANT")
//
//    @JvmField
//    val INTEGER_CONSTANT: CjConstantExpressionElementType = CjConstantExpressionElementType("INTEGER_CONSTANT")
//
//    //    CjClassLiteralExpressionElementType CLASS_LITERAL_EXPRESSION = new CjClassLiteralExpressionElementType("CLASS_LITERAL_EXPRESSION");
//    @JvmField
//    val COLLECTION_LITERAL_EXPRESSION: CjCollectionLiteralExpressionElementType =
//        CjCollectionLiteralExpressionElementType("COLLECTION_LITERAL_EXPRESSION")
//
//    @JvmField
//    val STRING_TEMPLATE: CjPlaceHolderStubElementType<CjStringTemplateExpression> =
//        CjStringTemplateExpressionElementType("STRING_TEMPLATE")
//
//    @JvmField
//    val LONG_STRING_TEMPLATE_ENTRY: CjPlaceHolderWithTextStubElementType<CjBlockStringTemplateEntry> =
//        CjPlaceHolderWithTextStubElementType(
//            "LONG_STRING_TEMPLATE_ENTRY",
//            CjBlockStringTemplateEntry::class.java
//        )
//
//    @JvmField
//    val SHORT_STRING_TEMPLATE_ENTRY: CjPlaceHolderWithTextStubElementType<CjSimpleNameStringTemplateEntry> =
//        CjPlaceHolderWithTextStubElementType(
//            "SHORT_STRING_TEMPLATE_ENTRY",
//            CjSimpleNameStringTemplateEntry::class.java
//        )
//
//    @JvmField
//    val LITERAL_STRING_TEMPLATE_ENTRY: CjPlaceHolderWithTextStubElementType<CjLiteralStringTemplateEntry> =
//        CjPlaceHolderWithTextStubElementType(
//            "LITERAL_STRING_TEMPLATE_ENTRY",
//            CjLiteralStringTemplateEntry::class.java
//        )
//
//    @JvmField
//    val ESCAPE_STRING_TEMPLATE_ENTRY: CjPlaceHolderWithTextStubElementType<CjEscapeStringTemplateEntry> =
//        CjPlaceHolderWithTextStubElementType(
//            "ESCAPE_STRING_TEMPLATE_ENTRY",
//            CjEscapeStringTemplateEntry::class.java
//        )
//
//
//    @JvmField
//    val UNIT_CONSTANT: CjConstantExpressionElementType = CjConstantExpressionElementType("UNIT_CONSTANT")
//
//    @JvmField
//    val MACRO_EXPRESSION: CjAnnotationEntryElementType = CjAnnotationEntryElementType("MACRO_EXPRESSION")
//
//
//    //    CjPlaceHolderStubElementType<CjAnnotation> ANNOTATION =
//    //            new CjPlaceHolderStubElementType<>("ANNOTATION", CjAnnotation.class);
//    @JvmField
//    val RUNE_CONSTANT: CjConstantExpressionElementType = CjConstantExpressionElementType("RUNE_CONSTANT")
//
//
//    @JvmField
//    val CHARACTER_BYTE_CONSTANT: CjConstantExpressionElementType =
//        CjConstantExpressionElementType("CHARACTER_BYTE_CONSTANT")
//
//
//    @JvmField
//    val INTERFACE: CjInterfaceElementType = CjInterfaceElementType("INTERFACE")
//
//    @JvmField
//
//    val STRUCT: CjStructElementType = CjStructElementType("STRUCT")
//
//
//    @JvmField
//    val MACRO: CjMacroElementType = CjMacroElementType("MACRO")
//
//    @JvmField
//    val FOREIGN: CjForeignDirectiveElementType = CjForeignDirectiveElementType("FOREIGN")
//
//    @JvmField
//    val FOREIGN_BODY: CjPlaceHolderStubElementType<CjForeignBody> = CjPlaceHolderStubElementType(
//        "FOREIGN_BODY",
//        CjForeignBody::class.java
//    )
//
//
//    @JvmField
//    val ENUM: CjEnumElementType = CjEnumElementType("ENUM")
//
//    @JvmField
//    val EXTEND: CjExtendElementType = CjExtendElementType("EXTEND")
//
//    //    CjTupleTypeElementType TUPLE_TYPE = new CjTupleTypeElementType("TUPLE_TYPE");
//    @JvmField
//    val TUPLE_TYPE: CjPlaceHolderStubElementType<CjTupleType> = CjPlaceHolderStubElementType(
//        "TUPLE_TYPE",
//        CjTupleType::class.java
//    )
//
//
//    @JvmField
//    val ENUM_BODY: CjPlaceHolderStubElementType<CjEnumBody> = CjPlaceHolderStubElementType(
//        "ENUM_BODY",
//        CjEnumBody::class.java
//    )
//
//    @JvmField
//    val PROPERTY_BODY: CjPlaceHolderStubElementType<CjPropertyBody> = CjPlaceHolderStubElementType(
//        "PROPERTY_BODY",
//        CjPropertyBody::class.java
//    )
//    @JvmField
//    val PROPERTY_GET: CjPlaceHolderStubElementType<CjPropertyGet> = CjPlaceHolderStubElementType(
//        "PROPERTY_GET",
//        CjPropertyGet::class.java
//    )
//
//    @JvmField
//    val PROPERTY_SET: CjPlaceHolderStubElementType<CjPropertySet> = CjPlaceHolderStubElementType(
//        "PROPERTY_SET",
//        CjPropertySet::class.java
//    )
//
//
//    @JvmField
//    val TYPE_LIST: CjPlaceHolderStubElementType<CjSuperTypeEntry> = CjPlaceHolderStubElementType(
//        "TYPE_LIST",
//        CjSuperTypeEntry::class.java
//    )
//
//
//    @JvmField
//    val TUPLE_LITERAL_EXPRESSION: CjCollectionLiteralExpressionElementType =
//        CjCollectionLiteralExpressionElementType("TUPLE_LITERAL_EXPRESSION")
//
//    @JvmField
//    val IMPORT_DIRECTIVE_ITEM: CjImportDirectiveItemElementType =
//        CjImportDirectiveItemElementType("IMPORT_DIRECTIVE_ITEM")
//
//
//    @JvmField
//    val MAIN_FUNC: CjMainFunctionElementType = CjMainFunctionElementType("MAIN_FUNC")
//    @JvmField
//    val CLASS_INIT: CjClassInitElementType = CjClassInitElementType("CLASS_INIT")
//    @JvmField
//    val CLASS_MAIN_INIT: CjClassInitElementType = CjClassInitElementType("CLASS_MAIN_INIT")
//    @JvmField
//    val CLASS_TILDE_INIT: CjClassInitElementType = CjClassInitElementType("CLASS_TILDE_INIT")
//
//    @JvmField
//    val BASIC_TYPE: CjBasicTypeElementType = CjBasicTypeElementType("BASIC_TYPE")
//
//
//    val CONSTANT_EXPRESSIONS_TYPES: TokenSet = TokenSet.create(
//        BOOLEAN_CONSTANT,
//        FLOAT_CONSTANT,
//        RUNE_CONSTANT,
//        INTEGER_CONSTANT,
//        CHARACTER_CONSTANT,
//
//        REFERENCE_EXPRESSION,
//        DOT_QUALIFIED_EXPRESSION,
//
//        STRING_TEMPLATE,  //            CLASS_LITERAL_EXPRESSION,
//
//        COLLECTION_LITERAL_EXPRESSION
//    )
//
//
//}
