package com.debugger.runconfig.breakpoint;

import com.debugger.backend.CjBreakpoint;
import com.debugger.backend.CjCodepoint;
import com.debugger.runconfig.CangJieDebugProcess;
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
