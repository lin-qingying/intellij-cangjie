//package com.huawei.cangjie.contracts.model
//
//import com.huawei.cangjie.builtins.CangJieBuiltIns
//import com.huawei.cangjie.contracts.model.visitors.Reducer
//import com.huawei.cangjie.resolve.calls.inference.components.EmptySubstitutor
//import com.huawei.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
//
///**
// * An abstraction of effect-generating nature of some computation.
// *
// * One can think of Functor as of adjoint to function declaration, responsible
// * for generating effects. It's [invokeWithArguments] method roughly corresponds
// * to call of corresponding function, but instead of taking values and returning
// * values, it takes effects and returns effects.
// */
//interface Functor {
//    fun invokeWithArguments(arguments: List<Computation>, typeSubstitution: ESTypeSubstitution, reducer: Reducer): List<ESEffect>
//}
//
//class ESTypeSubstitution(
//    val substitutor: NewTypeSubstitutor,
//    val builtIns: CangJieBuiltIns
//) {
//    companion object {
//        fun empty(builtIns: CangJieBuiltIns): ESTypeSubstitution {
//            return ESTypeSubstitution(EmptySubstitutor, builtIns)
//        }
//    }
//}
