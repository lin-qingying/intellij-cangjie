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

package org.cangnova.cangjie.contracts.model

import org.cangnova.cangjie.contracts.model.structure.ESType

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
