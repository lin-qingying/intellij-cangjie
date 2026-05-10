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

import junit.framework.Test
import junit.framework.TestSuite
import org.junit.runner.Description
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CangJieJUnit3RunnerWithInnersTest : CangJieNoPlatformTestBase() {
    fun testDetectsClassicJUnit3Method() {
        assertTrue(CangJieJUnit3RunnerWithInners.isTestMethod(JUnit3MethodSample::class.java.getMethod("testClassic")))
        assertFalse(CangJieJUnit3RunnerWithInners.isTestMethod(JUnit3MethodSample::class.java.getMethod("helper")))
    }

    fun testFakeEmptyDescriptionIsEmptyWhenOnlyInnerClassesContainTests() {
        CangJieJUnit3RunnerWithInners(OuterWithoutOwnTests.Nested::class.java)
        val runner = CangJieJUnit3RunnerWithInners(OuterWithoutOwnTests::class.java)

        assertEquals(Description.EMPTY, runner.description)
    }

    fun testRootWithUndeclaredInnerRunnerBuildsTreeSuite() {
        val suite = RunnerProbe(OuterWithNestedTests::class.java).collectedTests() as TestSuite

        assertEquals("org.cangnova.cangjie.test.OuterWithNestedTests", suite.name)
        assertTrue(suite.testCount() > 0)
    }

    fun testRootWithRequestedInnerRunnerFallsBackToOwnSuite() {
        CangJieJUnit3RunnerWithInners(OuterWithRequestedInnerRunner.Nested::class.java)
        val suite = RunnerProbe(OuterWithRequestedInnerRunner::class.java).collectedTests() as TestSuite

        assertEquals(1, suite.testCount())
    }
}

private class RunnerProbe(testClass: Class<*>) : CangJieJUnit3RunnerWithInners(testClass) {
    fun collectedTests(): Test {
        val method = CangJieJUnit3RunnerWithInners::class.java.getDeclaredMethod("getCollectedTests")
        method.isAccessible = true
        return method.invoke(this) as Test
    }
}

private open class JUnit3MethodSample : junit.framework.TestCase() {
    fun testClassic() {}

    fun helper() {}
}

private open class OuterWithoutOwnTests : junit.framework.TestCase() {
    class Nested : junit.framework.TestCase() {
        fun testNested() {}
    }
}

private open class OuterWithNestedTests : junit.framework.TestCase() {
    fun testRoot() {}

    class Nested : junit.framework.TestCase() {
        fun testNested() {}
    }
}

private open class OuterWithRequestedInnerRunner : junit.framework.TestCase() {
    fun testRoot() {}

    class Nested : junit.framework.TestCase() {
        fun testNested() {}
    }
}
