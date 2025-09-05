package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.renderer.AnnotationArgumentsRenderingPolicy
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.renderer.DescriptorRendererModifier.Companion.ALL
import org.cangnova.cangjie.types.CangJieType
import kotlin.math.min

object MemberComparator : Comparator<DeclarationDescriptor> {


    val RENDERER =
        DescriptorRenderer.withOptions {
            withDefinedIn = false
            verbose = true
            annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.UNLESS_EMPTY
            modifiers = ALL

        }


    class NameAndTypeMemberComparator private constructor() :
        java.util.Comparator<DeclarationDescriptor> {
        override fun compare(
            o1: DeclarationDescriptor,
            o2: DeclarationDescriptor
        ): Int {
            val compareInternal = compareInternal(o1, o2)
            return compareInternal ?: 0
        }

        companion object {
            val INSTANCE: NameAndTypeMemberComparator = NameAndTypeMemberComparator()

            private fun getDeclarationPriority(descriptor: DeclarationDescriptor): Int {
                if (DescriptorUtils.isEnumEntry(descriptor)) {
                    return 8
                } else if (descriptor is ConstructorDescriptor) {
                    return 7
                } else if (descriptor is PropertyDescriptor) {
                    return if (descriptor.extensionReceiverParameter == null) {
                        6
                    } else {
                        5
                    }
                } else if (descriptor is FunctionDescriptor) {
                    return if (descriptor.extensionReceiverParameter == null) {
                        4
                    } else {
                        3
                    }
                } else if (descriptor is ClassDescriptor) {
                    return 2
                } else if (descriptor is TypeAliasDescriptor) {
                    return 1
                }
                return 0
            }

            fun compareInternal(
                o1: DeclarationDescriptor,
                o2: DeclarationDescriptor
            ): Int? {
                val prioritiesCompareTo = getDeclarationPriority(o2) - getDeclarationPriority(o1)
                if (prioritiesCompareTo != 0) {
                    return prioritiesCompareTo
                }

                if (DescriptorUtils.isEnumEntry(o1) && DescriptorUtils.isEnumEntry(
                        o2
                    )
                ) {
                    //never reorder enum entries
                    return 0
                }

                val namesCompareTo: Int = o1.name.compareTo(o2.name)
                if (namesCompareTo != 0) {
                    return namesCompareTo
                }

                // Might be equal
                return null
            }
        }
    }

    override fun compare(o1: DeclarationDescriptor, o2: DeclarationDescriptor): Int {
        val typeAndNameCompareResult =
            NameAndTypeMemberComparator.compareInternal(o1, o2)
        if (typeAndNameCompareResult != null) {
            return typeAndNameCompareResult
        }

        if (o1 is TypeAliasDescriptor && o2 is TypeAliasDescriptor) {
            val ta1: TypeAliasDescriptor =
                o1
            val ta2: TypeAliasDescriptor =
                o2
            val r1: String = RENDERER.renderType(ta1.underlyingType)
            val r2: String = RENDERER.renderType(ta2.underlyingType)
            val underlyingTypesCompareTo = r1.compareTo(r2)
            if (underlyingTypesCompareTo != 0) {
                return underlyingTypesCompareTo
            }
        } else if (o1 is CallableDescriptor && o2 is CallableDescriptor) {
            val c1: CallableDescriptor =
                o1
            val c2: CallableDescriptor =
                o2

            val c1ReceiverParameter =
                c1.extensionReceiverParameter
            val c2ReceiverParameter =
                c2.extensionReceiverParameter
            assert((c1ReceiverParameter != null) == (c2ReceiverParameter != null))
            if (c1ReceiverParameter != null) {
                val r1: String =
                    RENDERER.renderType(c1ReceiverParameter.type)
                val r2: String =
                    RENDERER.renderType(c2ReceiverParameter!!.type)
                val receiversCompareTo = r1.compareTo(r2)
                if (receiversCompareTo != 0) {
                    return receiversCompareTo
                }
            }

            val c1ValueParameters: List<ValueParameterDescriptor> =
                c1.valueParameters
            val c2ValueParameters: List<ValueParameterDescriptor> =
                c2.valueParameters
            for (i in 0 until min(c1ValueParameters.size.toDouble(), c2ValueParameters.size.toDouble()).toInt()) {
                val p1: String =
                    RENDERER.renderType(c1ValueParameters[i].type)
                val p2: String =
                    RENDERER.renderType(c2ValueParameters[i].type)
                val parametersCompareTo = p1.compareTo(p2)
                if (parametersCompareTo != 0) {
                    return parametersCompareTo
                }
            }

            val valueParametersNumberCompareTo = c1ValueParameters.size - c2ValueParameters.size
            if (valueParametersNumberCompareTo != 0) {
                return valueParametersNumberCompareTo
            }

            val c1TypeParameters: List<TypeParameterDescriptor> =
                c1.typeParameters
            val c2TypeParameters: List<TypeParameterDescriptor> =
                c2.typeParameters
            for (i in 0 until min(c1TypeParameters.size.toDouble(), c2TypeParameters.size.toDouble()).toInt()) {
                val c1Bounds: List<CangJieType> = c1TypeParameters[i].upperBounds
                val c2Bounds: List<CangJieType> = c2TypeParameters[i].upperBounds
                val boundsCountCompareTo = c1Bounds.size - c2Bounds.size
                if (boundsCountCompareTo != 0) {
                    return boundsCountCompareTo
                }
                for (j in c1Bounds.indices) {
                    val b1: String = RENDERER.renderType(c1Bounds[j])
                    val b2: String = RENDERER.renderType(c2Bounds[j])
                    val boundCompareTo = b1.compareTo(b2)
                    if (boundCompareTo != 0) {
                        return boundCompareTo
                    }
                }
            }

            val typeParametersCompareTo = c1TypeParameters.size - c2TypeParameters.size
            if (typeParametersCompareTo != 0) {
                return typeParametersCompareTo
            }

            if (c1 is CallableMemberDescriptor && c2 is CallableMemberDescriptor) {
                val c1Kind: CallableMemberDescriptor.Kind =
                    c1.kind
                val c2Kind: CallableMemberDescriptor.Kind =
                    c2.kind
                val kindsCompareTo: Int = c1Kind.ordinal - c2Kind.ordinal
                if (kindsCompareTo != 0) {
                    return kindsCompareTo
                }
            }
        } else if (o1 is ClassDescriptor && o2 is ClassDescriptor) {
            val class1: ClassDescriptor =
                o1
            val class2: ClassDescriptor =
                o2

            if (class1.kind.ordinal != class2.kind.ordinal) {
                return class1.kind.ordinal - class2.kind.ordinal
            }


        } else {
            throw AssertionError(
                String.format(
                    """
            Unsupported pair of descriptors:
            '%s' Class: %s
            %s' Class: %s
            """.trimIndent(),
                    o1, o1.javaClass, o2, o2.javaClass
                )
            )
        }

        val renderDiff: Int = RENDERER.render(o1)
            .compareTo(RENDERER.render(o2))
        if (renderDiff != 0) return renderDiff

        val firstModuleName: Name =
            DescriptorUtils.getContainingModule(o1).name
        val secondModuleName: Name =
            DescriptorUtils.getContainingModule(o2).name

        return firstModuleName.compareTo(secondModuleName)
    }
}
