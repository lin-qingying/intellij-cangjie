package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.TypeAliasDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.calls.components.InferenceSession
import com.huawei.cangjie.resolve.scopes.receivers.DetailedReceiver
import com.huawei.cangjie.resolve.scopes.receivers.QualifierReceiver
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import com.huawei.cangjie.resolve.scopes.receivers.TransientReceiver
import com.huawei.cangjie.types.UnwrappedType

interface ReceiverCangJieCallArgument : CangJieCallArgument {
    val receiver: DetailedReceiver
    val isSafeCall: Boolean
}

interface CollectionLiteralCangJieCallArgument : PostponableCangJieCallArgument

interface CangJieCallArgument {
    val isSpread: Boolean
    val argumentName: Name?
}

interface PostponableCangJieCallArgument : CangJieCallArgument, ResolutionAtom

interface SimpleCangJieCallArgument : CangJieCallArgument, ReceiverCangJieCallArgument {
    override val receiver: ReceiverValueWithSmartCastInfo
}

interface CallableReferenceCangJieCallArgument : PostponableCangJieCallArgument, CallableReferenceResolutionAtom {
    override val isSpread: Boolean
        get() = false

    override val lhsResult: LHSResult

    override val call: CangJieCall
}

interface ExpressionCangJieCallArgument : SimpleCangJieCallArgument, ResolutionAtom

/**
 * cases: class A {}, class B { companion object }, object C, enum class D { E }
 * A::foo <-> Type
 * a::foo <-> Expression
 * B::foo <-> Type
 * C::foo <-> Object
 * D.E::foo <-> Expression
 */
sealed class LHSResult {
    class Type(val qualifier: QualifierReceiver?, resolvedType: UnwrappedType) : LHSResult() {
        val unboundDetailedReceiver: ReceiverValueWithSmartCastInfo

        init {
            if (qualifier != null) {
                assert(qualifier.descriptor is ClassDescriptor || qualifier.descriptor is TypeAliasDescriptor) {
                    "Should be ClassDescriptor: ${qualifier.descriptor}"
                }
            }

            val unboundReceiver = TransientReceiver(resolvedType)
            unboundDetailedReceiver = ReceiverValueWithSmartCastInfo(unboundReceiver, emptySet(), isStable = true)
        }
    }

    class Object(val qualifier: QualifierReceiver) : LHSResult() {
        val objectValueReceiver: ReceiverValueWithSmartCastInfo

        init {
//            assert(DescriptorUtils.isObject(qualifier.descriptor)) {
//                "Should be object descriptor: ${qualifier.descriptor}"
//            }
            objectValueReceiver =
                qualifier.classValueReceiverWithSmartCastInfo ?: error("class value should be not null for $qualifier")
        }
    }

    class Expression(val lshCallArgument: SimpleCangJieCallArgument) : LHSResult()

    // todo this case is forbid for now
    object Empty : LHSResult()

    object Error : LHSResult()
}
interface SimpleTypeArgument : TypeArgument {
    val type: UnwrappedType
}

interface FunctionExpression : LambdaCangJieCallArgument {
    override val parametersTypes: Array<UnwrappedType?>

    // null means that there function can not have receiver
    val receiverType: UnwrappedType?

    val contextReceiversTypes: Array<UnwrappedType?>

    // null means that return type is not declared, for fun(){ ... } returnType == Unit
    val returnType: UnwrappedType?
}

interface SimpleCangJieArgument : CangJieCallArgument, ReceiverCangJieCallArgument {
    override val receiver: ReceiverValueWithSmartCastInfo
}
interface TypeArgument
// Used as a stub or underscored type argument
object TypeArgumentPlaceholder : TypeArgument
interface SubCangJieCallArgument : SimpleCangJieCallArgument, ResolutionAtom {
    val callResult: PartialCallResolutionResult
}
class QualifierReceiverCangJieCallArgument(override val receiver: QualifierReceiver) : ReceiverCangJieCallArgument {
    override val isSafeCall: Boolean
        get() = false // TODO: add warning

    override fun toString() = "$receiver"

    override val isSpread get() = false
    override val argumentName: Name? get() = null
}
interface LambdaCangJieCallArgument : PostponableCangJieCallArgument {
    override val isSpread: Boolean
        get() = false

    /*
     * Builder inference is supported only for lambdas (so it's implemented only in `LambdaCangJieArgumentImpl`),
     * anonymous functions aren't supported
     */
    var hasBuilderInferenceAnnotation: Boolean
        get() = false
        set(_) {}

    var builderInferenceSession: InferenceSession?
        get() = null
        set(_) {}

    /**
     * parametersTypes == null means, that there is no declared arguments
     * null inside array means that this type is not declared explicitly
     */
    val parametersTypes: Array<UnwrappedType?>?
}
