package com.huawei.cangjie.resolve.calls.inference.components

class PostponedArgumentInputTypesResolver(
    private val resultTypeResolver: ResultTypeResolver,
    private val variableFixationFinder: VariableFixationFinder,
    private val resolutionTypeSystemContext: ConstraintSystemUtilContext,
//    private val languageVersionSettings: LanguageVersionSettings,
)
