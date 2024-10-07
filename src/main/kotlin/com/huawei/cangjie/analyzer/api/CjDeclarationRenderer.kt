package com.huawei.cangjie.analyzer.api

import com.huawei.cangjie.analyzer.CjAnalysisSession

class CjDeclarationRenderer private constructor(
    val nameRenderer: CjDeclarationNameRenderer,
//    val keywordsRenderer: CjKeywordsRenderer,
//    val contextReceiversRenderer: CjContextReceiversRenderer,
//    val codeStyle: CjRendererCodeStyle,
    val typeRenderer: CjTypeRenderer,
//    val annotationRenderer: CjAnnotationRenderer,
//    val modifiersRenderer: CjDeclarationModifiersRenderer,
//    val declarationTypeApproximator: CjRendererTypeApproximator,
//    val classifierBodyRenderer: CjClassifierBodyRenderer,
//
//
//    val superTypeRenderer: CjSuperTypeRenderer,
//    val superTypeListRenderer: CjSuperTypeListRenderer,
//    val superTypesFilter: CjSuperTypesFilter,
//    val superTypesArgumentRenderer: CjSuperTypesCallArgumentsRenderer,
//
//    val bodyMemberScopeProvider: CjRendererBodyMemberScopeProvider,
//    val bodyMemberScopeSorter: CjRendererBodyMemberScopeSorter,
//
//    val functionLikeBodyRenderer: CjFunctionLikeBodyRenderer,
//    val variableInitializerRenderer: CjVariableInitializerRenderer,
//    val parameterDefaultValueRenderer: CjParameterDefaultValueRenderer,
//    val accessorBodyRenderer: CjPropertyAccessorBodyRenderer,
//
//    val returnTypeRenderer: CjCallableReturnTypeRenderer,
//    val callableReceiverRenderer: CjCallableReceiverRenderer,
//
//    val valueParametersRenderer: CjCallableParameterRenderer,
//    val typeParametersRenderer: CjTypeParametersRenderer,
//    val typeParametersFilter: CjTypeParameterRendererFilter,
//
//    val callableSignatureRenderer: CjCallableSignatureRenderer,
//
//    val anonymousFunctionRenderer: CjAnonymousFunctionSymbolRenderer,
//    val backingFieldRenderer: CjBackingFieldSymbolRenderer,
//    val constructorRenderer: CjConstructorSymbolRenderer,
//    val enumEntryRenderer: CjEnumEntrySymbolRenderer,
//    val functionSymbolRenderer: CjFunctionSymbolRenderer,
//    val javaFieldRenderer: CjJavaFieldSymbolRenderer,
//    val localVariableRenderer: CjLocalVariableSymbolRenderer,
//    val getterRenderer: CjPropertyGetterSymbolRenderer,
//    val setterRenderer: CjPropertySetterSymbolRenderer,
//    val propertyRenderer: CjKotlinPropertySymbolRenderer,
//    val cangjiePropertyRenderer: CjKotlinPropertySymbolRenderer,
//    val syntheticJavaPropertyRenderer: CjSyntheticJavaPropertySymbolRenderer,
//    val valueParameterRenderer: CjValueParameterSymbolRenderer,
//    val samConstructorRenderer: CjSamConstructorSymbolRenderer,
//    val propertyAccessorsRenderer: CjPropertyAccessorsRenderer,
//    val destructuringDeclarationRenderer: CjDestructuringDeclarationRenderer,
//
//    val classInitializerRender: CjClassInitializerRenderer,
//    val classOrObjectRenderer: CjNamedClassOrObjectSymbolRenderer,
//    val typeAliasRenderer: CjTypeAliasSymbolRenderer,
//    val anonymousObjectRenderer: CjAnonymousObjectSymbolRenderer,
//    val singleTypeParameterRenderer: CjSingleTypeParameterSymbolRenderer,
//    val returnTypeFilter: CjCallableReturnTypeFilter,
//
//    val scriptRenderer: CjScriptSymbolRenderer,
//    val scriptInitializerRenderer: CjScriptInitializerRenderer
) {

    context(CjAnalysisSession)
    fun renderDeclaration(symbol: CjDeclarationSymbol, printer: PrettyPrinter) {
//        when (symbol) {
//            is CjAnonymousObjectSymbol -> anonymousObjectRenderer.renderSymbol(symbol, printer)
//            is CjNamedClassOrObjectSymbol -> classOrObjectRenderer.renderSymbol(symbol, printer)
//            is CjTypeAliasSymbol -> typeAliasRenderer.renderSymbol(symbol, printer)
//            is CjAnonymousFunctionSymbol -> anonymousFunctionRenderer.renderSymbol(symbol, printer)
//            is CjConstructorSymbol -> constructorRenderer.renderSymbol(symbol, printer)
//            is CjFunctionSymbol -> functionSymbolRenderer.renderSymbol(symbol, printer)
//            is CjPropertyGetterSymbol -> getterRenderer.renderSymbol(symbol, printer)
//            is CjPropertySetterSymbol -> setterRenderer.renderSymbol(symbol, printer)
//            is CjSamConstructorSymbol -> samConstructorRenderer.renderSymbol(symbol, printer)
//            is CjBackingFieldSymbol -> backingFieldRenderer.renderSymbol(symbol, printer)
//            is CjEnumEntrySymbol -> enumEntryRenderer.renderSymbol(symbol, printer)
//            is CjValueParameterSymbol -> valueParameterRenderer.renderSymbol(symbol, printer)
//            is CjJavaFieldSymbol -> javaFieldRenderer.renderSymbol(symbol, printer)
//            is CjLocalVariableSymbol -> localVariableRenderer.renderSymbol(symbol, printer)
//            is CjKotlinPropertySymbol -> cangjiePropertyRenderer.renderSymbol(symbol, printer)
//            is CjSyntheticJavaPropertySymbol -> syntheticJavaPropertyRenderer.renderSymbol(symbol, printer)
//            is CjTypeParameterSymbol -> singleTypeParameterRenderer.renderSymbol(symbol, printer)
//            is CjClassInitializerSymbol -> classInitializerRender.renderClassInitializer(symbol, printer)
//            is CjScriptSymbol -> scriptRenderer.renderSymbol(symbol, printer)
//            is CjDestructuringDeclarationSymbol -> destructuringDeclarationRenderer.renderSymbol(symbol, printer)
//        }
    }

    fun with(action: Builder.() -> Unit): CjDeclarationRenderer {
        val renderer = this
        return CjDeclarationRenderer {
            this.nameRenderer = renderer.nameRenderer
//            this.keywordsRenderer = renderer.keywordsRenderer
//            this.contextReceiversRenderer = renderer.contextReceiversRenderer
//            this.codeStyle = renderer.codeStyle
            this.typeRenderer = renderer.typeRenderer
//            this.annotationRenderer = renderer.annotationRenderer
//            this.modifiersRenderer = renderer.modifiersRenderer
//            this.declarationTypeApproximator = renderer.declarationTypeApproximator
//            this.classifierBodyRenderer = renderer.classifierBodyRenderer
//
//            this.superTypeRenderer = renderer.superTypeRenderer
//            this.superTypeListRenderer = renderer.superTypeListRenderer
//            this.superTypesFilter = renderer.superTypesFilter
//            this.superTypesArgumentRenderer = renderer.superTypesArgumentRenderer
//
//            this.bodyMemberScopeProvider = renderer.bodyMemberScopeProvider
//            this.bodyMemberScopeSorter = renderer.bodyMemberScopeSorter
//
//            this.functionLikeBodyRenderer = renderer.functionLikeBodyRenderer
//            this.variableInitializerRenderer = renderer.variableInitializerRenderer
//            this.parameterDefaultValueRenderer = renderer.parameterDefaultValueRenderer
//            this.accessorBodyRenderer = renderer.accessorBodyRenderer
//
//            this.returnTypeRenderer = renderer.returnTypeRenderer
//            this.callableReceiverRenderer = renderer.callableReceiverRenderer
//
//            this.valueParametersRenderer = renderer.valueParametersRenderer
//            this.typeParametersRenderer = renderer.typeParametersRenderer
//            this.typeParametersFilter = renderer.typeParametersFilter
//
//            this.callableSignatureRenderer = renderer.callableSignatureRenderer
//
//            this.anonymousFunctionRenderer = renderer.anonymousFunctionRenderer
//            this.backingFieldRenderer = renderer.backingFieldRenderer
//            this.constructorRenderer = renderer.constructorRenderer
//            this.enumEntryRenderer = renderer.enumEntryRenderer
//            this.functionSymbolRenderer = renderer.functionSymbolRenderer
//            this.javaFieldRenderer = renderer.javaFieldRenderer
//            this.localVariableRenderer = renderer.localVariableRenderer
//            this.getterRenderer = renderer.getterRenderer
//            this.setterRenderer = renderer.setterRenderer
//            this.propertyRenderer = renderer.propertyRenderer
//            this.cangjiePropertyRenderer = renderer.cangjiePropertyRenderer
//            this.syntheticJavaPropertyRenderer = renderer.syntheticJavaPropertyRenderer
//            this.valueParameterRenderer = renderer.valueParameterRenderer
//            this.samConstructorRenderer = renderer.samConstructorRenderer
//            this.propertyAccessorsRenderer = renderer.propertyAccessorsRenderer
//            this.destructuringDeclarationRenderer = renderer.destructuringDeclarationRenderer
//
//            this.classInitializerRender = renderer.classInitializerRender
//            this.classOrObjectRenderer = renderer.classOrObjectRenderer
//            this.typeAliasRenderer = renderer.typeAliasRenderer
//            this.anonymousObjectRenderer = renderer.anonymousObjectRenderer
//            this.singleTypeParameterRenderer = renderer.singleTypeParameterRenderer
//            this.returnTypeFilter = renderer.returnTypeFilter
//
//            this.scriptRenderer = renderer.scriptRenderer
//            this.scriptInitializerRenderer = renderer.scriptInitializerRenderer

            action()
        }
    }

    companion object {
        operator fun invoke(action: Builder.() -> Unit): CjDeclarationRenderer =
            Builder().apply(action).build()
    }

    open class Builder {
        //        public lateinit var returnTypeFilter: CjCallableReturnTypeFilter
        lateinit var nameRenderer: CjDeclarationNameRenderer
//        lateinit var contextReceiversRenderer: CjContextReceiversRenderer
//        lateinit var keywordsRenderer: CjKeywordsRenderer
//        lateinit var codeStyle: CjRendererCodeStyle
        lateinit var typeRenderer: CjTypeRenderer
//        public lateinit var annotationRenderer: CjAnnotationRenderer
//        public lateinit var modifiersRenderer: CjDeclarationModifiersRenderer
//        public lateinit var declarationTypeApproximator: CjRendererTypeApproximator
//        public lateinit var classifierBodyRenderer: CjClassifierBodyRenderer
//
//        public lateinit var superTypeRenderer: CjSuperTypeRenderer
//        public lateinit var superTypeListRenderer: CjSuperTypeListRenderer
//        public lateinit var superTypesFilter: CjSuperTypesFilter
//        public lateinit var superTypesArgumentRenderer: CjSuperTypesCallArgumentsRenderer
//
//        public lateinit var bodyMemberScopeProvider: CjRendererBodyMemberScopeProvider
//        public lateinit var bodyMemberScopeSorter: CjRendererBodyMemberScopeSorter
//
//        public lateinit var functionLikeBodyRenderer: CjFunctionLikeBodyRenderer
//        public lateinit var variableInitializerRenderer: CjVariableInitializerRenderer
//        public lateinit var parameterDefaultValueRenderer: CjParameterDefaultValueRenderer
//        public lateinit var accessorBodyRenderer: CjPropertyAccessorBodyRenderer
//
//        public lateinit var returnTypeRenderer: CjCallableReturnTypeRenderer
//        public lateinit var callableReceiverRenderer: CjCallableReceiverRenderer
//
//        public lateinit var valueParametersRenderer: CjCallableParameterRenderer
//        public lateinit var typeParametersRenderer: CjTypeParametersRenderer
//        public lateinit var typeParametersFilter: CjTypeParameterRendererFilter
//        public lateinit var callableSignatureRenderer: CjCallableSignatureRenderer
//
//        public lateinit var anonymousFunctionRenderer: CjAnonymousFunctionSymbolRenderer
//        public lateinit var backingFieldRenderer: CjBackingFieldSymbolRenderer
//        public lateinit var constructorRenderer: CjConstructorSymbolRenderer
//        public lateinit var enumEntryRenderer: CjEnumEntrySymbolRenderer
//        public lateinit var functionSymbolRenderer: CjFunctionSymbolRenderer
//        public lateinit var javaFieldRenderer: CjJavaFieldSymbolRenderer
//        public lateinit var localVariableRenderer: CjLocalVariableSymbolRenderer
//        public lateinit var getterRenderer: CjPropertyGetterSymbolRenderer
//        public lateinit var setterRenderer: CjPropertySetterSymbolRenderer
//        public lateinit var propertyRenderer: CjKotlinPropertySymbolRenderer
//        public lateinit var cangjiePropertyRenderer: CjKotlinPropertySymbolRenderer
//        public lateinit var syntheticJavaPropertyRenderer: CjSyntheticJavaPropertySymbolRenderer
//        public lateinit var valueParameterRenderer: CjValueParameterSymbolRenderer
//        public lateinit var samConstructorRenderer: CjSamConstructorSymbolRenderer
//        public lateinit var propertyAccessorsRenderer: CjPropertyAccessorsRenderer
//        public lateinit var destructuringDeclarationRenderer: CjDestructuringDeclarationRenderer
//
//        public lateinit var classInitializerRender: CjClassInitializerRenderer
//        public lateinit var classOrObjectRenderer: CjNamedClassOrObjectSymbolRenderer
//        public lateinit var typeAliasRenderer: CjTypeAliasSymbolRenderer
//        public lateinit var anonymousObjectRenderer: CjAnonymousObjectSymbolRenderer
//        public lateinit var singleTypeParameterRenderer: CjSingleTypeParameterSymbolRenderer
//
//        public lateinit var scriptRenderer: CjScriptSymbolRenderer
//        public lateinit var scriptInitializerRenderer: CjScriptInitializerRenderer

        fun build(): CjDeclarationRenderer = CjDeclarationRenderer(
            nameRenderer,
//            keywordsRenderer,
//            contextReceiversRenderer,
//            codeStyle,
            typeRenderer,
//            annotationRenderer,
//            modifiersRenderer,
//            declarationTypeApproximator,
//            classifierBodyRenderer,
//
//            superTypeRenderer,
//            superTypeListRenderer,
//            superTypesFilter,
//            superTypesArgumentRenderer,
//
//            bodyMemberScopeProvider,
//            bodyMemberScopeSorter,
//
//            functionLikeBodyRenderer,
//            variableInitializerRenderer,
//            parameterDefaultValueRenderer,
//            accessorBodyRenderer,
//
//            returnTypeRenderer,
//            callableReceiverRenderer,
//
//            valueParametersRenderer,
//            typeParametersRenderer,
//            typeParametersFilter,
//            callableSignatureRenderer,
//
//            anonymousFunctionRenderer,
//            backingFieldRenderer,
//            constructorRenderer,
//            enumEntryRenderer,
//            functionSymbolRenderer,
//            javaFieldRenderer,
//            localVariableRenderer,
//            getterRenderer,
//            setterRenderer,
//            propertyRenderer,
//            cangjiePropertyRenderer,
//            syntheticJavaPropertyRenderer,
//            valueParameterRenderer,
//            samConstructorRenderer,
//            propertyAccessorsRenderer,
//            destructuringDeclarationRenderer,
//
//            classInitializerRender,
//            classOrObjectRenderer,
//            typeAliasRenderer,
//            anonymousObjectRenderer,
//            singleTypeParameterRenderer,
//            returnTypeFilter,
//
//            scriptRenderer,
//            scriptInitializerRenderer,
        )
    }
}

