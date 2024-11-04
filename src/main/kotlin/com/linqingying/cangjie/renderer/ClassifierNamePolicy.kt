package com.linqingying.cangjie.renderer

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.DescriptorUtils.getFqName

interface ClassifierNamePolicy {
    object SHORT : ClassifierNamePolicy {
        override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String {
            if (classifier is TypeParameterDescriptor) return renderer.renderName(classifier.name, false)

            val qualifiedNameElements = ArrayList<Name>()

            // for nested classes qualified name should be used
            var current: DeclarationDescriptor? = classifier
            do {
                qualifiedNameElements.add(current!!.name)
                current = current.containingDeclaration
            } while (current is ClassDescriptor)

            return renderFqName(qualifiedNameElements.asReversed())
        }
    }

    object FULLY_QUALIFIED : ClassifierNamePolicy {
        override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String {
            if (classifier is TypeParameterDescriptor) return renderer.renderName(classifier.name, false)

            return renderer.renderFqName( getFqName(classifier))
        }
    }

    // for local declarations qualified up to function scope
    object SOURCE_CODE_QUALIFIED : ClassifierNamePolicy {
        override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String =
            qualifiedNameForSourceCode(classifier)

        private fun qualifiedNameForSourceCode(descriptor: ClassifierDescriptor): String {
            val nameString = descriptor.name.render()
            if (descriptor is TypeParameterDescriptor) {
                return nameString
            }
            val qualifier = qualifierName(descriptor.containingDeclaration)
            return if (qualifier != null && qualifier != "") qualifier + "." + nameString else nameString
        }

        private fun qualifierName(descriptor: DeclarationDescriptor): String? = when (descriptor) {
            is BasicTypeDescriptor -> null

            is ClassDescriptor -> qualifiedNameForSourceCode(descriptor)
            is PackageFragmentDescriptor -> descriptor.fqName.toUnsafe().render()
            else -> null
        }
    }

    fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String
}
