package com.linqingying.cangjie.metadata.internal


import com.linqingying.cangjie.metadata.*
import com.linqingying.cangjie.metadata.deserialization.*
import com.linqingying.cangjie.metadata.extensions.MetadataExtensions
import com.linqingying.cangjie.metadata.node.*
import kotlin.contracts.ExperimentalContracts
import com.linqingying.cangjie.metadata.deserialization.Flags as F

/**
 * Allows to populate [ReadContext] with additional data
 * that can be used when reading metadata in [MetadataExtensions].
 */
interface ReadContextExtension

class ReadContext(
    val strings: NameResolver,
    val types: TypeTable,
    @get:IgnoreInApiDump internal val versionRequirements: VersionRequirementTable,
    internal val ignoreUnknownVersionRequirements: Boolean,
    private val parent: ReadContext? = null,
    internal val contextExtensions: List<ReadContextExtension> = emptyList()
) {
    private val typeParameterNameToId = mutableMapOf<Int, Int>()

    internal val extensions = MetadataExtensions.INSTANCES

    operator fun get(index: Int): String =
        strings.getString(index)

    internal fun className(index: Int): ClassName =
        strings.getClassName(index)

    internal fun getTypeParameterId(name: Int): Int? =
        typeParameterNameToId[name] ?: parent?.getTypeParameterId(name)

    internal fun withTypeParameters(typeParameters: List<ProtoBuf.TypeParameter>): ReadContext =
        ReadContext(
            strings,
            types,
            versionRequirements,
            ignoreUnknownVersionRequirements,
            this,
            contextExtensions
        ).apply {
            for (typeParameter in typeParameters) {
                typeParameterNameToId[typeParameter.name] = typeParameter.id
            }
        }
}

@OptIn(ExperimentalContextReceivers::class)
fun ProtoBuf.Class.toCmClass(
    strings: NameResolver,
    ignoreUnknownVersionRequirements: Boolean = false,
    contextExtensions: List<ReadContextExtension> = emptyList(),
): CmClass {
    val v = CmClass()
    val c = ReadContext(
        strings,
        TypeTable(typeTable),
        VersionRequirementTable.create(versionRequirementTable),
        ignoreUnknownVersionRequirements,
        contextExtensions = contextExtensions
    ).withTypeParameters(typeParameterList)

    v.flags = flags
    v.name = c.className(fqName)

    typeParameterList.mapTo(v.typeParameters) { it.toCmTypeParameter(c) }
    supertypes(c.types).mapTo(v.supertypes) { it.toCmType(c) }
    constructorList.mapTo(v.constructors) { it.toCmConstructor(c) }
    v.visitDeclarations(functionList, propertyList, variableList, typeAliasList, c)
    if (hasCompanionObjectName()) {
        v.companionObject = c[companionObjectName]
    }

    nestedClassNameList.mapTo(v.nestedClasses) { c[it] }
    for (enumEntry in enumEntryList) {
        if (!enumEntry.hasName()) throw InconsistentCangJieMetadataException("No name for EnumEntry")
        v.enumEntries.add(c[enumEntry.name])
    }
    sealedSubclassFqNameList.mapTo(v.sealedSubclasses) { c.className(it) }
    if (hasInlineClassUnderlyingPropertyName()) {
        v.inlineClassUnderlyingPropertyName = c[inlineClassUnderlyingPropertyName]
    }
    v.inlineClassUnderlyingType = loadInlineClassUnderlyingType(c)?.toCmType(c)

    contextReceiverTypes(c.types).mapTo(v.contextReceiverTypes) { it.toCmType(c) }
    versionRequirementList.mapTo(v.versionRequirements) { readVersionRequirement(it, c) }

    c.extensions.forEach { it.readClassExtensions(v, this, c) }

    return v
}

private fun ProtoBuf.Class.loadInlineClassUnderlyingType(c: ReadContext): ProtoBuf.Type? {
    val type = inlineClassUnderlyingType(c.types)
    if (type != null) return type

    if (!hasInlineClassUnderlyingPropertyName()) return null

    // CangJie compiler doesn't write underlying type to metadata in case it can be loaded from the underlying property.
    return propertyList
        .singleOrNull { it.receiverType(c.types) == null && c[it.name] == c[inlineClassUnderlyingPropertyName] }
        ?.returnType(c.types)
}

fun ProtoBuf.Package.toCmPackage(
    strings: NameResolver,
    ignoreUnknownVersionRequirements: Boolean = false,
    contextExtensions: List<ReadContextExtension> = emptyList(),
): CmPackage {
    val v = CmPackage()
    val c = ReadContext(
        strings,
        TypeTable(typeTable),
        VersionRequirementTable.create(versionRequirementTable),
        ignoreUnknownVersionRequirements,
        contextExtensions = contextExtensions
    )

//    v.visitDeclarations(functionList, propertyList, typeAliasList, c)
    v.visitDeclarations(functionList, emptyList(), variableList, typeAliasList, c)

    c.extensions.forEach { it.readPackageExtensions(v, this, c) }

    return v
}

fun ProtoBuf.PackageFragment.toCmModuleFragment(
    strings: NameResolver,
    contextExtensions: List<ReadContextExtension> = emptyList(),
): CmModuleFragment {
    val v = CmModuleFragment()
    val c = ReadContext(
        strings,
        TypeTable(ProtoBuf.TypeTable.newBuilder().build()),
        VersionRequirementTable.EMPTY,
        false, // toCmModuleFragment is used for klib only
        contextExtensions = contextExtensions
    )

    v.pkg = `package`.toCmPackage(strings, false, contextExtensions)
    class_List.mapTo(v.classes) { it.toCmClass(strings, false, contextExtensions) }

    c.extensions.forEach { it.readModuleFragmentExtensions(v, this, c) }

    return v
}

private fun CmDeclarationContainer.visitDeclarations(
    protoFunctions: List<ProtoBuf.Function>,
    protoProperties: List<ProtoBuf.Property>,
    protoVariable: List<ProtoBuf.Variable>,
    protoTypeAliases: List<ProtoBuf.TypeAlias>,
    c: ReadContext,
) {
    protoFunctions.mapTo(functions) { it.toCmFunction(c) }
    protoVariable.mapTo(variables) { it.toCmVariable(c) }
    protoProperties.mapTo(properties) { it.toCmProperty(c) }
    protoTypeAliases.mapTo(typeAliases) { it.toCmTypeAlias(c) }
}

fun ProtoBuf.Function.toCmLambda(
    strings: NameResolver,
    ignoreUnknownVersionRequirements: Boolean = false
): CmLambda {
    val v = CmLambda()
    val c = ReadContext(strings, TypeTable(typeTable), VersionRequirementTable.EMPTY, ignoreUnknownVersionRequirements)
    v.function = this.toCmFunction(c)
    return v
}

private fun ProtoBuf.Constructor.toCmConstructor(c: ReadContext): CmConstructor {
    val v = CmConstructor(flags)
    valueParameterList.mapTo(v.valueParameters) { it.toCmValueParameter(c) }
    versionRequirementList.mapTo(v.versionRequirements) { readVersionRequirement(it, c) }

    c.extensions.forEach { it.readConstructorExtensions(v, this, c) }

    return v
}

@OptIn(ExperimentalContextReceivers::class)
private fun ProtoBuf.Function.toCmFunction(outer: ReadContext): CmFunction {
    val v = CmFunction(flags, outer[name])
    val c = outer.withTypeParameters(typeParameterList)

    typeParameterList.mapTo(v.typeParameters) { it.toCmTypeParameter(c) }
    v.receiverParameterType = receiverType(c.types)?.toCmType(c)
    contextReceiverTypes(c.types).mapTo(v.contextReceiverTypes) { it.toCmType(c) }
    valueParameterList.mapTo(v.valueParameters) { it.toCmValueParameter(c) }
    v.returnType = returnType(c.types).toCmType(c)

//    @OptIn(ExperimentalContracts::class)
//    if (hasContract()) {
//        v.contract = contract.toCmContract(c)
//    }

    versionRequirementList.mapTo(v.versionRequirements) { readVersionRequirement(it, c) }

    c.extensions.forEach { it.readFunctionExtensions(v, this, c) }

    return v
}

@OptIn(ExperimentalContextReceivers::class)
fun ProtoBuf.Variable.toCmVariable(outer: ReadContext): CmVariable {
    val v = CmVariable(flags, outer[name])
    val c = outer.withTypeParameters(typeParameterList)

    typeParameterList.mapTo(v.typeParameters) { it.toCmTypeParameter(c) }
    v.receiverParameterType = receiverType(c.types)?.toCmType(c)
    contextReceiverTypes(c.types).mapTo(v.contextReceiverTypes) { it.toCmType(c) }
    if (hasSetterValueParameter()) {
        v.setterParameter = setterValueParameter.toCmValueParameter(c)
    }
    v.returnType = returnType(c.types).toCmType(c)
    versionRequirementList.mapTo(v.versionRequirements) { readVersionRequirement(it, c) }

    c.extensions.forEach { it.readVariableExtensions(v, this, c) }

    return v
}

@OptIn(ExperimentalContextReceivers::class)
fun ProtoBuf.Property.toCmProperty(outer: ReadContext): CmProperty {
    val v = CmProperty(flags, outer[name], getPropertyGetterFlags(), getPropertySetterFlags())
    val c = outer.withTypeParameters(typeParameterList)

    typeParameterList.mapTo(v.typeParameters) { it.toCmTypeParameter(c) }
    v.receiverParameterType = receiverType(c.types)?.toCmType(c)
    contextReceiverTypes(c.types).mapTo(v.contextReceiverTypes) { it.toCmType(c) }
    if (hasSetterValueParameter()) {
        v.setterParameter = setterValueParameter.toCmValueParameter(c)
    }
    v.returnType = returnType(c.types).toCmType(c)
    versionRequirementList.mapTo(v.versionRequirements) { readVersionRequirement(it, c) }

    c.extensions.forEach { it.readPropertyExtensions(v, this, c) }

    return v
}

private fun ProtoBuf.TypeAlias.toCmTypeAlias(outer: ReadContext): CmTypeAlias {
    val v = CmTypeAlias(flags, outer[name])

    val c = outer.withTypeParameters(typeParameterList)

    typeParameterList.mapTo(v.typeParameters) { it.toCmTypeParameter(c) }
    v.underlyingType = underlyingType(c.types).toCmType(c)
    v.expandedType = expandedType(c.types).toCmType(c)
    annotationList.mapTo(v.annotations) { it.readAnnotation(c.strings) }

    versionRequirementList.mapTo(v.versionRequirements) { readVersionRequirement(it, c) }

    c.extensions.forEach { it.readTypeAliasExtensions(v, this, c) }

    return v
}

private fun ProtoBuf.ValueParameter.toCmValueParameter(c: ReadContext): CmValueParameter {
    val v = CmValueParameter(flags, c[name])
    v.type = type(c.types).toCmType(c)

    v.varargElementType = varargElementType(c.types)?.toCmType(c)

    c.extensions.forEach { it.readValueParameterExtensions(v, this, c) }

    return v
}

private fun ProtoBuf.TypeParameter.toCmTypeParameter(
    c: ReadContext,
): CmTypeParameter {
    val variance = when (requireNotNull(variance)) {

        ProtoBuf.TypeParameter.Variance.INV -> CmVariance.INVARIANT
    }
    val ktp = CmTypeParameter(typeParameterFlags, c[name], id, variance)

    upperBounds(c.types).mapTo(ktp.upperBounds) { it.toCmType(c) }

    c.extensions.forEach { it.readTypeParameterExtensions(ktp, this, c) }

    return ktp
}

private fun ProtoBuf.Type.toCmType(c: ReadContext): CmType {
    val v = CmType(typeFlags)
    v.classifier = when {
        hasClassName() -> CmClassifier.Class(c.className(className))
        hasTypeAliasName() -> CmClassifier.TypeAlias(c.className(typeAliasName))
        hasTypeParameter() -> CmClassifier.TypeParameter(typeParameter)
        hasTypeParameterName() -> {
            val id = c.getTypeParameterId(typeParameterName)
                ?: throw InconsistentCangJieMetadataException("No type parameter id for ${c[typeParameterName]}")
            CmClassifier.TypeParameter(id)
        }

        else -> throw InconsistentCangJieMetadataException("No classifier (class, type alias or type parameter) recorded for Type")
    }

    for (argument in argumentList) {
        val variance = when (requireNotNull(argument.projection)) {

            ProtoBuf.Type.Argument.Projection.INV -> CmVariance.INVARIANT

        }

        val argumentType = argument.type(c.types)
            ?: throw InconsistentCangJieMetadataException("No type argument for non-STAR projection in Type")
        v.arguments.add(CmTypeProjection(variance, argumentType.toCmType(c)))
    }

    v.abbreviatedType = abbreviatedType(c.types)?.toCmType(c)
    v.outerType = outerType(c.types)?.toCmType(c)

    v.flexibleTypeUpperBound = flexibleUpperBound(c.types)?.toCmType(c)?.let {
        CmFlexibleTypeUpperBound(it, if (hasFlexibleTypeCapabilitiesId()) c[flexibleTypeCapabilitiesId] else null)
    }

    c.extensions.forEach { it.readTypeExtensions(v, this, c) }

    return v
}

private fun readVersionRequirement(id: Int, c: ReadContext): CmVersionRequirement {
    val v = CmVersionRequirement()
    val message = VersionRequirement.create(id, c.strings, c.versionRequirements)
    if (message == null && !c.ignoreUnknownVersionRequirements) throw InconsistentCangJieMetadataException("No VersionRequirement with the given id in the table")

    val kind = when (message?.kind) {
        ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION -> CmVersionRequirementVersionKind.LANGUAGE_VERSION
        ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION -> CmVersionRequirementVersionKind.COMPILER_VERSION
        ProtoBuf.VersionRequirement.VersionKind.API_VERSION -> CmVersionRequirementVersionKind.API_VERSION
        null -> CmVersionRequirementVersionKind.UNKNOWN
    }

    val level = when (message?.level) {
        DeprecationLevel.WARNING -> CmVersionRequirementLevel.WARNING
        DeprecationLevel.ERROR -> CmVersionRequirementLevel.ERROR
        DeprecationLevel.HIDDEN, null -> CmVersionRequirementLevel.HIDDEN
    }

    v.kind = kind
    v.level = level
    v.errorCode = message?.errorCode
    v.message = message?.message

    val (major, minor, patch) = message?.version ?: VersionRequirement.Version.INFINITY
    v.version = CmVersion(major, minor, patch)
    return v
}

@ExperimentalContracts
private fun ProtoBuf.Contract.toCmContract(c: ReadContext): CmContract {
    val v = CmContract()
    for (effect in effectList) {
        if (!effect.hasEffectType()) continue

        val effectType = when (requireNotNull(effect.effectType)) {
            ProtoBuf.Effect.EffectType.RETURNS_CONSTANT -> CmEffectType.RETURNS_CONSTANT
            ProtoBuf.Effect.EffectType.CALLS -> CmEffectType.CALLS
            ProtoBuf.Effect.EffectType.RETURNS_NOT_NULL -> CmEffectType.RETURNS_NOT_NULL
        }

        val effectKind = if (!effect.hasKind()) null else when (requireNotNull(effect.kind)) {
            ProtoBuf.Effect.InvocationKind.AT_MOST_ONCE -> CmEffectInvocationKind.AT_MOST_ONCE
            ProtoBuf.Effect.InvocationKind.EXACTLY_ONCE -> CmEffectInvocationKind.EXACTLY_ONCE
            ProtoBuf.Effect.InvocationKind.AT_LEAST_ONCE -> CmEffectInvocationKind.AT_LEAST_ONCE
        }

        v.effects.add(effect.toCmEffect(effectType, effectKind, c))
    }

    return v
}

@ExperimentalContracts
private fun ProtoBuf.Effect.toCmEffect(type: CmEffectType, kind: CmEffectInvocationKind?, c: ReadContext): CmEffect {
    val v = CmEffect(type, kind)
    effectConstructorArgumentList.mapTo(v.constructorArguments) { it.toCmEffectExpression(c) }

    if (hasConclusionOfConditionalEffect()) {
        v.conclusion = conclusionOfConditionalEffect.toCmEffectExpression(c)
    }

    return v
}

@ExperimentalContracts
private fun ProtoBuf.Expression.toCmEffectExpression(c: ReadContext): CmEffectExpression {
    val v = CmEffectExpression()
    v.flags = flags
    v.parameterIndex = if (hasValueParameterReference()) valueParameterReference else null

    if (hasConstantValue()) {
        v.constantValue = CmConstantValue(
            when (requireNotNull(constantValue)) {
                ProtoBuf.Expression.ConstantValue.TRUE -> true
                ProtoBuf.Expression.ConstantValue.FALSE -> false
                ProtoBuf.Expression.ConstantValue.NULL -> null
            }
        )
    }

    v.isInstanceType = isInstanceType(c.types)?.toCmType(c)

    andArgumentList.mapTo(v.andArguments) { it.toCmEffectExpression(c) }
    orArgumentList.mapTo(v.orArguments) { it.toCmEffectExpression(c) }

    return v
}

private val ProtoBuf.Type.typeFlags: Int
    get() = (if (nullable) 1 shl 0 else 0) +
            (flags shl 1)

private val ProtoBuf.TypeParameter.typeParameterFlags: Int
    get() = if (reified) 1 else 0

fun ProtoBuf.Property.getPropertyGetterFlags(): Int =
    if (hasGetterFlags()) getterFlags else getDefaultPropertyAccessorFlags(flags)

fun ProtoBuf.Property.getPropertySetterFlags(): Int =
    if (hasSetterFlags()) setterFlags else getDefaultPropertyAccessorFlags(flags)

internal fun getDefaultPropertyAccessorFlags(flags: Int): Int =
    F.getAccessorFlags(
        F.HAS_ANNOTATIONS.get(flags),
        F.VISIBILITY.get(flags),
        F.MODALITY.get(flags),
        false,

    )
