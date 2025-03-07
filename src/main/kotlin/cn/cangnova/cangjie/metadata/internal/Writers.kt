/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.metadata.internal

import cn.cangnova.cangjie.metadata.*
import cn.cangnova.cangjie.metadata.deserialization.VersionRequirement
import cn.cangnova.cangjie.metadata.extensions.MetadataExtensions
import cn.cangnova.cangjie.metadata.node.*
import cn.cangnova.cangjie.metadata.serialization.MutableVersionRequirementTable
import cn.cangnova.cangjie.metadata.serialization.StringTable
import kotlin.contracts.ExperimentalContracts

/**
 * Allows to populate [WriteContext] with additional data
 * that can be used when writing metadata in [MetadataExtensions].
 */
interface WriteContextExtension

open class WriteContext(
    val strings: StringTable,
    val contextExtensions: List<WriteContextExtension> = emptyList()
) {
    internal val versionRequirements: MutableVersionRequirementTable = MutableVersionRequirementTable()

    internal val extensions = MetadataExtensions.INSTANCES

    operator fun get(string: String): Int =
        strings.getStringIndex(string)

    internal fun getClassName(name: ClassName): Int =
        strings.getClassNameIndex(name)
}

private fun WriteContext.writeTypeParameter(cmTypeParameter: CmTypeParameter): ProtoBuf.TypeParameter.Builder {
    val t = ProtoBuf.TypeParameter.newBuilder()
    cmTypeParameter.upperBounds.forEach { ub ->
        t.addUpperBound(writeType(ub).build())
    }
    extensions.forEach {
        it.writeTypeParameterExtensions(cmTypeParameter, t, this)
    }
    t.name = this[cmTypeParameter.name]
    t.id = cmTypeParameter.id
    val reified = cmTypeParameter.isReified
    if (reified != ProtoBuf.TypeParameter.getDefaultInstance().reified) {
        t.reified = reified
    }

    return t
}

private fun WriteContext.writeTypeProjection(argument: CmTypeProjection): ProtoBuf.Type.Argument.Builder {
    val t = ProtoBuf.Type.Argument.newBuilder()

    val (variance, argType) = argument
    if (variance == null || argType == null)
        throw InconsistentCangJieMetadataException("Variance and type must be set for non-star type projection")

    t.type = writeType(argType).build()

    return t
}

private fun WriteContext.writeType(cmType: CmType): ProtoBuf.Type.Builder {
    val t = ProtoBuf.Type.newBuilder()
    when (val cls = cmType.classifier) {
        is CmClassifier.Class -> t.className = getClassName(cls.name)
        is CmClassifier.TypeAlias -> t.typeAliasName = getClassName(cls.name)
        is CmClassifier.TypeParameter -> t.typeParameter = cls.id
    }
    cmType.arguments.forEach { argument ->
        t.addArgument(writeTypeProjection(argument))
    }

    cmType.abbreviatedType?.let { t.abbreviatedType = writeType(it).build() }
    cmType.outerType?.let { t.outerType = writeType(it).build() }
    cmType.flexibleTypeUpperBound?.let { fub ->
        val fubType = writeType(fub.type)
        fub.typeFlexibilityId?.let { t.flexibleTypeCapabilitiesId = this[it] }
        t.flexibleUpperBound = fubType.build()
    }

    extensions.forEach { it.writeTypeExtensions(cmType, t, this) }

    if (cmType.isNullable) {
        t.nullable = true
    }
    val flagsToWrite = cmType.flags shr 1
    if (flagsToWrite != ProtoBuf.Type.getDefaultInstance().flags) {
        t.flags = flagsToWrite
    }
    return t
}

private fun WriteContext.writeConstructor(cmConstructor: CmConstructor): ProtoBuf.Constructor.Builder {
    val t = ProtoBuf.Constructor.newBuilder()

    cmConstructor.valueParameters.forEach {
        t.addValueParameter(writeValueParameter(it).build())
    }
    t.addAllVersionRequirement(cmConstructor.versionRequirements.mapNotNull(::writeVersionRequirement))
    extensions.forEach {
        it.writeConstructorExtensions(cmConstructor, t, this)
    }
    if (cmConstructor.flags != ProtoBuf.Constructor.getDefaultInstance().flags) {
        t.flags = cmConstructor.flags
    }
    return t
}

private fun WriteContext.writeFunction(cmFunction: CmFunction): ProtoBuf.Function.Builder {
    val t = ProtoBuf.Function.newBuilder()
    t.addAllTypeParameter(cmFunction.typeParameters.map { writeTypeParameter(it).build() })
    cmFunction.receiverParameterType?.let { t.receiverType = writeType(it).build() }

    @OptIn(ExperimentalContextReceivers::class)
    t.addAllContextReceiverType(cmFunction.contextReceiverTypes.map { writeType(it).build() })
    t.addAllValueParameter(cmFunction.valueParameters.map { writeValueParameter(it).build() })
    t.returnType = writeType(cmFunction.returnType).build()
    t.addAllVersionRequirement(cmFunction.versionRequirements.mapNotNull(::writeVersionRequirement))

    @OptIn(ExperimentalContracts::class)
//    cmFunction.contract?.let { t.contract = writeContract(it) }

    extensions.forEach { it.writeFunctionExtensions(cmFunction, t, this) }

    t.name = this[cmFunction.name]
    if (cmFunction.flags != ProtoBuf.Function.getDefaultInstance().flags) {
        t.flags = cmFunction.flags
    }
    return t
}

fun WriteContext.writeVariable(cmVariable: CmVariable): ProtoBuf.Variable.Builder {
    val t = ProtoBuf.Variable.newBuilder()

    cmVariable.typeParameters.forEach { tp ->
        t.addTypeParameter(writeTypeParameter(tp).build())
    }
    cmVariable.receiverParameterType?.let { t.receiverType = writeType(it).build() }

    @OptIn(ExperimentalContextReceivers::class)
    t.addAllContextReceiverType(cmVariable.contextReceiverTypes.map { writeType(it).build() })
    cmVariable.setterParameter?.let { t.setterValueParameter = writeValueParameter(it).build() }
    t.returnType = writeType(cmVariable.returnType).build()
    t.addAllVersionRequirement(cmVariable.versionRequirements.mapNotNull { writeVersionRequirement(it) })

    extensions.forEach { it.writeVariableExtensions(cmVariable, t, this) }

    t.name = this[cmVariable.name]
    if (cmVariable.flags != ProtoBuf.Property.getDefaultInstance().flags) {
        t.flags = cmVariable.flags
    }

    return t
}

fun WriteContext.writeProperty(cmProperty: CmProperty): ProtoBuf.Property.Builder {
    val t = ProtoBuf.Property.newBuilder()

    cmProperty.typeParameters.forEach { tp ->
        t.addTypeParameter(writeTypeParameter(tp).build())
    }
    cmProperty.receiverParameterType?.let { t.receiverType = writeType(it).build() }

    @OptIn(ExperimentalContextReceivers::class)
    t.addAllContextReceiverType(cmProperty.contextReceiverTypes.map { writeType(it).build() })
    cmProperty.setterParameter?.let { t.setterValueParameter = writeValueParameter(it).build() }
    t.returnType = writeType(cmProperty.returnType).build()
    t.addAllVersionRequirement(cmProperty.versionRequirements.mapNotNull { writeVersionRequirement(it) })

    extensions.forEach { it.writePropertyExtensions(cmProperty, t, this) }

    t.name = this[cmProperty.name]
    if (cmProperty.flags != ProtoBuf.Property.getDefaultInstance().flags) {
        t.flags = cmProperty.flags
    }
    // TODO: do not write getterFlags/setterFlags if not needed
    t.getterFlags = cmProperty.getter.flags
    if (cmProperty.setter != null) t.setterFlags = cmProperty.setter!!.flags
    return t
}

private fun WriteContext.writeValueParameter(
    cmValueParameter: CmValueParameter,
): ProtoBuf.ValueParameter.Builder {
    val t = ProtoBuf.ValueParameter.newBuilder()
    t.type = writeType(cmValueParameter.type).build()
    cmValueParameter.varargElementType?.let { t.varargElementType = writeType(it).build() }
    extensions.forEach { it.writeValueParameterExtensions(cmValueParameter, t, this) }
    if (cmValueParameter.flags != ProtoBuf.ValueParameter.getDefaultInstance().flags) {
        t.flags = cmValueParameter.flags
    }
    t.name = this[cmValueParameter.name]
    return t
}

private fun WriteContext.writeTypeAlias(
    typeAlias: CmTypeAlias,
): ProtoBuf.TypeAlias.Builder {
    val t = ProtoBuf.TypeAlias.newBuilder()
    t.addAllTypeParameter(typeAlias.typeParameters.map { writeTypeParameter(it).build() })
    t.underlyingType = writeType(typeAlias.underlyingType).build()
    t.expandedType = writeType(typeAlias.expandedType).build()
    t.addAllAnnotation(typeAlias.annotations.map { it.writeAnnotation(strings).build() })
    t.addAllVersionRequirement(typeAlias.versionRequirements.mapNotNull(::writeVersionRequirement))
    extensions.forEach { it.writeTypeAliasExtensions(typeAlias, t, this) }

    if (typeAlias.flags != ProtoBuf.TypeAlias.getDefaultInstance().flags) {
        t.flags = typeAlias.flags
    }
    t.name = this[typeAlias.name]
    return t
}

private fun WriteContext.writeVersionRequirement(cmVersionRequirement: CmVersionRequirement): Int? {
    val kind = cmVersionRequirement.kind
    val level = cmVersionRequirement.level
    val errorCode = cmVersionRequirement.errorCode
    val message = cmVersionRequirement.message
    val t = ProtoBuf.VersionRequirement.newBuilder().apply {
        val versionKind = when (kind) {
            CmVersionRequirementVersionKind.LANGUAGE_VERSION -> ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION
            CmVersionRequirementVersionKind.COMPILER_VERSION -> ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION
            CmVersionRequirementVersionKind.API_VERSION -> ProtoBuf.VersionRequirement.VersionKind.API_VERSION
            CmVersionRequirementVersionKind.UNKNOWN -> return null
        }
        if (versionKind != defaultInstanceForType.versionKind) {
            this.versionKind = versionKind
        }
        val requirementLevel = when (level) {
            CmVersionRequirementLevel.WARNING -> ProtoBuf.VersionRequirement.Level.WARNING
            CmVersionRequirementLevel.ERROR -> ProtoBuf.VersionRequirement.Level.ERROR
            CmVersionRequirementLevel.HIDDEN -> ProtoBuf.VersionRequirement.Level.HIDDEN
        }
        if (requirementLevel != defaultInstanceForType.level) {
            this.level = requirementLevel
        }
        if (errorCode != null) {
            this.errorCode = errorCode
        }
        if (message != null) {
            this.message = this@writeVersionRequirement[message]
        }
    }
    val (major, minor, patch) = cmVersionRequirement.version

    VersionRequirement.Version(major, minor, patch).encode(
        writeVersion = { t!!.version = it },
        writeVersionFull = { t!!.versionFull = it }
    )

    return this.versionRequirements[t]
}

@ExperimentalContracts
private fun WriteContext.writeContract(contract: CmContract): ProtoBuf.Contract {
    val t = ProtoBuf.Contract.newBuilder()

    t.addAllEffect(contract.effects.map(::writeEffect))
    return t.build()
}

@ExperimentalContracts
private fun WriteContext.writeEffect(
    effect: CmEffect,
): ProtoBuf.Effect {
    val t = ProtoBuf.Effect.newBuilder()

    t.addAllEffectConstructorArgument(effect.constructorArguments.map(::writeEffectExpression))
    effect.conclusion?.let { t.conclusionOfConditionalEffect = writeEffectExpression(it) }

    when (effect.type) {
        CmEffectType.RETURNS_CONSTANT -> t.effectType = ProtoBuf.Effect.EffectType.RETURNS_CONSTANT
        CmEffectType.CALLS -> t.effectType = ProtoBuf.Effect.EffectType.CALLS
        CmEffectType.RETURNS_NOT_NULL -> t.effectType = ProtoBuf.Effect.EffectType.RETURNS_NOT_NULL
    }
    when (effect.invocationKind) {
        CmEffectInvocationKind.AT_MOST_ONCE -> t.kind = ProtoBuf.Effect.InvocationKind.AT_MOST_ONCE
        CmEffectInvocationKind.EXACTLY_ONCE -> t.kind = ProtoBuf.Effect.InvocationKind.EXACTLY_ONCE
        CmEffectInvocationKind.AT_LEAST_ONCE -> t.kind = ProtoBuf.Effect.InvocationKind.AT_LEAST_ONCE
        null -> {} // nop
    }
    return t.build()
}

@ExperimentalContracts
private fun WriteContext.writeEffectExpression(effectExpression: CmEffectExpression): ProtoBuf.Expression {
    val t = ProtoBuf.Expression.newBuilder()

    if (effectExpression.flags != ProtoBuf.Expression.getDefaultInstance().flags) {
        t.flags = effectExpression.flags
    }
    effectExpression.parameterIndex?.let { t.valueParameterReference = it }
    val cv = effectExpression.constantValue
    if (cv != null) {
        when (val value = cv.value) {
            true -> t.constantValue = ProtoBuf.Expression.ConstantValue.TRUE
            false -> t.constantValue = ProtoBuf.Expression.ConstantValue.FALSE
            null -> t.constantValue = ProtoBuf.Expression.ConstantValue.NULL
            else -> throw IllegalArgumentException("Only true, false or null constant values are allowed for effects (was=$value)")
        }
    }
    effectExpression.isInstanceType?.let { t.isInstanceType = writeType(it).build() }
    t.addAllAndArgument(effectExpression.andArguments.map(::writeEffectExpression))
    t.addAllOrArgument(effectExpression.orArguments.map(::writeEffectExpression))
    return t.build()
}

open class ClassWriter(stringTable: StringTable, contextExtensions: List<WriteContextExtension> = emptyList()) {
    val t: ProtoBuf.Class.Builder = ProtoBuf.Class.newBuilder()!!
    val c: WriteContext = WriteContext(stringTable, contextExtensions)

    fun writeClass(cmClass: CmClass) {
        if (cmClass.flags != ProtoBuf.Class.getDefaultInstance().flags) {
            t.flags = cmClass.flags
        }
        t.fqName = c.getClassName(cmClass.name)

        t.addAllTypeParameter(cmClass.typeParameters.map { c.writeTypeParameter(it).build() })
        t.addAllSupertype(cmClass.supertypes.map { c.writeType(it).build() })
        t.addAllConstructor(cmClass.constructors.map { c.writeConstructor(it).build() })
        t.addAllFunction(cmClass.functions.map { c.writeFunction(it).build() })
        t.addAllProperty(cmClass.properties.map { c.writeProperty(it).build() })
        t.addAllTypeAlias(cmClass.typeAliases.map { c.writeTypeAlias(it).build() })

        cmClass.companionObject?.let { t.companionObjectName = c[it] }
        cmClass.nestedClasses.forEach { t.addNestedClassName(c[it]) }

        cmClass.enumEntries.forEach { name ->
            t.addEnumEntry(ProtoBuf.EnumEntry.newBuilder().also { enumEntry ->
                enumEntry.name = c[name]
            })
        }

        t.addAllSealedSubclassFqName(cmClass.sealedSubclasses.map { c.getClassName(it) })

        cmClass.inlineClassUnderlyingPropertyName?.let { t.inlineClassUnderlyingPropertyName = c[it] }
        cmClass.inlineClassUnderlyingType?.let { t.inlineClassUnderlyingType = c.writeType(it).build() }

        @OptIn(ExperimentalContextReceivers::class)
        t.addAllContextReceiverType(cmClass.contextReceiverTypes.map { c.writeType(it).build() })

        t.addAllVersionRequirement(cmClass.versionRequirements.mapNotNull { c.writeVersionRequirement(it) })

        c.extensions.forEach { it.writeClassExtensions(cmClass, t, c) }

        c.versionRequirements.serialize()?.let {
            t.versionRequirementTable = it
        }
    }
}

open class PackageWriter(
    stringTable: StringTable,
    contextExtensions: List<WriteContextExtension> = emptyList()
) {
    val t: ProtoBuf.Package.Builder = ProtoBuf.Package.newBuilder()
    val c: WriteContext = WriteContext(stringTable, contextExtensions)

    fun writePackage(cmPackage: CmPackage) {
        t.addAllFunction(cmPackage.functions.map { c.writeFunction(it).build() })
//        t.addAllProperty(cmPackage.properties.map { c.writeProperty(it).build() })
        t.addAllVariable(cmPackage.variables.map { c.writeVariable(it).build() })

        t.addAllTypeAlias(cmPackage.typeAliases.map { c.writeTypeAlias(it).build() })
        c.extensions.forEach { it.writePackageExtensions(cmPackage, t, c) }
        c.versionRequirements.serialize()?.let {
            t.versionRequirementTable = it
        }
    }
}

open class ModuleFragmentWriter(
    stringTable: StringTable,
    contextExtensions: List<WriteContextExtension> = emptyList()
) {
    protected val t: ProtoBuf.PackageFragment.Builder = ProtoBuf.PackageFragment.newBuilder()!!
    protected val c: WriteContext = WriteContext(stringTable, contextExtensions)

    open fun writeModuleFragment(cmPackageFragment: CmModuleFragment) {
        cmPackageFragment.pkg?.let {
            val pkgWriter = PackageWriter(c.strings, c.contextExtensions)
            pkgWriter.writePackage(it)
            t.setPackage(pkgWriter.t)
        }

        cmPackageFragment.classes.forEach {
            val classWriter = ClassWriter(c.strings, c.contextExtensions)
            classWriter.writeClass(it)
            t.addClass_(classWriter.t)
        }

        c.extensions.forEach { it.writeModuleFragmentExtensions(cmPackageFragment, t, c) }
    }
}

open class LambdaWriter(stringTable: StringTable) {
    var t: ProtoBuf.Function.Builder? = null
    val c: WriteContext = WriteContext(stringTable)

    fun writeLambda(cmLambda: CmLambda) {
        t = c.writeFunction(cmLambda.function)
    }
}
