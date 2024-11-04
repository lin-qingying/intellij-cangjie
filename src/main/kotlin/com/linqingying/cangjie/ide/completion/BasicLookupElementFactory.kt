package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.ide.CangJieDescriptorIconProvider
import com.linqingying.cangjie.ide.completion.handlers.BaseDeclarationInsertHandler
import com.linqingying.cangjie.ide.completion.handlers.DeclarationLookupObjectImpl
import com.linqingying.cangjie.ide.completion.handlers.InsertHandlerProvider
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.references.util.DescriptorToSourceUtilsIde
import com.linqingying.cangjie.renderer.DescriptorRenderer
import com.linqingying.cangjie.resolve.DescriptorToSourceUtils
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.descriptorUtil.unwrapIfFakeOverride
import com.linqingying.cangjie.resolve.isExtension
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.project.Project
import javax.swing.Icon

class BasicLookupElementFactory(
    private val project: Project,
    val insertHandlerProvider: InsertHandlerProvider
) {
    companion object {
        // we skip parameter names in functional types in most of the cases for shortness
        val SHORT_NAMES_RENDERER = DescriptorRenderer.SHORT_NAMES_IN_TYPES.withOptions {
            enhancedTypes = true
            parameterNamesInFunctionalTypes = false
        }

        private fun getIcon(
            lookupObject: DescriptorBasedDeclarationLookupObject,
            descriptor: DeclarationDescriptor,
            flags: Int
        ): Icon? {
            // CangJieDescriptorIconProvider does not use declaration if it is KtElement,
            // so, do not try to look up psiElement for known CangJie descriptors as it could be a heavy deserialization (e.g. from kotlin libs)
            val declaration = when (descriptor) {
                is ReceiverParameterDescriptor -> null
                else -> {
                    lookupObject.psiElement
                }
            }
            return CangJieDescriptorIconProvider.getIcon(descriptor, declaration, flags)
        }
    }

    fun createLookupElement(
        descriptor: DeclarationDescriptor,
        qualifyNestedClasses: Boolean = false,
        includeClassTypeArguments: Boolean = true,
        parametersAndTypeGrayed: Boolean = false
    ): LookupElement {
        return createLookupElementUnwrappedDescriptor(
            descriptor.unwrapIfFakeOverride(),
            qualifyNestedClasses,
            includeClassTypeArguments,
            parametersAndTypeGrayed
        )
    }


    fun createLookupElementForPackage(name: FqName): LookupElement {
        var element = LookupElementBuilder.create(PackageLookupObject(name), name.shortName().asString())

        element = element.withInsertHandler(BaseDeclarationInsertHandler())

        if (!name.parent().isRoot) {
            element = element.appendTailText(" (${name.asString()})", true)
        }

        return element.withIconFromLookupObject()
    }

    private fun createLookupElementUnwrappedDescriptor(
        descriptor: DeclarationDescriptor,
        qualifyNestedClasses: Boolean,
        includeClassTypeArguments: Boolean,
        parametersAndTypeGrayed: Boolean
    ): LookupElement {

        if (descriptor is PackageViewDescriptor) {
            return createLookupElementForPackage(descriptor.fqName)
        }
        if (descriptor is PackageFragmentDescriptor) {
            return createLookupElementForPackage(descriptor.fqName)
        }

        val lookupObject: DescriptorBasedDeclarationLookupObject
        val name: String = when (descriptor) {
            is ConstructorDescriptor -> {
                // for constructor use name and icon of containing class
                val classifierDescriptor = descriptor.containingDeclaration
                lookupObject = object : DeclarationLookupObjectImpl(descriptor) {
                    override val psiElement by lazy {
                        DescriptorToSourceUtilsIde.getAnyDeclaration(
                            project,
                            classifierDescriptor
                        )
                    }

                    override fun getIcon(flags: Int): Icon? = getIcon(this, classifierDescriptor, flags)
                }
                classifierDescriptor.name.asString()
            }


            else -> {
                lookupObject = object : DeclarationLookupObjectImpl(descriptor) {
                    override val psiElement by lazy {
                        DescriptorToSourceUtils.getSourceFromDescriptor(descriptor)
                            ?: DescriptorToSourceUtilsIde.getAnyDeclaration(
                                project,
                                descriptor
                            )
                    }

                    override fun getIcon(flags: Int): Icon? = getIcon(this, descriptor, flags)
                }
                descriptor.name.asString()
            }
        }

        var element = LookupElementBuilder.create(lookupObject, name)

        val insertHandler = insertHandlerProvider.insertHandler(descriptor)
        element = element.withInsertHandler(insertHandler)

        when (descriptor) {
            is FunctionDescriptor -> {
                val returnType = descriptor.returnType
                element = element.withTypeText(
                    if (returnType != null) SHORT_NAMES_RENDERER.renderType(returnType) else "",
                    parametersAndTypeGrayed
                )

                val insertsLambda = when (insertHandler) {
//                    is CangJieFunctionInsertHandler.Normal -> insertHandler.lambdaInfo != null
//                    is CangJieFunctionCompositeDeclarativeInsertHandler -> insertHandler.isLambda
                    else -> false
                }

                if (insertsLambda) {
                    element = element.appendTailText(" {...} ", parametersAndTypeGrayed)
                }

                element = element.appendTailText(
                    SHORT_NAMES_RENDERER.renderFunctionParameters(descriptor),
                    parametersAndTypeGrayed || insertsLambda
                )
            }

            is VariableDescriptor -> {
                element =
                    element.withTypeText(SHORT_NAMES_RENDERER.renderType(descriptor.type), parametersAndTypeGrayed)
            }

            is ClassifierDescriptorWithTypeParameters -> {
                val typeParams = descriptor.declaredTypeParameters
                if (includeClassTypeArguments && typeParams.isNotEmpty()) {
                    element =
                        element.appendTailText(typeParams.joinToString(", ", "<", ">") { it.name.asString() }, true)
                }

                var container = descriptor.containingDeclaration

                if (descriptor.isArtificialImportAliasedDescriptor) {
                    container =
                        descriptor.original // we show original descriptor instead of container for import aliased descriptors
                } else if (qualifyNestedClasses) {
                    element = element.withPresentableText(SHORT_NAMES_RENDERER.renderClassifierName(descriptor))

                    while (container is ClassDescriptor) {
                        val containerName = container.name
                        if (!containerName.isSpecial) {
                            element = element.withLookupString(containerName.asString())
                        }
                        container = container.containingDeclaration
                    }
                }

                if (container is PackageFragmentDescriptor || container is ClassifierDescriptor) {
                    element = element.appendTailText(" (" + DescriptorUtils.getFqName(container) + ")", true)
                }

                if (descriptor is TypeAliasDescriptor) {
                    // here we render with DescriptorRenderer.SHORT_NAMES_IN_TYPES to include parameter names in functional types
                    element = element.withTypeText(
                        DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(descriptor.underlyingType),
                        false
                    )
                }
            }

            else -> {
                element = element.withTypeText(SHORT_NAMES_RENDERER.render(descriptor), parametersAndTypeGrayed)
            }
        }

        var isMarkedAsDsl = false
//        if (descriptor is CallableDescriptor) {
//            appendContainerAndReceiverInformation(descriptor) { element = element.appendTailText(it, true) }
//
//            val dslTextAttributes = DslCangJieHighlightingVisitorExtension.dslCustomTextStyle(descriptor)?.let {
//                EditorColorsManager.getInstance().globalScheme.getAttributes(it.attributesKey)
//            }
//            if (dslTextAttributes != null) {
//                isMarkedAsDsl = true
//                element = element.withBoldness(dslTextAttributes.fontType == Font.BOLD)
//                dslTextAttributes.foregroundColor?.let { element = element.withItemTextForeground(it) }
//            }
//        }

//        if (descriptor is PropertyDescriptor) {
//            val getterName = JvmAbi.getterName(name)
//            if (getterName != name) {
//                element = element.withLookupString(getterName)
//            }
//            if (descriptor.isVar) {
//                element = element.withLookupString(JvmAbi.setterName(name))
//            }
//        }

        if (lookupObject.isDeprecated) {
            element = element.withStrikeoutness(true)
        }

//        if ((insertHandler as? CangJieFunctionInsertHandler.Normal)?.lambdaInfo != null) {
//            element.acceptOpeningBrace = true
//        }

        val result = element.withIconFromLookupObject()
//        result.isDslMember = isMarkedAsDsl
        return result
    }

    fun appendContainerAndReceiverInformation(descriptor: CallableDescriptor, appendTailText: (String) -> Unit) {
        val information = CompletionInformationProvider.EP_NAME.extensions.firstNotNullOfOrNull {
            it.getContainerAndReceiverInformation(descriptor)
        }

        if (information != null) {
            appendTailText(information)
            return
        }

        val extensionReceiver = descriptor.original.extensionReceiverParameter
        if (extensionReceiver != null) {
            when (descriptor) {
//                is SamAdapterExtensionFunctionDescriptor -> {
//                    // no need to show them as extensions
//                    return
//                }
//
//                is SyntheticJavaPropertyDescriptor -> {
//                    var from = descriptor.getMethod.name.asString() + "()"
//                    descriptor.setMethod?.let { from += "/" + it.name.asString() + "()" }
//                    appendTailText(CangJieCompletionBundle.message("presentation.tail.from.0", from))
//                    return
//                }

                else -> {
                    val receiverPresentation = SHORT_NAMES_RENDERER.renderType(extensionReceiver.type)
                    appendTailText(CangJieCompletionBundle.message("presentation.tail.for.0", receiverPresentation))
                }
            }
        }

        val containerPresentation = containerPresentation(descriptor)
        if (containerPresentation != null) {
            appendTailText(" ")
            appendTailText(containerPresentation)
        }
    }

    private fun containerPresentation(descriptor: DeclarationDescriptor): String? {
        when {
            descriptor.isArtificialImportAliasedDescriptor -> {
                return "(${DescriptorUtils.getFqName(descriptor.original)})"
            }

            descriptor.isExtension -> {
                val containerPresentation = when (val container = descriptor.containingDeclaration) {
                    is ClassDescriptor -> DescriptorUtils.getFqNameFromTopLevelClass(container).toString()
                    is PackageFragmentDescriptor -> container.fqName.toString()
                    else -> return null
                }
                return CangJieCompletionBundle.message("presentation.tail.in.0", containerPresentation)
            }

            else -> {
                val container = descriptor.containingDeclaration as? PackageFragmentDescriptor
                // we show container only for global functions and properties
                    ?: return null
                //TODO: it would be probably better to show it also for static declarations which are not from the current class (imported)
                return "(${container.fqName})"
            }
        }
    }

    // add icon in renderElement only to pass presentation.isReal()
    private fun LookupElement.withIconFromLookupObject(): LookupElement =
        object : LookupElementDecorator<LookupElement>(this) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.icon = DefaultLookupItemRenderer.getRawIcon(this@withIconFromLookupObject)
            }
        }
}
