package com.huawei.cangjie.dapDebugger.runconfig.breakpoint;

import com.huawei.cangjie.dapDebugger.backend.CjBreakpoint;
import com.huawei.cangjie.dapDebugger.runconfig.CangJieDebugProcess;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.PathUtil;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
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
