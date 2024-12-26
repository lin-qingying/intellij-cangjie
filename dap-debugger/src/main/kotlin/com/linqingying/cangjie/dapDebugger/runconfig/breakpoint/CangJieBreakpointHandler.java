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

package com.linqingying.cangjie.dapDebugger.runconfig.breakpoint;

import com.linqingying.cangjie.dapDebugger.backend.CjBreakpoint;
import com.linqingying.cangjie.dapDebugger.runconfig.CangJieDebugProcess;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.PathUtil;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;



public class CangJieBreakpointHandler extends CangJieCodePointHandlerBase<XLineBreakpoint<?>, CjBreakpoint> {


    public CangJieBreakpointHandler(CangJieDebugProcess process) {
        this(process, CangJieLineBreakpointType.class);
    }

    public CangJieBreakpointHandler(@NotNull CangJieDebugProcess process, @NotNull Class<? extends XLineBreakpointType<?>> type) {
        super(process, type);
    }

    protected List<CjBreakpoint> addCodepointsInBackend(XLineBreakpoint<?> breakpoint, long threadId ) {


        String filePath =        VfsUtilCore.urlToPath(breakpoint.getFileUrl());
        String fileName = PathUtil.getFileName(filePath);
        int line = breakpoint.getLine() + 1;


        CjBreakpoint cjBreakpoint = new CjBreakpoint(fileName, filePath);

        cjBreakpoint.addLine(Map.of(line, breakpoint));


        return List.of(cjBreakpoint);
    }
}
