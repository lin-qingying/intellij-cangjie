package com.linqingying.cangjie.types.expressions

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.ClassifierDescriptor
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.psi.CjTypeReference
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.PossiblyBareType
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils.createErrorType
import com.linqingying.cangjie.types.TypeConstructor
import com.linqingying.cangjie.types.TypeReconstructionResult
import com.linqingying.cangjie.types.error.ErrorTypeKind

object TypeReconstructionUtil {

    private fun allProjectionsString(constructor: TypeConstructor): String {
        val size: Int = constructor.getParameters().size
        assert(size != 0) { "No projections possible for a nilary type constructor$constructor" }
        val declarationDescriptor: ClassifierDescriptor =
            checkNotNull(constructor.getDeclarationDescriptor()) { "No declaration descriptor for type constructor $constructor" }
        val name: String = declarationDescriptor.name.asString()

        return getTypeNameAndProjectionsString(
            name,
            size
        )
    }

    fun getTypeNameAndProjectionsString(name: String, size: Int): String {
        val builder = StringBuilder(name)
        builder.append("<")
        for (i in 0 until size) {
            builder.append("*")
            if (i == size - 1) break
            builder.append(", ")
        }
        builder.append(">")

        return builder.toString()
    }

    fun reconstructBareType(
        right: CjTypeReference,
        possiblyBareTarget: PossiblyBareType,
        subjectType: CangJieType?,
        trace: BindingTrace,
        builtIns: CangJieBuiltIns
    ): CangJieType {
        var subjectType: CangJieType? = subjectType
        if (subjectType == null) {
            // Recovery: let's reconstruct as if we were casting from Any, to get some type there
            subjectType = builtIns.anyType
        }
        val reconstructionResult: TypeReconstructionResult =
            possiblyBareTarget.reconstruct(subjectType)
        if (!reconstructionResult.isAllArgumentsInferred) {
            val typeConstructor: TypeConstructor =
                possiblyBareTarget.bareTypeConstructor
            trace.report(
                Errors.NO_TYPE_ARGUMENTS_ON_RHS.on(
                    right,
                    typeConstructor.getParameters().size,
                    allProjectionsString(
                        typeConstructor
                    )
                )
            )
        }

        val targetType = reconstructionResult.resultingType
        if (targetType != null) {
            if (possiblyBareTarget.isBare) {
                trace.record<CjTypeReference, CangJieType>(
                    BindingContext.TYPE,
                    right,
                    targetType
                )
            }
            return targetType
        }

        return createErrorType(
            ErrorTypeKind.ERROR_WHILE_RECONSTRUCTING_BARE_TYPE,
            right.text
        )
    }

}
