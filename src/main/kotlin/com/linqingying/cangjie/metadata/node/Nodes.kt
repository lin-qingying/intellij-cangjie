@file:Suppress("MemberVisibilityCanBePrivate")

package com.linqingying.cangjie.metadata.node


import com.linqingying.cangjie.metadata.CmAnnotation
import com.linqingying.cangjie.metadata.ExperimentalContextReceivers
import com.linqingying.cangjie.metadata.deserialization.Flags
import com.linqingying.cangjie.metadata.extensions.*
import com.linqingying.cangjie.metadata.extensions.MetadataExtensions
import com.linqingying.cangjie.metadata.internal.FlagImpl
import com.linqingying.cangjie.metadata.internal.propertyBooleanFlag

/**
 * Represents a Kotlin declaration container, such as a class or a package fragment.
 */
interface CmDeclarationContainer {
    /**
     * Functions in the container.
     */
    val functions: MutableList<CmFunction>
    /**
     * Properties in the container.
     */
    val variables: MutableList<CmVariable>

    /**
     * Properties in the container.
     */
    val properties: MutableList<CmProperty>

    /**
     * Type aliases in the container.
     */
    val typeAliases: MutableList<CmTypeAlias>
}

/**
 * Represents a Kotlin class.
 *
 * 'Class' here is used in a broad sense and includes interfaces, enum classes, companion objects, et cetera.
 * Precise kind of the class can be obtained via [CmClass.kind].
 * Various class attributes can be read and manipulated via extension properties, such as [CmClass.visibility] or [CmClass.isData].
 */
class CmClass : CmDeclarationContainer {
    internal var flags: Int = 0

    /**
     * Name of the class.
     */
    lateinit var name: ClassName

    /**
     * Type parameters of the class.
     */
    val typeParameters: MutableList<CmTypeParameter> = ArrayList(0)

    /**
     * Supertypes of the class.
     */
    val supertypes: MutableList<CmType> = ArrayList(1)

    /**
     * Functions in the class.
     */
    override val functions: MutableList<CmFunction> = ArrayList()
    override val variables: MutableList<CmVariable> = ArrayList()

    /**
     * Properties in the class.
     */
    override val properties: MutableList<CmProperty> = ArrayList()

    /**
     * Type aliases in the class.
     */
    override val typeAliases: MutableList<CmTypeAlias> = ArrayList(0)

    /**
     * Constructors of the class.
     */
    val constructors: MutableList<CmConstructor> = ArrayList(1)

    /**
     * Name of the companion object of this class, if it has one.
     */
    var companionObject: String? = null

    /**
     * Names of nested classes of this class.
     */
    val nestedClasses: MutableList<String> = ArrayList(0)

    /**
     * Names of enum entries, if this class is an enum class.
     */
    val enumEntries: MutableList<String> = ArrayList(0)

    /**
     * Names of direct subclasses of this class, if this class is `sealed`.
     */
    val sealedSubclasses: MutableList<ClassName> = ArrayList(0)

    /**
     * Name of the underlying property, if this class is `inline`.
     */
    var inlineClassUnderlyingPropertyName: String? = null

    /**
     * Type of the underlying property, if this class is `inline`.
     */
    var inlineClassUnderlyingType: CmType? = null

    /**
     * Types of context receivers of the class.
     */
    @ExperimentalContextReceivers
    val contextReceiverTypes: MutableList<CmType> = ArrayList(0)

    /**
     * Version requirements on this class.
     */
    val versionRequirements: MutableList<CmVersionRequirement> = ArrayList(0)

    internal val extensions: List<CmClassExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createClassExtension)
}

/**
 * Represents a Kotlin package fragment that contains top-level functions, properties, and type aliases.
 * Package fragments are produced from single file facades and multi-file class parts.
 * Note that a package fragment does not contain any classes, as classes are not a part of file facades and have their own metadata.
 */
class CmPackage : CmDeclarationContainer {
    /**
     * Functions in the package fragment.
     */
    override val functions: MutableList<CmFunction> = ArrayList()

    /**
     * Properties in the package fragment.
     */
    override val variables: MutableList<CmVariable> = ArrayList()

    /**
     * Properties in the package fragment.
     */
    override val properties: MutableList<CmProperty> = ArrayList()

    /**
     * Type aliases in the package fragment.
     */
    override val typeAliases: MutableList<CmTypeAlias> = ArrayList(0)

    internal val extensions: List<CmPackageExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createPackageExtension)
}

/**
 * Represents a synthetic class generated for a Kotlin lambda.
 */
class CmLambda {
    /**
     * Signature of the synthetic anonymous function, representing the lambda.
     */
    lateinit var function: CmFunction
}

/**
 * Represents a constructor of a Kotlin class.
 *
 * Various constructor attributes can be read and manipulated via extension properties,
 * such as [CmConstructor.visibility] or [CmConstructor.isSecondary].
 */
class CmConstructor internal constructor(internal var flags: Int) {
    constructor() : this(0)

    /**
     * Value parameters of the constructor.
     */
    val valueParameters: MutableList<CmValueParameter> = ArrayList()

    /**
     * Version requirements on the constructor.
     */
    val versionRequirements: MutableList<CmVersionRequirement> = ArrayList(0)

    internal val extensions: List<CmConstructorExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createConstructorExtension)
}

/**
 * Represents a Kotlin function declaration.
 *
 * Various function attributes can be read and manipulated via extension properties,
 * such as [CmFunction.visibility] or [CmFunction.isSuspend].
 *
 * @property name the name of the function
 */
class CmFunction internal constructor(internal var flags: Int, var name: String) {

    constructor(name: String) : this(0, name)

    /**
     * Type parameters of the function.
     */
    val typeParameters: MutableList<CmTypeParameter> = ArrayList(0)

    /**
     * Type of the receiver of the function, if this is an extension function.
     */
    var receiverParameterType: CmType? = null

    /**
     * Types of context receivers of the function.
     */
    @ExperimentalContextReceivers
    val contextReceiverTypes: MutableList<CmType> = ArrayList(0)

    /**
     * Value parameters of the function.
     */
    val valueParameters: MutableList<CmValueParameter> = ArrayList()

    /**
     * Return type of the function.
     */
    lateinit var returnType: CmType

    /**
     * Version requirements on the function.
     */
    val versionRequirements: MutableList<CmVersionRequirement> = ArrayList(0)


    internal val extensions: List<CmFunctionExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createFunctionExtension)
}

/**
 * Represents a Kotlin property accessor.
 *
 * Does not contain meaningful information except attributes, such as visibility and modality.
 * Attributes can be read and written using extension properties, e.g. [CmPropertyAccessorAttributes.visibility] or [CmPropertyAccessorAttributes.isNotDefault].
 */
class CmPropertyAccessorAttributes internal constructor(internal var flags: Int) {
    constructor() : this(0)
}
/**
 * Represents a Kotlin property declaration.
 *
 * Various property attributes can be read and manipulated via extension properties,
 * such as [CmProperty.visibility] or [CmProperty.isVar].
 *
 * Getter and setter attributes are available separately via extensions on [CmProperty.getter] and [CmProperty.setter] correspondingly.
 *
 * @property name the name of the property
 */
class CmVariable internal constructor(
    internal var flags: Int,
    var name: String,

) {
    constructor(name: String) : this(0, name )




    /**
     * Type parameters of the property.
     */
    val typeParameters: MutableList<CmTypeParameter> = ArrayList(0)

    /**
     * Type of the receiver of the property, if this is an extension property.
     */
    var receiverParameterType: CmType? = null

    /**
     * Types of context receivers of the property.
     */
    @ExperimentalContextReceivers
    val contextReceiverTypes: MutableList<CmType> = ArrayList(0)

    /**
     * Value parameter of the setter of this property, if this is a `var` property and parameter is present.
     * Parameter is present if and only if the setter is not default:
     *
     * ```kotlin
     * var foo: String = ""
     *   set(param) {
     *     field = param.removePrefix("bar")
     *   }
     * ```
     */
    var setterParameter: CmValueParameter? = null

    /**
     * Type of the property.
     */
    lateinit var returnType: CmType

    /**
     * Version requirements on the property.
     */
    val versionRequirements: MutableList<CmVersionRequirement> = ArrayList(0)

    internal val extensions: List<CmPropertyExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createPropertyExtension)
}
/**
 * Represents a Kotlin property declaration.
 *
 * Various property attributes can be read and manipulated via extension properties,
 * such as [CmProperty.visibility] or [CmProperty.isVar].
 *
 * Getter and setter attributes are available separately via extensions on [CmProperty.getter] and [CmProperty.setter] correspondingly.
 *
 * @property name the name of the property
 */
class CmProperty internal constructor(
    internal var flags: Int,
    var name: String,
    getterFlags: Int,
    setterFlags: Int,
) {
    constructor(name: String) : this(0, name, 0, 0)

    // needed for reading/writing flags back to protobuf as a whole pack
    private var _hasSetter: Boolean by propertyBooleanFlag(FlagImpl(Flags.HAS_SETTER))
    private var _hasGetter: Boolean by propertyBooleanFlag(FlagImpl(Flags.HAS_GETTER))

    /**
     * Attributes of the getter of this property.
     * Attributes can be retrieved with extension properties, such as [CmPropertyAccessorAttributes.visibility] or [CmPropertyAccessorAttributes.isNotDefault].
     *
     * Getter for property is always present, therefore, the type of this property is non-nullable.
     */
    val getter: CmPropertyAccessorAttributes =
        CmPropertyAccessorAttributes(getterFlags).also { _hasGetter = true }

    /**
     * Attributes of the setter of this property.
     * Attributes can be retrieved with extension properties, such as [CmPropertyAccessorAttributes.visibility] or [CmPropertyAccessorAttributes.isNotDefault].
     *
     * Returns null if setter is absent, i.e., [CmProperty.isVar] is false.
     *
     * Note that setting [CmProperty.isVar] to true does not automatically create [CmProperty.setter] and vice versa. This has to be done explicitly.
     */
    var setter: CmPropertyAccessorAttributes? =
        if (this._hasSetter) CmPropertyAccessorAttributes(setterFlags) else null
        set(new) {
            this._hasSetter = new != null
            field = new
        }

    /**
     * Type parameters of the property.
     */
    val typeParameters: MutableList<CmTypeParameter> = ArrayList(0)

    /**
     * Type of the receiver of the property, if this is an extension property.
     */
    var receiverParameterType: CmType? = null

    /**
     * Types of context receivers of the property.
     */
    @ExperimentalContextReceivers
    val contextReceiverTypes: MutableList<CmType> = ArrayList(0)

    /**
     * Value parameter of the setter of this property, if this is a `var` property and parameter is present.
     * Parameter is present if and only if the setter is not default:
     *
     * ```kotlin
     * var foo: String = ""
     *   set(param) {
     *     field = param.removePrefix("bar")
     *   }
     * ```
     */
    var setterParameter: CmValueParameter? = null

    /**
     * Type of the property.
     */
    lateinit var returnType: CmType

    /**
     * Version requirements on the property.
     */
    val versionRequirements: MutableList<CmVersionRequirement> = ArrayList(0)

    internal val extensions: List<CmPropertyExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createPropertyExtension)
}

/**
 * Represents a Kotlin type alias declaration.
 *
 * Various type alias attributes can be read and manipulated via extension properties,
 * such as [CmTypeAlias.visibility] or [CmTypeAlias.hasAnnotations].
 *
 * @property name the name of the type alias
 */
class CmTypeAlias internal constructor(
    internal var flags: Int,
    var name: String,
) {

    constructor(name: String) : this(0, name)

    /**
     * Type parameters of the type alias.
     */
    val typeParameters: MutableList<CmTypeParameter> = ArrayList(0)

    /**
     * Underlying type of the type alias, i.e., the type in the right-hand side of the type alias declaration.
     */
    lateinit var underlyingType: CmType

    /**
     * Expanded type of the type alias, i.e., the full expansion of the underlying type, where all type aliases are substituted
     * with their expanded types. If no type aliases are used in the underlying type, the expanded type is equal to the underlying type.
     */
    lateinit var expandedType: CmType

    /**
     * Annotations on the type alias.
     */
    val annotations: MutableList<CmAnnotation> = ArrayList(0)

    /**
     * Version requirements on the type alias.
     */
    val versionRequirements: MutableList<CmVersionRequirement> = ArrayList(0)

    internal val extensions: List<CmTypeAliasExtension> =
        MetadataExtensions.INSTANCES.mapNotNull(MetadataExtensions::createTypeAliasExtension)
}

/**
 * Represents a value parameter of a Kotlin constructor, function, or property setter.
 *
 * Various value parameter attributes can be read and manipulated via extension properties,
 * such as [CmValueParameter.declaresDefaultValue].
 *
 * @property name the name of the value parameter
 */
class CmValueParameter internal constructor(
    internal var flags: Int,
    var name: String,
) {

    constructor(name: String) : this(0, name)

    /**
     * Type of the value parameter.
     * If this is a `vararg` parameter of type `X`, returns the type `Array<out X>`.
     */
    lateinit var type: CmType

    /**
     * Type of the `vararg` value parameter, or `null` if this is not a `vararg` parameter.
     */
    var varargElementType: CmType? = null

    internal val extensions: List<CmValueParameterExtension> =
        MetadataExtensions.INSTANCES.mapNotNull(MetadataExtensions::createValueParameterExtension)
}

/**
 * Represents a type parameter of a Kotlin class, function, property, or type alias.
 *
 * Various type parameter attributes can be read and manipulated via extension properties,
 * such as [CmTypeParameter.isReified].
 *
 * @property name the name of the type parameter
 * @property id the id of the type parameter, useful to be able to uniquely identify the type parameter in different contexts where
 *           the name is not enough (e.g. `class A<T> { fun <T> foo(t: T) }`)
 * @property variance the declaration-site variance of the type parameter
 */
class CmTypeParameter internal constructor(
    internal var flags: Int,
    var name: String,
    var id: Int,
    var variance: CmVariance,
) {

    constructor(name: String, id: Int, variance: CmVariance) : this(0, name, id, variance)

    /**
     * Upper bounds of the type parameter.
     */
    val upperBounds: MutableList<CmType> = ArrayList(1)

    internal val extensions: List<CmTypeParameterExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createTypeParameterExtension)
}

/**
 * Represents a type.
 *
 * Various type attributes can be read and manipulated via extension properties,
 * such as [CmType.isNullable].
 */
class CmType internal constructor(
    internal var flags: Int,
) {

    constructor() : this(0)

    /**
     * Classifier of the type.
     */
    lateinit var classifier: CmClassifier

    /**
     * Arguments of the type, if the type's classifier is a class or a type alias.
     */
    val arguments: MutableList<CmTypeProjection> = ArrayList(0)

    /**
     * Abbreviation of this type. Note that all types are expanded for metadata produced by the Kotlin compiler. For example:
     *
     *     typealias A<T> = MutableList<T>
     *
     *     fun foo(a: A<Any>) {}
     *
     * The type of the `foo`'s parameter in the metadata is actually `MutableList<Any>`, and its abbreviation is `A<Any>`.
     */
    var abbreviatedType: CmType? = null

    /**
     * Outer type of this type, if this type's classifier is an inner class. For example:
     *
     *     class A<T> { inner class B<U> }
     *
     *     fun foo(a: A<*>.B<Byte?>) {}
     *
     * The type of the `foo`'s parameter in the metadata is `B<Byte>` (a type whose classifier is class `B`, and it has one type argument,
     * type `Byte?`), and its outer type is `A<*>` (a type whose classifier is class `A`, and it has one type argument, star projection).
     */
    var outerType: CmType? = null

    /**
     * Upper bound of this type, if this type is flexible. In that case, all other data refers to the lower bound of the type.
     *
     * Flexible types in Kotlin include platform types in Kotlin/JVM and `dynamic` type in Kotlin/JS.
     */
    var flexibleTypeUpperBound: CmFlexibleTypeUpperBound? = null

    internal val extensions: List<CmTypeExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createTypeExtension)

    /**
     * Determines whether this CmType is equal to the given [other].
     *
     * CmTypes are compared using structural equality, i.e., two objects are considered equal
     * if they have all the following parts equal:
     * attributes (such as [isNullable] and [isSuspend]), [classifier],
     * [arguments], [outerType], [abbreviatedType], [flexibleTypeUpperBound],
     * and all platform extensions (such as annotations on JVM).
     *
     * Note that equality of [CmType] instances differs from the concept of type equality in the Kotlin language.
     * In the language, types A and B are equal if `A.isSubtypeOf(B) && B.isSubtypeOf(A)`.
     * Since kotlin-metadata-jvm does not provide subtyping algorithms (or any kind of type inference algorithms whatsoever),
     * [CmType] equality adheres to a comparison of what is written to the metadata.
     * For example, flexible types are not considered equal to their lower and upper bounds —
     * which means that `String?` and `String!` are not equal CmTypes, despite being freely assignable from each other
     * in the Kotlin language.
     *
     * @param other The object to compare for equality.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CmType

        if (flags != other.flags) return false
        if (classifier != other.classifier) return false
        if (arguments != other.arguments) return false
        if (outerType != other.outerType) return false
        if (abbreviatedType != other.abbreviatedType) return false
        if (flexibleTypeUpperBound != other.flexibleTypeUpperBound) return false
        if (extensions != other.extensions) return false

        return true
    }

    /**
     * Computes the hash code of the CmType object using its properties.
     *
     * @return The computed hash code of the CmType object.
     */
    override fun hashCode(): Int {
        var result = flags
        result = 31 * result + classifier.hashCode()
        result = 31 * result + arguments.hashCode()
        /**
         * outerType, abbreviatedType, and flexibleTypeUpperBound are omitted, so we can compute hash code faster
         * with a trade-off for rare collisions.
         */
        return result
    }
}

/**
 * Represents a version requirement on a Kotlin declaration.
 *
 * Version requirement is an internal feature of the Kotlin compiler and the standard Kotlin library,
 * enabled, for example, with the internal [kotlin.internal.RequireKotlin] annotation.
 */
class CmVersionRequirement {
    /**
     * Kind of the version that this declaration requires.
     */
    lateinit var kind: CmVersionRequirementVersionKind

    /**
     * Level of the diagnostic that must be reported on the usages of the declaration in case the version requirement is not satisfied.
     */
    lateinit var level: CmVersionRequirementLevel

    /**
     * Optional error code to be displayed in the diagnostic.
     */
    var errorCode: Int? = null

    /**
     * Optional message to be displayed in the diagnostic.
     */
    var message: String? = null

    /**
     * Version required by this requirement.
     */
    lateinit var version: CmVersion

    /**
     * Returns the String representation of this CmVersionRequirement object, consisting of
     * [kind], [level], [version], [errorCode], and [message].
     */
    override fun toString(): String {
        return "CmVersionRequirement(kind=$kind, level=$level, version=$version, errorCode=$errorCode, message=$message)"
    }
}

/**
 * Represents a classifier of a Kotlin type. A classifier is a class, type parameter, or type alias.
 * For example, in `MutableMap<in String?, *>`, `MutableMap` is the classifier.
 */
sealed class CmClassifier {
    /**
     * Represents a class used as a classifier in a type.
     *
     * @property name the name of the class
     */
    data class Class(val name: ClassName) : CmClassifier()

    /**
     * Represents a type parameter used as a classifier in a type.
     *
     * @property id id of the type parameter
     */
    data class TypeParameter(val id: Int) : CmClassifier()

    /**
     * Represents a type alias used as a classifier in a type. Note that all types are expanded for metadata produced
     * by the Kotlin compiler, so the type with a type alias classifier may only appear in [CmType.abbreviatedType].
     *
     * @property name the name of the type alias
     */
    data class TypeAlias(val name: ClassName) : CmClassifier()
}

/**
 * Represents type projection used in a type argument of the type based on a class or on a type alias.
 * For example, in `MutableMap<in String?, *>`, `in String?` is the type projection which is the first type argument of the type.
 *
 * @property variance the variance of the type projection, or `null` if this is a star projection
 * @property type the projected type, or `null` if this is a star projection
 */
data class CmTypeProjection(var variance: CmVariance?, var type: CmType?) {
    /**
     * Contains default instance for star projection: [CmTypeProjection.STAR].
     */
    companion object {
        /**
         * Star projection (`*`).
         * For example, in `MutableMap<in String?, *>`, `*` is the star projection which is the second type argument of the type.
         */
        @JvmField
        val STAR: CmTypeProjection = CmTypeProjection(null, null)
    }
}

/**
 * Represents an upper bound of a flexible Kotlin type.
 *
 * @property type upper bound of the flexible type
 * @property typeFlexibilityId id of the kind of flexibility this type has. For example, "kotlin.jvm.PlatformType" for JVM platform types,
 *                          or "kotlin.DynamicType" for JS dynamic type
 */
data class CmFlexibleTypeUpperBound(var type: CmType, var typeFlexibilityId: String?) {
    /**
     * A companion object providing possibility to declare various platform-dependent constant ids as extension properties of it.
     */
    companion object
}

/**
 * Variance applied to a type parameter on the declaration site (*declaration-site variance*),
 * or to a type in a projection (*use-site variance*).
 */
enum class CmVariance {
    /**
     * The affected type parameter or type is *invariant*, which means it has no variance applied to it.
     */
    INVARIANT,

    /**
     * The affected type parameter or type is *contravariant*. Denoted by the `in` modifier in the source code.
     */
    IN,

    /**
     * The affected type parameter or type is *covariant*. Denoted by the `out` modifier in the source code.
     */
    OUT,
}

/**
 * Represents a version used in a version requirement.
 *
 * @property major the major component of the version (e.g. "1" in "1.2.3")
 * @property minor the minor component of the version (e.g. "2" in "1.2.3")
 * @property patch the patch component of the version (e.g. "3" in "1.2.3")
 */
data class CmVersion(val major: Int, val minor: Int, val patch: Int) {

    /**
     * Returns a string representation of this version in "$major.$minor.$patch" form.
     */
    override fun toString(): String = "$major.$minor.$patch"
}

/**
 * Severity of the diagnostic reported by the compiler when a version requirement is not satisfied.
 */
enum class CmVersionRequirementLevel {
    /**
     * Represents a diagnostic with 'WARNING' severity.
     */
    WARNING,

    /**
     * Represents a diagnostic with 'ERROR' severity.
     */
    ERROR,

    /**
     * Excludes the declaration from the resolution process completely when the version requirement is not satisfied.
     */
    HIDDEN,
}

/**
 * The kind of the version that is required by a version requirement.
 */
enum class CmVersionRequirementVersionKind {
    /**
     * Indicates that a certain language version is required.
     */
    LANGUAGE_VERSION,

    /**
     * Indicates that a certain compiler version is required.
     */
    COMPILER_VERSION,

    /**
     * Indicates that a certain API version is required.
     */
    API_VERSION,

    /**
     * Represents a version requirement not successfully parsed from the metadata.
     *
     * The old metadata format (from Kotlin 1.3 and earlier) did not have enough information for correct parsing of version requirements in some cases,
     * so a stub of this kind is inserted instead.
     *
     * [CmVersionRequirement] with this kind always has [CmVersionRequirementLevel.HIDDEN] level, `256.256.256` [CmVersionRequirement.version],
     * and `null` [CmVersionRequirement.errorCode] and [CmVersionRequirement.message].
     *
     * Version requirements of this kind are being ignored by writers (i.e., are not written back).
     *
     */
    UNKNOWN
    ;
}

/**
 * Represents a Kotlin module fragment.
 *
 * Do not confuse with `KmModule`: while KmModule represents JVM-specific `.kotlin_module` file, KmModuleFragment is not platform-specific.
 * It usually represents metadata serialized to klib or part of klib,
 * but also may represent a special `.kotlin_builtins` file that can be encountered only in standard library.
 *
 * Can be read with [KotlinCommonMetadata.read].
 */
class   CmModuleFragment {

    /**
     * Top-level functions, type aliases and properties in the module fragment.
     */
    var pkg: CmPackage? = null

    /**
     * Classes in the module fragment.
     */
    val classes: MutableList<CmClass> = ArrayList()

    internal val extensions: List<CmModuleFragmentExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createModuleFragmentExtensions)
}
