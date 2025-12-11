/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.deprecation

abstract class DeprecationInfo : Comparable<DeprecationInfo> {
    abstract val deprecationLevel: DeprecationLevelValue
    abstract val propagatesToOverrides: Boolean
    abstract val message: String?

    override fun compareTo(other: DeprecationInfo): Int {
        val lr = deprecationLevel.compareTo(other.deprecationLevel)
        //to prefer inheritable deprecation
        return if (lr == 0 && !propagatesToOverrides && other.propagatesToOverrides) 1
        else lr
    }
}

/**
 * This corresponds to [DeprecationLevel] in CangJie standard library. A symbol annotated with [java.lang.Deprecated] is considered a
 * warning.
 */
enum class DeprecationLevelValue {
    WARNING, ERROR, HIDDEN
}
