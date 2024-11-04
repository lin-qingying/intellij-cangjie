package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.impl.EnumEntryConstructorDescriptor
import com.linqingying.cangjie.diagnostics.DiagnosticFactory1
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.diagnostics.reportOnDeclaration
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.name.FqNameUnsafe
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap

@DefaultImplementation(impl = ConflictingOverloadsDispatcher.Default::class)
interface ConflictingOverloadsDispatcher {
    fun getDiagnostic(
        languageVersionSettings: LanguageVersionSettings,
        declaration: DeclarationDescriptor,
        redeclarations: Collection<DeclarationDescriptor>
    ): DiagnosticFactory1<PsiElement, Collection<DeclarationDescriptor>>?

    object Default : ConflictingOverloadsDispatcher {
        override fun getDiagnostic(
            languageVersionSettings: LanguageVersionSettings,
            declaration: DeclarationDescriptor,
            redeclarations: Collection<DeclarationDescriptor>
        ): DiagnosticFactory1<PsiElement, Collection<DeclarationDescriptor>>? {
            return when (declaration) {
                is PropertyDescriptor, is ClassifierDescriptor -> Errors.REDECLARATION
                is FunctionDescriptor -> Errors.CONFLICTING_OVERLOADS
                else -> null
            }
        }
    }
}

class OverloadResolver(
    private val trace: BindingTrace,
    private val overloadFilter: OverloadFilter,
    private val overloadChecker: OverloadChecker,
    private val errorDispatcher: ConflictingOverloadsDispatcher,
    private val languageVersionSettings: LanguageVersionSettings,
//    mainFunctionDetectorFactory: MainFunctionDetector.Factory
) {


    private fun findConstructorsInNestedClassesAndTypeAliases(c: BodiesResolveContext): MultiMap<ClassDescriptor, FunctionDescriptor> {
        val constructorsByOuterClass = MultiMap.create<ClassDescriptor, FunctionDescriptor>()

        for (cclass in c.declaredClasses.values) {
            if (/*cclass.kind.isObject*/cclass.hasClassValueDescriptor || cclass.name.isSpecial) {
                // Constructors of singletons or anonymous object aren't callable from the code, so they shouldn't participate in overload name checking
                continue
            }
            val containingDeclaration = cclass.containingDeclaration
            if (containingDeclaration is ClassDescriptor) {

                constructorsByOuterClass.putValues(containingDeclaration, cclass.constructors)
            } else if (!(containingDeclaration is FunctionDescriptor ||
                        containingDeclaration is PropertyDescriptor ||
                        containingDeclaration is PackageFragmentDescriptor)
            ) {
                throw IllegalStateException("Illegal class container: " + containingDeclaration)
            }
        }

//        for (typeAlias in c.typeAliases.values) {
//            val containingDeclaration = typeAlias.containingDeclaration
//            if (containingDeclaration is ClassDescriptor) {
//                constructorsByOuterClass.putValues(containingDeclaration, typeAlias.constructors)
//            }
//        }

        return constructorsByOuterClass
    }

    fun checkOverloads(c: BodiesResolveContext) {
        val inClasses = findConstructorsInNestedClassesAndTypeAliases(c)

        for (value in c.declaredClasses.values) {
            checkOverloadsInClass(value, inClasses.get(value))
        }
        checkOverloadsInPackages(c)
    }

    private inline fun getModulePackageMembersWithSameName(
        descriptor: DeclarationDescriptor,
        overloadFilter: OverloadFilter,
        getMembersByName: (MemberScope, Name) -> Collection<DeclarationDescriptorNonRoot>
    ): Collection<DeclarationDescriptorNonRoot> {
        var containingPackage = descriptor.containingDeclaration
        if (containingPackage is LazyExtendClassDescriptor) {
            containingPackage = containingPackage.containingDeclaration
        }
        if (containingPackage !is PackageFragmentDescriptor) {
            throw AssertionError("$descriptor is not a top-level package member")
        }

        val containingModule = DescriptorUtils.getContainingModuleOrNull(descriptor) ?: return when (descriptor) {
            is CallableMemberDescriptor -> listOf(descriptor)
            is ClassDescriptor -> descriptor.constructors
            else -> throw AssertionError("Unexpected descriptor kind: $descriptor")
        }

        val containingPackageScope = containingModule.getPackage(
            containingPackage.fqName
        ).memberScope
        val possibleOverloads =
            getMembersByName(containingPackageScope, descriptor.name).filter {
                // NB memberScope for PackageViewDescriptor includes module dependencies
                DescriptorUtils.getContainingModule(it) == containingModule
            }

        return overloadFilter.filterPackageMemberOverloads(possibleOverloads)
    }

    private inline fun collectModulePackageMembersWithSameName(
        packageMembersByName: MultiMap<FqNameUnsafe, DeclarationDescriptorNonRoot>,
        interestingDescriptors: Collection<DeclarationDescriptor>,
        overloadFilter: OverloadFilter,
        getMembersByName: (MemberScope, Name) -> Collection<DeclarationDescriptorNonRoot>
    ) {
        val observedFQNs = hashSetOf<FqNameUnsafe>()
        for (descriptor in interestingDescriptors) {
            if (descriptor.containingDeclaration !is PackageFragmentDescriptor && descriptor.containingDeclaration !is
                        LazyExtendClassDescriptor
            ) continue

            val descriptorFQN = DescriptorUtils.getFqName(descriptor)
            if (observedFQNs.contains(descriptorFQN)) continue
            observedFQNs.add(descriptorFQN)

            val packageMembersWithSameName =
                getModulePackageMembersWithSameName(descriptor, overloadFilter, getMembersByName)
            packageMembersByName.putValues(descriptorFQN, packageMembersWithSameName)
        }
    }

    private fun groupModulePackageMembersByFqName(
        c: BodiesResolveContext,
        overloadFilter: OverloadFilter
    ): MultiMap<FqNameUnsafe, DeclarationDescriptorNonRoot> {
        val packageMembersByName = MultiMap<FqNameUnsafe, DeclarationDescriptorNonRoot>()

        collectModulePackageMembersWithSameName(
            packageMembersByName,
            (c.functions.values as Collection<DeclarationDescriptor>) + c.declaredClasses.values + c.typeAliases.values,
            overloadFilter
        ) { scope, name ->
            val functions = scope.getContributedFunctions(name, NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS)
            val classifier = scope.getContributedClassifier(name, NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS)
            when (classifier) {
                is ClassDescriptor ->
                    if (!classifier.kind.isObject)
                        functions + classifier.constructors
                    else
                        functions

                is TypeAliasDescriptor ->
                    functions + classifier.constructors

                else ->
                    functions
            }
        }

        collectModulePackageMembersWithSameName(
            packageMembersByName,
            c.variables.values,
            overloadFilter
        ) { scope, name ->
            val variables = scope.getContributedVariables(name, NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS)
            val classifier = scope.getContributedClassifier(name, NoLookupLocation.MATCH_CHECK_DECLARATION_CONFLICTS)
            variables + listOfNotNull(classifier)
        }

        return packageMembersByName
    }

    private fun checkOverloadsInPackages(c: BodiesResolveContext) {
        val membersByName = groupModulePackageMembersByFqName(c, overloadFilter)

        for (e in membersByName.entrySet()) {
            checkOverloadsInPackage(e.value)
        }
    }

    private fun DeclarationDescriptor.isPrivate() =
        this is DeclarationDescriptorWithVisibility &&
                DescriptorVisibilities.isPrivate(this.visibility)

    private fun getPossibleRedeclarationGroups(members: Collection<DeclarationDescriptorNonRoot>): Collection<Collection<DeclarationDescriptorNonRoot>> {
        val result = arrayListOf<Collection<DeclarationDescriptorNonRoot>>()

        val nonPrivates = members.filter { !it.isPrivate() }

//        val bySourceFile = members.groupBy { DescriptorUtils.getContainingSourceFile(it) }
//
        var hasGroupIncludingNonPrivateMembers = false
//        for (membersInFile in bySourceFile.values) {
//            // File member groups are interesting in redeclaration check if at least one file member is private.
//            if (membersInFile.any { it.isPrivate() }) {
//                hasGroupIncludingNonPrivateMembers = true
//                val group = LinkedHashSet<DeclarationDescriptorNonRoot>(nonPrivates) + membersInFile
//                result.add(group)
//            }
//        }

        if (!hasGroupIncludingNonPrivateMembers && nonPrivates.size > 1) {
            result.add(nonPrivates)
        }

        return result
    }

    private fun DeclarationDescriptor.isSynthesized() =
        this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.SYNTHESIZED

    private fun findRedeclarations(members: Collection<DeclarationDescriptorNonRoot>): Collection<DeclarationDescriptorNonRoot> {
        val redeclarations = linkedSetOf<DeclarationDescriptorNonRoot>()
        for (member1 in members) {
            if (member1.isSynthesized()) continue

            for (member2 in members) {
                if (member1 == member2) continue
//                if (isConstructorsOfDifferentRedeclaredClasses(member1, member2)) continue
//                if (isTopLevelMainInDifferentFiles(member1, member2)) continue
//                if (isDefinitionsForDifferentPlatforms(member1, member2)) continue
//                if (isExpectDeclarationAndDefinition(member1, member2) || isExpectDeclarationAndDefinition(member2, member1)) continue

                if (!overloadChecker.isOverloadable(member1, member2)) {
                    redeclarations.add(member1)
                }
            }
        }
        return redeclarations
    }

    fun checkOverloadsInPackage(members: Collection<DeclarationDescriptorNonRoot>) {
        if (members.size == 1) return

        val redeclarationsMap = LinkedHashMap<DeclarationDescriptorNonRoot, MutableSet<DeclarationDescriptorNonRoot>>()
        for (redeclarationGroup in getPossibleRedeclarationGroups(members)) {
            val redeclarations = findRedeclarations(redeclarationGroup)
            redeclarations.forEach {
                redeclarationsMap.getOrPut(it) { LinkedHashSet() }.addAll(redeclarations)
            }
        }

        val reported = HashSet<DeclarationDescriptorNonRoot>()
        for ((member, conflicting) in redeclarationsMap) {
            if (!reported.contains(member)) {
                reported.addAll(conflicting)
                reportRedeclarations(conflicting)
            }
        }
    }

    private fun reportRedeclarations(redeclarations: Collection<DeclarationDescriptorNonRoot>) {
        if (redeclarations.isEmpty()) return

        for (memberDescriptor in redeclarations) {
            when (memberDescriptor) {

                is EnumEntryConstructorDescriptor -> {}

                is PropertyDescriptor,
                is VariableDescriptor,
                is ClassifierDescriptor,
                is FunctionDescriptor ->
                    reportOnDeclaration(trace, memberDescriptor) { Errors.REDECLARATION.on(it, redeclarations) }

//                is FunctionDescriptor ->
//                    reportOnDeclaration(trace, memberDescriptor) { Errors.CONFLICTING_OVERLOADS.on(it, redeclarations) }
            }
        }
//
//        for (memberDescriptor in redeclarations) {
//            val diagnostic = errorDispatcher.getDiagnostic(languageVersionSettings, memberDescriptor, redeclarations) ?: continue
//            reportOnDeclaration(trace, memberDescriptor) {
//                diagnostic.on(it, redeclarations)
//            }
//        }
    }

    private fun checkOverloadsInClass(
        classDescriptor: ClassDescriptorWithResolutionScopes,
        nestedClassConstructors: Collection<FunctionDescriptor>
    ) {
        val functionsByName = MultiMap.create<Name, CallableMemberDescriptor>()

        for (function in classDescriptor.declaredCallableMembers) {
            functionsByName.putValue(function.name, function)
        }

        for (constructor in classDescriptor.endConstructors) {
            functionsByName.putValue(constructor.name, constructor)
        }

        for (nestedConstructor in nestedClassConstructors) {
            val name = nestedConstructor.containingDeclaration.name
            functionsByName.putValue(name, nestedConstructor)
        }

        for (e in functionsByName.entrySet()) {
            checkOverloadsInClass(e.value)
        }
    }

    private fun checkOverloadsInClass(members: Collection<CallableMemberDescriptor>) {
        if (members.size == 1) return
        reportRedeclarations(findRedeclarations(members))
    }
}
