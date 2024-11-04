package com.linqingying.cangjie.ide


/**
 * Indicates sensitive frontend API, which should be used with caution to avoid invariant violation.
 * Use sites of this annotation include all methods for direct access to frontend components.
 * Please make sure that components don't receive resolution results (descriptors etc.) from different resolution facade for processing.
 * The simplest way to do so is to explicitly provide the same resolution facade to all related computations.
 * Not following this rule may lead to obscure memory leaks and other potential problems.
 */
@RequiresOptIn
annotation class FrontendInternals
