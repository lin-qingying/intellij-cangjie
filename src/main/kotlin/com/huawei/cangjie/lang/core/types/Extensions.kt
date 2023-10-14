package com.huawei.cangjie.lang.core.types

import com.huawei.cangjie.lang.core.psi.ext.CjInferenceContextOwner

//val CjInferenceContextOwner.selfInferenceResult: CjInferenceResult
//    get() {
//        if (this is CjPath) {
//            val parent = parent
//            if (parent != null && !parent.isAllowedPathParent()) {
//                return CjInferenceResult.EMPTY
//            }
//        }
//        return CachedValuesManager.getCachedValue(this, TYPE_INFERENCE_KEY) {
//            val inferred = inferTypesIn(this)
//
//            createCachedResult(inferred)
//        }
//    }
