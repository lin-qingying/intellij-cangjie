package com.linqingying.cangjie.resolve.calls.tower

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.enumd.EnumEntryDescriptor
import com.linqingying.cangjie.ide.codeinsight.toSourceElement
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.calls.util.FakeCallableDescriptorForObject
import com.linqingying.cangjie.resolve.hasClassValueDescriptor
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyClassMemberScope

import com.linqingying.cangjie.resolve.scopes.*
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils
import com.linqingying.cangjie.types.TypeSubstitutor
import com.intellij.util.SmartList
import com.intellij.util.containers.addIfNotNull

internal class ImportingScopeBasedTowerLevel(
    scopeTower: ImplicitScopeTower,
    importingScope: ImportingScope
) : ScopeBasedTowerLevel(scopeTower, importingScope)

internal abstract class AbstractScopeTowerLevel(
    protected val scopeTower: ImplicitScopeTower
) : ScopeTowerLevel {
    protected val location: LookupLocation get() = scopeTower.location

    protected fun createCandidateDescriptor(
        descriptor: CallableDescriptor,
        dispatchReceiver: ReceiverValueWithSmartCastInfo?,
        specialError: ResolutionDiagnostic? = null,
        dispatchReceiverSmartCastType: CangJieType? = null
    ): CandidateWithBoundDispatchReceiver {
        val diagnostics = SmartList<ResolutionDiagnostic>()
        diagnostics.addIfNotNull(specialError)

//        if (ErrorUtils.isError(descriptor)) {
//            diagnostics.add(ErrorDescriptorDiagnostic)
//        } else {
//            if (descriptor.hasLowPriorityInOverloadResolution() || descriptor.isLowPriorityFromStdlibJre7Or8()) {
//                diagnostics.add(LowPriorityDescriptorDiagnostic)
//            }
//            if (dispatchReceiverSmartCastType != null) diagnostics.add(UsedSmartCastForDispatchReceiver(dispatchReceiverSmartCastType))
//
//            val shouldSkipVisibilityCheck = scopeTower.isNewInferenceEnabled
//            if (!shouldSkipVisibilityCheck) {
//                DescriptorVisibilityUtils.findInvisibleMember(
//                    getReceiverValueWithSmartCast(dispatchReceiver?.receiverValue, dispatchReceiverSmartCastType),
//                    descriptor,
//                    scopeTower.lexicalScope.ownerDescriptor,
//                    scopeTower.languageVersionSettings
//                )?.let { diagnostics.add(VisibilityError(it)) }
//            }
//        }

        return CandidateWithBoundDispatchReceiver(dispatchReceiver, descriptor, diagnostics)


    }

}

internal open class ScopeBasedTowerLevel protected constructor(
    scopeTower: ImplicitScopeTower,
    private val resolutionScope: ResolutionScope
) : AbstractScopeTowerLevel(scopeTower) {

    val deprecationDiagnosticOfThisScope: ResolutionDiagnostic? =
        if (resolutionScope is DeprecatedLexicalScope) ResolvedUsingDeprecatedVisibility(
            resolutionScope,
            location
        ) else null

    internal constructor(scopeTower: ImplicitScopeTower, lexicalScope: LexicalScope) : this(
        scopeTower,
        lexicalScope as ResolutionScope
    )

    override fun getVariables(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {

        return resolutionScope.getContributedVariablesAndIntercept(
            name,
            location,
            null,
            extensionReceiver,
            scopeTower
        ).map {
            createCandidateDescriptor(
                it,
                dispatchReceiver = null,
                specialError = deprecationDiagnosticOfThisScope
            )
        }
    }

    override fun getObjects(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> =
//        resolutionScope.getContributedObjectVariablesIncludeDeprecated(name, location)

        resolutionScope.getContributedObjectVariablesIncludeDeprecateds(name, location)
            .map { (classifier, isDeprecated) ->
                createCandidateDescriptor(
                    classifier,
                    dispatchReceiver = null,
                    specialError = if (isDeprecated) ResolvedUsingDeprecatedVisibility(
                        resolutionScope,
                        location
                    ) else null
                )
            }

    override fun getEnumTypeByKind(
        name: Name,
        kind: ClassKind,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return resolutionScope.getContributedClassifiers(name, location).filter {
            (it as? ClassDescriptor)?.kind == kind
        }
            .map {
                createCandidateDescriptor(

                    EnumClassCallableDescriptor(it)

                    ,
                    dispatchReceiver = null,
                    specialError = ResolvedUsingDeprecatedVisibility(
                        resolutionScope,
                        location
                    )
                )
            }
    }

    override fun getClassType(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return resolutionScope.getContributedClassifiers(name, location)
            .map {
                createCandidateDescriptor(
                    /*   if(it is ClassDescriptor && it.kind == ClassKind.ENUM){
                           it.unsubstitutedPrimaryConstructor!!

                       }else{*/
                    ClassCallableDescriptor(it)
//                    }
                    ,
                    dispatchReceiver = null,
                    specialError = ResolvedUsingDeprecatedVisibility(
                        resolutionScope,
                        location
                    )
                )
            }
    }

    override fun getFunctions(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        val result: ArrayList<CandidateWithBoundDispatchReceiver> = ArrayList()

        resolutionScope.getContributedFunctionsAndConstructors(name, location, null, extensionReceiver, scopeTower)
            .mapTo(result) {
                createCandidateDescriptor(
                    it,
                    dispatchReceiver = null,
                    specialError = deprecationDiagnosticOfThisScope
                )
            }

        // Add constructors of deprecated classifier with an additional diagnostic
        val descriptorWithDeprecation = resolutionScope.getContributedClassifierIncludeDeprecated(name, location)
        if (descriptorWithDeprecation != null && descriptorWithDeprecation.isDeprecated) {
            getConstructorsOfClassifier(descriptorWithDeprecation.descriptor).mapTo(result) {
                createCandidateDescriptor(
                    it,
                    dispatchReceiver = null,
                    specialError = ResolvedUsingDeprecatedVisibility(resolutionScope, location)
                )
            }
        }

        return result
    }

    override fun recordLookup(name: Name) {
        resolutionScope.recordLookup(name, location)
    }
}

fun ResolutionScope.getContributedVariablesAndIntercept(
    name: Name,
    location: LookupLocation,
    dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    extensionReceiver: ReceiverValueWithSmartCastInfo?,
    scopeTower: ImplicitScopeTower
): Collection<VariableDescriptor> {
    val variables = getContributedVariables(name, location)
    val propertys = getContributedPropertys(name, location)
    val result = variables + propertys
    return scopeTower.interceptVariableCandidates(this, name, result, location, dispatchReceiver, extensionReceiver)
}

fun ResolutionScope.getContributedFunctionsAndConstructors(
    name: Name,
    location: LookupLocation,
    dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    extensionReceiver: ReceiverValueWithSmartCastInfo?,
    scopeTower: ImplicitScopeTower
): Collection<FunctionDescriptor> {
    val contributedFunctions = getContributedFunctions(name, location)

    val result = ArrayList<FunctionDescriptor>(contributedFunctions)

    getContributedClassifier(name, location)?.let {
        if (DescriptorUtils.isEnum(it) || DescriptorUtils.isEnumEntry(it)) {
            return@let
        }
        result.addAll(getConstructorsOfClassifier(it))
        result.addAll(scopeTower.syntheticScopes.collectSyntheticConstructors(it, location))
    }

    if (contributedFunctions.isNotEmpty()) {
        result.addAll(scopeTower.syntheticScopes.collectSyntheticStaticFunctions(contributedFunctions, location))
    }

    return scopeTower.interceptFunctionCandidates(this, name, result, location, dispatchReceiver, extensionReceiver)
}

private fun getConstructorsOfClassifier(classifier: ClassifierDescriptor?): List<ConstructorDescriptor> {
    val callableConstructors = when (classifier) {
        is EnumEntryDescriptor -> listOf(classifier.unsubstitutedPrimaryConstructor)
        is TypeAliasDescriptor -> if (classifier.canHaveCallableConstructors) classifier.constructors else emptyList()
        is ClassDescriptor -> if (classifier.canHaveCallableConstructors) classifier.constructors else emptyList()
        else -> emptyList()
    }

    return callableConstructors.filter { it.dispatchReceiverParameter == null }
}

private val ClassDescriptor.canHaveCallableConstructors: Boolean
    get() = !ErrorUtils.isError(this) && !hasClassValueDescriptor

private val TypeAliasDescriptor.canHaveCallableConstructors: Boolean
    get() = classDescriptor != null && !ErrorUtils.isError(classDescriptor) && classDescriptor!!.canHaveCallableConstructors

fun getFakeDescriptorForObject(classifier: ClassifierDescriptor?): FakeCallableDescriptorForObject? =
    when (classifier) {
//        is TypeAliasDescriptor ->
//            classifier.classDescriptor?.let { classDescriptor ->
//                if (classDescriptor.hasClassValueDescriptor)
//                    FakeCallableDescriptorForTypeAliasObject(classifier)
//                else
//                    null
//            }
        is ClassDescriptor ->
            if (classifier.hasClassValueDescriptor )
                FakeCallableDescriptorForObject(classifier)
            else
                null

        else -> null
    }

private fun ResolutionScope.getContributedObjectVariablesIncludeDeprecated(
    name: Name,
    location: LookupLocation
): Collection<DescriptorWithDeprecation<VariableDescriptor>> {
    val (classifier, isOwnerDeprecated) = getContributedClassifierIncludeDeprecated(name, location)
        ?: return emptyList()
    val objectDescriptor = getFakeDescriptorForObject(classifier) ?: return emptyList()
    return listOf(DescriptorWithDeprecation(objectDescriptor, isOwnerDeprecated))
}

private fun ResolutionScope.getContributedObjectVariablesIncludeDeprecateds(
    name: Name,
    location: LookupLocation
): Collection<DescriptorWithDeprecation<VariableDescriptor>> {
    val list = getContributedClassifierIncludeDeprecateds(name, location) ?: return emptyList()


    return list.mapNotNull {
        getFakeDescriptorForObject(it.descriptor)?.let { objectDescriptor ->
            DescriptorWithDeprecation(objectDescriptor, it.isDeprecated)
        }

    }

}

//用于枚举类与枚举项
class EnumClassCallableDescriptor(val type: DeclarationDescriptor) : CallableDescriptor {
    val isEnumEntry get() = DescriptorUtils.isEnumEntry(type)
    private val memberScope = when (type) {
        is ClassDescriptor -> type.unsubstitutedMemberScope
        else -> null
    }
    private val constructors = when (memberScope) {
        is LazyClassMemberScope -> memberScope.getConstructors()
        else -> emptyList()
    }
      var constructor = constructors.firstOrNull()
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitEnumClassCallDescriptor(this, data)
    }

    //    是否具有无参构造
    fun hashUnsubstitutedPrimaryConstructor(): Boolean {

        if (DescriptorUtils.isEnum(type)) return true
        if (DescriptorUtils.isEnumEntry(type)) {
            return (type as EnumEntryDescriptor).hasUnsubstitutedPrimaryConstructor()
        }
        return false
    }


    override fun getValueParameters(): List<ValueParameterDescriptor> {

        return constructor?.valueParameters ?: emptyList()
    }


    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {

    }

    override fun getSource(): SourceElement {
        return type.toSourceElement

    }

    override fun substitute(substitutor: TypeSubstitutor): CallableDescriptor {

        constructor = constructor?.substitute(substitutor)
        return this
    }

    override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> {
        return emptyList()
    }

    override fun getReturnType(): CangJieType? {

        return constructor?.returnType
    }

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? {
        return null
    }

    override fun getOverriddenDescriptors(): List<CallableDescriptor> {
        return emptyList()
    }

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        return null

    }

    override fun hasSynthesizedParameterNames(): Boolean {
        return false
    }

    override fun getTypeParameters(): List<TypeParameterDescriptor> {
        return constructors.firstOrNull()?.typeParameters ?: emptyList()
    }

    override fun hasStableParameterNames(): Boolean {
        return false
    }

    override val isStatic: Boolean
        get() = type.isStatic

    override val original: CallableDescriptor
        get() = this
    override val containingDeclaration: DeclarationDescriptor
        get() = type.containingDeclaration ?: type
    override val visibility: DescriptorVisibility
        get() = type.visibility
    override val name: Name
        get() = type.name
}

//    无其他用处，请勿使用，只作用于重载检查
class ClassCallableDescriptor(val type: DeclarationDescriptor) : CallableDescriptor {
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitClassCallDescriptor(this, data)
    }

    override fun getValueParameters(): List<ValueParameterDescriptor> {

        return emptyList()
    }


    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {

    }

    override fun getSource(): SourceElement {
        return type.toSourceElement
    }

    override fun substitute(substitutor: TypeSubstitutor): CallableDescriptor {
        return this
    }

    override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> {
        return emptyList()
    }

    override fun getReturnType(): CangJieType? {
        return null
    }

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? {
        return null
    }

    override fun getOverriddenDescriptors(): List<CallableDescriptor> {
        return emptyList()
    }

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        return null

    }

    override fun hasSynthesizedParameterNames(): Boolean {
        return false
    }

    override fun getTypeParameters(): List<TypeParameterDescriptor> {
        return emptyList()
    }

    override fun hasStableParameterNames(): Boolean {
        return false
    }

    override val isStatic: Boolean
        get() = type.isStatic
    override val original: CallableDescriptor
        get() = this
    override val containingDeclaration: DeclarationDescriptor
        get() = type
    override val visibility: DescriptorVisibility
        get() = type.visibility
    override val name: Name
        get() = type.name
}
