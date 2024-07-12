package com.huawei.cangjie.resolve.calls

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.resolve.calls.inference.BuilderInferenceSupport
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory

class GenericCandidateResolver(
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val builderInferenceSupport: BuilderInferenceSupport,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dataFlowValueFactory: DataFlowValueFactory
)
