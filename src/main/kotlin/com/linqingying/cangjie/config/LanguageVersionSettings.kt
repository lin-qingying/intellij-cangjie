package com.linqingying.cangjie.config


import com.linqingying.cangjie.config.CangJieVersion.Companion.MAX_COMPONENT_VALUE
import com.linqingying.cangjie.config.LanguageFeature.Kind.*
import com.linqingying.cangjie.config.LanguageVersion.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface DescriptionAware {
    val description: String
}

enum class LanguageFeature(
    val sinceVersion: LanguageVersion?,
    val sinceApiVersion: ApiVersion = ApiVersion.CANGJIE_0_53_4,
    val hintUrl: String? = null,
    internal val isEnabledWithWarning: Boolean = false,
    val kind: Kind = OTHER // NB: default value OTHER doesn't force pre-releaseness (see KDoc)
) {


    TypeAliases(CANGJIE_0_53_4),
    OverloadResolutionByLambdaReturnType(CANGJIE_0_53_4),
    RefinedSamAdaptersPriority(CANGJIE_0_53_4),
    ProhibitConstructorCallOnFunctionalSupertype(CANGJIE_0_53_4, kind = BUG_FIX),


    SingleUnderscoreForParameterName(CANGJIE_0_53_4),
    SealedInterfaces(CANGJIE_0_53_4),

    SafeCallBoundSmartCasts(CANGJIE_0_53_4),

    DefaultImportOfPackageCangJieComparisons(CANGJIE_0_53_4),

ImprovedCapturedTypeApproximationInInference(CANGJIE_0_53_4, kind = OTHER),

    SafeCastCheckBoundSmartCasts(CANGJIE_0_53_4),

    ExpectedTypeFromCast(CANGJIE_0_53_4),

    ProhibitVisibilityOfNestedClassifiersFromSupertypes(CANGJIE_0_53_4, kind = BUG_FIX),

    MixedNamedArgumentsInTheirOwnPosition(CANGJIE_0_53_4),

    TrailingCommas(CANGJIE_0_53_4),

    NewInference(CANGJIE_0_53_4),

    SamConversionForCangJieFunctions(CANGJIE_0_53_4),
    SamConversionPerArgument(CANGJIE_0_53_4),

    ProhibitInvisibleAbstractMethodsInSuperclasses(CANGJIE_0_53_4, kind = BUG_FIX),

    InferenceCompatibility(CANGJIE_0_53_4, kind = BUG_FIX),
    RequiredPrimaryConstructorDelegationCallInEnums(CANGJIE_0_53_4, kind = BUG_FIX),

    UseCorrectExecutionOrderForVarargArguments(CANGJIE_0_53_4, kind = BUG_FIX),

    AllowSealedInheritorsInDifferentFilesOfSamePackage(CANGJIE_0_53_4),

    StrictOnlyInputTypesChecks(CANGJIE_0_53_4),

    TypeInferenceOnCallsWithSelfTypes(CANGJIE_0_53_4),
    TakeIntoAccountEffectivelyFinalInMustBeInitializedCheck(CANGJIE_0_53_4, kind = OTHER),
    ForbidInferringTypeVariablesIntoEmptyIntersection(sinceVersion = null, kind = BUG_FIX),

    UseBuilderInferenceWithoutAnnotation(CANGJIE_0_53_4),

    DefinitelyNonNullableTypes(CANGJIE_0_53_4),

    SafeCallsAreAlwaysNullable(CANGJIE_0_53_4),
    RestrictionOfLetReassignmentViaBackingField(CANGJIE_0_53_4, kind = BUG_FIX),

    ProperTypeInferenceConstraintsProcessing(CANGJIE_0_53_4, kind = BUG_FIX),

    EliminateAmbiguitiesWithExternalTypeParameters(CANGJIE_0_53_4),
    EliminateAmbiguitiesOnInheritedSamInterfaces(CANGJIE_0_53_4),
    ProperInternalVisibilityCheckInImportingScope(CANGJIE_0_53_4, kind = BUG_FIX),

    YieldIsNoMoreReserved(CANGJIE_0_53_4),

    SuspendOnlySamConversions(CANGJIE_0_53_4),

    UseConsistentRulesForPrivateConstructorsOfSealedClasses(sinceVersion = CANGJIE_0_53_4, kind = BUG_FIX),

    AbstractClassMemberNotImplementedWithIntermediateAbstractClass(CANGJIE_0_53_4, kind = BUG_FIX),

    ReportMissingUpperBoundsViolatedErrorOnAbbreviationAtSupertypes(CANGJIE_0_53_4, kind = BUG_FIX),


    AllowEmptyIntersectionsInResultTypeResolver(CANGJIE_0_53_4, kind = OTHER),

    NoAdditionalErrorsInDiagnosticReporter(sinceVersion = null, kind = OTHER),

    FunctionalTypeWithExtensionAsSupertype(sinceVersion = null),

    ContextReceivers(sinceVersion = null),

    ImplicitSignedToUnsignedIntegerConversion(sinceVersion = null),
    NoBuilderInferenceWithoutAnnotationRestriction(sinceVersion = null, kind = OTHER),

    ;

    init {
        if (sinceVersion == null && isEnabledWithWarning) {
            error("$this: '${::isEnabledWithWarning.name}' has no effect if the feature is disabled by default")
        }
    }

    val presentableName: String
        // E.g. "DestructuringLambdaParameters" -> ["Destructuring", "Lambda", "Parameters"] -> "destructuring lambda parameters"
        get() = name.split("(?<!^)(?=[A-Z])".toRegex()).joinToString(separator = " ", transform = String::lowercase)

    enum class State(override val description: String) : DescriptionAware {
        ENABLED("Enabled"),
        ENABLED_WITH_WARNING("Enabled with warning"),
        DISABLED("Disabled");
    }

    enum class Kind(val forcesPreReleaseBinaries: Boolean) {
        /**
         * Simple bug fix which just forbids some language constructions.
         * Rule of thumb: it turns "green code" into "red".
         *
         * Note that, some actual bug fixes can affect overload resolution/inference, silently changing semantics of
         * users' code -- DO NOT use Kind.BUG_FIX for them!
         */
        BUG_FIX(false),

        /**
         * A new feature in the language which has no impact on the binary output of the compiler, and therefore
         * does not cause pre-release binaries to be generated.
         * Rule of thumb: it turns "red" code into "green" and the old compilers can correctly use the binaries
         * produced by the new compiler.
         *
         * NB. OTHER is not a conservative fallback, as it doesn't imply generation of pre-release binaries
         */
        OTHER(false),
    }

    companion object {
        @JvmStatic
        fun fromString(str: String) = entries.find { it.name == str }
    }
}

enum class LanguageVersion(val major: Int, val minor: Int, val patch: Int) : DescriptionAware, LanguageOrApiVersion {

    CANGJIE_0_53_4(0, 53, 4),
    ;

    override val isStable: Boolean
        get() = this <= LATEST_STABLE

    override val isDeprecated: Boolean
        get() = FIRST_SUPPORTED <= this && this < FIRST_NON_DEPRECATED

    override val isUnsupported: Boolean
        get() = this < FIRST_SUPPORTED

    override val versionString: String = "$major.$minor"

    override fun toString() = versionString

    companion object {
        @JvmStatic
        fun fromVersionString(str: String?) = entries.find { it.versionString == str }

        @JvmStatic
        fun fromFullVersionString(str: String) =
            str.split(".", "-").let { if (it.size >= 2) fromVersionString("${it[0]}.${it[1]}") else null }

        // Version status
        //            1.0..1.3        1.4..1.6           1.7..2.0    2.1
        // Language:  UNSUPPORTED --> DEPRECATED ------> STABLE ---> EXPERIMENTAL
        // API:       UNSUPPORTED --> DEPRECATED ------> STABLE ---> EXPERIMENTAL

        @JvmField
        val FIRST_API_SUPPORTED = CANGJIE_0_53_4

        @JvmField
        val FIRST_SUPPORTED = CANGJIE_0_53_4

        @JvmField
        val FIRST_NON_DEPRECATED = CANGJIE_0_53_4

        @JvmField
        val LATEST_STABLE = CANGJIE_0_53_4
    }
}

interface LanguageOrApiVersion : DescriptionAware {
    val versionString: String

    val isStable: Boolean

    val isDeprecated: Boolean

    val isUnsupported: Boolean

    override val description: String
        get() = when {
            !isStable -> "$versionString (experimental)"
            isDeprecated -> "$versionString (deprecated)"
            isUnsupported -> "$versionString (unsupported)"
            else -> versionString
        }
}

fun LanguageVersion.toCangJieVersion() = CangJieVersion(major, minor)

interface LanguageVersionSettings {
    fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State

    fun supportsFeature(feature: LanguageFeature): Boolean =
        getFeatureSupport(feature).let {
            it == LanguageFeature.State.ENABLED ||
                    it == LanguageFeature.State.ENABLED_WITH_WARNING
        }


    fun <T> getFlag(flag: AnalysisFlag<T>): T

    val apiVersion: ApiVersion

    // Please do not use this to enable/disable specific features/checks. Instead add a new LanguageFeature entry and call supportsFeature
    val languageVersion: LanguageVersion

    companion object {
        const val RESOURCE_NAME_TO_ALLOW_READING_FROM_ENVIRONMENT = "META-INF/allow-configuring-from-environment"
    }
}

class LanguageVersionSettingsImpl @JvmOverloads constructor(
    override val languageVersion: LanguageVersion,
    override val apiVersion: ApiVersion,
    analysisFlags: Map<AnalysisFlag<*>, Any?> = emptyMap(),
    specificFeatures: Map<LanguageFeature, LanguageFeature.State> = emptyMap()
) : LanguageVersionSettings {
    private val analysisFlags: Map<AnalysisFlag<*>, *> = Collections.unmodifiableMap(analysisFlags)
    private val specificFeatures: Map<LanguageFeature, LanguageFeature.State> =
        Collections.unmodifiableMap(specificFeatures)

    @Suppress("UNCHECKED_CAST")
    override fun <T> getFlag(flag: AnalysisFlag<T>): T = analysisFlags[flag] as T? ?: flag.defaultValue

    override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State {
        specificFeatures[feature]?.let { return it }

        val since = feature.sinceVersion
        if (since != null && languageVersion >= since && apiVersion >= feature.sinceApiVersion) {
            return if (feature.isEnabledWithWarning) LanguageFeature.State.ENABLED_WITH_WARNING else LanguageFeature.State.ENABLED
        }

        return LanguageFeature.State.DISABLED
    }

    override fun toString() = buildString {
        append("Language = $languageVersion, API = $apiVersion")
        specificFeatures.entries.sortedBy { (feature, _) -> feature.ordinal }.forEach { (feature, state) ->
            val char = when (state) {
                LanguageFeature.State.ENABLED -> '+'
                LanguageFeature.State.ENABLED_WITH_WARNING -> '~'
                LanguageFeature.State.DISABLED -> '-'
            }
            append(" $char$feature")
        }
        analysisFlags.entries.sortedBy { (flag, _) -> flag.toString() }.forEach { (flag, value) ->
            append(" $flag:$value")
        }
    }



    companion object {
        @JvmField
        val DEFAULT = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
    }
}


fun LanguageFeature.forcesPreReleaseBinariesIfEnabled(): Boolean {
    val isFeatureNotReleasedYet = sinceVersion?.isStable != true
    return isFeatureNotReleasedYet && kind.forcesPreReleaseBinaries
}


class ApiVersion private constructor(
    val version: String,
    override val versionString: String
) : Comparable<ApiVersion>, DescriptionAware, LanguageOrApiVersion {

    override val isStable: Boolean
        get() = this <= LATEST_STABLE

    override val isDeprecated: Boolean
        get() = FIRST_SUPPORTED <= this && this < FIRST_NON_DEPRECATED

    override val isUnsupported: Boolean
        get() = this < FIRST_SUPPORTED

    override fun compareTo(other: ApiVersion): Int =
        version.compareTo(other.version)

    override fun equals(other: Any?) =
        (other as? ApiVersion)?.version == version

    override fun hashCode() =
        version.hashCode()

    override fun toString() = versionString

    companion object {

        @JvmField
        val CANGJIE_0_53_4 = createByLanguageVersion(LanguageVersion.CANGJIE_0_53_4)

        @JvmField
        val LATEST: ApiVersion = createByLanguageVersion(LanguageVersion.entries.last())

        @JvmField
        val LATEST_STABLE: ApiVersion = createByLanguageVersion(LanguageVersion.LATEST_STABLE)

        @JvmField
        val FIRST_SUPPORTED: ApiVersion = createByLanguageVersion(LanguageVersion.FIRST_API_SUPPORTED)

        @JvmField
        val FIRST_NON_DEPRECATED: ApiVersion = createByLanguageVersion(LanguageVersion.FIRST_NON_DEPRECATED)

        @JvmStatic
        fun createByLanguageVersion(version: LanguageVersion): ApiVersion = parse(version.versionString)!!

        fun parse(versionString: String): ApiVersion? = try {
            ApiVersion(versionString, versionString)
        } catch (e: Exception) {
            null
        }
    }
}

class AnalysisFlag<out T> internal constructor(
    private val name: String,
    val defaultValue: T
) {
    override fun equals(other: Any?): Boolean = other is AnalysisFlag<*> && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    class Delegate<out T>(name: String, defaultValue: T) : ReadOnlyProperty<Any?, AnalysisFlag<T>> {
        private val flag = AnalysisFlag(name, defaultValue)

        override fun getValue(thisRef: Any?, property: KProperty<*>): AnalysisFlag<T> = flag
    }

    object Delegates {
        object Boolean {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Delegate(property.name, false)
        }

        object ApiModeDisabledByDefault {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) =
                Delegate(property.name, ExplicitApiMode.DISABLED)
        }

        object ListOfStrings {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) =
                Delegate(property.name, emptyList<String>())
        }
    }
}

enum class ExplicitApiMode(val state: String) {
    DISABLED("disable"),
    STRICT("strict"),
    WARNING("warning");

    companion object {
        fun fromString(string: String): ExplicitApiMode? = entries.find { it.state == string }

        fun availableValues() = values().joinToString(prefix = "{", postfix = "}") { it.state }
    }
}


object CangJieCompilerVersion {
    const val VERSION_FILE_PATH: String = "/META-INF/compiler.version"
    var VERSION: String? = null

    // True if the latest stable language version supported by this compiler has not yet been released.
    // Binaries produced by this compiler with that language version (or any future language version) are going to be marked
    // as "pre-release" and will not be loaded by release versions of the compiler.
    // Change this value before and after every major release
    private const val IS_PRE_RELEASE = true


    val version: String?
        /**
         * @return version of this compiler, or `null` if it isn't known (if VERSION is "@snapshot@")
         */
        get() = if (VERSION == "@snapshot@") null else VERSION

    @Throws(IOException::class)
    private fun loadCangJieCompilerVersion(): String {
        val versionReader = BufferedReader(
            InputStreamReader(CangJieCompilerVersion::class.java.getResourceAsStream(VERSION_FILE_PATH))
        )
        try {
            return versionReader.readLine()
        } finally {
            versionReader.close()
        }
    }

    init {
        try {
            VERSION = loadCangJieCompilerVersion()
        } catch (e: IOException) {
            throw IllegalStateException("Failed to read compiler version from " + VERSION_FILE_PATH)
        }

        check(!(VERSION != "@snapshot@" && !VERSION!!.contains("-") && IS_PRE_RELEASE)) {
            """
                IS_PRE_RELEASE cannot be true for a compiler without '-' in its version.
                Please change IS_PRE_RELEASE to false, commit and push this change to master
                """.trimIndent()
        }
    }
}

/**
 * Represents a version of the CangJie standard library.
 *
 * [major], [minor] and [patch] are integer components of a version,
 * they must be non-negative and not greater than 255 ([MAX_COMPONENT_VALUE]).
 *
 * @constructor Creates a version from all three components.
 */

class CangJieVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<CangJieVersion> {
    /**
     * Creates a version from [major] and [minor] components, leaving [patch] component zero.
     */
    constructor(major: Int, minor: Int) : this(major, minor, 0)

    private val version = versionOf(major, minor, patch)

    private fun versionOf(major: Int, minor: Int, patch: Int): Int {
        require(major in 0..MAX_COMPONENT_VALUE && minor in 0..MAX_COMPONENT_VALUE && patch in 0..MAX_COMPONENT_VALUE) {
            "Version components are out of range: $major.$minor.$patch"
        }
        return major.shl(16) + minor.shl(8) + patch
    }

    /**
     * Returns the string representation of this version
     */
    override fun toString(): String = "$major.$minor.$patch"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherVersion = (other as? CangJieVersion) ?: return false
        return this.version == otherVersion.version
    }

    override fun hashCode(): Int = version

    override fun compareTo(other: CangJieVersion): Int = version - other.version

    /**
     * Returns `true` if this version is not less than the version specified
     * with the provided [major] and [minor] components.
     */
    fun isAtLeast(major: Int, minor: Int): Boolean = // this.version >= versionOf(major, minor, 0)
        this.major > major || (this.major == major &&
                this.minor >= minor)

    /**
     * Returns `true` if this version is not less than the version specified
     * with the provided [major], [minor] and [patch] components.
     */
    fun isAtLeast(major: Int, minor: Int, patch: Int): Boolean =
        // this.version >= versionOf(major, minor, patch)
        this.major > major || (this.major == major &&
                (this.minor > minor || this.minor == minor &&
                        this.patch >= patch))

    companion object {
        /**
         * Maximum value a version component can have, a constant value 255.
         */
        // NOTE: Must be placed before CURRENT because its initialization requires this field being initialized in JS
        const val MAX_COMPONENT_VALUE = 255

        /**
         * Returns the current version of the CangJie standard library.
         */
        @JvmField
        val CURRENT: CangJieVersion = CangJieVersionCurrentValue.get()
    }
}

// this class is ignored during classpath normalization when considering whether to recompile dependencies in CangJie build
private object CangJieVersionCurrentValue {
    @JvmStatic
    fun get(): CangJieVersion = CangJieVersion(1, 9, 22) // value is written here automatically during build
}

