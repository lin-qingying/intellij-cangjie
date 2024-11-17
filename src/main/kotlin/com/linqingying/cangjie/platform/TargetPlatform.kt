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

package com.linqingying.cangjie.platform

open class TargetPlatform(val componentPlatforms: Set<SimplePlatform>) : Collection<SimplePlatform> by componentPlatforms
abstract class SimplePlatform(val platformName: String) {
    override fun toString(): String {
        val targetName = targetName
        return if (targetName.isNotEmpty()) "$platformName ($targetName)" else platformName
    }

    // description of TargetPlatformVersion or name of custom platform-specific target; used in serialization
    open val targetName: String
        get() = targetPlatformVersion.description

    /** See KDoc for [TargetPlatform.oldFashionedDescription] */
    abstract val oldFashionedDescription: String

    // FIXME(dsavvinov): hack to allow injection inject JvmTarget into container.
    //   Proper fix would be to rewrite clients to get JdkPlatform from container, and pull JvmTarget from it
    //   (this will also remove need in TargetPlatformVersion as the whole, and, in particular, ugly passing
    //   of TargetPlatformVersion.NoVersion in non-JVM code)
    open val targetPlatformVersion: TargetPlatformVersion = TargetPlatformVersion.NoVersion
}
interface TargetPlatformVersion {
    val description: String

    object NoVersion : TargetPlatformVersion {
        override val description = ""
    }
}
