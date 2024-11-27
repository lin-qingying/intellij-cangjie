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

package com.huawei.cangjie.dapDebugger.runconfig.breakpoint;

import com.linqingying.cangjie.dapDebugger.backend.CjBreakpoint;
import com.linqingying.cangjie.dapDebugger.backend.CjCodepoint;
import com.linqingying.cangjie.dapDebugger.runconfig.CangJieDebugProcess;
import com.intellij.util.containers.FactoryMap;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class CangJieCodePointHandlerBase<T extends XBreakpoint<?>, C extends CjCodepoint> extends XBreakpointHandler<T> {
    protected final CangJieDebugProcess myProcess;
    private final Map<T, List<Integer>> myBreakpoints;

    final Map<Integer, T> myIdToBreakpoint;

    protected CangJieCodePointHandlerBase(CangJieDebugProcess process, @NotNull Class<? extends XBreakpointType<T, ?>> breakpointTypeClass) {
        super(breakpointTypeClass);

        this.myBreakpoints = FactoryMap.create((key) -> {
            return new ArrayList();
        });
        this.myIdToBreakpoint = new HashMap();
        this.myProcess = process;
    }

    @Override
    public void registerBreakpoint(@NotNull T breakpoint) {


        long threadId = this.myProcess.getCurrentThreadId();
//        int frameIndex = this.myProcess.getCurrentFrameIndex();



        this.myProcess.addBreakpoints(this.addCodepointsInBackend(breakpoint, threadId));

    }


    public @Nullable T getXBreakpoint(int id) {
        synchronized (this.myBreakpoints) {
            return this.myIdToBreakpoint.get(id);
        }
    }

    public void addBreakpoint(@NotNull T breakpoint, int id) {
        synchronized (this.myBreakpoints) {
            this.myIdToBreakpoint.put(id, breakpoint);
        }
    }

    public void removeBreakpoint(int id) {
        synchronized (this.myBreakpoints) {
            this.myIdToBreakpoint.remove(id);
        }
    }

    @Override
    public void unregisterBreakpoint(@NotNull T breakpoint, boolean temporary) {
        long threadId = this.myProcess.getCurrentThreadId();
//        int frameIndex = this.myProcess.getCurrentFrameIndex();

        this.myProcess.removeBreakpoints(this.addCodepointsInBackend(breakpoint, threadId));
    }


    protected abstract List<CjBreakpoint> addCodepointsInBackend(T breakpoint, long threadId );

}
