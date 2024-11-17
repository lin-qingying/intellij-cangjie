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

import kotlin.reflect.KClass
/**
 * 用于辅助注入，为某个组件提供默认实现，并减少注入代码中的样板代码。
 * 参数类必须是非抽象组件类或实现目标接口的 Kotlin 对象。
 * 当组件没有明确的“默认”行为时，避免使用此注解。
 *
 * 注意：在解析组件时，DefaultImplementation 会被*区分*处理，这意味着：
 * - 如果恰好有一个非默认实现和零个或多个默认实现，则选择非默认实现。
 * - 如果没有非默认实现，则选择默认实现。
 *
 * 例如，在多平台模块中可能会出现这样的配置：考虑分析一个 JVM+JS 模块，其中 JS 提供了某个特定服务的默认实现，而 JVM 提供了非默认实现。
 *
 * 如果你需要更细粒度的冲突解决控制，请考虑使用 [PlatformExtensionsClashResolver]。
 **/
annotation class DefaultImplementation(val impl: KClass<*>)
