package com.huawei.cangjie.contracts.model

sealed class ESEffect {
    /**
     * Returns:
     *  - true, when presence of `this`-effect necessary implies presence of `other`-effect
     *  - false, when presence of `this`-effect necessary implies absence of `other`-effect
     *  - null, when presence of `this`-effect doesn't implies neither presence nor absence of `other`-effect
     */
    abstract fun isImplies(other: ESEffect): Boolean?
}