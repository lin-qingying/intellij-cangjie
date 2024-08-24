package com.huawei.cangjie.ide.completion.back
//
//import com.huawei.cangjie.analyzer.CjAnalysisSession
//import com.huawei.cangjie.ide.completion.contributors.CangJieCompletionContributorFactory
//import com.huawei.cangjie.ide.completion.positionContext.CangJieExpressionNameReferencePositionContext
//import com.huawei.cangjie.ide.completion.positionContext.CangJieRawPositionContext
//import com.huawei.cangjie.psi.CjCallElement
//import com.huawei.cangjie.psi.CjValueArgumentList
//
//internal object Completions {
//    context(CjAnalysisSession)
//    fun complete(
//        factory: CangJieCompletionContributorFactory,
//        positionContext: CangJieRawPositionContext,
//        weighingContext: WeighingContext,
//        sessionParameters: CangJieCompletionSessionParameters,
//    ) {
//        when (positionContext) {
//            is CangJieExpressionNameReferencePositionContext -> if (positionContext.allowsOnlyNamedArguments()) {
//                complete(factory.namedArgumentContributor(0), positionContext, weighingContext, sessionParameters)
//            } else {
//                complete(factory.keywordContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.namedArgumentContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.callableContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.classifierContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.packageCompletionContributor(1), positionContext, weighingContext, sessionParameters)
//            }
//
//            is CangJieSuperReceiverNameReferencePositionContext -> {
//                complete(factory.superMemberContributor(0), positionContext, weighingContext, sessionParameters)
//            }
//
//            is CangJieTypeNameReferencePositionContext -> {
//                if (sessionParameters.allowClassifiersAndPackagesForPossibleExtensionCallables) {
//                    complete(factory.classifierContributor(0), positionContext, weighingContext, sessionParameters)
//                }
//                complete(factory.keywordContributor(1), positionContext, weighingContext, sessionParameters)
//                if (sessionParameters.allowClassifiersAndPackagesForPossibleExtensionCallables) {
//                    complete(factory.packageCompletionContributor(2), positionContext, weighingContext, sessionParameters)
//                }
//                // For `val` and `fun` completion. For example, with `val i<caret>`, the fake file contains `val iX.f`. Hence a
//                // FirTypeNameReferencePositionContext is created because `iX` is parsed as a type reference.
//                complete(factory.declarationFromUnresolvedNameContributor(1), positionContext, weighingContext, sessionParameters)
//                complete(factory.declarationFromOverridableMembersContributor(1), positionContext, weighingContext, sessionParameters)
//                complete(factory.variableOrParameterNameWithTypeContributor(0), positionContext, weighingContext, sessionParameters)
//            }
//
//            is CangJieAnnotationTypeNameReferencePositionContext -> {
//                complete(factory.annotationsContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.keywordContributor(1), positionContext, weighingContext, sessionParameters)
//                complete(factory.packageCompletionContributor(2), positionContext, weighingContext, sessionParameters)
//            }
//
//            is CangJieSuperTypeCallNameReferencePositionContext -> {
//                complete(factory.superEntryContributor(0), positionContext, weighingContext, sessionParameters)
//            }
//
//            is CangJieImportDirectivePositionContext -> {
//                complete(factory.packageCompletionContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.importDirectivePackageMembersContributor(0), positionContext, weighingContext, sessionParameters)
//            }
//
//            is CangJiePackageDirectivePositionContext -> {
//                complete(factory.packageCompletionContributor(0), positionContext, weighingContext, sessionParameters)
//            }
//
//            is CangJieTypeConstraintNameInWhereClausePositionContext -> {
//                complete(factory.typeParameterConstraintNameInWhereClauseContributor(), positionContext, weighingContext, sessionParameters)
//            }
//
//            is CangJieMemberDeclarationExpectedPositionContext -> {
//                complete(factory.keywordContributor(0), positionContext, weighingContext, sessionParameters)
//            }
//
//            is CangJieUnknownPositionContext -> {
//                complete(factory.keywordContributor(0), positionContext, weighingContext, sessionParameters)
//            }
//
//            is CangJieClassifierNamePositionContext -> {
//                complete(factory.classifierNameContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.declarationFromUnresolvedNameContributor(1), positionContext, weighingContext, sessionParameters)
//            }
//
//            is CangJieWithSubjectEntryPositionContext -> {
//                complete(factory.whenWithSubjectConditionContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.callableContributor(1), positionContext, weighingContext, sessionParameters)
//            }
//
//            is CangJieCallableReferencePositionContext -> {
//                complete(factory.classReferenceContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.callableReferenceContributor(1), positionContext, weighingContext, sessionParameters)
//                complete(factory.classifierReferenceContributor(1), positionContext, weighingContext, sessionParameters)
//            }
//
//            is CangJieInfixCallPositionContext -> {
//                complete(factory.keywordContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.infixCallableContributor(0), positionContext, weighingContext, sessionParameters)
//            }
//
//            is CangJieIncorrectPositionContext -> {
//                // do nothing, completion is not supposed to be called here
//            }
//
//            is CangJieSimpleParameterPositionContext -> {
//                // for parameter declaration
//                complete(factory.declarationFromUnresolvedNameContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.keywordContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.variableOrParameterNameWithTypeContributor(0), positionContext, weighingContext, sessionParameters)
//            }
//
//            is CangJiePrimaryConstructorParameterPositionContext -> {
//                // for parameter declaration
//                complete(factory.declarationFromUnresolvedNameContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.declarationFromOverridableMembersContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.keywordContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.variableOrParameterNameWithTypeContributor(0), positionContext, weighingContext, sessionParameters)
//            }
//
//            is KDocParameterNamePositionContext -> {
//                complete(factory.kDocParameterNameContributor(0), positionContext, weighingContext, sessionParameters)
//            }
//
//            is KDocLinkNamePositionContext -> {
//                complete(factory.kDocCallableContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.classifierContributor(0), positionContext, weighingContext, sessionParameters)
//                complete(factory.packageCompletionContributor(1), positionContext, weighingContext, sessionParameters)
//            }
//        }
//    }
//
//    context(CjAnalysisSession)
//    fun createWeighingContext(
//        basicContext: FirBasicCompletionContext,
//        positionContext: CangJieRawPositionContext
//    ): WeighingContext = when (positionContext) {
//        is CangJieSuperReceiverNameReferencePositionContext -> {
//            val expectedType = positionContext.nameExpression.getExpectedType()
//            val receiver = positionContext.superExpression
//
//            // Implicit receivers do not match for this position completion context.
//            WeighingContext.createWeighingContext(
//                basicContext,
//                receiver,
//                expectedType,
//                implicitReceivers = emptyList(),
//                positionContext.position
//            )
//        }
//
//        is CangJieWithSubjectEntryPositionContext -> {
//            val subjectReference = (positionContext.subjectExpression as? CjSimpleNameExpression)?.mainReference
//            val symbolsToSkip = setOfNotNull(subjectReference?.resolveToSymbol())
//            createWeighingContextForNameReference(basicContext, positionContext, symbolsToSkip)
//        }
//
//        is CangJieNameReferencePositionContext -> createWeighingContextForNameReference(basicContext, positionContext)
//        else -> WeighingContext.createEmptyWeighingContext(basicContext, positionContext.position)
//    }
//
//    context(CjAnalysisSession)
//    private fun createWeighingContextForNameReference(
//        basicContext: FirBasicCompletionContext,
//        positionContext: CangJieNameReferencePositionContext,
//        symbolsToSkip: Set<CjSymbol> = emptySet(),
//    ): WeighingContext {
//        val expectedType = when (positionContext) {
//            // during the sorting of completion suggestions expected type from position and actual types of suggestions are compared;
//            // see `org.jetbrains.kotlin.idea.completion.weighers.ExpectedTypeWeigher`;
//            // currently in case of callable references actual types are calculated incorrectly, which is why we don't use information
//            // about expected type at all
//            // TODO: calculate actual types for callable references correctly and use information about expected type
//            is CangJieCallableReferencePositionContext -> null
//            else -> positionContext.nameExpression.getExpectedType()
//        }
//        val receiver = positionContext.explicitReceiver
//        val implicitReceivers = basicContext.originalCjFile.getScopeContextForPosition(positionContext.nameExpression).implicitReceivers
//
//        return WeighingContext.createWeighingContext(
//            basicContext,
//            receiver,
//            expectedType,
//            implicitReceivers,
//            positionContext.position,
//            symbolsToSkip
//        )
//    }
//}
//
//context(CjAnalysisSession)
//private fun CangJieExpressionNameReferencePositionContext.allowsOnlyNamedArguments(): Boolean {
//    if (explicitReceiver != null) return false
//
//    val valueArgument = findValueArgument(nameExpression) ?: return false
//    val valueArgumentList = valueArgument.parent as? CjValueArgumentList ?: return false
//    val callElement = valueArgumentList.parent as? CjCallElement ?: return false
//
//    if (valueArgument.getArgumentName() != null) return false
//
//    val call = callElement.resolveCall()?.singleCallOrNull<CjFunctionCall<*>>() ?: return false
//
//    if (CallParameterInfoProvider.isJavaArgumentWithNonDefaultName(
//            call.partiallyAppliedSymbol.signature,
//            call.argumentMapping,
//            valueArgument
//        )
//    ) return true
//
//    val firstArgumentInNamedMode = CallParameterInfoProvider.firstArgumentInNamedMode(
//        callElement,
//        call.partiallyAppliedSymbol.signature,
//        call.argumentMapping,
//        callElement.languageVersionSettings
//    ) ?: return false
//
//    return with(valueArgumentList.arguments) { indexOf(valueArgument) >= indexOf(firstArgumentInNamedMode) }
//}
