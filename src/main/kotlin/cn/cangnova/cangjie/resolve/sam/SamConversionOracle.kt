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

package cn.cangnova.cangjie.resolve.sam

import cn.cangnova.cangjie.container.DefaultImplementation
import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.types.CangJieType

@DefaultImplementation(impl = SamConversionOracleDefault::class)
interface SamConversionOracle {
    fun shouldRunSamConversionForFunction(candidate: CallableDescriptor): Boolean
    fun isPossibleSamType(samType: CangJieType): Boolean

}
class SamConversionOracleDefault : SamConversionOracle {
    override fun shouldRunSamConversionForFunction(candidate: CallableDescriptor): Boolean = true

    override fun isPossibleSamType(samType: CangJieType): Boolean {
        val descriptor = samType.constructor.declarationDescriptor
        return descriptor is ClassDescriptor && descriptor.isFun
    }


}
