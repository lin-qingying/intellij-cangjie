package com.huawei.cangjie.analyzer.api


object CjDeclarationRendererForSource {
    val WITH_QUALIFIED_NAMES: CjDeclarationRenderer = CjDeclarationRenderer {
        nameRenderer = CjDeclarationNameRenderer.QUOTED
//        keywordsRenderer = CjKeywordsRenderer.AS_WORD
//        contextReceiversRenderer = CjContextReceiversRendererForSource.WITH_LABELS
//        codeStyle = CjRecommendedRendererCodeStyle
//        modifiersRenderer = CjDeclarationModifiersRendererForSource.NO_IMPLICIT_MODIFIERS
//        classifierBodyRenderer = CjClassifierBodyRenderer.NO_BODY
//        bodyMemberScopeProvider = CjRendererBodyMemberScopeProvider.ALL_DECLARED
//        bodyMemberScopeSorter = CjRendererBodyMemberScopeSorter.ENUM_ENTRIES_AT_BEGINING
//
//        superTypeRenderer = CjSuperTypeRenderer.WITH_OUT_APPROXIMATION
//        superTypeListRenderer = CjSuperTypeListRenderer.AS_LIST
//        superTypesFilter = CjSuperTypesFilter.NO_DEFAULT_TYPES
//        superTypesArgumentRenderer = CjSuperTypesCallArgumentsRenderer.EMPTY_PARENS
//        functionLikeBodyRenderer = CjFunctionLikeBodyRenderer.NO_BODY
//        valueParametersRenderer = CjCallableParameterRenderer.PARAMETERS_IN_PARENS
//
//        typeParametersRenderer = CjTypeParametersRenderer.WITH_BOUNDS_IN_WHERE_CLAUSE
//        typeParametersFilter = CjTypeParameterRendererFilter.NO_FOR_CONSTURCTORS
//
//        classInitializerRender = CjClassInitializerRenderer.INIT_BLOCK_WITH_BRACES
//
//        anonymousFunctionRenderer = CjAnonymousFunctionSymbolRenderer.AS_SOURCE
//        backingFieldRenderer = CjBackingFieldSymbolRenderer.AS_FIELD_KEYWORD
//        constructorRenderer = CjConstructorSymbolRenderer.AS_SOURCE
//        enumEntryRenderer = CjEnumEntrySymbolRenderer.AS_SOURCE
//        functionSymbolRenderer = CjFunctionSymbolRenderer.AS_SOURCE
//        javaFieldRenderer = CjJavaFieldSymbolRenderer.AS_SOURCE
//        localVariableRenderer = CjLocalVariableSymbolRenderer.AS_SOURCE
//        getterRenderer = CjPropertyGetterSymbolRenderer.AS_SOURCE
//        setterRenderer = CjPropertySetterSymbolRenderer.AS_SOURCE
//        propertyRenderer = CjKotlinPropertySymbolRenderer.AS_SOURCE
//        kotlinPropertyRenderer = CjKotlinPropertySymbolRenderer.AS_SOURCE
//        syntheticJavaPropertyRenderer = CjSyntheticJavaPropertySymbolRenderer.AS_SOURCE
//        valueParameterRenderer = CjValueParameterSymbolRenderer.AS_SOURCE
//        samConstructorRenderer = CjSamConstructorSymbolRenderer.NOT_RENDER
//
//        callableSignatureRenderer = CjCallableSignatureRenderer.FOR_SOURCE
//        accessorBodyRenderer = CjPropertyAccessorBodyRenderer.NO_BODY
//        parameterDefaultValueRenderer = CjParameterDefaultValueRenderer.NO_DEFAULT_VALUE
//        variableInitializerRenderer = CjVariableInitializerRenderer.NO_INITIALIZER
//
//        classOrObjectRenderer = CjNamedClassOrObjectSymbolRenderer.AS_SOURCE
//        typeAliasRenderer = CjTypeAliasSymbolRenderer.AS_SOURCE
//        anonymousObjectRenderer = CjAnonymousObjectSymbolRenderer.AS_SOURCE
//        singleTypeParameterRenderer = CjSingleTypeParameterSymbolRenderer.WITHOUT_BOUNDS
//        propertyAccessorsRenderer = CjPropertyAccessorsRenderer.NO_DEFAULT
//        destructuringDeclarationRenderer = CjDestructuringDeclarationRenderer.WITH_ENTRIES
//
//        callableReceiverRenderer = CjCallableReceiverRenderer.AS_TYPE_WITH_IN_APPROXIMATION
//        returnTypeRenderer = CjCallableReturnTypeRenderer.WITH_OUT_APPROXIMATION

        typeRenderer = CjTypeRendererForSource.WITH_QUALIFIED_NAMES
//        annotationRenderer = CjAnnotationRendererForSource.WITH_QUALIFIED_NAMES
//        declarationTypeApproximator = CjRendererTypeApproximator.TO_DENOTABLE
//        returnTypeFilter = CjCallableReturnTypeFilter.NO_UNIT_FOR_FUNCTIONS
//
//        scriptRenderer = CjScriptSymbolRenderer.AS_SOURCE
//        scriptInitializerRenderer = CjScriptInitializerRenderer.NO_INITIALIZER
    }

    val WITH_SHORT_NAMES: CjDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
//        annotationRenderer = CjAnnotationRendererForSource.WITH_SHORT_NAMES
        typeRenderer = CjTypeRendererForSource.WITH_SHORT_NAMES
    }
}
