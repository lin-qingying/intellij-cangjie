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

package org.cangnova.cangjie.test;

import com.intellij.testFramework.JUnit38AssumeSupportRunner;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.junit.Ignore;
import org.junit.internal.MethodSorter;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 对位 Kotlin `JUnit3RunnerWithInners` 的仓颉 JUnit3 runner。
 *
 * 这里保持 Kotlin 的框架职责：
 * 1. 支持 JUnit3 `testXxx` 发现；
 * 2. 支持 inner test class tree suite；
 * 3. 在只有 inner tests 时提供 fake empty class 语义；
 * 4. 仍然暴露独立 runner 名称，供 generated tests 直接绑定。
 *
 * 唯一未照搬 Kotlin 的地方是 IntelliJ 253.28294.357 当前测试框架已不再提供
 * `TestFrameworkUtil.flattenSuite` / `TestIndexingModeSupporter`，因此这里不能伪造同名平台层能力。
 */
public class CangJieJUnit3RunnerWithInners extends Runner implements Filterable, Sortable {
    private static final Set<Class<?>> requestedRunners = new HashSet<>();

    private JUnit38ClassRunner delegateRunner;
    private final Class<?> testClass;
    private boolean isFakeTest = false;

    public CangJieJUnit3RunnerWithInners(Class<?> testClass) {
        this.testClass = testClass;
        requestedRunners.add(testClass);
    }

    @Override
    public void run(RunNotifier notifier) {
        initialize();
        delegateRunner.run(notifier);
    }

    @Override
    public Description getDescription() {
        initialize();
        return isFakeTest ? Description.EMPTY : delegateRunner.getDescription();
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        initialize();
        delegateRunner.filter(filter);
    }

    @Override
    public void sort(Sorter sorter) {
        initialize();
        delegateRunner.sort(sorter);
    }

    protected void initialize() {
        if (delegateRunner != null) return;
        delegateRunner = new JUnit38AssumeSupportRunner(getCollectedTests());
    }

    private Test getCollectedTests() {
        List<Class<?>> innerClasses = collectDeclaredClasses(testClass, false);
        Set<Class<?>> unprocessedInnerClasses = unprocessedClasses(innerClasses);

        if (unprocessedInnerClasses.isEmpty()) {
            if (!innerClasses.isEmpty() && !hasTestMethods(testClass)) {
                isFakeTest = true;
                return new FakeEmptyClassTest(testClass);
            } else {
                // Kotlin 这里会调用平台 `TestFrameworkUtil.flattenSuite(testSuite)`；
                // 当前 IntelliJ 253 平台不再暴露该 API，因此只能保留平台原生 TestSuite。
                return new TestSuite(testClass.asSubclass(TestCase.class));
            }
        } else if (unprocessedInnerClasses.size() == innerClasses.size()) {
            return createTreeTestSuite(testClass);
        } else {
            // 同上，保留当前平台仍然存在的 suite 结构，不伪造缺失的平台 helper。
            return new TestSuite(testClass.asSubclass(TestCase.class));
        }
    }

    private static TestSuite createTreeTestSuite(Class<?> root) {
        Set<Class<?>> classes = new LinkedHashSet<>(collectDeclaredClasses(root, true));
        java.util.Map<Class<?>, TestSuite> classSuites = new java.util.HashMap<>();

        for (Class<?> aClass : classes) {
            TestSuite testSuite = hasTestMethods(aClass) ? new TestSuite(aClass) : new TestSuite(aClass.getCanonicalName());
            classSuites.put(aClass, testSuite);
        }

        for (Class<?> aClass : classes) {
            if (aClass.getEnclosingClass() != null && classes.contains(aClass.getEnclosingClass())) {
                classSuites.get(aClass.getEnclosingClass()).addTest(classSuites.get(aClass));
            }
        }

        return classSuites.get(root);
    }

    private static Set<Class<?>> unprocessedClasses(Collection<Class<?>> classes) {
        Set<Class<?>> result = new LinkedHashSet<>();
        for (Class<?> aClass : classes) {
            if (!requestedRunners.contains(aClass)) {
                result.add(aClass);
            }
        }
        return result;
    }

    private static List<Class<?>> collectDeclaredClasses(Class<?> klass, boolean withItself) {
        List<Class<?>> result = new ArrayList<>();
        if (withItself) {
            result.add(klass);
        }

        for (Class<?> aClass : klass.getDeclaredClasses()) {
            result.addAll(collectDeclaredClasses(aClass, true));
        }

        return result;
    }

    private static boolean hasTestMethods(Class<?> klass) {
        for (Class<?> currentClass = klass; Test.class.isAssignableFrom(currentClass); currentClass = currentClass.getSuperclass()) {
            for (Method each : MethodSorter.getDeclaredMethods(currentClass)) {
                if (isTestMethod(each)) return true;
            }
        }

        return false;
    }

    static boolean isTestMethod(Method method) {
        return method.getParameterTypes().length == 0 &&
               method.getName().startsWith("test") &&
               method.getReturnType().equals(Void.TYPE) &&
               Modifier.isPublic(method.getModifiers()) &&
               method.getAnnotation(Ignore.class) == null;
    }

    static class FakeEmptyClassTest implements Test, Filterable {
        private final String className;

        FakeEmptyClassTest(Class<?> klass) {
            this.className = klass.getName();
        }

        @Override
        public int countTestCases() {
            return 0;
        }

        @Override
        public void run(TestResult result) {
            result.startTest(this);
            result.endTest(this);
        }

        @Override
        public String toString() {
            return "Empty class with inners for " + className;
        }

        @Override
        public void filter(Filter filter) throws NoTestsRemainException {
            throw new NoTestsRemainException();
        }
    }
}
