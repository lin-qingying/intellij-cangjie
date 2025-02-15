/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.container


/**
 * This is a marker-interface for components which are needed for common resolve
 * facilities (like resolve, or deserialization), but are platform-specific.
 *
 * PlatformSpecificExtensions has to be present in the container in exactly one
 * instance (hence common pattern with providing no-op DEFAULT/EMPTY implementation
 * in the corresponding interface)
 *
 * In multiplatform modules such components require special treatment. Namely,
 * if several components of the same type are provided, then it's not an illegal state;
 * rather, we have to carefully resolve clash on case-by-case basis.
 * See also [PlatformExtensionsClashResolver].
 */
interface PlatformSpecificExtension<S : PlatformSpecificExtension<S>>

/**
 * Allows to specify which [PlatformSpecificExtension] should be used if there were two or more registrations
 * for [applicableTo] class in the container.
 *
 * [PlatformExtensionsClashResolver] should be registred in the container via [useClashResolver]-extension.
 *
 * NB. YOU DON'T NEED this mechanism for the most popular case of "one or several default vs.
 * zero or one non-default". Just use [DefaultImplementation], and default instances will be automatically
 * discriminated (see respective CDoc).
 * Use [PlatformExtensionsClashResolver] only for cases when you need more invloved logic.
 *
 * Example: [com.linqingying.cangjie.resolve.IdentifierChecker]. It is used in platform-agnostic code,
 * which resolves and checks identifiers for correctness. Each platform has it's own rules
 * regarding identifier correctness. In MPP modules we can't choose only one IdentifierChecker;
 * instead, we have to provide a "composite" IdentifierChecker which will launch checks of *each*
 * platform.
 *
 */
abstract class PlatformExtensionsClashResolver<E : PlatformSpecificExtension<E>>(val applicableTo: Class<E>) {
    abstract fun resolveExtensionsClash(extensions: List<E>): E

    class FallbackToDefault<E : PlatformSpecificExtension<E>>(
        private val defaultValue: E,
        applicableTo: Class<E>
    ) : PlatformExtensionsClashResolver<E>(applicableTo) {

        override fun resolveExtensionsClash(extensions: List<E>): E = defaultValue
    }

    class FirstWins<E : PlatformSpecificExtension<E>>(applicableTo: Class<E>) : PlatformExtensionsClashResolver<E>(applicableTo) {

        override fun resolveExtensionsClash(extensions: List<E>): E = extensions.first()
    }
}

