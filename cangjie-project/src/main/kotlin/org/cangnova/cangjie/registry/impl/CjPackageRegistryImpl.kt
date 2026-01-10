/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.registry.impl

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.model.CjPackageMetadata
import org.cangnova.cangjie.project.model.CjVersion
import org.cangnova.cangjie.registry.CjPackageRegistry
import java.util.concurrent.ConcurrentHashMap

/**
 * 包注册表实现
 */
@Service(Service.Level.PROJECT)
class CjPackageRegistryImpl(
    @Suppress("UNUSED_PARAMETER") project: Project
) : CjPackageRegistry {

    private val log = logger<CjPackageRegistryImpl>()

    /**
     * 包缓存: "name:version" -> CjPackageMetadata
     */
    private val packageCache = ConcurrentHashMap<String, CjPackageMetadata>()

    override fun registerPackage(pkg: CjPackageMetadata) {
        val key = makeKey(pkg.name, pkg.version)
        packageCache[key] = pkg
        log.info("Package registered: ${pkg.name}:${pkg.version}")
    }

    override fun unregisterPackage(name: String, version: CjVersion) {
        val key = makeKey(name, version)
        val removed = packageCache.remove(key)
        if (removed != null) {
            log.info("Package unregistered: $name:$version")
        }
    }

    override fun findPackage(name: String, version: CjVersion): CjPackageMetadata? {
        val key = makeKey(name, version)
        return packageCache[key]
    }

    override fun getAllPackages(): List<CjPackageMetadata> {
        return packageCache.values.toList()
    }

    override fun isPackageRegistered(name: String, version: CjVersion): Boolean {
        val key = makeKey(name, version)
        return packageCache.containsKey(key)
    }

    private fun makeKey(name: String, version: CjVersion): String {
        return "$name:${version.versionString}"
    }
}