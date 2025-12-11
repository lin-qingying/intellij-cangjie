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

package org.cangnova.cangjie.container

// ==================== 容器相关异常 ====================

/**
 * 容器一致性异常
 */
class ContainerConsistencyException(message: String) : Exception(message)

/**
 * 未解析依赖异常
 */
class UnresolvedDependenciesException(message: String) : Exception(message)

/**
 * 无效基数异常（注册数量冲突）
 */
internal class InvalidCardinalityException(message: String) : Exception(message)

/**
 * 未解析服务异常
 */
class UnresolvedServiceException(container: ComponentProvider, request: Class<*>) :
    IllegalArgumentException("Unresolved service: $request in $container")

// ==================== 数据结构相关异常 ====================

/**
 * 拓扑排序中的循环依赖异常
 */
class CycleInTopoSortException : Exception()
