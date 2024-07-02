package com.huawei.cangjie.contracts.model

import com.huawei.cangjie.contracts.model.structure.ESType

/**
 * Generic abstraction of static information about some part of program.
 */
interface Computation {
    /**
     * Return-type of corresponding part of program.
     * If type is unknown or computation doesn't have a type (e.g. if
     * it is some construction, like "for"-loop), then type is 'null'
     */
    val type: ESType?

    /**
     * List of all possible effects of this computation.
     * Note that it's not guaranteed to be complete, i.e. if list
     * doesn't mention some effect, then it should be interpreted
     * as the absence of information about that effect.
     */
    val effects: List<ESEffect>
}