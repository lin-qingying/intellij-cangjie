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

package com.linqingying.cangjie.descriptors

import com.google.common.collect.ImmutableSet
import com.linqingying.cangjie.diagnostics.Severity
import com.intellij.util.containers.ContainerUtil

abstract class AbstractCangJieSuppressCache<Element> {
//    protected abstract fun getClosestAnnotatedAncestorElement(
//        element: Element,
//        rootElement: Element,
//        excludeSelf: Boolean
//    ): Element?

    protected val suppressors = ContainerUtil.createConcurrentWeakValueMap<Element, Suppressor<Element>>()

    protected abstract fun getClosestAnnotatedAncestorElement(element: Element, rootElement: Element, excludeSelf: Boolean): Element?

    protected open fun isSuppressed(request: SuppressRequest<Element>): Boolean {

        val annotated = getClosestAnnotatedAncestorElement(request.element, request.rootElement, false) ?: return false

        return isSuppressedByAnnotated(request.suppressKey, request.severity, annotated, request.rootElement, 0)

    }

    /*
     The cache is optimized for the case where no warnings are suppressed (most frequent one)

     trait Root {
       suppress("X")
       trait A {
         trait B {
           suppress("Y")
           trait C {
             fun foo() = warning
           }
         }
       }
     }

     Nothing is suppressed at foo, so we look above. While looking above we went up to the root (once) and propagated
     all the suppressors down, so now we have:

        foo  - suppress(Y) from C
        C    - suppress(Y) from C
        B    - suppress(X) from A
        A    - suppress(X) from A
        Root - suppress() from Root

     Next time we look up anything under foo, we try the Y-suppressor and then immediately the X-suppressor, then to the empty
     suppressor at the root. All the intermediate empty nodes are skipped, because every suppressor remembers its definition point.

     This way we need no more lookups than the number of suppress() annotations from here to the root.
   */
    protected open fun isSuppressedByAnnotated(
        suppressionKey: String,
        severity: Severity,
        annotated: Element,
        rootElement: Element,
        debugDepth: Int
    ): Boolean {
        val suppressor = getOrCreateSuppressor(annotated)
        if (suppressor.isSuppressed(suppressionKey, severity)) return true

        val annotatedAbove = getClosestAnnotatedAncestorElement(suppressor.annotatedElement, rootElement, true) ?: return false

        val suppressed = isSuppressedByAnnotated(suppressionKey, severity, annotatedAbove, rootElement, debugDepth + 1)
        val suppressorAbove = suppressors[annotatedAbove]
        if (suppressorAbove != null && suppressorAbove.dominates(suppressor)) {
            suppressors[annotated] = suppressorAbove
        }

        return suppressed
    }
    protected fun getOrCreateSuppressor(annotated: Element): Suppressor<Element> =
        suppressors.getOrPut(annotated) {
            val strings = getSuppressingStrings(annotated)
            when (strings.size) {
                0 -> EmptySuppressor(annotated)
                1 -> SingularSuppressor(annotated, strings.first())
                else -> MultiSuppressor(annotated, strings)
            }
        }

    // TODO: consider replacing set with list, assuming that the list of suppresses is usually very small
    protected abstract fun getSuppressingStrings(annotated: Element): Set<String>

    protected abstract class Suppressor<Element>(val annotatedElement: Element) {
        abstract fun isSuppressed(suppressionKey: String, severity: Severity): Boolean

        // true is \forall x. other.isSuppressed(x) -> this.isSuppressed(x)
        abstract fun dominates(other: Suppressor<Element>): Boolean
    }

//    protected open fun isSuppressed(request: SuppressRequest<Element>): Boolean {
//
//        val annotated = getClosestAnnotatedAncestorElement(request.element, request.rootElement, false) ?: return false
//
//        return isSuppressedByAnnotated(request.suppressKey, request.severity, annotated, request.rootElement, 0)
//
//    }

    /*
         The cache is optimized for the case where no warnings are suppressed (most frequent one)

         trait Root {
           suppress("X")
           trait A {
             trait B {
               suppress("Y")
               trait C {
                 fun foo() = warning
               }
             }
           }
         }

         Nothing is suppressed at foo, so we look above. While looking above we went up to the root (once) and propagated
         all the suppressors down, so now we have:

            foo  - suppress(Y) from C
            C    - suppress(Y) from C
            B    - suppress(X) from A
            A    - suppress(X) from A
            Root - suppress() from Root

         Next time we look up anything under foo, we try the Y-suppressor and then immediately the X-suppressor, then to the empty
         suppressor at the root. All the intermediate empty nodes are skipped, because every suppressor remembers its definition point.

         This way we need no more lookups than the number of suppress() annotations from here to the root.
       */
//    protected open fun isSuppressedByAnnotated(
//        suppressionKey: String,
//        severity: Severity,
//       annotated: Element,
//        rootElement: Element,
//        debugDepth: Int
//    ): Boolean {
//        val suppressor = getOrCreateSuppressor(annotated)
//        if (suppressor.isSuppressed(suppressionKey, severity)) return true
//
//        val annotatedAbove =
//            getClosestAnnotatedAncestorElement(suppressor.annotatedElement, rootElement, true) ?: return false
//
//        val suppressed = isSuppressedByAnnotated(suppressionKey, severity, annotatedAbove, rootElement, debugDepth + 1)
//        val suppressorAbove = suppressors[annotatedAbove]
//        if (suppressorAbove != null && suppressorAbove.dominates(suppressor)) {
//            suppressors[annotated] = suppressorAbove
//        }
//
//        return suppressed
//    }

//    protected abstract fun getSuppressingStrings(annotated: Element): Set<String>

//    protected fun getOrCreateSuppressor(annotated: Element): Suppressor<Element> =
//        suppressors.getOrPut(annotated) {
//            val strings = getSuppressingStrings(annotated)
//            when (strings.size) {
//                0 -> EmptySuppressor(annotated)
//                1 -> SingularSuppressor(annotated, strings.first())
//                else -> MultiSuppressor(annotated, strings)
//            }
//        }

    private class MultiSuppressor<Element>(annotated: Element, private val strings: Set<String>) :
        Suppressor<Element>(annotated) {
        override fun isSuppressed(suppressionKey: String, severity: Severity): Boolean {
            return isSuppressedByStrings(suppressionKey, strings, severity)
        }

        override fun dominates(other: Suppressor<Element>): Boolean {
            // it's too costly to check set inclusion
            return other is EmptySuppressor
        }
    }

    private class EmptySuppressor<Element>(annotated: Element) : Suppressor<Element>(annotated) {
        override fun isSuppressed(suppressionKey: String, severity: Severity): Boolean = false
        override fun dominates(other: Suppressor<Element>): Boolean = other is EmptySuppressor
    }

    private class SingularSuppressor<Element>(annotated: Element, private val string: String) :
        Suppressor<Element>(annotated) {
        override fun isSuppressed(suppressionKey: String, severity: Severity): Boolean {
            return isSuppressedByStrings(suppressionKey, ImmutableSet.of(string), severity)
        }

        override fun dominates(other: Suppressor<Element>): Boolean {
            return other is EmptySuppressor || (other is SingularSuppressor && other.string == string)
        }
    }

    protected interface SuppressRequest<Element> {
        val element: Element
        val rootElement: Element
        val severity: Severity
        val suppressKey: String
    }

    private class StringSuppressRequest<Element>(
        override val element: Element,
        override val rootElement: Element,
        override val severity: Severity,
        override val suppressKey: String
    ) : SuppressRequest<Element>


    companion object {
        private fun isSuppressedByStrings(key: String, strings: Set<String>, severity: Severity): Boolean =
            severity == Severity.WARNING && "warnings" in strings || key.lowercase() in strings
    }
}
