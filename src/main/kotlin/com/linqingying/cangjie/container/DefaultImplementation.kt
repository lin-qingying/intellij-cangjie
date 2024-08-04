package com.linqingying.cangjie.container

import kotlin.reflect.KClass

/**
 * Use to assist injection to provide a default implementation for a certain component and reduce boilerplate in injector code.
 * Argument class must be a non-abstract component class or a kotlin object implementing target interface.
 * Avoid using when there is no clear 'default' behaviour for a component.
 *
 * NB: DefaultImplementation are *discriminated* during resolution of components, meaning that:
 * - if there is exactly one non-default implementation and zero or several default, non-default will be chosen.
 * - if there is none non-default implementations, default will be chosen
 *
 * Such configurations may arise, for example, for multiplatform modules: consider analyzing JVM+JS module, where JS contributes
 * default implementation of some particular service, and JVM contributes non-default.
 *
 * If you need more fine-grained control of clashes resolution, consider using [PlatformExtensionsClashResolver]
 **/
annotation class DefaultImplementation(val impl: KClass<*>)
