package com.linqingying.cangjie.resolve.calls

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.resolve.calls.inference.BuilderInferenceSupport
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.linqingying.cangjie.types.expressions.ControlStructureTypingUtils

val SPECIAL_FUNCTION_NAMES = ControlStructureTypingUtils.ResolveConstruct.entries.map { it.specialFunctionName }.toSet()

class GenericCandidateResolver(
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val builderInferenceSupport: BuilderInferenceSupport,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dataFlowValueFactory: DataFlowValueFactory
)
