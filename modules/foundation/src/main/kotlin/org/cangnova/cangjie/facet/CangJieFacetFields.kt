@file:JvmName("CangJieFacetFieldsUtils")

package org.cangnova.cangjie.facet

/**
 * 仓颉 facet 字段集合。
 *
 * 对位 Kotlin `KotlinFacetFields.kt`。
 * 仓颉当前只有单一的通用编译参数层，不存在按 target-platform 分裂的参数面。
 */
fun getExposedFacetFields(): List<String> = commonFields.exposedFields

internal class CangJieFacetFields(
    base: CangJieFacetFields? = null,
    exposedFields: List<String>,
    hiddenFields: List<String>,
) {
    val exposedFields: List<String> = if (base != null) base.exposedFields + exposedFields else exposedFields
    private val hiddenFields: List<String> = if (base != null) base.hiddenFields + hiddenFields else hiddenFields
    val allFields: List<String>
        get() = exposedFields + hiddenFields
}

internal val commonFields = CangJieFacetFields(
    exposedFields = listOf(
        LANGUAGE_VERSION_FIELD,
        VERBOSE_FIELD,
        REPORT_PERF_FIELD,
    ),
    hiddenFields = listOf(
        AUTO_ADVANCE_LANGUAGE_VERSION_FIELD,
        AUTO_ADVANCE_API_VERSION_FIELD,
        DUMP_PERF_FIELD,
    ),
)

private const val LANGUAGE_VERSION_FIELD = "languageVersion"
private const val VERBOSE_FIELD = "verbose"
private const val REPORT_PERF_FIELD = "reportPerf"
private const val AUTO_ADVANCE_LANGUAGE_VERSION_FIELD = "autoAdvanceLanguageVersion"
private const val AUTO_ADVANCE_API_VERSION_FIELD = "autoAdvanceApiVersion"
private const val DUMP_PERF_FIELD = "dumpPerf"
