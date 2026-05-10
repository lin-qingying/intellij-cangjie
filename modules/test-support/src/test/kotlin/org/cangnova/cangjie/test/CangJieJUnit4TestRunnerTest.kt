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
 */

package org.cangnova.cangjie.test

import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CangJieJUnit4TestRunnerTest : CangJieNoPlatformTestBase() {
    fun testAddJUnit3MethodsAddsClassicTestMethods() {
        val methods = CangJieJUnit4TestRunner.addJUnit3Methods(emptyList(), ClassicJUnit3Sample::class.java)

        assertEquals(listOf("testAlpha", "testBeta"), methods.map { it.name })
    }

    fun testAddJUnit3MethodsSkipsIgnoredAndAnnotatedJUnit4Methods() {
        val methods = CangJieJUnit4TestRunner.addJUnit3Methods(emptyList(), MixedDiscoverySample::class.java)

        assertEquals(listOf("testLegacy"), methods.map { it.name })
    }

    fun testAddJUnit3MethodsKeepsSubclassOverrideOnlyOnce() {
        val methods = CangJieJUnit4TestRunner.addJUnit3Methods(emptyList(), OverrideDiscoverySample::class.java)

        val names = methods.map { it.name }
        assertEquals(3, names.size)
        assertEquals(1, names.count { it == "testShared" })
        assertTrue("testOwn" in names)
        assertTrue("testBaseOnly" in names)
    }

    fun testRunnerWithInnersIsIndependentRunner() {
        assertTrue(org.junit.runner.Runner::class.java.isAssignableFrom(CangJieJUnit3RunnerWithInners::class.java))
    }
}

private open class ClassicJUnit3Sample : junit.framework.TestCase() {
    fun testAlpha() {}

    fun testBeta() {}
}

private open class MixedDiscoverySample : junit.framework.TestCase() {
    fun testLegacy() {}

    @Ignore
    fun testIgnored() {}

    @Test
    fun testAnnotatedJUnit4() {}
}

private open class OverrideDiscoveryBase : junit.framework.TestCase() {
    open fun testShared() {}

    fun testBaseOnly() {}
}

private class OverrideDiscoverySample : OverrideDiscoveryBase() {
    override fun testShared() {}

    fun testOwn() {}
}
