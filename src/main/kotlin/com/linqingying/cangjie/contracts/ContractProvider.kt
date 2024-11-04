package com.linqingying.cangjie.contracts

interface ContractProvider {

    companion object {
        object Default : ContractProvider
    }
}

abstract class AbstractContractProvider : ContractProvider {
//    abstract fun getContractDescription(): ContractDescription?
}


/**
 * Such contract providers are used where we can be sure about contract presence and don't need
 * additional resolve (e.g., for deserialized declarations)
 */
//class ContractProviderImpl(private val contractDescription: ContractDescription) : AbstractContractProvider() {
//    override fun getContractDescription(): ContractDescription = contractDescription
//}
/**
 * This is actual model of contracts, i.e. what is expected to be parsed from
 * general protobuf format.
 *
 * Its intention is to provide declarative representation which is more stable
 * than inner representation of effect system, while enforcing type-checking which
 * isn't possible in protobuf representation.
 *
 * Any changes to this model should be done with previous versions in mind to keep
 * backward compatibility. Ideally, this model should only be extended, but not
 * changed.
 */
//open class ContractDescription(
//    val effects: List<EffectDeclaration>,
//    val ownerFunction: FunctionDescriptor,
//    storageManager: StorageManager
//) {
//    private val computeFunctor = storageManager.createNullableLazyValue {
//        ContractInterpretationDispatcher().convertContractDescriptorToFunctor(this)
//    }
//
////    @Suppress("UNUSED_PARAMETER")
////    fun getFunctor(usageModule: ModuleDescriptor): Functor? = computeFunctor.invoke()
//}
interface EffectDeclaration : ContractDescriptionElement {
    override fun <R, D> accept(contractDescriptionVisitor: ContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitEffectDeclaration(this, data)
}

interface ContractDescriptionElement {
    fun <R, D> accept(contractDescriptionVisitor: ContractDescriptionVisitor<R, D>, data: D): R
}
/**
 * Essentially, this is a composition of two fields: value of type 'ContractDescription' and
 * 'computation', which guarantees to initialize this field.
 *
 * Such contract providers are present only for source-based declarations, where we need additional
 * resolve (force-resolve of the body) to get ContractDescription
 */
//class LazyContractProvider(private val storageManager: StorageManager, private val computation: () -> Any?) : AbstractContractProvider() {
//    @Volatile
//    private var isComputed: Boolean = false
//
//    private var contractDescription: ContractDescription? = null
//
//
//    override fun getContractDescription(): ContractDescription? {
//        if (!isComputed) {
//            storageManager.compute(computation)
//            assert(isComputed) { "Computation of contract hasn't initialized contract properly" }
//        }
//
//        return contractDescription
//    }
//
//    fun setContractDescription(contractDescription: ContractDescription?) {
//        this.contractDescription = contractDescription
//        isComputed = true // publish
//    }
//}
