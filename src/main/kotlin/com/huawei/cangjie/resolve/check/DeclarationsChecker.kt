package com.huawei.cangjie.resolve.check

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ClassDescriptorWithResolutionScopes
import com.huawei.cangjie.descriptors.Errors.UNRESOLVED_REFERENCE
import com.huawei.cangjie.psi.CjClass
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.resolve.*
import com.huawei.cangjie.resolve.calls.results.TypeSpecificityComparator

class DeclarationsChecker(
    private val descriptorResolver: DescriptorResolver,
    modifiersChecker: ModifiersChecker,
    private val annotationChecker: AnnotationChecker,
    private val identifierChecker: IdentifierChecker,
    private val trace: BindingTrace,
    private val languageVersionSettings: LanguageVersionSettings,
    typeSpecificityComparator: TypeSpecificityComparator,
//    private val diagnosticSuppressor: PlatformDiagnosticSuppressor,
    private val upperBoundChecker: UpperBoundChecker
)
{
//    private val exposedChecker = ExposedVisibilityChecker(languageVersionSettings, trace)

    private val modifiersChecker = modifiersChecker.withTrace(trace)
    private fun checkClass(classDescriptor: ClassDescriptorWithResolutionScopes, classOrObject: CjTypeStatement) {
//        checkSupertypesForConsistency(classDescriptor, classOrObject)
//        checkLocalAnnotation(classDescriptor, classOrObject)
//        checkTypesInClassHeader(classOrObject)

        when (classOrObject) {
            is CjClass -> {
//                checkClassButNotObject(classOrObject, classDescriptor)
                descriptorResolver.checkNamesInConstraints(
                    classOrObject, classDescriptor, classDescriptor.scopeForClassHeaderResolution, trace
                )
            }

        }

//        checkPrimaryConstructor(classOrObject, classDescriptor)

//        checkExpectDeclarationModifiers(classOrObject, classDescriptor)
    }

    private fun checkModifiersAndAnnotationsInPackageDirective(file: CjFile) {
        val packageDirective = file.packageDirective ?: return
        val modifierList = packageDirective.modifierList ?: return

        for (annotationEntry in modifierList.annotationEntries) {
            val calleeExpression = annotationEntry.calleeExpression
            if (calleeExpression != null) {
                calleeExpression.constructorReferenceExpression?.let { trace.report(UNRESOLVED_REFERENCE.on(it, it)) }
            }
        }
        annotationChecker.check(packageDirective, trace, null)
        ModifierCheckerCore.check(packageDirective, trace, descriptor = null, languageVersionSettings = languageVersionSettings)
    }
    fun process(bodiesResolveContext: BodiesResolveContext) {
//        for (file in bodiesResolveContext.files) {
//            checkModifiersAndAnnotationsInPackageDirective(file)
//            annotationChecker.check(file, trace, null)
//        }

        for ((classOrObject, classDescriptor) in bodiesResolveContext.declaredClasses.entries) {
            checkClass(classDescriptor, classOrObject)
            modifiersChecker.checkModifiersForDeclaration(classOrObject, classDescriptor)
            identifierChecker.checkDeclaration(classOrObject, trace)
//            exposedChecker.checkClassHeader(classOrObject, classDescriptor)
        }

//        for ((function, functionDescriptor) in bodiesResolveContext.functions.entries) {
//            checkFunction(function, functionDescriptor)
//            modifiersChecker.checkModifiersForDeclaration(function, functionDescriptor)
//            identifierChecker.checkDeclaration(function, trace)
//        }
//
//        for ((property, propertyDescriptor) in bodiesResolveContext.properties.entries) {
//            checkProperty(property, propertyDescriptor)
//            modifiersChecker.checkModifiersForDeclaration(property, propertyDescriptor)
//            identifierChecker.checkDeclaration(property, trace)
//        }
//
//        val destructuringDeclarations = bodiesResolveContext.destructuringDeclarationEntries.entries
//            .map { (entry, _) -> entry.parent }
//            .filterIsInstance<KtDestructuringDeclaration>()
//            .distinct()
//
//        for (multiDeclaration in destructuringDeclarations) {
//            modifiersChecker.checkModifiersForDestructuringDeclaration(multiDeclaration)
//            identifierChecker.checkDeclaration(multiDeclaration, trace)
//        }
//
//        for ((declaration, constructorDescriptor) in bodiesResolveContext.secondaryConstructors.entries) {
//            checkConstructorDeclaration(constructorDescriptor, declaration)
//            exposedChecker.checkFunction(declaration, constructorDescriptor)
//        }
//
//        for ((declaration, typeAliasDescriptor) in bodiesResolveContext.typeAliases.entries) {
//            checkTypeAliasDeclaration(declaration, typeAliasDescriptor)
//            modifiersChecker.checkModifiersForDeclaration(declaration, typeAliasDescriptor)
//            exposedChecker.checkTypeAlias(declaration, typeAliasDescriptor)
//        }
    }

}
