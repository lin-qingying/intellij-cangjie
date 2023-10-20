package com.huawei.cangjie.cjpm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class CjpmConfig(
    val buildTargets: List<String>,
    val env: Map<String, EnvValue>,

    ) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EnvValue(
        @JsonProperty("value")
        val value: String,
        @JsonProperty("force")
        val isForced: Boolean = false,
        @JsonProperty("relative")
        val isRelative: Boolean = false
    )

    companion object {
        val DEFAULT = CjpmConfig(emptyList(), emptyMap())
    }
}
