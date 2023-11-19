(() => {
    var e = {
        2635: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.activate = void 0;
            const o = i(9496), r = i(657);
            t.activate = function (e) {
                return n(this, void 0, void 0, (function* () {
                    const t = o.window.createOutputChannel("Cangjie Debug");
                    e.subscriptions.push(t), yield r.activate(e, t)
                }))
            }
        }, 4050: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.pickNativeProcess = t.ProcessPickItem = void 0;
            const o = i(9496), r = i(5697), s = i(2037);

            class a {
                constructor(e, t, i, n) {
                    this.label = e, this.pid = t, this.description = i, this.detail = n
                }

                getProgramPath() {
                    if (!this.detail) return;
                    let e;
                    const [t] = this.detail.split(" ");
                    if ("win" === r.getOs()) {
                        this.detail.startsWith('"') ? (e = this.detail.substring(1), e = e.substring(0, e.indexOf('"'))) : e = t;
                        for (const t of a.NEED_REMOVE_PREFIX_ON_WIN_ARR) if (e.startsWith(t)) {
                            e = e.substring(t.length);
                            break
                        }
                    } else e = t;
                    return e
                }
            }

            t.ProcessPickItem = a, a.NEED_REMOVE_PREFIX_ON_WIN_ARR = ["\\??\\", "\\\\?\\"];
            let c = null;
            t.pickNativeProcess = function () {
                return n(this, void 0, void 0, (function* () {
                    let e;
                    if ("win" === r.getOs()) e = yield function () {
                        return n(this, void 0, void 0, (function* () {
                            const e = (yield r.execNativeCommand("wmic process get Name,ProcessId,CommandLine /FORMAT:list")).split(s.EOL);
                            let t = [];
                            for (const i of e) {
                                if (!i) continue;
                                const e = i.indexOf("=");
                                if (e > 0) {
                                    const n = i.slice(0, e).trim();
                                    let o = i.slice(e + 1).trim();
                                    switch (n) {
                                        case"CommandLine":
                                            t.push(new a(null, 0, null, o));
                                            break;
                                        case"Name":
                                            t[t.length - 1].label = o;
                                            break;
                                        case"ProcessId":
                                            t[t.length - 1].pid = parseInt(o), t[t.length - 1].description = o
                                    }
                                }
                            }
                            return t = t.filter((e => e.detail && e.detail.length > 0)), t.sort(((e, t) => e.pid - t.pid)), t
                        }))
                    }(); else {
                        e = (yield function () {
                            return n(this, void 0, void 0, (function* () {
                                let e = (yield r.execNativeCommand("ps -awwxo pid,cmd")).trim().split("\n");
                                return e.shift(), e.map((e => {
                                    let t = e;
                                    t = t.trim();
                                    let i = t.indexOf(" "), n = Number.parseInt(t.substring(0, i)),
                                        o = t.substring(i + 1);
                                    const [r] = o.split(" ");
                                    let s = r;
                                    return s.startsWith("[") && s.endsWith("]") && (s = s.substring(1, s.length - 1)), {
                                        name: s.substring(s.lastIndexOf("/") + 1),
                                        pid: n,
                                        cmd: o
                                    }
                                }))
                            }))
                        }()).map((e => new a(e.name, e.pid, e.pid.toString(), e.cmd)))
                    }
                    const t = e.findIndex((e => e.label === c));
                    if (t >= 0) {
                        const i = e.slice(t, t + 1);
                        e.splice(0, 0, i[0])
                    }
                    const i = yield o.window.showQuickPick(e);
                    if (!i) throw new Error("process not selected");
                    return c = i.label, i
                }))
            }
        }, 5755: (e, t) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.BuildTypeItem = void 0;
            t.BuildTypeItem = class {
                constructor(e) {
                    switch (this.buildType = e, e) {
                        case"singleFile":
                            this.label = "Build And Debug Single Source File";
                            break;
                        case"cangjieProject":
                            this.label = "Build And Debug Cangjie Project";
                            break;
                        case"chooseFile":
                            this.label = "Choose Executable File Later"
                    }
                }
            }
        }, 7012: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.buildAndDebugSingleFile = t.createSingleFileBuildCommand = t.getTargetFilePathFromSourcePath = void 0;
            const o = i(1017), r = i(5697), s = i(9496), a = i(5828), c = i(310), l = i(5768);

            function u(e) {
                if (!e) return;
                let t = o.basename(e);
                const i = e.substring(0, e.length - t.length);
                t.indexOf(".") >= 0 && (t = t.substring(0, t.lastIndexOf(".")));
                const n = "win" === r.getOs() ? `${t}.exe` : t;
                return o.join(i, n)
            }

            t.getTargetFilePathFromSourcePath = u, t.createSingleFileBuildCommand = function (e, t) {
                return [e, "-g", t, "-o", u(t)]
            }, t.buildAndDebugSingleFile = function (e) {
                var t;
                return n(this, void 0, void 0, (function* () {
                    let i = e;
                    if (i || (i = null === (t = s.window.activeTextEditor) || void 0 === t ? void 0 : t.document.uri), !i) return void s.window.showErrorMessage("no opened file in editor");
                    if (!r.isCjSourceFile(i.fsPath)) throw new Error("source file is not Cangjie");
                    const n = s.workspace.getWorkspaceFolder(i),
                        o = c.getVSCodeJsonFileDataArray(n, c.launchJsonType).filter((e => {
                            const t = e;
                            return t.preLaunchTask && t.type === l.debugType && t.program === u(i.fsPath)
                        }));
                    if (o.length > 0) return void (yield s.debug.startDebugging(n, o[0]));
                    const d = new a.CangjieDebugConfigAndPreTaskBuilder;
                    d.workspaceFolder = n, d.debuggerType = "cjdb", d.startDebugType = "launch", d.buildType = "singleFile", d.sourceFilePath = i.fsPath;
                    const p = yield d.buildConfig(), h = d.buildPreTask();
                    yield c.addDataToVSCodeJsonFile(n, c.tasksJsonType, h), yield c.addDataToVSCodeJsonFile(n, c.launchJsonType, [p]), yield s.debug.startDebugging(n, p)
                }))
            }
        }, 2668: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.CangjieBuildTaskProvider = void 0;
            const o = i(4276), r = i(5697), s = i(9496);

            class a {
                static resolveTaskAsync(e) {
                    return n(this, void 0, void 0, (function* () {
                        if (e.execution) return;
                        const t = e.definition;
                        try {
                            yield r.execNativeProcess(t.cmd, ["-v"])
                        } catch (e) {
                            s.window.showErrorMessage(`${t.cmd} doesn't exist or permission denied.`)
                        }
                        return o.resolveBuildTaskDefinition(t)
                    }))
                }

                provideTasks(e) {
                }

                resolveTask(e, t) {
                    return a.resolveTaskAsync(e)
                }
            }

            t.CangjieBuildTaskProvider = a
        }, 9792: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.CangjieDebugAdapterDescriptorFactory = void 0;
            const o = i(9496), r = i(7147), s = i(5768), a = i(5697), c = i(6682), l = i(5697);

            class u {
                constructor() {
                    this.currentServerExecution = null, this.serverTerminateTimeMillis = 500, this.taskExecuteCount = 20, this.taskWaitTimeMillis = 50
                }

                createDebugAdapterDescriptor(e, t) {
                    return this.createDebugAdapterImplementation(e, t)
                }

                sessionTerminated() {
                    null !== this.currentServerExecution && this.currentServerExecution.terminate()
                }

                createDebugAdapterImplementation(e, t) {
                    return n(this, void 0, void 0, (function* () {
                        yield this.checkCurrentExecution();
                        const t = e.configuration;
                        if (null === t) throw new Error("debug configuration not found");
                        const i = void 0 === t.preLaunchTask && "launch" === t.request && void 0 !== t.buildBeforeLaunch && t.buildBeforeLaunch;
                        l.isCangjieProject() && l.isBuildCommandAvailable() && i && (yield l.buildCangjieProject()), r.existsSync(a.getServerLogPath()) || r.mkdirSync(a.getServerLogPath(), {recursive: !0}), "win" !== a.getOs() && (a.setExecPermission(a.getDapServerPath()), "launch" !== t.request || t.remote || a.configTerminalCangjieEnv());
                        const n = a.getDapServerPath(), c = a.getServerLogPath(),
                            d = yield l.findAPortNotInUse(s.portLowerBound, s.portHigherBound),
                            p = [s.serverPortArgPrefix + d, s.serverLogPathArgPrefix + a.unifySlashOfPath(c), s.serverDebuggerType],
                            h = new o.Task({type: `${s.debugType}-${Date.now()}`}, o.TaskScope.Workspace, a.getDapServerName(), s.debugType, new o.ShellExecution(n, p));
                        h.isBackground = !0, h.presentationOptions = {
                            reveal: o.TaskRevealKind.Never,
                            focus: !1
                        }, u.dapServerStartStatus = !0;
                        const g = yield a.toPromise(o.tasks.executeTask(h));
                        this.currentServerExecution = g, a.onTaskProcessEnded(g, (e => {
                            e.execution === g && (this.currentServerExecution = null, 0 !== e.exitCode && (u.dapServerStartStatus = !1))
                        }));
                        const m = yield this.createDebugAdapter(d, t);
                        if (m.socketConnect) return new o.DebugAdapterInlineImplementation(m);
                        if (g.terminate(), !u.dapServerStartStatus) throw new Error("Debug server failed to start");
                        throw new Error(`server socket on ${d} not established`)
                    }))
                }

                checkCurrentExecution() {
                    return n(this, void 0, void 0, (function* () {
                        try {
                            yield a.executeScheduledTask((() => {
                                if (null === this.currentServerExecution) throw new Error("last execution finished")
                            }), this.taskWaitTimeMillis, this.taskExecuteCount)
                        } catch (e) {
                        }
                        if (null !== this.currentServerExecution) throw new Error(`${a.getDapServerName()} is still running, please stop current debugging`)
                    }))
                }

                createDebugAdapter(e, t) {
                    return n(this, void 0, void 0, (function* () {
                        const i = new c.CangjieSocketDebugAdapter(e, t);
                        return yield i.init(), i.addMessageListener("response", "stepOut", (e => {
                            const t = e;
                            !t.success && t.message.indexOf("not meaningful in the outermost frame.") >= 0 && i.terminate()
                        })), i.addMessageListener("event", "exited", (e => {
                            const t = e;
                            0 !== t.body.exitCode && o.window.showErrorMessage(`target program exited with code ${t.body.exitCode}`)
                        })), i.addMessageFilter("request", "variables", (e => {
                            const t = e;
                            return void 0 === t.arguments.start && (t.arguments.start = 0, t.arguments.count = 1e3), t
                        })), i.addMessageListener("request", "disconnect", (() => {
                            const e = this.currentServerExecution;
                            setTimeout((() => {
                                e && e.terminate()
                            }), this.serverTerminateTimeMillis)
                        })), i.addMessageFilter("event", "breakpoint", (e => null)), i.addMessageFilter("request", "evaluate", (e => {
                            const t = e;
                            if ("repl" !== t.arguments.context) return t;
                            let n = t.arguments.expression;
                            if (!l.isFieldLengthRight(n, "command")) return null;
                            const o = "-exec";
                            if (0 !== n.indexOf(o)) return t;
                            n = a.trimAllStartSpace(n.substring(5));
                            if (/[a-z]|-/.test(n.charAt(0))) {
                                const e = {debugCommand: n};
                                a.sendRequest("debugInConsole", e).then((e => {
                                    const t = {
                                        body: {category: "stdout", output: a.standardDebugReplyMsg(e.output)},
                                        event: "output",
                                        seq: 0,
                                        type: "event"
                                    };
                                    i.serverMsgEventEmitter.fire(t)
                                }))
                            } else {
                                const e = {
                                    body: {
                                        category: "stderr",
                                        output: "illegal command: The command must start with a lowercase letter or '-'."
                                    }, event: "output", seq: 0, type: "event"
                                };
                                i.serverMsgEventEmitter.fire(e)
                            }
                            return null
                        })), i.addMessageListener("response", "configurationDone", (e => {
                            a.sendRequest("debugInConsole", {debugCommand: "process handle -p true -s false -n false SIGSEGV"})
                        })), i.addMessageListener("request", "launch", (e => {
                            const t = e;
                            if (t.arguments.remote) {
                                const e = t.arguments.remoteCangjieSdkPath;
                                t.arguments.scriptCommands || (t.arguments.scriptCommands = []), t.arguments.scriptCommands.push(`env PATH=${e}/bin:${e}/tools/bin::$PATH`), t.arguments.scriptCommands.push(`env LD_LIBRARY_PATH=${e}/lib/linux_x86_64_llvm:\${LD_LIBRARY_PATH}`)
                            }
                            return t
                        })), i.addMessageFilter("request", "setDataBreakpoints", (e => {
                            const t = e;
                            return t.arguments.breakpoints.length > s.maximumNumberOfDataBreakpoint ? (o.window.showErrorMessage("The number of dataBreakpoints is not allowed to exceed four."), null) : t
                        })), i.addMessageListener("response", "scopes", (e => {
                            const t = e;
                            for (let e = 0; e < t.body.scopes.length; e++) "Globals" === t.body.scopes[e].name ? t.body.scopes[e].name = "Globals & Statics" : "Statics" === t.body.scopes[e].name && (t.body.scopes.splice(e, 1), e--);
                            return t
                        })), i.addMessageFilter("request", "setBreakpoints", (e => {
                            const t = e;
                            if (t.arguments.breakpoints.length > 0) for (let e = 0; e < t.arguments.breakpoints.length; e++) l.isFieldLengthRight(t.arguments.breakpoints[e].condition, "breakpoint condition") || (t.arguments.breakpoints[e].condition = ""), l.isFieldLengthRight(t.arguments.breakpoints[e].hitCondition, "breakpoint hitCondition") || (t.arguments.breakpoints[e].hitCondition = ""), l.isFieldLengthRight(t.arguments.breakpoints[e].logMessage, "breakpoint logMessage") || (t.arguments.breakpoints[e].logMessage = "");
                            return t
                        })), i.addMessageFilter("request", "setFunctionBreakpoints", (e => {
                            const t = e;
                            if (t.arguments.breakpoints.length > 0) {
                                let e = [];
                                for (let i = 0; i < t.arguments.breakpoints.length; i++) l.isFieldLengthRight(t.arguments.breakpoints[i].condition, "breakpoint condition") || (t.arguments.breakpoints[i].condition = ""), l.isFieldLengthRight(t.arguments.breakpoints[i].hitCondition, "breakpoint hitCondition") || (t.arguments.breakpoints[i].hitCondition = ""), l.isFieldLengthRight(t.arguments.breakpoints[i].name, "breakpoint name") && e.push(t.arguments.breakpoints[i]);
                                t.arguments.breakpoints = e
                            }
                            return t
                        })), i
                    }))
                }
            }

            t.CangjieDebugAdapterDescriptorFactory = u, u.dapServerStartStatus = !0
        }, 5828: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.CangjieDebugConfigAndPreTaskBuilder = void 0;
            const n = i(4276), o = i(5697), r = i(5768), s = i(1017), a = i(7012);

            class c {
                constructor() {
                    this.preTasks = [], this.preLaunchTaskLabel = `${c.buildTaskPrefix}${o.randomStr(r.taskTitleSuffixLength)}`
                }

                get execPath() {
                    return this._execPath
                }

                set execPath(e) {
                    this._execPath = e, this.execName = s.basename(e)
                }

                get debuggerType() {
                    return this._debuggerType
                }

                set debuggerType(e) {
                    this._debuggerType = e
                }

                get startDebugType() {
                    return this._startDebugType
                }

                set startDebugType(e) {
                    this._startDebugType = e
                }

                get sourceFilePath() {
                    return this._sourceFilePath
                }

                set sourceFilePath(e) {
                    this._sourceFilePath = e;
                    const t = a.getTargetFilePathFromSourcePath(e);
                    t && (this.execPath = t)
                }

                get buildType() {
                    return this._buildType
                }

                set buildType(e) {
                    this._buildType = e
                }

                get workspaceFolder() {
                    return this._workspaceFolder
                }

                set workspaceFolder(e) {
                    this._workspaceFolder = e
                }

                buildConfig(e) {
                    let t, i, n, s;
                    switch (this.buildType) {
                        case"singleFile":
                            t = `Cangjie Debug (${this.debuggerType}): ${this.execName}`, i = "launch", n = this.execPath;
                            break;
                        case"cangjieProject":
                            t = `Cangjie Debug (${this.debuggerType}): ${this.startDebugType}`, i = this.startDebugType, n = o.getDefaultBuildBinaryPath(), s = !0;
                            break;
                        case"chooseFile":
                            t = `Cangjie Debug (${this.debuggerType}): ${this.startDebugType}`, i = this.startDebugType, n = r.debuggeePathPlaceholder
                    }
                    const a = {name: t, program: n, request: i, type: r.debugType};
                    if ("launch" === this.startDebugType ? (a.externalConsole = !1, a.buildBeforeLaunch = s) : (a.remote = !1, a.processId = "", a.remoteAddress = "", a.remotePlatform = "", "linux" === o.getOs() && (a.remotePlatform = "remote-linux")), !0 !== e) {
                        const e = this.buildPreTask();
                        e && e.length > 0 && (a.preLaunchTask = this.preLaunchTaskLabel)
                    }
                    return a
                }

                buildPreTask() {
                    if (this.preTasks.length > 0) return this.preTasks;
                    if ("attach" === this.startDebugType) return [];
                    switch (this.buildType) {
                        case"singleFile": {
                            const e = o.getCjcPath(),
                                t = a.createSingleFileBuildCommand(e, this.sourceFilePath), [i] = t;
                            t.shift(), this.preTasks = [{
                                type: n.cangjieBuildType,
                                cmd: i,
                                args: t,
                                label: this.preLaunchTaskLabel
                            }];
                            break
                        }
                    }
                    return this.preTasks
                }
            }

            t.CangjieDebugConfigAndPreTaskBuilder = c, c.buildTaskPrefix = `${r.debugType} build task - `
        }, 389: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.CangjieDebugConfigurationProvider = void 0;
            const o = i(9496), r = i(5768), s = i(1773), a = i(4050), c = i(310), l = i(5828), u = i(5697), d = i(4900),
                p = i(5755);

            class h {
                static createDebugConfigurationAsync(e, t, i) {
                    var r;
                    return n(this, void 0, void 0, (function* () {
                        const n = u.isCangjieProject() && u.isBuildCommandAvailable(),
                            a = new l.CangjieDebugConfigAndPreTaskBuilder;
                        if (n) a.workspaceFolder = e, a.debuggerType = "cjdb", a.startDebugType = "launch", a.buildType = "cangjieProject"; else {
                            const t = yield o.window.showQuickPick(s.debuggerAndStartTypeArr.map((e => new d.DebugAndStartTypeItem(e))));
                            if (!t) return null;
                            let i = ["chooseFile"];
                            const n = null === (r = o.window.activeTextEditor) || void 0 === r ? void 0 : r.document.fileName;
                            let c;
                            if (null !== n && i.unshift("singleFile"), "attach" === t.typeTuple[1] && (i = ["chooseFile"]), 1 === i.length ? [c] = i : c = (yield o.window.showQuickPick(i.map((e => new p.BuildTypeItem(e))))).buildType, "singleFile" === c) a.sourceFilePath = n;
                            a.workspaceFolder = e, [a.debuggerType, a.startDebugType] = t.typeTuple, a.buildType = c
                        }
                        const h = a.buildPreTask(), g = a.buildConfig();
                        return yield c.addDataToVSCodeJsonFile(e, c.tasksJsonType, h), t && (yield c.addDataToVSCodeJsonFile(e, c.launchJsonType, [g])), i && "chooseFile" !== a.buildType && o.debug.startDebugging(e, g), g
                    }))
                }

                static configBaseCheck(e) {
                    if ("launch" !== e.request && "attach" !== e.request) throw new Error('The value of request can be "launch" or "attach"');
                    u.checkConfigFieldLength(e.program, "program"), u.checkConfigFieldLength(e.processId, "processId"), u.checkConfigFieldLength(e.preLaunchTask, "preLaunchTask"), u.checkConfigFieldLength(e.remoteAddress, "remoteAddress"), u.checkConfigFieldLength(e.remotePlatform, "remotePlatform"), u.checkConfigFieldLength(e.remoteFilePath, "remoteFilePath"), u.checkConfigFieldLength(e.remoteCangjieSdkPath, "remoteCangjieSdkPath");
                    const {scriptCommands: t} = e;
                    if (null != t) for (let e = 0; e < t.length; e++) u.checkConfigFieldLength(t[e], "scriptCommands")
                }

                static attachConfigCheck(e) {
                    return n(this, void 0, void 0, (function* () {
                        if (e.remote) this.remoteConfigCheck(e); else {
                            const t = yield a.pickNativeProcess();
                            e.processId = t.pid.toString(), e.program = t.getProgramPath()
                        }
                    }))
                }

                static remoteConfigCheck(e) {
                    if (null === e.remoteAddress || "" === u.trimSpacesAndLineBreaks(e.remoteAddress)) throw new Error("remoteAddress not set");
                    if (e.remoteAddress = `connect://${e.remoteAddress}`, null === e.remotePlatform || "" === u.trimSpacesAndLineBreaks(e.remotePlatform)) throw new Error("remotePlatform not set")
                }

                static launchConfigCheck(e, t) {
                    return n(this, void 0, void 0, (function* () {
                        if (e.remote) {
                            if (this.remoteConfigCheck(e), null === e.remoteFilePath || "" === u.trimSpacesAndLineBreaks(e.remoteFilePath)) throw new Error("remoteFilePath not set");
                            if (null === e.remoteCangjieSdkPath || "" === u.trimSpacesAndLineBreaks(e.remoteCangjieSdkPath)) throw new Error("remoteCangjieSdkPath not set")
                        }
                        if (null === e.program || e.program === r.debuggeePathPlaceholder) {
                            if (e.program = yield u.selectFileFromWorkspace(t), !e.program || !t) throw new Error("target program not set");
                            if (yield c.updatePlaceholderInLaunchJson("program", r.debuggeePathPlaceholder, e.program, e, t), !e.preLaunchTask && !u.isExistingFile(e.program)) throw new Error("target program file doesn't exist or permission denied")
                        } else {
                            if (!(e.remote || e.preLaunchTask || e.buildBeforeLaunch || u.isExistingFile(e.program))) throw new Error("target program file doesn't exist or permission denied")
                        }
                    }))
                }

                static resolveDebugConfiguration(e, t) {
                    return n(this, void 0, void 0, (function* () {
                        this.configBaseCheck(e), "attach" === e.request ? yield this.attachConfigCheck(e) : yield this.launchConfigCheck(e, t)
                    }))
                }

                provideDebugConfigurations(e, t) {
                    return this.provideDebugConfigurationsAsync(e)
                }

                resolveDebugConfiguration(e, t, i) {
                    return this.resolveDebugConfigurationAsync(t, e)
                }

                createDebugConfiguration(e, t, i) {
                    return h.createDebugConfigurationAsync(e, i, t)
                }

                provideDebugConfigurationsAsync(e) {
                    return n(this, void 0, void 0, (function* () {
                        const t = yield this.createDebugConfiguration(e, !0, !1);
                        return t ? [t] : []
                    }))
                }

                resolveDebugConfigurationAsync(e, t) {
                    return n(this, void 0, void 0, (function* () {
                        let i = e;
                        if (!i || !i.type) {
                            if (!t) return null;
                            if (0 !== c.getVSCodeJsonFileDataArray(t, c.launchJsonType).length) return null;
                            i = yield this.createDebugConfiguration(t, !1, !0)
                        }
                        const n = i;
                        return null !== n.cwd && void 0 !== n.cwd && (n.cwd = u.unifySlashOfPath(n.cwd)), null === n.externalConsole && (n.externalConsole = !1), yield h.resolveDebugConfiguration(n, t), n.program = u.unifySlashOfPath(n.program), n
                    }))
                }
            }

            t.CangjieDebugConfigurationProvider = h
        }, 3010: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.launchJsonCompletionFinishedCallback = t.CangjieDebugConfigurationSnippetProvider = void 0;
            const o = i(9496), r = i(9496), s = i(5697), a = i(1096), c = i(1773), l = i(5755), u = i(5828), d = i(310),
                p = i(3644), h = i(5768);
            t.CangjieDebugConfigurationSnippetProvider = class {
                provideCompletionItems(e, t, i, n) {
                    const l = new p.JsonCommentsHandler(e.getText()).stripJsonComments(), d = JSON.parse(l);
                    let g = [];
                    for (const t of c.debuggerAndStartTypeArr) {
                        const i = o.workspace.getWorkspaceFolder(e.uri), [n, c] = t,
                            l = new u.CangjieDebugConfigAndPreTaskBuilder;
                        l.workspaceFolder = i, l.debuggerType = n, l.startDebugType = c, l.buildType = "chooseFile";
                        const p = l.buildConfig(), m = new r.CompletionItem(p.name);
                        p.name = s.getIncrementalName(p.name, d.configurations.map((e => e.name))), m.insertText = JSON.stringify(p, null, h.spaceFillNum), m.command = {
                            title: "Update Path",
                            command: a.launchJsonCompletionFinishedCallback,
                            arguments: [p, i, l]
                        }, g.push(m)
                    }
                    return 0 !== d.configurations.length && g.forEach((e => e.insertText = `${e.insertText},`)), new o.CompletionList(g, !0)
                }

                resolveCompletionItem(e, t) {
                    return e
                }
            }, t.launchJsonCompletionFinishedCallback = function (e, t, i) {
                var r;
                return n(this, void 0, void 0, (function* () {
                    let n, a = ["singleFile", "chooseFile"];
                    switch (s.isCangjieProject() && a.unshift("cangjieProject"), "attach" === i.startDebugType && (a = ["chooseFile"]), a.length > 1 && (n = null === (r = yield o.window.showQuickPick(a.map((e => new l.BuildTypeItem(e))))) || void 0 === r ? void 0 : r.buildType), n || (n = "chooseFile"), i.buildType = n, n) {
                        case"singleFile": {
                            const e = {
                                canSelectMany: !1,
                                canSelectFolders: !1,
                                canSelectFiles: !0,
                                defaultUri: t.uri,
                                filters: {"Cangjie Source File": ["cj"]}
                            }, n = yield o.window.showOpenDialog(e);
                            if (!n || 0 === n.length) throw new Error("Cangjie source file not selected");
                            i.sourceFilePath = n[0].fsPath;
                            break
                        }
                    }
                    const c = i.buildPreTask(), u = i.buildConfig();
                    yield o.workspace.saveAll(!1), yield d.addDataToVSCodeJsonFile(t, d.tasksJsonType, c), yield d.substituteDataInVSCodeJsonFile(t, d.launchJsonType, e, u), "chooseFile" !== n && (yield o.debug.startDebugging(t, u))
                }))
            }
        }, 1773: (e, t) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.debuggerAndStartTypeArr = void 0, t.debuggerAndStartTypeArr = [["cjdb", "launch"], ["cjdb", "attach"]]
        }, 6682: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.CangjieSocketDebugAdapter = void 0;
            const o = i(9496), r = i(1808), s = i(5697), a = i(2445), c = i(3470), l = i(5768), u = i(9792);

            class d {
                constructor(e, t) {
                    this.serverMsgEventEmitter = new o.EventEmitter, this.onDidSendMessage = this.serverMsgEventEmitter.event, this.rawData = Buffer.allocUnsafe(0), this.contentSize = -1, this._socketConnect = !1, this.filterMap = new Map, this.listenerMap = new Map, this.destroySocketTimeMillis = 1e3, this.disposables = [], this.port = e, this._config = t, this.addMessageListener("response", "disconnect", (() => this.destroySocket()))
                }

                get socketConnect() {
                    return this._socketConnect
                }

                static getMapKey(e, t) {
                    return `${t}-${e}`
                }

                handleMessage(e) {
                    const t = this.preHandleMessage(e);
                    if (t) if (this.socket) {
                        const e = JSON.stringify(t);
                        this.socket.write(`Content-Length: ${Buffer.byteLength(e, "utf8")}${d.TWO_CRLF}${e}`, "utf8")
                    } else s.getOutputChannel().appendLine("error: sending message before socket established")
                }

                init() {
                    return n(this, void 0, void 0, (function* () {
                        let e = 0;
                        for (; !this._socketConnect && u.CangjieDebugAdapterDescriptorFactory.dapServerStartStatus && e < l.startUpRetryMaxCount;) e++, e % 20 == 0 && o.window.showInformationMessage("Try to connect to the debug server, please wait."), void 0 !== this.socket && null !== this.socket && this.socket.connecting || (this.socket = r.createConnection(this.port, "127.0.0.1").on("data", (e => this.handleData(e))).on("connect", (() => {
                            this._socketConnect = !0
                        })).on("close", (() => this.terminate()))), yield s.delay(l.startUpRetryInterval)
                    }))
                }

                terminate() {
                    const e = {event: "terminated", seq: Number.MAX_SAFE_INTEGER, type: "event"};
                    this.serverMsgEventEmitter.fire(e)
                }

                addMessageFilter(e, t, i) {
                    const n = d.getMapKey(e, t);
                    this.filterMap.has(n) || this.filterMap.set(n, []);
                    const o = new a.DapMessageFilter(i);
                    return this.filterMap.get(n).push(o), o.disposable
                }

                addMessageListener(e, t, i) {
                    const n = d.getMapKey(e, t);
                    this.listenerMap.has(n) || this.listenerMap.set(n, []);
                    const o = new c.DapMessageListener(i);
                    return this.listenerMap.get(n).push(o), o.disposable
                }

                dispose() {
                    setTimeout((() => this.destroySocket()), this.destroySocketTimeMillis), this.disposables.forEach((e => e.dispose()))
                }

                addDisposable(e) {
                    this.disposables.push(e)
                }

                destroySocket() {
                    null === this.socket || this.socket.destroyed || this.socket.destroy()
                }

                handleData(e) {
                    this.rawData = Buffer.concat([this.rawData, e]);
                    for (; ;) {
                        if (this.contentSize >= 0) {
                            if (this.rawData.length >= this.contentSize) {
                                const e = this.rawData.toString("utf8", 0, this.contentSize);
                                this.rawData = this.rawData.slice(this.contentSize), this.contentSize = -1, e.length > 0 && this.handleServerMessage(JSON.parse(e));
                                continue
                            }
                        } else {
                            const e = this.rawData.indexOf(d.TWO_CRLF);
                            if (-1 !== e) {
                                this.getContentSize(e), this.rawData = this.rawData.slice(e + d.TWO_CRLF.length);
                                continue
                            }
                        }
                        break
                    }
                }

                getContentSize(e) {
                    const t = this.rawData.toString("utf8", 0, e).split(d.HEADER_LINE_SEPARATOR);
                    for (const e of t) {
                        const t = e.split(d.HEADER_FIELD_SEPARATOR);
                        "Content-Length" === t[0] && (this.contentSize = Number(t[1]))
                    }
                }

                preHandleMessage(e) {
                    let t;
                    switch (e.type) {
                        case"request":
                            t = e.command;
                            break;
                        case"response":
                            t = e.command;
                            break;
                        case"event":
                            t = e.event;
                            break
                    }
                    const i = e.type, n = t, o = d.getMapKey(i, n);
                    let r = this.filterMap.get(o), s = e;
                    r && (r = r.filter((e => (!e.disposable.disposed && s && (s = e.filterFunction(s)), !e.disposable.disposed))), this.filterMap.set(o, r));
                    let a = this.listenerMap.get(o);
                    return a && (a = a.filter((e => (!e.disposable.disposed && s && e.listenerFunction(s), !e.disposable.disposed))), this.listenerMap.set(o, a)), s
                }

                handleServerMessage(e) {
                    const t = this.preHandleMessage(e);
                    t && this.serverMsgEventEmitter.fire(t)
                }
            }

            t.CangjieSocketDebugAdapter = d, d.TWO_CRLF = "\r\n\r\n", d.HEADER_LINE_SEPARATOR = /\r?\n/, d.HEADER_FIELD_SEPARATOR = /: */
        }, 1096: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.openFileInEditor = t.launchJsonCompletionFinishedCallback = t.buildAndDebugCurrentFile = t.prefix = void 0;
            const o = i(9496);
            t.prefix = "cangjie.debug.", t.buildAndDebugCurrentFile = `${t.prefix}buildAndDebugCurrentFile`, t.launchJsonCompletionFinishedCallback = `${t.prefix}launchJsonCompletionFinishedCallback`, t.openFileInEditor = function (e) {
                return n(this, void 0, void 0, (function* () {
                    return o.commands.executeCommand("vscode.open", e)
                }))
            }
        }, 5697: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.isFieldLengthRight = t.checkConfigFieldLength = t.getEnvPaths = t.getCangjieHome = t.getSdkOption = t.findAPortNotInUse = t.buildCangjieProject = t.isBuildCommandAvailable = t.isCangjieProject = t.getDefaultBuildBinaryPath = t.getCjcPath = t.getLibLldbPath = t.configTerminalCangjieEnv = t.replaceEscapeCharacter = t.standardDebugReplyMsg = t.trimAllStartSpace = t.trimSpacesAndLineBreaks = t.execNativeProcess = t.execNativeCommand = t.isCjSourceFile = t.randomStr = t.executeScheduledTask = t.setExecPermission = t.sendRequest = t.isExistingFile = t.delay = t.toPromise = t.getIncrementalName = t.onTaskProcessEnded = t.selectFileFromWorkspace = t.unifySlashOfPath = t.setOutputChannel = t.getOutputChannel = t.setExtensionPath = t.getServerLogPath = t.getDapServerPath = t.getLauncherName = t.getDapServerName = t.getExecFileSuffix = t.getArch = t.GlobalNlCharacter = t.getOs = t.envPathName = void 0;
            const o = i(1017), r = i(9496), s = i(9496), a = i(5768), c = i(7147), l = i(2037), u = i(1808),
                d = i(2081), p = i(6113);
            let h, g;

            function m() {
                switch (l.platform()) {
                    case"win32":
                        return "win";
                    case"darwin":
                        return "mac";
                    default:
                        return "linux"
                }
            }

            t.envPathName = {
                CANGJIE_HOME: "CANGJIE_HOME",
                PATH: "PATH",
                LD_LIBRARY_PATH: "LD_LIBRARY_PATH"
            }, t.getOs = m;

            class f {
                static getNLCharacter() {
                    const e = f.nlMap.get(m());
                    return e || "\n"
                }
            }

            function v() {
                switch (l.arch()) {
                    case"arm":
                    case"arm64":
                        return "arm";
                    default:
                        return "x86"
                }
            }

            function y() {
                switch (m()) {
                    case"win":
                        return ".exe";
                    case"linux":
                    case"mac":
                        return v(), "";
                    default:
                        return ""
                }
            }

            function C() {
                return a.dapServerNameBase + y()
            }

            function w(e) {
                return new Promise(((t, i) => {
                    e.then((e => t(e)), (e => i(e)))
                }))
            }

            function b(e) {
                return n(this, void 0, void 0, (function* () {
                    yield new Promise((t => {
                        setTimeout(t, e)
                    }))
                }))
            }

            function k(e) {
                let t = e;
                return t = t.replace(/\\\\/g, "\\"), t = t.replace(/\\n/g, "\n"), t = t.replace(/\\t/g, "\t"), t = t.replace(/\\o/g, "o"), t = t.replace(/\\r/g, "\r"), t = t.replace(/\\v/g, "\v"), t = t.replace(/\\b/g, "\b"), t = t.replace(/\\f/g, "\f"), t = t.replace(/\\"/g, '"'), t = t.replace(/\\'/g, "'"), t
            }

            function S() {
                const e = T();
                if (!e) throw r.window.showErrorMessage("Cangjie sdk not set in settings"),
                    new Error("Cangjie sdk not set in settings");
                return `${e}${a.defualtLiblldbPath}`
            }

            function D(e) {
                const t = "127.0.0.1";
                return new Promise(((i, n) => {
                    const o = new u.Socket;
                    o.connect(e, t), o.on("connect", (() => {
                        o.destroy(), i()
                    })), o.on("error", (e => {
                        o.destroy(), n(e)
                    })), o.setTimeout(400), o.on("timeout", (() => {
                        o.destroy(), n(new Error(`Timeout (400ms) occurred waiting for ${t}:${e} to be available`))
                    }))
                }))
            }

            function P() {
                return "CJNative"
            }

            function T() {
                return s.workspace.getConfiguration("CangjieSdk").get("Path")
            }

            function R() {
                const e = T();
                if (void 0 === e) throw new Error("Please install the cangjie language service extension!");
                try {
                    if (!c.existsSync(e)) throw new Error("cangjie sdk path not exist, please configuration it first!");
                    let t = c.readFileSync(`${e}/envsetup.sh`, "utf8"), i = /\${script_dir}/g, n = /\${CANGJIE_HOME}/g,
                        o = /export(.)*/g;
                    if ("win32" === process.platform && (t = c.readFileSync(`${e}/envsetup.bat`, "utf8"), i = /%~dp0/g, n = /%CANGJIE_HOME%/g, o = /set(.)*/g), !t) throw new Error("envsetup.sh is empty");
                    t = t.replace(i, `${e}`), t = "linux" === process.platform ? t.replace(n, `${e}`) : t.replace(n, `${e}\\`);
                    return t.match(o)
                } catch (e) {
                    return []
                }
            }

            t.GlobalNlCharacter = f, f.nlMap = new Map([["win", "\r\n"], ["linux", "\n"], ["mac", "\n"]]), t.getArch = v, t.getExecFileSuffix = y, t.getDapServerName = C, t.getLauncherName = function () {
                return a.launcherNameBase + y()
            }, t.getDapServerPath = function () {
                return o.join(h, a.executableFolder, C())
            }, t.getServerLogPath = function () {
                return o.join(l.homedir(), a.serverLogPathSubFolder)
            }, t.setExtensionPath = function (e) {
                h = e
            }, t.getOutputChannel = function () {
                return g
            }, t.setOutputChannel = function (e) {
                g = e
            }, t.unifySlashOfPath = function (e) {
                let t = e;
                return "\\" === o.sep && (t = t.replace(/\\/g, "/")), t
            }, t.selectFileFromWorkspace = function (e, t = []) {
                return n(this, void 0, void 0, (function* () {
                    if ("Cancel" === (yield r.window.showQuickPick(["Open File Chooser", "Cancel"], {placeHolder: "Select Target Program File"}))) return;
                    const i = {canSelectMany: !1, canSelectFolders: !1, canSelectFiles: !0, defaultUri: e.uri};
                    t.length > 0 && (i.filters = {suffix: t});
                    const n = yield r.window.showOpenDialog(i);
                    return n && 0 !== n.length ? n[0].fsPath : void 0
                }))
            }, t.onTaskProcessEnded = function (e, t) {
                const i = r.tasks.onDidEndTaskProcess((n => {
                    n.execution === e && (t(n), i.dispose())
                }))
            }, t.getIncrementalName = function (e, t) {
                const i = t.map((t => {
                    if (t.startsWith(e)) {
                        if (t === e) return 0;
                        try {
                            const i = t.substr(e.length + 1);
                            return parseInt(i)
                        } catch (e) {
                            return null
                        }
                    }
                    return null
                })).filter((e => null !== e));
                if (0 === i.length) return e;
                const n = Math.max(...i);
                return `${e} ${n + 1}`
            }, t.toPromise = w, t.delay = b, t.isExistingFile = function (e) {
                return !!c.existsSync(e) && c.lstatSync(e).isFile()
            }, t.sendRequest = function (e, t) {
                return r.debug.activeDebugSession ? w(r.debug.activeDebugSession.customRequest(e, t)) : Promise.reject(new Error(`error: try send ${e} request when there is no active session`))
            }, t.setExecPermission = function (e) {
                c.chmodSync(e, "550")
            }, t.executeScheduledTask = function (e, t, i) {
                return n(this, void 0, void 0, (function* () {
                    let n = 0;
                    for (; n++, e(), n !== i;) yield b(t)
                }))
            }, t.randomStr = function (e) {
                const t = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789", i = p.randomBytes(e),
                    n = new Array(e);
                for (let o = 0; o < e; o++) n[o] = t[i[o] % 62];
                return n.join("")
            }, t.isCjSourceFile = function (e) {
                return e.endsWith(".cj")
            }, t.execNativeCommand = function (e) {
                return new Promise(((t, i) => {
                    d.exec(e, ((e, n, o) => {
                        e ? i(e) : o && o.length > 0 ? i(o) : t(n)
                    }))
                }))
            }, t.execNativeProcess = function (e, t) {
                return new Promise(((i, n) => {
                    const o = d.spawn(e, t);
                    o.on("error", (e => {
                        n(e)
                    })), o.on("exit", (e => {
                        0 === e ? i(e) : n(e)
                    }))
                }))
            }, t.trimSpacesAndLineBreaks = function (e) {
                return e.replace(/^\s+|\s+$/g, "")
            }, t.trimAllStartSpace = function (e) {
                let t = e;
                for (; " " === t.charAt(0);) t = t.substring(1);
                return t
            }, t.standardDebugReplyMsg = function (e) {
                const t = e.split(f.getNLCharacter());
                let i = "";
                for (let e of t) 0 !== e.length && ("@" === e.charAt(0) || "~" === e.charAt(0) || "&" === e.charAt(0) ? (e = e.substring(1), '"' === e.charAt(0) && (e = e.substring(1)), '"' === e.charAt(e.length - 1) && (e = e.substring(0, e.length - 1)), i += k(e)) : i = `${i + e}\n`);
                return i
            }, t.replaceEscapeCharacter = k, t.configTerminalCangjieEnv = function () {
                if ("linux" !== m()) return;
                const e = T();
                if (!c.existsSync(e)) return;
                let i, n = {};
                R().forEach((e => {
                    let t = e.replace("export ", "").split("=");
                    [, n[t[0]]] = t
                }));
                let o = S(),
                    r = s.workspace.getConfiguration("terminal.integrated.env.linux");
                if (r.has("LD_LIBRARY_PATH")) {
                    let e = r.get("LD_LIBRARY_PATH");
                    i = e.indexOf(o) < 0 ? `${o}:${e}` : e
                } else i = `${o}:${n[t.envPathName.LD_LIBRARY_PATH]}`;
                s.workspace.getConfiguration("terminal.integrated.env").update("linux", {
                    CANGJIE_HOME: `${e}`,
                    PATH: `${e}/bin:${e}/tools/bin:\${env:PATH}`,
                    LD_LIBRARY_PATH: `${i}`
                })
            }, t.getLibLldbPath = S, t.getCjcPath = function () {
                const e = T();
                if (!e) throw r.window.showErrorMessage("Cangjie sdk not set in settings"), new Error("Cangjie sdk not set in settings");
                return "win" === m() ? `${e}\\bin\\cjc.exe` : `${e}/bin/cjc`
            }, t.getDefaultBuildBinaryPath = function () {
                const e = s.workspace.workspaceFolders[0].uri.fsPath;
                return "win" === m() ? `${e}\\build\\bin\\main.exe` : `${e}/build/bin/main`
            }, t.isCangjieProject = function () {
                if (s.workspace.workspaceFolders) {
                    const e = s.workspace.workspaceFolders[0].uri.fsPath, t = o.join(e, "module.json");
                    return c.existsSync(t)
                }
                return !1
            }, t.isBuildCommandAvailable = function () {
                return r.commands.getCommands(!0).then((e => -1 !== e.indexOf(a.cjpmIncrementalCompilationCommand))), !0
            }, t.buildCangjieProject = function () {
                return n(this, void 0, void 0, (function* () {
                    yield r.commands.executeCommand(a.cjpmIncrementalCompilationCommand).then((e => {
                        if (!e) throw new Error("Build failed!")
                    }))
                }))
            }, t.findAPortNotInUse = function (e, t) {
                return n(this, void 0, void 0, (function* () {
                    for (let i = e; i <= t; i++) {
                        if (!0 === (yield D(i).catch((e => "ECONNREFUSED" === e.code)))) return i
                    }
                    throw new Error(`port range of ${e}-${t} is not available to start server`)
                }))
            }, t.getSdkOption = P, t.getCangjieHome = T, t.getEnvPaths = R, t.checkConfigFieldLength = function (e, t) {
                if (null != e && e.length > a.maxFieldLength) throw new Error(`The length of ${t} is not allowed to exceed ${a.maxFieldLength}`)
            }, t.isFieldLengthRight = function (e, t) {
                return !(null != e && e.length > a.maxFieldLength) || (r.window.showErrorMessage(`The length of ${t} is not allowed to exceed ${a.maxFieldLength}.`), !1)
            }
        }, 5768: (e, t) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}),
                t.taskTitleSuffixLength = t.maxFieldLength = t.spaceFillNum = t.cjpmIncrementalCompilationCommand = t.maximumNumberOfDataBreakpoint = t.portHigherBound = t.portLowerBound = t.startUpRetryInterval = t.startUpRetryMaxCount = t.launcherExtraParamPrefix = t.serverLogPathSubFolder = t.serverDebuggerType = t.serverLogPathArgPrefix = t.serverPortArgPrefix = t.launcherNameBase = t.dapServerNameBase = t.executableFolder = t.debugType = t.debuggerPathPlaceholder = t.debuggeePathPlaceholder = t.defualtLiblldbPath = t.debugSettingsPrefix = t.extensionId = void 0,
                t.extensionId = "IDE-Innovation-Lab.Cangjie",
                t.debugSettingsPrefix = "cangjie_cj_sdk",
                t.defualtLiblldbPath = "/third_party/llvm/lldb/lib/",
                t.debuggeePathPlaceholder = "${programPath}",
                t.debuggerPathPlaceholder = "${debuggerPath}",
                t.debugType = "cangjieDebug",
                t.executableFolder = "bin",
                t.dapServerNameBase = "dap_server",
                t.launcherNameBase = "launcher",
                t.serverPortArgPrefix = "--port=",
                t.serverLogPathArgPrefix = "--logpath=",
                t.serverDebuggerType = "--debuggertype=lldbapi",
                t.serverLogPathSubFolder = ".cangjie/debug/logs/server",
                t.launcherExtraParamPrefix = "--dbgParams=",
                t.startUpRetryMaxCount = 60,
                t.startUpRetryInterval = 100,
                t.portLowerBound = 9995, t.portHigherBound = 65535,
                t.maximumNumberOfDataBreakpoint = 4,
                t.cjpmIncrementalCompilationCommand = "cangjie.build.incrementWithDebug",
                t.spaceFillNum = 2, t.maxFieldLength = 1e4,
                t.taskTitleSuffixLength = 10
        }, 2445: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DapMessageFilter = void 0;
            const n = i(7282);
            t.DapMessageFilter = class {
                constructor(e) {
                    this._disposable = new n.MyDisposable, this.filterFunction = e
                }

                get disposable() {
                    return this._disposable
                }
            }
        }, 3470: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DapMessageListener = void 0;
            const n = i(7282);
            t.DapMessageListener = class {
                constructor(e) {
                    this._disposable = new n.MyDisposable, this.listenerFunction = e
                }

                get disposable() {
                    return this._disposable
                }
            }
        }, 7199: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DapMessageTrackerFactory = void 0;
            const n = i(7089);
            t.DapMessageTrackerFactory = class {
                constructor(e) {
                    this.out = e
                }

                createDebugAdapterTracker(e) {
                    return new n.Tracker(this.out)
                }
            }
        }, 4900: (e, t) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DebugAndStartTypeItem = void 0;
            t.DebugAndStartTypeItem = class {
                constructor(e) {
                    this.typeTuple = e, this.label = `Cangjie Debug (${e[0]}): ${e[1]}`
                }
            }
        }, 657: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.activate = void 0;
            const o = i(9496), r = i(389), s = i(9792), a = i(7199), c = i(2668), l = i(5768), u = i(3010), d = i(1096),
                p = i(4276), h = i(7012), g = i(5697);
            t.activate = function (e, t) {
                return n(this, void 0, void 0, (function* () {
                    g.setExtensionPath(e.extensionPath), g.setOutputChannel(t);
                    e.subscriptions.push(o.languages.registerCompletionItemProvider([{
                        scheme: "file",
                        language: "jsonc",
                        pattern: "**/launch.json"
                    }], new u.CangjieDebugConfigurationSnippetProvider)), e.subscriptions.push(o.debug.registerDebugConfigurationProvider(l.debugType, new r.CangjieDebugConfigurationProvider)), e.subscriptions.push(o.debug.registerDebugAdapterTrackerFactory(l.debugType, new a.DapMessageTrackerFactory(t)));
                    const i = new s.CangjieDebugAdapterDescriptorFactory;
                    e.subscriptions.push(o.debug.registerDebugAdapterDescriptorFactory(l.debugType, i)), e.subscriptions.push(o.debug.onDidTerminateDebugSession((() => i.sessionTerminated()))), e.subscriptions.push(o.commands.registerCommand(d.launchJsonCompletionFinishedCallback, u.launchJsonCompletionFinishedCallback)), e.subscriptions.push(o.commands.registerCommand(d.buildAndDebugCurrentFile, h.buildAndDebugSingleFile)), e.subscriptions.push(o.tasks.registerTaskProvider(p.cangjieBuildType, new c.CangjieBuildTaskProvider))
                }))
            }
        }, 3644: (e, t) => {
            "use strict";
            var i;
            Object.defineProperty(t, "__esModule", {value: !0}), t.JsonCommentsHandler = void 0, function (e) {
                e[e.NOTCOMMENT = 0] = "NOTCOMMENT", e[e.SINGLECOMMENT = 1] = "SINGLECOMMENT", e[e.MULTICOMMENT = 2] = "MULTICOMMENT"
            }(i || (i = {}));

            class n {
                constructor(e) {
                    this.jsonString = e, this.isInsideString = !1, this.isInsideComment = i.NOTCOMMENT, this.offset = 0, this.buffer = "", this.result = "", this.commaIndex = -1
                }

                static strip(e, t, i) {
                    return e.slice(t, i).replace(/\S/g, " ")
                }

                static isEscaped(e, t) {
                    let i = t - 1, n = 0;
                    for (; "\\" === e[i];) i -= 1, n += 1;
                    return Boolean(n % 2)
                }

                stripJsonComments() {
                    if ("string" != typeof this.jsonString) throw new TypeError(`Expected argument \`jsonString\` to be a \`string\`, got \`${typeof this.jsonString}\``);
                    for (let e = 0; e < this.jsonString.length; e++) {
                        const t = this.jsonString[e], o = this.jsonString[e + 1];
                        if (!this.isInsideComment && '"' === t) {
                            n.isEscaped(this.jsonString, e) || (this.isInsideString = !this.isInsideString)
                        }
                        this.isInsideString || (this.isInsideComment || t + o !== "//" ? this.isInsideComment === i.SINGLECOMMENT && t + o === "\r\n" ? this.exitSingleLineComments(e) : this.isInsideComment === i.SINGLECOMMENT && "\n" === t ? this.exitSingleLineComments2(e) : this.isInsideComment || t + o !== "/*" ? this.isInsideComment === i.MULTICOMMENT && t + o === "*/" ? this.exitMultilineComments(e) : this.isInsideComment || this.handleNotComment(t, e) : this.enterMultilineComments(e) : this.enterSingleLineComments(e))
                    }
                    return this.result + this.buffer + (this.isInsideComment ? n.strip(this.jsonString.slice(this.offset), 0, 0) : this.jsonString.slice(this.offset))
                }

                enterSingleLineComments(e) {
                    this.buffer += this.jsonString.slice(this.offset, e), this.offset = e, this.isInsideComment = i.SINGLECOMMENT
                }

                exitSingleLineComments(e) {
                    let t = e + 1;
                    this.isInsideComment = i.NOTCOMMENT, this.buffer += n.strip(this.jsonString, this.offset, t), this.offset = t
                }

                exitSingleLineComments2(e) {
                    this.isInsideComment = i.NOTCOMMENT, this.buffer += n.strip(this.jsonString, this.offset, e), this.offset = e
                }

                enterMultilineComments(e) {
                    this.buffer += this.jsonString.slice(this.offset, e), this.offset = e, this.isInsideComment = i.MULTICOMMENT
                }

                exitMultilineComments(e) {
                    let t = e + 1;
                    this.isInsideComment = i.NOTCOMMENT, this.buffer += n.strip(this.jsonString, this.offset, t + 1), this.offset = t + 1
                }

                handleNotComment(e, t) {
                    -1 !== this.commaIndex ? "}" === e || "]" === e ? (this.buffer += this.jsonString.slice(this.offset, t), this.result += n.strip(this.buffer, 0, 1) + this.buffer.slice(1), this.buffer = "", this.offset = t, this.commaIndex = -1) : " " !== e && "\t" !== e && "\r" !== e && "\n" !== e && (this.buffer += this.jsonString.slice(this.offset, t), this.offset = t, this.commaIndex = -1) : "," === e && (this.result += this.buffer + this.jsonString.slice(this.offset, t), this.buffer = "", this.offset = t, this.commaIndex = t)
                }
            }

            t.JsonCommentsHandler = n
        }, 310: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.updatePlaceholderInLaunchJson = t.substituteDataInVSCodeJsonFile = t.addDataToVSCodeJsonFile = t.getVSCodeJsonFileDataArray = t.tasksJsonType = t.launchJsonType = void 0;
            const o = i(9496);

            function r(e, t) {
                let i = o.workspace.getConfiguration(t.fileName, e).get(t.jsonArrField);
                return i || (i = []), i
            }

            function s(e, t, i, s) {
                return n(this, void 0, void 0, (function* () {
                    if (!i || !s) return;
                    let n = r(e, t);
                    for (let e = 0; e < n.length; e++) if ({}.hasOwnProperty.call(n, e)) {
                        const t = n[e];
                        if (JSON.stringify(t) === JSON.stringify(i)) {
                            n[e] = s;
                            break
                        }
                    }
                    const a = o.workspace.getConfiguration(t.fileName, e);
                    yield a.update(t.jsonArrField, n, o.ConfigurationTarget.WorkspaceFolder)
                }))
            }

            t.launchJsonType = {
                fileName: "launch",
                jsonArrField: "configurations",
                version: "0.2.0"
            }, t.tasksJsonType = {
                fileName: "tasks",
                jsonArrField: "tasks",
                version: "2.0.0"
            }, t.getVSCodeJsonFileDataArray = r, t.addDataToVSCodeJsonFile = function (e, t, i) {
                return n(this, void 0, void 0, (function* () {
                    if (!i) return;
                    const n = o.workspace.getConfiguration(t.fileName, e);
                    let s = r(e, t);
                    0 === s.length && (yield n.update("version", t.version, o.ConfigurationTarget.WorkspaceFolder)), s = s.concat(i), yield n.update(t.jsonArrField, s, o.ConfigurationTarget.WorkspaceFolder)
                }))
            }, t.substituteDataInVSCodeJsonFile = s, t.updatePlaceholderInLaunchJson = function (e, i, o, a, c) {
                return n(this, void 0, void 0, (function* () {
                    const n = r(c, t.launchJsonType).filter((t => {
                        const n = t;
                        return n.type === a.type && n.name === a.name && n.request === a.request && n[e] === i
                    }));
                    if (n.length > 0) for (const i of n) {
                        const n = i;
                        n[e] = o, yield s(c, t.launchJsonType, i, n)
                    }
                }))
            }
        }, 7282: (e, t) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.MyDisposable = void 0;
            t.MyDisposable = class {
                constructor() {
                    this._disposed = !1
                }

                get disposed() {
                    return this._disposed
                }

                dispose() {
                    this._disposed = !0
                }
            }
        }, 4276: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.resolveBuildTaskDefinition = t.cangjieBuildType = void 0;
            const n = i(9496), o = i(5768), r = i(1017);
            t.cangjieBuildType = "cangjieDebugBuild", t.resolveBuildTaskDefinition = function (e) {
                return {
                    definition: e,
                    isBackground: !1,
                    name: `${o.debugType} - ${r.basename(e.cmd)}`,
                    execution: new n.ProcessExecution(e.cmd, e.args),
                    source: o.debugType,
                    presentationOptions: {
                        focus: !0,
                        reveal: n.TaskRevealKind.Always,
                        clear: !0,
                        echo: !0,
                        panel: n.TaskPanelKind.Shared,
                        showReuseMessage: !1
                    },
                    runOptions: {},
                    problemMatchers: [],
                    scope: n.TaskScope.Workspace
                }
            }
        }, 7089: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.Tracker = void 0;
            const n = i(5768);
            t.Tracker = class {
                constructor(e) {
                    this.out = e
                }

                onWillStartSession() {
                    this.out.appendLine("start session")
                }

                onWillReceiveMessage(e) {
                    this.out.appendLine(`================== send message to server at ${(new Date).toLocaleTimeString()} ======================`), this.out.appendLine(JSON.stringify(e, null, n.spaceFillNum))
                }

                onDidSendMessage(e) {
                    this.out.appendLine(`================== receive message from server at ${(new Date).toLocaleTimeString()} =================`), this.out.appendLine(JSON.stringify(e, null, n.spaceFillNum))
                }

                onWillStopSession() {
                    this.out.appendLine("stop session")
                }

                onError(e) {
                    this.out.appendLine(`================== error message at ${(new Date).toLocaleTimeString()}  ==============`), this.out.appendLine(e.name), this.out.appendLine(e.message), e.stack && this.out.append(e.stack)
                }

                onExit(e, t) {
                    this.out.appendLine(`exit with code ${e} and with signal ${t}`)
                }
            }
        }, 7581: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.activate = void 0;
            const o = i(8883), r = i(966), s = i(2635), a = i(9310);
            t.activate = function (e) {
                return n(this, void 0, void 0, (function* () {
                    a.Utility.serverRun = !0, o.activate(e), r.activate(e), s.activate(e)
                }))
            }
        }, 2155: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.ClangdContext = t.isClangdDocument = t.clangdDocumentSelector = void 0;
            const o = i(9496), r = i(6396), s = i(1017), a = i(2037), c = i(7220), l = i(3503), u = i(4962);

            function d(e) {
                return o.languages.match(t.clangdDocumentSelector, e)
            }

            t.clangdDocumentSelector = [{scheme: "file", language: "c"}, {
                scheme: "file",
                language: "cpp"
            }, {scheme: "file", language: "cuda-cpp"}, {scheme: "file", language: "objective-c"}, {
                scheme: "file",
                language: "objective-cpp"
            }], t.isClangdDocument = d;

            class p extends r.LanguageClient {
                handleFailedRequest(e, t, i, n) {
                    return t instanceof r.ResponseError && "workspace/executeCommand" === e.method && o.window.showErrorMessage(t.message), super.handleFailedRequest(e, i, t, n)
                }
            }

            class h {
                initialize() {
                }

                fillClientCapabilities(e) {
                    var t;
                    (null === (t = e.textDocument) || void 0 === t ? void 0 : t.completion).editsNearCursor = !0
                }

                getState() {
                    return {kind: "static"}
                }

                dispose() {
                }
            }

            class g {
                constructor() {
                    this.subscriptions = []
                }

                static getDefaultServerPath() {
                    const e = "win32" === a.platform(), t = u.Utility.getSdkOption(),
                        i = e ? "LSPServer.exe" : "LSPServer", n = s.resolve(s.join(g.cwd, "bin"));
                    var o = null;
                    o = e ? "windows" : "arm64" === a.arch() ? "aarch64" : "x64";
                    var r = s.resolve(s.join(n, t, o, i));
                    return r = r.trim()
                }

                static setEnv() {
                    "linux" === process.platform ? g.env = u.Utility.getExportLDPath() : g.env = u.Utility.getWindowsPath()
                }

                activate(e, t, i, s, a) {
                    return n(this, void 0, void 0, (function* () {
                        g.cwd = s.extensionPath, g.setEnv();
                        const e = {command: g.getDefaultServerPath(), args: ["src"], options: {cwd: g.cwd, env: g.env}},
                            t = c.get("trace");
                        if (t) {
                            const i = {CLANGD_TRACE: t};
                            e.options = {env: Object.assign(Object.assign({}, process.env), i)}
                        }
                        const i = e, n = o.window.createOutputChannel("Cangjie Language Server Trace"), d = {
                            documentSelector: [{scheme: "file", language: "Cangjie"}],
                            progressOnInitialization: !0,
                            outputChannelName: "Cangjie Language Server Trace",
                            stdioEncoding: "utf8",
                            initializationOptions: null != a ? a : {
                                clangdFileStatus: !0,
                                fallbackFlags: c.get("fallbackFlags")
                            },
                            outputChannel: n,
                            revealOutputChannelOn: r.RevealOutputChannelOn.Never,
                            synchronize: {fileEvents: o.workspace.createFileSystemWatcher("**/*", !1, !0, !1)},
                            middleware: {
                                didOpen: l.didOpenMidware,
                                provideDocumentLinks: l.documentLinkMidware,
                                provideDefinition: l.definitionMidware
                            }
                        };
                        this.client || (this.client = new p("Cangjie Language Server", i, d), this.client.clientOptions.errorHandler = this.client.createDefaultErrorHandler(c.get("restartAfterCrash") ? 4 : 0), this.client.registerFeature(new h)), o.window.showInformationMessage("Cangjie Project is initializing"), yield this.client.start(), o.window.showInformationMessage("Cangjie Project initialized"), u.Utility.serverRun = !0
                    }))
                }

                get visibleClangdEditors() {
                    return o.window.visibleTextEditors.filter((e => d(e.document)))
                }

                dispose() {
                    return n(this, void 0, void 0, (function* () {
                        this.subscriptions.forEach((e => {
                            e.dispose()
                        })), this.client && (yield this.client.stop(), this.client = null, this.subscriptions = [])
                    }))
                }
            }

            t.ClangdContext = g
        }, 7220: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.update = t.getSecureOrPrompt = t.getSecure = t.get = void 0;
            const o = i(1017), r = i(9496);

            function s(e) {
                if ("string" == typeof e) e = e.replace(/\$\{(.*?)\}/g, ((e, t) => {
                    var i;
                    return null !== (i = function (e) {
                        var t;
                        if ("workspaceRoot" === e || "workspaceFolder" === e || "cwd" === e) return void 0 !== r.workspace.rootPath ? r.workspace.rootPath : void 0 !== r.window.activeTextEditor ? o.dirname(r.window.activeTextEditor.document.uri.fsPath) : process.cwd();
                        const i = "env:";
                        if (e.startsWith(i)) return null !== (t = process.env[e.substr(i.length)]) && void 0 !== t ? t : "";
                        const n = "config:";
                        if (e.startsWith(n)) {
                            const t = r.workspace.getConfiguration().get(e.substr(n.length));
                            return "string" == typeof t ? t : void 0
                        }

                    }(t)) && void 0 !== i ? i : e
                })); else if (Array.isArray(e)) e = e.map((e => s(e))); else if ("object" == typeof e) {
                    const t = {};
                    for (let [i, n] of Object.entries(e)) t[i] = s(n);
                    e = t
                }
                return e
            }

            t.get = function (e) {
                return s(r.workspace.getConfiguration("clangd").get(e))
            }, t.getSecure = function (e, t) {
                var i;
                const n = new a(e, t);
                return n.get(null !== (i = n.blessed) && void 0 !== i && i)
            }, t.getSecureOrPrompt = function (e, t) {
                return n(this, void 0, void 0, (function* () {
                    const i = new a(e, t);
                    if (!i.mismatched) return i.get(!1);
                    const n = i.blessed;
                    if (void 0 !== n) return i.get(n);
                    const o = "Yes, use this setting", s = "No, use my default", c = "More Info";
                    switch (yield r.window.showWarningMessage(`This workspace wants to set clangd.${e} to ${i.insecureJSON}.\n    \u2029\n    This will override your default of ${i.secureJSON}.`, o, s, c)) {
                        case c:
                            r.env.openExternal(r.Uri.parse("https://github.com/clangd/vscode-clangd/blob/master/docs/settings.md#security"));
                            break;
                        case o:
                            return yield i.bless(!0), i.get(!0);
                        case s:
                            yield i.bless(!1)
                    }
                    return i.get(!1)
                }))
            }, t.update = function (e, t, i) {
                return r.workspace.getConfiguration("clangd").update(e, t, i)
            };

            class a {
                constructor(e, t) {
                    var i;
                    this.workspaceState = t;
                    const n = r.workspace.getConfiguration("clangd"), o = n.inspect(e);
                    this.secure = null !== (i = o.globalValue) && void 0 !== i ? i : o.defaultValue, this.insecure = n.get(e), this.secureJSON = JSON.stringify(this.secure), this.insecureJSON = JSON.stringify(this.insecure), this.blessKey = "bless." + e
                }

                get mismatched() {
                    return this.secureJSON !== this.insecureJSON
                }

                get(e) {
                    return s(e ? this.insecure : this.secure)
                }

                get blessed() {
                    let e = this.workspaceState.get(this.blessKey);
                    if (e && e.json === this.insecureJSON) return e.allowed
                }

                bless(e) {
                    return n(this, void 0, void 0, (function* () {
                        yield this.workspaceState.update(this.blessKey, {json: this.insecureJSON, allowed: e})
                    }))
                }
            }
        }, 8883: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.activate = void 0;
            const o = i(9496), r = i(1017), s = i(2614), a = i(4962), c = i(2155);
            t.activate = function (e) {
                var t;
                return n(this, void 0, void 0, (function* () {
                    const i = o.window.createOutputChannel("cangjie");
                    e.subscriptions.push(i);
                    const l = new c.ClangdContext;
                    e.subscriptions.push(l), o.workspace.getConfiguration("editor").update("gotoLocation.alternativeDeclarationCommand", "editor.action.revealDefinition"), o.workspace.getConfiguration("editor").update("gotoLocation.alternativeDefinitionCommand", "editor.action.revealDefinition"), o.workspace.getConfiguration("editor").update("gotoLocation.alternativeTypeDefinitionCommand", "editor.action.revealDefinition"), o.workspace.getConfiguration("editor").update("selectionHighlight", !1), o.workspace.getConfiguration("files").update("autoSave", "onFocusChange"), o.workspace.getConfiguration("editor").update("suggest.snippetsPreventQuickSuggestions", !1), o.workspace.getConfiguration("editor").update("quickSuggestions", {
                        other: "on",
                        comments: "off",
                        strings: "on"
                    }), e.subscriptions.push(o.commands.registerCommand("cangjie.activate", (() => n(this, void 0, void 0, (function* () {
                    }))))), e.subscriptions.push(o.commands.registerCommand("cangjie.restart", (() => n(this, void 0, void 0, (function* () {
                        yield l.dispose(), yield l.activate(e.globalStoragePath, i, e.workspaceState, e)
                    }))))), yield l.activate(e.globalStoragePath, i, e.workspaceState, e, a.Utility.getInitializationOptions());
                    const u = o.workspace.createFileSystemWatcher(new o.RelativePattern(o.workspace.workspaceFolders[0], "**/module.json"));
                    e.subscriptions.push(u.onDidChange((t => n(this, void 0, void 0, (function* () {
                        const n = a.Utility.getAllKeys(), s = r.dirname(t.toString());
                        if (!n.includes(s)) return;
                        let c = !1;
                        if (c) return;
                        c = !0;
                        "Yes" === (yield o.window.showQuickPick(["Yes", "No"], {
                            title: "module.json is modified!",
                            placeHolder: "The module.json file has been modified, do you want to restart the LSPServer to active modifications?"
                        })) && (a.Utility.clearMultiModuleOption(), yield l.dispose(), yield l.activate(e.globalStoragePath, i, e.workspaceState, e, a.Utility.getInitializationOptions())), setTimeout((() => {
                            c = !1
                        }), 100)
                    })))));
                    const d = o.workspace.createFileSystemWatcher(new o.RelativePattern(null === (t = o.workspace.workspaceFolders) || void 0 === t ? void 0 : t[0], "**/module-lock.json"));
                    e.subscriptions.push(d.onDidCreate((t => n(this, void 0, void 0, (function* () {
                        yield l.dispose(), a.Utility.delay(s.delay100), yield l.activate(e.globalStoragePath, i, e.workspaceState, e, a.Utility.getInitializationOptions())
                    }))))), e.subscriptions.push(o.commands.registerCommand("cangjie.lsp.condition", (() => n(this, void 0, void 0, (function* () {
                        a.Utility.conditionBuild(e, l, i)
                    }))))), e.subscriptions.push(o.window.onDidChangeActiveTextEditor((t => {
                        var n, o;
                        a.Utility.serverRun || ".cj" !== (null === (o = null === (n = null == t ? void 0 : t.document) || void 0 === n ? void 0 : n.fileName) || void 0 === o ? void 0 : o.slice(s.fileExtension)) || l.activate(e.globalStoragePath, i, e.workspaceState, e, a.Utility.getInitializationOptions())
                    })))
                }))
            }
        }, 3503: (e, t) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.definitionMidware = t.documentLinkMidware = t.didOpenMidware = void 0, t.didOpenMidware = function (e, t) {
                return e.uri.toString().endsWith(".cj") || e.uri.toString().endsWith(".cj.macrocall") ? t(e) : new Promise((e => {
                    e()
                }))
            }, t.documentLinkMidware = function (e, t, i) {
                return ((e, t) => {
                    if (e.uri.toString().endsWith(".cj") || e.uri.toString().endsWith(".cj.macrocall")) return i(e, t)
                })(e, t)
            }, t.definitionMidware = function (e, t, i, n) {
                return ((e, t, i) => e.uri.toString().endsWith(".cj") ? n(e, t, i) : null)(e, t, i)
            }
        }, 2614: (e, t) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.gitCommitIdKey = t.cjpmCacheSuffix = t.packageRequieChild = t.builtinConditions = t.testCangjieFile = t.moduleGitKeyInModuleJson = t.modulePathKeyInModuleJson = t.moduleNameKeyInModuleJson = t.requireCategory = t.fileExtension = t.moduleJsonextname = t.delay100 = t.cpmBuildArgs = void 0, t.cpmBuildArgs = {
                help: !1,
                debug: !1,
                verbose: !1,
                coverage: !1,
                serial: !1,
                increment: !1,
                alias: "",
                cross: "",
                condition: ""
            }, t.delay100 = 100, t.moduleJsonextname = "/module.json", t.fileExtension = -3, t.requireCategory = ["requires", "package_requires", "foreign_requires"], t.moduleNameKeyInModuleJson = "name", t.modulePathKeyInModuleJson = "path", t.moduleGitKeyInModuleJson = "git", t.testCangjieFile = "testCangjie.cj", t.builtinConditions = new Set(["os", "backend", "debug", "cjc_version"]), t.packageRequieChild = ["path_option", "package_option"], t.cjpmCacheSuffix = ".cjpm/git", t.gitCommitIdKey = "commitId"
        }, 4962: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.Utility = void 0;
            const o = i(1017), r = i(7147), s = i(9496), a = i(2614), c = i(2037);

            class l {
                static getWorkspaceFolders() {
                    if (l.checkIsValid(s.workspace.workspaceFolders)) return s.workspace.workspaceFolders[0].uri.fsPath;
                    s.window.showWarningMessage("no open work folder!")
                }

                static getJsonContent(e) {
                    if (e !== a.moduleJsonextname) return;
                    const t = o.join(l.getWorkspaceFolders(), e);
                    if (!r.existsSync(t)) return;
                    const i = r.readFileSync(t, "utf8");
                    return "" === i ? "" : JSON.parse(i)
                }

                static getExportLDPath() {
                    const e = l.getEnvPaths();
                    let t = "";
                    return e.forEach((e => {
                        if (e.match("LD_LIBRARY_PATH")) {
                            let i = e.split("=");
                            t = i[i.length - 1]
                        }
                    })), {LD_LIBRARY_PATH: t}
                }

                static getWindowsPath() {
                    const e = l.getEnvPaths();
                    let t = "";
                    return e.forEach((e => {
                        if (e.match("PATH")) {
                            let i = e.split("=");
                            t = i[i.length - 1]
                        }
                    })), {PATH: t}
                }

                static getSdkOption() {
                    return "CJNative"
                }

                static getCangjieHome() {
                    return "CJNative" === this.getSdkOption() ? s.workspace.getConfiguration("CangjieSdk").get("Path") : s.workspace.getConfiguration("CangjieSdkPath").get("cjvmBackend")
                }

                static getEnvPaths() {
                    const e = l.getCangjieHome();
                    try {
                        if (!r.existsSync(e)) throw new Error("cangjie sdk path not exist, please configuration it first!");
                        let t = o.join(e, "envsetup.sh"), i = r.readFileSync(t, "utf8"), n = /\${script_dir}/g,
                            s = /\${CANGJIE_HOME}/g, a = /export(?<id>.)*/g;
                        if ("win32" === process.platform && (t = o.join(e, "envsetup.bat"), i = r.readFileSync(t, "utf8"), n = /%~dp0/g, s = /%CANGJIE_HOME%/g, a = /set(?<id>.)*/g), !l.checkIsValid(i)) throw new Error("envsetup.sh is empty");
                        i = i.replace(n, e), i = "linux" === process.platform ? i.replace(s, e) : i.replace(s, `${e}\\`);
                        return i.match(a)
                    } catch (e) {
                        return s.window.showErrorMessage(`[error]: ${e}`), []
                    }
                }

                static getInitializationOptions() {
                    if (!l.checkIsValid(s.workspace.workspaceFolders)) return "";
                    const e = s.workspace.workspaceFolders[0].uri.fsPath;
                    return this.getMultiModuleOption(e), this.getConditionCompileOption(), this.allInitializationOptions
                }

                static getMultiModuleOption(e) {
                    this.findAllModuleJson(e, ""), this.allInitializationOptions.multiModuleOption = this.multiModuleOption
                }

                static getConditionCompileOption() {
                    this.allInitializationOptions.conditionCompileOption = this.conditionCompileOption, this.allInitializationOptions.singleConditionCompileOption = this.conditionPacakgeCompileOption
                }

                static setConditionCompileOption(e, t) {
                    return this.regexName(e) && this.regexName(t) ? this.checkBuiltinCondition(e) ? "builtin" : Object.prototype.hasOwnProperty.call(this.conditionCompileOption, e) ? "repeat" : (this.conditionCompileOption[e] = t, "success") : "invalid"
                }

                static setPackageCompileOption(e, t, i) {
                    return Object.prototype.hasOwnProperty.call(this.conditionPacakgeCompileOption, i) || (this.conditionPacakgeCompileOption[i] = {}), this.regexName(e) && this.regexName(t) ? this.checkBuiltinCondition(e) ? "builtin" : Object.prototype.hasOwnProperty.call(this.conditionCompileOption, e) || Object.prototype.hasOwnProperty.call(this.conditionPacakgeCompileOption[i], e) ? "repeat" : (this.conditionPacakgeCompileOption[i][e] = t, "success") : "invalid"
                }

                static getConditionCompileContent(e) {
                    if (-1 === e.indexOf("--conditional-compilation-config=")) return "";
                    const t = e.indexOf("("), i = e.indexOf(")");
                    return -1 === t || -1 === i || t >= i ? "" : e.substring(t + 1, i)
                }

                static getAllKeys() {
                    return Object.keys(this.multiModuleOption)
                }

                static regexName(e) {
                    return /^[A-Za-z_]\w*$/.test(e)
                }

                static getPackageRequires(e, t) {
                    if (Object.prototype.hasOwnProperty.call(e[a.requireCategory[1]], a.packageRequieChild[0])) for (let i = 0; i < e[a.requireCategory[1]][a.packageRequieChild[0]].length; i++) {
                        let n = e[a.requireCategory[1]][a.packageRequieChild[0]][i], r = o.normalize(n);
                        o.isAbsolute(r) || (n = o.join(t, r)), n[n.length - 1] === o.sep && (n = n.substring(0, n.length - 1)), e[a.requireCategory[1]][a.packageRequieChild[0]][i] = s.Uri.file(n).toString()
                    }
                    if (Object.prototype.hasOwnProperty.call(e[a.requireCategory[1]], a.packageRequieChild[1])) for (let i in e[a.requireCategory[1]][a.packageRequieChild[1]]) if (Object.prototype.hasOwnProperty.call(e[a.requireCategory[1]][a.packageRequieChild[1]], i)) {
                        let n = e[a.requireCategory[1]][a.packageRequieChild[1]][i], r = o.normalize(n);
                        o.isAbsolute(r) || (n = o.join(t, r)), e[a.requireCategory[1]][a.packageRequieChild[1]][i] = s.Uri.file(n).toString()
                    }
                    return e[a.requireCategory[1]]
                }

                static findAllModuleJson(e, t) {
                    const i = s.Uri.file(e).toString();
                    if (this.existed.includes(i)) return;
                    this.existed.push(i);
                    const n = o.join(e, "module.json");
                    let c = {};
                    if (!r.existsSync(n)) return void (this.multiModuleOption[i] = c);
                    const l = r.readFileSync(n, "utf8");
                    let u = {};
                    try {
                        u = JSON.parse(l)
                    } catch (e) {
                        return s.window.showWarningMessage(`The content of module.json is invalid in ${i}. Enter the correct content.`), void (this.multiModuleOption[i] = c)
                    }
                    if (Object.prototype.hasOwnProperty.call(u, a.moduleNameKeyInModuleJson)) {
                        let e = u[a.moduleNameKeyInModuleJson];
                        this.regexName(e) || s.window.showWarningMessage(`Enter a valid 'name' (like [A-Za-z_]w*) in ${n}`), "" !== t && e !== t && s.window.showWarningMessage(`The require module name ${t}\n          ' is different to file module name ${e} in ${n}`), c[a.moduleNameKeyInModuleJson] = e
                    } else c[a.moduleNameKeyInModuleJson] = o.dirname(e);
                    if (Object.prototype.hasOwnProperty.call(u, a.requireCategory[1]) && (c[a.requireCategory[1]] = this.getPackageRequires(u, e)), Object.prototype.hasOwnProperty.call(u, a.requireCategory[0])) {
                        for (let t in u[a.requireCategory[0]]) {
                            if (Object.prototype.hasOwnProperty.call(u[a.requireCategory[0]], t) && Object.prototype.hasOwnProperty.call(u[a.requireCategory[0]][t], a.modulePathKeyInModuleJson)) {
                                this.regexName(t) || s.window.showWarningMessage(`Enter a valid 'requires' key ${t} (like [A-Za-z_]w*) in ${n}`);
                                let i = u[a.requireCategory[0]][t][a.modulePathKeyInModuleJson], r = o.normalize(i);
                                o.isAbsolute(r) || (i = o.dirname(o.join(e, r, a.testCangjieFile))), u[a.requireCategory[0]][t][a.modulePathKeyInModuleJson] = s.Uri.file(i).toString(), this.findAllModuleJson(o.join(i), t)
                            }
                            if (Object.prototype.hasOwnProperty.call(u[a.requireCategory[0]], t) && Object.prototype.hasOwnProperty.call(u[a.requireCategory[0]][t], a.moduleGitKeyInModuleJson)) {
                                this.regexName(t) || s.window.showWarningMessage(`Enter a valid 'requires' key ${t} (like [A-Za-z_]w*) in ${n}`);
                                let i = this.getPathByLockFile(e, t);
                                if (this.checkIsValid(i)) {
                                    let e = o.normalize(i);
                                    u[a.requireCategory[0]][t][a.modulePathKeyInModuleJson] = s.Uri.file(e).toString()
                                }
                                this.findAllModuleJson(o.join(i), t)
                            }
                        }
                        c[a.requireCategory[0]] = u[a.requireCategory[0]]
                    }
                    this.multiModuleOption[i] = c
                }

                static getPathByLockFile(e, t) {
                    let i = this.getCjpmConfigPath();
                    const n = o.join(e, "module-lock.json");
                    try {
                        r.accessSync(n, r.constants.R_OK)
                    } catch (e) {
                        s.window.showWarningMessage("The module-lock.json file does not exist or cannot be accessed.")
                    }
                    const c = r.readFileSync(n, "utf8");
                    let l = JSON.parse(c);
                    return a.requireCategory[0] in l && t in l[a.requireCategory[0]] && this.checkIsValid(l[a.requireCategory[0]][t][a.gitCommitIdKey]) ? o.join(i, t, l[a.requireCategory[0]][t][a.gitCommitIdKey]) : ""
                }

                static getCjpmConfigPath() {
                    return this.checkIsValid(process.env.CJPM_CONFIG) ? o.join(process.env.CJPM_CONFIG, a.cjpmCacheSuffix) : "win32" === c.platform() ? o.join(process.env.LOCALAPPDATA, a.cjpmCacheSuffix) : o.join(process.env.HOME, a.cjpmCacheSuffix)
                }

                static clearMultiModuleOption() {
                    this.existed = [], this.multiModuleOption = {}, this.allInitializationOptions = {
                        multiModuleOption: {},
                        conditionCompileOption: {},
                        singleConditionCompileOption: {}
                    }
                }

                static clearConditionOption() {
                    this.conditionCompileOption = {}, this.conditionPacakgeCompileOption = {}
                }

                static checkBuiltinCondition(e) {
                    return a.builtinConditions.has(e)
                }

                static printWrongConditionMessage(e) {
                    switch (e) {
                        case"invalid":
                            s.window.showWarningMessage('Condition string need a valid "name" (like [A-Za-z_]w*)');
                            break;
                        case"repeat":
                            s.window.showWarningMessage('User defined condition"s key can not repeat');
                            break;
                        case"builtin":
                            s.window.showWarningMessage('User defined condition"s key can not be the same with builtin condition')
                    }
                }

                static checkKeysRepeat(e) {
                    let t = new Set;
                    if (!("condition_option" in e) || !("package_configuration" in e)) return !0;
                    let i = [];
                    l.checkIsValid(e.condition_option) && (i = Object.keys(e.condition_option));
                    let n = [];
                    l.checkIsValid(e.package_configuration) && (n = Object.keys(e.package_configuration));
                    for (let e of i) t.add(e);
                    for (let i of n) if ("condition_option" in e.package_configuration[i]) {
                        const n = e.package_configuration[i].condition_option;
                        for (let e of Object.keys(n)) if (t.has(e)) return s.window.showWarningMessage(`The condition option ${e} is not allowed to appear in both 'package_configuration' field and 'condition_option' field at the same time`), !0
                    }
                    return !1
                }

                static checkPackageRequire() {
                    let e = l.getJsonContent(a.moduleJsonextname);
                    return !l.checkIsValid(e) || ("package_requires" in e ? "package_option" in e.package_requires && "path_option" in e.package_requires || (s.window.showErrorMessage("package_requires field of module.json must only have two fields: path_option and package_option.Please fix it and reload window again"), !1) : (s.window.showErrorMessage("There is no package_requires field of module.json"), !1))
                }

                static checkUserConditions(e, t) {
                    let i = "success";
                    for (let n of e) {
                        let o = e[n].split("=");
                        const r = 2;
                        if (o.length === r && (i = l.setPackageCompileOption(o[0], o[1], t), "success" !== i)) {
                            l.printWrongConditionMessage(i);
                            break
                        }
                    }
                    return i
                }

                static conditionBuild(e, t, i) {
                    return n(this, void 0, void 0, (function* () {
                        let n = l.getJsonContent("/module.json");
                        if (l.checkKeysRepeat(n)) return;
                        l.clearConditionOption();
                        let o = [];
                        l.checkIsValid(n.condition_option) && (o = Object.keys(n.condition_option));
                        let r = [];
                        if (l.checkIsValid(n.package_configuration) && (r = Object.keys(n.package_configuration)), 0 === o.length && 0 === r.length) return void s.window.showWarningMessage("There is no related configuration for condition_option in module.json, please configure this parameter first");
                        let c = [];
                        o.length > 0 && o.forEach((e => {
                            c.push({
                                label: e,
                                target: {global: n.condition_option[e]},
                                description: n.condition_option[e]
                            })
                        }));
                        let u = new Map;
                        null == r || r.forEach((e => {
                            if ("condition_option" in n.package_configuration[e]) {
                                let t = Object.keys(n.package_configuration[e].condition_option);
                                for (let i of t) u.has(i) ? u.get(i).push(e) : u.set(i, [e])
                            }
                        }));
                        for (let [e, t] of u) {
                            let i = {}, o = "";
                            t.forEach((t => {
                                i[t] = n.package_configuration[t].condition_option[e], o += `package ${t} `
                            })), c.push({label: e, target: i, description: o})
                        }
                        yield l.delay(a.delay100);
                        let d = yield s.window.showQuickPick(c, {
                            placeHolder: "choose one or more condition_option params",
                            canPickMany: !0
                        });
                        if (!l.checkIsValid(d)) return;
                        let p = [];
                        d.forEach((e => {
                            let t = l.getConditionCompileContent(e.description);
                            "" !== t && t.split(",").forEach((e => {
                                p.push(e.trim())
                            }))
                        }));
                        let h = "success";
                        if (p.forEach((e => {
                            let t = e.split("=");
                            2 !== t.length || (h = l.setConditionCompileOption(t[0], t[1]))
                        })), "success" === h) {
                            h = "success";
                            for (let e of d) if (!Object.prototype.hasOwnProperty.call(e.target, "global")) for (let t of e.target) {
                                let i = [], n = l.getConditionCompileContent(e.target[t]);
                                "" !== n && n.split(",").forEach((e => {
                                    i.push(e.trim())
                                })), h = l.checkUserConditions(i, t)
                            }
                            "success" === h ? (l.clearMultiModuleOption(), yield t.dispose(), yield t.activate(e.globalStoragePath, i, e.workspaceState, e, l.getInitializationOptions())) : l.printWrongConditionMessage(h)
                        } else l.printWrongConditionMessage(h)
                    }))
                }

                static delay(e) {
                    return n(this, void 0, void 0, (function* () {
                        yield new Promise((t => {
                            setTimeout(t, e)
                        }))
                    }))
                }

                static checkIsValid(e) {
                    return null != e && (("string" != typeof e || "" !== e) && (("number" != typeof e || 0 !== e) && ("boolean" != typeof e || e)))
                }
            }

            t.Utility = l, l.serverRun = !1, l.allInitializationOptions = {
                multiModuleOption: {},
                conditionCompileOption: {},
                singleConditionCompileOption: {}
            }, l.multiModuleOption = {}, l.conditionCompileOption = {}, l.conditionPacakgeCompileOption = {}, l.existed = []
        }, 9420: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.CjProjectBuildProvider = void 0;
            const o = i(9496), r = i(2081), s = i(3897), a = i(9310), c = i(6529), l = i(7026), u = i(9311),
                d = i(1017), p = i(7147);
            t.CjProjectBuildProvider = class {
                static executeCmd(e, t = !1) {
                    return n(this, void 0, void 0, (function* () {
                        let i = e;
                        if ("linux" !== process.platform && "win32" !== process.platform) return o.window.showInformationMessage("Only support linux and windows now"), !1;
                        if (!a.Utility.checkIsValid(i)) return !1;
                        const n = a.Utility.getWorkspaceFolders();
                        if (!a.Utility.checkIsValid(n)) return o.window.showErrorMessage("Project is not exist"), !1;
                        if (!a.Utility.isCangjieProject()) return o.window.showErrorMessage("Can not use cjpm to build project which without a module.json file"), c.OutputHelper.appendLine(`[stop]: ${i} \n`, !0), !1;
                        if (t) return c.OutputHelper.execCommand(a.Utility.getExportPath().concat(i), n);
                        const s = a.Utility.getModuleJsonContent(u.moduleJsonextname);
                        if ("command_option" in s && !s.command_option.includes("--diagnostic-format=noColor")) {
                            s.command_option += " --diagnostic-format=noColor", s.command_option = s.command_option.trim();
                            const e = d.join(a.Utility.getWorkspaceFolders(), u.moduleJsonextname);
                            p.writeFileSync(e, JSON.stringify(s, null, "\t"))
                        }
                        try {
                            const e = d.join(a.Utility.getWorkspaceFolders(), "/module-resolve.json");
                            return p.existsSync(e) && r.execSync(a.Utility.getExportPath().concat("cjpm update"), {
                                cwd: n,
                                encoding: "utf8"
                            }), void l.TerminalHelper.execCommand(i)
                        } catch (e) {
                            return o.window.showErrorMessage(e.message), !1
                        }
                    }))
                }

                static multi() {
                    s.multiParamsBuild().then((e => {
                        "".concat(e), a.Utility.checkIsValid(e) && this.executeCmd(e)
                    }))
                }

                static multiTest() {
                    s.multiParamsTest().then((e => {
                        a.Utility.checkIsValid(e) && this.executeCmd(e)
                    }))
                }
            }
        }, 178: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.CustomBuildTaskProvider = void 0;
            const o = i(9496), r = i(9310), s = i(9311);

            class a {
                constructor() {
                }

                provideTasks() {
                    return n(this, void 0, void 0, (function* () {
                        return this.getTasks()
                    }))
                }

                resolveTask(e) {
                }

                getTasks() {
                    return n(this, void 0, void 0, (function* () {
                        return this.tasks = [this.getTask("")], this.tasks
                    }))
                }

                getTask(e, t) {
                    let i = t;
                    void 0 === i && (i = {type: a.customBuildScriptType, group: "build", cmd: "cjpm build"});
                    const n = this.getBuildArgs();
                    let r = new o.Task(i, o.TaskScope.Workspace, "cjpm build", "cangjie", new o.ShellExecution("cjpm", ["build", ...n]));
                    return r.group = o.TaskGroup.Build, r
                }

                getBuildArgs() {
                    return r.Utility.getCjpmBuildArgsContent(s.cjpmBuildArgExtname).split(/\s+/g)
                }
            }

            t.CustomBuildTaskProvider = a, a.customBuildScriptType = "buildcangjie"
        }, 3240: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.CjpmBuildCollection = t.BuildArgsCollection = void 0;
            const o = i(9496), r = i(9420), s = i(9310), a = i(7026), c = i(1017), l = i(3464), u = i(9311),
                d = i(7865);
            var p;
            !function (e) {
                e.BUILD_HELPER = "cjpm build -h", e.BUILD_WITH_ALIAS = "cjpm build -o", e.PARALLELLED_COMPILE = "cjpm build", e.SERIALLED_COMPILE = "cjpm build -s", e.RUN_FOR_VERBOSE = "cjpm build -V", e.RUN_FOR_COVERAGE = "cjpm build --coverage", e.RUN_FOR_DEBUG = "cjpm build -g", e.MUTIL_PARAM_COMPILE = "multi", e.CJPM_TEST = "cjpm test", e.CJPM_UPDATE = "cjpm update", e.CJPM_CLEAN = "cjpm clean", e.CJPM_CHECK = "cjpm check", e.BUILD_WITH_CONDITION = "cjpm build --condition=", e.INCREMENT = "cjpm build -i", e.INCREMENT_WITH_DEBUG = "cjpm build -i -g"
            }(p = t.BuildArgsCollection || (t.BuildArgsCollection = {}));

            class h {
                constructor(e) {
                    this.context = e, this.registerRun(), this.registerConfig(), this.registerBuild(), this.registerBuildCondition(), this.context.subscriptions.push(o.commands.registerCommand("cangjie.build.helper", (() => {
                        r.CjProjectBuildProvider.executeCmd(p.BUILD_HELPER)
                    }))), this.context.subscriptions.push(o.commands.registerCommand("cangjie.build.debug", (() => {
                        r.CjProjectBuildProvider.executeCmd(p.RUN_FOR_DEBUG)
                    }))), this.context.subscriptions.push(o.commands.registerCommand("cangjie.build.update", (() => {
                        r.CjProjectBuildProvider.executeCmd(p.CJPM_UPDATE)
                    }))), this.context.subscriptions.push(o.commands.registerCommand("cangjie.build.clean", (() => {
                        r.CjProjectBuildProvider.executeCmd(p.CJPM_CLEAN)
                    }))), this.context.subscriptions.push(o.commands.registerCommand("cangjie.build.check", (() => {
                        r.CjProjectBuildProvider.executeCmd(p.CJPM_CHECK)
                    }))), this.context.subscriptions.push(o.commands.registerCommand("cangjie.build.incrementWithDebug", (() => n(this, void 0, void 0, (function* () {
                        let e = !1;
                        return yield r.CjProjectBuildProvider.executeCmd(p.INCREMENT_WITH_DEBUG, !0).then((t => {
                            e = t
                        })), e
                    }))))), this.context.subscriptions.push(o.commands.registerCommand("cangjie.build.multiArgsCheck", (() => {
                        r.CjProjectBuildProvider.multiTest()
                    }))), this.context.subscriptions.push(o.commands.registerCommand("Cangjie.BuildArg.Json.Edit", (() => {
                        const e = o.Uri.file(c.join(s.Utility.getWorkspaceFolders(), ".vscode", "cjpm_build_args.json"));
                        o.commands.executeCommand("vscode.open", e)
                    })))
                }

                registerBuild() {
                    this.context.subscriptions.push(o.commands.registerCommand("cangjie.build.parallel", (() => {
                        r.CjProjectBuildProvider.executeCmd(p.PARALLELLED_COMPILE)
                    }))), this.context.subscriptions.push(o.commands.registerCommand("cangjie.build.serial", (() => {
                        r.CjProjectBuildProvider.executeCmd(p.SERIALLED_COMPILE)
                    }))), this.context.subscriptions.push(o.commands.registerCommand("cangjie.build.verbose", (() => {
                        r.CjProjectBuildProvider.executeCmd(p.RUN_FOR_VERBOSE)
                    }))), this.context.subscriptions.push(o.commands.registerCommand("cangjie.build.coverage", (() => {
                        r.CjProjectBuildProvider.executeCmd(p.RUN_FOR_COVERAGE)
                    }))), this.context.subscriptions.push(o.commands.registerCommand("cangjie.build.alias", (() => n(this, void 0, void 0, (function* () {
                        const e = yield o.window.showInputBox({prompt: "Input a custom output name."});
                        void 0 === e || "" === e ? r.CjProjectBuildProvider.executeCmd(p.PARALLELLED_COMPILE) : r.CjProjectBuildProvider.executeCmd(`${p.BUILD_WITH_ALIAS} ${e}`)
                    }))))), this.context.subscriptions.push(o.commands.registerCommand("cangjie.build.multiParameter", (() => {
                        r.CjProjectBuildProvider.multi()
                    }))), this.context.subscriptions.push(o.commands.registerCommand("cangjie.build.increment", (() => {
                        r.CjProjectBuildProvider.executeCmd(p.INCREMENT)
                    })))
                }

                registerBuildCondition() {
                    this.context.subscriptions.push(o.commands.registerCommand("cangjie.build.condition", (() => n(this, void 0, void 0, (function* () {
                        let e = s.Utility.getModuleJsonContent(u.moduleJsonextname);
                        if (!s.Utility.checkIsValid(e) || !("condition_option" in e) || !("package_configuration" in e)) return void o.window.showWarningMessage("The condition_option or package_configuration content of the module.json file is missing, please fill it out");
                        let t = Object.keys(e.condition_option), i = [];
                        if (Object.keys(e.package_configuration).forEach((t => {
                            if ("condition_option" in e.package_configuration[t]) {
                                const n = e.package_configuration[t].condition_option;
                                for (let e of Object.keys(n)) i.push({
                                    label: e,
                                    target: e,
                                    description: `package ${t} ${n[e]}`
                                })
                            }
                        })), 0 === i.length && 0 === t.length) return void o.window.showWarningMessage("There is no related configuration for condition_option in module.json, please configure this parameter first");
                        let n = i.reduce(((e, t) => (e.push(t.label), e)), []);
                        for (let r of t) {
                            if (n.includes(r)) return void o.window.showWarningMessage("There is conflict between condition_option of package_configuration and global condition_option,\n          please fix it first");
                            i.push({label: r, target: r, description: e.condition_option[r]})
                        }
                        yield s.Utility.delay(u.delay100);
                        let a = yield o.window.showQuickPick(i, {
                            placeHolder: "choose one or more condition_option params",
                            canPickMany: !0
                        });
                        if (!s.Utility.checkIsValid(a)) return;
                        let c = "";
                        a.forEach((e => {
                            c += `${e.label}, `
                        }));
                        const l = c.replace(/(?<id>,\x20)$/, " ").trim();
                        r.CjProjectBuildProvider.executeCmd(`${p.BUILD_WITH_CONDITION}"${[...new Set(l.split(", "))].join(", ")}"`)
                    })))))
                }

                registerConfig() {
                    this.context.subscriptions.push(o.commands.registerCommand("cangjieBuild.editBuildConfiguration", (() => {
                        s.Utility.isCangjieProject() ? (s.Utility.checkIsValid(h.configSetting) || (h.configSetting = new l.SettingsProvider(this.context)), h.configSetting.createOrShow("html/setting.html", "configSetByUI.js", "configSet.css")) : o.window.showWarningMessage("The project can not find module.json file. you can use cjpm init to create")
                    }))), this.context.subscriptions.push(o.commands.registerCommand("cangjie.require.uiSetting", (() => {
                        s.Utility.isCangjieProject() ? (s.Utility.checkIsValid(h.requireConfigure) || (h.requireConfigure = new d.RequiresActionController(this.context)), h.requireConfigure.createOrShow("html/requireTree.html", "requireTreeByUI.js", "requireSet.css")) : o.window.showWarningMessage("The project can not find module.json file. you can use cjpm init to create")
                    }))), this.context.subscriptions.push(o.commands.registerCommand("Cangjie.Module.Json.Edit", (() => {
                        const e = o.Uri.file(c.join(s.Utility.getWorkspaceFolders(), "module.json"));
                        o.commands.executeCommand("vscode.open", e)
                    })))
                }

                registerRun() {
                    this.context.subscriptions.push(o.commands.registerCommand("cangjie.run", (() => n(this, void 0, void 0, (function* () {
                        if (!(yield o.workspace.saveAll(!1))) return void o.window.showErrorMessage("auto save failed, please check and save dirty files manually!");
                        if ("linux" !== process.platform && "win32" !== process.platform) return void o.window.showInformationMessage("Only support linux and windows now");
                        let e = "cjpm build";
                        e += ` ${s.Utility.getCjpmBuildArgsContent(u.cjpmBuildArgExtname)} `;
                        let t = s.Utility.getModuleJsonContent(u.moduleJsonextname),
                            i = "executable" === t.output_type || "cbc" === t.output_type;
                        a.TerminalHelper.execCommand(e, i)
                    })))))
                }
            }

            t.CjpmBuildCollection = h
        }, 2521: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.NodeData = void 0;
            const n = i(9496);

            class o extends n.TreeItem {
                constructor(e, t, i, n, o) {
                    super(e.label, e.collapsibleState), this.viewItemName = t, this.path = i, this.parent = n, this.contextValue = this.viewItemName, this.tooltip = null !== this.path && void 0 !== this.path ? this.path : "", this.description = null !== this.path && void 0 !== this.path ? this.path : "", this.viewItemName = t, this.parent = n, this.command = o
                }
            }

            t.NodeData = o
        }, 5970: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.LibTreeDataProvider = void 0;
            const n = i(9496), o = i(2521), r = i(9311), s = i(9310), a = i(1017), c = i(7290);
            t.LibTreeDataProvider = class {
                constructor(e) {
                    this.context = e, this.nodeDataItem = "NodeDataItem", this.nodeDataTitle = "NodeDataTitle", this.nodeDataTitleNoChild = "NodeDataTitleNoChild", this._onDidChangeTreeData = new n.EventEmitter, this.onDidChangeTreeData = this._onDidChangeTreeData.event
                }

                getLibTree() {
                    const e = new c.ModuleJsonImpl, t = s.Utility.getModuleJsonContent(r.moduleJsonextname);
                    for (let i of r.requireCategory) e[i] = t[i];
                    return e
                }

                getTreeItem(e) {
                    return e
                }

                recurrentCreateNode(e, t, i) {
                    const c = i.label;
                    r.packageRequieChild.includes(c) && void 0 === e.package_requires && (e.package_requires = {});
                    let l = r.packageRequieChild.includes(c) ? e.package_requires[c] : e[c];
                    if (s.Utility.checkIsValid(l) || (l = {}), "package_requires" === c) return t.push(new o.NodeData({
                        label: "path_option",
                        collapsibleState: n.TreeItemCollapsibleState.Collapsed
                    }, this.nodeDataTitle, "", i)), void t.push(new o.NodeData({
                        label: "package_option",
                        collapsibleState: n.TreeItemCollapsibleState.Collapsed
                    }, this.nodeDataTitle, "", i));
                    (l instanceof Array ? l : Object.keys(l)).forEach((e => {
                        var r;
                        let c = (null === (r = l[e]) || void 0 === r ? void 0 : r.path) || l[e] || e;
                        /^[a-zA-Z]:/.test(c) || (c = a.resolve(s.Utility.getWorkspaceFolders(), c)), t.push(new o.NodeData({
                            label: e,
                            collapsibleState: n.TreeItemCollapsibleState.None
                        }, this.nodeDataItem, c, i))
                    }))
                }

                getChildren(e) {
                    if (!s.Utility.isCangjieProject()) return Promise.resolve(void 0);
                    const t = this.getLibTree(), i = [];
                    if (s.Utility.checkIsValid(e) && "foreign_requires" === e.label && "cjvm" === s.Utility.getSdkOption()) return n.window.showWarningMessage("the foreign_requires is only setted up in CJNative path"), Promise.resolve(void 0);
                    if (s.Utility.checkIsValid(e) && r.requireCategoryNew.includes(e.label)) return this.recurrentCreateNode(t, i, e), Promise.resolve(i);
                    let a = Object.keys(t);
                    for (let e of a) {
                        let t = "package_requires" === e ? this.nodeDataTitleNoChild : this.nodeDataTitle;
                        i.push(new o.NodeData({
                            label: e,
                            collapsibleState: n.TreeItemCollapsibleState.Collapsed
                        }, t, ""))
                    }
                    return Promise.resolve(i)
                }

                refresh(e) {
                    s.Utility.checkIsValid(e) && (e.collapsibleState = n.TreeItemCollapsibleState.Expanded), this._onDidChangeTreeData.fire(e)
                }
            }
        }, 2590: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.MultiStepChoose = void 0;
            const o = i(9496), r = i(9311), s = i(9310);

            class a {
                constructor() {
                    this.steps = []
                }

                static run(e) {
                    return n(this, void 0, void 0, (function* () {
                        return (new a).stepThrough(e)
                    }))
                }

                showStepPick({title: e, step: t, totalSteps: i, items: a, placeholder: c, button: l, canPickMany: u}) {
                    return n(this, void 0, void 0, (function* () {
                        const d = [];
                        try {
                            return yield new Promise(((p, h) => {
                                const g = o.window.createQuickPick();
                                g.title = null != t && 0 !== t ? `${e} (${t}/${i})` : e, g.items = a, g.placeholder = c, g.buttons = [...s.Utility.checkIsValid(l) ? [o.QuickInputButtons.Back] : []], u && (g.canSelectMany = !0, g.ignoreFocusOut), d.push(g.onDidTriggerButton((e => {
                                    e === o.QuickInputButtons.Back ? h(r.CustomAction.back) : p(e)
                                })), g.onDidHide((() => {
                                    h(r.CustomAction.cancel)
                                })), g.onDidChangeSelection((e => {
                                    s.Utility.checkIsValid(e[0]) && !g.canSelectMany && p(e[0])
                                })), g.onDidAccept((() => n(this, void 0, void 0, (function* () {
                                    if (g.canSelectMany) {
                                        const e = [];
                                        g.selectedItems.forEach((t => {
                                            e.push(t.target)
                                        })), p(e)
                                    }
                                }))))), s.Utility.checkIsValid(this.current) && this.current.dispose(), this.current = g, this.current.show()
                            }))
                        } finally {
                            d.forEach((e => e.dispose()))
                        }
                    }))
                }

                showEnterInput({prompt: e, value: t, title: i, button: a, step: c, totalSteps: l}) {
                    return n(this, void 0, void 0, (function* () {
                        const u = [];
                        try {
                            return yield new Promise(((d, p) => {
                                const h = o.window.createInputBox();
                                h.value = t || void 0, h.prompt = e, h.title = null != c && 0 !== c ? `${i} (${c}/${l})` : i, h.buttons = [...s.Utility.checkIsValid(a) ? [o.QuickInputButtons.Back] : []], u.push(h.onDidTriggerButton((e => {
                                    e === o.QuickInputButtons.Back ? p(r.CustomAction.back) : d(e)
                                })), h.onDidAccept((() => n(this, void 0, void 0, (function* () {
                                    const e = h.value;
                                    h.enabled = !1, h.busy = !0, d(e), h.enabled = !0, h.busy = !1
                                })))), h.onDidHide((() => {
                                    p(r.CustomAction.cancel)
                                }))), s.Utility.checkIsValid(this.current) && this.current.dispose(), this.current = h, this.current.show()
                            }))
                        } finally {
                            u.forEach((e => e.dispose()))
                        }
                    }))
                }

                stepThrough(e) {
                    return n(this, void 0, void 0, (function* () {
                        let t = e;
                        for (; t;) {
                            this.steps.push(t), s.Utility.checkIsValid(this.current) && (this.current.enabled = !1, this.current.busy = !0);
                            try {
                                t = yield t(this)
                            } catch (e) {
                                if (e === r.CustomAction.back) this.steps.pop(), t = this.steps.pop(); else if (e === r.CustomAction.resume) t = this.steps.pop(); else {
                                    if (e !== r.CustomAction.cancel) throw e;
                                    t = void 0
                                }
                            }
                        }
                        s.Utility.checkIsValid(this.current) && this.current.dispose()
                    }))
                }
            }

            t.MultiStepChoose = a
        }, 3897: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.multiParamsTest = t.multiParamsBuild = void 0;
            const o = i(9496), r = i(9311), s = i(9310), a = i(2590);
            let c = [], l = !1;

            function u(e, t = "build") {
                return n(this, void 0, void 0, (function* () {
                    yield new Promise((e => {
                        setTimeout(e, r.delay10)
                    })), c = [];
                    const i = s.Utility.getModuleJsonContent(r.moduleJsonextname),
                        n = {label: `cjpm ${t} --condition=<name>`, target: "--condition"};
                    return s.Utility.checkIsValid(i) && function (e) {
                        let t = [];
                        s.Utility.checkIsValid(e.package_configuration) && (t = Object.keys(e.package_configuration)), t.forEach((t => {
                            if ("condition_option" in e.package_configuration[t]) {
                                const i = e.package_configuration[t].condition_option;
                                for (let e of Object.keys(i)) c.push({
                                    label: e,
                                    target: e,
                                    description: `package ${t} ${i[e]}`
                                })
                            }
                        })), c.length > 0 && (l = !0);
                        let i = [];
                        s.Utility.checkIsValid(e.condition_option) && (i = Object.keys(e.condition_option));
                        let n = c.reduce(((e, t) => (e.push(t.label), e)), []);
                        for (let t of i) {
                            if (n.includes(t)) return o.window.showWarningMessage("There is conflict between condition_option of package_configuration and global condition_option,\n      please fix it first"), l = !1, c = [], l;
                            c.push({label: t, target: t, description: e.condition_option[t]})
                        }
                        return i.length > 0 && (l = !0), l
                    }(i) && e.push(n), e.map((e => ({label: e.label, target: e.target, picked: !0})))
                }))
            }

            function d(e, t, i) {
                return n(this, void 0, void 0, (function* () {
                    const n = t.pickedParams.indexOf("--condition");
                    l = !1;
                    let r = c.slice(0);
                    c = [];
                    const a = yield e.showStepPick({
                        title: i,
                        placeholder: "Choose one or more condition options for arg --condition.",
                        items: r,
                        button: [o.QuickInputButtons.Back],
                        canPickMany: !0
                    });
                    if (s.Utility.checkIsValid(a)) {
                        let e = "";
                        a.forEach((t => {
                            e += `${t}, `
                        })), e.replace(/(?<id>,\x20)$/, " "), t.pickedParams[n] = `--condition='${[...new Set(a)]}'`, t.extraPickedParams = t.pickedParams.join(" ").trim()
                    } else o.window.showErrorMessage("Condition compilation using cjpm need select the compilation conditions")
                }))
            }

            function p(e) {
                return n(this, void 0, void 0, (function* () {
                    if (e.extraPickedParams.includes("--condition") && !e.extraPickedParams.includes("--condition=")) {
                        let t = e.extraPickedParams.indexOf("--condition");
                        e.extraPickedParams = e.extraPickedParams.substring(0, t - 1)
                    }
                }))
            }

            t.multiParamsBuild = function () {
                return n(this, void 0, void 0, (function* () {
                    const e = [{label: "cjpm build -s | --serial", target: "-s"}, {
                        label: "cjpm build -V | --verbose",
                        target: "-V"
                    }, {label: "cjpm build -g", target: "-g"}, {
                        label: "cjpm build -i | --incremental",
                        target: "-i"
                    }, {
                        label: "cjpm build --coverage",
                        target: "--coverage"
                    }, {label: "cjpm build -o <name> | --output=<name>", target: "-o"}], t = "mutiple params build";

                    function i(i, a) {
                        return n(this, void 0, void 0, (function* () {
                            const c = yield u(e), l = yield i.showStepPick({
                                title: t,
                                step: a.curStep,
                                totalSteps: r.buildMagicNum.mutilPickTotalSteps,
                                placeholder: "Pick one or more compilation parameters",
                                items: c,
                                canPickMany: !0
                            });
                            return a.pickedParams = l, l.includes("-o") ? e => function (e, i) {
                                return n(this, void 0, void 0, (function* () {
                                    let n;
                                    for (let [e, t] of i.pickedParams.entries()) if (s.Utility.checkIsValid(t.match(/-o/))) {
                                        n = e;
                                        break
                                    }
                                    const a = yield e.showEnterInput({
                                        prompt: "Input a custom output name for arg -o.",
                                        value: "",
                                        title: t,
                                        button: [o.QuickInputButtons.Back]
                                    });
                                    return s.Utility.checkIsValid(a) ? (i.pickedParams[n] = `-o ${a}`, i.extraPickedParams = i.pickedParams.join(" ").trim(), i.pickedParams.includes("--condition") ? (yield s.Utility.delay(r.delay100), e => d(e, i, t)) : () => Promise.resolve()) : (o.window.showErrorMessage("cjpm build arg -o need an alias"), () => Promise.resolve())
                                }))
                            }(e, a) : a.pickedParams.includes("--condition") ? e => d(e, a, t) : (a.extraPickedParams = l.join(" "), () => Promise.resolve())
                        }))
                    }

                    const c = {};
                    return yield a.MultiStepChoose.run((e => i(e, c))), s.Utility.checkIsValid(c) && void 0 !== c.extraPickedParams ? (p(c), `cjpm build ${c.extraPickedParams}`) : ""
                }))
            }, t.multiParamsTest = function () {
                return n(this, void 0, void 0, (function* () {
                    const e = [{label: "cjpm test -s | --serial", target: "-s"}, {
                        label: "cjpm test -V | --verbose",
                        target: "-V"
                    }, {label: "cjpm test --coverage", target: "--coverage"}, {
                        label: "cjpm test --bench",
                        target: "--bench"
                    }, {label: "cjpm test --filter=<value>", target: "--filter"}], t = "multiple params Test";

                    function i(i, a) {
                        return n(this, void 0, void 0, (function* () {
                            const c = yield i.showEnterInput({
                                prompt: "Specify the test path, or press Enter directly for module-level unit tests",
                                value: "",
                                title: t,
                                step: r.buildMagicNum.firstPick,
                                totalSteps: r.buildMagicNum.mutilPickTotalSteps,
                                button: void 0
                            });
                            return s.Utility.checkIsValid(c) ? a.specifiePath = `${c}` : a.specifiePath = "", a.curStep = r.buildMagicNum.secondPick, i => function (i, a) {
                                return n(this, void 0, void 0, (function* () {
                                    const c = yield u(e, "test"), l = yield i.showStepPick({
                                        title: t,
                                        step: a.curStep,
                                        totalSteps: r.buildMagicNum.mutilPickTotalSteps,
                                        placeholder: "Pick one or more cjpm test parameters",
                                        items: c,
                                        canPickMany: !0,
                                        button: [o.QuickInputButtons.Back]
                                    });
                                    return a.pickedParams = l, a.pickedParams.includes("--filter") ? e => function (e, i) {
                                        return n(this, void 0, void 0, (function* () {
                                            let n = i.pickedParams.indexOf("--filter");
                                            const r = yield e.showEnterInput({
                                                prompt: "Specify the reg",
                                                value: "",
                                                title: t,
                                                button: [o.QuickInputButtons.Back]
                                            });
                                            return s.Utility.checkIsValid(r) ? (i.pickedParams[n] = `--filter=${r}`, i.extraPickedParams = i.pickedParams.join(" ").trim(), i.pickedParams.includes("--condition") ? e => d(e, i, t) : () => Promise.resolve()) : (o.window.showErrorMessage("cjpm test arg --filter= need an regular expression"), () => Promise.resolve())
                                        }))
                                    }(e, a) : a.pickedParams.includes("--condition") ? e => d(e, a, t) : (a.extraPickedParams = a.pickedParams.join(" "), () => Promise.resolve())
                                }))
                            }(i, a)
                        }))
                    }

                    const c = {};
                    return yield a.MultiStepChoose.run((e => i(e, c))), s.Utility.checkIsValid(c) && void 0 !== c.extraPickedParams ? (p(c), `cjpm test ${c.specifiePath} ${c.extraPickedParams}`) : ""
                }))
            }
        }, 7865: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.RequiresActionController = void 0;
            const o = i(9496), r = i(7147), s = i(1017), a = i(9310), c = i(7277), l = i(2853), u = i(9311),
                d = i(7290);

            class p extends l.ViewProvider {
                constructor(e) {
                    super(e), this.requireContent = new d.ModuleJsonImpl, this.canSelectFiles = !0, this.canSelectFolders = !1, this.packageRequiresItem = ["path_option", "package_option"], this.viewType = "RequiresTreeProvider", this.title = "Configuration Required Tree", o.commands.registerCommand("cangjie.require.addRequireLibs", (e => {
                        a.Utility.isCangjieProject() ? this.addLibraries(e) : a.Utility.noModuleJson()
                    })), o.commands.registerCommand("cangjie.require.removeRequireLibs", (e => {
                        a.Utility.isCangjieProject() ? this.removeLibrary(e) : a.Utility.noModuleJson()
                    }))
                }

                addLibraries(e, t) {
                    if (!a.Utility.checkIsValid(e) && !a.Utility.checkIsValid(t)) return;
                    const i = a.Utility.checkIsValid(e) ? e.label : t.requireType;
                    u.requireCategoryNew.includes(i) && (this.canSelectFiles = !0, this.canSelectFolders = !1, "requires" !== i && "path_option" !== i || (this.canSelectFiles = !1, this.canSelectFolders = !0), this.showPathDialog(i, e))
                }

                onMessageReceived(e) {
                    switch (super.onMessageReceived(e), e.command) {
                        case"initialRequireUI":
                            this.updateRequireUI();
                            break;
                        case"addRequire":
                            if ("cjvm" === a.Utility.getSdkOption() && "foreign_requires" === e.requireType) {
                                o.window.showWarningMessage("the foreign_requires is only setted up in CJNative path");
                                break
                            }
                            this.addLibraries(void 0, e);
                            break;
                        case"delRequire":
                            this.removeLibrary(void 0, e)
                    }
                }

                showPathDialog(e, t) {
                    var i;
                    "foreign_requires" !== e || "cjvm" !== a.Utility.getSdkOption() ? o.window.showOpenDialog({
                        defaultUri: null === (i = a.Utility.getDefaultWorkspaceFolder()) || void 0 === i ? void 0 : i.uri,
                        canSelectFiles: this.canSelectFiles,
                        canSelectFolders: this.canSelectFolders,
                        openLabel: "Choose the required lib or lib path"
                    }).then((i => n(this, void 0, void 0, (function* () {
                        if (!a.Utility.checkIsValid(i)) return;
                        const n = "linux" === process.platform ? i[0].path : i[0].path.substring(u.secondPosition);
                        let l = s.extname(n), d = a.Utility.getModuleJsonContent(u.moduleJsonextname);
                        if ("package_option" === e) {
                            if (!(yield this.choosePackageOption(n, l, d))) return
                        } else if ("path_option" === e) {
                            if (!this.choosePathOption(d, n)) return
                        } else if (!this.chooseOther(e, n, d, l)) return;
                        try {
                            const e = s.join(a.Utility.getWorkspaceFolders(), u.moduleJsonextname);
                            r.writeFileSync(e, JSON.stringify(d, null, "\t"))
                        } catch (e) {
                            o.window.showErrorMessage(e)
                        }
                        c.LibTreeView._instance.updateTreeView("add", t), this.updateRequireUI(e)
                    })))) : o.window.showWarningMessage("the foreign_requires is only setted up in CJNative path")
                }

                removeLibrary(e, t) {
                    if (!a.Utility.checkIsValid(e) && !a.Utility.checkIsValid(t)) return;
                    const i = (null == e ? void 0 : e.parent.label) || (null == t ? void 0 : t.requireType),
                        n = (null == e ? void 0 : e.label) || (null == t ? void 0 : t.requireName),
                        l = a.Utility.getModuleJsonContent(u.moduleJsonextname);
                    if ("package_option" === i) delete l.package_requires.package_option[n]; else if ("path_option" === i) {
                        let e = l.package_requires.path_option.indexOf(n);
                        e > -1 && l.package_requires.path_option.splice(e, 1)
                    } else delete l[i][n];
                    try {
                        const e = s.join(a.Utility.getWorkspaceFolders(), u.moduleJsonextname);
                        r.writeFileSync(e, JSON.stringify(l, null, "\t"))
                    } catch (e) {
                        o.window.showErrorMessage(e)
                    }
                    c.LibTreeView._instance.updateTreeView("delete", e), this.updateRequireUI(i)
                }

                choosePackageOption(e, t, i) {
                    return n(this, void 0, void 0, (function* () {
                        let n, r = s.basename(e, t), c = ".so", l = ".a";
                        if ("win32" === process.platform && (c = ".dll"), "cjvm" === a.Utility.getSdkOption() && (l = ".bc"), t !== l && t !== c) return o.window.showWarningMessage(`Please choose a correct package_require name, only ${l} or ${c} file`), !1;
                        n = `${r.replace("lib", "")}`, a.Utility.checkIsValid(i.package_requires) || (i.package_requires = {}), a.Utility.checkIsValid(i.package_requires.package_option) || (i.package_requires.package_option = {});
                        const d = i.package_requires.package_option[n];
                        if (a.Utility.checkIsValid(d)) return o.window.showWarningMessage("The choosed require name already exists"), !1;
                        const p = yield o.window.showOpenDialog({
                            defaultUri: o.Uri.file(s.resolve(e, "..")),
                            canSelectFiles: !0,
                            canSelectFolders: !1,
                            openLabel: "Choose cjo file"
                        });
                        return ".cjo" !== s.extname(p[0].path) ? (o.window.showWarningMessage("Please choose a correct package require"), !1) : (i.package_requires.package_option[n] = "linux" === process.platform ? p[0].path : p[0].path.substring(u.secondPosition), !0)
                    }))
                }

                chooseOther(e, t, i, n) {
                    const c = s.join(t, u.moduleJsonextname);
                    switch (e) {
                        case"requires":
                            if (!r.existsSync(c)) return o.window.showWarningMessage("Please choose a correct require"), !1;
                        {
                            let e = JSON.parse(r.readFileSync(c, "utf8"));
                            const n = {organization: e.organization, version: e.version, path: t};
                            if (a.Utility.checkIsValid(i.requires) || (i.requires = new Map), !this.repeatedCheckAndAdd(i.requires, e.name, n)) return !1
                        }
                            break;
                        case"foreign_requires": {
                            let e = ".so";
                            if ("win32" === process.platform && (e = ".dll"), n !== e) return o.window.showWarningMessage("Please choose a correct foreign require"), !1;
                            let r = s.basename(t, n).replace("lib", "");
                            const c = {path: s.dirname(t), exports: []};
                            if (a.Utility.checkIsValid(i.foreign_requires) || (i.foreign_requires = new Map), !this.repeatedCheckAndAdd(i.foreign_requires, r, c)) return !1;
                            break
                        }
                    }
                    return !0
                }

                choosePathOption(e, t) {
                    a.Utility.checkIsValid(e.package_requires) || (e.package_requires = {}), a.Utility.checkIsValid(e.package_requires.path_option) || (e.package_requires.path_option = []);
                    for (let i of e.package_requires.path_option) if (s.resolve(t) === s.resolve(i)) return o.window.showWarningMessage("There is already a path with the same name"), !1;
                    return e.package_requires.path_option.push(t), !0
                }

                updateRequireUI(e) {
                    const t = a.Utility.getModuleJsonContent(this.moduleJsonPath);
                    let i, n = e;
                    if (a.Utility.checkIsValid(n)) if (u.packageRequieChild.includes(n)) {
                        this.requireContent.package_requires = a.Utility.checkIsValid(this.requireContent.package_requires) ? this.requireContent.package_requires : {
                            package_option: JSON,
                            path_option: []
                        }, this.requireContent.package_requires[n] = t.package_requires[n], i = t.package_requires;
                        const e = 2;
                        n = n.split("_").slice(0, e).join("_")
                    } else this.requireContent[n] = t[n], i = t[n]; else {
                        for (let e of u.requireCategory) this.requireContent[e] = t[e];
                        i = this.requireContent
                    }
                    a.Utility.checkIsValid(this.panel) && (this.delIconUri = a.Utility.checkIsValid(this.delIconUri) ? this.delIconUri : this.panel.webview.asWebviewUri(o.Uri.file(s.join(this.context.extensionPath, "images", "delete-dark.png"))), this.panel.webview.postMessage({
                        command: "settedRequire",
                        requireLibs: i,
                        requireType: null != n ? n : "all",
                        uri: this.delIconUri.toString()
                    }))
                }

                repeatedCheckAndAdd(e, t, i) {
                    return a.Utility.checkIsValid(e[t]) ? (o.window.showWarningMessage("The choosed require name already exists"), !1) : (e[t] = i, !0)
                }
            }

            t.RequiresActionController = p
        }, 7277: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.LibTreeView = void 0;
            const n = i(9496), o = i(5970);

            class r {
                constructor(e) {
                    this.treeViewInstance = new o.LibTreeDataProvider(e), this._viewer = n.window.createTreeView("extraLibrary", {
                        treeDataProvider: this.treeViewInstance,
                        showCollapseAll: !0
                    }), e.subscriptions.push(this._viewer), e.subscriptions.push(this._viewer.onDidExpandElement((e => {
                        this.treeViewInstance.refresh(e.element)
                    })))
                }

                updateTreeView(e, t) {
                    switch (e) {
                        case"add":
                            this.treeViewInstance.getChildren(t), this.treeViewInstance.refresh(t);
                            break;
                        case"delete":
                            this.treeViewInstance.getChildren(null == t ? void 0 : t.parent), this.treeViewInstance.refresh(null == t ? void 0 : t.parent)
                    }
                }

                dispose() {
                    r._instance = null, this.treeViewInstance = null
                }
            }

            t.LibTreeView = r
        }, 3464: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.SettingsProvider = void 0;
            const n = i(7147), o = i(9496), r = i(2853), s = i(9310), a = i(6529), c = i(9311), l = i(1017);

            class u extends r.ViewProvider {
                constructor(e) {
                    super(e), this.initialized = !1, this.viewType = "SettingsProvider", this.title = "Cangjie Configurations"
                }

                getcjpmBuildArgs() {
                    const e = s.Utility.getCjpmBuildArgsContent(this.cjpmbuildPath).split(/\s+/g), t = {};
                    for (let i = 0; i < e.length; i++) {
                        let n = e[i];
                        if (n.startsWith("--condition")) if (n.startsWith("--condition=")) {
                            const e = 13, i = n.substring(e, n.length - 1);
                            t.condition = i
                        } else {
                            const n = e[i + 1];
                            t.condition = n, i++
                        } else if ("-o" === n) {
                            const n = e[i + 1];
                            t.alias = n, i++
                        } else {
                            const e = this.cjpmBuildMap.get(n);
                            s.Utility.checkIsValid(e) ? t[e] = !0 : t[n] = " "
                        }
                    }
                    return this.cjpmBuildArgs = t, this.cjpmBuildArgs
                }

                getModuleJson() {
                    return this.moduleJson = s.Utility.getModuleJsonContent(this.moduleJsonPath), this.moduleJson
                }

                updateWebview(e, t) {
                    s.Utility.checkIsValid(this.panel) && (this.panel.webview.postMessage({
                        command: "settedBackend",
                        config: s.Utility.getSdkOption()
                    }), s.Utility.checkIsValid(e) && this.panel.webview.postMessage({
                        command: "settedcjpmBuildArgs",
                        config: e
                    }), s.Utility.checkIsValid(t) && this.panel.webview.postMessage({
                        command: "settedModuleJson",
                        config: t
                    }))
                }

                onPanelDisposed() {
                    u.existWebView = !1, s.Utility.checkIsValid(this.disposablesPanel) && (this.disposablesPanel.dispose(), this.panel = void 0)
                }

                onMessageReceived(e) {
                    switch (super.onMessageReceived(e), e.command) {
                        case"change":
                            if ("cjvm" === s.Utility.getSdkOption() && "coverage" === e.key) {
                                o.window.showWarningMessage("the coverage is only setted up in CJNative path");
                                break
                            }
                            this.updateConfig(e);
                            break;
                        case"initialized":
                            this.initialized = !0, this.settingsProviderActivated.fire(), this.updateWebview(this.getcjpmBuildArgs(), this.getModuleJson());
                            break;
                        case"addConfig":
                            if ("cjvm" === s.Utility.getSdkOption() && "cross_compile_configuration" === e.field) {
                                o.window.showWarningMessage("the cross_compile_configuration is only setted up in CJNative path");
                                break
                            }
                            this.updateCrsCmpConfig(e, "add");
                            break;
                        case"delConfig":
                            if ("cjvm" === s.Utility.getSdkOption() && "cross_compile_configuration" === e.field) {
                                o.window.showWarningMessage("the cross_compile_configuration is only setted up in CJNative path");
                                break
                            }
                            this.updateCrsCmpConfig(e, "delete");
                            break;
                        case"errorAdd":
                            o.window.showErrorMessage(e.value)
                    }
                }

                updateConfig(e) {
                    c.MODULEJSONARGS.includes(e.key) ? this.updateJsonConfig(e.key, this.moduleJsonPath, e.value) : this.updateJsonConfig(e.key, this.cjpmbuildPath, e.value)
                }

                updateJsonConfig(e, t, i) {
                    if (t.includes(c.cjpmBuildArgExtname)) {
                        this.getcjpmBuildArgs()[e] = i, this.updateCjpmbuildArgs(e, i)
                    } else {
                        const o = s.Utility.getModuleJsonContent(t);
                        o[e] = i;
                        try {
                            const e = l.join(s.Utility.getWorkspaceFolders(), t);
                            n.writeFileSync(e, JSON.stringify(o, null, "\t"))
                        } catch (e) {
                            a.OutputHelper.appendLine(e)
                        }
                    }
                }

                updateCjpmbuildArgs(e, t) {
                    this.getcjpmBuildArgs()[e] = t;
                    const i = c.cjpmBuildReplace[e];
                    let n = o.workspace.getConfiguration("cangjie.cangjieBuild").get("cjpmBuildArgs");
                    if ("boolean" == typeof t) {
                        const e = new RegExp(`(\\s+|^)${i}\\b`);
                        if (t && !n.match(e)) n += ` ${i} `; else {
                            if (t || !n.match(e)) return;
                            n = n.replace(e, "")
                        }
                    }
                    if ("string" == typeof t) {
                        let e, o;
                        if ("-o" === i) e = new RegExp(`(\\s+|^)${i}(\\s+\\S+)?`), o = ` -o ${t} `; else {
                            if ("--condition=" !== i) return;
                            e = n.includes("--condition=") ? new RegExp("(\\s+|^)--condition=(\\S+)?") : new RegExp("(\\s+|^)--condition(\\s+\\S+)?"), o = ` --condition="${t}" `
                        }
                        n = n.replace(e, ""), "" !== t && (n += o)
                    }
                    o.workspace.getConfiguration("cangjie.cangjieBuild").update("cjpmBuildArgs", n)
                }

                updateCrsCmpConfig(e, t) {
                    var i, o, r, c, u, d, p, h;
                    const g = e.field;
                    let m = s.Utility.getModuleJsonContent(this.moduleJsonPath);
                    if (!this.checkIsInit(m, g)) return;
                    let f = m[g], v = e.key, y = null === (i = e.oldValue) || void 0 === i ? void 0 : i[0], C = e.value;
                    if ("single_condition_option" === g && "string" != typeof e.value) {
                        if (!this.check(m, e, t)) return;
                        m.package_configuration[e.key].condition_option = s.Utility.checkIsValid(m.package_configuration[e.key].condition_option) ? m.package_configuration[e.key].condition_option : {};
                        let i = m.package_configuration[e.key].condition_option;
                        f = s.Utility.checkIsValid(i) ? i : {}, y = null === (o = e.oldValue) || void 0 === o ? void 0 : o[1], v = (null === (r = e.value) || void 0 === r ? void 0 : r.condition) || e.singleCnd, C = null === (c = e.value) || void 0 === c ? void 0 : c.configuration
                    }
                    switch (t) {
                        case"add": {
                            "package_configuration" !== g || "object" != typeof e.value || s.Utility.checkIsValid(e.value.output_type) || delete e.value.output_type;
                            let t = (null === (d = null === (u = m[g]) || void 0 === u ? void 0 : u[y]) || void 0 === d ? void 0 : d.condition_option) || (null === (h = null === (p = m[g]) || void 0 === p ? void 0 : p[v]) || void 0 === h ? void 0 : h.condition_option);
                            "package_configuration" === g && "object" == typeof C && s.Utility.checkIsValid(t) && (C.condition_option = t), s.Utility.checkIsValid(e.oldValue) && y !== v && this.replaceOldKey(f, y, v), f[v] = C;
                            break
                        }
                        case"delete":
                            delete f[v]
                    }
                    try {
                        const e = l.join(s.Utility.getWorkspaceFolders(), this.moduleJsonPath);
                        n.writeFileSync(e, JSON.stringify(m, null, "\t"))
                    } catch (e) {
                        a.OutputHelper.appendLine(e)
                    }
                }

                checkIsInit(e, t) {
                    if (void 0 === e[t]) if ("single_condition_option" === t) {
                        if (void 0 === e.package_configuration) return o.window.showWarningMessage("please fill package_configuration field first"), !1
                    } else e[t] = {};
                    return !0
                }

                check(e, t, i) {
                    var n, r;
                    if ("object" != typeof t.value) return !0;
                    if (!s.Utility.checkIsValid(e.package_configuration[t.key])) return o.window.showWarningMessage(`There is no setted package ${t.key} of package_configuration field`), !1;
                    return !("" === (null === (n = null == t ? void 0 : t.value) || void 0 === n ? void 0 : n.condition) || void 0 === (null === (r = null == t ? void 0 : t.value) || void 0 === r ? void 0 : r.condition)) || "add" !== i || (o.window.showWarningMessage("Condition name is not reasonable"), !1)
                }

                replaceOldKey(e, t, i) {
                    s.Utility.checkIsValid(t) && Object.keys(e).forEach((n => {
                        n === t ? (e[i] = e[t], delete e[t]) : (e[`_${n}`] = e[n], delete e[n], e[n] = e[`_${n}`], delete e[`_${n}`])
                    }))
                }
            }

            t.SettingsProvider = u, u.existWebView = !1
        }, 2853: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ViewProvider = void 0;
            const n = i(9496), o = i(1017), r = i(7147), s = i(6113), a = i(9310);
            t.ViewProvider = class {
                constructor(e) {
                    this.initialized = !1, this.existWebView = !1, this.settingsProviderActivated = new n.EventEmitter, this.moduleJsonPath = "/module.json", this.cjpmbuildPath = "/.vscode/cjpm_build_args.json", this.cjpmBuildMap = new Map([["-h", "help"], ["-g", "debug"], ["-V", "verbose"], ["--coverage", "coverage"], ["-s", "serial"], ["-i", "increment"], ["-o", "alias"], ["--condition", "condition"]]), this.context = e, this.disposable = n.Disposable.from(this.settingsProviderActivated)
                }

                createOrShow(e, t, i, o) {
                    var r;
                    const s = null != o ? o : null === (r = n.window.activeTextEditor) || void 0 === r ? void 0 : r.viewColumn;
                    a.Utility.checkIsValid(this.panel) ? this.panel.reveal(s, !1) : (this.initialized = !1, this.panel = n.window.createWebviewPanel(this.viewType, this.title, null != s ? s : n.ViewColumn.One, {
                        enableCommandUris: !0,
                        enableScripts: !0
                    }), this.existWebView = !0, this.disposablesPanel = n.Disposable.from(this.panel, this.panel.onDidDispose(this.onPanelDisposed, this), this.panel.webview.onDidReceiveMessage(this.onMessageReceived, this)), this.panel.webview.html = this.getHtml(e, t, i))
                }

                dispose() {
                    a.Utility.checkIsValid(this.panel) && this.panel.dispose(), a.Utility.checkIsValid(this.disposable) && this.disposable.dispose(), a.Utility.checkIsValid(this.disposablesPanel) && this.disposablesPanel.dispose()
                }

                onPanelDisposed() {
                    this.existWebView = !1, a.Utility.checkIsValid(this.disposablesPanel) && (this.disposablesPanel.dispose(), this.panel = void 0)
                }

                onMessageReceived(e) {
                    a.Utility.checkIsValid(e)
                }

                getHtml(e, t, i) {
                    var s;
                    let c;
                    if (c = r.readFileSync(o.resolve(this.context.extensionPath, e)).toString(), a.Utility.checkIsValid(null === (s = this.panel) || void 0 === s ? void 0 : s.webview)) {
                        const e = this.panel.webview.asWebviewUri(n.Uri.file(o.join(this.context.extensionPath, "media", t))),
                            r = this.panel.webview.asWebviewUri(n.Uri.file(o.join(this.context.extensionPath, "media", i)));
                        c = c.replace(/{{settings_js_uri}}/g, e.toString()), c = c.replace(/{{settings_css_uri}}/g, r.toString())
                    }
                    return c = c.replace(/{{nonce}}/g, this.getNonce()), c
                }

                getNonce() {
                    let e = "";
                    const t = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
                    for (let i = 0; i < 32; i++) e += t.charAt(Math.floor(62 * s.randomBytes(1)[0]));
                    return e
                }
            }
        }, 4795: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.CjcovWebview = t.Commands = void 0;
            const o = i(9496), r = i(1017), s = i(7147), a = i(2081), c = i(9310), l = i(6529), u = i(454), d = i(3837);
            var p;
            !function (e) {
                e.CJPM_UPDATE = "cjpm update", e.CJPM_COVERAGE = "cjpm build --coverage", e.LINUX_IMPLEMENT = "./build/bin/main", e.WIN_IMPLEMENT = ".\\build\\bin\\main", e.CJCOV = "cjcov -o output --html-details -i ", e.CJPM_CLEAN = "cjpm clean"
            }(p = t.Commands || (t.Commands = {}));

            class h extends u.Webview {
                constructor() {
                    super(), this.panelArray = [], this.disposable = o.Disposable.from(o.commands.registerCommand("cangjie.cjcov", (e => n(this, void 0, void 0, (function* () {
                        this.init(e)
                    }))))), this.disposableFolder = o.Disposable.from(o.commands.registerCommand("cangjie.cjcovFloder", (e => n(this, void 0, void 0, (function* () {
                        this.init(e)
                    })))))
                }

                static html(e) {
                    const t = r.join(c.Utility.getWorkspaceFolders(), "output", e);
                    let i = s.readFileSync(t, "utf-8");
                    return i = i.replace(/(?<id><\/html>)/g, (e => "\n    <script>\n    const vscode = acquireVsCodeApi();\n    let elementA = document.getElementsByTagName('a');\n    for (let a of elementA) {\n      a.addEventListener('click', function () {\n        vscode.postMessage({\n          path: a.title\n        });\n      });\n    }\n    <\/script> \n" + e)), i
                }

                dispose() {
                    this.disposable.dispose(), this.disposableFolder.dispose()
                }

                init(e) {
                    const t = Object.create(null, {newPanel: {get: () => super.newPanel}});
                    return n(this, void 0, void 0, (function* () {
                        if ("linux" !== process.platform && "win32" !== process.platform) return void o.window.showInformationMessage("This command applies only to Linux and Windows");
                        if (!(yield o.workspace.saveAll(!1))) return void o.window.showErrorMessage("auto save failed, please check and save dirty files manually!");
                        if (this.panelArray.length > 0) {
                            for (const e of this.panelArray) e.dispose();
                            this.panelArray.length = 0
                        }
                        let i = c.Utility.rightClickPath(e), n = c.Utility.getWorkspaceFolders();
                        if (!(yield this.execfunc(p.CJPM_UPDATE, n))) return;
                        if (!(yield this.execfunc(p.CJPM_COVERAGE, n))) return;
                        let r = p.LINUX_IMPLEMENT;
                        "win32" === process.platform && (r = p.WIN_IMPLEMENT);
                        if (!(yield this.execfunc(r, n))) return;
                        (yield this.execfunc(p.CJCOV, n, i)) && (t.newPanel.call(this, "cjcovWebview", "index.html"), this.panel.webview.html = h.html("./index.html"), this.panelArray.push(this.panel), this.panel.webview.onDidReceiveMessage((e => {
                            t.newPanel.call(this, "cjcovhtml", e.path), this.panel.webview.html = h.html(e.path), this.panelArray.push(this.panel)
                        })))
                    }))
                }

                execfunc(e, t, i) {
                    return n(this, void 0, void 0, (function* () {
                        let n = i, o = e;
                        void 0 === n ? n = "" : o += n;
                        const r = d.promisify(a.exec);
                        try {
                            let {stdout: e, stderr: i} = yield r(c.Utility.getExportPath().concat(o), {
                                cwd: t,
                                encoding: "utf8"
                            }), n = p.LINUX_IMPLEMENT;
                            return "win32" === process.platform && (n = p.WIN_IMPLEMENT), o !== n && o !== p.CJPM_CLEAN && l.OutputHelper.appendLine(e), !0
                        } catch (e) {
                            return l.OutputHelper.appendLine(e.stderr), o === p.CJPM_COVERAGE && (this.execfunc(p.CJPM_CLEAN, c.Utility.getWorkspaceFolders()), !1)
                        }
                    }))
                }
            }

            t.CjcovWebview = h
        }, 7913: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.CjlintWebview = void 0;
            const o = i(9496), r = i(1017), s = i(7147), a = i(2081), c = i(9310), l = i(6529), u = i(454);

            class d extends u.Webview {
                constructor(e) {
                    super(), this._context = e, this.disposable = o.Disposable.from(o.commands.registerCommand("cangjie.cjlint", (() => n(this, void 0, void 0, (function* () {
                        this.init()
                    }))))), this.disposableFolder = o.Disposable.from(o.commands.registerCommand("cangjie.cjlintFloder", (() => n(this, void 0, void 0, (function* () {
                        this.init()
                    })))))
                }

                static deleteFiles(e) {
                    let t = r.join(e, "./report.json"), i = r.join(e, "./default_CHIRDebug"),
                        n = r.join(e, "./page_CHIRDebug");
                    t.indexOf("..") < 0 && s.existsSync(t) && s.rm(t, (() => {
                    })), i.indexOf("..") < 0 && s.existsSync(i) && s.rm(i, (() => {
                    })), n.indexOf("..") < 0 && s.existsSync(n) && s.rm(n, (() => {
                    }))
                }

                static goFileLocation(e, t, i) {
                    s.existsSync(e) ? o.workspace.openTextDocument(e).then((e => {
                        o.window.showTextDocument(e, 1, !1).then((e => {
                            let n = e.document, r = n.lineAt(t).range.start.character,
                                s = n.lineAt(t).range.end.character, a = new o.Range(t, r, t, s);
                            e.revealRange(a, 1), e.selection = new o.Selection(new o.Position(t, i), new o.Position(t, i))
                        }))
                    })) : l.OutputHelper.appendLine(`${e} not found`)
                }

                dispose() {
                    this.disposable.dispose(), this.disposableFolder.dispose()
                }

                init() {
                    const e = Object.create(null, {newPanel: {get: () => super.newPanel}});
                    return n(this, void 0, void 0, (function* () {
                        if ("linux" !== process.platform && "win32" !== process.platform) return void o.window.showInformationMessage("This command applies only to Linux and Windows");
                        if (!(yield o.workspace.saveAll(!1))) return void o.window.showErrorMessage("auto save failed, please check and save dirty files manually!");
                        let t = r.join(c.Utility.getWorkspaceFolders(), "src");
                        if (l.OutputHelper.appendLine(`cjlint starts checking ${t} `), t.indexOf("..") >= 0) return void o.window.showErrorMessage("the target path is invaild!");
                        d.deleteFiles(t);
                        const i = `cjlint -f ${t} -o ./report`;
                        a.exec(c.Utility.getExportPath().concat(i), {cwd: t, encoding: "utf8"}, ((i, n, o) => {
                            if (c.Utility.checkIsValid(o)) return l.OutputHelper.appendLine(`cjlint check failed: ${o}`), this.closePanel(), void d.deleteFiles(t);
                            const a = r.join(t, "./report.json"), u = s.readFileSync(a, "utf-8");
                            if ("null" === u || "[\r\n]" === u) return l.OutputHelper.appendLine("The cjlint check is done, there are no out-of-spec issues.\n"), this.closePanel(), void d.deleteFiles(t);
                            e.newPanel.call(this, "cjlintWebview", "codecheck"), this.panel.webview.onDidReceiveMessage((e => {
                                d.goFileLocation(e.fileName, e.line - 1, e.column - 1)
                            }));
                            let p = JSON.stringify(JSON.parse(u), ["description", "file", "line", "column", "defectLevel", "defectType"]);
                            p = p.replace(/\\/g, "\\\\"), this.panel.webview.postMessage(JSON.parse(p)), this.resultPage(), l.OutputHelper.appendLine("cjlint check completed\n"), d.deleteFiles(t)
                        }))
                    }))
                }

                resultPage() {
                    this.panel.webview.html = this.html("html/contentHtml.html")
                }

                html(e) {
                    const {extensionPath: t} = this._context, i = r.join(t, e);
                    let n = s.readFileSync(i, "utf-8");
                    const a = this.panel.webview.asWebviewUri(o.Uri.file(r.join(t, "media", "CjlintResult.js")));
                    return n = n.replace(/{{cjlint_js_uri}}/g, a.toString()), n
                }
            }

            t.CjlintWebview = d
        }, 8619: (e, t) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.Commands = void 0, t.Commands = {
                CANGJIE_PROJECT_CREATE: "cangjie.project.create",
                CANGJIE_PROJECT_CREATE_VIEW: "cangjie.project.create.view",
                VSCODE_OPEN_FOLDER: "vscode.openFolder",
                VSCODE_OPEN: "vscode.open"
            }
        }, 9966: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.CreateProjectUtils = void 0;
            const o = i(1017), r = i(7147), s = i(9496), a = i(6529), c = i(9310), l = i(8619), u = i(2081),
                d = i(6729);

            class p {
                static selectPath() {
                    return new Promise((e => {
                        const t = c.Utility.getDefaultWorkspaceFolder();
                        let i, n;
                        c.Utility.checkIsValid(t) && (i = s.Uri.parse(o.resolve(t.uri.path, "..")), "win32" === process.platform && (i = s.Uri.file(o.resolve(t.uri.fsPath, "..")))), s.window.showOpenDialog({
                            defaultUri: t && i,
                            canSelectFiles: !1,
                            canSelectFolders: !0,
                            openLabel: "Select the project location"
                        }).then((t => {
                            let i;
                            n = t, c.Utility.checkIsValid(n) && 0 !== n.length && (i = "win32" === process.platform ? n[0].fsPath : n[0].path, e(i))
                        }))
                    }))
                }

                static createProject(e, t, i, p, h = "cangjie") {
                    return n(this, void 0, void 0, (function* () {
                        const g = o.join(t, i);
                        try {
                            r.mkdirSync(g);
                            o.join(p, "tools", "bin", "cjpm");
                            const t = {cwd: g};
                            let m = c.Utility.getExportPath().concat(`cjpm init ${h} ${i}`);
                            if (e !== d.ProjectType.EXECUTABLE && e !== d.ProjectType.CBC) {
                                const t = o.join(g, "src");
                                let n;
                                r.mkdirSync(t), r.writeFileSync(o.join(t, "demo.cj"), "// You can write Cangjie code here."), n = e === d.ProjectType.CJVM_STATIC ? "static" : e, m = c.Utility.getExportPath().concat(`cjpm init --type=${n} ${h} ${i}`)
                            }
                            c.Utility.isCreatedProject = !1, c.Utility.outputType = "executable", u.exec(m, t, ((e, t, i) => n(this, void 0, void 0, (function* () {
                                if (c.Utility.checkIsValid(e)) return a.OutputHelper.appendLine(i), g.indexOf("..") < 0 && r.existsSync(g) && r.rmdir(g, (() => {
                                })), void s.window.showErrorMessage("Failed to create the file. Please try again");
                                if (c.Utility.checkIsValid(t)) try {
                                    yield s.commands.executeCommand(l.Commands.VSCODE_OPEN_FOLDER, s.Uri.file(g), !0), this.closePanel(), s.window.showInformationMessage("Created successfully")
                                } catch (e) {
                                    return void s.window.showErrorMessage("Failed to open the file. Please open the file again")
                                }
                            }))))
                        } catch (e) {
                            return void s.window.showErrorMessage("Failed to create the file. Please try again")
                        }
                    }))
                }

                static closePanel() {
                    c.Utility.checkIsValid(p.panel) && (p.panel.dispose(), p.panel = void 0)
                }
            }

            t.CreateProjectUtils = p
        }, 6729: (e, t) => {
            "use strict";
            var i;
            Object.defineProperty(t, "__esModule", {value: !0}), t.projectTypes = t.ProjectType = void 0, function (e) {
                e.EXECUTABLE = "executable", e.CJNATIVE_STATIC = "static", e.DYNAMIC = "dynamic", e.CBC = "cbc", e.CJVM_STATIC = "cjvmStatic", e.CANGJIE = "CANGJIE"
            }(i = t.ProjectType || (t.ProjectType = {})), t.projectTypes = [{
                displayName: "Create Executable Output Cangjie project",
                metadata: {type: i.EXECUTABLE, extensionId: "", extensionName: "", createCommandId: ""}
            }, {
                displayName: "Create Static Output Cangjie project",
                metadata: {type: i.CJNATIVE_STATIC, extensionId: "", extensionName: "", createCommandId: ""}
            }, {
                displayName: "Create Dynamic Output Cangjie project",
                metadata: {type: i.DYNAMIC, extensionId: "", extensionName: "", createCommandId: ""}
            }]
        }, 8607: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.ProjectController = void 0;
            const o = i(9496), r = i(9496), s = i(8619), a = i(9310), c = i(6529), l = i(6729), u = i(9966);
            t.ProjectController = class {
                constructor(e) {
                    this.context = e, this.disposable = r.Disposable.from(r.commands.registerCommand(s.Commands.CANGJIE_PROJECT_CREATE, (() => {
                        "linux" === process.platform || "win32" === process.platform ? a.Utility.isExistSdk("cjpm").then((e => {
                            this.createCangjieProject(e)
                        })).catch((e => {
                            c.OutputHelper.appendLine(e)
                        })) : r.window.showInformationMessage("This command applies only to Linux and Windows")
                    })))
                }

                dispose() {
                    this.disposable.dispose()
                }

                createCangjieProject(e) {
                    return n(this, void 0, void 0, (function* () {
                        const t = l.projectTypes.map((e => ({
                            label: e.displayName,
                            description: e.description,
                            detail: a.Utility.checkIsValid(e.metadata.extensionName) ? `Provided by $(extensions) ${e.metadata.extensionName}` : e.detail,
                            metadata: e.metadata
                        }))), i = yield r.window.showQuickPick(t, {
                            ignoreFocusOut: !0,
                            placeHolder: "Select the output type"
                        });
                        if (!a.Utility.checkIsValid(i) || !(yield function (e, t) {
                            return n(this, void 0, void 0, (function* () {
                                if (!a.Utility.checkIsValid(t.extensionId)) return !0;
                                const e = r.extensions.getExtension(t.extensionId);
                                return void 0 !== e && (yield e.activate(), !0)
                            }))
                        }(i.label, i.metadata))) return;
                        let s = e;
                        s = "cjvmStatic" === i.metadata.type || "cbc" === i.metadata.type ? o.workspace.getConfiguration("Path").get("cjvmBackend") : o.workspace.getConfiguration("CangjieSdk").get("Path"), yield function (e, t) {
                            return n(this, void 0, void 0, (function* () {
                                const i = yield u.CreateProjectUtils.selectPath(),
                                    o = a.Utility.getBasePathAllFolders(i), s = yield r.window.showInputBox({
                                        prompt: "Input a Cangjie project name.",
                                        ignoreFocusOut: !0,
                                        validateInput: e => n(this, void 0, void 0, (function* () {
                                            return "" !== e && (null == e ? void 0 : e.match(/^[^*~/\\]+$/)) ? o.includes(e) ? "Repeats file name." : "" : "Please input a valid project name."
                                        }))
                                    });
                                yield function (e, t, i, o) {
                                    return n(this, void 0, void 0, (function* () {
                                        const s = /(?<id>^([a-zA-Z]+[_a-zA-Z0-9]*))$/, a = yield r.window.showInputBox({
                                            prompt: "Input module organization name.",
                                            ignoreFocusOut: !0,
                                            value: "cangjie",
                                            validateInput: e => n(this, void 0, void 0, (function* () {
                                                return "" === e.trim() || s.test(e.trim()) ? "" : "Please input a valid module organization name."
                                            }))
                                        });
                                        yield u.CreateProjectUtils.createProject(e, t, i, o, a)
                                    }))
                                }(e, i, s, t)
                            }))
                        }(i.metadata.type, s)
                    }))
                }
            }
        }, 5030: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.ViewController = void 0;
            const o = i(9496), r = i(1017), s = i(7147), a = i(9310), c = i(6529), l = i(8619), u = i(9966);
            t.ViewController = class {
                constructor(e) {
                    this.context = e, this.disposable = o.Disposable.from(o.commands.registerCommand(l.Commands.CANGJIE_PROJECT_CREATE_VIEW, (() => {
                        "linux" === process.platform || "win32" === process.platform ? a.Utility.isExistSdk("cjpm").then((t => {
                            this.init(e.extensionPath, t)
                        })).catch((e => {
                            c.OutputHelper.appendLine(e)
                        })) : o.window.showInformationMessage("This command applies only to Linux and Windows")
                    })))
                }

                dispose() {
                    this.disposable.dispose()
                }

                init(e, t) {
                    return n(this, void 0, void 0, (function* () {
                        if (a.Utility.checkIsValid(u.CreateProjectUtils.panel)) {
                            const e = a.Utility.checkIsValid(o.window.activeTextEditor) ? o.window.activeTextEditor.viewColumn : void 0;
                            return void u.CreateProjectUtils.panel.reveal(e)
                        }
                        u.CreateProjectUtils.panel = o.window.createWebviewPanel("cangjieWebview", "CreateNewCangjieProject", o.ViewColumn.One, {
                            enableScripts: !0,
                            retainContextWhenHidden: !0
                        }), u.CreateProjectUtils.panel.webview.onDidReceiveMessage((e => {
                            let i = t;
                            switch (e.command) {
                                case"selectPath":
                                    return void u.CreateProjectUtils.selectPath().then((e => {
                                        a.Utility.checkIsValid(e) && u.CreateProjectUtils.panel.webview.postMessage({
                                            command: "showPath",
                                            text: e
                                        })
                                    }));
                                case"cancelSetting":
                                    return void u.CreateProjectUtils.closePanel();
                                case"finishSetting":
                                    return i = "CJNative" === e.compileBackend ? o.workspace.getConfiguration("CangjieSdk").get("Path") : o.workspace.getConfiguration("CangjieSdkPath").get("cjvmBackend"), void this.finishSetting(e, i, u.CreateProjectUtils.panel);
                                default:
                                    return
                            }
                        })), u.CreateProjectUtils.panel.onDidDispose(u.CreateProjectUtils.closePanel);
                        const i = r.join(e, "html", "createProject.html");
                        let n = s.readFileSync(i, "utf8");
                        const c = u.CreateProjectUtils.panel.webview.asWebviewUri(o.Uri.file(r.join(e, "media", "createProject", "createSetting.js"))),
                            l = u.CreateProjectUtils.panel.webview.asWebviewUri(o.Uri.file(r.join(e, "media", "createProject", "createSetting.css")));
                        n = n.replace(/{{creat_js_uri}}/g, c.toString()), n = n.replace(/{{creat_css_uri}}/g, l.toString()), u.CreateProjectUtils.panel.webview.html = n
                    }))
                }

                finishSetting(e, t, i) {
                    a.Utility.getBasePathAllFolders(e.projectDir).includes(e.projectName) ? i.webview.postMessage({
                        command: "repeatsName",
                        text: !0
                    }) : (a.Utility.isCreatedProject = !0, a.Utility.outputType = e.outputType, u.CreateProjectUtils.createProject(e.outputType, e.projectDir, e.projectName, t, e.organization))
                }
            }
        }, 8757: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.CjFormat = void 0;
            const o = i(9496), r = i(2037), s = i(7147), a = i(2081), c = i(1017), l = i(6529), u = i(9310);
            t.CjFormat = class {
                static formatFunc(e, t = !1) {
                    return n(this, void 0, void 0, (function* () {
                        const t = "win32" === r.platform();
                        let i, n, d, p, h;
                        if (p = u.Utility.checkIsValid(e) ? e.fsPath : o.window.activeTextEditor.document.fileName, void 0 === p || p.indexOf("..") >= 0 || !s.existsSync(p)) return !1;
                        if (d = u.Utility.getCangjieHome(), !u.Utility.checkIsValid(d)) return o.window.showErrorMessage("Please check sdk path"), !1;
                        let g = "cjfmt";
                        if (t && (g = "cjfmt.exe"), i = c.join(d, "tools", "bin", g), n = c.join(d, "tools", "bin"), !s.existsSync(i)) return o.window.showErrorMessage("Format failed, no cjfmt tool"), !1;
                        if (!s.existsSync(n)) return o.window.showErrorMessage("Format failed, no bin directory in cangjie sdk"), !1;
                        try {
                            h = s.lstatSync(p)
                        } catch (e) {
                            return o.window.showErrorMessage(e.message), !1
                        }
                        if (h.isDirectory()) try {
                            if (!(yield o.workspace.saveAll(!1))) return o.window.showErrorMessage("auto save failed, please check and save dirty files manually!"), !1;
                            const e = `cjfmt -d ${p}`;
                            a.exec(u.Utility.getExportPath().concat(e), {cwd: n}, (e => {
                                u.Utility.checkIsValid(e) && l.OutputHelper.appendLine(`cjfmt check failed: ${e}`)
                            }))
                        } catch (e) {
                            return o.window.showErrorMessage(`Format failed ${e}`), e
                        } else {
                            if (".cj" !== c.extname(p)) return o.window.showErrorMessage("The plugin only can format Cangjie source code file"), !1;
                            try {
                                const e = o.window.activeTextEditor.document;
                                if (!u.Utility.checkIsValid(e)) return !1;
                                if (!(yield e.save())) return o.window.showErrorMessage("auto save failed, please check and save dirty files manually!"), !1;
                                const t = `cjfmt -f ${p}`;
                                a.exec(u.Utility.getExportPath().concat(t), {cwd: n}, (e => {
                                    u.Utility.checkIsValid(e) && l.OutputHelper.appendLine(`cjfmt check failed: ${e}`)
                                }))
                            } catch (e) {
                                return o.window.showErrorMessage(`Format failed ${e}`), e
                            }
                        }
                        return !0
                    }))
                }
            }
        }, 966: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.deactivate = t.activate = void 0;
            const o = i(9496), r = i(1017), s = i(3680), a = i(8607), c = i(8757), l = i(9310), u = i(3240),
                d = i(5030), p = i(7913), h = i(7865), g = i(7277), m = i(4795), f = i(7026), v = i(2838), y = i(178);
            t.activate = function (e) {
                return n(this, void 0, void 0, (function* () {
                    if (!v.CheckJson.checkJsonKey()) return;
                    l.Utility.configTerminalcjpmEnv(), "cjvm" === l.Utility.getSdkOption() && o.commands.executeCommand("setContext", "cangjieSdk.cjvm", !0), new u.CjpmBuildCollection(e), f.TerminalHelper.setContext(e), e.subscriptions.push(new a.ProjectController(e)), e.subscriptions.push(new d.ViewController(e)), l.Utility.checkIsValid(u.CjpmBuildCollection.requireConfigure) || (h.RequiresActionController.instace = new h.RequiresActionController(e), u.CjpmBuildCollection.requireConfigure = h.RequiresActionController.instace), e.subscriptions.push(h.RequiresActionController.instace), g.LibTreeView._instance = new g.LibTreeView(e), function (e) {
                        const t = o.workspace.createFileSystemWatcher(new o.RelativePattern(o.workspace.workspaceFolders[0], "*.json"));
                        e.subscriptions.push(t), e.subscriptions.push(t.onDidCreate((t => {
                            if (o.workspace.workspaceFolders[0].uri.fsPath === r.dirname(t.fsPath)) {
                                const t = l.Utility.getModuleJsonContent("/module.json");
                                if (!l.Utility.checkIsValid(t)) return;
                                v.CheckJson.checkJsonKey() && (g.LibTreeView._instance = new g.LibTreeView(e))
                            }
                        })))
                    }(e);
                    const t = o.workspace.createFileSystemWatcher(new o.RelativePattern(o.workspace.workspaceFolders[0], "**/module.json"));
                    e.subscriptions.push(t.onDidChange((e => n(this, void 0, void 0, (function* () {
                        v.CheckJson.checkJsonKey()
                    }))))), e.subscriptions.push(o.commands.registerCommand("cangjie.lsp.updateState", (e => {
                        l.Utility.serverRun = e
                    }))), e.subscriptions.push(o.workspace.onDidChangeConfiguration((e => n(this, void 0, void 0, (function* () {
                        s.UpdateSdk.listenSdkPath(e), l.Utility.configTerminalcjpmEnv(), l.Utility.checkSaveState()
                    }))))), function (e) {
                        e.subscriptions.push(o.commands.registerCommand("extension.formatCj", (e => n(this, void 0, void 0, (function* () {
                            c.CjFormat.formatFunc(e)
                        }))))), e.subscriptions.push(o.commands.registerCommand("extension.formatCjFolder", (e => n(this, void 0, void 0, (function* () {
                            c.CjFormat.formatFunc(e)
                        })))))
                    }(e), function (e) {
                        e.subscriptions.push(o.workspace.onDidSaveTextDocument((e => {
                            if (".cj" !== r.extname(e.fileName)) return;
                            const t = o.workspace.getConfiguration("cangjie.format").get("formatOnSave");
                            "boolean" == typeof t && t && c.CjFormat.formatFunc(e.uri, !0)
                        })))
                    }(e), e.subscriptions.push(new p.CjlintWebview(e)), e.subscriptions.push(new m.CjcovWebview), o.tasks.registerTaskProvider("buildcangjie", new y.CustomBuildTaskProvider)
                }))
            }, t.deactivate = function () {
            }
        }, 3680: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.UpdateSdk = void 0;
            const o = i(9496), r = i(7147), s = i(9310), a = i(1017);

            class c {
                static listenSdkPath(e) {
                    return n(this, void 0, void 0, (function* () {
                        let t, i = s.Utility.getCangjieHome();
                        o.workspace.getConfiguration(), t = "CJNative" === s.Utility.getSdkOption() ? e.affectsConfiguration("CangjieSdk.Path") : e.affectsConfiguration("CangjieSdkPath.cjvmBackend"), t && (r.existsSync(i) && c.envsetupFileIsExist(i) ? (s.Utility.configTerminalcjpmEnv(), o.window.showInformationMessage("Set SDK path success")) : o.window.showErrorMessage("SDK path is not exists"))
                    }))
                }

                static listenSdkOption(e) {
                    return n(this, void 0, void 0, (function* () {
                        if (e.affectsConfiguration("CangjieSdk.Option")) {
                            let e = s.Utility.getSdkOption();
                            o.window.showInformationMessage(`You have changed to ${e}-backend compiler.`);
                            const t = a.join(s.Utility.getWorkspaceFolders(), "module.json");
                            if (r.existsSync(t)) {
                                let i = JSON.parse(r.readFileSync(t, "utf-8"));
                                "CJNative" === e ? (o.commands.executeCommand("setContext", "cangjieSdk.cjvm", !1), "cbc" === i.output_type ? i.output_type = "executable" : i.output_type = "static") : (o.commands.executeCommand("setContext", "cangjieSdk.cjvm", !0), "executable" === i.output_type ? i.output_type = "cbc" : i.output_type = "static"), r.writeFileSync(t, JSON.stringify(i, null, "\t"))
                            }
                        }
                    }))
                }

                static envsetupFileIsExist(e) {
                    let t = !1;
                    return r.readdirSync(e).forEach((e => {
                        "envsetup.sh" !== e || (t = !0)
                    })), t
                }
            }

            t.UpdateSdk = c
        }, 2838: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.CheckJson = void 0;
            const n = i(9310), o = i(9311), r = i(9496);
            t.CheckJson = class {
                static checkJsonKey() {
                    const e = n.Utility.getModuleJsonContent(o.moduleJsonextname);
                    if (!n.Utility.checkIsValid(e)) return !0;
                    const t = Object.keys(e);
                    for (let e in o.MUSTKEYOFMODULEJSON) if (!t.includes(o.MUSTKEYOFMODULEJSON[e])) return r.window.showErrorMessage(`The module.json is missing required field ${o.MUSTKEYOFMODULEJSON[e]}`), !1;
                    return !0
                }
            }
        }, 9311: (e, t) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.builtinConditions = t.secondPosition = t.firstPosition = t.conditionArrayNum = t.testCangjieFile = t.modulePathKeyInModuleJson = t.moduleNameKeyInModuleJson = t.cjpmBuildArgExtname = t.moduleJsonextname = t.requireCategoryNew = t.packageRequieChild = t.requireCategory = t.numOfcjpmTest = t.numOfModuleJson = t.delay10 = t.delay100 = t.envPathName = t.CustomAction = t.CJVMKEYOFMODULEJSON = t.KEYOFMODULEJSON = t.MUSTKEYOFMODULEJSON = t.MODULEJSONARGS = t.cjpmBuildReplace = t.cjpmBuildArgs = t.buildMagicNum = void 0, t.buildMagicNum = {
                mutilPickTotalSteps: 2,
                firstPick: 1,
                secondPick: 2,
                enterAlias: 3,
                extraStep: 1
            }, t.cjpmBuildArgs = {
                help: !1,
                debug: !1,
                verbose: !1,
                coverage: !1,
                serial: !1,
                increment: !1,
                alias: "",
                condition: ""
            }, t.cjpmBuildReplace = {
                help: "-h",
                debug: "-g",
                verbose: "-V",
                coverage: "--coverage",
                alias: "-o",
                serial: "-s",
                increment: "-i",
                condition: "--condition="
            }, t.MODULEJSONARGS = ["command_option", "cjc_version", "description", "version", "organization", "name", "output_type", "link_option", "package_configuration", "condition_option"], t.MUSTKEYOFMODULEJSON = ["cjc_version", "organization", "name", "version", "output_type"], t.KEYOFMODULEJSON = ["cjc_version", "organization", "name", "description", "version", "requires", "package_requires", "foreign_requires", "output_type", "command_option", "condition_option", "link_option", "package_configuration"], t.CJVMKEYOFMODULEJSON = ["cjc_version", "organization", "name", "description", "version", "build_dir", "requires", "package_requires", "output_type", "command_option", "condition_option", "link_option", "package_configuration"];

            class i {
            }

            t.CustomAction = i, i.back = new i, i.cancel = new i, i.resume = new i, t.envPathName = {
                CANGJIE_HOME: "CANGJIE_HOME",
                PATH: "PATH",
                LD_LIBRARY_PATH: "LD_LIBRARY_PATH"
            }, t.delay100 = 100, t.delay10 = 10, t.numOfModuleJson = 6, t.numOfcjpmTest = 5, t.requireCategory = ["requires", "package_requires", "foreign_requires"], t.packageRequieChild = ["path_option", "package_option"], t.requireCategoryNew = ["requires", "package_option", "path_option", "package_requires", "foreign_requires"], t.moduleJsonextname = "/module.json", t.cjpmBuildArgExtname = "/.vscode/cjpm_build_args.json", t.moduleNameKeyInModuleJson = "name", t.modulePathKeyInModuleJson = "path", t.testCangjieFile = "testCangjie.cj", t.conditionArrayNum = 2, t.firstPosition = 0, t.secondPosition = 1, t.builtinConditions = new Set(["os", "backend", "debug", "cjc_version"])
        }, 7290: (e, t) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ModuleJsonImpl = void 0;
            t.ModuleJsonImpl = class {
            }
        }, 6529: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.OutputHelper = void 0;
            const n = i(9496), o = i(2081), r = i(9310);

            class s {
                static appendLine(e, t) {
                    r.Utility.checkIsValid(this.outputChannel) || (this.outputChannel = n.window.createOutputChannel("Cangjie Project Related Trace")), t || this.outputChannel.appendLine(`[Execute Time - ${(new Date).toLocaleString()}]`), this.outputChannel.show(), this.outputChannel.appendLine(e)
                }

                static execCommand(e, t) {
                    return new Promise(((i, n) => {
                        o.exec(e, {cwd: t, encoding: "utf8"}, ((e, t, n) => {
                            r.Utility.checkIsValid(e) && (s.appendLine(null == e ? void 0 : e.stack, !0), i(!1)), r.Utility.checkIsValid(n) && r.Utility.checkIsValid(t) && (s.appendLine(n, !0), s.appendLine(t), i(!0)), r.Utility.checkIsValid(n) && r.Utility.checkIsValid(e) && (s.appendLine(n, !0), i(!1)), !r.Utility.checkIsValid(n) && r.Utility.checkIsValid(t) && (s.appendLine(t), i(!0))
                        }))
                    }))
                }
            }

            t.OutputHelper = s
        }, 7026: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.TerminalHelper = void 0;
            const o = i(9496), r = i(9496), s = i(9310), a = i(9311), c = i(2081), l = i(1017);
            t.TerminalHelper = class {
                static execCommand(e, t = !1) {
                    return n(this, void 0, void 0, (function* () {
                        this.clearCjPanels();
                        let i = "/bin/bash";
                        if ("win32" === process.platform && (i = "C:\\Windows\\System32\\cmd.exe"), this.cangjieTerminal = r.window.createTerminal({
                            name: "cangjie",
                            shellPath: i
                        }), "linux" === process.platform) {
                            let e = `source ${l.join(s.Utility.getCangjieHome(), "envsetup.sh")}`;
                            this.cangjieTerminal.sendText(e), this.cangjieTerminal.sendText("clear")
                        } else {
                            if ("win32" !== process.platform) return;
                            this.cangjieTerminal.sendText("cls")
                        }
                        let n = e;
                        if (t) {
                            let t = "./build/bin/";
                            if ("linux" === process.platform) {
                                const i = l.join(t, this.getBinaryName(e));
                                n = `${e}; if [ $? == 0 ]; then ${i}; fi`
                            } else {
                                t = ".\\build\\bin\\";
                                const i = l.join(t, this.getBinaryName(e));
                                n = `${e}&&${i}`
                            }
                        }
                        if (this.cangjieTerminal.sendText(n), this.cangjieTerminal.show(!1), "win32" === process.platform) return;
                        let c = yield this.cangjieTerminal.processId;
                        if ("number" == typeof c) {
                            for (yield this.sleep(a.delay100); this.checkcjpmProcess(c);) this.sleep(a.delay10);
                            s.Utility.serverRun && s.Utility.checkMacroLib() && o.commands.executeCommand("cangjie.lsp.reLaunch")
                        }
                    }))
                }

                static setContext(e) {
                    this.context = e
                }

                static clearCjPanels() {
                    const e = r.window.terminals;
                    for (let t of e) "cangjie" === t.name && t.dispose()
                }

                static getBinaryName(e) {
                    if (e.includes("-o")) {
                        let t = e.split(" "), i = t.indexOf("-o");
                        return "CJNative" === s.Utility.getSdkOption() ? t[i + 1] : `${t[i + 1]}.cbc`
                    }
                    return "CJNative" === s.Utility.getSdkOption() ? "main" : "main.cbc"
                }

                static sleep(e) {
                    return n(this, void 0, void 0, (function* () {
                        yield new Promise((t => {
                            setTimeout(t, e)
                        }))
                    }))
                }

                static checkcjpmProcess(e) {
                    let t = `ps -ef | grep cjpm | grep ${e.toString()}`;
                    return -1 !== c.execSync(t).toString().indexOf("cjpm build")
                }
            }
        }, 9310: function (e, t, i) {
            "use strict";
            var n = this && this.__awaiter || function (e, t, i, n) {
                return new (i || (i = Promise))((function (o, r) {
                    function s(e) {
                        try {
                            c(n.next(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function a(e) {
                        try {
                            c(n.throw(e))
                        } catch (e) {
                            r(e)
                        }
                    }

                    function c(e) {
                        var t;
                        e.done ? o(e.value) : (t = e.value, t instanceof i ? t : new i((function (e) {
                            e(t)
                        }))).then(s, a)
                    }

                    c((n = n.apply(e, t || [])).next())
                }))
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.Utility = void 0;
            const o = i(1017), r = i(7147), s = i(2081), a = i(9496), c = i(9311), l = i(6529);

            class u {
                static getDefaultWorkspaceFolder() {
                    if (void 0 !== a.workspace.workspaceFolders) return 1 === a.workspace.workspaceFolders.length ? a.workspace.workspaceFolders[0] : u.checkIsValid(a.window.activeTextEditor) ? a.workspace.getWorkspaceFolder(a.window.activeTextEditor.document.uri) : void 0
                }

                static rightClickPath(e) {
                    let t;
                    return t = u.checkIsValid(e) ? e.fsPath : a.window.activeTextEditor.document.fileName, void 0 === t ? "" : t
                }

                static isExistSdk(e) {
                    return new Promise(((t, i) => {
                        const n = u.getCangjieHome();
                        if (u.checkIsValid(n)) switch (e) {
                            case"cjpm": {
                                o.join(n, "tools", "bin", e);
                                const r = u.getExportPath().concat("cjpm -v");
                                s.exec(r, (e => {
                                    u.checkIsValid(e) && i(e), t(n)
                                }));
                                break
                            }
                            default:
                                t(n)
                        } else i(new Error("error: Please download and configure the Cangjie SDK.\n"))
                    }))
                }

                static delay(e) {
                    return n(this, void 0, void 0, (function* () {
                        yield new Promise((t => {
                            setTimeout(t, e)
                        }))
                    }))
                }

                static getBasePathAllFolders(e) {
                    return r.readdirSync(e)
                }

                static getWorkspaceFolders() {
                    if (u.checkIsValid(a.workspace.workspaceFolders)) return a.workspace.workspaceFolders[0].uri.fsPath;
                    l.OutputHelper.appendLine("no open work folder!")
                }

                static getSdkOption() {
                    return "CJNative"
                }

                static getCangjieHome() {
                    return this.isCreatedProject ? "cbc" === this.outputType || "cjvmStatic" === this.outputType ? a.workspace.getConfiguration("CangjieSdkPath").get("cjvmBackend") : a.workspace.getConfiguration("CangjieSdk").get("Path") : "CJNative" === this.getSdkOption() ? a.workspace.getConfiguration("CangjieSdk").get("Path") : a.workspace.getConfiguration("CangjieSdkPath").get("cjvmBackend")
                }

                static configTerminalcjpmEnv() {
                    return n(this, void 0, void 0, (function* () {
                        if ("linux" !== process.platform && "win32" !== process.platform) return;
                        const e = u.getCangjieHome();
                        if (!r.existsSync(e)) return;
                        if (!u.isCangjieProject()) return;
                        const t = u.getenvPaths();
                        let i = {};
                        "linux" === process.platform ? (t.forEach((e => {
                            let t = e.replace("export ", "").split("=");
                            const [n, o] = t;
                            i[n] = o
                        })), i[c.envPathName.LD_LIBRARY_PATH] = i[c.envPathName.LD_LIBRARY_PATH].replace(":${LD_LIBRARY_PATH}", "")) : t.forEach((e => {
                            let t = e.replace("set ", "").split("=");
                            const [n, o] = t;
                            i[n] = o
                        })), "linux" === process.platform ? a.workspace.getConfiguration("terminal.integrated.env").update("linux", {
                            CANGJIE_HOME: `${e}`,
                            PATH: `${e}/bin:${e}/tools/bin:${e}/debugger/bin:\${env:PATH}`,
                            LD_LIBRARY_PATH: `${i[c.envPathName.LD_LIBRARY_PATH]}:\${env:LD_LIBRARY_PATH}`
                        }) : a.workspace.getConfiguration("terminal.integrated.env").update("windows", {
                            CANGJIE_HOME: `${e}`,
                            PATH: `${e}\\runtime\\lib\\windows_x86_64_llvm;${e}\\bin;${e}\\tools\\bin;${e}\\debugger\\bin;\${env:PATH}`
                        })
                    }))
                }

                static getModuleJsonContent(e) {
                    if (e !== c.moduleJsonextname) return;
                    const t = o.join(u.getWorkspaceFolders(), e);
                    if (!r.existsSync(t)) return;
                    const i = r.readFileSync(t, "utf8");
                    return "" === i ? "" : JSON.parse(i)
                }

                static getCjpmBuildArgsContent(e) {
                    if (e !== c.cjpmBuildArgExtname) return;
                    return a.workspace.getConfiguration("cangjie.cangjieBuild").get("cjpmBuildArgs")
                }

                static getExportPath() {
                    const e = u.getenvPaths();
                    let t = "";
                    return e.forEach((e => {
                        t += `${e}&&`
                    })), t
                }

                static getenvPaths() {
                    const e = u.getCangjieHome();
                    try {
                        if (!r.existsSync(e)) throw new Error("cangjie sdk path not exist, please configuration it first!");
                        let t = o.join(e, "envsetup.sh"), i = r.readFileSync(t, "utf8"), n = /\${script_dir}/g,
                            s = /\${CANGJIE_HOME}/g, a = /export(?<id>.)*/g;
                        if ("win32" === process.platform && (t = o.join(e, "envsetup.bat"), i = r.readFileSync(t, "utf8"), n = /%~dp0/g, s = /%CANGJIE_HOME%/g, a = /set(?<id>.)*/g), !u.checkIsValid(i)) throw new Error("envsetup.sh is empty");
                        i = i.replace(n, `${e}`), i = "linux" === process.platform ? i.replace(s, `${e}`) : i.replace(s, `${e}\\`);
                        return i.match(a)
                    } catch (e) {
                        return l.OutputHelper.appendLine(`[error]: ${e}`, !0), []
                    }
                }

                static isCangjieProject() {
                    if (u.checkIsValid(a.workspace.workspaceFolders)) {
                        const e = a.workspace.workspaceFolders[0].uri.fsPath, t = o.join(e, "module.json");
                        return r.existsSync(t)
                    }
                    return !1
                }

                static noModuleJson() {
                    a.window.showErrorMessage("The project can not find module.json file. you can use cjpm init to create")
                }

                static checkIsValid(e) {
                    return null != e && (("string" != typeof e || "" !== e) && (("number" != typeof e || 0 !== e) && ("boolean" != typeof e || e)))
                }

                static checkMacroLib() {
                    const e = o.join(this.getWorkspaceFolders(), "build"),
                        t = o.join(this.getWorkspaceFolders(), "module.json");
                    if (!r.existsSync(e) || !r.existsSync(t)) return !1;
                    let i = this.getModuleJsonContent(c.moduleJsonextname);
                    if (!("name" in i)) return !1;
                    const n = i.name, s = o.join(this.getWorkspaceFolders(), "build", n);
                    if (!r.existsSync(s)) return !1;
                    const a = r.readdirSync(s);
                    for (let e of a) if (e.endsWith(".so")) return !0;
                    return !1
                }

                static checkSaveState() {
                    const e = a.workspace.getConfiguration("files").get("autoSave"),
                        t = a.workspace.getConfiguration("cangjie.format").get("formatOnSave");
                    "boolean" == typeof t && t && "afterDelay" === e && (a.window.showWarningMessage("formatOnSave can not be used with autoSave!"), a.workspace.getConfiguration("cangjie.format").update("formatOnSave", !1))
                }
            }

            t.Utility = u, u.isCreatedProject = !1, u.outputType = "executable", u.serverRun = !1
        }, 454: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.Webview = void 0;
            const n = i(9496), o = i(9310);
            t.Webview = class {
                constructor() {
                    this.panel = null
                }

                newPanel(e, t) {
                    o.Utility.checkIsValid(this.panel) && "cjcovhtml" !== e ? this.panel.reveal(n.ViewColumn.Beside, !0) : (this.panel = n.window.createWebviewPanel(e, t, {
                        viewColumn: n.ViewColumn.Beside,
                        preserveFocus: !0
                    }, {enableScripts: !0, retainContextWhenHidden: !0}), this.panel.onDidDispose((() => {
                        this.panel = null
                    })))
                }

                closePanel() {
                    o.Utility.checkIsValid(this.panel) && (this.panel.dispose(), this.panel = null)
                }
            }
        }, 8178: e => {
            "use strict";

            function t(e, t, o) {
                e instanceof RegExp && (e = i(e, o)), t instanceof RegExp && (t = i(t, o));
                var r = n(e, t, o);
                return r && {
                    start: r[0],
                    end: r[1],
                    pre: o.slice(0, r[0]),
                    body: o.slice(r[0] + e.length, r[1]),
                    post: o.slice(r[1] + t.length)
                }
            }

            function i(e, t) {
                var i = t.match(e);
                return i ? i[0] : null
            }

            function n(e, t, i) {
                var n, o, r, s, a, c = i.indexOf(e), l = i.indexOf(t, c + 1), u = c;
                if (c >= 0 && l > 0) {
                    if (e === t) return [c, l];
                    for (n = [], r = i.length; u >= 0 && !a;) u == c ? (n.push(u), c = i.indexOf(e, u + 1)) : 1 == n.length ? a = [n.pop(), l] : ((o = n.pop()) < r && (r = o, s = l), l = i.indexOf(t, u + 1)), u = c < l && c >= 0 ? c : l;
                    n.length && (a = [r, s])
                }
                return a
            }

            e.exports = t, t.range = n
        }, 1266: (e, t, i) => {
            var n = i(732), o = i(8178);
            e.exports = function (e) {
                if (!e) return [];
                "{}" === e.substr(0, 2) && (e = "\\{\\}" + e.substr(2));
                return v(function (e) {
                    return e.split("\\\\").join(r).split("\\{").join(s).split("\\}").join(a).split("\\,").join(c).split("\\.").join(l)
                }(e), !0).map(d)
            };
            var r = "\0SLASH" + Math.random() + "\0", s = "\0OPEN" + Math.random() + "\0",
                a = "\0CLOSE" + Math.random() + "\0", c = "\0COMMA" + Math.random() + "\0",
                l = "\0PERIOD" + Math.random() + "\0";

            function u(e) {
                return parseInt(e, 10) == e ? parseInt(e, 10) : e.charCodeAt(0)
            }

            function d(e) {
                return e.split(r).join("\\").split(s).join("{").split(a).join("}").split(c).join(",").split(l).join(".")
            }

            function p(e) {
                if (!e) return [""];
                var t = [], i = o("{", "}", e);
                if (!i) return e.split(",");
                var n = i.pre, r = i.body, s = i.post, a = n.split(",");
                a[a.length - 1] += "{" + r + "}";
                var c = p(s);
                return s.length && (a[a.length - 1] += c.shift(), a.push.apply(a, c)), t.push.apply(t, a), t
            }

            function h(e) {
                return "{" + e + "}"
            }

            function g(e) {
                return /^-?0\d/.test(e)
            }

            function m(e, t) {
                return e <= t
            }

            function f(e, t) {
                return e >= t
            }

            function v(e, t) {
                var i = [], r = o("{", "}", e);
                if (!r || /\$$/.test(r.pre)) return [e];
                var s, c = /^-?\d+\.\.-?\d+(?:\.\.-?\d+)?$/.test(r.body),
                    l = /^[a-zA-Z]\.\.[a-zA-Z](?:\.\.-?\d+)?$/.test(r.body), d = c || l, y = r.body.indexOf(",") >= 0;
                if (!d && !y) return r.post.match(/,.*\}/) ? v(e = r.pre + "{" + r.body + a + r.post) : [e];
                if (d) s = r.body.split(/\.\./); else if (1 === (s = p(r.body)).length && 1 === (s = v(s[0], !1).map(h)).length) return (b = r.post.length ? v(r.post, !1) : [""]).map((function (e) {
                    return r.pre + s[0] + e
                }));
                var C, w = r.pre, b = r.post.length ? v(r.post, !1) : [""];
                if (d) {
                    var k = u(s[0]), S = u(s[1]), D = Math.max(s[0].length, s[1].length),
                        P = 3 == s.length ? Math.abs(u(s[2])) : 1, T = m;
                    S < k && (P *= -1, T = f);
                    var R = s.some(g);
                    C = [];
                    for (var _ = k; T(_, S); _ += P) {
                        var x;
                        if (l) "\\" === (x = String.fromCharCode(_)) && (x = ""); else if (x = String(_), R) {
                            var E = D - x.length;
                            if (E > 0) {
                                var O = new Array(E + 1).join("0");
                                x = _ < 0 ? "-" + O + x.slice(1) : O + x
                            }
                        }
                        C.push(x)
                    }
                } else C = n(s, (function (e) {
                    return v(e, !1)
                }));
                for (var j = 0; j < C.length; j++) for (var M = 0; M < b.length; M++) {
                    var I = w + C[j] + b[M];
                    (!t || d || I) && i.push(I)
                }
                return i
            }
        }, 732: e => {
            e.exports = function (e, i) {
                for (var n = [], o = 0; o < e.length; o++) {
                    var r = i(e[o], o);
                    t(r) ? n.push.apply(n, r) : n.push(r)
                }
                return n
            };
            var t = Array.isArray || function (e) {
                return "[object Array]" === Object.prototype.toString.call(e)
            }
        }, 4830: (e, t, i) => {
            "use strict";
            const n = i(852), o = Symbol("max"), r = Symbol("length"), s = Symbol("lengthCalculator"),
                a = Symbol("allowStale"), c = Symbol("maxAge"), l = Symbol("dispose"), u = Symbol("noDisposeOnSet"),
                d = Symbol("lruList"), p = Symbol("cache"), h = Symbol("updateAgeOnGet"), g = () => 1;
            const m = (e, t, i) => {
                const n = e[p].get(t);
                if (n) {
                    const t = n.value;
                    if (f(e, t)) {
                        if (y(e, n), !e[a]) return
                    } else i && (e[h] && (n.value.now = Date.now()), e[d].unshiftNode(n));
                    return t.value
                }
            }, f = (e, t) => {
                if (!t || !t.maxAge && !e[c]) return !1;
                const i = Date.now() - t.now;
                return t.maxAge ? i > t.maxAge : e[c] && i > e[c]
            }, v = e => {
                if (e[r] > e[o]) for (let t = e[d].tail; e[r] > e[o] && null !== t;) {
                    const i = t.prev;
                    y(e, t), t = i
                }
            }, y = (e, t) => {
                if (t) {
                    const i = t.value;
                    e[l] && e[l](i.key, i.value), e[r] -= i.length, e[p].delete(i.key), e[d].removeNode(t)
                }
            };

            class C {
                constructor(e, t, i, n, o) {
                    this.key = e, this.value = t, this.length = i, this.now = n, this.maxAge = o || 0
                }
            }

            const w = (e, t, i, n) => {
                let o = i.value;
                f(e, o) && (y(e, i), e[a] || (o = void 0)), o && t.call(n, o.value, o.key, e)
            };
            e.exports = class {
                constructor(e) {
                    if ("number" == typeof e && (e = {max: e}), e || (e = {}), e.max && ("number" != typeof e.max || e.max < 0)) throw new TypeError("max must be a non-negative number");
                    this[o] = e.max || 1 / 0;
                    const t = e.length || g;
                    if (this[s] = "function" != typeof t ? g : t, this[a] = e.stale || !1, e.maxAge && "number" != typeof e.maxAge) throw new TypeError("maxAge must be a number");
                    this[c] = e.maxAge || 0, this[l] = e.dispose, this[u] = e.noDisposeOnSet || !1, this[h] = e.updateAgeOnGet || !1, this.reset()
                }

                set max(e) {
                    if ("number" != typeof e || e < 0) throw new TypeError("max must be a non-negative number");
                    this[o] = e || 1 / 0, v(this)
                }

                get max() {
                    return this[o]
                }

                set allowStale(e) {
                    this[a] = !!e
                }

                get allowStale() {
                    return this[a]
                }

                set maxAge(e) {
                    if ("number" != typeof e) throw new TypeError("maxAge must be a non-negative number");
                    this[c] = e, v(this)
                }

                get maxAge() {
                    return this[c]
                }

                set lengthCalculator(e) {
                    "function" != typeof e && (e = g), e !== this[s] && (this[s] = e, this[r] = 0, this[d].forEach((e => {
                        e.length = this[s](e.value, e.key), this[r] += e.length
                    }))), v(this)
                }

                get lengthCalculator() {
                    return this[s]
                }

                get length() {
                    return this[r]
                }

                get itemCount() {
                    return this[d].length
                }

                rforEach(e, t) {
                    t = t || this;
                    for (let i = this[d].tail; null !== i;) {
                        const n = i.prev;
                        w(this, e, i, t), i = n
                    }
                }

                forEach(e, t) {
                    t = t || this;
                    for (let i = this[d].head; null !== i;) {
                        const n = i.next;
                        w(this, e, i, t), i = n
                    }
                }

                keys() {
                    return this[d].toArray().map((e => e.key))
                }

                values() {
                    return this[d].toArray().map((e => e.value))
                }

                reset() {
                    this[l] && this[d] && this[d].length && this[d].forEach((e => this[l](e.key, e.value))), this[p] = new Map, this[d] = new n, this[r] = 0
                }

                dump() {
                    return this[d].map((e => !f(this, e) && {
                        k: e.key,
                        v: e.value,
                        e: e.now + (e.maxAge || 0)
                    })).toArray().filter((e => e))
                }

                dumpLru() {
                    return this[d]
                }

                set(e, t, i) {
                    if ((i = i || this[c]) && "number" != typeof i) throw new TypeError("maxAge must be a number");
                    const n = i ? Date.now() : 0, a = this[s](t, e);
                    if (this[p].has(e)) {
                        if (a > this[o]) return y(this, this[p].get(e)), !1;
                        const s = this[p].get(e).value;
                        return this[l] && (this[u] || this[l](e, s.value)), s.now = n, s.maxAge = i, s.value = t, this[r] += a - s.length, s.length = a, this.get(e), v(this), !0
                    }
                    const h = new C(e, t, a, n, i);
                    return h.length > this[o] ? (this[l] && this[l](e, t), !1) : (this[r] += h.length, this[d].unshift(h), this[p].set(e, this[d].head), v(this), !0)
                }

                has(e) {
                    if (!this[p].has(e)) return !1;
                    const t = this[p].get(e).value;
                    return !f(this, t)
                }

                get(e) {
                    return m(this, e, !0)
                }

                peek(e) {
                    return m(this, e, !1)
                }

                pop() {
                    const e = this[d].tail;
                    return e ? (y(this, e), e.value) : null
                }

                del(e) {
                    y(this, this[p].get(e))
                }

                load(e) {
                    this.reset();
                    const t = Date.now();
                    for (let i = e.length - 1; i >= 0; i--) {
                        const n = e[i], o = n.e || 0;
                        if (0 === o) this.set(n.k, n.v); else {
                            const e = o - t;
                            e > 0 && this.set(n.k, n.v, e)
                        }
                    }
                }

                prune() {
                    this[p].forEach(((e, t) => m(this, t, !1)))
                }
            }
        }, 9842: (e, t, i) => {
            e.exports = p, p.Minimatch = h;
            var n = function () {
                try {
                    return i(1017)
                } catch (e) {
                }
            }() || {sep: "/"};
            p.sep = n.sep;
            var o = p.GLOBSTAR = h.GLOBSTAR = {}, r = i(1266), s = {
                "!": {open: "(?:(?!(?:", close: "))[^/]*?)"},
                "?": {open: "(?:", close: ")?"},
                "+": {open: "(?:", close: ")+"},
                "*": {open: "(?:", close: ")*"},
                "@": {open: "(?:", close: ")"}
            }, a = "[^/]", c = a + "*?", l = "().*{}+?[]^$\\!".split("").reduce((function (e, t) {
                return e[t] = !0, e
            }), {});
            var u = /\/+/;

            function d(e, t) {
                t = t || {};
                var i = {};
                return Object.keys(e).forEach((function (t) {
                    i[t] = e[t]
                })), Object.keys(t).forEach((function (e) {
                    i[e] = t[e]
                })), i
            }

            function p(e, t, i) {
                return m(t), i || (i = {}), !(!i.nocomment && "#" === t.charAt(0)) && new h(t, i).match(e)
            }

            function h(e, t) {
                if (!(this instanceof h)) return new h(e, t);
                m(e), t || (t = {}), e = e.trim(), t.allowWindowsEscape || "/" === n.sep || (e = e.split(n.sep).join("/")), this.options = t, this.set = [], this.pattern = e, this.regexp = null, this.negate = !1, this.comment = !1, this.empty = !1, this.partial = !!t.partial, this.make()
            }

            function g(e, t) {
                return t || (t = this instanceof h ? this.options : {}), e = void 0 === e ? this.pattern : e, m(e), t.nobrace || !/\{(?:(?!\{).)*\}/.test(e) ? [e] : r(e)
            }

            p.filter = function (e, t) {
                return t = t || {}, function (i, n, o) {
                    return p(i, e, t)
                }
            }, p.defaults = function (e) {
                if (!e || "object" != typeof e || !Object.keys(e).length) return p;
                var t = p, i = function (i, n, o) {
                    return t(i, n, d(e, o))
                };
                return (i.Minimatch = function (i, n) {
                    return new t.Minimatch(i, d(e, n))
                }).defaults = function (i) {
                    return t.defaults(d(e, i)).Minimatch
                }, i.filter = function (i, n) {
                    return t.filter(i, d(e, n))
                }, i.defaults = function (i) {
                    return t.defaults(d(e, i))
                }, i.makeRe = function (i, n) {
                    return t.makeRe(i, d(e, n))
                }, i.braceExpand = function (i, n) {
                    return t.braceExpand(i, d(e, n))
                }, i.match = function (i, n, o) {
                    return t.match(i, n, d(e, o))
                }, i
            }, h.defaults = function (e) {
                return p.defaults(e).Minimatch
            }, h.prototype.debug = function () {
            }, h.prototype.make = function () {
                var e = this.pattern, t = this.options;
                if (!t.nocomment && "#" === e.charAt(0)) return void (this.comment = !0);
                if (!e) return void (this.empty = !0);
                this.parseNegate();
                var i = this.globSet = this.braceExpand();
                t.debug && (this.debug = function () {
                    console.error.apply(console, arguments)
                });
                this.debug(this.pattern, i), i = this.globParts = i.map((function (e) {
                    return e.split(u)
                })), this.debug(this.pattern, i), i = i.map((function (e, t, i) {
                    return e.map(this.parse, this)
                }), this), this.debug(this.pattern, i), i = i.filter((function (e) {
                    return -1 === e.indexOf(!1)
                })), this.debug(this.pattern, i), this.set = i
            }, h.prototype.parseNegate = function () {
                var e = this.pattern, t = !1, i = this.options, n = 0;
                if (i.nonegate) return;
                for (var o = 0, r = e.length; o < r && "!" === e.charAt(o); o++) t = !t, n++;
                n && (this.pattern = e.substr(n));
                this.negate = t
            }, p.braceExpand = function (e, t) {
                return g(e, t)
            }, h.prototype.braceExpand = g;
            var m = function (e) {
                if ("string" != typeof e) throw new TypeError("invalid pattern");
                if (e.length > 65536) throw new TypeError("pattern is too long")
            };
            h.prototype.parse = function (e, t) {
                m(e);
                var i = this.options;
                if ("**" === e) {
                    if (!i.noglobstar) return o;
                    e = "*"
                }
                if ("" === e) return "";
                var n, r = "", u = !!i.nocase, d = !1, p = [], h = [], g = !1, v = -1, y = -1,
                    C = "." === e.charAt(0) ? "" : i.dot ? "(?!(?:^|\\/)\\.{1,2}(?:$|\\/))" : "(?!\\.)", w = this;

                function b() {
                    if (n) {
                        switch (n) {
                            case"*":
                                r += c, u = !0;
                                break;
                            case"?":
                                r += a, u = !0;
                                break;
                            default:
                                r += "\\" + n
                        }
                        w.debug("clearStateChar %j %j", n, r), n = !1
                    }
                }

                for (var k, S = 0, D = e.length; S < D && (k = e.charAt(S)); S++) if (this.debug("%s\t%s %s %j", e, S, r, k), d && l[k]) r += "\\" + k, d = !1; else switch (k) {
                    case"/":
                        return !1;
                    case"\\":
                        b(), d = !0;
                        continue;
                    case"?":
                    case"*":
                    case"+":
                    case"@":
                    case"!":
                        if (this.debug("%s\t%s %s %j <-- stateChar", e, S, r, k), g) {
                            this.debug("  in class"), "!" === k && S === y + 1 && (k = "^"), r += k;
                            continue
                        }
                        w.debug("call clearStateChar %j", n), b(), n = k, i.noext && b();
                        continue;
                    case"(":
                        if (g) {
                            r += "(";
                            continue
                        }
                        if (!n) {
                            r += "\\(";
                            continue
                        }
                        p.push({
                            type: n,
                            start: S - 1,
                            reStart: r.length,
                            open: s[n].open,
                            close: s[n].close
                        }), r += "!" === n ? "(?:(?!(?:" : "(?:", this.debug("plType %j %j", n, r), n = !1;
                        continue;
                    case")":
                        if (g || !p.length) {
                            r += "\\)";
                            continue
                        }
                        b(), u = !0;
                        var P = p.pop();
                        r += P.close, "!" === P.type && h.push(P), P.reEnd = r.length;
                        continue;
                    case"|":
                        if (g || !p.length || d) {
                            r += "\\|", d = !1;
                            continue
                        }
                        b(), r += "|";
                        continue;
                    case"[":
                        if (b(), g) {
                            r += "\\" + k;
                            continue
                        }
                        g = !0, y = S, v = r.length, r += k;
                        continue;
                    case"]":
                        if (S === y + 1 || !g) {
                            r += "\\" + k, d = !1;
                            continue
                        }
                        var T = e.substring(y + 1, S);
                        try {
                            RegExp("[" + T + "]")
                        } catch (e) {
                            var R = this.parse(T, f);
                            r = r.substr(0, v) + "\\[" + R[0] + "\\]", u = u || R[1], g = !1;
                            continue
                        }
                        u = !0, g = !1, r += k;
                        continue;
                    default:
                        b(), d ? d = !1 : !l[k] || "^" === k && g || (r += "\\"), r += k
                }
                g && (T = e.substr(y + 1), R = this.parse(T, f), r = r.substr(0, v) + "\\[" + R[0], u = u || R[1]);
                for (P = p.pop(); P; P = p.pop()) {
                    var _ = r.slice(P.reStart + P.open.length);
                    this.debug("setting tail", r, P), _ = _.replace(/((?:\\{2}){0,64})(\\?)\|/g, (function (e, t, i) {
                        return i || (i = "\\"), t + t + i + "|"
                    })), this.debug("tail=%j\n   %s", _, _, P, r);
                    var x = "*" === P.type ? c : "?" === P.type ? a : "\\" + P.type;
                    u = !0, r = r.slice(0, P.reStart) + x + "\\(" + _
                }
                b(), d && (r += "\\\\");
                var E = !1;
                switch (r.charAt(0)) {
                    case"[":
                    case".":
                    case"(":
                        E = !0
                }
                for (var O = h.length - 1; O > -1; O--) {
                    var j = h[O], M = r.slice(0, j.reStart), I = r.slice(j.reStart, j.reEnd - 8),
                        F = r.slice(j.reEnd - 8, j.reEnd), N = r.slice(j.reEnd);
                    F += N;
                    var q = M.split("(").length - 1, L = N;
                    for (S = 0; S < q; S++) L = L.replace(/\)[+*?]?/, "");
                    var A = "";
                    "" === (N = L) && t !== f && (A = "$"), r = M + I + N + A + F
                }
                "" !== r && u && (r = "(?=.)" + r);
                E && (r = C + r);
                if (t === f) return [r, u];
                if (!u) return function (e) {
                    return e.replace(/\\(.)/g, "$1")
                }(e);
                var $ = i.nocase ? "i" : "";
                try {
                    var U = new RegExp("^" + r + "$", $)
                } catch (e) {
                    return new RegExp("$.")
                }
                return U._glob = e, U._src = r, U
            };
            var f = {};
            p.makeRe = function (e, t) {
                return new h(e, t || {}).makeRe()
            }, h.prototype.makeRe = function () {
                if (this.regexp || !1 === this.regexp) return this.regexp;
                var e = this.set;
                if (!e.length) return this.regexp = !1, this.regexp;
                var t = this.options,
                    i = t.noglobstar ? c : t.dot ? "(?:(?!(?:\\/|^)(?:\\.{1,2})($|\\/)).)*?" : "(?:(?!(?:\\/|^)\\.).)*?",
                    n = t.nocase ? "i" : "", r = e.map((function (e) {
                        return e.map((function (e) {
                            return e === o ? i : "string" == typeof e ? function (e) {
                                return e.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&")
                            }(e) : e._src
                        })).join("\\/")
                    })).join("|");
                r = "^(?:" + r + ")$", this.negate && (r = "^(?!" + r + ").*$");
                try {
                    this.regexp = new RegExp(r, n)
                } catch (e) {
                    this.regexp = !1
                }
                return this.regexp
            }, p.match = function (e, t, i) {
                var n = new h(t, i = i || {});
                return e = e.filter((function (e) {
                    return n.match(e)
                })), n.options.nonull && !e.length && e.push(t), e
            }, h.prototype.match = function (e, t) {
                if (void 0 === t && (t = this.partial), this.debug("match", e, this.pattern), this.comment) return !1;
                if (this.empty) return "" === e;
                if ("/" === e && t) return !0;
                var i = this.options;
                "/" !== n.sep && (e = e.split(n.sep).join("/")), e = e.split(u), this.debug(this.pattern, "split", e);
                var o, r, s = this.set;
                for (this.debug(this.pattern, "set", s), r = e.length - 1; r >= 0 && !(o = e[r]); r--) ;
                for (r = 0; r < s.length; r++) {
                    var a = s[r], c = e;
                    if (i.matchBase && 1 === a.length && (c = [o]), this.matchOne(c, a, t)) return !!i.flipNegate || !this.negate
                }
                return !i.flipNegate && this.negate
            }, h.prototype.matchOne = function (e, t, i) {
                var n = this.options;
                this.debug("matchOne", {this: this, file: e, pattern: t}), this.debug("matchOne", e.length, t.length);
                for (var r = 0, s = 0, a = e.length, c = t.length; r < a && s < c; r++, s++) {
                    this.debug("matchOne loop");
                    var l, u = t[s], d = e[r];
                    if (this.debug(t, u, d), !1 === u) return !1;
                    if (u === o) {
                        this.debug("GLOBSTAR", [t, u, d]);
                        var p = r, h = s + 1;
                        if (h === c) {
                            for (this.debug("** at the end"); r < a; r++) if ("." === e[r] || ".." === e[r] || !n.dot && "." === e[r].charAt(0)) return !1;
                            return !0
                        }
                        for (; p < a;) {
                            var g = e[p];
                            if (this.debug("\nglobstar while", e, p, t, h, g), this.matchOne(e.slice(p), t.slice(h), i)) return this.debug("globstar found match!", p, a, g), !0;
                            if ("." === g || ".." === g || !n.dot && "." === g.charAt(0)) {
                                this.debug("dot detected!", e, p, t, h);
                                break
                            }
                            this.debug("globstar swallow a segment, and continue"), p++
                        }
                        return !(!i || (this.debug("\n>>> no match, partial?", e, p, t, h), p !== a))
                    }
                    if ("string" == typeof u ? (l = d === u, this.debug("string match", u, d, l)) : (l = d.match(u), this.debug("pattern match", u, d, l)), !l) return !1
                }
                if (r === a && s === c) return !0;
                if (r === a) return i;
                if (s === c) return r === a - 1 && "" === e[r];
                throw new Error("wtf?")
            }
        }, 7181: (e, t, i) => {
            const n = Symbol("SemVer ANY");

            class o {
                static get ANY() {
                    return n
                }

                constructor(e, t) {
                    if (t = r(t), e instanceof o) {
                        if (e.loose === !!t.loose) return e;
                        e = e.value
                    }
                    e = e.trim().split(/\s+/).join(" "), l("comparator", e, t), this.options = t, this.loose = !!t.loose, this.parse(e), this.semver === n ? this.value = "" : this.value = this.operator + this.semver.version, l("comp", this)
                }

                parse(e) {
                    const t = this.options.loose ? s[a.COMPARATORLOOSE] : s[a.COMPARATOR], i = e.match(t);
                    if (!i) throw new TypeError(`Invalid comparator: ${e}`);
                    this.operator = void 0 !== i[1] ? i[1] : "", "=" === this.operator && (this.operator = ""), i[2] ? this.semver = new u(i[2], this.options.loose) : this.semver = n
                }

                toString() {
                    return this.value
                }

                test(e) {
                    if (l("Comparator.test", e, this.options.loose), this.semver === n || e === n) return !0;
                    if ("string" == typeof e) try {
                        e = new u(e, this.options)
                    } catch (e) {
                        return !1
                    }
                    return c(e, this.operator, this.semver, this.options)
                }

                intersects(e, t) {
                    if (!(e instanceof o)) throw new TypeError("a Comparator is required");
                    return "" === this.operator ? "" === this.value || new d(e.value, t).test(this.value) : "" === e.operator ? "" === e.value || new d(this.value, t).test(e.semver) : (!(t = r(t)).includePrerelease || "<0.0.0-0" !== this.value && "<0.0.0-0" !== e.value) && (!(!t.includePrerelease && (this.value.startsWith("<0.0.0") || e.value.startsWith("<0.0.0"))) && (!(!this.operator.startsWith(">") || !e.operator.startsWith(">")) || (!(!this.operator.startsWith("<") || !e.operator.startsWith("<")) || (!(this.semver.version !== e.semver.version || !this.operator.includes("=") || !e.operator.includes("=")) || (!!(c(this.semver, "<", e.semver, t) && this.operator.startsWith(">") && e.operator.startsWith("<")) || !!(c(this.semver, ">", e.semver, t) && this.operator.startsWith("<") && e.operator.startsWith(">")))))))
                }
            }

            e.exports = o;
            const r = i(2233), {safeRe: s, t: a} = i(4873), c = i(1918), l = i(9018), u = i(9827), d = i(1554)
        }, 1554: (e, t, i) => {
            class n {
                constructor(e, t) {
                    if (t = r(t), e instanceof n) return e.loose === !!t.loose && e.includePrerelease === !!t.includePrerelease ? e : new n(e.raw, t);
                    if (e instanceof s) return this.raw = e.value, this.set = [[e]], this.format(), this;
                    if (this.options = t, this.loose = !!t.loose, this.includePrerelease = !!t.includePrerelease, this.raw = e.trim().split(/\s+/).join(" "), this.set = this.raw.split("||").map((e => this.parseRange(e.trim()))).filter((e => e.length)), !this.set.length) throw new TypeError(`Invalid SemVer Range: ${this.raw}`);
                    if (this.set.length > 1) {
                        const e = this.set[0];
                        if (this.set = this.set.filter((e => !f(e[0]))), 0 === this.set.length) this.set = [e]; else if (this.set.length > 1) for (const e of this.set) if (1 === e.length && v(e[0])) {
                            this.set = [e];
                            break
                        }
                    }
                    this.format()
                }

                format() {
                    return this.range = this.set.map((e => e.join(" ").trim())).join("||").trim(), this.range
                }

                toString() {
                    return this.range
                }

                parseRange(e) {
                    const t = ((this.options.includePrerelease && g) | (this.options.loose && m)) + ":" + e,
                        i = o.get(t);
                    if (i) return i;
                    const n = this.options.loose, r = n ? l[u.HYPHENRANGELOOSE] : l[u.HYPHENRANGE];
                    e = e.replace(r, x(this.options.includePrerelease)), a("hyphen replace", e), e = e.replace(l[u.COMPARATORTRIM], d), a("comparator trim", e), e = e.replace(l[u.TILDETRIM], p), a("tilde trim", e), e = e.replace(l[u.CARETTRIM], h), a("caret trim", e);
                    let c = e.split(" ").map((e => C(e, this.options))).join(" ").split(/\s+/).map((e => _(e, this.options)));
                    n && (c = c.filter((e => (a("loose invalid filter", e, this.options), !!e.match(l[u.COMPARATORLOOSE]))))), a("range list", c);
                    const v = new Map, y = c.map((e => new s(e, this.options)));
                    for (const e of y) {
                        if (f(e)) return [e];
                        v.set(e.value, e)
                    }
                    v.size > 1 && v.has("") && v.delete("");
                    const w = [...v.values()];
                    return o.set(t, w), w
                }

                intersects(e, t) {
                    if (!(e instanceof n)) throw new TypeError("a Range is required");
                    return this.set.some((i => y(i, t) && e.set.some((e => y(e, t) && i.every((i => e.every((e => i.intersects(e, t)))))))))
                }

                test(e) {
                    if (!e) return !1;
                    if ("string" == typeof e) try {
                        e = new c(e, this.options)
                    } catch (e) {
                        return !1
                    }
                    for (let t = 0; t < this.set.length; t++) if (E(this.set[t], e, this.options)) return !0;
                    return !1
                }
            }

            e.exports = n;
            const o = new (i(4830))({max: 1e3}), r = i(2233), s = i(7181), a = i(9018), c = i(9827), {
                    safeRe: l,
                    t: u,
                    comparatorTrimReplace: d,
                    tildeTrimReplace: p,
                    caretTrimReplace: h
                } = i(4873), {FLAG_INCLUDE_PRERELEASE: g, FLAG_LOOSE: m} = i(6757), f = e => "<0.0.0-0" === e.value,
                v = e => "" === e.value, y = (e, t) => {
                    let i = !0;
                    const n = e.slice();
                    let o = n.pop();
                    for (; i && n.length;) i = n.every((e => o.intersects(e, t))), o = n.pop();
                    return i
                },
                C = (e, t) => (a("comp", e, t), e = S(e, t), a("caret", e), e = b(e, t), a("tildes", e), e = P(e, t), a("xrange", e), e = R(e, t), a("stars", e), e),
                w = e => !e || "x" === e.toLowerCase() || "*" === e,
                b = (e, t) => e.trim().split(/\s+/).map((e => k(e, t))).join(" "), k = (e, t) => {
                    const i = t.loose ? l[u.TILDELOOSE] : l[u.TILDE];
                    return e.replace(i, ((t, i, n, o, r) => {
                        let s;
                        return a("tilde", e, t, i, n, o, r), w(i) ? s = "" : w(n) ? s = `>=${i}.0.0 <${+i + 1}.0.0-0` : w(o) ? s = `>=${i}.${n}.0 <${i}.${+n + 1}.0-0` : r ? (a("replaceTilde pr", r), s = `>=${i}.${n}.${o}-${r} <${i}.${+n + 1}.0-0`) : s = `>=${i}.${n}.${o} <${i}.${+n + 1}.0-0`, a("tilde return", s), s
                    }))
                }, S = (e, t) => e.trim().split(/\s+/).map((e => D(e, t))).join(" "), D = (e, t) => {
                    a("caret", e, t);
                    const i = t.loose ? l[u.CARETLOOSE] : l[u.CARET], n = t.includePrerelease ? "-0" : "";
                    return e.replace(i, ((t, i, o, r, s) => {
                        let c;
                        return a("caret", e, t, i, o, r, s), w(i) ? c = "" : w(o) ? c = `>=${i}.0.0${n} <${+i + 1}.0.0-0` : w(r) ? c = "0" === i ? `>=${i}.${o}.0${n} <${i}.${+o + 1}.0-0` : `>=${i}.${o}.0${n} <${+i + 1}.0.0-0` : s ? (a("replaceCaret pr", s), c = "0" === i ? "0" === o ? `>=${i}.${o}.${r}-${s} <${i}.${o}.${+r + 1}-0` : `>=${i}.${o}.${r}-${s} <${i}.${+o + 1}.0-0` : `>=${i}.${o}.${r}-${s} <${+i + 1}.0.0-0`) : (a("no pr"), c = "0" === i ? "0" === o ? `>=${i}.${o}.${r}${n} <${i}.${o}.${+r + 1}-0` : `>=${i}.${o}.${r}${n} <${i}.${+o + 1}.0-0` : `>=${i}.${o}.${r} <${+i + 1}.0.0-0`), a("caret return", c), c
                    }))
                }, P = (e, t) => (a("replaceXRanges", e, t), e.split(/\s+/).map((e => T(e, t))).join(" ")), T = (e, t) => {
                    e = e.trim();
                    const i = t.loose ? l[u.XRANGELOOSE] : l[u.XRANGE];
                    return e.replace(i, ((i, n, o, r, s, c) => {
                        a("xRange", e, i, n, o, r, s, c);
                        const l = w(o), u = l || w(r), d = u || w(s), p = d;
                        return "=" === n && p && (n = ""), c = t.includePrerelease ? "-0" : "", l ? i = ">" === n || "<" === n ? "<0.0.0-0" : "*" : n && p ? (u && (r = 0), s = 0, ">" === n ? (n = ">=", u ? (o = +o + 1, r = 0, s = 0) : (r = +r + 1, s = 0)) : "<=" === n && (n = "<", u ? o = +o + 1 : r = +r + 1), "<" === n && (c = "-0"), i = `${n + o}.${r}.${s}${c}`) : u ? i = `>=${o}.0.0${c} <${+o + 1}.0.0-0` : d && (i = `>=${o}.${r}.0${c} <${o}.${+r + 1}.0-0`), a("xRange return", i), i
                    }))
                }, R = (e, t) => (a("replaceStars", e, t), e.trim().replace(l[u.STAR], "")),
                _ = (e, t) => (a("replaceGTE0", e, t), e.trim().replace(l[t.includePrerelease ? u.GTE0PRE : u.GTE0], "")),
                x = e => (t, i, n, o, r, s, a, c, l, u, d, p, h) => `${i = w(n) ? "" : w(o) ? `>=${n}.0.0${e ? "-0" : ""}` : w(r) ? `>=${n}.${o}.0${e ? "-0" : ""}` : s ? `>=${i}` : `>=${i}${e ? "-0" : ""}`} ${c = w(l) ? "" : w(u) ? `<${+l + 1}.0.0-0` : w(d) ? `<${l}.${+u + 1}.0-0` : p ? `<=${l}.${u}.${d}-${p}` : e ? `<${l}.${u}.${+d + 1}-0` : `<=${c}`}`.trim(),
                E = (e, t, i) => {
                    for (let i = 0; i < e.length; i++) if (!e[i].test(t)) return !1;
                    if (t.prerelease.length && !i.includePrerelease) {
                        for (let i = 0; i < e.length; i++) if (a(e[i].semver), e[i].semver !== s.ANY && e[i].semver.prerelease.length > 0) {
                            const n = e[i].semver;
                            if (n.major === t.major && n.minor === t.minor && n.patch === t.patch) return !0
                        }
                        return !1
                    }
                    return !0
                }
        }, 9827: (e, t, i) => {
            const n = i(9018), {MAX_LENGTH: o, MAX_SAFE_INTEGER: r} = i(6757), {safeRe: s, t: a} = i(4873),
                c = i(2233), {compareIdentifiers: l} = i(4954);

            class u {
                constructor(e, t) {
                    if (t = c(t), e instanceof u) {
                        if (e.loose === !!t.loose && e.includePrerelease === !!t.includePrerelease) return e;
                        e = e.version
                    } else if ("string" != typeof e) throw new TypeError(`Invalid version. Must be a string. Got type "${typeof e}".`);
                    if (e.length > o) throw new TypeError(`version is longer than ${o} characters`);
                    n("SemVer", e, t), this.options = t, this.loose = !!t.loose, this.includePrerelease = !!t.includePrerelease;
                    const i = e.trim().match(t.loose ? s[a.LOOSE] : s[a.FULL]);
                    if (!i) throw new TypeError(`Invalid Version: ${e}`);
                    if (this.raw = e, this.major = +i[1], this.minor = +i[2], this.patch = +i[3], this.major > r || this.major < 0) throw new TypeError("Invalid major version");
                    if (this.minor > r || this.minor < 0) throw new TypeError("Invalid minor version");
                    if (this.patch > r || this.patch < 0) throw new TypeError("Invalid patch version");
                    i[4] ? this.prerelease = i[4].split(".").map((e => {
                        if (/^[0-9]+$/.test(e)) {
                            const t = +e;
                            if (t >= 0 && t < r) return t
                        }
                        return e
                    })) : this.prerelease = [], this.build = i[5] ? i[5].split(".") : [], this.format()
                }

                format() {
                    return this.version = `${this.major}.${this.minor}.${this.patch}`, this.prerelease.length && (this.version += `-${this.prerelease.join(".")}`), this.version
                }

                toString() {
                    return this.version
                }

                compare(e) {
                    if (n("SemVer.compare", this.version, this.options, e), !(e instanceof u)) {
                        if ("string" == typeof e && e === this.version) return 0;
                        e = new u(e, this.options)
                    }
                    return e.version === this.version ? 0 : this.compareMain(e) || this.comparePre(e)
                }

                compareMain(e) {
                    return e instanceof u || (e = new u(e, this.options)), l(this.major, e.major) || l(this.minor, e.minor) || l(this.patch, e.patch)
                }

                comparePre(e) {
                    if (e instanceof u || (e = new u(e, this.options)), this.prerelease.length && !e.prerelease.length) return -1;
                    if (!this.prerelease.length && e.prerelease.length) return 1;
                    if (!this.prerelease.length && !e.prerelease.length) return 0;
                    let t = 0;
                    do {
                        const i = this.prerelease[t], o = e.prerelease[t];
                        if (n("prerelease compare", t, i, o), void 0 === i && void 0 === o) return 0;
                        if (void 0 === o) return 1;
                        if (void 0 === i) return -1;
                        if (i !== o) return l(i, o)
                    } while (++t)
                }

                compareBuild(e) {
                    e instanceof u || (e = new u(e, this.options));
                    let t = 0;
                    do {
                        const i = this.build[t], o = e.build[t];
                        if (n("prerelease compare", t, i, o), void 0 === i && void 0 === o) return 0;
                        if (void 0 === o) return 1;
                        if (void 0 === i) return -1;
                        if (i !== o) return l(i, o)
                    } while (++t)
                }

                inc(e, t, i) {
                    switch (e) {
                        case"premajor":
                            this.prerelease.length = 0, this.patch = 0, this.minor = 0, this.major++, this.inc("pre", t, i);
                            break;
                        case"preminor":
                            this.prerelease.length = 0, this.patch = 0, this.minor++, this.inc("pre", t, i);
                            break;
                        case"prepatch":
                            this.prerelease.length = 0, this.inc("patch", t, i), this.inc("pre", t, i);
                            break;
                        case"prerelease":
                            0 === this.prerelease.length && this.inc("patch", t, i), this.inc("pre", t, i);
                            break;
                        case"major":
                            0 === this.minor && 0 === this.patch && 0 !== this.prerelease.length || this.major++, this.minor = 0, this.patch = 0, this.prerelease = [];
                            break;
                        case"minor":
                            0 === this.patch && 0 !== this.prerelease.length || this.minor++, this.patch = 0, this.prerelease = [];
                            break;
                        case"patch":
                            0 === this.prerelease.length && this.patch++, this.prerelease = [];
                            break;
                        case"pre": {
                            const e = Number(i) ? 1 : 0;
                            if (!t && !1 === i) throw new Error("invalid increment argument: identifier is empty");
                            if (0 === this.prerelease.length) this.prerelease = [e]; else {
                                let n = this.prerelease.length;
                                for (; --n >= 0;) "number" == typeof this.prerelease[n] && (this.prerelease[n]++, n = -2);
                                if (-1 === n) {
                                    if (t === this.prerelease.join(".") && !1 === i) throw new Error("invalid increment argument: identifier already exists");
                                    this.prerelease.push(e)
                                }
                            }
                            if (t) {
                                let n = [t, e];
                                !1 === i && (n = [t]), 0 === l(this.prerelease[0], t) ? isNaN(this.prerelease[1]) && (this.prerelease = n) : this.prerelease = n
                            }
                            break
                        }
                        default:
                            throw new Error(`invalid increment argument: ${e}`)
                    }
                    return this.raw = this.format(), this.build.length && (this.raw += `+${this.build.join(".")}`), this
                }
            }

            e.exports = u
        }, 1918: (e, t, i) => {
            const n = i(9306), o = i(2812), r = i(4926), s = i(6017), a = i(4198), c = i(8661);
            e.exports = (e, t, i, l) => {
                switch (t) {
                    case"===":
                        return "object" == typeof e && (e = e.version), "object" == typeof i && (i = i.version), e === i;
                    case"!==":
                        return "object" == typeof e && (e = e.version), "object" == typeof i && (i = i.version), e !== i;
                    case"":
                    case"=":
                    case"==":
                        return n(e, i, l);
                    case"!=":
                        return o(e, i, l);
                    case">":
                        return r(e, i, l);
                    case">=":
                        return s(e, i, l);
                    case"<":
                        return a(e, i, l);
                    case"<=":
                        return c(e, i, l);
                    default:
                        throw new TypeError(`Invalid operator: ${t}`)
                }
            }
        }, 8818: (e, t, i) => {
            const n = i(9827);
            e.exports = (e, t, i) => new n(e, i).compare(new n(t, i))
        }, 9306: (e, t, i) => {
            const n = i(8818);
            e.exports = (e, t, i) => 0 === n(e, t, i)
        }, 4926: (e, t, i) => {
            const n = i(8818);
            e.exports = (e, t, i) => n(e, t, i) > 0
        }, 6017: (e, t, i) => {
            const n = i(8818);
            e.exports = (e, t, i) => n(e, t, i) >= 0
        }, 4198: (e, t, i) => {
            const n = i(8818);
            e.exports = (e, t, i) => n(e, t, i) < 0
        }, 8661: (e, t, i) => {
            const n = i(8818);
            e.exports = (e, t, i) => n(e, t, i) <= 0
        }, 2812: (e, t, i) => {
            const n = i(8818);
            e.exports = (e, t, i) => 0 !== n(e, t, i)
        }, 6652: (e, t, i) => {
            const n = i(9827);
            e.exports = (e, t, i = !1) => {
                if (e instanceof n) return e;
                try {
                    return new n(e, t)
                } catch (e) {
                    if (!i) return null;
                    throw e
                }
            }
        }, 4866: (e, t, i) => {
            const n = i(1554);
            e.exports = (e, t, i) => {
                try {
                    t = new n(t, i)
                } catch (e) {
                    return !1
                }
                return t.test(e)
            }
        }, 6757: e => {
            const t = Number.MAX_SAFE_INTEGER || 9007199254740991;
            e.exports = {
                MAX_LENGTH: 256,
                MAX_SAFE_COMPONENT_LENGTH: 16,
                MAX_SAFE_BUILD_LENGTH: 250,
                MAX_SAFE_INTEGER: t,
                RELEASE_TYPES: ["major", "premajor", "minor", "preminor", "patch", "prepatch", "prerelease"],
                SEMVER_SPEC_VERSION: "2.0.0",
                FLAG_INCLUDE_PRERELEASE: 1,
                FLAG_LOOSE: 2
            }
        }, 9018: e => {
            const t = "object" == typeof process && process.env && process.env.NODE_DEBUG && /\bsemver\b/i.test(process.env.NODE_DEBUG) ? (...e) => console.error("SEMVER", ...e) : () => {
            };
            e.exports = t
        }, 4954: e => {
            const t = /^[0-9]+$/, i = (e, i) => {
                const n = t.test(e), o = t.test(i);
                return n && o && (e = +e, i = +i), e === i ? 0 : n && !o ? -1 : o && !n ? 1 : e < i ? -1 : 1
            };
            e.exports = {compareIdentifiers: i, rcompareIdentifiers: (e, t) => i(t, e)}
        }, 2233: e => {
            const t = Object.freeze({loose: !0}), i = Object.freeze({});
            e.exports = e => e ? "object" != typeof e ? t : e : i
        }, 4873: (e, t, i) => {
            const {MAX_SAFE_COMPONENT_LENGTH: n, MAX_SAFE_BUILD_LENGTH: o, MAX_LENGTH: r} = i(6757), s = i(9018),
                a = (t = e.exports = {}).re = [], c = t.safeRe = [], l = t.src = [], u = t.t = {};
            let d = 0;
            const p = "[a-zA-Z0-9-]", h = [["\\s", 1], ["\\d", r], [p, o]], g = (e, t, i) => {
                const n = (e => {
                    for (const [t, i] of h) e = e.split(`${t}*`).join(`${t}{0,${i}}`).split(`${t}+`).join(`${t}{1,${i}}`);
                    return e
                })(t), o = d++;
                s(e, o, t), u[e] = o, l[o] = t, a[o] = new RegExp(t, i ? "g" : void 0), c[o] = new RegExp(n, i ? "g" : void 0)
            };
            g("NUMERICIDENTIFIER", "0|[1-9]\\d*"), g("NUMERICIDENTIFIERLOOSE", "\\d+"), g("NONNUMERICIDENTIFIER", `\\d*[a-zA-Z-]${p}*`), g("MAINVERSION", `(${l[u.NUMERICIDENTIFIER]})\\.(${l[u.NUMERICIDENTIFIER]})\\.(${l[u.NUMERICIDENTIFIER]})`), g("MAINVERSIONLOOSE", `(${l[u.NUMERICIDENTIFIERLOOSE]})\\.(${l[u.NUMERICIDENTIFIERLOOSE]})\\.(${l[u.NUMERICIDENTIFIERLOOSE]})`), g("PRERELEASEIDENTIFIER", `(?:${l[u.NUMERICIDENTIFIER]}|${l[u.NONNUMERICIDENTIFIER]})`), g("PRERELEASEIDENTIFIERLOOSE", `(?:${l[u.NUMERICIDENTIFIERLOOSE]}|${l[u.NONNUMERICIDENTIFIER]})`), g("PRERELEASE", `(?:-(${l[u.PRERELEASEIDENTIFIER]}(?:\\.${l[u.PRERELEASEIDENTIFIER]})*))`), g("PRERELEASELOOSE", `(?:-?(${l[u.PRERELEASEIDENTIFIERLOOSE]}(?:\\.${l[u.PRERELEASEIDENTIFIERLOOSE]})*))`), g("BUILDIDENTIFIER", `${p}+`), g("BUILD", `(?:\\+(${l[u.BUILDIDENTIFIER]}(?:\\.${l[u.BUILDIDENTIFIER]})*))`), g("FULLPLAIN", `v?${l[u.MAINVERSION]}${l[u.PRERELEASE]}?${l[u.BUILD]}?`), g("FULL", `^${l[u.FULLPLAIN]}$`), g("LOOSEPLAIN", `[v=\\s]*${l[u.MAINVERSIONLOOSE]}${l[u.PRERELEASELOOSE]}?${l[u.BUILD]}?`), g("LOOSE", `^${l[u.LOOSEPLAIN]}$`), g("GTLT", "((?:<|>)?=?)"), g("XRANGEIDENTIFIERLOOSE", `${l[u.NUMERICIDENTIFIERLOOSE]}|x|X|\\*`), g("XRANGEIDENTIFIER", `${l[u.NUMERICIDENTIFIER]}|x|X|\\*`), g("XRANGEPLAIN", `[v=\\s]*(${l[u.XRANGEIDENTIFIER]})(?:\\.(${l[u.XRANGEIDENTIFIER]})(?:\\.(${l[u.XRANGEIDENTIFIER]})(?:${l[u.PRERELEASE]})?${l[u.BUILD]}?)?)?`), g("XRANGEPLAINLOOSE", `[v=\\s]*(${l[u.XRANGEIDENTIFIERLOOSE]})(?:\\.(${l[u.XRANGEIDENTIFIERLOOSE]})(?:\\.(${l[u.XRANGEIDENTIFIERLOOSE]})(?:${l[u.PRERELEASELOOSE]})?${l[u.BUILD]}?)?)?`), g("XRANGE", `^${l[u.GTLT]}\\s*${l[u.XRANGEPLAIN]}$`), g("XRANGELOOSE", `^${l[u.GTLT]}\\s*${l[u.XRANGEPLAINLOOSE]}$`), g("COERCE", `(^|[^\\d])(\\d{1,${n}})(?:\\.(\\d{1,${n}}))?(?:\\.(\\d{1,${n}}))?(?:$|[^\\d])`), g("COERCERTL", l[u.COERCE], !0), g("LONETILDE", "(?:~>?)"), g("TILDETRIM", `(\\s*)${l[u.LONETILDE]}\\s+`, !0), t.tildeTrimReplace = "$1~", g("TILDE", `^${l[u.LONETILDE]}${l[u.XRANGEPLAIN]}$`), g("TILDELOOSE", `^${l[u.LONETILDE]}${l[u.XRANGEPLAINLOOSE]}$`), g("LONECARET", "(?:\\^)"), g("CARETTRIM", `(\\s*)${l[u.LONECARET]}\\s+`, !0), t.caretTrimReplace = "$1^", g("CARET", `^${l[u.LONECARET]}${l[u.XRANGEPLAIN]}$`), g("CARETLOOSE", `^${l[u.LONECARET]}${l[u.XRANGEPLAINLOOSE]}$`), g("COMPARATORLOOSE", `^${l[u.GTLT]}\\s*(${l[u.LOOSEPLAIN]})$|^$`), g("COMPARATOR", `^${l[u.GTLT]}\\s*(${l[u.FULLPLAIN]})$|^$`), g("COMPARATORTRIM", `(\\s*)${l[u.GTLT]}\\s*(${l[u.LOOSEPLAIN]}|${l[u.XRANGEPLAIN]})`, !0), t.comparatorTrimReplace = "$1$2$3", g("HYPHENRANGE", `^\\s*(${l[u.XRANGEPLAIN]})\\s+-\\s+(${l[u.XRANGEPLAIN]})\\s*$`), g("HYPHENRANGELOOSE", `^\\s*(${l[u.XRANGEPLAINLOOSE]})\\s+-\\s+(${l[u.XRANGEPLAINLOOSE]})\\s*$`), g("STAR", "(<|>)?=?\\s*\\*"), g("GTE0", "^\\s*>=\\s*0\\.0\\.0\\s*$"), g("GTE0PRE", "^\\s*>=\\s*0\\.0\\.0-0\\s*$")
        }, 4539: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.TraceFormat = t.TraceValues = t.Trace = t.ProgressType = t.ProgressToken = t.createMessageConnection = t.NullLogger = t.ConnectionOptions = t.ConnectionStrategy = t.WriteableStreamMessageWriter = t.AbstractMessageWriter = t.MessageWriter = t.ReadableStreamMessageReader = t.AbstractMessageReader = t.MessageReader = t.CancellationToken = t.CancellationTokenSource = t.Emitter = t.Event = t.Disposable = t.LRUCache = t.Touch = t.LinkedMap = t.ParameterStructures = t.NotificationType9 = t.NotificationType8 = t.NotificationType7 = t.NotificationType6 = t.NotificationType5 = t.NotificationType4 = t.NotificationType3 = t.NotificationType2 = t.NotificationType1 = t.NotificationType0 = t.NotificationType = t.ErrorCodes = t.ResponseError = t.RequestType9 = t.RequestType8 = t.RequestType7 = t.RequestType6 = t.RequestType5 = t.RequestType4 = t.RequestType3 = t.RequestType2 = t.RequestType1 = t.RequestType0 = t.RequestType = t.Message = t.RAL = void 0, t.CancellationStrategy = t.CancellationSenderStrategy = t.CancellationReceiverStrategy = t.ConnectionError = t.ConnectionErrors = t.LogTraceNotification = t.SetTraceNotification = void 0;
            const n = i(7318);
            Object.defineProperty(t, "Message", {
                enumerable: !0, get: function () {
                    return n.Message
                }
            }), Object.defineProperty(t, "RequestType", {
                enumerable: !0, get: function () {
                    return n.RequestType
                }
            }), Object.defineProperty(t, "RequestType0", {
                enumerable: !0, get: function () {
                    return n.RequestType0
                }
            }), Object.defineProperty(t, "RequestType1", {
                enumerable: !0, get: function () {
                    return n.RequestType1
                }
            }), Object.defineProperty(t, "RequestType2", {
                enumerable: !0, get: function () {
                    return n.RequestType2
                }
            }), Object.defineProperty(t, "RequestType3", {
                enumerable: !0, get: function () {
                    return n.RequestType3
                }
            }), Object.defineProperty(t, "RequestType4", {
                enumerable: !0, get: function () {
                    return n.RequestType4
                }
            }), Object.defineProperty(t, "RequestType5", {
                enumerable: !0, get: function () {
                    return n.RequestType5
                }
            }), Object.defineProperty(t, "RequestType6", {
                enumerable: !0, get: function () {
                    return n.RequestType6
                }
            }), Object.defineProperty(t, "RequestType7", {
                enumerable: !0, get: function () {
                    return n.RequestType7
                }
            }), Object.defineProperty(t, "RequestType8", {
                enumerable: !0, get: function () {
                    return n.RequestType8
                }
            }), Object.defineProperty(t, "RequestType9", {
                enumerable: !0, get: function () {
                    return n.RequestType9
                }
            }), Object.defineProperty(t, "ResponseError", {
                enumerable: !0, get: function () {
                    return n.ResponseError
                }
            }), Object.defineProperty(t, "ErrorCodes", {
                enumerable: !0, get: function () {
                    return n.ErrorCodes
                }
            }), Object.defineProperty(t, "NotificationType", {
                enumerable: !0, get: function () {
                    return n.NotificationType
                }
            }), Object.defineProperty(t, "NotificationType0", {
                enumerable: !0, get: function () {
                    return n.NotificationType0
                }
            }), Object.defineProperty(t, "NotificationType1", {
                enumerable: !0, get: function () {
                    return n.NotificationType1
                }
            }), Object.defineProperty(t, "NotificationType2", {
                enumerable: !0, get: function () {
                    return n.NotificationType2
                }
            }), Object.defineProperty(t, "NotificationType3", {
                enumerable: !0, get: function () {
                    return n.NotificationType3
                }
            }), Object.defineProperty(t, "NotificationType4", {
                enumerable: !0, get: function () {
                    return n.NotificationType4
                }
            }), Object.defineProperty(t, "NotificationType5", {
                enumerable: !0, get: function () {
                    return n.NotificationType5
                }
            }), Object.defineProperty(t, "NotificationType6", {
                enumerable: !0, get: function () {
                    return n.NotificationType6
                }
            }), Object.defineProperty(t, "NotificationType7", {
                enumerable: !0, get: function () {
                    return n.NotificationType7
                }
            }), Object.defineProperty(t, "NotificationType8", {
                enumerable: !0, get: function () {
                    return n.NotificationType8
                }
            }), Object.defineProperty(t, "NotificationType9", {
                enumerable: !0, get: function () {
                    return n.NotificationType9
                }
            }), Object.defineProperty(t, "ParameterStructures", {
                enumerable: !0, get: function () {
                    return n.ParameterStructures
                }
            });
            const o = i(8364);
            Object.defineProperty(t, "LinkedMap", {
                enumerable: !0, get: function () {
                    return o.LinkedMap
                }
            }), Object.defineProperty(t, "LRUCache", {
                enumerable: !0, get: function () {
                    return o.LRUCache
                }
            }), Object.defineProperty(t, "Touch", {
                enumerable: !0, get: function () {
                    return o.Touch
                }
            });
            const r = i(737);
            Object.defineProperty(t, "Disposable", {
                enumerable: !0, get: function () {
                    return r.Disposable
                }
            });
            const s = i(9225);
            Object.defineProperty(t, "Event", {
                enumerable: !0, get: function () {
                    return s.Event
                }
            }), Object.defineProperty(t, "Emitter", {
                enumerable: !0, get: function () {
                    return s.Emitter
                }
            });
            const a = i(9910);
            Object.defineProperty(t, "CancellationTokenSource", {
                enumerable: !0, get: function () {
                    return a.CancellationTokenSource
                }
            }), Object.defineProperty(t, "CancellationToken", {
                enumerable: !0, get: function () {
                    return a.CancellationToken
                }
            });
            const c = i(2514);
            Object.defineProperty(t, "MessageReader", {
                enumerable: !0, get: function () {
                    return c.MessageReader
                }
            }), Object.defineProperty(t, "AbstractMessageReader", {
                enumerable: !0, get: function () {
                    return c.AbstractMessageReader
                }
            }), Object.defineProperty(t, "ReadableStreamMessageReader", {
                enumerable: !0, get: function () {
                    return c.ReadableStreamMessageReader
                }
            });
            const l = i(486);
            Object.defineProperty(t, "MessageWriter", {
                enumerable: !0, get: function () {
                    return l.MessageWriter
                }
            }), Object.defineProperty(t, "AbstractMessageWriter", {
                enumerable: !0, get: function () {
                    return l.AbstractMessageWriter
                }
            }), Object.defineProperty(t, "WriteableStreamMessageWriter", {
                enumerable: !0, get: function () {
                    return l.WriteableStreamMessageWriter
                }
            });
            const u = i(1585);
            Object.defineProperty(t, "ConnectionStrategy", {
                enumerable: !0, get: function () {
                    return u.ConnectionStrategy
                }
            }), Object.defineProperty(t, "ConnectionOptions", {
                enumerable: !0, get: function () {
                    return u.ConnectionOptions
                }
            }), Object.defineProperty(t, "NullLogger", {
                enumerable: !0, get: function () {
                    return u.NullLogger
                }
            }), Object.defineProperty(t, "createMessageConnection", {
                enumerable: !0, get: function () {
                    return u.createMessageConnection
                }
            }), Object.defineProperty(t, "ProgressToken", {
                enumerable: !0, get: function () {
                    return u.ProgressToken
                }
            }), Object.defineProperty(t, "ProgressType", {
                enumerable: !0, get: function () {
                    return u.ProgressType
                }
            }), Object.defineProperty(t, "Trace", {
                enumerable: !0, get: function () {
                    return u.Trace
                }
            }), Object.defineProperty(t, "TraceValues", {
                enumerable: !0, get: function () {
                    return u.TraceValues
                }
            }), Object.defineProperty(t, "TraceFormat", {
                enumerable: !0, get: function () {
                    return u.TraceFormat
                }
            }), Object.defineProperty(t, "SetTraceNotification", {
                enumerable: !0, get: function () {
                    return u.SetTraceNotification
                }
            }), Object.defineProperty(t, "LogTraceNotification", {
                enumerable: !0, get: function () {
                    return u.LogTraceNotification
                }
            }), Object.defineProperty(t, "ConnectionErrors", {
                enumerable: !0, get: function () {
                    return u.ConnectionErrors
                }
            }), Object.defineProperty(t, "ConnectionError", {
                enumerable: !0, get: function () {
                    return u.ConnectionError
                }
            }), Object.defineProperty(t, "CancellationReceiverStrategy", {
                enumerable: !0, get: function () {
                    return u.CancellationReceiverStrategy
                }
            }), Object.defineProperty(t, "CancellationSenderStrategy", {
                enumerable: !0, get: function () {
                    return u.CancellationSenderStrategy
                }
            }), Object.defineProperty(t, "CancellationStrategy", {
                enumerable: !0, get: function () {
                    return u.CancellationStrategy
                }
            });
            const d = i(2182);
            t.RAL = d.default
        }, 9910: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.CancellationTokenSource = t.CancellationToken = void 0;
            const n = i(2182), o = i(5818), r = i(9225);
            var s;
            !function (e) {
                e.None = Object.freeze({
                    isCancellationRequested: !1,
                    onCancellationRequested: r.Event.None
                }), e.Cancelled = Object.freeze({
                    isCancellationRequested: !0,
                    onCancellationRequested: r.Event.None
                }), e.is = function (t) {
                    const i = t;
                    return i && (i === e.None || i === e.Cancelled || o.boolean(i.isCancellationRequested) && !!i.onCancellationRequested)
                }
            }(s = t.CancellationToken || (t.CancellationToken = {}));
            const a = Object.freeze((function (e, t) {
                const i = (0, n.default)().timer.setTimeout(e.bind(t), 0);
                return {
                    dispose() {
                        i.dispose()
                    }
                }
            }));

            class c {
                constructor() {
                    this._isCancelled = !1
                }

                cancel() {
                    this._isCancelled || (this._isCancelled = !0, this._emitter && (this._emitter.fire(void 0), this.dispose()))
                }

                get isCancellationRequested() {
                    return this._isCancelled
                }

                get onCancellationRequested() {
                    return this._isCancelled ? a : (this._emitter || (this._emitter = new r.Emitter), this._emitter.event)
                }

                dispose() {
                    this._emitter && (this._emitter.dispose(), this._emitter = void 0)
                }
            }

            t.CancellationTokenSource = class {
                get token() {
                    return this._token || (this._token = new c), this._token
                }

                cancel() {
                    this._token ? this._token.cancel() : this._token = s.Cancelled
                }

                dispose() {
                    this._token ? this._token instanceof c && this._token.dispose() : this._token = s.None
                }
            }
        }, 1585: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.createMessageConnection = t.ConnectionOptions = t.CancellationStrategy = t.CancellationSenderStrategy = t.CancellationReceiverStrategy = t.ConnectionStrategy = t.ConnectionError = t.ConnectionErrors = t.LogTraceNotification = t.SetTraceNotification = t.TraceFormat = t.TraceValues = t.Trace = t.NullLogger = t.ProgressType = t.ProgressToken = void 0;
            const n = i(2182), o = i(5818), r = i(7318), s = i(8364), a = i(9225), c = i(9910);
            var l, u, d, p, h, g, m, f, v, y, C, w, b, k;
            !function (e) {
                e.type = new r.NotificationType("$/cancelRequest")
            }(l || (l = {})), function (e) {
                e.is = function (e) {
                    return "string" == typeof e || "number" == typeof e
                }
            }(u = t.ProgressToken || (t.ProgressToken = {})), function (e) {
                e.type = new r.NotificationType("$/progress")
            }(d || (d = {}));
            t.ProgressType = class {
                constructor() {
                }
            }, function (e) {
                e.is = function (e) {
                    return o.func(e)
                }
            }(p || (p = {})), t.NullLogger = Object.freeze({
                error: () => {
                }, warn: () => {
                }, info: () => {
                }, log: () => {
                }
            }), function (e) {
                e[e.Off = 0] = "Off", e[e.Messages = 1] = "Messages", e[e.Compact = 2] = "Compact", e[e.Verbose = 3] = "Verbose"
            }(h = t.Trace || (t.Trace = {})), function (e) {
                e.Off = "off", e.Messages = "messages", e.Compact = "compact", e.Verbose = "verbose"
            }(t.TraceValues || (t.TraceValues = {})), function (e) {
                e.fromString = function (t) {
                    if (!o.string(t)) return e.Off;
                    switch (t = t.toLowerCase()) {
                        case"off":
                        default:
                            return e.Off;
                        case"messages":
                            return e.Messages;
                        case"compact":
                            return e.Compact;
                        case"verbose":
                            return e.Verbose
                    }
                }, e.toString = function (t) {
                    switch (t) {
                        case e.Off:
                            return "off";
                        case e.Messages:
                            return "messages";
                        case e.Compact:
                            return "compact";
                        case e.Verbose:
                            return "verbose";
                        default:
                            return "off"
                    }
                }
            }(h = t.Trace || (t.Trace = {})), function (e) {
                e.Text = "text", e.JSON = "json"
            }(t.TraceFormat || (t.TraceFormat = {})), function (e) {
                e.fromString = function (t) {
                    return o.string(t) && "json" === (t = t.toLowerCase()) ? e.JSON : e.Text
                }
            }(g = t.TraceFormat || (t.TraceFormat = {})), function (e) {
                e.type = new r.NotificationType("$/setTrace")
            }(m = t.SetTraceNotification || (t.SetTraceNotification = {})), function (e) {
                e.type = new r.NotificationType("$/logTrace")
            }(f = t.LogTraceNotification || (t.LogTraceNotification = {})), function (e) {
                e[e.Closed = 1] = "Closed", e[e.Disposed = 2] = "Disposed", e[e.AlreadyListening = 3] = "AlreadyListening"
            }(v = t.ConnectionErrors || (t.ConnectionErrors = {}));

            class S extends Error {
                constructor(e, t) {
                    super(t), this.code = e, Object.setPrototypeOf(this, S.prototype)
                }
            }

            t.ConnectionError = S, function (e) {
                e.is = function (e) {
                    const t = e;
                    return t && o.func(t.cancelUndispatched)
                }
            }(y = t.ConnectionStrategy || (t.ConnectionStrategy = {})), function (e) {
                e.Message = Object.freeze({createCancellationTokenSource: e => new c.CancellationTokenSource}), e.is = function (e) {
                    const t = e;
                    return t && o.func(t.createCancellationTokenSource)
                }
            }(C = t.CancellationReceiverStrategy || (t.CancellationReceiverStrategy = {})), function (e) {
                e.Message = Object.freeze({
                    sendCancellation: (e, t) => e.sendNotification(l.type, {id: t}), cleanup(e) {
                    }
                }), e.is = function (e) {
                    const t = e;
                    return t && o.func(t.sendCancellation) && o.func(t.cleanup)
                }
            }(w = t.CancellationSenderStrategy || (t.CancellationSenderStrategy = {})), function (e) {
                e.Message = Object.freeze({receiver: C.Message, sender: w.Message}), e.is = function (e) {
                    const t = e;
                    return t && C.is(t.receiver) && w.is(t.sender)
                }
            }(b = t.CancellationStrategy || (t.CancellationStrategy = {})), function (e) {
                e.is = function (e) {
                    const t = e;
                    return t && (b.is(t.cancellationStrategy) || y.is(t.connectionStrategy))
                }
            }(t.ConnectionOptions || (t.ConnectionOptions = {})), function (e) {
                e[e.New = 1] = "New", e[e.Listening = 2] = "Listening", e[e.Closed = 3] = "Closed", e[e.Disposed = 4] = "Disposed"
            }(k || (k = {})), t.createMessageConnection = function (e, i, y, C) {
                const w = void 0 !== y ? y : t.NullLogger;
                let D = 0, P = 0, T = 0;
                const R = "2.0";
                let _;
                const x = new Map;
                let E;
                const O = new Map, j = new Map;
                let M, I, F = new s.LinkedMap, N = new Map, q = new Set, L = new Map, A = h.Off, $ = g.Text, U = k.New;
                const H = new a.Emitter, W = new a.Emitter, V = new a.Emitter, K = new a.Emitter, B = new a.Emitter,
                    J = C && C.cancellationStrategy ? C.cancellationStrategy : b.Message;

                function z(e) {
                    if (null === e) throw new Error("Can't send requests with id null since the response can't be correlated.");
                    return "req-" + e.toString()
                }

                function G(e, t) {
                    var i;
                    r.Message.isRequest(t) ? e.set(z(t.id), t) : r.Message.isResponse(t) ? e.set(null === (i = t.id) ? "res-unknown-" + (++T).toString() : "res-" + i.toString(), t) : e.set("not-" + (++P).toString(), t)
                }

                function X(e) {
                }

                function Y() {
                    return U === k.Listening
                }

                function Q() {
                    return U === k.Closed
                }

                function Z() {
                    return U === k.Disposed
                }

                function ee() {
                    U !== k.New && U !== k.Listening || (U = k.Closed, W.fire(void 0))
                }

                function te() {
                    M || 0 === F.size || (M = (0, n.default)().timer.setImmediate((() => {
                        M = void 0, function () {
                            if (0 === F.size) return;
                            const e = F.shift();
                            try {
                                r.Message.isRequest(e) ? function (e) {
                                    if (Z()) return;

                                    function t(t, n, o) {
                                        const s = {jsonrpc: R, id: e.id};
                                        t instanceof r.ResponseError ? s.error = t.toJson() : s.result = void 0 === t ? null : t, oe(s, n, o), i.write(s).catch((() => w.error("Sending response failed.")))
                                    }

                                    function n(t, n, o) {
                                        const r = {jsonrpc: R, id: e.id, error: t.toJson()};
                                        oe(r, n, o), i.write(r).catch((() => w.error("Sending response failed.")))
                                    }

                                    function s(t, n, o) {
                                        void 0 === t && (t = null);
                                        const r = {jsonrpc: R, id: e.id, result: t};
                                        oe(r, n, o), i.write(r).catch((() => w.error("Sending response failed.")))
                                    }

                                    !function (e) {
                                        if (A === h.Off || !I) return;
                                        if ($ === g.Text) {
                                            let t;
                                            A !== h.Verbose && A !== h.Compact || !e.params || (t = `Params: ${ne(e.params)}\n\n`), I.log(`Received request '${e.method} - (${e.id})'.`, t)
                                        } else se("receive-request", e)
                                    }(e);
                                    const a = x.get(e.method);
                                    let c, l;
                                    a && (c = a.type, l = a.handler);
                                    const u = Date.now();
                                    if (l || _) {
                                        const i = e.id ?? String(Date.now()),
                                            a = J.receiver.createCancellationTokenSource(i);
                                        null !== e.id && q.has(e.id) && a.cancel(), null !== e.id && L.set(i, a);
                                        try {
                                            let d;
                                            if (l) if (void 0 === e.params) {
                                                if (void 0 !== c && 0 !== c.numberOfParams) return void n(new r.ResponseError(r.ErrorCodes.InvalidParams, `Request ${e.method} defines ${c.numberOfParams} params but received none.`), e.method, u);
                                                d = l(a.token)
                                            } else if (Array.isArray(e.params)) {
                                                if (void 0 !== c && c.parameterStructures === r.ParameterStructures.byName) return void n(new r.ResponseError(r.ErrorCodes.InvalidParams, `Request ${e.method} defines parameters by name but received parameters by position`), e.method, u);
                                                d = l(...e.params, a.token)
                                            } else {
                                                if (void 0 !== c && c.parameterStructures === r.ParameterStructures.byPosition) return void n(new r.ResponseError(r.ErrorCodes.InvalidParams, `Request ${e.method} defines parameters by position but received parameters by name`), e.method, u);
                                                d = l(e.params, a.token)
                                            } else _ && (d = _(e.method, e.params, a.token));
                                            const p = d;
                                            d ? p.then ? p.then((n => {
                                                L.delete(i), t(n, e.method, u)
                                            }), (t => {
                                                L.delete(i), t instanceof r.ResponseError ? n(t, e.method, u) : t && o.string(t.message) ? n(new r.ResponseError(r.ErrorCodes.InternalError, `Request ${e.method} failed with message: ${t.message}`), e.method, u) : n(new r.ResponseError(r.ErrorCodes.InternalError, `Request ${e.method} failed unexpectedly without providing any details.`), e.method, u)
                                            })) : (L.delete(i), t(d, e.method, u)) : (L.delete(i), s(d, e.method, u))
                                        } catch (s) {
                                            L.delete(i), s instanceof r.ResponseError ? t(s, e.method, u) : s && o.string(s.message) ? n(new r.ResponseError(r.ErrorCodes.InternalError, `Request ${e.method} failed with message: ${s.message}`), e.method, u) : n(new r.ResponseError(r.ErrorCodes.InternalError, `Request ${e.method} failed unexpectedly without providing any details.`), e.method, u)
                                        }
                                    } else n(new r.ResponseError(r.ErrorCodes.MethodNotFound, `Unhandled method ${e.method}`), e.method, u)
                                }(e) : r.Message.isNotification(e) ? function (e) {
                                    if (Z()) return;
                                    let t, i;
                                    if (e.method === l.type.method) {
                                        const t = e.params.id;
                                        return q.delete(t), void re(e)
                                    }
                                    {
                                        const n = O.get(e.method);
                                        n && (i = n.handler, t = n.type)
                                    }
                                    if (i || E) try {
                                        if (re(e), i) if (void 0 === e.params) void 0 !== t && 0 !== t.numberOfParams && t.parameterStructures !== r.ParameterStructures.byName && w.error(`Notification ${e.method} defines ${t.numberOfParams} params but received none.`), i(); else if (Array.isArray(e.params)) {
                                            const n = e.params;
                                            e.method === d.type.method && 2 === n.length && u.is(n[0]) ? i({
                                                token: n[0],
                                                value: n[1]
                                            }) : (void 0 !== t && (t.parameterStructures === r.ParameterStructures.byName && w.error(`Notification ${e.method} defines parameters by name but received parameters by position`), t.numberOfParams !== e.params.length && w.error(`Notification ${e.method} defines ${t.numberOfParams} params but received ${n.length} arguments`)), i(...n))
                                        } else void 0 !== t && t.parameterStructures === r.ParameterStructures.byPosition && w.error(`Notification ${e.method} defines parameters by position but received parameters by name`), i(e.params); else E && E(e.method, e.params)
                                    } catch (t) {
                                        t.message ? w.error(`Notification handler '${e.method}' failed with message: ${t.message}`) : w.error(`Notification handler '${e.method}' failed unexpectedly.`)
                                    } else V.fire(e)
                                }(e) : r.Message.isResponse(e) ? function (e) {
                                    if (Z()) return;
                                    if (null === e.id) e.error ? w.error(`Received response message without id: Error is: \n${JSON.stringify(e.error, void 0, 4)}`) : w.error("Received response message without id. No further error information provided."); else {
                                        const t = e.id, i = N.get(t);
                                        if (function (e, t) {
                                            if (A === h.Off || !I) return;
                                            if ($ === g.Text) {
                                                let i;
                                                if (A !== h.Verbose && A !== h.Compact || (e.error && e.error.data ? i = `Error data: ${ne(e.error.data)}\n\n` : e.result ? i = `Result: ${ne(e.result)}\n\n` : void 0 === e.error && (i = "No result returned.\n\n")), t) {
                                                    const n = e.error ? ` Request failed: ${e.error.message} (${e.error.code}).` : "";
                                                    I.log(`Received response '${t.method} - (${e.id})' in ${Date.now() - t.timerStart}ms.${n}`, i)
                                                } else I.log(`Received response ${e.id} without active response promise.`, i)
                                            } else se("receive-response", e)
                                        }(e, i), void 0 !== i) {
                                            N.delete(t);
                                            try {
                                                if (e.error) {
                                                    const t = e.error;
                                                    i.reject(new r.ResponseError(t.code, t.message, t.data))
                                                } else {
                                                    if (void 0 === e.result) throw new Error("Should never happen.");
                                                    i.resolve(e.result)
                                                }
                                            } catch (e) {
                                                e.message ? w.error(`Response handler '${i.method}' failed with message: ${e.message}`) : w.error(`Response handler '${i.method}' failed unexpectedly.`)
                                            }
                                        }
                                    }
                                }(e) : function (e) {
                                    if (!e) return void w.error("Received empty message.");
                                    w.error(`Received message which is neither a response nor a notification message:\n${JSON.stringify(e, null, 4)}`);
                                    const t = e;
                                    if (o.string(t.id) || o.number(t.id)) {
                                        const e = t.id, i = N.get(e);
                                        i && i.reject(new Error("The received response has neither a result nor an error property."))
                                    }
                                }(e)
                            } finally {
                                te()
                            }
                        }()
                    })))
                }

                e.onClose(ee), e.onError((function (e) {
                    H.fire([e, void 0, void 0])
                })), i.onClose(ee), i.onError((function (e) {
                    H.fire(e)
                }));
                const ie = e => {
                    try {
                        if (r.Message.isNotification(e) && e.method === l.type.method) {
                            const t = e.params.id, n = z(t), o = F.get(n);
                            if (r.Message.isRequest(o)) {
                                const r = C?.connectionStrategy,
                                    s = r && r.cancelUndispatched ? r.cancelUndispatched(o, X) : void 0;
                                if (s && (void 0 !== s.error || void 0 !== s.result)) return F.delete(n), L.delete(t), s.id = o.id, oe(s, e.method, Date.now()), void i.write(s).catch((() => w.error("Sending response for canceled message failed.")))
                            }
                            const s = L.get(t);
                            if (void 0 !== s) return s.cancel(), void re(e);
                            q.add(t)
                        }
                        G(F, e)
                    } finally {
                        te()
                    }
                };

                function ne(e) {
                    if (null != e) switch (A) {
                        case h.Verbose:
                            return JSON.stringify(e, null, 4);
                        case h.Compact:
                            return JSON.stringify(e);
                        default:
                            return
                    }
                }

                function oe(e, t, i) {
                    if (A !== h.Off && I) if ($ === g.Text) {
                        let n;
                        A !== h.Verbose && A !== h.Compact || (e.error && e.error.data ? n = `Error data: ${ne(e.error.data)}\n\n` : e.result ? n = `Result: ${ne(e.result)}\n\n` : void 0 === e.error && (n = "No result returned.\n\n")), I.log(`Sending response '${t} - (${e.id})'. Processing request took ${Date.now() - i}ms`, n)
                    } else se("send-response", e)
                }

                function re(e) {
                    if (A !== h.Off && I && e.method !== f.type.method) if ($ === g.Text) {
                        let t;
                        A !== h.Verbose && A !== h.Compact || (t = e.params ? `Params: ${ne(e.params)}\n\n` : "No parameters provided.\n\n"), I.log(`Received notification '${e.method}'.`, t)
                    } else se("receive-notification", e)
                }

                function se(e, t) {
                    if (!I || A === h.Off) return;
                    const i = {isLSPMessage: !0, type: e, message: t, timestamp: Date.now()};
                    I.log(i)
                }

                function ae() {
                    if (Q()) throw new S(v.Closed, "Connection is closed.");
                    if (Z()) throw new S(v.Disposed, "Connection is disposed.")
                }

                function ce(e) {
                    return void 0 === e ? null : e
                }

                function le(e) {
                    return null === e ? void 0 : e
                }

                function ue(e) {
                    return null != e && !Array.isArray(e) && "object" == typeof e
                }

                function de(e, t) {
                    switch (e) {
                        case r.ParameterStructures.auto:
                            return ue(t) ? le(t) : [ce(t)];
                        case r.ParameterStructures.byName:
                            if (!ue(t)) throw new Error("Received parameters by name but param is not an object literal.");
                            return le(t);
                        case r.ParameterStructures.byPosition:
                            return [ce(t)];
                        default:
                            throw new Error(`Unknown parameter structure ${e.toString()}`)
                    }
                }

                function pe(e, t) {
                    let i;
                    const n = e.numberOfParams;
                    switch (n) {
                        case 0:
                            i = void 0;
                            break;
                        case 1:
                            i = de(e.parameterStructures, t[0]);
                            break;
                        default:
                            i = [];
                            for (let e = 0; e < t.length && e < n; e++) i.push(ce(t[e]));
                            if (t.length < n) for (let e = t.length; e < n; e++) i.push(null)
                    }
                    return i
                }

                const he = {
                    sendNotification: (e, ...t) => {
                        let n, s;
                        if (ae(), o.string(e)) {
                            n = e;
                            const i = t[0];
                            let o = 0, a = r.ParameterStructures.auto;
                            r.ParameterStructures.is(i) && (o = 1, a = i);
                            let c = t.length;
                            const l = c - o;
                            switch (l) {
                                case 0:
                                    s = void 0;
                                    break;
                                case 1:
                                    s = de(a, t[o]);
                                    break;
                                default:
                                    if (a === r.ParameterStructures.byName) throw new Error(`Received ${l} parameters for 'by Name' notification parameter structure.`);
                                    s = t.slice(o, c).map((e => ce(e)))
                            }
                        } else {
                            const i = t;
                            n = e.method, s = pe(e, i)
                        }
                        const a = {jsonrpc: R, method: n, params: s};
                        return function (e) {
                            if (A !== h.Off && I) if ($ === g.Text) {
                                let t;
                                A !== h.Verbose && A !== h.Compact || (t = e.params ? `Params: ${ne(e.params)}\n\n` : "No parameters provided.\n\n"), I.log(`Sending notification '${e.method}'.`, t)
                            } else se("send-notification", e)
                        }(a), i.write(a).catch((() => w.error("Sending notification failed.")))
                    },
                    onNotification: (e, t) => {
                        let i;
                        return ae(), o.func(e) ? E = e : t && (o.string(e) ? (i = e, O.set(e, {
                            type: void 0,
                            handler: t
                        })) : (i = e.method, O.set(e.method, {type: e, handler: t}))), {
                            dispose: () => {
                                void 0 !== i ? O.delete(i) : E = void 0
                            }
                        }
                    },
                    onProgress: (e, t, i) => {
                        if (j.has(t)) throw new Error(`Progress handler for token ${t} already registered`);
                        return j.set(t, i), {
                            dispose: () => {
                                j.delete(t)
                            }
                        }
                    },
                    sendProgress: (e, t, i) => he.sendNotification(d.type, {token: t, value: i}),
                    onUnhandledProgress: K.event,
                    sendRequest: (e, ...t) => {
                        let n, s, a;
                        if (ae(), function () {
                            if (!Y()) throw new Error("Call listen() first.")
                        }(), o.string(e)) {
                            n = e;
                            const i = t[0], o = t[t.length - 1];
                            let l = 0, u = r.ParameterStructures.auto;
                            r.ParameterStructures.is(i) && (l = 1, u = i);
                            let d = t.length;
                            c.CancellationToken.is(o) && (d -= 1, a = o);
                            const p = d - l;
                            switch (p) {
                                case 0:
                                    s = void 0;
                                    break;
                                case 1:
                                    s = de(u, t[l]);
                                    break;
                                default:
                                    if (u === r.ParameterStructures.byName) throw new Error(`Received ${p} parameters for 'by Name' request parameter structure.`);
                                    s = t.slice(l, d).map((e => ce(e)))
                            }
                        } else {
                            const i = t;
                            n = e.method, s = pe(e, i);
                            const o = e.numberOfParams;
                            a = c.CancellationToken.is(i[o]) ? i[o] : void 0
                        }
                        const l = D++;
                        let u;
                        a && (u = a.onCancellationRequested((() => {
                            const e = J.sender.sendCancellation(he, l);
                            return void 0 === e ? (w.log(`Received no promise from cancellation strategy when cancelling id ${l}`), Promise.resolve()) : e.catch((() => {
                                w.log(`Sending cancellation messages for id ${l} failed`)
                            }))
                        })));
                        return new Promise(((e, t) => {
                            const o = {jsonrpc: R, id: l, method: n, params: s};
                            let a = {
                                method: n, timerStart: Date.now(), resolve: t => {
                                    e(t), J.sender.cleanup(l), u?.dispose()
                                }, reject: e => {
                                    t(e), J.sender.cleanup(l), u?.dispose()
                                }
                            };
                            !function (e) {
                                if (A !== h.Off && I) if ($ === g.Text) {
                                    let t;
                                    A !== h.Verbose && A !== h.Compact || !e.params || (t = `Params: ${ne(e.params)}\n\n`), I.log(`Sending request '${e.method} - (${e.id})'.`, t)
                                } else se("send-request", e)
                            }(o);
                            try {
                                i.write(o).catch((() => w.error("Sending request failed.")))
                            } catch (e) {
                                a.reject(new r.ResponseError(r.ErrorCodes.MessageWriteError, e.message ? e.message : "Unknown reason")), a = null
                            }
                            a && N.set(l, a)
                        }))
                    },
                    onRequest: (e, t) => {
                        ae();
                        let i = null;
                        return p.is(e) ? (i = void 0, _ = e) : o.string(e) ? (i = null, void 0 !== t && (i = e, x.set(e, {
                            handler: t,
                            type: void 0
                        }))) : void 0 !== t && (i = e.method, x.set(e.method, {type: e, handler: t})), {
                            dispose: () => {
                                null !== i && (void 0 !== i ? x.delete(i) : _ = void 0)
                            }
                        }
                    },
                    hasPendingResponse: () => N.size > 0,
                    trace: async (e, t, i) => {
                        let n = !1, r = g.Text;
                        void 0 !== i && (o.boolean(i) ? n = i : (n = i.sendNotification || !1, r = i.traceFormat || g.Text)), A = e, $ = r, I = A === h.Off ? void 0 : t, !n || Q() || Z() || await he.sendNotification(m.type, {value: h.toString(e)})
                    },
                    onError: H.event,
                    onClose: W.event,
                    onUnhandledNotification: V.event,
                    onDispose: B.event,
                    end: () => {
                        i.end()
                    },
                    dispose: () => {
                        if (Z()) return;
                        U = k.Disposed, B.fire(void 0);
                        const t = new r.ResponseError(r.ErrorCodes.PendingResponseRejected, "Pending response rejected since connection got disposed");
                        for (const e of N.values()) e.reject(t);
                        N = new Map, L = new Map, q = new Set, F = new s.LinkedMap, o.func(i.dispose) && i.dispose(), o.func(e.dispose) && e.dispose()
                    },
                    listen: () => {
                        ae(), function () {
                            if (Y()) throw new S(v.AlreadyListening, "Connection is already listening")
                        }(), U = k.Listening, e.listen(ie)
                    },
                    inspect: () => {
                        (0, n.default)().console.log("inspect")
                    }
                };
                return he.onNotification(f.type, (e => {
                    if (A === h.Off || !I) return;
                    const t = A === h.Verbose || A === h.Compact;
                    I.log(e.message, t ? e.verbose : void 0)
                })), he.onNotification(d.type, (e => {
                    const t = j.get(e.token);
                    t ? t(e.value) : K.fire(e)
                })), he
            }
        }, 737: (e, t) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.Disposable = void 0, function (e) {
                e.create = function (e) {
                    return {dispose: e}
                }
            }(t.Disposable || (t.Disposable = {}))
        }, 9225: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.Emitter = t.Event = void 0;
            const n = i(2182);
            !function (e) {
                const t = {
                    dispose() {
                    }
                };
                e.None = function () {
                    return t
                }
            }(t.Event || (t.Event = {}));

            class o {
                add(e, t = null, i) {
                    this._callbacks || (this._callbacks = [], this._contexts = []), this._callbacks.push(e), this._contexts.push(t), Array.isArray(i) && i.push({dispose: () => this.remove(e, t)})
                }

                remove(e, t = null) {
                    if (!this._callbacks) return;
                    let i = !1;
                    for (let n = 0, o = this._callbacks.length; n < o; n++) if (this._callbacks[n] === e) {
                        if (this._contexts[n] === t) return this._callbacks.splice(n, 1), void this._contexts.splice(n, 1);
                        i = !0
                    }
                    if (i) throw new Error("When adding a listener with a context, you should remove it with the same context")
                }

                invoke(...e) {
                    if (!this._callbacks) return [];
                    const t = [], i = this._callbacks.slice(0), o = this._contexts.slice(0);
                    for (let r = 0, s = i.length; r < s; r++) try {
                        t.push(i[r].apply(o[r], e))
                    } catch (e) {
                        (0, n.default)().console.error(e)
                    }
                    return t
                }

                isEmpty() {
                    return !this._callbacks || 0 === this._callbacks.length
                }

                dispose() {
                    this._callbacks = void 0, this._contexts = void 0
                }
            }

            class r {
                constructor(e) {
                    this._options = e
                }

                get event() {
                    return this._event || (this._event = (e, t, i) => {
                        this._callbacks || (this._callbacks = new o), this._options && this._options.onFirstListenerAdd && this._callbacks.isEmpty() && this._options.onFirstListenerAdd(this), this._callbacks.add(e, t);
                        const n = {
                            dispose: () => {
                                this._callbacks && (this._callbacks.remove(e, t), n.dispose = r._noop, this._options && this._options.onLastListenerRemove && this._callbacks.isEmpty() && this._options.onLastListenerRemove(this))
                            }
                        };
                        return Array.isArray(i) && i.push(n), n
                    }), this._event
                }

                fire(e) {
                    this._callbacks && this._callbacks.invoke.call(this._callbacks, e)
                }

                dispose() {
                    this._callbacks && (this._callbacks.dispose(), this._callbacks = void 0)
                }
            }

            t.Emitter = r, r._noop = function () {
            }
        }, 5818: (e, t) => {
            "use strict";

            function i(e) {
                return "string" == typeof e || e instanceof String
            }

            function n(e) {
                return Array.isArray(e)
            }

            Object.defineProperty(t, "__esModule", {value: !0}), t.stringArray = t.array = t.func = t.error = t.number = t.string = t.boolean = void 0, t.boolean = function (e) {
                return !0 === e || !1 === e
            }, t.string = i, t.number = function (e) {
                return "number" == typeof e || e instanceof Number
            }, t.error = function (e) {
                return e instanceof Error
            }, t.func = function (e) {
                return "function" == typeof e
            }, t.array = n, t.stringArray = function (e) {
                return n(e) && e.every((e => i(e)))
            }
        }, 8364: (e, t) => {
            "use strict";
            var i, n;
            Object.defineProperty(t, "__esModule", {value: !0}), t.LRUCache = t.LinkedMap = t.Touch = void 0, function (e) {
                e.None = 0, e.First = 1, e.AsOld = e.First, e.Last = 2, e.AsNew = e.Last
            }(n = t.Touch || (t.Touch = {}));

            class o {
                constructor() {
                    this[i] = "LinkedMap", this._map = new Map, this._head = void 0, this._tail = void 0, this._size = 0, this._state = 0
                }

                clear() {
                    this._map.clear(), this._head = void 0, this._tail = void 0, this._size = 0, this._state++
                }

                isEmpty() {
                    return !this._head && !this._tail
                }

                get size() {
                    return this._size
                }

                get first() {
                    return this._head?.value
                }

                get last() {
                    return this._tail?.value
                }

                has(e) {
                    return this._map.has(e)
                }

                get(e, t = n.None) {
                    const i = this._map.get(e);
                    if (i) return t !== n.None && this.touch(i, t), i.value
                }

                set(e, t, i = n.None) {
                    let o = this._map.get(e);
                    if (o) o.value = t, i !== n.None && this.touch(o, i); else {
                        switch (o = {key: e, value: t, next: void 0, previous: void 0}, i) {
                            case n.None:
                                this.addItemLast(o);
                                break;
                            case n.First:
                                this.addItemFirst(o);
                                break;
                            case n.Last:
                            default:
                                this.addItemLast(o)
                        }
                        this._map.set(e, o), this._size++
                    }
                    return this
                }

                delete(e) {
                    return !!this.remove(e)
                }

                remove(e) {
                    const t = this._map.get(e);
                    if (t) return this._map.delete(e), this.removeItem(t), this._size--, t.value
                }

                shift() {
                    if (!this._head && !this._tail) return;
                    if (!this._head || !this._tail) throw new Error("Invalid list");
                    const e = this._head;
                    return this._map.delete(e.key), this.removeItem(e), this._size--, e.value
                }

                forEach(e, t) {
                    const i = this._state;
                    let n = this._head;
                    for (; n;) {
                        if (t ? e.bind(t)(n.value, n.key, this) : e(n.value, n.key, this), this._state !== i) throw new Error("LinkedMap got modified during iteration.");
                        n = n.next
                    }
                }

                keys() {
                    const e = this._state;
                    let t = this._head;
                    const i = {
                        [Symbol.iterator]: () => i, next: () => {
                            if (this._state !== e) throw new Error("LinkedMap got modified during iteration.");
                            if (t) {
                                const e = {value: t.key, done: !1};
                                return t = t.next, e
                            }
                            return {value: void 0, done: !0}
                        }
                    };
                    return i
                }

                values() {
                    const e = this._state;
                    let t = this._head;
                    const i = {
                        [Symbol.iterator]: () => i, next: () => {
                            if (this._state !== e) throw new Error("LinkedMap got modified during iteration.");
                            if (t) {
                                const e = {value: t.value, done: !1};
                                return t = t.next, e
                            }
                            return {value: void 0, done: !0}
                        }
                    };
                    return i
                }

                entries() {
                    const e = this._state;
                    let t = this._head;
                    const i = {
                        [Symbol.iterator]: () => i, next: () => {
                            if (this._state !== e) throw new Error("LinkedMap got modified during iteration.");
                            if (t) {
                                const e = {value: [t.key, t.value], done: !1};
                                return t = t.next, e
                            }
                            return {value: void 0, done: !0}
                        }
                    };
                    return i
                }

                [(i = Symbol.toStringTag, Symbol.iterator)]() {
                    return this.entries()
                }

                trimOld(e) {
                    if (e >= this.size) return;
                    if (0 === e) return void this.clear();
                    let t = this._head, i = this.size;
                    for (; t && i > e;) this._map.delete(t.key), t = t.next, i--;
                    this._head = t, this._size = i, t && (t.previous = void 0), this._state++
                }

                addItemFirst(e) {
                    if (this._head || this._tail) {
                        if (!this._head) throw new Error("Invalid list");
                        e.next = this._head, this._head.previous = e
                    } else this._tail = e;
                    this._head = e, this._state++
                }

                addItemLast(e) {
                    if (this._head || this._tail) {
                        if (!this._tail) throw new Error("Invalid list");
                        e.previous = this._tail, this._tail.next = e
                    } else this._head = e;
                    this._tail = e, this._state++
                }

                removeItem(e) {
                    if (e === this._head && e === this._tail) this._head = void 0, this._tail = void 0; else if (e === this._head) {
                        if (!e.next) throw new Error("Invalid list");
                        e.next.previous = void 0, this._head = e.next
                    } else if (e === this._tail) {
                        if (!e.previous) throw new Error("Invalid list");
                        e.previous.next = void 0, this._tail = e.previous
                    } else {
                        const t = e.next, i = e.previous;
                        if (!t || !i) throw new Error("Invalid list");
                        t.previous = i, i.next = t
                    }
                    e.next = void 0, e.previous = void 0, this._state++
                }

                touch(e, t) {
                    if (!this._head || !this._tail) throw new Error("Invalid list");
                    if (t === n.First || t === n.Last) if (t === n.First) {
                        if (e === this._head) return;
                        const t = e.next, i = e.previous;
                        e === this._tail ? (i.next = void 0, this._tail = i) : (t.previous = i, i.next = t), e.previous = void 0, e.next = this._head, this._head.previous = e, this._head = e, this._state++
                    } else if (t === n.Last) {
                        if (e === this._tail) return;
                        const t = e.next, i = e.previous;
                        e === this._head ? (t.previous = void 0, this._head = t) : (t.previous = i, i.next = t), e.next = void 0, e.previous = this._tail, this._tail.next = e, this._tail = e, this._state++
                    }
                }

                toJSON() {
                    const e = [];
                    return this.forEach(((t, i) => {
                        e.push([i, t])
                    })), e
                }

                fromJSON(e) {
                    this.clear();
                    for (const [t, i] of e) this.set(t, i)
                }
            }

            t.LinkedMap = o;
            t.LRUCache = class extends o {
                constructor(e, t = 1) {
                    super(), this._limit = e, this._ratio = Math.min(Math.max(0, t), 1)
                }

                get limit() {
                    return this._limit
                }

                set limit(e) {
                    this._limit = e, this.checkTrim()
                }

                get ratio() {
                    return this._ratio
                }

                set ratio(e) {
                    this._ratio = Math.min(Math.max(0, e), 1), this.checkTrim()
                }

                get(e, t = n.AsNew) {
                    return super.get(e, t)
                }

                peek(e) {
                    return super.get(e, n.None)
                }

                set(e, t) {
                    return super.set(e, t, n.Last), this.checkTrim(), this
                }

                checkTrim() {
                    this.size > this._limit && this.trimOld(Math.round(this._limit * this._ratio))
                }
            }
        }, 5377: (e, t) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.AbstractMessageBuffer = void 0;
            t.AbstractMessageBuffer = class {
                constructor(e = "utf-8") {
                    this._encoding = e, this._chunks = [], this._totalLength = 0
                }

                get encoding() {
                    return this._encoding
                }

                append(e) {
                    const t = "string" == typeof e ? this.fromString(e, this._encoding) : e;
                    this._chunks.push(t), this._totalLength += t.byteLength
                }

                tryReadHeaders() {
                    if (0 === this._chunks.length) return;
                    let e = 0, t = 0, i = 0, n = 0;
                    e:for (; t < this._chunks.length;) {
                        const o = this._chunks[t];
                        for (i = 0; i < o.length;) {
                            switch (o[i]) {
                                case 13:
                                    switch (e) {
                                        case 0:
                                            e = 1;
                                            break;
                                        case 2:
                                            e = 3;
                                            break;
                                        default:
                                            e = 0
                                    }
                                    break;
                                case 10:
                                    switch (e) {
                                        case 1:
                                            e = 2;
                                            break;
                                        case 3:
                                            e = 4, i++;
                                            break e;
                                        default:
                                            e = 0
                                    }
                                    break;
                                default:
                                    e = 0
                            }
                            i++
                        }
                        n += o.byteLength, t++
                    }
                    if (4 !== e) return;
                    const o = this._read(n + i), r = new Map, s = this.toString(o, "ascii").split("\r\n");
                    if (s.length < 2) return r;
                    for (let e = 0; e < s.length - 2; e++) {
                        const t = s[e], i = t.indexOf(":");
                        if (-1 === i) throw new Error("Message header must separate key and value using :");
                        const n = t.substr(0, i), o = t.substr(i + 1).trim();
                        r.set(n, o)
                    }
                    return r
                }

                tryReadBody(e) {
                    if (!(this._totalLength < e)) return this._read(e)
                }

                get numberOfBytes() {
                    return this._totalLength
                }

                _read(e) {
                    if (0 === e) return this.emptyBuffer();
                    if (e > this._totalLength) throw new Error("Cannot read so many bytes!");
                    if (this._chunks[0].byteLength === e) {
                        const t = this._chunks[0];
                        return this._chunks.shift(), this._totalLength -= e, this.asNative(t)
                    }
                    if (this._chunks[0].byteLength > e) {
                        const t = this._chunks[0], i = this.asNative(t, e);
                        return this._chunks[0] = t.slice(e), this._totalLength -= e, i
                    }
                    const t = this.allocNative(e);
                    let i = 0;
                    for (; e > 0;) {
                        const n = this._chunks[0];
                        if (n.byteLength > e) {
                            const o = n.slice(0, e);
                            t.set(o, i), i += e, this._chunks[0] = n.slice(e), this._totalLength -= e, e -= e
                        } else t.set(n, i), i += n.byteLength, this._chunks.shift(), this._totalLength -= n.byteLength, e -= n.byteLength
                    }
                    return t
                }
            }
        }, 2514: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ReadableStreamMessageReader = t.AbstractMessageReader = t.MessageReader = void 0;
            const n = i(2182), o = i(5818), r = i(9225);
            var s;
            !function (e) {
                e.is = function (e) {
                    let t = e;
                    return t && o.func(t.listen) && o.func(t.dispose) && o.func(t.onError) && o.func(t.onClose) && o.func(t.onPartialMessage)
                }
            }(t.MessageReader || (t.MessageReader = {}));

            class a {
                constructor() {
                    this.errorEmitter = new r.Emitter, this.closeEmitter = new r.Emitter, this.partialMessageEmitter = new r.Emitter
                }

                dispose() {
                    this.errorEmitter.dispose(), this.closeEmitter.dispose()
                }

                get onError() {
                    return this.errorEmitter.event
                }

                fireError(e) {
                    this.errorEmitter.fire(this.asError(e))
                }

                get onClose() {
                    return this.closeEmitter.event
                }

                fireClose() {
                    this.closeEmitter.fire(void 0)
                }

                get onPartialMessage() {
                    return this.partialMessageEmitter.event
                }

                firePartialMessage(e) {
                    this.partialMessageEmitter.fire(e)
                }

                asError(e) {
                    return e instanceof Error ? e : new Error(`Reader received error. Reason: ${o.string(e.message) ? e.message : "unknown"}`)
                }
            }

            t.AbstractMessageReader = a, function (e) {
                e.fromOptions = function (e) {
                    let t, i;
                    const o = new Map;
                    let r;
                    const s = new Map;
                    if (void 0 === e || "string" == typeof e) t = e ?? "utf-8"; else {
                        if (t = e.charset ?? "utf-8", void 0 !== e.contentDecoder && (i = e.contentDecoder, o.set(i.name, i)), void 0 !== e.contentDecoders) for (const t of e.contentDecoders) o.set(t.name, t);
                        if (void 0 !== e.contentTypeDecoder && (r = e.contentTypeDecoder, s.set(r.name, r)), void 0 !== e.contentTypeDecoders) for (const t of e.contentTypeDecoders) s.set(t.name, t)
                    }
                    return void 0 === r && (r = (0, n.default)().applicationJson.decoder, s.set(r.name, r)), {
                        charset: t,
                        contentDecoder: i,
                        contentDecoders: o,
                        contentTypeDecoder: r,
                        contentTypeDecoders: s
                    }
                }
            }(s || (s = {}));
            t.ReadableStreamMessageReader = class extends a {
                constructor(e, t) {
                    super(), this.readable = e, this.options = s.fromOptions(t), this.buffer = (0, n.default)().messageBuffer.create(this.options.charset), this._partialMessageTimeout = 1e4, this.nextMessageLength = -1, this.messageToken = 0
                }

                set partialMessageTimeout(e) {
                    this._partialMessageTimeout = e
                }

                get partialMessageTimeout() {
                    return this._partialMessageTimeout
                }

                listen(e) {
                    this.nextMessageLength = -1, this.messageToken = 0, this.partialMessageTimer = void 0, this.callback = e;
                    const t = this.readable.onData((e => {
                        this.onData(e)
                    }));
                    return this.readable.onError((e => this.fireError(e))), this.readable.onClose((() => this.fireClose())), t
                }

                onData(e) {
                    for (this.buffer.append(e); ;) {
                        if (-1 === this.nextMessageLength) {
                            const e = this.buffer.tryReadHeaders();
                            if (!e) return;
                            const t = e.get("Content-Length");
                            if (!t) throw new Error("Header must provide a Content-Length property.");
                            const i = parseInt(t);
                            if (isNaN(i)) throw new Error("Content-Length value must be a number.");
                            this.nextMessageLength = i
                        }
                        const e = this.buffer.tryReadBody(this.nextMessageLength);
                        if (void 0 === e) return void this.setPartialMessageTimer();
                        let t;
                        this.clearPartialMessageTimer(), this.nextMessageLength = -1, t = void 0 !== this.options.contentDecoder ? this.options.contentDecoder.decode(e) : Promise.resolve(e), t.then((e => {
                            this.options.contentTypeDecoder.decode(e, this.options).then((e => {
                                this.callback(e)
                            }), (e => {
                                this.fireError(e)
                            }))
                        }), (e => {
                            this.fireError(e)
                        }))
                    }
                }

                clearPartialMessageTimer() {
                    this.partialMessageTimer && (this.partialMessageTimer.dispose(), this.partialMessageTimer = void 0)
                }

                setPartialMessageTimer() {
                    this.clearPartialMessageTimer(), this._partialMessageTimeout <= 0 || (this.partialMessageTimer = (0, n.default)().timer.setTimeout(((e, t) => {
                        this.partialMessageTimer = void 0, e === this.messageToken && (this.firePartialMessage({
                            messageToken: e,
                            waitingTime: t
                        }), this.setPartialMessageTimer())
                    }), this._partialMessageTimeout, this.messageToken, this._partialMessageTimeout))
                }
            }
        }, 486: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.WriteableStreamMessageWriter = t.AbstractMessageWriter = t.MessageWriter = void 0;
            const n = i(2182), o = i(5818), r = i(2204), s = i(9225);
            var a;
            !function (e) {
                e.is = function (e) {
                    let t = e;
                    return t && o.func(t.dispose) && o.func(t.onClose) && o.func(t.onError) && o.func(t.write)
                }
            }(t.MessageWriter || (t.MessageWriter = {}));

            class c {
                constructor() {
                    this.errorEmitter = new s.Emitter, this.closeEmitter = new s.Emitter
                }

                dispose() {
                    this.errorEmitter.dispose(), this.closeEmitter.dispose()
                }

                get onError() {
                    return this.errorEmitter.event
                }

                fireError(e, t, i) {
                    this.errorEmitter.fire([this.asError(e), t, i])
                }

                get onClose() {
                    return this.closeEmitter.event
                }

                fireClose() {
                    this.closeEmitter.fire(void 0)
                }

                asError(e) {
                    return e instanceof Error ? e : new Error(`Writer received error. Reason: ${o.string(e.message) ? e.message : "unknown"}`)
                }
            }

            t.AbstractMessageWriter = c, function (e) {
                e.fromOptions = function (e) {
                    return void 0 === e || "string" == typeof e ? {
                        charset: e ?? "utf-8",
                        contentTypeEncoder: (0, n.default)().applicationJson.encoder
                    } : {
                        charset: e.charset ?? "utf-8",
                        contentEncoder: e.contentEncoder,
                        contentTypeEncoder: e.contentTypeEncoder ?? (0, n.default)().applicationJson.encoder
                    }
                }
            }(a || (a = {}));
            t.WriteableStreamMessageWriter = class extends c {
                constructor(e, t) {
                    super(), this.writable = e, this.options = a.fromOptions(t), this.errorCount = 0, this.writeSemaphore = new r.Semaphore(1), this.writable.onError((e => this.fireError(e))), this.writable.onClose((() => this.fireClose()))
                }

                async write(e) {
                    return this.writeSemaphore.lock((async () => this.options.contentTypeEncoder.encode(e, this.options).then((e => void 0 !== this.options.contentEncoder ? this.options.contentEncoder.encode(e) : e)).then((t => {
                        const i = [];
                        return i.push("Content-Length: ", t.byteLength.toString(), "\r\n"), i.push("\r\n"), this.doWrite(e, i, t)
                    }), (e => {
                        throw this.fireError(e), e
                    }))))
                }

                async doWrite(e, t, i) {
                    try {
                        return await this.writable.write(t.join(""), "ascii"), this.writable.write(i)
                    } catch (t) {
                        return this.handleError(t, e), Promise.reject(t)
                    }
                }

                handleError(e, t) {
                    this.errorCount++, this.fireError(e, t, this.errorCount)
                }

                end() {
                    this.writable.end()
                }
            }
        }, 7318: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.Message = t.NotificationType9 = t.NotificationType8 = t.NotificationType7 = t.NotificationType6 = t.NotificationType5 = t.NotificationType4 = t.NotificationType3 = t.NotificationType2 = t.NotificationType1 = t.NotificationType0 = t.NotificationType = t.RequestType9 = t.RequestType8 = t.RequestType7 = t.RequestType6 = t.RequestType5 = t.RequestType4 = t.RequestType3 = t.RequestType2 = t.RequestType1 = t.RequestType = t.RequestType0 = t.AbstractMessageSignature = t.ParameterStructures = t.ResponseError = t.ErrorCodes = void 0;
            const n = i(5818);
            var o;
            !function (e) {
                e.ParseError = -32700, e.InvalidRequest = -32600, e.MethodNotFound = -32601, e.InvalidParams = -32602, e.InternalError = -32603, e.jsonrpcReservedErrorRangeStart = -32099, e.serverErrorStart = -32099, e.MessageWriteError = -32099, e.MessageReadError = -32098, e.PendingResponseRejected = -32097, e.ConnectionInactive = -32096, e.ServerNotInitialized = -32002, e.UnknownErrorCode = -32001, e.jsonrpcReservedErrorRangeEnd = -32e3, e.serverErrorEnd = -32e3
            }(o = t.ErrorCodes || (t.ErrorCodes = {}));

            class r extends Error {
                constructor(e, t, i) {
                    super(t), this.code = n.number(e) ? e : o.UnknownErrorCode, this.data = i, Object.setPrototypeOf(this, r.prototype)
                }

                toJson() {
                    const e = {code: this.code, message: this.message};
                    return void 0 !== this.data && (e.data = this.data), e
                }
            }

            t.ResponseError = r;

            class s {
                constructor(e) {
                    this.kind = e
                }

                static is(e) {
                    return e === s.auto || e === s.byName || e === s.byPosition
                }

                toString() {
                    return this.kind
                }
            }

            t.ParameterStructures = s, s.auto = new s("auto"), s.byPosition = new s("byPosition"), s.byName = new s("byName");

            class a {
                constructor(e, t) {
                    this.method = e, this.numberOfParams = t
                }

                get parameterStructures() {
                    return s.auto
                }
            }

            t.AbstractMessageSignature = a;
            t.RequestType0 = class extends a {
                constructor(e) {
                    super(e, 0)
                }
            };
            t.RequestType = class extends a {
                constructor(e, t = s.auto) {
                    super(e, 1), this._parameterStructures = t
                }

                get parameterStructures() {
                    return this._parameterStructures
                }
            };
            t.RequestType1 = class extends a {
                constructor(e, t = s.auto) {
                    super(e, 1), this._parameterStructures = t
                }

                get parameterStructures() {
                    return this._parameterStructures
                }
            };
            t.RequestType2 = class extends a {
                constructor(e) {
                    super(e, 2)
                }
            };
            t.RequestType3 = class extends a {
                constructor(e) {
                    super(e, 3)
                }
            };
            t.RequestType4 = class extends a {
                constructor(e) {
                    super(e, 4)
                }
            };
            t.RequestType5 = class extends a {
                constructor(e) {
                    super(e, 5)
                }
            };
            t.RequestType6 = class extends a {
                constructor(e) {
                    super(e, 6)
                }
            };
            t.RequestType7 = class extends a {
                constructor(e) {
                    super(e, 7)
                }
            };
            t.RequestType8 = class extends a {
                constructor(e) {
                    super(e, 8)
                }
            };
            t.RequestType9 = class extends a {
                constructor(e) {
                    super(e, 9)
                }
            };
            t.NotificationType = class extends a {
                constructor(e, t = s.auto) {
                    super(e, 1), this._parameterStructures = t
                }

                get parameterStructures() {
                    return this._parameterStructures
                }
            };
            t.NotificationType0 = class extends a {
                constructor(e) {
                    super(e, 0)
                }
            };
            t.NotificationType1 = class extends a {
                constructor(e, t = s.auto) {
                    super(e, 1), this._parameterStructures = t
                }

                get parameterStructures() {
                    return this._parameterStructures
                }
            };
            t.NotificationType2 = class extends a {
                constructor(e) {
                    super(e, 2)
                }
            };
            t.NotificationType3 = class extends a {
                constructor(e) {
                    super(e, 3)
                }
            };
            t.NotificationType4 = class extends a {
                constructor(e) {
                    super(e, 4)
                }
            };
            t.NotificationType5 = class extends a {
                constructor(e) {
                    super(e, 5)
                }
            };
            t.NotificationType6 = class extends a {
                constructor(e) {
                    super(e, 6)
                }
            };
            t.NotificationType7 = class extends a {
                constructor(e) {
                    super(e, 7)
                }
            };
            t.NotificationType8 = class extends a {
                constructor(e) {
                    super(e, 8)
                }
            };
            t.NotificationType9 = class extends a {
                constructor(e) {
                    super(e, 9)
                }
            }, function (e) {
                e.isRequest = function (e) {
                    const t = e;
                    return t && n.string(t.method) && (n.string(t.id) || n.number(t.id))
                }, e.isNotification = function (e) {
                    const t = e;
                    return t && n.string(t.method) && void 0 === e.id
                }, e.isResponse = function (e) {
                    const t = e;
                    return t && (void 0 !== t.result || !!t.error) && (n.string(t.id) || n.number(t.id) || null === t.id)
                }
            }(t.Message || (t.Message = {}))
        }, 2182: (e, t) => {
            "use strict";
            let i;

            function n() {
                if (void 0 === i) throw new Error("No runtime abstraction layer installed");
                return i
            }

            Object.defineProperty(t, "__esModule", {value: !0}), function (e) {
                e.install = function (e) {
                    if (void 0 === e) throw new Error("No runtime abstraction layer provided");
                    i = e
                }
            }(n || (n = {})), t.default = n
        }, 2204: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.Semaphore = void 0;
            const n = i(2182);
            t.Semaphore = class {
                constructor(e = 1) {
                    if (e <= 0) throw new Error("Capacity must be greater than 0");
                    this._capacity = e, this._active = 0, this._waiting = []
                }

                lock(e) {
                    return new Promise(((t, i) => {
                        this._waiting.push({thunk: e, resolve: t, reject: i}), this.runNext()
                    }))
                }

                get active() {
                    return this._active
                }

                runNext() {
                    0 !== this._waiting.length && this._active !== this._capacity && (0, n.default)().timer.setImmediate((() => this.doRunNext()))
                }

                doRunNext() {
                    if (0 === this._waiting.length || this._active === this._capacity) return;
                    const e = this._waiting.shift();
                    if (this._active++, this._active > this._capacity) throw new Error("To many thunks active");
                    try {
                        const t = e.thunk();
                        t instanceof Promise ? t.then((t => {
                            this._active--, e.resolve(t), this.runNext()
                        }), (t => {
                            this._active--, e.reject(t), this.runNext()
                        })) : (this._active--, e.resolve(t), this.runNext())
                    } catch (t) {
                        this._active--, e.reject(t), this.runNext()
                    }
                }
            }
        }, 8862: function (e, t, i) {
            "use strict";
            var n = this && this.__createBinding || (Object.create ? function (e, t, i, n) {
                void 0 === n && (n = i);
                var o = Object.getOwnPropertyDescriptor(t, i);
                o && !("get" in o ? !t.__esModule : o.writable || o.configurable) || (o = {
                    enumerable: !0,
                    get: function () {
                        return t[i]
                    }
                }), Object.defineProperty(e, n, o)
            } : function (e, t, i, n) {
                void 0 === n && (n = i), e[n] = t[i]
            }), o = this && this.__exportStar || function (e, t) {
                for (var i in e) "default" === i || Object.prototype.hasOwnProperty.call(t, i) || n(t, e, i)
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.createMessageConnection = t.createServerSocketTransport = t.createClientSocketTransport = t.createServerPipeTransport = t.createClientPipeTransport = t.generateRandomPipeName = t.StreamMessageWriter = t.StreamMessageReader = t.SocketMessageWriter = t.SocketMessageReader = t.IPCMessageWriter = t.IPCMessageReader = void 0;
            const r = i(4472);
            r.default.install();
            const s = i(4539), a = i(1017), c = i(2037), l = i(6113), u = i(1808);
            o(i(4539), t);

            class d extends s.AbstractMessageReader {
                constructor(e) {
                    super(), this.process = e;
                    let t = this.process;
                    t.on("error", (e => this.fireError(e))), t.on("close", (() => this.fireClose()))
                }

                listen(e) {
                    return this.process.on("message", e), s.Disposable.create((() => this.process.off("message", e)))
                }
            }

            t.IPCMessageReader = d;

            class p extends s.AbstractMessageWriter {
                constructor(e) {
                    super(), this.process = e, this.errorCount = 0;
                    let t = this.process;
                    t.on("error", (e => this.fireError(e))), t.on("close", (() => this.fireClose))
                }

                write(e) {
                    try {
                        return "function" == typeof this.process.send && this.process.send(e, void 0, void 0, (t => {
                            t ? (this.errorCount++, this.handleError(t, e)) : this.errorCount = 0
                        })), Promise.resolve()
                    } catch (t) {
                        return this.handleError(t, e), Promise.reject(t)
                    }
                }

                handleError(e, t) {
                    this.errorCount++, this.fireError(e, t, this.errorCount)
                }

                end() {
                }
            }

            t.IPCMessageWriter = p;

            class h extends s.ReadableStreamMessageReader {
                constructor(e, t = "utf-8") {
                    super((0, r.default)().stream.asReadableStream(e), t)
                }
            }

            t.SocketMessageReader = h;

            class g extends s.WriteableStreamMessageWriter {
                constructor(e, t) {
                    super((0, r.default)().stream.asWritableStream(e), t), this.socket = e
                }

                dispose() {
                    super.dispose(), this.socket.destroy()
                }
            }

            t.SocketMessageWriter = g;

            class m extends s.ReadableStreamMessageReader {
                constructor(e, t) {
                    super((0, r.default)().stream.asReadableStream(e), t)
                }
            }

            t.StreamMessageReader = m;

            class f extends s.WriteableStreamMessageWriter {
                constructor(e, t) {
                    super((0, r.default)().stream.asWritableStream(e), t)
                }
            }

            t.StreamMessageWriter = f;
            const v = process.env.XDG_RUNTIME_DIR, y = new Map([["linux", 107], ["darwin", 103]]);
            t.generateRandomPipeName = function () {
                const e = (0, l.randomBytes)(21).toString("hex");
                if ("win32" === process.platform) return `\\\\.\\pipe\\vscode-jsonrpc-${e}-sock`;
                let t;
                t = v ? a.join(v, `vscode-ipc-${e}.sock`) : a.join(c.tmpdir(), `vscode-${e}.sock`);
                const i = y.get(process.platform);
                return void 0 !== i && t.length >= i && (0, r.default)().console.warn(`WARNING: IPC handle "${t}" is longer than ${i} characters.`), t
            }, t.createClientPipeTransport = function (e, t = "utf-8") {
                let i;
                const n = new Promise(((e, t) => {
                    i = e
                }));
                return new Promise(((o, r) => {
                    let s = (0, u.createServer)((e => {
                        s.close(), i([new h(e, t), new g(e, t)])
                    }));
                    s.on("error", r), s.listen(e, (() => {
                        s.removeListener("error", r), o({onConnected: () => n})
                    }))
                }))
            }, t.createServerPipeTransport = function (e, t = "utf-8") {
                const i = (0, u.createConnection)(e);
                return [new h(i, t), new g(i, t)]
            }, t.createClientSocketTransport = function (e, t = "utf-8") {
                let i;
                const n = new Promise(((e, t) => {
                    i = e
                }));
                return new Promise(((o, r) => {
                    const s = (0, u.createServer)((e => {
                        s.close(), i([new h(e, t), new g(e, t)])
                    }));
                    s.on("error", r), s.listen(e, "127.0.0.1", (() => {
                        s.removeListener("error", r), o({onConnected: () => n})
                    }))
                }))
            }, t.createServerSocketTransport = function (e, t = "utf-8") {
                const i = (0, u.createConnection)(e, "127.0.0.1");
                return [new h(i, t), new g(i, t)]
            }, t.createMessageConnection = function (e, t, i, n) {
                i || (i = s.NullLogger);
                const o = function (e) {
                    const t = e;
                    return void 0 !== t.read && void 0 !== t.addListener
                }(e) ? new m(e) : e, r = function (e) {
                    const t = e;
                    return void 0 !== t.write && void 0 !== t.addListener
                }(t) ? new f(t) : t;
                return s.ConnectionStrategy.is(n) && (n = {connectionStrategy: n}), (0, s.createMessageConnection)(o, r, i, n)
            }
        }, 4472: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0});
            const n = i(2182), o = i(3837), r = i(737), s = i(5377);

            class a extends s.AbstractMessageBuffer {
                constructor(e = "utf-8") {
                    super(e)
                }

                emptyBuffer() {
                    return a.emptyBuffer
                }

                fromString(e, t) {
                    return Buffer.from(e, t)
                }

                toString(e, t) {
                    return e instanceof Buffer ? e.toString(t) : new o.TextDecoder(t).decode(e)
                }

                asNative(e, t) {
                    return void 0 === t ? e instanceof Buffer ? e : Buffer.from(e) : e instanceof Buffer ? e.slice(0, t) : Buffer.from(e, 0, t)
                }

                allocNative(e) {
                    return Buffer.allocUnsafe(e)
                }
            }

            a.emptyBuffer = Buffer.allocUnsafe(0);

            class c {
                constructor(e) {
                    this.stream = e
                }

                onClose(e) {
                    return this.stream.on("close", e), r.Disposable.create((() => this.stream.off("close", e)))
                }

                onError(e) {
                    return this.stream.on("error", e), r.Disposable.create((() => this.stream.off("error", e)))
                }

                onEnd(e) {
                    return this.stream.on("end", e), r.Disposable.create((() => this.stream.off("end", e)))
                }

                onData(e) {
                    return this.stream.on("data", e), r.Disposable.create((() => this.stream.off("data", e)))
                }
            }

            class l {
                constructor(e) {
                    this.stream = e
                }

                onClose(e) {
                    return this.stream.on("close", e), r.Disposable.create((() => this.stream.off("close", e)))
                }

                onError(e) {
                    return this.stream.on("error", e), r.Disposable.create((() => this.stream.off("error", e)))
                }

                onEnd(e) {
                    return this.stream.on("end", e), r.Disposable.create((() => this.stream.off("end", e)))
                }

                write(e, t) {
                    return new Promise(((i, n) => {
                        const o = e => {
                            null == e ? i() : n(e)
                        };
                        "string" == typeof e ? this.stream.write(e, t, o) : this.stream.write(e, o)
                    }))
                }

                end() {
                    this.stream.end()
                }
            }

            const u = Object.freeze({
                messageBuffer: Object.freeze({create: e => new a(e)}),
                applicationJson: Object.freeze({
                    encoder: Object.freeze({
                        name: "application/json", encode: (e, t) => {
                            try {
                                return Promise.resolve(Buffer.from(JSON.stringify(e, void 0, 0), t.charset))
                            } catch (e) {
                                return Promise.reject(e)
                            }
                        }
                    }), decoder: Object.freeze({
                        name: "application/json", decode: (e, t) => {
                            try {
                                return e instanceof Buffer ? Promise.resolve(JSON.parse(e.toString(t.charset))) : Promise.resolve(JSON.parse(new o.TextDecoder(t.charset).decode(e)))
                            } catch (e) {
                                return Promise.reject(e)
                            }
                        }
                    })
                }),
                stream: Object.freeze({asReadableStream: e => new c(e), asWritableStream: e => new l(e)}),
                console,
                timer: Object.freeze({
                    setTimeout(e, t, ...i) {
                        const n = setTimeout(e, t, ...i);
                        return {dispose: () => clearTimeout(n)}
                    }, setImmediate(e, ...t) {
                        const i = setImmediate(e, ...t);
                        return {dispose: () => clearImmediate(i)}
                    }, setInterval(e, t, ...i) {
                        const n = setInterval(e, t, ...i);
                        return {dispose: () => clearInterval(n)}
                    }
                })
            });

            function d() {
                return u
            }

            !function (e) {
                e.install = function () {
                    n.default.install(u)
                }
            }(d || (d = {})), t.default = d
        }, 304: (e, t, i) => {
            "use strict";
            e.exports = i(8862)
        }, 6498: function (e, t, i) {
            "use strict";
            var n = this && this.__createBinding || (Object.create ? function (e, t, i, n) {
                void 0 === n && (n = i);
                var o = Object.getOwnPropertyDescriptor(t, i);
                o && !("get" in o ? !t.__esModule : o.writable || o.configurable) || (o = {
                    enumerable: !0,
                    get: function () {
                        return t[i]
                    }
                }), Object.defineProperty(e, n, o)
            } : function (e, t, i, n) {
                void 0 === n && (n = i), e[n] = t[i]
            }), o = this && this.__exportStar || function (e, t) {
                for (var i in e) "default" === i || Object.prototype.hasOwnProperty.call(t, i) || n(t, e, i)
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.DiagnosticPullMode = t.vsdiag = void 0, o(i(3455), t), o(i(5151), t);
            var r = i(5031);
            Object.defineProperty(t, "vsdiag", {
                enumerable: !0, get: function () {
                    return r.vsdiag
                }
            }), Object.defineProperty(t, "DiagnosticPullMode", {
                enumerable: !0, get: function () {
                    return r.DiagnosticPullMode
                }
            }), o(i(1912), t)
        }, 7849: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.CallHierarchyFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151);

            class s {
                constructor(e) {
                    this.client = e, this.middleware = e.middleware
                }

                prepareCallHierarchy(e, t, i) {
                    const n = this.client, r = this.middleware, s = (e, t, i) => {
                        const r = n.code2ProtocolConverter.asTextDocumentPositionParams(e, t);
                        return n.sendRequest(o.CallHierarchyPrepareRequest.type, r, i).then((e => i.isCancellationRequested ? null : n.protocol2CodeConverter.asCallHierarchyItems(e, i)), (e => n.handleFailedRequest(o.CallHierarchyPrepareRequest.type, i, e, null)))
                    };
                    return r.prepareCallHierarchy ? r.prepareCallHierarchy(e, t, i, s) : s(e, t, i)
                }

                provideCallHierarchyIncomingCalls(e, t) {
                    const i = this.client, n = this.middleware, r = (e, t) => {
                        const n = {item: i.code2ProtocolConverter.asCallHierarchyItem(e)};
                        return i.sendRequest(o.CallHierarchyIncomingCallsRequest.type, n, t).then((e => t.isCancellationRequested ? null : i.protocol2CodeConverter.asCallHierarchyIncomingCalls(e, t)), (e => i.handleFailedRequest(o.CallHierarchyIncomingCallsRequest.type, t, e, null)))
                    };
                    return n.provideCallHierarchyIncomingCalls ? n.provideCallHierarchyIncomingCalls(e, t, r) : r(e, t)
                }

                provideCallHierarchyOutgoingCalls(e, t) {
                    const i = this.client, n = this.middleware, r = (e, t) => {
                        const n = {item: i.code2ProtocolConverter.asCallHierarchyItem(e)};
                        return i.sendRequest(o.CallHierarchyOutgoingCallsRequest.type, n, t).then((e => t.isCancellationRequested ? null : i.protocol2CodeConverter.asCallHierarchyOutgoingCalls(e, t)), (e => i.handleFailedRequest(o.CallHierarchyOutgoingCallsRequest.type, t, e, null)))
                    };
                    return n.provideCallHierarchyOutgoingCalls ? n.provideCallHierarchyOutgoingCalls(e, t, r) : r(e, t)
                }
            }

            class a extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.CallHierarchyPrepareRequest.type)
                }

                fillClientCapabilities(e) {
                    const t = e;
                    (0, r.ensure)((0, r.ensure)(t, "textDocument"), "callHierarchy").dynamicRegistration = !0
                }

                initialize(e, t) {
                    const [i, n] = this.getRegistration(t, e.callHierarchyProvider);
                    i && n && this.register({id: i, registerOptions: n})
                }

                registerLanguageProvider(e) {
                    const t = this._client, i = new s(t);
                    return [n.languages.registerCallHierarchyProvider(this._client.protocol2CodeConverter.asDocumentSelector(e.documentSelector), i), i]
                }
            }

            t.CallHierarchyFeature = a
        }, 1912: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ProposedFeatures = t.BaseLanguageClient = t.MessageTransports = t.SuspendMode = t.State = t.CloseAction = t.ErrorAction = t.RevealOutputChannelOn = void 0;
            const n = i(9496), o = i(3455), r = i(9061), s = i(1761), a = i(2592), c = i(2570), l = i(2678), u = i(448),
                d = i(5151), p = i(5031), h = i(8565), g = i(1285), m = i(7686), f = i(5466), v = i(2293), y = i(1251),
                C = i(9779), w = i(7669), b = i(6157), k = i(5361), S = i(7101), D = i(3922), P = i(5003), T = i(4483),
                R = i(1996), _ = i(752), x = i(8392), E = i(7750), O = i(2648), j = i(4111), M = i(6372), I = i(6053),
                F = i(380), N = i(6381), q = i(3341), L = i(7800), A = i(7849), $ = i(6049), U = i(4373), H = i(2258),
                W = i(6769), V = i(7830), K = i(6790);
            var B, J, z, G, X, Y;
            !function (e) {
                e[e.Info = 1] = "Info", e[e.Warn = 2] = "Warn", e[e.Error = 3] = "Error", e[e.Never = 4] = "Never"
            }(B = t.RevealOutputChannelOn || (t.RevealOutputChannelOn = {})), function (e) {
                e[e.Continue = 1] = "Continue", e[e.Shutdown = 2] = "Shutdown"
            }(J = t.ErrorAction || (t.ErrorAction = {})), function (e) {
                e[e.DoNotRestart = 1] = "DoNotRestart", e[e.Restart = 2] = "Restart"
            }(z = t.CloseAction || (t.CloseAction = {})), function (e) {
                e[e.Stopped = 1] = "Stopped", e[e.Starting = 3] = "Starting", e[e.Running = 2] = "Running"
            }(G = t.State || (t.State = {})), function (e) {
                e.off = "off", e.on = "on"
            }(t.SuspendMode || (t.SuspendMode = {}));

            class Q {
                constructor(e, t) {
                    this.client = e, this.maxRestartCount = t, this.restarts = []
                }

                error(e, t, i) {
                    return i && i <= 3 ? {action: J.Continue} : {action: J.Shutdown}
                }

                closed() {
                    if (this.restarts.push(Date.now()), this.restarts.length <= this.maxRestartCount) return {action: z.Restart};
                    return this.restarts[this.restarts.length - 1] - this.restarts[0] <= 18e4 ? {
                        action: z.DoNotRestart,
                        message: `The ${this.client.name} server crashed ${this.maxRestartCount + 1} times in the last 3 minutes. The server will not be restarted. See the output for more information.`
                    } : (this.restarts.shift(), {action: z.Restart})
                }
            }

            !function (e) {
                e.Initial = "initial", e.Starting = "starting", e.StartFailed = "startFailed", e.Running = "running", e.Stopping = "stopping", e.Stopped = "stopped"
            }(X || (X = {})), function (e) {
                e.is = function (e) {
                    return e && o.MessageReader.is(e.reader) && o.MessageWriter.is(e.writer)
                }
            }(t.MessageTransports || (t.MessageTransports = {}));

            class Z {
                constructor(e, t, i) {
                    this._traceFormat = o.TraceFormat.Text, this._diagnosticQueue = new Map, this._diagnosticQueueState = {state: "idle"}, this._features = [], this._dynamicFeatures = new Map, this.workspaceEditLock = new c.Semaphore(1), this._id = e, this._name = t;
                    const n = {isTrusted: !1, supportHtml: !1};
                    void 0 !== (i = i || {}).markdown && (n.isTrusted = !0 === i.markdown.isTrusted, n.supportHtml = !0 === i.markdown.supportHtml), this._clientOptions = {
                        documentSelector: i.documentSelector ?? [],
                        synchronize: i.synchronize ?? {},
                        diagnosticCollectionName: i.diagnosticCollectionName,
                        outputChannelName: i.outputChannelName ?? this._name,
                        revealOutputChannelOn: i.revealOutputChannelOn ?? B.Error,
                        stdioEncoding: i.stdioEncoding ?? "utf8",
                        initializationOptions: i.initializationOptions,
                        initializationFailedHandler: i.initializationFailedHandler,
                        progressOnInitialization: !!i.progressOnInitialization,
                        errorHandler: i.errorHandler ?? this.createDefaultErrorHandler(i.connectionOptions?.maxRestartCount),
                        middleware: i.middleware ?? {},
                        uriConverters: i.uriConverters,
                        workspaceFolder: i.workspaceFolder,
                        connectionOptions: i.connectionOptions,
                        markdown: n,
                        diagnosticPullOptions: i.diagnosticPullOptions ?? {onChange: !0, onSave: !1},
                        notebookDocumentOptions: i.notebookDocumentOptions ?? {}
                    }, this._clientOptions.synchronize = this._clientOptions.synchronize || {}, this._state = X.Initial, this._ignoredRegistrations = new Set, this._listeners = [], this._notificationHandlers = new Map, this._pendingNotificationHandlers = new Map, this._notificationDisposables = new Map, this._requestHandlers = new Map, this._pendingRequestHandlers = new Map, this._requestDisposables = new Map, this._progressHandlers = new Map, this._pendingProgressHandlers = new Map, this._progressDisposables = new Map, this._connection = void 0, this._initializeResult = void 0, i.outputChannel ? (this._outputChannel = i.outputChannel, this._disposeOutputChannel = !1) : (this._outputChannel = void 0, this._disposeOutputChannel = !0), this._traceOutputChannel = i.traceOutputChannel, this._diagnostics = void 0, this._fileEvents = [], this._fileEventDelayer = new c.Delayer(250), this._onStop = void 0, this._telemetryEmitter = new o.Emitter, this._stateChangeEmitter = new o.Emitter, this._trace = o.Trace.Off, this._tracer = {
                        log: (e, t) => {
                            a.string(e) ? this.logTrace(e, t) : this.logObjectTrace(e)
                        }
                    }, this._c2p = r.createConverter(i.uriConverters ? i.uriConverters.code2Protocol : void 0), this._p2c = s.createConverter(i.uriConverters ? i.uriConverters.protocol2Code : void 0, this._clientOptions.markdown.isTrusted, this._clientOptions.markdown.supportHtml), this._syncedDocuments = new Map, this.registerBuiltinFeatures()
                }

                get name() {
                    return this._name
                }

                get middleware() {
                    return this._clientOptions.middleware ?? Object.create(null)
                }

                get clientOptions() {
                    return this._clientOptions
                }

                get protocol2CodeConverter() {
                    return this._p2c
                }

                get code2ProtocolConverter() {
                    return this._c2p
                }

                get onTelemetry() {
                    return this._telemetryEmitter.event
                }

                get onDidChangeState() {
                    return this._stateChangeEmitter.event
                }

                get outputChannel() {
                    return this._outputChannel || (this._outputChannel = n.window.createOutputChannel(this._clientOptions.outputChannelName ? this._clientOptions.outputChannelName : this._name)), this._outputChannel
                }

                get traceOutputChannel() {
                    return this._traceOutputChannel ? this._traceOutputChannel : this.outputChannel
                }

                get diagnostics() {
                    return this._diagnostics
                }

                get state() {
                    return this.getPublicState()
                }

                get $state() {
                    return this._state
                }

                set $state(e) {
                    let t = this.getPublicState();
                    this._state = e;
                    let i = this.getPublicState();
                    i !== t && this._stateChangeEmitter.fire({oldState: t, newState: i})
                }

                getPublicState() {
                    switch (this.$state) {
                        case X.Starting:
                            return G.Starting;
                        case X.Running:
                            return G.Running;
                        default:
                            return G.Stopped
                    }
                }

                get initializeResult() {
                    return this._initializeResult
                }

                async sendRequest(e, ...t) {
                    if (this.$state === X.StartFailed || this.$state === X.Stopping || this.$state === X.Stopped) return Promise.reject(new o.ResponseError(o.ErrorCodes.ConnectionInactive, "Client is not running"));
                    try {
                        const i = await this.$start();
                        return await this.forceDocumentSync(), i.sendRequest(e, ...t)
                    } catch (t) {
                        throw this.error(`Sending request ${a.string(e) ? e : e.method} failed.`, t), t
                    }
                }

                onRequest(e, t) {
                    const i = "string" == typeof e ? e : e.method;
                    this._requestHandlers.set(i, t);
                    const n = this.activeConnection();
                    let o;
                    return void 0 !== n ? (this._requestDisposables.set(i, n.onRequest(e, t)), o = {
                        dispose: () => {
                            const e = this._requestDisposables.get(i);
                            void 0 !== e && (e.dispose(), this._requestDisposables.delete(i))
                        }
                    }) : (this._pendingRequestHandlers.set(i, t), o = {
                        dispose: () => {
                            this._pendingRequestHandlers.delete(i);
                            const e = this._requestDisposables.get(i);
                            void 0 !== e && (e.dispose(), this._requestDisposables.delete(i))
                        }
                    }), {
                        dispose: () => {
                            this._requestHandlers.delete(i), o.dispose()
                        }
                    }
                }

                async sendNotification(e, t) {
                    if (this.$state === X.StartFailed || this.$state === X.Stopping || this.$state === X.Stopped) return Promise.reject(new o.ResponseError(o.ErrorCodes.ConnectionInactive, "Client is not running"));
                    try {
                        const i = await this.$start();
                        return await this.forceDocumentSync(), i.sendNotification(e, t)
                    } catch (t) {
                        throw this.error(`Sending notification ${a.string(e) ? e : e.method} failed.`, t), t
                    }
                }

                onNotification(e, t) {
                    const i = "string" == typeof e ? e : e.method;
                    this._notificationHandlers.set(i, t);
                    const n = this.activeConnection();
                    let o;
                    return void 0 !== n ? (this._notificationDisposables.set(i, n.onNotification(e, t)), o = {
                        dispose: () => {
                            const e = this._notificationDisposables.get(i);
                            void 0 !== e && (e.dispose(), this._notificationDisposables.delete(i))
                        }
                    }) : (this._pendingNotificationHandlers.set(i, t), o = {
                        dispose: () => {
                            this._pendingNotificationHandlers.delete(i);
                            const e = this._notificationDisposables.get(i);
                            void 0 !== e && (e.dispose(), this._notificationDisposables.delete(i))
                        }
                    }), {
                        dispose: () => {
                            this._notificationHandlers.delete(i), o.dispose()
                        }
                    }
                }

                async sendProgress(e, t, i) {
                    if (this.$state === X.StartFailed || this.$state === X.Stopping || this.$state === X.Stopped) return Promise.reject(new o.ResponseError(o.ErrorCodes.ConnectionInactive, "Client is not running"));
                    try {
                        return (await this.$start()).sendProgress(e, t, i)
                    } catch (e) {
                        throw this.error(`Sending progress for token ${t} failed.`, e), e
                    }
                }

                onProgress(e, t, i) {
                    this._progressHandlers.set(t, {type: e, handler: i});
                    const n = this.activeConnection();
                    let r;
                    const s = this._clientOptions.middleware?.handleWorkDoneProgress,
                        a = o.WorkDoneProgress.is(e) && void 0 !== s ? e => {
                            s(t, e, (() => i(e)))
                        } : i;
                    return void 0 !== n ? (this._progressDisposables.set(t, n.onProgress(e, t, a)), r = {
                        dispose: () => {
                            const e = this._progressDisposables.get(t);
                            void 0 !== e && (e.dispose(), this._progressDisposables.delete(t))
                        }
                    }) : (this._pendingProgressHandlers.set(t, {type: e, handler: i}), r = {
                        dispose: () => {
                            this._pendingProgressHandlers.delete(t);
                            const e = this._progressDisposables.get(t);
                            void 0 !== e && (e.dispose(), this._progressDisposables.delete(t))
                        }
                    }), {
                        dispose: () => {
                            this._progressHandlers.delete(t), r.dispose()
                        }
                    }
                }

                createDefaultErrorHandler(e) {
                    if (void 0 !== e && e < 0) throw new Error(`Invalid maxRestartCount: ${e}`);
                    return new Q(this, e ?? 4)
                }

                async setTrace(e) {
                    this._trace = e;
                    const t = this.activeConnection();
                    void 0 !== t && await t.trace(this._trace, this._tracer, {
                        sendNotification: !1,
                        traceFormat: this._traceFormat
                    })
                }

                data2String(e) {
                    if (e instanceof o.ResponseError) {
                        const t = e;
                        return `  Message: ${t.message}\n  Code: ${t.code} ${t.data ? "\n" + t.data.toString() : ""}`
                    }
                    return e instanceof Error ? a.string(e.stack) ? e.stack : e.message : a.string(e) ? e : e.toString()
                }

                info(e, t, i = !0) {
                    this.outputChannel.appendLine(`[Info  - ${(new Date).toLocaleTimeString()}] ${e}`), null != t && this.outputChannel.appendLine(this.data2String(t)), i && this._clientOptions.revealOutputChannelOn <= B.Info && this.showNotificationMessage(o.MessageType.Info, e)
                }

                warn(e, t, i = !0) {
                    this.outputChannel.appendLine(`[Warn  - ${(new Date).toLocaleTimeString()}] ${e}`), null != t && this.outputChannel.appendLine(this.data2String(t)), i && this._clientOptions.revealOutputChannelOn <= B.Warn && this.showNotificationMessage(o.MessageType.Warning, e)
                }

                error(e, t, i = !0) {
                    this.outputChannel.appendLine(`[Error - ${(new Date).toLocaleTimeString()}] ${e}`), null != t && this.outputChannel.appendLine(this.data2String(t)), ("force" === i || i && this._clientOptions.revealOutputChannelOn <= B.Error) && this.showNotificationMessage(o.MessageType.Error, e)
                }

                showNotificationMessage(e, t) {
                    t = t ?? "A request has failed. See the output for more information.";
                    (e === o.MessageType.Error ? n.window.showErrorMessage : e === o.MessageType.Warning ? n.window.showWarningMessage : n.window.showInformationMessage)(t, "Go to output").then((e => {
                        void 0 !== e && this.outputChannel.show(!0)
                    }))
                }

                logTrace(e, t) {
                    this.traceOutputChannel.appendLine(`[Trace - ${(new Date).toLocaleTimeString()}] ${e}`), t && this.traceOutputChannel.appendLine(this.data2String(t))
                }

                logObjectTrace(e) {
                    e.isLSPMessage && e.type ? this.traceOutputChannel.append(`[LSP   - ${(new Date).toLocaleTimeString()}] `) : this.traceOutputChannel.append(`[Trace - ${(new Date).toLocaleTimeString()}] `), e && this.traceOutputChannel.appendLine(`${JSON.stringify(e)}`)
                }

                needsStart() {
                    return this.$state === X.Initial || this.$state === X.Stopping || this.$state === X.Stopped
                }

                needsStop() {
                    return this.$state === X.Starting || this.$state === X.Running
                }

                activeConnection() {
                    return this.$state === X.Running && void 0 !== this._connection ? this._connection : void 0
                }

                isRunning() {
                    return this.$state === X.Running
                }

                async start() {
                    if ("disposing" === this._disposed || "disposed" === this._disposed) throw new Error("Client got disposed and can't be restarted.");
                    if (this.$state === X.Stopping) throw new Error("Client is currently stopping. Can only restart a full stopped client");
                    if (void 0 !== this._onStart) return this._onStart;
                    const [e, t, i] = this.createOnStartPromise();
                    this._onStart = e, void 0 === this._diagnostics && (this._diagnostics = this._clientOptions.diagnosticCollectionName ? n.languages.createDiagnosticCollection(this._clientOptions.diagnosticCollectionName) : n.languages.createDiagnosticCollection());
                    for (const [e, t] of this._notificationHandlers) this._pendingNotificationHandlers.has(e) || this._pendingNotificationHandlers.set(e, t);
                    for (const [e, t] of this._requestHandlers) this._pendingRequestHandlers.has(e) || this._pendingRequestHandlers.set(e, t);
                    for (const [e, t] of this._progressHandlers) this._pendingProgressHandlers.has(e) || this._pendingProgressHandlers.set(e, t);
                    this.$state = X.Starting;
                    try {
                        const e = await this.createConnection();
                        e.onNotification(o.LogMessageNotification.type, (e => {
                            switch (e.type) {
                                case o.MessageType.Error:
                                    this.error(e.message, void 0, !1);
                                    break;
                                case o.MessageType.Warning:
                                    this.warn(e.message, void 0, !1);
                                    break;
                                case o.MessageType.Info:
                                    this.info(e.message, void 0, !1);
                                    break;
                                default:
                                    this.outputChannel.appendLine(e.message)
                            }
                        })), e.onNotification(o.ShowMessageNotification.type, (e => {
                            switch (e.type) {
                                case o.MessageType.Error:
                                    n.window.showErrorMessage(e.message);
                                    break;
                                case o.MessageType.Warning:
                                    n.window.showWarningMessage(e.message);
                                    break;
                                case o.MessageType.Info:
                                default:
                                    n.window.showInformationMessage(e.message)
                            }
                        })), e.onRequest(o.ShowMessageRequest.type, (e => {
                            let t;
                            switch (e.type) {
                                case o.MessageType.Error:
                                    t = n.window.showErrorMessage;
                                    break;
                                case o.MessageType.Warning:
                                    t = n.window.showWarningMessage;
                                    break;
                                case o.MessageType.Info:
                                default:
                                    t = n.window.showInformationMessage
                            }
                            let i = e.actions || [];
                            return t(e.message, ...i)
                        })), e.onNotification(o.TelemetryEventNotification.type, (e => {
                            this._telemetryEmitter.fire(e)
                        })), e.onRequest(o.ShowDocumentRequest.type, (async e => {
                            const t = async e => {
                                const t = this.protocol2CodeConverter.asUri(e.uri);
                                try {
                                    if (!0 === e.external) {
                                        return {success: await n.env.openExternal(t)}
                                    }
                                    {
                                        const i = {};
                                        return void 0 !== e.selection && (i.selection = this.protocol2CodeConverter.asRange(e.selection)), void 0 === e.takeFocus || !1 === e.takeFocus ? i.preserveFocus = !0 : !0 === e.takeFocus && (i.preserveFocus = !1), await n.window.showTextDocument(t, i), {success: !0}
                                    }
                                } catch (e) {
                                    return {success: !1}
                                }
                            }, i = this._clientOptions.middleware.window?.showDocument;
                            return void 0 !== i ? i(e, t) : t(e)
                        })), e.listen(), await this.initialize(e), t()
                    } catch (e) {
                        this.$state = X.StartFailed, this.error(`${this._name} client: couldn't create connection to server.`, e, "force"), i(e)
                    }
                    return this._onStart
                }

                createOnStartPromise() {
                    let e, t;
                    return [new Promise(((i, n) => {
                        e = i, t = n
                    })), e, t]
                }

                async initialize(e) {
                    this.refreshTrace(e, !1);
                    const t = this._clientOptions.initializationOptions, [i, r] = void 0 !== this._clientOptions.workspaceFolder ? [this._clientOptions.workspaceFolder.uri.fsPath, [{
                        uri: this._c2p.asUri(this._clientOptions.workspaceFolder.uri),
                        name: this._clientOptions.workspaceFolder.name
                    }]] : [this._clientGetRootPath(), null], s = {
                        processId: null,
                        clientInfo: {name: n.env.appName, version: n.version},
                        locale: this.getLocale(),
                        rootPath: i || null,
                        rootUri: i ? this._c2p.asUri(n.Uri.file(i)) : null,
                        capabilities: this.computeClientCapabilities(),
                        initializationOptions: a.func(t) ? t() : t,
                        trace: o.Trace.toString(this._trace),
                        workspaceFolders: r
                    };
                    if (this.fillInitializeParams(s), !this._clientOptions.progressOnInitialization) return this.doInitialize(e, s);
                    {
                        const t = l.generateUuid(), i = new u.ProgressPart(e, t);
                        s.workDoneToken = t;
                        try {
                            const t = await this.doInitialize(e, s);
                            return i.done(), t
                        } catch (e) {
                            throw i.cancel(), e
                        }
                    }
                }

                async doInitialize(e, t) {
                    try {
                        const i = await e.initialize(t);
                        if (void 0 !== i.capabilities.positionEncoding && i.capabilities.positionEncoding !== o.PositionEncodingKind.UTF16) throw new Error(`Unsupported position encoding (${i.capabilities.positionEncoding}) received from server ${this.name}`);
                        let n;
                        this._initializeResult = i, this.$state = X.Running, a.number(i.capabilities.textDocumentSync) ? n = i.capabilities.textDocumentSync === o.TextDocumentSyncKind.None ? {
                            openClose: !1,
                            change: o.TextDocumentSyncKind.None,
                            save: void 0
                        } : {
                            openClose: !0,
                            change: i.capabilities.textDocumentSync,
                            save: {includeText: !1}
                        } : void 0 !== i.capabilities.textDocumentSync && null !== i.capabilities.textDocumentSync && (n = i.capabilities.textDocumentSync), this._capabilities = Object.assign({}, i.capabilities, {resolvedTextDocumentSync: n}), e.onNotification(o.PublishDiagnosticsNotification.type, (e => this.handleDiagnostics(e))), e.onRequest(o.RegistrationRequest.type, (e => this.handleRegistrationRequest(e))), e.onRequest("client/registerFeature", (e => this.handleRegistrationRequest(e))), e.onRequest(o.UnregistrationRequest.type, (e => this.handleUnregistrationRequest(e))), e.onRequest("client/unregisterFeature", (e => this.handleUnregistrationRequest(e))), e.onRequest(o.ApplyWorkspaceEditRequest.type, (e => this.handleApplyWorkspaceEdit(e)));
                        for (const [t, i] of this._pendingNotificationHandlers) this._notificationDisposables.set(t, e.onNotification(t, i));
                        this._pendingNotificationHandlers.clear();
                        for (const [t, i] of this._pendingRequestHandlers) this._requestDisposables.set(t, e.onRequest(t, i));
                        this._pendingRequestHandlers.clear();
                        for (const [t, i] of this._pendingProgressHandlers) this._progressDisposables.set(t, e.onProgress(i.type, t, i.handler));
                        return this._pendingProgressHandlers.clear(), await e.sendNotification(o.InitializedNotification.type, {}), this.hookFileEvents(e), this.hookConfigurationChanged(e), this.initializeFeatures(e), i
                    } catch (t) {
                        throw this._clientOptions.initializationFailedHandler ? this._clientOptions.initializationFailedHandler(t) ? this.initialize(e) : this.stop() : t instanceof o.ResponseError && t.data && t.data.retry ? n.window.showErrorMessage(t.message, {
                            title: "Retry",
                            id: "retry"
                        }).then((t => {
                            t && "retry" === t.id ? this.initialize(e) : this.stop()
                        })) : (t && t.message && n.window.showErrorMessage(t.message), this.error("Server initialization failed.", t), this.stop()), t
                    }
                }

                _clientGetRootPath() {
                    let e = n.workspace.workspaceFolders;
                    if (!e || 0 === e.length) return;
                    let t = e[0];
                    return "file" === t.uri.scheme ? t.uri.fsPath : void 0
                }

                stop(e = 2e3) {
                    return this.shutdown("stop", e)
                }

                dispose(e = 2e3) {
                    try {
                        return this._disposed = "disposing", this.stop(e)
                    } finally {
                        this._disposed = "disposed"
                    }
                }

                async shutdown(e, t) {
                    if (this.$state === X.Stopped || this.$state === X.Initial) return;
                    if (this.$state === X.Stopping) {
                        if (void 0 !== this._onStop) return this._onStop;
                        throw new Error("Client is stopping but no stop promise available.")
                    }
                    const i = this.activeConnection();
                    if (void 0 === i || this.$state !== X.Running) throw new Error(`Client is not running and can't be stopped. It's current state is: ${this.$state}`);
                    this._initializeResult = void 0, this.$state = X.Stopping, this.cleanUp(e);
                    const n = new Promise((e => {
                        (0, o.RAL)().timer.setTimeout(e, t)
                    })), r = (async e => (await e.shutdown(), await e.exit(), e))(i);
                    return this._onStop = Promise.race([n, r]).then((e => {
                        if (void 0 === e) throw this.error("Stopping server timed out", void 0, !1), new Error("Stopping the server timed out");
                        e.end(), e.dispose()
                    }), (e => {
                        throw this.error("Stopping server failed", e, !1), e
                    })).finally((() => {
                        this.$state = X.Stopped, "stop" === e && this.cleanUpChannel(), this._onStart = void 0, this._onStop = void 0, this._connection = void 0, this._ignoredRegistrations.clear()
                    }))
                }

                cleanUp(e) {
                    this._fileEvents = [], this._fileEventDelayer.cancel();
                    const t = this._listeners.splice(0, this._listeners.length);
                    for (const e of t) e.dispose();
                    this._syncedDocuments && this._syncedDocuments.clear();
                    for (const e of Array.from(this._features.entries()).map((e => e[1])).reverse()) e.dispose();
                    "stop" === e && void 0 !== this._diagnostics && (this._diagnostics.dispose(), this._diagnostics = void 0), void 0 !== this._idleInterval && (this._idleInterval.dispose(), this._idleInterval = void 0)
                }

                cleanUpChannel() {
                    void 0 !== this._outputChannel && this._disposeOutputChannel && (this._outputChannel.dispose(), this._outputChannel = void 0)
                }

                notifyFileEvent(e) {
                    const t = this;

                    async function i(e) {
                        return t._fileEvents.push(e), t._fileEventDelayer.trigger((async () => {
                            const e = await t.$start();
                            await t.forceDocumentSync();
                            const i = e.sendNotification(o.DidChangeWatchedFilesNotification.type, {changes: t._fileEvents});
                            return t._fileEvents = [], i
                        }))
                    }

                    const n = this.clientOptions.middleware?.workspace;
                    (n?.didChangeWatchedFile ? n.didChangeWatchedFile(e, i) : i(e)).catch((e => {
                        t.error("Notify file events failed.", e)
                    }))
                }

                async forceDocumentSync() {
                    return void 0 === this._didChangeTextDocumentFeature && (this._didChangeTextDocumentFeature = this._dynamicFeatures.get(o.DidChangeTextDocumentNotification.type.method)), this._didChangeTextDocumentFeature.forceDelivery()
                }

                handleDiagnostics(e) {
                    if (!this._diagnostics) return;
                    const t = e.uri;
                    "busy" === this._diagnosticQueueState.state && this._diagnosticQueueState.document === t && this._diagnosticQueueState.tokenSource.cancel(), this._diagnosticQueue.set(e.uri, e.diagnostics), this.triggerDiagnosticQueue()
                }

                triggerDiagnosticQueue() {
                    (0, o.RAL)().timer.setImmediate((() => {
                        this.workDiagnosticQueue()
                    }))
                }

                workDiagnosticQueue() {
                    if ("busy" === this._diagnosticQueueState.state) return;
                    const e = this._diagnosticQueue.entries().next();
                    if (!0 === e.done) return;
                    const [t, i] = e.value;
                    this._diagnosticQueue.delete(t);
                    const o = new n.CancellationTokenSource;
                    this._diagnosticQueueState = {
                        state: "busy",
                        document: t,
                        tokenSource: o
                    }, this._p2c.asDiagnostics(i, o.token).then((e => {
                        if (!o.token.isCancellationRequested) {
                            const i = this._p2c.asUri(t), n = this.clientOptions.middleware;
                            n.handleDiagnostics ? n.handleDiagnostics(i, e, ((e, t) => this.setDiagnostics(e, t))) : this.setDiagnostics(i, e)
                        }
                    })).finally((() => {
                        this._diagnosticQueueState = {state: "idle"}, this.triggerDiagnosticQueue()
                    }))
                }

                setDiagnostics(e, t) {
                    this._diagnostics && this._diagnostics.set(e, t)
                }

                async $start() {
                    if (this.$state === X.StartFailed) throw new Error("Previous start failed. Can't restart server.");
                    await this.start();
                    const e = this.activeConnection();
                    if (void 0 === e) throw new Error("Starting server failed");
                    return e
                }

                async createConnection() {
                    const e = await this.createMessageTransports(this._clientOptions.stdioEncoding || "utf8");
                    return this._connection = function (e, t, i, n, r) {
                        let s = -1;
                        const c = new ee, l = (0, o.createProtocolConnection)(e, t, c, r);
                        l.onError((e => {
                            i(e[0], e[1], e[2])
                        })), l.onClose(n);
                        return {
                            get lastUsed() {
                                return s
                            },
                            resetLastUsed: () => {
                                s = -1
                            },
                            listen: () => l.listen(),
                            sendRequest: (e, ...t) => (s = Date.now(), l.sendRequest(e, ...t)),
                            onRequest: (e, t) => l.onRequest(e, t),
                            hasPendingResponse: () => l.hasPendingResponse(),
                            sendNotification: (e, t) => (s = Date.now(), l.sendNotification(e, t)),
                            onNotification: (e, t) => l.onNotification(e, t),
                            onProgress: l.onProgress,
                            sendProgress: l.sendProgress,
                            trace: (e, t, i) => {
                                const n = {sendNotification: !1, traceFormat: o.TraceFormat.Text};
                                return void 0 === i ? l.trace(e, t, n) : (a.boolean(i), l.trace(e, t, i))
                            },
                            initialize: e => (s = Date.now(), l.sendRequest(o.InitializeRequest.type, e)),
                            shutdown: () => (s = Date.now(), l.sendRequest(o.ShutdownRequest.type, void 0)),
                            exit: () => (s = Date.now(), l.sendNotification(o.ExitNotification.type)),
                            end: () => l.end(),
                            dispose: () => l.dispose()
                        }
                    }(e.reader, e.writer, ((e, t, i) => {
                        this.handleConnectionError(e, t, i)
                    }), (() => {
                        this.handleConnectionClosed()
                    }), this._clientOptions.connectionOptions), this._connection
                }

                handleConnectionClosed() {
                    if (this.$state === X.Stopped) return;
                    try {
                        void 0 !== this._connection && this._connection.dispose()
                    } catch (e) {
                    }
                    let e = {action: z.DoNotRestart};
                    if (this.$state !== X.Stopping) try {
                        e = this._clientOptions.errorHandler.closed()
                    } catch (e) {
                    }
                    this._connection = void 0, e.action === z.DoNotRestart ? (this.error(e.message ?? "Connection to server got closed. Server will not be restarted.", void 0, "force"), this.cleanUp("stop"), this.$state === X.Starting ? this.$state = X.StartFailed : this.$state = X.Stopped, this._onStop = Promise.resolve(), this._onStart = void 0) : e.action === z.Restart && (this.info(e.message ?? "Connection to server got closed. Server will restart."), this.cleanUp("restart"), this.$state = X.Initial, this._onStop = Promise.resolve(), this._onStart = void 0, this.start().catch((e => this.error("Restarting server failed", e, "force"))))
                }

                handleConnectionError(e, t, i) {
                    const n = this._clientOptions.errorHandler.error(e, t, i);
                    n.action === J.Shutdown && (this.error(n.message ?? `Client ${this._name}: connection to server is erroring. Shutting down server.`, void 0, "force"), this.stop().catch((e => {
                        this.error("Stopping server failed", e, !1)
                    })))
                }

                hookConfigurationChanged(e) {
                    this._listeners.push(n.workspace.onDidChangeConfiguration((() => {
                        this.refreshTrace(e, !0)
                    })))
                }

                refreshTrace(e, t = !1) {
                    const i = n.workspace.getConfiguration(this._id);
                    let r = o.Trace.Off, s = o.TraceFormat.Text;
                    if (i) {
                        const e = i.get("trace.server", "off");
                        "string" == typeof e ? r = o.Trace.fromString(e) : (r = o.Trace.fromString(i.get("trace.server.verbosity", "off")), s = o.TraceFormat.fromString(i.get("trace.server.format", "text")))
                    }
                    this._trace = r, this._traceFormat = s, e.trace(this._trace, this._tracer, {
                        sendNotification: t,
                        traceFormat: this._traceFormat
                    }).catch((e => {
                        this.error("Updating trace failed with error", e, !1)
                    }))
                }

                hookFileEvents(e) {
                    let t, i = this._clientOptions.synchronize.fileEvents;
                    i && (t = a.array(i) ? i : [i], t && this._dynamicFeatures.get(o.DidChangeWatchedFilesNotification.type.method).registerRaw(l.generateUuid(), t))
                }

                registerFeatures(e) {
                    for (let t of e) this.registerFeature(t)
                }

                registerFeature(e) {
                    if (this._features.push(e), d.DynamicFeature.is(e)) {
                        const t = e.registrationType;
                        this._dynamicFeatures.set(t.method, e)
                    }
                }

                getFeature(e) {
                    return this._dynamicFeatures.get(e)
                }

                hasDedicatedTextSynchronizationFeature(e) {
                    const t = this.getFeature(o.NotebookDocumentSyncRegistrationType.method);
                    return void 0 !== t && t instanceof h.NotebookDocumentSyncFeature && t.handles(e)
                }

                registerBuiltinFeatures() {
                    this.registerFeature(new g.ConfigurationFeature(this)), this.registerFeature(new m.DidOpenTextDocumentFeature(this, this._syncedDocuments)), this.registerFeature(new m.DidChangeTextDocumentFeature(this)), this.registerFeature(new m.WillSaveFeature(this)), this.registerFeature(new m.WillSaveWaitUntilFeature(this)), this.registerFeature(new m.DidSaveTextDocumentFeature(this)), this.registerFeature(new m.DidCloseTextDocumentFeature(this, this._syncedDocuments)), this.registerFeature(new E.FileSystemWatcherFeature(this, (e => this.notifyFileEvent(e)))), this.registerFeature(new f.CompletionItemFeature(this)), this.registerFeature(new v.HoverFeature(this)), this.registerFeature(new C.SignatureHelpFeature(this)), this.registerFeature(new y.DefinitionFeature(this)), this.registerFeature(new S.ReferencesFeature(this)), this.registerFeature(new w.DocumentHighlightFeature(this)), this.registerFeature(new b.DocumentSymbolFeature(this)), this.registerFeature(new k.WorkspaceSymbolFeature(this)), this.registerFeature(new D.CodeActionFeature(this)), this.registerFeature(new P.CodeLensFeature(this)), this.registerFeature(new T.DocumentFormattingFeature(this)), this.registerFeature(new T.DocumentRangeFormattingFeature(this)), this.registerFeature(new T.DocumentOnTypeFormattingFeature(this)), this.registerFeature(new R.RenameFeature(this)), this.registerFeature(new _.DocumentLinkFeature(this)), this.registerFeature(new x.ExecuteCommandFeature(this)), this.registerFeature(new g.SyncConfigurationFeature(this)), this.registerFeature(new M.TypeDefinitionFeature(this)), this.registerFeature(new j.ImplementationFeature(this)), this.registerFeature(new O.ColorProviderFeature(this)), void 0 === this.clientOptions.workspaceFolder && this.registerFeature(new I.WorkspaceFoldersFeature(this)), this.registerFeature(new F.FoldingRangeFeature(this)), this.registerFeature(new N.DeclarationFeature(this)), this.registerFeature(new q.SelectionRangeFeature(this)), this.registerFeature(new L.ProgressFeature(this)), this.registerFeature(new A.CallHierarchyFeature(this)), this.registerFeature(new $.SemanticTokensFeature(this)), this.registerFeature(new H.LinkedEditingFeature(this)), this.registerFeature(new U.DidCreateFilesFeature(this)), this.registerFeature(new U.DidRenameFilesFeature(this)), this.registerFeature(new U.DidDeleteFilesFeature(this)), this.registerFeature(new U.WillCreateFilesFeature(this)), this.registerFeature(new U.WillRenameFilesFeature(this)), this.registerFeature(new U.WillDeleteFilesFeature(this)), this.registerFeature(new W.TypeHierarchyFeature(this)), this.registerFeature(new V.InlineValueFeature(this)), this.registerFeature(new K.InlayHintsFeature(this)), this.registerFeature(new p.DiagnosticFeature(this)), this.registerFeature(new h.NotebookDocumentSyncFeature(this))
                }

                registerProposedFeatures() {
                    this.registerFeatures(Y.createAll(this))
                }

                fillInitializeParams(e) {
                    for (let t of this._features) a.func(t.fillInitializeParams) && t.fillInitializeParams(e)
                }

                computeClientCapabilities() {
                    const e = {};
                    (0, d.ensure)(e, "workspace").applyEdit = !0;
                    const t = (0, d.ensure)((0, d.ensure)(e, "workspace"), "workspaceEdit");
                    t.documentChanges = !0, t.resourceOperations = [o.ResourceOperationKind.Create, o.ResourceOperationKind.Rename, o.ResourceOperationKind.Delete], t.failureHandling = o.FailureHandlingKind.TextOnlyTransactional, t.normalizesLineEndings = !0, t.changeAnnotationSupport = {groupsOnLabel: !0};
                    const i = (0, d.ensure)((0, d.ensure)(e, "textDocument"), "publishDiagnostics");
                    i.relatedInformation = !0, i.versionSupport = !1, i.tagSupport = {valueSet: [o.DiagnosticTag.Unnecessary, o.DiagnosticTag.Deprecated]}, i.codeDescriptionSupport = !0, i.dataSupport = !0;
                    const n = (0, d.ensure)(e, "window");
                    (0, d.ensure)(n, "showMessage").messageActionItem = {additionalPropertiesSupport: !0};
                    (0, d.ensure)(n, "showDocument").support = !0;
                    const r = (0, d.ensure)(e, "general");
                    r.staleRequestSupport = {
                        cancel: !0,
                        retryOnContentModified: Array.from(Z.RequestsToCancelOnContentModified)
                    }, r.regularExpressions = {engine: "ECMAScript", version: "ES2020"}, r.markdown = {
                        parser: "marked",
                        version: "1.1.0"
                    }, r.positionEncodings = ["utf-16"], this._clientOptions.markdown.supportHtml && (r.markdown.allowedTags = ["ul", "li", "p", "code", "blockquote", "ol", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "em", "pre", "table", "thead", "tbody", "tr", "th", "td", "div", "del", "a", "strong", "br", "img", "span"]);
                    for (let t of this._features) t.fillClientCapabilities(e);
                    return e
                }

                initializeFeatures(e) {
                    const t = this._clientOptions.documentSelector;
                    for (const e of this._features) a.func(e.preInitialize) && e.preInitialize(this._capabilities, t);
                    for (const e of this._features) e.initialize(this._capabilities, t)
                }

                async handleRegistrationRequest(e) {
                    if (this.isRunning()) for (const t of e.registrations) {
                        const e = this._dynamicFeatures.get(t.method);
                        if (void 0 === e) return Promise.reject(new Error(`No feature implementation for ${t.method} found. Registration failed.`));
                        const i = t.registerOptions ?? {};
                        i.documentSelector = i.documentSelector ?? this._clientOptions.documentSelector;
                        const n = {id: t.id, registerOptions: i};
                        try {
                            e.register(n)
                        } catch (e) {
                            return Promise.reject(e)
                        }
                    } else for (const t of e.registrations) this._ignoredRegistrations.add(t.id)
                }

                async handleUnregistrationRequest(e) {
                    for (let t of e.unregisterations) {
                        if (this._ignoredRegistrations.has(t.id)) continue;
                        const e = this._dynamicFeatures.get(t.method);
                        if (!e) return Promise.reject(new Error(`No feature implementation for ${t.method} found. Unregistration failed.`));
                        e.unregister(t.id)
                    }
                }

                async handleApplyWorkspaceEdit(e) {
                    const t = e.edit, i = await this.workspaceEditLock.lock((() => this._p2c.asWorkspaceEdit(t))),
                        r = new Map;
                    n.workspace.textDocuments.forEach((e => r.set(e.uri.toString(), e)));
                    let s = !1;
                    if (t.documentChanges) for (const e of t.documentChanges) if (o.TextDocumentEdit.is(e) && e.textDocument.version && e.textDocument.version >= 0) {
                        const t = r.get(e.textDocument.uri);
                        if (t && t.version !== e.textDocument.version) {
                            s = !0;
                            break
                        }
                    }
                    return s ? Promise.resolve({applied: !1}) : a.asPromise(n.workspace.applyEdit(i).then((e => ({applied: e}))))
                }

                handleFailedRequest(e, t, i, r, s = !0) {
                    if (i instanceof o.ResponseError) {
                        if (i.code === o.ErrorCodes.PendingResponseRejected || i.code === o.ErrorCodes.ConnectionInactive) return r;
                        if (i.code === o.LSPErrorCodes.RequestCancelled || i.code === o.LSPErrorCodes.ServerCancelled) {
                            if (void 0 !== t && t.isCancellationRequested) return r;
                            throw void 0 !== i.data ? new d.LSPCancellationError(i.data) : new n.CancellationError
                        }
                        if (i.code === o.LSPErrorCodes.ContentModified) {
                            if (Z.RequestsToCancelOnContentModified.has(e.method)) throw new n.CancellationError;
                            return r
                        }
                    }
                    throw this.error(`Request ${e.method} failed.`, i, s), i
                }
            }

            t.BaseLanguageClient = Z, Z.RequestsToCancelOnContentModified = new Set([o.SemanticTokensRequest.method, o.SemanticTokensRangeRequest.method, o.SemanticTokensDeltaRequest.method]);

            class ee {
                error(e) {
                    (0, o.RAL)().console.error(e)
                }

                warn(e) {
                    (0, o.RAL)().console.warn(e)
                }

                info(e) {
                    (0, o.RAL)().console.info(e)
                }

                log(e) {
                    (0, o.RAL)().console.log(e)
                }
            }

            !function (e) {
                e.createAll = function (e) {
                    return []
                }
            }(Y = t.ProposedFeatures || (t.ProposedFeatures = {}))
        }, 3922: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.CodeActionFeature = void 0;
            const n = i(9496), o = i(3455), r = i(2678), s = i(5151);

            class a extends s.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.CodeActionRequest.type)
                }

                fillClientCapabilities(e) {
                    const t = (0, s.ensure)((0, s.ensure)(e, "textDocument"), "codeAction");
                    t.dynamicRegistration = !0, t.isPreferredSupport = !0, t.disabledSupport = !0, t.dataSupport = !0, t.resolveSupport = {properties: ["edit"]}, t.codeActionLiteralSupport = {codeActionKind: {valueSet: [o.CodeActionKind.Empty, o.CodeActionKind.QuickFix, o.CodeActionKind.Refactor, o.CodeActionKind.RefactorExtract, o.CodeActionKind.RefactorInline, o.CodeActionKind.RefactorRewrite, o.CodeActionKind.Source, o.CodeActionKind.SourceOrganizeImports]}}, t.honorsChangeAnnotations = !1
                }

                initialize(e, t) {
                    const i = this.getRegistrationOptions(t, e.codeActionProvider);
                    i && this.register({id: r.generateUuid(), registerOptions: i})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideCodeActions: (e, t, i, n) => {
                            const r = this._client, s = async (e, t, i, n) => {
                                const s = {
                                    textDocument: r.code2ProtocolConverter.asTextDocumentIdentifier(e),
                                    range: r.code2ProtocolConverter.asRange(t),
                                    context: await r.code2ProtocolConverter.asCodeActionContext(i, n)
                                };
                                return r.sendRequest(o.CodeActionRequest.type, s, n).then((e => n.isCancellationRequested || null == e ? null : r.protocol2CodeConverter.asCodeActionResult(e, n)), (e => r.handleFailedRequest(o.CodeActionRequest.type, n, e, null)))
                            }, a = r.middleware;
                            return a.provideCodeActions ? a.provideCodeActions(e, t, i, n, s) : s(e, t, i, n)
                        }, resolveCodeAction: e.resolveProvider ? (e, t) => {
                            const i = this._client, n = this._client.middleware,
                                r = async (e, t) => i.sendRequest(o.CodeActionResolveRequest.type, await i.code2ProtocolConverter.asCodeAction(e, t), t).then((n => t.isCancellationRequested ? e : i.protocol2CodeConverter.asCodeAction(n, t)), (n => i.handleFailedRequest(o.CodeActionResolveRequest.type, t, n, e)));
                            return n.resolveCodeAction ? n.resolveCodeAction(e, t, r) : r(e, t)
                        } : void 0
                    };
                    return [n.languages.registerCodeActionsProvider(this._client.protocol2CodeConverter.asDocumentSelector(t), i, e.codeActionKinds ? {providedCodeActionKinds: this._client.protocol2CodeConverter.asCodeActionKinds(e.codeActionKinds)} : void 0), i]
                }
            }

            t.CodeActionFeature = a
        }, 9061: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.createConverter = void 0;
            const n = i(9496), o = i(3455), r = i(2592), s = i(2570), a = i(6103), c = i(4332), l = i(8039),
                u = i(7317), d = i(3055), p = i(8889), h = i(696), g = i(3572), m = i(6598);
            var f;
            !function (e) {
                e.is = function (e) {
                    const t = e;
                    return t && !!t.inserting && !!t.replacing
                }
            }(f || (f = {})), t.createConverter = function (e) {
                const t = e || (e => e.toString());

                function i(e) {
                    return t(e)
                }

                function v(e) {
                    return {uri: t(e.uri)}
                }

                function y(e) {
                    return {uri: t(e.uri), languageId: e.languageId, version: e.version, text: e.getText()}
                }

                function C(e) {
                    switch (e) {
                        case n.TextDocumentSaveReason.Manual:
                            return o.TextDocumentSaveReason.Manual;
                        case n.TextDocumentSaveReason.AfterDelay:
                            return o.TextDocumentSaveReason.AfterDelay;
                        case n.TextDocumentSaveReason.FocusOut:
                            return o.TextDocumentSaveReason.FocusOut
                    }
                    return o.TextDocumentSaveReason.Manual
                }

                function w(e) {
                    switch (e) {
                        case n.CompletionTriggerKind.TriggerCharacter:
                            return o.CompletionTriggerKind.TriggerCharacter;
                        case n.CompletionTriggerKind.TriggerForIncompleteCompletions:
                            return o.CompletionTriggerKind.TriggerForIncompleteCompletions;
                        default:
                            return o.CompletionTriggerKind.Invoked
                    }
                }

                function b(e) {
                    switch (e) {
                        case n.SignatureHelpTriggerKind.Invoke:
                            return o.SignatureHelpTriggerKind.Invoked;
                        case n.SignatureHelpTriggerKind.TriggerCharacter:
                            return o.SignatureHelpTriggerKind.TriggerCharacter;
                        case n.SignatureHelpTriggerKind.ContentChange:
                            return o.SignatureHelpTriggerKind.ContentChange
                    }
                }

                function k(e) {
                    return {label: e.label}
                }

                function S(e) {
                    return {label: e.label, parameters: (t = e.parameters, t.map(k))};
                    var t
                }

                function D(e) {
                    return {line: e.line, character: e.character}
                }

                function P(e) {
                    return null == e ? e : {
                        line: e.line > o.uinteger.MAX_VALUE ? o.uinteger.MAX_VALUE : e.line,
                        character: e.character > o.uinteger.MAX_VALUE ? o.uinteger.MAX_VALUE : e.character
                    }
                }

                function T(e) {
                    return null == e ? e : {start: P(e.start), end: P(e.end)}
                }

                function R(e) {
                    return null == e ? e : o.Location.create(i(e.uri), T(e.range))
                }

                function _(e) {
                    switch (e) {
                        case n.DiagnosticSeverity.Error:
                            return o.DiagnosticSeverity.Error;
                        case n.DiagnosticSeverity.Warning:
                            return o.DiagnosticSeverity.Warning;
                        case n.DiagnosticSeverity.Information:
                            return o.DiagnosticSeverity.Information;
                        case n.DiagnosticSeverity.Hint:
                            return o.DiagnosticSeverity.Hint
                    }
                }

                function x(e) {
                    switch (e) {
                        case n.DiagnosticTag.Unnecessary:
                            return o.DiagnosticTag.Unnecessary;
                        case n.DiagnosticTag.Deprecated:
                            return o.DiagnosticTag.Deprecated;
                        default:
                            return
                    }
                }

                function E(e) {
                    return {message: e.message, location: R(e.location)}
                }

                function O(e) {
                    const t = o.Diagnostic.create(T(e.range), e.message),
                        n = e instanceof d.ProtocolDiagnostic ? e : void 0;
                    void 0 !== n && void 0 !== n.data && (t.data = n.data);
                    const s = function (e) {
                        if (null != e) return r.number(e) || r.string(e) ? e : {value: e.value, target: i(e.target)}
                    }(e.code);
                    return d.DiagnosticCode.is(s) ? void 0 !== n && n.hasDiagnosticCode ? t.code = s : (t.code = s.value, t.codeDescription = {href: s.target}) : t.code = s, r.number(e.severity) && (t.severity = _(e.severity)), Array.isArray(e.tags) && (t.tags = function (e) {
                        if (!e) return;
                        let t = [];
                        for (let i of e) {
                            let e = x(i);
                            void 0 !== e && t.push(e)
                        }
                        return t.length > 0 ? t : void 0
                    }(e.tags)), e.relatedInformation && (t.relatedInformation = e.relatedInformation.map(E)), e.source && (t.source = e.source), t
                }

                function j(e, t) {
                    return null == e ? e : s.map(e, O, t)
                }

                function M(e) {
                    if (e === n.CompletionItemTag.Deprecated) return o.CompletionItemTag.Deprecated
                }

                function I(e) {
                    return {range: T(e.range), newText: e.newText}
                }

                function F(e) {
                    return null == e ? e : e.map(I)
                }

                function N(e) {
                    return e <= n.SymbolKind.TypeParameter ? e + 1 : o.SymbolKind.Property
                }

                function q(e) {
                    return e
                }

                function L(e) {
                    return e.map(q)
                }

                function A(e) {
                    let t = o.Command.create(e.title, e.command);
                    return e.arguments && (t.arguments = e.arguments), t
                }

                function $(e) {
                    const t = o.InlayHintLabelPart.create(e.value);
                    return void 0 !== e.location && (t.location = R(e.location)), void 0 !== e.command && (t.command = A(e.command)), void 0 !== e.tooltip && (t.tooltip = U(e.tooltip)), t
                }

                function U(e) {
                    if ("string" == typeof e) return e;
                    return {kind: o.MarkupKind.Markdown, value: e.value}
                }

                return {
                    asUri: i,
                    asTextDocumentIdentifier: v,
                    asTextDocumentItem: y,
                    asVersionedTextDocumentIdentifier: function (e) {
                        return {uri: t(e.uri), version: e.version}
                    },
                    asOpenTextDocumentParams: function (e) {
                        return {textDocument: y(e)}
                    },
                    asChangeTextDocumentParams: function (e) {
                        if (function (e) {
                            let t = e;
                            return !!t.uri && !!t.version
                        }(e)) {
                            return {
                                textDocument: {uri: t(e.uri), version: e.version},
                                contentChanges: [{text: e.getText()}]
                            }
                        }
                        if (function (e) {
                            let t = e;
                            return !!t.document && !!t.contentChanges
                        }(e)) {
                            let i = e.document;
                            return {
                                textDocument: {uri: t(i.uri), version: i.version},
                                contentChanges: e.contentChanges.map((e => {
                                    let t = e.range;
                                    return {
                                        range: {
                                            start: {line: t.start.line, character: t.start.character},
                                            end: {line: t.end.line, character: t.end.character}
                                        }, rangeLength: e.rangeLength, text: e.text
                                    }
                                }))
                            }
                        }
                        throw Error("Unsupported text document change parameter")
                    },
                    asCloseTextDocumentParams: function (e) {
                        return {textDocument: v(e)}
                    },
                    asSaveTextDocumentParams: function (e, t = !1) {
                        let i = {textDocument: v(e)};
                        return t && (i.text = e.getText()), i
                    },
                    asWillSaveTextDocumentParams: function (e) {
                        return {textDocument: v(e.document), reason: C(e.reason)}
                    },
                    asDidCreateFilesParams: function (e) {
                        return {files: e.files.map((e => ({uri: t(e)})))}
                    },
                    asDidRenameFilesParams: function (e) {
                        return {files: e.files.map((e => ({oldUri: t(e.oldUri), newUri: t(e.newUri)})))}
                    },
                    asDidDeleteFilesParams: function (e) {
                        return {files: e.files.map((e => ({uri: t(e)})))}
                    },
                    asWillCreateFilesParams: function (e) {
                        return {files: e.files.map((e => ({uri: t(e)})))}
                    },
                    asWillRenameFilesParams: function (e) {
                        return {files: e.files.map((e => ({oldUri: t(e.oldUri), newUri: t(e.newUri)})))}
                    },
                    asWillDeleteFilesParams: function (e) {
                        return {files: e.files.map((e => ({uri: t(e)})))}
                    },
                    asTextDocumentPositionParams: function (e, t) {
                        return {textDocument: v(e), position: D(t)}
                    },
                    asCompletionParams: function (e, t, i) {
                        return {
                            textDocument: v(e),
                            position: D(t),
                            context: {triggerKind: w(i.triggerKind), triggerCharacter: i.triggerCharacter}
                        }
                    },
                    asSignatureHelpParams: function (e, t, i) {
                        return {
                            textDocument: v(e),
                            position: D(t),
                            context: {
                                isRetrigger: i.isRetrigger,
                                triggerCharacter: i.triggerCharacter,
                                triggerKind: b(i.triggerKind),
                                activeSignatureHelp: (n = i.activeSignatureHelp, void 0 === n ? n : {
                                    signatures: (o = n.signatures, o.map(S)),
                                    activeSignature: n.activeSignature,
                                    activeParameter: n.activeParameter
                                })
                            }
                        };
                        var n, o
                    },
                    asWorkerPosition: D,
                    asRange: T,
                    asPosition: P,
                    asPositions: function (e, t) {
                        return s.map(e, P, t)
                    },
                    asLocation: R,
                    asDiagnosticSeverity: _,
                    asDiagnosticTag: x,
                    asDiagnostic: O,
                    asDiagnostics: j,
                    asCompletionItem: function (e, t = !1) {
                        let i, s;
                        r.string(e.label) ? i = e.label : (i = e.label.label, !t || void 0 === e.label.detail && void 0 === e.label.description || (s = {
                            detail: e.label.detail,
                            description: e.label.description
                        }));
                        let c = {label: i};
                        void 0 !== s && (c.labelDetails = s);
                        let l = e instanceof a.default ? e : void 0;
                        var u, d;
                        e.detail && (c.detail = e.detail), e.documentation && (l && "$string" !== l.documentationFormat ? c.documentation = function (e, t) {
                            switch (e) {
                                case"$string":
                                    return t;
                                case o.MarkupKind.PlainText:
                                    return {kind: e, value: t};
                                case o.MarkupKind.Markdown:
                                    return {kind: e, value: t.value};
                                default:
                                    return `Unsupported Markup content received. Kind is: ${e}`
                            }
                        }(l.documentationFormat, e.documentation) : c.documentation = e.documentation), e.filterText && (c.filterText = e.filterText), function (e, t) {
                            let i, r, s = o.InsertTextFormat.PlainText;
                            t.textEdit ? (i = t.textEdit.newText, r = t.textEdit.range) : t.insertText instanceof n.SnippetString ? (s = o.InsertTextFormat.Snippet, i = t.insertText.value) : i = t.insertText;
                            t.range && (r = t.range);
                            e.insertTextFormat = s, t.fromEdit && void 0 !== i && void 0 !== r ? e.textEdit = function (e, t) {
                                return f.is(t) ? o.InsertReplaceEdit.create(e, T(t.inserting), T(t.replacing)) : {
                                    newText: e,
                                    range: T(t)
                                }
                            }(i, r) : e.insertText = i
                        }(c, e), r.number(e.kind) && (c.kind = (u = e.kind, void 0 !== (d = l && l.originalItemKind) ? d : u + 1)), e.sortText && (c.sortText = e.sortText), e.additionalTextEdits && (c.additionalTextEdits = F(e.additionalTextEdits)), e.commitCharacters && (c.commitCharacters = e.commitCharacters.slice()), e.command && (c.command = A(e.command)), !0 !== e.preselect && !1 !== e.preselect || (c.preselect = e.preselect);
                        const p = function (e) {
                            if (void 0 === e) return e;
                            const t = [];
                            for (let i of e) {
                                const e = M(i);
                                void 0 !== e && t.push(e)
                            }
                            return t
                        }(e.tags);
                        if (l) {
                            if (void 0 !== l.data && (c.data = l.data), !0 === l.deprecated || !1 === l.deprecated) {
                                if (!0 === l.deprecated && void 0 !== p && p.length > 0) {
                                    const e = p.indexOf(n.CompletionItemTag.Deprecated);
                                    -1 !== e && p.splice(e, 1)
                                }
                                c.deprecated = l.deprecated
                            }
                            void 0 !== l.insertTextMode && (c.insertTextMode = l.insertTextMode)
                        }
                        return void 0 !== p && p.length > 0 && (c.tags = p), void 0 === c.insertTextMode && !0 === e.keepWhitespace && (c.insertTextMode = o.InsertTextMode.adjustIndentation), c
                    },
                    asTextEdit: I,
                    asSymbolKind: N,
                    asSymbolTag: q,
                    asSymbolTags: L,
                    asReferenceParams: function (e, t, i) {
                        return {textDocument: v(e), position: D(t), context: {includeDeclaration: i.includeDeclaration}}
                    },
                    asCodeAction: async function (e, t) {
                        let i = o.CodeAction.create(e.title);
                        if (e instanceof u.default && void 0 !== e.data && (i.data = e.data), void 0 !== e.kind && (i.kind = function (e) {
                            if (null == e) return;
                            return e.value
                        }(e.kind)), void 0 !== e.diagnostics && (i.diagnostics = await j(e.diagnostics, t)), void 0 !== e.edit) throw new Error("VS Code code actions can only be converted to a protocol code action without an edit.");
                        return void 0 !== e.command && (i.command = A(e.command)), void 0 !== e.isPreferred && (i.isPreferred = e.isPreferred), void 0 !== e.disabled && (i.disabled = {reason: e.disabled.reason}), i
                    },
                    asCodeActionContext: async function (e, t) {
                        if (null == e) return e;
                        let i;
                        return e.only && r.string(e.only.value) && (i = [e.only.value]), o.CodeActionContext.create(await j(e.diagnostics, t), i, function (e) {
                            switch (e) {
                                case n.CodeActionTriggerKind.Invoke:
                                    return o.CodeActionTriggerKind.Invoked;
                                case n.CodeActionTriggerKind.Automatic:
                                    return o.CodeActionTriggerKind.Automatic;
                                default:
                                    return
                            }
                        }(e.triggerKind))
                    },
                    asInlineValueContext: function (e) {
                        return null == e ? e : o.InlineValueContext.create(e.frameId, T(e.stoppedLocation))
                    },
                    asCommand: A,
                    asCodeLens: function (e) {
                        let t = o.CodeLens.create(T(e.range));
                        return e.command && (t.command = A(e.command)), e instanceof c.default && e.data && (t.data = e.data), t
                    },
                    asFormattingOptions: function (e, t) {
                        const i = {tabSize: e.tabSize, insertSpaces: e.insertSpaces};
                        return t.trimTrailingWhitespace && (i.trimTrailingWhitespace = !0), t.trimFinalNewlines && (i.trimFinalNewlines = !0), t.insertFinalNewline && (i.insertFinalNewline = !0), i
                    },
                    asDocumentSymbolParams: function (e) {
                        return {textDocument: v(e)}
                    },
                    asCodeLensParams: function (e) {
                        return {textDocument: v(e)}
                    },
                    asDocumentLink: function (e) {
                        let t = o.DocumentLink.create(T(e.range));
                        e.target && (t.target = i(e.target)), void 0 !== e.tooltip && (t.tooltip = e.tooltip);
                        let n = e instanceof l.default ? e : void 0;
                        return n && n.data && (t.data = n.data), t
                    },
                    asDocumentLinkParams: function (e) {
                        return {textDocument: v(e)}
                    },
                    asCallHierarchyItem: function (e) {
                        const t = {
                            name: e.name,
                            kind: N(e.kind),
                            uri: i(e.uri),
                            range: T(e.range),
                            selectionRange: T(e.selectionRange)
                        };
                        return void 0 !== e.detail && e.detail.length > 0 && (t.detail = e.detail), void 0 !== e.tags && (t.tags = L(e.tags)), e instanceof p.default && void 0 !== e.data && (t.data = e.data), t
                    },
                    asTypeHierarchyItem: function (e) {
                        const t = {
                            name: e.name,
                            kind: N(e.kind),
                            uri: i(e.uri),
                            range: T(e.range),
                            selectionRange: T(e.selectionRange)
                        };
                        return void 0 !== e.detail && e.detail.length > 0 && (t.detail = e.detail), void 0 !== e.tags && (t.tags = L(e.tags)), e instanceof h.default && void 0 !== e.data && (t.data = e.data), t
                    },
                    asInlayHint: function (e) {
                        const t = "string" == typeof e.label ? e.label : e.label.map($),
                            i = o.InlayHint.create(P(e.position), t);
                        return void 0 !== e.kind && (i.kind = e.kind), void 0 !== e.textEdits && (i.textEdits = F(e.textEdits)), void 0 !== e.tooltip && (i.tooltip = U(e.tooltip)), void 0 !== e.paddingLeft && (i.paddingLeft = e.paddingLeft), void 0 !== e.paddingRight && (i.paddingRight = e.paddingRight), e instanceof m.default && void 0 !== e.data && (i.data = e.data), i
                    },
                    asWorkspaceSymbol: function (e) {
                        const i = e instanceof g.default ? {
                            name: e.name,
                            kind: N(e.kind),
                            location: e.hasRange ? R(e.location) : {uri: t(e.location.uri)},
                            data: e.data
                        } : {name: e.name, kind: N(e.kind), location: R(e.location)};
                        return void 0 !== e.tags && (i.tags = L(e.tags)), "" !== e.containerName && (i.containerName = e.containerName), i
                    }
                }
            }
        }, 5003: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.CodeLensFeature = void 0;
            const n = i(9496), o = i(3455), r = i(2678), s = i(5151);

            class a extends s.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.CodeLensRequest.type)
                }

                fillClientCapabilities(e) {
                    (0, s.ensure)((0, s.ensure)(e, "textDocument"), "codeLens").dynamicRegistration = !0, (0, s.ensure)((0, s.ensure)(e, "workspace"), "codeLens").refreshSupport = !0
                }

                initialize(e, t) {
                    this._client.onRequest(o.CodeLensRefreshRequest.type, (async () => {
                        for (const e of this.getAllProviders()) e.onDidChangeCodeLensEmitter.fire()
                    }));
                    const i = this.getRegistrationOptions(t, e.codeLensProvider);
                    i && this.register({id: r.generateUuid(), registerOptions: i})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = new n.EventEmitter, r = {
                        onDidChangeCodeLenses: i.event, provideCodeLenses: (e, t) => {
                            const i = this._client,
                                n = (e, t) => i.sendRequest(o.CodeLensRequest.type, i.code2ProtocolConverter.asCodeLensParams(e), t).then((e => t.isCancellationRequested ? null : i.protocol2CodeConverter.asCodeLenses(e, t)), (e => i.handleFailedRequest(o.CodeLensRequest.type, t, e, null))),
                                r = i.middleware;
                            return r.provideCodeLenses ? r.provideCodeLenses(e, t, n) : n(e, t)
                        }, resolveCodeLens: e.resolveProvider ? (e, t) => {
                            const i = this._client,
                                n = (e, t) => i.sendRequest(o.CodeLensResolveRequest.type, i.code2ProtocolConverter.asCodeLens(e), t).then((n => t.isCancellationRequested ? e : i.protocol2CodeConverter.asCodeLens(n)), (n => i.handleFailedRequest(o.CodeLensResolveRequest.type, t, n, e))),
                                r = i.middleware;
                            return r.resolveCodeLens ? r.resolveCodeLens(e, t, n) : n(e, t)
                        } : void 0
                    };
                    return [n.languages.registerCodeLensProvider(this._client.protocol2CodeConverter.asDocumentSelector(t), r), {
                        provider: r,
                        onDidChangeCodeLensEmitter: i
                    }]
                }
            }

            t.CodeLensFeature = a
        }, 2648: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ColorProviderFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151);

            class s extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.DocumentColorRequest.type)
                }

                fillClientCapabilities(e) {
                    (0, r.ensure)((0, r.ensure)(e, "textDocument"), "colorProvider").dynamicRegistration = !0
                }

                initialize(e, t) {
                    let [i, n] = this.getRegistration(t, e.colorProvider);
                    i && n && this.register({id: i, registerOptions: n})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideColorPresentations: (e, t, i) => {
                            const n = this._client, r = (e, t, i) => {
                                const r = {
                                    color: e,
                                    textDocument: n.code2ProtocolConverter.asTextDocumentIdentifier(t.document),
                                    range: n.code2ProtocolConverter.asRange(t.range)
                                };
                                return n.sendRequest(o.ColorPresentationRequest.type, r, i).then((e => i.isCancellationRequested ? null : this._client.protocol2CodeConverter.asColorPresentations(e, i)), (e => n.handleFailedRequest(o.ColorPresentationRequest.type, i, e, null)))
                            }, s = n.middleware;
                            return s.provideColorPresentations ? s.provideColorPresentations(e, t, i, r) : r(e, t, i)
                        }, provideDocumentColors: (e, t) => {
                            const i = this._client, n = (e, t) => {
                                const n = {textDocument: i.code2ProtocolConverter.asTextDocumentIdentifier(e)};
                                return i.sendRequest(o.DocumentColorRequest.type, n, t).then((e => t.isCancellationRequested ? null : this._client.protocol2CodeConverter.asColorInformations(e, t)), (e => i.handleFailedRequest(o.DocumentColorRequest.type, t, e, null)))
                            }, r = i.middleware;
                            return r.provideDocumentColors ? r.provideDocumentColors(e, t, n) : n(e, t)
                        }
                    };
                    return [n.languages.registerColorProvider(this._client.protocol2CodeConverter.asDocumentSelector(t), i), i]
                }
            }

            t.ColorProviderFeature = s
        }, 5466: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.CompletionItemFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151), s = i(2678),
                a = [o.CompletionItemKind.Text, o.CompletionItemKind.Method, o.CompletionItemKind.Function, o.CompletionItemKind.Constructor, o.CompletionItemKind.Field, o.CompletionItemKind.Variable, o.CompletionItemKind.Class, o.CompletionItemKind.Interface, o.CompletionItemKind.Module, o.CompletionItemKind.Property, o.CompletionItemKind.Unit, o.CompletionItemKind.Value, o.CompletionItemKind.Enum, o.CompletionItemKind.Keyword, o.CompletionItemKind.Snippet, o.CompletionItemKind.Color, o.CompletionItemKind.File, o.CompletionItemKind.Reference, o.CompletionItemKind.Folder, o.CompletionItemKind.EnumMember, o.CompletionItemKind.Constant, o.CompletionItemKind.Struct, o.CompletionItemKind.Event, o.CompletionItemKind.Operator, o.CompletionItemKind.TypeParameter];

            class c extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.CompletionRequest.type), this.labelDetailsSupport = new Map
                }

                fillClientCapabilities(e) {
                    let t = (0, r.ensure)((0, r.ensure)(e, "textDocument"), "completion");
                    t.dynamicRegistration = !0, t.contextSupport = !0, t.completionItem = {
                        snippetSupport: !0,
                        commitCharactersSupport: !0,
                        documentationFormat: [o.MarkupKind.Markdown, o.MarkupKind.PlainText],
                        deprecatedSupport: !0,
                        preselectSupport: !0,
                        tagSupport: {valueSet: [o.CompletionItemTag.Deprecated]},
                        insertReplaceSupport: !0,
                        resolveSupport: {properties: ["documentation", "detail", "additionalTextEdits"]},
                        insertTextModeSupport: {valueSet: [o.InsertTextMode.asIs, o.InsertTextMode.adjustIndentation]},
                        labelDetailsSupport: !0
                    }, t.insertTextMode = o.InsertTextMode.adjustIndentation, t.completionItemKind = {valueSet: a}, t.completionList = {itemDefaults: ["commitCharacters", "editRange", "insertTextFormat", "insertTextMode"]}
                }

                initialize(e, t) {
                    const i = this.getRegistrationOptions(t, e.completionProvider);
                    i && this.register({id: s.generateUuid(), registerOptions: i})
                }

                registerLanguageProvider(e, t) {
                    this.labelDetailsSupport.set(t, !!e.completionItem?.labelDetailsSupport);
                    const i = e.triggerCharacters ?? [], r = e.allCommitCharacters, s = e.documentSelector, a = {
                        provideCompletionItems: (e, t, i, n) => {
                            const s = this._client, a = this._client.middleware,
                                c = (e, t, i, n) => s.sendRequest(o.CompletionRequest.type, s.code2ProtocolConverter.asCompletionParams(e, t, i), n).then((e => n.isCancellationRequested ? null : s.protocol2CodeConverter.asCompletionResult(e, r, n)), (e => s.handleFailedRequest(o.CompletionRequest.type, n, e, null)));
                            return a.provideCompletionItem ? a.provideCompletionItem(e, t, n, i, c) : c(e, t, n, i)
                        }, resolveCompletionItem: e.resolveProvider ? (e, i) => {
                            const n = this._client, r = this._client.middleware,
                                s = (e, i) => n.sendRequest(o.CompletionResolveRequest.type, n.code2ProtocolConverter.asCompletionItem(e, !!this.labelDetailsSupport.get(t)), i).then((e => i.isCancellationRequested ? null : n.protocol2CodeConverter.asCompletionItem(e)), (t => n.handleFailedRequest(o.CompletionResolveRequest.type, i, t, e)));
                            return r.resolveCompletionItem ? r.resolveCompletionItem(e, i, s) : s(e, i)
                        } : void 0
                    };
                    return [n.languages.registerCompletionItemProvider(this._client.protocol2CodeConverter.asDocumentSelector(s), a, ...i), a]
                }
            }

            t.CompletionItemFeature = c
        }, 1285: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.SyncConfigurationFeature = t.toJSONObject = t.ConfigurationFeature = void 0;
            const n = i(9496), o = i(3455), r = i(2592), s = i(2678), a = i(5151);

            function c(e) {
                if (e) {
                    if (Array.isArray(e)) return e.map(c);
                    if ("object" == typeof e) {
                        const t = Object.create(null);
                        for (const i in e) Object.prototype.hasOwnProperty.call(e, i) && (t[i] = c(e[i]));
                        return t
                    }
                }
                return e
            }

            t.ConfigurationFeature = class {
                constructor(e) {
                    this._client = e
                }

                getState() {
                    return {kind: "static"}
                }

                fillClientCapabilities(e) {
                    e.workspace = e.workspace || {}, e.workspace.configuration = !0
                }

                initialize() {
                    let e = this._client;
                    e.onRequest(o.ConfigurationRequest.type, ((t, i) => {
                        let n = e => {
                            let t = [];
                            for (let i of e.items) {
                                let e = void 0 !== i.scopeUri && null !== i.scopeUri ? this._client.protocol2CodeConverter.asUri(i.scopeUri) : void 0;
                                t.push(this.getConfiguration(e, null !== i.section ? i.section : void 0))
                            }
                            return t
                        }, o = e.middleware.workspace;
                        return o && o.configuration ? o.configuration(t, i, n) : n(t)
                    }))
                }

                getConfiguration(e, t) {
                    let i = null;
                    if (t) {
                        let o = t.lastIndexOf(".");
                        if (-1 === o) i = c(n.workspace.getConfiguration(void 0, e).get(t)); else {
                            let r = n.workspace.getConfiguration(t.substr(0, o), e);
                            r && (i = c(r.get(t.substr(o + 1))))
                        }
                    } else {
                        let t = n.workspace.getConfiguration(void 0, e);
                        i = {};
                        for (let e of Object.keys(t)) t.has(e) && (i[e] = c(t.get(e)))
                    }
                    return void 0 === i && (i = null), i
                }

                dispose() {
                }
            }, t.toJSONObject = c;
            t.SyncConfigurationFeature = class {
                constructor(e) {
                    this._client = e, this._listeners = new Map
                }

                getState() {
                    return {
                        kind: "workspace",
                        id: this.registrationType.method,
                        registrations: this._listeners.size > 0
                    }
                }

                get registrationType() {
                    return o.DidChangeConfigurationNotification.type
                }

                fillClientCapabilities(e) {
                    (0, a.ensure)((0, a.ensure)(e, "workspace"), "didChangeConfiguration").dynamicRegistration = !0
                }

                initialize() {
                    let e = this._client.clientOptions.synchronize?.configurationSection;
                    void 0 !== e && this.register({id: s.generateUuid(), registerOptions: {section: e}})
                }

                register(e) {
                    let t = n.workspace.onDidChangeConfiguration((t => {
                        this.onDidChangeConfiguration(e.registerOptions.section, t)
                    }));
                    this._listeners.set(e.id, t), void 0 !== e.registerOptions.section && this.onDidChangeConfiguration(e.registerOptions.section, void 0)
                }

                unregister(e) {
                    let t = this._listeners.get(e);
                    t && (this._listeners.delete(e), t.dispose())
                }

                dispose() {
                    for (const e of this._listeners.values()) e.dispose();
                    this._listeners.clear()
                }

                onDidChangeConfiguration(e, t) {
                    let i;
                    if (i = r.string(e) ? [e] : e, void 0 !== i && void 0 !== t) {
                        if (!i.some((e => t.affectsConfiguration(e)))) return
                    }
                    const n = async e => void 0 === e ? this._client.sendNotification(o.DidChangeConfigurationNotification.type, {settings: null}) : this._client.sendNotification(o.DidChangeConfigurationNotification.type, {settings: this.extractSettingsInformation(e)});
                    let s = this._client.middleware.workspace?.didChangeConfiguration;
                    (s ? s(i, n) : n(i)).catch((e => {
                        this._client.error(`Sending notification ${o.DidChangeConfigurationNotification.type.method} failed`, e)
                    }))
                }

                extractSettingsInformation(e) {
                    function t(e, t) {
                        let i = e;
                        for (let e = 0; e < t.length - 1; e++) {
                            let n = i[t[e]];
                            n || (n = Object.create(null), i[t[e]] = n), i = n
                        }
                        return i
                    }

                    let i = this._client.clientOptions.workspaceFolder ? this._client.clientOptions.workspaceFolder.uri : void 0,
                        o = Object.create(null);
                    for (let r = 0; r < e.length; r++) {
                        let s = e[r], a = s.indexOf("."), l = null;
                        if (l = a >= 0 ? n.workspace.getConfiguration(s.substr(0, a), i).get(s.substr(a + 1)) : n.workspace.getConfiguration(void 0, i).get(s), l) {
                            let i = e[r].split(".");
                            t(o, i)[i[i.length - 1]] = c(l)
                        }
                    }
                    return o
                }
            }
        }, 6381: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DeclarationFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151);

            class s extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.DeclarationRequest.type)
                }

                fillClientCapabilities(e) {
                    const t = (0, r.ensure)((0, r.ensure)(e, "textDocument"), "declaration");
                    t.dynamicRegistration = !0, t.linkSupport = !0
                }

                initialize(e, t) {
                    const [i, n] = this.getRegistration(t, e.declarationProvider);
                    i && n && this.register({id: i, registerOptions: n})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideDeclaration: (e, t, i) => {
                            const n = this._client,
                                r = (e, t, i) => n.sendRequest(o.DeclarationRequest.type, n.code2ProtocolConverter.asTextDocumentPositionParams(e, t), i).then((e => i.isCancellationRequested ? null : n.protocol2CodeConverter.asDeclarationResult(e, i)), (e => n.handleFailedRequest(o.DeclarationRequest.type, i, e, null))),
                                s = n.middleware;
                            return s.provideDeclaration ? s.provideDeclaration(e, t, i, r) : r(e, t, i)
                        }
                    };
                    return [this.registerProvider(t, i), i]
                }

                registerProvider(e, t) {
                    return n.languages.registerDeclarationProvider(this._client.protocol2CodeConverter.asDocumentSelector(e), t)
                }
            }

            t.DeclarationFeature = s
        }, 1251: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DefinitionFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151), s = i(2678);

            class a extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.DefinitionRequest.type)
                }

                fillClientCapabilities(e) {
                    let t = (0, r.ensure)((0, r.ensure)(e, "textDocument"), "definition");
                    t.dynamicRegistration = !0, t.linkSupport = !0
                }

                initialize(e, t) {
                    const i = this.getRegistrationOptions(t, e.definitionProvider);
                    i && this.register({id: s.generateUuid(), registerOptions: i})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideDefinition: (e, t, i) => {
                            const n = this._client,
                                r = (e, t, i) => n.sendRequest(o.DefinitionRequest.type, n.code2ProtocolConverter.asTextDocumentPositionParams(e, t), i).then((e => i.isCancellationRequested ? null : n.protocol2CodeConverter.asDefinitionResult(e, i)), (e => n.handleFailedRequest(o.DefinitionRequest.type, i, e, null))),
                                s = n.middleware;
                            return s.provideDefinition ? s.provideDefinition(e, t, i, r) : r(e, t, i)
                        }
                    };
                    return [this.registerProvider(t, i), i]
                }

                registerProvider(e, t) {
                    return n.languages.registerDefinitionProvider(this._client.protocol2CodeConverter.asDocumentSelector(e), t)
                }
            }

            t.DefinitionFeature = a
        }, 5031: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DiagnosticFeature = t.DiagnosticPullMode = t.vsdiag = void 0;
            const n = i(9842), o = i(9496), r = i(3455), s = i(2678), a = i(5151);

            function c(e, t) {
                return void 0 === e[t] && (e[t] = {}), e[t]
            }

            var l, u, d, p;
            !function (e) {
                let t;
                !function (e) {
                    e.full = "full", e.unChanged = "unChanged"
                }(t = e.DocumentDiagnosticReportKind || (e.DocumentDiagnosticReportKind = {}))
            }(l = t.vsdiag || (t.vsdiag = {})), function (e) {
                e.onType = "onType", e.onSave = "onSave"
            }(u = t.DiagnosticPullMode || (t.DiagnosticPullMode = {})), function (e) {
                e.active = "open", e.reschedule = "reschedule", e.outDated = "drop"
            }(d || (d = {}));

            class h {
                constructor() {
                    this.open = new Set, this._onOpen = new o.EventEmitter, this._onClose = new o.EventEmitter, h.fillTabResources(this.open);
                    const e = e => {
                        if (0 === e.closed.length && 0 === e.opened.length) return;
                        const t = this.open, i = new Set;
                        h.fillTabResources(i);
                        const n = new Set, r = new Set(i);
                        for (const e of t.values()) i.has(e) ? r.delete(e) : n.add(e);
                        if (this.open = i, n.size > 0) {
                            const e = new Set;
                            for (const t of n) e.add(o.Uri.parse(t));
                            this._onClose.fire(e)
                        }
                        if (r.size > 0) {
                            const e = new Set;
                            for (const t of r) e.add(o.Uri.parse(t));
                            this._onOpen.fire(e)
                        }
                    };
                    void 0 !== o.window.tabGroups.onDidChangeTabs ? this.disposable = o.window.tabGroups.onDidChangeTabs(e) : this.disposable = {
                        dispose: () => {
                        }
                    }
                }

                get onClose() {
                    return this._onClose.event
                }

                get onOpen() {
                    return this._onOpen.event
                }

                dispose() {
                    this.disposable.dispose()
                }

                isActive(e) {
                    return e instanceof o.Uri ? o.window.activeTextEditor?.document.uri === e : o.window.activeTextEditor?.document === e
                }

                isVisible(e) {
                    const t = e instanceof o.Uri ? e : e.uri;
                    return this.open.has(t.toString())
                }

                getTabResources() {
                    const e = new Set;
                    return h.fillTabResources(new Set, e), e
                }

                static fillTabResources(e, t) {
                    const i = e ?? new Set;
                    for (const e of o.window.tabGroups.all) for (const n of e.tabs) {
                        const e = n.input;
                        let r;
                        e instanceof o.TabInputText ? r = e.uri : e instanceof o.TabInputTextDiff && (r = e.modified), void 0 === r || i.has(r.toString()) || (i.add(r.toString()), void 0 !== t && t.add(r))
                    }
                }
            }

            !function (e) {
                e[e.document = 1] = "document", e[e.workspace = 2] = "workspace"
            }(p || (p = {}));

            class g {
                constructor() {
                    this.documentPullStates = new Map, this.workspacePullStates = new Map
                }

                track(e, t, i) {
                    const n = e === p.document ? this.documentPullStates : this.workspacePullStates, [r, s, a] = t instanceof o.Uri ? [t.toString(), t, i] : [t.uri.toString(), t.uri, t.version];
                    let c = n.get(r);
                    return void 0 === c && (c = {document: s, pulledVersion: a, resultId: void 0}, n.set(r, c)), c
                }

                update(e, t, i, n) {
                    const r = e === p.document ? this.documentPullStates : this.workspacePullStates, [s, a, c, l] = t instanceof o.Uri ? [t.toString(), t, i, n] : [t.uri.toString(), t.uri, t.version, i];
                    let u = r.get(s);
                    void 0 === u ? (u = {
                        document: a,
                        pulledVersion: c,
                        resultId: l
                    }, r.set(s, u)) : (u.pulledVersion = c, u.resultId = l)
                }

                unTrack(e, t) {
                    const i = t instanceof o.Uri ? t.toString() : t.uri.toString();
                    (e === p.document ? this.documentPullStates : this.workspacePullStates).delete(i)
                }

                tracks(e, t) {
                    const i = t instanceof o.Uri ? t.toString() : t.uri.toString();
                    return (e === p.document ? this.documentPullStates : this.workspacePullStates).has(i)
                }

                getResultId(e, t) {
                    const i = t instanceof o.Uri ? t.toString() : t.uri.toString(),
                        n = e === p.document ? this.documentPullStates : this.workspacePullStates;
                    return n.get(i)?.resultId
                }

                getAllResultIds() {
                    const e = [];
                    for (let [t, i] of this.workspacePullStates) this.documentPullStates.has(t) && (i = this.documentPullStates.get(t)), void 0 !== i.resultId && e.push({
                        uri: t,
                        value: i.resultId
                    });
                    return e
                }
            }

            class m {
                constructor(e, t, i) {
                    this.client = e, this.tabs = t, this.options = i, this.isDisposed = !1, this.onDidChangeDiagnosticsEmitter = new o.EventEmitter, this.provider = this.createProvider(), this.diagnostics = o.languages.createDiagnosticCollection(i.identifier), this.openRequests = new Map, this.documentStates = new g, this.workspaceErrorCounter = 0
                }

                knows(e, t) {
                    return this.documentStates.tracks(e, t)
                }

                forget(e, t) {
                    this.documentStates.unTrack(e, t)
                }

                pull(e, t) {
                    if (this.isDisposed) return;
                    const i = e instanceof o.Uri ? e : e.uri;
                    this.pullAsync(e).then((() => {
                        t && t()
                    }), (e => {
                        this.client.error(`Document pull failed for text document ${i.toString()}`, e, !1)
                    }))
                }

                async pullAsync(e, t) {
                    if (this.isDisposed) return;
                    const i = e instanceof o.Uri, n = i ? e : e.uri, s = n.toString();
                    t = i ? t : e.version;
                    const c = this.openRequests.get(s),
                        u = i ? this.documentStates.track(p.document, e, t) : this.documentStates.track(p.document, e);
                    if (void 0 === c) {
                        const i = new o.CancellationTokenSource;
                        let c, h;
                        this.openRequests.set(s, {state: d.active, document: e, version: t, tokenSource: i});
                        try {
                            c = await this.provider.provideDiagnostics(e, u.resultId, i.token) ?? {
                                kind: l.DocumentDiagnosticReportKind.full,
                                items: []
                            }
                        } catch (t) {
                            if (t instanceof a.LSPCancellationError && r.DiagnosticServerCancellationData.is(t.data) && !1 === t.data.retriggerRequest && (h = {
                                state: d.outDated,
                                document: e
                            }), !(void 0 === h && t instanceof o.CancellationError)) throw t;
                            h = {state: d.reschedule, document: e}
                        }
                        if (h = h ?? this.openRequests.get(s), void 0 === h) return this.client.error(`Lost request state in diagnostic pull model. Clearing diagnostics for ${s}`), void this.diagnostics.delete(n);
                        if (this.openRequests.delete(s), !this.tabs.isVisible(e)) return void this.documentStates.unTrack(p.document, e);
                        if (h.state === d.outDated) return;
                        void 0 !== c && (c.kind === l.DocumentDiagnosticReportKind.full && this.diagnostics.set(n, c.items), u.pulledVersion = t, u.resultId = c.resultId), h.state === d.reschedule && this.pull(e)
                    } else c.state === d.active ? (c.tokenSource.cancel(), this.openRequests.set(s, {
                        state: d.reschedule,
                        document: c.document
                    })) : c.state === d.outDated && this.openRequests.set(s, {
                        state: d.reschedule,
                        document: c.document
                    })
                }

                forgetDocument(e) {
                    const t = e instanceof o.Uri ? e : e.uri, i = t.toString(), n = this.openRequests.get(i);
                    this.options.workspaceDiagnostics ? void 0 !== n ? this.openRequests.set(i, {
                        state: d.reschedule,
                        document: e
                    }) : this.pull(e, (() => {
                        this.forget(p.document, e)
                    })) : (void 0 !== n && (n.state === d.active && n.tokenSource.cancel(), this.openRequests.set(i, {
                        state: d.outDated,
                        document: e
                    })), this.diagnostics.delete(t), this.forget(p.document, e))
                }

                pullWorkspace() {
                    this.isDisposed || this.pullWorkspaceAsync().then((() => {
                        this.workspaceTimeout = (0, r.RAL)().timer.setTimeout((() => {
                            this.pullWorkspace()
                        }), 2e3)
                    }), (e => {
                        e instanceof a.LSPCancellationError || r.DiagnosticServerCancellationData.is(e.data) || (this.client.error("Workspace diagnostic pull failed.", e, !1), this.workspaceErrorCounter++), this.workspaceErrorCounter <= 5 && (this.workspaceTimeout = (0, r.RAL)().timer.setTimeout((() => {
                            this.pullWorkspace()
                        }), 2e3))
                    }))
                }

                async pullWorkspaceAsync() {
                    if (!this.provider.provideWorkspaceDiagnostics || this.isDisposed) return;
                    void 0 !== this.workspaceCancellation && (this.workspaceCancellation.cancel(), this.workspaceCancellation = void 0), this.workspaceCancellation = new o.CancellationTokenSource;
                    const e = this.documentStates.getAllResultIds().map((e => ({
                        uri: this.client.protocol2CodeConverter.asUri(e.uri),
                        value: e.value
                    })));
                    await this.provider.provideWorkspaceDiagnostics(e, this.workspaceCancellation.token, (e => {
                        if (e && !this.isDisposed) for (const t of e.items) t.kind === l.DocumentDiagnosticReportKind.full && (this.documentStates.tracks(p.document, t.uri) || this.diagnostics.set(t.uri, t.items)), this.documentStates.update(p.workspace, t.uri, t.version ?? void 0, t.resultId)
                    }))
                }

                createProvider() {
                    const e = {
                        onDidChangeDiagnostics: this.onDidChangeDiagnosticsEmitter.event,
                        provideDiagnostics: (e, t, i) => {
                            const n = (e, t, i) => {
                                const n = {
                                    identifier: this.options.identifier,
                                    textDocument: {uri: this.client.code2ProtocolConverter.asUri(e instanceof o.Uri ? e : e.uri)},
                                    previousResultId: t
                                };
                                return !0 !== this.isDisposed && this.client.isRunning() ? this.client.sendRequest(r.DocumentDiagnosticRequest.type, n, i).then((async e => null == e || this.isDisposed || i.isCancellationRequested ? {
                                    kind: l.DocumentDiagnosticReportKind.full,
                                    items: []
                                } : e.kind === r.DocumentDiagnosticReportKind.Full ? {
                                    kind: l.DocumentDiagnosticReportKind.full,
                                    resultId: e.resultId,
                                    items: await this.client.protocol2CodeConverter.asDiagnostics(e.items, i)
                                } : {
                                    kind: l.DocumentDiagnosticReportKind.unChanged,
                                    resultId: e.resultId
                                }), (e => this.client.handleFailedRequest(r.DocumentDiagnosticRequest.type, i, e, {
                                    kind: l.DocumentDiagnosticReportKind.full,
                                    items: []
                                }))) : {kind: l.DocumentDiagnosticReportKind.full, items: []}
                            }, s = this.client.middleware;
                            return s.provideDiagnostics ? s.provideDiagnostics(e, t, i, n) : n(e, t, i)
                        }
                    };
                    return this.options.workspaceDiagnostics && (e.provideWorkspaceDiagnostics = (e, t, i) => {
                        const n = async e => e.kind === r.DocumentDiagnosticReportKind.Full ? {
                            kind: l.DocumentDiagnosticReportKind.full,
                            uri: this.client.protocol2CodeConverter.asUri(e.uri),
                            resultId: e.resultId,
                            version: e.version,
                            items: await this.client.protocol2CodeConverter.asDiagnostics(e.items, t)
                        } : {
                            kind: l.DocumentDiagnosticReportKind.unChanged,
                            uri: this.client.protocol2CodeConverter.asUri(e.uri),
                            resultId: e.resultId,
                            version: e.version
                        }, o = e => {
                            const t = [];
                            for (const i of e) t.push({
                                uri: this.client.code2ProtocolConverter.asUri(i.uri),
                                value: i.value
                            });
                            return t
                        }, a = (e, t) => {
                            const a = (0, s.generateUuid)(),
                                c = this.client.onProgress(r.WorkspaceDiagnosticRequest.partialResult, a, (async e => {
                                    if (null == e) return void i(null);
                                    const t = {items: []};
                                    for (const i of e.items) try {
                                        t.items.push(await n(i))
                                    } catch (e) {
                                        this.client.error("Converting workspace diagnostics failed.", e)
                                    }
                                    i(t)
                                })), l = {
                                    identifier: this.options.identifier,
                                    previousResultIds: o(e),
                                    partialResultToken: a
                                };
                            return !0 !== this.isDisposed && this.client.isRunning() ? this.client.sendRequest(r.WorkspaceDiagnosticRequest.type, l, t).then((async e => {
                                if (t.isCancellationRequested) return {items: []};
                                const o = {items: []};
                                for (const t of e.items) o.items.push(await n(t));
                                return c.dispose(), i(o), {items: []}
                            }), (e => (c.dispose(), this.client.handleFailedRequest(r.DocumentDiagnosticRequest.type, t, e, {items: []})))) : {items: []}
                        }, c = this.client.middleware;
                        return c.provideWorkspaceDiagnostics ? c.provideWorkspaceDiagnostics(e, t, i, a) : a(e, t)
                    }), e
                }

                dispose() {
                    this.isDisposed = !0, this.workspaceCancellation?.cancel(), this.workspaceTimeout?.dispose();
                    for (const [e, t] of this.openRequests) t.state === d.active && t.tokenSource.cancel(), this.openRequests.set(e, {
                        state: d.outDated,
                        document: t.document
                    });
                    this.diagnostics.dispose()
                }
            }

            class f {
                constructor(e) {
                    this.diagnosticRequestor = e, this.documents = new r.LinkedMap, this.isDisposed = !1
                }

                add(e) {
                    if (!0 === this.isDisposed) return;
                    const t = e instanceof o.Uri ? e.toString() : e.uri.toString();
                    this.documents.has(t) || (this.documents.set(t, e, r.Touch.Last), this.trigger())
                }

                remove(e) {
                    const t = e instanceof o.Uri ? e.toString() : e.uri.toString();
                    this.documents.has(t) && (this.documents.delete(t), this.diagnosticRequestor.pull(e)), 0 === this.documents.size ? this.stop() : e === this.endDocument && (this.endDocument = this.documents.last)
                }

                trigger() {
                    !0 !== this.isDisposed && (void 0 === this.intervalHandle ? (this.endDocument = this.documents.last, this.intervalHandle = (0, r.RAL)().timer.setInterval((() => {
                        const e = this.documents.first;
                        if (void 0 !== e) {
                            const t = e instanceof o.Uri ? e.toString() : e.uri.toString();
                            this.diagnosticRequestor.pull(e), this.documents.set(t, e, r.Touch.Last), e === this.endDocument && this.stop()
                        }
                    }), 200)) : this.endDocument = this.documents.last)
                }

                dispose() {
                    this.isDisposed = !0, this.stop(), this.documents.clear()
                }

                stop() {
                    this.intervalHandle?.dispose(), this.intervalHandle = void 0, this.endDocument = void 0
                }
            }

            class v {
                constructor(e, t, i) {
                    const s = e.clientOptions.diagnosticPullOptions ?? {onChange: !0, onSave: !1},
                        a = e.protocol2CodeConverter.asDocumentSelector(i.documentSelector), c = [],
                        l = e => e instanceof o.Uri ? (e => {
                            const t = i.documentSelector;
                            if (void 0 !== s.match) return s.match(t, e);
                            for (const i of t) if (r.TextDocumentFilter.is(i)) {
                                if ("string" == typeof i) return !1;
                                if (void 0 !== i.language && "*" !== i.language) return !1;
                                if (void 0 !== i.scheme && "*" !== i.scheme && i.scheme !== e.scheme) return !1;
                                if (void 0 !== i.pattern) {
                                    const t = new n.Minimatch(i.pattern, {noext: !0});
                                    if (!t.makeRe()) return !1;
                                    if (!t.match(e.fsPath)) return !1
                                }
                            }
                            return !0
                        })(e) : o.languages.match(a, e) > 0 && t.isVisible(e),
                        d = e => e instanceof o.Uri ? this.activeTextDocument?.uri.toString() === e.toString() : this.activeTextDocument === e;
                    this.diagnosticRequestor = new m(e, t, i), this.backgroundScheduler = new f(this.diagnosticRequestor);
                    const h = e => {
                        l(e) && i.interFileDependencies && !d(e) && this.backgroundScheduler.add(e)
                    };
                    this.activeTextDocument = o.window.activeTextEditor?.document, o.window.onDidChangeActiveTextEditor((e => {
                        const t = this.activeTextDocument;
                        this.activeTextDocument = e?.document, void 0 !== t && h(t), void 0 !== this.activeTextDocument && this.backgroundScheduler.remove(this.activeTextDocument)
                    }));
                    const g = e.getFeature(r.DidOpenTextDocumentNotification.method);
                    c.push(g.onNotificationSent((e => {
                        const t = e.original;
                        l(t) && this.diagnosticRequestor.pull(t, (() => {
                            h(t)
                        }))
                    })));
                    const v = new Set;
                    for (const e of o.workspace.textDocuments) l(e) && (this.diagnosticRequestor.pull(e, (() => {
                        h(e)
                    })), v.add(e.uri.toString()));
                    if (!0 === s.onTabs) for (const e of t.getTabResources()) !v.has(e.toString()) && l(e) && this.diagnosticRequestor.pull(e, (() => {
                        h(e)
                    }));
                    if (t.onOpen((e => {
                        for (const t of e) l(t) && !this.diagnosticRequestor.knows(p.document, t) && this.diagnosticRequestor.pull(t, (() => {
                            h(t)
                        }))
                    })), !0 === s.onChange) {
                        const t = e.getFeature(r.DidChangeTextDocumentNotification.method);
                        c.push(t.onNotificationSent((async e => {
                            const t = e.original.document;
                            (void 0 === s.filter || !s.filter(t, u.onType)) && this.diagnosticRequestor.knows(p.document, t) && e.original.contentChanges.length > 0 && this.diagnosticRequestor.pull(t, (() => {
                                this.backgroundScheduler.trigger()
                            }))
                        })))
                    }
                    if (!0 === s.onSave) {
                        const t = e.getFeature(r.DidSaveTextDocumentNotification.method);
                        c.push(t.onNotificationSent((e => {
                            const t = e.original;
                            void 0 !== s.filter && s.filter(t, u.onSave) || !this.diagnosticRequestor.knows(p.document, t) || this.diagnosticRequestor.pull(e.original, (() => {
                                this.backgroundScheduler.trigger()
                            }))
                        })))
                    }
                    const y = e.getFeature(r.DidCloseTextDocumentNotification.method);
                    c.push(y.onNotificationSent((e => {
                        this.cleanUpDocument(e.original)
                    }))), t.onClose((e => {
                        for (const t of e) this.cleanUpDocument(t)
                    })), this.diagnosticRequestor.onDidChangeDiagnosticsEmitter.event((() => {
                        for (const e of o.workspace.textDocuments) l(e) && this.diagnosticRequestor.pull(e)
                    })), !0 === i.workspaceDiagnostics && "da348dc5-c30a-4515-9d98-31ff3be38d14" !== i.identifier && this.diagnosticRequestor.pullWorkspace(), this.disposable = o.Disposable.from(...c, this.backgroundScheduler, this.diagnosticRequestor)
                }

                get onDidChangeDiagnosticsEmitter() {
                    return this.diagnosticRequestor.onDidChangeDiagnosticsEmitter
                }

                get diagnostics() {
                    return this.diagnosticRequestor.provider
                }

                cleanUpDocument(e) {
                    this.diagnosticRequestor.knows(p.document, e) && (this.diagnosticRequestor.forgetDocument(e), this.backgroundScheduler.remove(e))
                }
            }

            class y extends a.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, r.DocumentDiagnosticRequest.type)
                }

                fillClientCapabilities(e) {
                    let t = c(c(e, "textDocument"), "diagnostic");
                    t.dynamicRegistration = !0, t.relatedDocumentSupport = !1, c(c(e, "workspace"), "diagnostics").refreshSupport = !0
                }

                initialize(e, t) {
                    this._client.onRequest(r.DiagnosticRefreshRequest.type, (async () => {
                        for (const e of this.getAllProviders()) e.onDidChangeDiagnosticsEmitter.fire()
                    }));
                    let [i, n] = this.getRegistration(t, e.diagnosticProvider);
                    i && n && this.register({id: i, registerOptions: n})
                }

                dispose() {
                    void 0 !== this.tabs && (this.tabs.dispose(), this.tabs = void 0), super.dispose()
                }

                registerLanguageProvider(e) {
                    void 0 === this.tabs && (this.tabs = new h);
                    const t = new v(this._client, this.tabs, e);
                    return [t.disposable, t]
                }
            }

            t.DiagnosticFeature = y
        }, 7669: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DocumentHighlightFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151), s = i(2678);

            class a extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.DocumentHighlightRequest.type)
                }

                fillClientCapabilities(e) {
                    (0, r.ensure)((0, r.ensure)(e, "textDocument"), "documentHighlight").dynamicRegistration = !0
                }

                initialize(e, t) {
                    const i = this.getRegistrationOptions(t, e.documentHighlightProvider);
                    i && this.register({id: s.generateUuid(), registerOptions: i})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideDocumentHighlights: (e, t, i) => {
                            const n = this._client,
                                r = (e, t, i) => n.sendRequest(o.DocumentHighlightRequest.type, n.code2ProtocolConverter.asTextDocumentPositionParams(e, t), i).then((e => i.isCancellationRequested ? null : n.protocol2CodeConverter.asDocumentHighlights(e, i)), (e => n.handleFailedRequest(o.DocumentHighlightRequest.type, i, e, null))),
                                s = n.middleware;
                            return s.provideDocumentHighlights ? s.provideDocumentHighlights(e, t, i, r) : r(e, t, i)
                        }
                    };
                    return [n.languages.registerDocumentHighlightProvider(this._client.protocol2CodeConverter.asDocumentSelector(t), i), i]
                }
            }

            t.DocumentHighlightFeature = a
        }, 752: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DocumentLinkFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151), s = i(2678);

            class a extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.DocumentLinkRequest.type)
                }

                fillClientCapabilities(e) {
                    const t = (0, r.ensure)((0, r.ensure)(e, "textDocument"), "documentLink");
                    t.dynamicRegistration = !0, t.tooltipSupport = !0
                }

                initialize(e, t) {
                    const i = this.getRegistrationOptions(t, e.documentLinkProvider);
                    i && this.register({id: s.generateUuid(), registerOptions: i})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideDocumentLinks: (e, t) => {
                            const i = this._client,
                                n = (e, t) => i.sendRequest(o.DocumentLinkRequest.type, i.code2ProtocolConverter.asDocumentLinkParams(e), t).then((e => t.isCancellationRequested ? null : i.protocol2CodeConverter.asDocumentLinks(e, t)), (e => i.handleFailedRequest(o.DocumentLinkRequest.type, t, e, null))),
                                r = i.middleware;
                            return r.provideDocumentLinks ? r.provideDocumentLinks(e, t, n) : n(e, t)
                        }, resolveDocumentLink: e.resolveProvider ? (e, t) => {
                            const i = this._client;
                            let n = (e, t) => i.sendRequest(o.DocumentLinkResolveRequest.type, i.code2ProtocolConverter.asDocumentLink(e), t).then((n => t.isCancellationRequested ? e : i.protocol2CodeConverter.asDocumentLink(n)), (n => i.handleFailedRequest(o.DocumentLinkResolveRequest.type, t, n, e)));
                            const r = i.middleware;
                            return r.resolveDocumentLink ? r.resolveDocumentLink(e, t, n) : n(e, t)
                        } : void 0
                    };
                    return [n.languages.registerDocumentLinkProvider(this._client.protocol2CodeConverter.asDocumentSelector(t), i), i]
                }
            }

            t.DocumentLinkFeature = a
        }, 6157: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DocumentSymbolFeature = t.SupportedSymbolTags = t.SupportedSymbolKinds = void 0;
            const n = i(9496), o = i(3455), r = i(5151), s = i(2678);
            t.SupportedSymbolKinds = [o.SymbolKind.File, o.SymbolKind.Module, o.SymbolKind.Namespace, o.SymbolKind.Package, o.SymbolKind.Class, o.SymbolKind.Method, o.SymbolKind.Property, o.SymbolKind.Field, o.SymbolKind.Constructor, o.SymbolKind.Enum, o.SymbolKind.Interface, o.SymbolKind.Function, o.SymbolKind.Variable, o.SymbolKind.Constant, o.SymbolKind.String, o.SymbolKind.Number, o.SymbolKind.Boolean, o.SymbolKind.Array, o.SymbolKind.Object, o.SymbolKind.Key, o.SymbolKind.Null, o.SymbolKind.EnumMember, o.SymbolKind.Struct, o.SymbolKind.Event, o.SymbolKind.Operator, o.SymbolKind.TypeParameter], t.SupportedSymbolTags = [o.SymbolTag.Deprecated];

            class a extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.DocumentSymbolRequest.type)
                }

                fillClientCapabilities(e) {
                    let i = (0, r.ensure)((0, r.ensure)(e, "textDocument"), "documentSymbol");
                    i.dynamicRegistration = !0, i.symbolKind = {valueSet: t.SupportedSymbolKinds}, i.hierarchicalDocumentSymbolSupport = !0, i.tagSupport = {valueSet: t.SupportedSymbolTags}, i.labelSupport = !0
                }

                initialize(e, t) {
                    const i = this.getRegistrationOptions(t, e.documentSymbolProvider);
                    i && this.register({id: s.generateUuid(), registerOptions: i})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideDocumentSymbols: (e, t) => {
                            const i = this._client,
                                n = (e, t) => i.sendRequest(o.DocumentSymbolRequest.type, i.code2ProtocolConverter.asDocumentSymbolParams(e), t).then((async e => {
                                    if (t.isCancellationRequested || null == e) return null;
                                    if (0 === e.length) return [];
                                    {
                                        const n = e[0];
                                        return o.DocumentSymbol.is(n) ? await i.protocol2CodeConverter.asDocumentSymbols(e, t) : await i.protocol2CodeConverter.asSymbolInformations(e, t)
                                    }
                                }), (e => i.handleFailedRequest(o.DocumentSymbolRequest.type, t, e, null))),
                                r = i.middleware;
                            return r.provideDocumentSymbols ? r.provideDocumentSymbols(e, t, n) : n(e, t)
                        }
                    }, r = void 0 !== e.label ? {label: e.label} : void 0;
                    return [n.languages.registerDocumentSymbolProvider(this._client.protocol2CodeConverter.asDocumentSelector(t), i, r), i]
                }
            }

            t.DocumentSymbolFeature = a
        }, 8392: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ExecuteCommandFeature = void 0;
            const n = i(9496), o = i(3455), r = i(2678), s = i(5151);
            t.ExecuteCommandFeature = class {
                constructor(e) {
                    this._client = e, this._commands = new Map
                }

                getState() {
                    return {kind: "workspace", id: this.registrationType.method, registrations: this._commands.size > 0}
                }

                get registrationType() {
                    return o.ExecuteCommandRequest.type
                }

                fillClientCapabilities(e) {
                    (0, s.ensure)((0, s.ensure)(e, "workspace"), "executeCommand").dynamicRegistration = !0
                }

                initialize(e) {
                    e.executeCommandProvider && this.register({
                        id: r.generateUuid(),
                        registerOptions: Object.assign({}, e.executeCommandProvider)
                    })
                }

                register(e) {
                    const t = this._client, i = t.middleware, r = (e, i) => {
                        let n = {command: e, arguments: i};
                        return t.sendRequest(o.ExecuteCommandRequest.type, n).then(void 0, (e => t.handleFailedRequest(o.ExecuteCommandRequest.type, void 0, e, void 0)))
                    };
                    if (e.registerOptions.commands) {
                        const t = [];
                        for (const o of e.registerOptions.commands) t.push(n.commands.registerCommand(o, ((...e) => i.executeCommand ? i.executeCommand(o, e, r) : r(o, e))));
                        this._commands.set(e.id, t)
                    }
                }

                unregister(e) {
                    let t = this._commands.get(e);
                    t && t.forEach((e => e.dispose()))
                }

                dispose() {
                    this._commands.forEach((e => {
                        e.forEach((e => e.dispose()))
                    })), this._commands.clear()
                }
            }
        }, 5151: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.WorkspaceFeature = t.TextDocumentLanguageFeature = t.TextDocumentEventFeature = t.DynamicDocumentFeature = t.DynamicFeature = t.StaticFeature = t.ensure = t.LSPCancellationError = void 0;
            const n = i(9496), o = i(3455), r = i(2592), s = i(2678);

            class a extends n.CancellationError {
                constructor(e) {
                    super(), this.data = e
                }
            }

            t.LSPCancellationError = a, t.ensure = function (e, t) {
                return void 0 === e[t] && (e[t] = {}), e[t]
            }, function (e) {
                e.is = function (e) {
                    const t = e;
                    return null != t && r.func(t.fillClientCapabilities) && r.func(t.initialize) && r.func(t.getState) && r.func(t.dispose) && (void 0 === t.fillInitializeParams || r.func(t.fillInitializeParams))
                }
            }(t.StaticFeature || (t.StaticFeature = {})), function (e) {
                e.is = function (e) {
                    const t = e;
                    return null != t && r.func(t.fillClientCapabilities) && r.func(t.initialize) && r.func(t.getState) && r.func(t.dispose) && (void 0 === t.fillInitializeParams || r.func(t.fillInitializeParams)) && r.func(t.register) && r.func(t.unregister) && void 0 !== t.registrationType
                }
            }(t.DynamicFeature || (t.DynamicFeature = {}));

            class c {
                constructor(e) {
                    this._client = e
                }

                getState() {
                    const e = this.getDocumentSelectors();
                    let t = 0;
                    for (const i of e) {
                        t++;
                        for (const e of n.workspace.textDocuments) if (n.languages.match(i, e) > 0) return {
                            kind: "document",
                            id: this.registrationType.method,
                            registrations: !0,
                            matches: !0
                        }
                    }
                    const i = t > 0;
                    return {kind: "document", id: this.registrationType.method, registrations: i, matches: !1}
                }
            }

            t.DynamicDocumentFeature = c;
            t.TextDocumentEventFeature = class extends c {
                constructor(e, t, i, o, r, s, a) {
                    super(e), this._event = t, this._type = i, this._middleware = o, this._createParams = r, this._textDocument = s, this._selectorFilter = a, this._selectors = new Map, this._onNotificationSent = new n.EventEmitter
                }

                static textDocumentFilter(e, t) {
                    for (const i of e) if (n.languages.match(i, t) > 0) return !0;
                    return !1
                }

                getStateInfo() {
                    return [this._selectors.values(), !1]
                }

                getDocumentSelectors() {
                    return this._selectors.values()
                }

                register(e) {
                    e.registerOptions.documentSelector && (this._listener || (this._listener = this._event((e => {
                        this.callback(e).catch((e => {
                            this._client.error(`Sending document notification ${this._type.method} failed.`, e)
                        }))
                    }))), this._selectors.set(e.id, this._client.protocol2CodeConverter.asDocumentSelector(e.registerOptions.documentSelector)))
                }

                async callback(e) {
                    const t = async e => {
                        const t = this._createParams(e);
                        await this._client.sendNotification(this._type, t).catch(), this.notificationSent(e, this._type, t)
                    };
                    if (this.matches(e)) {
                        const i = this._middleware();
                        return i ? i(e, (e => t(e))) : t(e)
                    }
                }

                matches(e) {
                    return !this._client.hasDedicatedTextSynchronizationFeature(this._textDocument(e)) && (!this._selectorFilter || this._selectorFilter(this._selectors.values(), e))
                }

                get onNotificationSent() {
                    return this._onNotificationSent.event
                }

                notificationSent(e, t, i) {
                    this._onNotificationSent.fire({original: e, type: t, params: i})
                }

                unregister(e) {
                    this._selectors.delete(e), 0 === this._selectors.size && this._listener && (this._listener.dispose(), this._listener = void 0)
                }

                dispose() {
                    this._selectors.clear(), this._onNotificationSent.dispose(), this._listener && (this._listener.dispose(), this._listener = void 0)
                }

                getProvider(e) {
                    for (const t of this._selectors.values()) if (n.languages.match(t, e) > 0) return {send: e => this.callback(e)}
                }
            };
            t.TextDocumentLanguageFeature = class extends c {
                constructor(e, t) {
                    super(e), this._registrationType = t, this._registrations = new Map
                }

                * getDocumentSelectors() {
                    for (const e of this._registrations.values()) {
                        const t = e.data.registerOptions.documentSelector;
                        null !== t && (yield this._client.protocol2CodeConverter.asDocumentSelector(t))
                    }
                }

                get registrationType() {
                    return this._registrationType
                }

                register(e) {
                    if (!e.registerOptions.documentSelector) return;
                    let t = this.registerLanguageProvider(e.registerOptions, e.id);
                    this._registrations.set(e.id, {disposable: t[0], data: e, provider: t[1]})
                }

                unregister(e) {
                    let t = this._registrations.get(e);
                    void 0 !== t && t.disposable.dispose()
                }

                dispose() {
                    this._registrations.forEach((e => {
                        e.disposable.dispose()
                    })), this._registrations.clear()
                }

                getRegistration(e, t) {
                    if (!t) return [void 0, void 0];
                    if (o.TextDocumentRegistrationOptions.is(t)) {
                        const i = o.StaticRegistrationOptions.hasId(t) ? t.id : s.generateUuid(),
                            n = t.documentSelector || e;
                        if (n) return [i, Object.assign({}, t, {documentSelector: n})]
                    } else if (r.boolean(t) && !0 === t || o.WorkDoneProgressOptions.is(t)) {
                        if (!e) return [void 0, void 0];
                        let i = r.boolean(t) && !0 === t ? {documentSelector: e} : Object.assign({}, t, {documentSelector: e});
                        return [s.generateUuid(), i]
                    }
                    return [void 0, void 0]
                }

                getRegistrationOptions(e, t) {
                    if (e && t) return r.boolean(t) && !0 === t ? {documentSelector: e} : Object.assign({}, t, {documentSelector: e})
                }

                getProvider(e) {
                    for (const t of this._registrations.values()) {
                        let i = t.data.registerOptions.documentSelector;
                        if (null !== i && n.languages.match(this._client.protocol2CodeConverter.asDocumentSelector(i), e) > 0) return t.provider
                    }
                }

                getAllProviders() {
                    const e = [];
                    for (const t of this._registrations.values()) e.push(t.provider);
                    return e
                }
            };
            t.WorkspaceFeature = class {
                constructor(e, t) {
                    this._client = e, this._registrationType = t, this._registrations = new Map
                }

                getState() {
                    const e = this._registrations.size > 0;
                    return {kind: "workspace", id: this._registrationType.method, registrations: e}
                }

                get registrationType() {
                    return this._registrationType
                }

                register(e) {
                    const t = this.registerLanguageProvider(e.registerOptions);
                    this._registrations.set(e.id, {disposable: t[0], provider: t[1]})
                }

                unregister(e) {
                    let t = this._registrations.get(e);
                    void 0 !== t && t.disposable.dispose()
                }

                dispose() {
                    this._registrations.forEach((e => {
                        e.disposable.dispose()
                    })), this._registrations.clear()
                }

                getProviders() {
                    const e = [];
                    for (const t of this._registrations.values()) e.push(t.provider);
                    return e
                }
            }
        }, 4373: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.WillDeleteFilesFeature = t.WillRenameFilesFeature = t.WillCreateFilesFeature = t.DidDeleteFilesFeature = t.DidRenameFilesFeature = t.DidCreateFilesFeature = void 0;
            const n = i(9496), o = i(9842), r = i(3455), s = i(2678);

            function a(e, t) {
                return void 0 === e[t] && (e[t] = {}), e[t]
            }

            function c(e, t, i) {
                e[t] = i
            }

            class l {
                constructor(e, t, i, n, o) {
                    this._client = e, this._event = t, this._registrationType = i, this._clientCapability = n, this._serverCapability = o, this._filters = new Map
                }

                getState() {
                    return {kind: "workspace", id: this._registrationType.method, registrations: this._filters.size > 0}
                }

                filterSize() {
                    return this._filters.size
                }

                get registrationType() {
                    return this._registrationType
                }

                fillClientCapabilities(e) {
                    const t = a(a(e, "workspace"), "fileOperations");
                    c(t, "dynamicRegistration", !0), c(t, this._clientCapability, !0)
                }

                initialize(e) {
                    const t = e.workspace?.fileOperations,
                        i = void 0 !== t ? (n = t, o = this._serverCapability, n[o]) : void 0;
                    var n, o;
                    if (void 0 !== i?.filters) try {
                        this.register({id: s.generateUuid(), registerOptions: {filters: i.filters}})
                    } catch (e) {
                        this._client.warn(`Ignoring invalid glob pattern for ${this._serverCapability} registration: ${e}`)
                    }
                }

                register(e) {
                    this._listener || (this._listener = this._event(this.send, this));
                    const t = e.registerOptions.filters.map((e => {
                        const t = new o.Minimatch(e.pattern.glob, l.asMinimatchOptions(e.pattern.options));
                        if (!t.makeRe()) throw new Error(`Invalid pattern ${e.pattern.glob}!`);
                        return {scheme: e.scheme, matcher: t, kind: e.pattern.matches}
                    }));
                    this._filters.set(e.id, t)
                }

                unregister(e) {
                    this._filters.delete(e), 0 === this._filters.size && this._listener && (this._listener.dispose(), this._listener = void 0)
                }

                dispose() {
                    this._filters.clear(), this._listener && (this._listener.dispose(), this._listener = void 0)
                }

                getFileType(e) {
                    return l.getFileType(e)
                }

                async filter(e, t) {
                    const i = await Promise.all(e.files.map((async e => {
                        const i = t(e), o = i.fsPath.replace(/\\/g, "/");
                        for (const e of this._filters.values()) for (const t of e) if (void 0 === t.scheme || t.scheme === i.scheme) if (t.matcher.match(o)) {
                            if (void 0 === t.kind) return !0;
                            const e = await this.getFileType(i);
                            if (void 0 === e) return this._client.error(`Failed to determine file type for ${i.toString()}.`), !0;
                            if (e === n.FileType.File && t.kind === r.FileOperationPatternKind.file || e === n.FileType.Directory && t.kind === r.FileOperationPatternKind.folder) return !0
                        } else if (t.kind === r.FileOperationPatternKind.folder) {
                            if (await l.getFileType(i) === n.FileType.Directory && t.matcher.match(`${o}/`)) return !0
                        }
                        return !1
                    }))), o = e.files.filter(((e, t) => i[t]));
                    return {...e, files: o}
                }

                static async getFileType(e) {
                    try {
                        return (await n.workspace.fs.stat(e)).type
                    } catch (e) {

                    }
                }

                static asMinimatchOptions(e) {
                    if (void 0 !== e) return !0 === e.ignoreCase ? {nocase: !0} : void 0
                }
            }

            class u extends l {
                constructor(e, t, i, n, o, r, s) {
                    super(e, t, i, n, o), this._notificationType = i, this._accessUri = r, this._createParams = s
                }

                async send(e) {
                    const t = await this.filter(e, this._accessUri);
                    if (t.files.length) {
                        const e = async e => this._client.sendNotification(this._notificationType, this._createParams(e));
                        return this.doSend(t, e)
                    }
                }
            }

            class d extends u {
                constructor() {
                    super(...arguments), this._fsPathFileTypes = new Map
                }

                async getFileType(e) {
                    const t = e.fsPath;
                    if (this._fsPathFileTypes.has(t)) return this._fsPathFileTypes.get(t);
                    const i = await l.getFileType(e);
                    return i && this._fsPathFileTypes.set(t, i), i
                }

                async cacheFileTypes(e, t) {
                    await this.filter(e, t)
                }

                clearFileTypeCache() {
                    this._fsPathFileTypes.clear()
                }

                unregister(e) {
                    super.unregister(e), 0 === this.filterSize() && this._willListener && (this._willListener.dispose(), this._willListener = void 0)
                }

                dispose() {
                    super.dispose(), this._willListener && (this._willListener.dispose(), this._willListener = void 0)
                }
            }

            t.DidCreateFilesFeature = class extends u {
                constructor(e) {
                    super(e, n.workspace.onDidCreateFiles, r.DidCreateFilesNotification.type, "didCreate", "didCreate", (e => e), e.code2ProtocolConverter.asDidCreateFilesParams)
                }

                doSend(e, t) {
                    const i = this._client.middleware.workspace;
                    return i?.didCreateFiles ? i.didCreateFiles(e, t) : t(e)
                }
            };
            t.DidRenameFilesFeature = class extends d {
                constructor(e) {
                    super(e, n.workspace.onDidRenameFiles, r.DidRenameFilesNotification.type, "didRename", "didRename", (e => e.oldUri), e.code2ProtocolConverter.asDidRenameFilesParams)
                }

                register(e) {
                    this._willListener || (this._willListener = n.workspace.onWillRenameFiles(this.willRename, this)), super.register(e)
                }

                willRename(e) {
                    e.waitUntil(this.cacheFileTypes(e, (e => e.oldUri)))
                }

                doSend(e, t) {
                    this.clearFileTypeCache();
                    const i = this._client.middleware.workspace;
                    return i?.didRenameFiles ? i.didRenameFiles(e, t) : t(e)
                }
            };
            t.DidDeleteFilesFeature = class extends d {
                constructor(e) {
                    super(e, n.workspace.onDidDeleteFiles, r.DidDeleteFilesNotification.type, "didDelete", "didDelete", (e => e), e.code2ProtocolConverter.asDidDeleteFilesParams)
                }

                register(e) {
                    this._willListener || (this._willListener = n.workspace.onWillDeleteFiles(this.willDelete, this)), super.register(e)
                }

                willDelete(e) {
                    e.waitUntil(this.cacheFileTypes(e, (e => e)))
                }

                doSend(e, t) {
                    this.clearFileTypeCache();
                    const i = this._client.middleware.workspace;
                    return i?.didDeleteFiles ? i.didDeleteFiles(e, t) : t(e)
                }
            };

            class p extends l {
                constructor(e, t, i, n, o, r, s) {
                    super(e, t, i, n, o), this._requestType = i, this._accessUri = r, this._createParams = s
                }

                async send(e) {
                    const t = this.waitUntil(e);
                    e.waitUntil(t)
                }

                async waitUntil(e) {
                    const t = await this.filter(e, this._accessUri);
                    if (t.files.length) {
                        const e = e => this._client.sendRequest(this._requestType, this._createParams(e), e.token).then(this._client.protocol2CodeConverter.asWorkspaceEdit);
                        return this.doSend(t, e)
                    }
                }
            }

            t.WillCreateFilesFeature = class extends p {
                constructor(e) {
                    super(e, n.workspace.onWillCreateFiles, r.WillCreateFilesRequest.type, "willCreate", "willCreate", (e => e), e.code2ProtocolConverter.asWillCreateFilesParams)
                }

                doSend(e, t) {
                    const i = this._client.middleware.workspace;
                    return i?.willCreateFiles ? i.willCreateFiles(e, t) : t(e)
                }
            };
            t.WillRenameFilesFeature = class extends p {
                constructor(e) {
                    super(e, n.workspace.onWillRenameFiles, r.WillRenameFilesRequest.type, "willRename", "willRename", (e => e.oldUri), e.code2ProtocolConverter.asWillRenameFilesParams)
                }

                doSend(e, t) {
                    const i = this._client.middleware.workspace;
                    return i?.willRenameFiles ? i.willRenameFiles(e, t) : t(e)
                }
            };
            t.WillDeleteFilesFeature = class extends p {
                constructor(e) {
                    super(e, n.workspace.onWillDeleteFiles, r.WillDeleteFilesRequest.type, "willDelete", "willDelete", (e => e), e.code2ProtocolConverter.asWillDeleteFilesParams)
                }

                doSend(e, t) {
                    const i = this._client.middleware.workspace;
                    return i?.willDeleteFiles ? i.willDeleteFiles(e, t) : t(e)
                }
            }
        }, 7750: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.FileSystemWatcherFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151);
            t.FileSystemWatcherFeature = class {
                constructor(e, t) {
                    this._client = e, this._notifyFileEvent = t, this._watchers = new Map
                }

                getState() {
                    return {kind: "workspace", id: this.registrationType.method, registrations: this._watchers.size > 0}
                }

                get registrationType() {
                    return o.DidChangeWatchedFilesNotification.type
                }

                fillClientCapabilities(e) {
                    (0, r.ensure)((0, r.ensure)(e, "workspace"), "didChangeWatchedFiles").dynamicRegistration = !0, (0, r.ensure)((0, r.ensure)(e, "workspace"), "didChangeWatchedFiles").relativePatternSupport = !0
                }

                initialize(e, t) {
                }

                register(e) {
                    if (!Array.isArray(e.registerOptions.watchers)) return;
                    const t = [];
                    for (const i of e.registerOptions.watchers) {
                        const e = this._client.protocol2CodeConverter.asGlobPattern(i.globPattern);
                        if (void 0 === e) continue;
                        let r = !0, s = !0, a = !0;
                        void 0 !== i.kind && null !== i.kind && (r = 0 != (i.kind & o.WatchKind.Create), s = 0 != (i.kind & o.WatchKind.Change), a = 0 != (i.kind & o.WatchKind.Delete));
                        const c = n.workspace.createFileSystemWatcher(e, !r, !s, !a);
                        this.hookListeners(c, r, s, a, t), t.push(c)
                    }
                    this._watchers.set(e.id, t)
                }

                registerRaw(e, t) {
                    let i = [];
                    for (let e of t) this.hookListeners(e, !0, !0, !0, i);
                    this._watchers.set(e, i)
                }

                hookListeners(e, t, i, n, r) {
                    t && e.onDidCreate((e => this._notifyFileEvent({
                        uri: this._client.code2ProtocolConverter.asUri(e),
                        type: o.FileChangeType.Created
                    })), null, r), i && e.onDidChange((e => this._notifyFileEvent({
                        uri: this._client.code2ProtocolConverter.asUri(e),
                        type: o.FileChangeType.Changed
                    })), null, r), n && e.onDidDelete((e => this._notifyFileEvent({
                        uri: this._client.code2ProtocolConverter.asUri(e),
                        type: o.FileChangeType.Deleted
                    })), null, r)
                }

                unregister(e) {
                    let t = this._watchers.get(e);
                    if (t) for (let e of t) e.dispose()
                }

                dispose() {
                    this._watchers.forEach((e => {
                        for (let t of e) t.dispose()
                    })), this._watchers.clear()
                }
            }
        }, 380: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.FoldingRangeFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151);

            class s extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.FoldingRangeRequest.type)
                }

                fillClientCapabilities(e) {
                    let t = (0, r.ensure)((0, r.ensure)(e, "textDocument"), "foldingRange");
                    t.dynamicRegistration = !0, t.rangeLimit = 5e3, t.lineFoldingOnly = !0, t.foldingRangeKind = {valueSet: [o.FoldingRangeKind.Comment, o.FoldingRangeKind.Imports, o.FoldingRangeKind.Region]}, t.foldingRange = {collapsedText: !1}
                }

                initialize(e, t) {
                    let [i, n] = this.getRegistration(t, e.foldingRangeProvider);
                    i && n && this.register({id: i, registerOptions: n})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideFoldingRanges: (e, t, i) => {
                            const n = this._client, r = (e, t, i) => {
                                const r = {textDocument: n.code2ProtocolConverter.asTextDocumentIdentifier(e)};
                                return n.sendRequest(o.FoldingRangeRequest.type, r, i).then((e => i.isCancellationRequested ? null : n.protocol2CodeConverter.asFoldingRanges(e, i)), (e => n.handleFailedRequest(o.FoldingRangeRequest.type, i, e, null)))
                            }, s = n.middleware;
                            return s.provideFoldingRanges ? s.provideFoldingRanges(e, t, i, r) : r(e, 0, i)
                        }
                    };
                    return [n.languages.registerFoldingRangeProvider(this._client.protocol2CodeConverter.asDocumentSelector(t), i), i]
                }
            }

            t.FoldingRangeFeature = s
        }, 4483: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DocumentOnTypeFormattingFeature = t.DocumentRangeFormattingFeature = t.DocumentFormattingFeature = void 0;
            const n = i(9496), o = i(3455), r = i(2678), s = i(5151);
            var a;
            !function (e) {
                e.fromConfiguration = function (e) {
                    const t = n.workspace.getConfiguration("files", e);
                    return {
                        trimTrailingWhitespace: t.get("trimTrailingWhitespace"),
                        trimFinalNewlines: t.get("trimFinalNewlines"),
                        insertFinalNewline: t.get("insertFinalNewline")
                    }
                }
            }(a || (a = {}));

            class c extends s.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.DocumentFormattingRequest.type)
                }

                fillClientCapabilities(e) {
                    (0, s.ensure)((0, s.ensure)(e, "textDocument"), "formatting").dynamicRegistration = !0
                }

                initialize(e, t) {
                    const i = this.getRegistrationOptions(t, e.documentFormattingProvider);
                    i && this.register({id: r.generateUuid(), registerOptions: i})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideDocumentFormattingEdits: (e, t, i) => {
                            const n = this._client, r = (e, t, i) => {
                                const r = {
                                    textDocument: n.code2ProtocolConverter.asTextDocumentIdentifier(e),
                                    options: n.code2ProtocolConverter.asFormattingOptions(t, a.fromConfiguration(e))
                                };
                                return n.sendRequest(o.DocumentFormattingRequest.type, r, i).then((e => i.isCancellationRequested ? null : n.protocol2CodeConverter.asTextEdits(e, i)), (e => n.handleFailedRequest(o.DocumentFormattingRequest.type, i, e, null)))
                            }, s = n.middleware;
                            return s.provideDocumentFormattingEdits ? s.provideDocumentFormattingEdits(e, t, i, r) : r(e, t, i)
                        }
                    };
                    return [n.languages.registerDocumentFormattingEditProvider(this._client.protocol2CodeConverter.asDocumentSelector(t), i), i]
                }
            }

            t.DocumentFormattingFeature = c;

            class l extends s.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.DocumentRangeFormattingRequest.type)
                }

                fillClientCapabilities(e) {
                    (0, s.ensure)((0, s.ensure)(e, "textDocument"), "rangeFormatting").dynamicRegistration = !0
                }

                initialize(e, t) {
                    const i = this.getRegistrationOptions(t, e.documentRangeFormattingProvider);
                    i && this.register({id: r.generateUuid(), registerOptions: i})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideDocumentRangeFormattingEdits: (e, t, i, n) => {
                            const r = this._client, s = (e, t, i, n) => {
                                const s = {
                                    textDocument: r.code2ProtocolConverter.asTextDocumentIdentifier(e),
                                    range: r.code2ProtocolConverter.asRange(t),
                                    options: r.code2ProtocolConverter.asFormattingOptions(i, a.fromConfiguration(e))
                                };
                                return r.sendRequest(o.DocumentRangeFormattingRequest.type, s, n).then((e => n.isCancellationRequested ? null : r.protocol2CodeConverter.asTextEdits(e, n)), (e => r.handleFailedRequest(o.DocumentRangeFormattingRequest.type, n, e, null)))
                            }, c = r.middleware;
                            return c.provideDocumentRangeFormattingEdits ? c.provideDocumentRangeFormattingEdits(e, t, i, n, s) : s(e, t, i, n)
                        }
                    };
                    return [n.languages.registerDocumentRangeFormattingEditProvider(this._client.protocol2CodeConverter.asDocumentSelector(t), i), i]
                }
            }

            t.DocumentRangeFormattingFeature = l;

            class u extends s.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.DocumentOnTypeFormattingRequest.type)
                }

                fillClientCapabilities(e) {
                    (0, s.ensure)((0, s.ensure)(e, "textDocument"), "onTypeFormatting").dynamicRegistration = !0
                }

                initialize(e, t) {
                    const i = this.getRegistrationOptions(t, e.documentOnTypeFormattingProvider);
                    i && this.register({id: r.generateUuid(), registerOptions: i})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideOnTypeFormattingEdits: (e, t, i, n, r) => {
                            const s = this._client, c = (e, t, i, n, r) => {
                                let c = {
                                    textDocument: s.code2ProtocolConverter.asTextDocumentIdentifier(e),
                                    position: s.code2ProtocolConverter.asPosition(t),
                                    ch: i,
                                    options: s.code2ProtocolConverter.asFormattingOptions(n, a.fromConfiguration(e))
                                };
                                return s.sendRequest(o.DocumentOnTypeFormattingRequest.type, c, r).then((e => r.isCancellationRequested ? null : s.protocol2CodeConverter.asTextEdits(e, r)), (e => s.handleFailedRequest(o.DocumentOnTypeFormattingRequest.type, r, e, null)))
                            }, l = s.middleware;
                            return l.provideOnTypeFormattingEdits ? l.provideOnTypeFormattingEdits(e, t, i, n, r, c) : c(e, t, i, n, r)
                        }
                    }, r = e.moreTriggerCharacter || [];
                    return [n.languages.registerOnTypeFormattingEditProvider(this._client.protocol2CodeConverter.asDocumentSelector(t), i, e.firstTriggerCharacter, ...r), i]
                }
            }

            t.DocumentOnTypeFormattingFeature = u
        }, 2293: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.HoverFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151), s = i(2678);

            class a extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.HoverRequest.type)
                }

                fillClientCapabilities(e) {
                    const t = (0, r.ensure)((0, r.ensure)(e, "textDocument"), "hover");
                    t.dynamicRegistration = !0, t.contentFormat = [o.MarkupKind.Markdown, o.MarkupKind.PlainText]
                }

                initialize(e, t) {
                    const i = this.getRegistrationOptions(t, e.hoverProvider);
                    i && this.register({id: s.generateUuid(), registerOptions: i})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideHover: (e, t, i) => {
                            const n = this._client,
                                r = (e, t, i) => n.sendRequest(o.HoverRequest.type, n.code2ProtocolConverter.asTextDocumentPositionParams(e, t), i).then((e => i.isCancellationRequested ? null : n.protocol2CodeConverter.asHover(e)), (e => n.handleFailedRequest(o.HoverRequest.type, i, e, null))),
                                s = n.middleware;
                            return s.provideHover ? s.provideHover(e, t, i, r) : r(e, t, i)
                        }
                    };
                    return [this.registerProvider(t, i), i]
                }

                registerProvider(e, t) {
                    return n.languages.registerHoverProvider(this._client.protocol2CodeConverter.asDocumentSelector(e), t)
                }
            }

            t.HoverFeature = a
        }, 4111: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ImplementationFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151);

            class s extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.ImplementationRequest.type)
                }

                fillClientCapabilities(e) {
                    let t = (0, r.ensure)((0, r.ensure)(e, "textDocument"), "implementation");
                    t.dynamicRegistration = !0, t.linkSupport = !0
                }

                initialize(e, t) {
                    let [i, n] = this.getRegistration(t, e.implementationProvider);
                    i && n && this.register({id: i, registerOptions: n})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideImplementation: (e, t, i) => {
                            const n = this._client,
                                r = (e, t, i) => n.sendRequest(o.ImplementationRequest.type, n.code2ProtocolConverter.asTextDocumentPositionParams(e, t), i).then((e => i.isCancellationRequested ? null : n.protocol2CodeConverter.asDefinitionResult(e, i)), (e => n.handleFailedRequest(o.ImplementationRequest.type, i, e, null))),
                                s = n.middleware;
                            return s.provideImplementation ? s.provideImplementation(e, t, i, r) : r(e, t, i)
                        }
                    };
                    return [this.registerProvider(t, i), i]
                }

                registerProvider(e, t) {
                    return n.languages.registerImplementationProvider(this._client.protocol2CodeConverter.asDocumentSelector(e), t)
                }
            }

            t.ImplementationFeature = s
        }, 6790: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.InlayHintsFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151);

            class s extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.InlayHintRequest.type)
                }

                fillClientCapabilities(e) {
                    const t = (0, r.ensure)((0, r.ensure)(e, "textDocument"), "inlayHint");
                    t.dynamicRegistration = !0, t.resolveSupport = {properties: ["tooltip", "textEdits", "label.tooltip", "label.location", "label.command"]}, (0, r.ensure)((0, r.ensure)(e, "workspace"), "inlayHint").refreshSupport = !0
                }

                initialize(e, t) {
                    this._client.onRequest(o.InlayHintRefreshRequest.type, (async () => {
                        for (const e of this.getAllProviders()) e.onDidChangeInlayHints.fire()
                    }));
                    const [i, n] = this.getRegistration(t, e.inlayHintProvider);
                    i && n && this.register({id: i, registerOptions: n})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = new n.EventEmitter, r = {
                        onDidChangeInlayHints: i.event, provideInlayHints: (e, t, i) => {
                            const n = this._client, r = async (e, t, i) => {
                                const r = {
                                    textDocument: n.code2ProtocolConverter.asTextDocumentIdentifier(e),
                                    range: n.code2ProtocolConverter.asRange(t)
                                };
                                try {
                                    const e = await n.sendRequest(o.InlayHintRequest.type, r, i);
                                    return i.isCancellationRequested ? null : n.protocol2CodeConverter.asInlayHints(e, i)
                                } catch (e) {
                                    return n.handleFailedRequest(o.InlayHintRequest.type, i, e, null)
                                }
                            }, s = n.middleware;
                            return s.provideInlayHints ? s.provideInlayHints(e, t, i, r) : r(e, t, i)
                        }
                    };
                    return r.resolveInlayHint = !0 === e.resolveProvider ? (e, t) => {
                        const i = this._client, n = async (e, t) => {
                            try {
                                const n = await i.sendRequest(o.InlayHintResolveRequest.type, i.code2ProtocolConverter.asInlayHint(e), t);
                                if (t.isCancellationRequested) return null;
                                const r = i.protocol2CodeConverter.asInlayHint(n, t);
                                return t.isCancellationRequested ? null : r
                            } catch (e) {
                                return i.handleFailedRequest(o.InlayHintResolveRequest.type, t, e, null)
                            }
                        }, r = i.middleware;
                        return r.resolveInlayHint ? r.resolveInlayHint(e, t, n) : n(e, t)
                    } : void 0, [this.registerProvider(t, r), {provider: r, onDidChangeInlayHints: i}]
                }

                registerProvider(e, t) {
                    return n.languages.registerInlayHintsProvider(this._client.protocol2CodeConverter.asDocumentSelector(e), t)
                }
            }

            t.InlayHintsFeature = s
        }, 7830: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.InlineValueFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151);

            class s extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.InlineValueRequest.type)
                }

                fillClientCapabilities(e) {
                    (0, r.ensure)((0, r.ensure)(e, "textDocument"), "inlineValue").dynamicRegistration = !0, (0, r.ensure)((0, r.ensure)(e, "workspace"), "inlineValue").refreshSupport = !0
                }

                initialize(e, t) {
                    this._client.onRequest(o.InlineValueRefreshRequest.type, (async () => {
                        for (const e of this.getAllProviders()) e.onDidChangeInlineValues.fire()
                    }));
                    const [i, n] = this.getRegistration(t, e.inlineValueProvider);
                    i && n && this.register({id: i, registerOptions: n})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = new n.EventEmitter, r = {
                        onDidChangeInlineValues: i.event, provideInlineValues: (e, t, i, n) => {
                            const r = this._client, s = (e, t, i, n) => {
                                const s = {
                                    textDocument: r.code2ProtocolConverter.asTextDocumentIdentifier(e),
                                    range: r.code2ProtocolConverter.asRange(t),
                                    context: r.code2ProtocolConverter.asInlineValueContext(i)
                                };
                                return r.sendRequest(o.InlineValueRequest.type, s, n).then((e => n.isCancellationRequested ? null : r.protocol2CodeConverter.asInlineValues(e, n)), (e => r.handleFailedRequest(o.InlineValueRequest.type, n, e, null)))
                            }, a = r.middleware;
                            return a.provideInlineValues ? a.provideInlineValues(e, t, i, n, s) : s(e, t, i, n)
                        }
                    };
                    return [this.registerProvider(t, r), {provider: r, onDidChangeInlineValues: i}]
                }

                registerProvider(e, t) {
                    return n.languages.registerInlineValuesProvider(this._client.protocol2CodeConverter.asDocumentSelector(e), t)
                }
            }

            t.InlineValueFeature = s
        }, 2258: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.LinkedEditingFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151);

            class s extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.LinkedEditingRangeRequest.type)
                }

                fillClientCapabilities(e) {
                    (0, r.ensure)((0, r.ensure)(e, "textDocument"), "linkedEditingRange").dynamicRegistration = !0
                }

                initialize(e, t) {
                    let [i, n] = this.getRegistration(t, e.linkedEditingRangeProvider);
                    i && n && this.register({id: i, registerOptions: n})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideLinkedEditingRanges: (e, t, i) => {
                            const n = this._client,
                                r = (e, t, i) => n.sendRequest(o.LinkedEditingRangeRequest.type, n.code2ProtocolConverter.asTextDocumentPositionParams(e, t), i).then((e => i.isCancellationRequested ? null : n.protocol2CodeConverter.asLinkedEditingRanges(e, i)), (e => n.handleFailedRequest(o.LinkedEditingRangeRequest.type, i, e, null))),
                                s = n.middleware;
                            return s.provideLinkedEditingRange ? s.provideLinkedEditingRange(e, t, i, r) : r(e, t, i)
                        }
                    };
                    return [this.registerProvider(t, i), i]
                }

                registerProvider(e, t) {
                    return n.languages.registerLinkedEditingRangeProvider(this._client.protocol2CodeConverter.asDocumentSelector(e), t)
                }
            }

            t.LinkedEditingFeature = s
        }, 8565: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.NotebookDocumentSyncFeature = void 0;
            const n = i(9496), o = i(9842), r = i(3455), s = i(2678), a = i(2592);

            function c(e, t) {
                return void 0 === e[t] && (e[t] = {}), e[t]
            }

            var l, u, d, p, h;
            !function (e) {
                let t;
                !function (t) {
                    function i(e, t) {
                        return e.map((e => s(e, t)))
                    }

                    function o(e) {
                        return c(new Set, e)
                    }

                    function s(e, t) {
                        const i = r.NotebookCell.create(function (e) {
                            switch (e) {
                                case n.NotebookCellKind.Markup:
                                    return r.NotebookCellKind.Markup;
                                case n.NotebookCellKind.Code:
                                    return r.NotebookCellKind.Code
                            }
                        }(e.kind), t.asUri(e.document.uri));
                        return Object.keys(e.metadata).length > 0 && (i.metadata = o(e.metadata)), void 0 !== e.executionSummary && a.number(e.executionSummary.executionOrder) && a.boolean(e.executionSummary.success) && (i.executionSummary = {
                            executionOrder: e.executionSummary.executionOrder,
                            success: e.executionSummary.success
                        }), i
                    }

                    function c(e, t) {
                        if (e.has(t)) throw new Error("Can't deep copy cyclic structures.");
                        if (Array.isArray(t)) {
                            const i = [];
                            for (const n of t) if (null !== n && "object" == typeof n || Array.isArray(n)) i.push(c(e, n)); else {
                                if (n instanceof RegExp) throw new Error("Can't transfer regular expressions to the server");
                                i.push(n)
                            }
                            return i
                        }
                        {
                            const i = Object.keys(t), n = Object.create(null);
                            for (const o of i) {
                                const i = t[o];
                                if (null !== i && "object" == typeof i || Array.isArray(i)) n[o] = c(e, i); else {
                                    if (i instanceof RegExp) throw new Error("Can't transfer regular expressions to the server");
                                    n[o] = i
                                }
                            }
                            return n
                        }
                    }

                    t.asVersionedNotebookDocumentIdentifier = function (e, t) {
                        return {version: e.version, uri: t.asUri(e.uri)}
                    }, t.asNotebookDocument = function (e, t, n) {
                        const s = r.NotebookDocument.create(n.asUri(e.uri), e.notebookType, e.version, i(t, n));
                        return Object.keys(e.metadata).length > 0 && (s.metadata = o(e.metadata)), s
                    }, t.asNotebookCells = i, t.asMetadata = o, t.asNotebookCell = s, t.asTextContentChange = function (e, t) {
                        const i = t.asChangeTextDocumentParams(e);
                        return {document: i.textDocument, changes: i.contentChanges}
                    }, t.asNotebookDocumentChangeEvent = function (t, i) {
                        const n = Object.create(null);
                        if (t.metadata && (n.metadata = e.c2p.asMetadata(t.metadata)), void 0 !== t.cells) {
                            const o = Object.create(null), r = t.cells;
                            r.structure && (o.structure = {
                                array: {
                                    start: r.structure.array.start,
                                    deleteCount: r.structure.array.deleteCount,
                                    cells: void 0 !== r.structure.array.cells ? r.structure.array.cells.map((t => e.c2p.asNotebookCell(t, i))) : void 0
                                },
                                didOpen: void 0 !== r.structure.didOpen ? r.structure.didOpen.map((e => i.asOpenTextDocumentParams(e.document).textDocument)) : void 0,
                                didClose: void 0 !== r.structure.didClose ? r.structure.didClose.map((e => i.asCloseTextDocumentParams(e.document).textDocument)) : void 0
                            }), void 0 !== r.data && (o.data = r.data.map((t => e.c2p.asNotebookCell(t, i)))), void 0 !== r.textContent && (o.textContent = r.textContent.map((t => e.c2p.asTextContentChange(t, i)))), Object.keys(o).length > 0 && (n.cells = o)
                        }
                        return n
                    }
                }(t = e.c2p || (e.c2p = {}))
            }(l || (l = {})), function (e) {
                function t(e, t, n = !0) {
                    return !(e.kind !== t.kind || e.document.uri.toString() !== t.document.uri.toString() || e.document.languageId !== t.document.languageId || !function (e, t) {
                        if (e === t) return !0;
                        if (void 0 === e || void 0 === t) return !1;
                        return e.executionOrder === t.executionOrder && e.success === t.success && function (e, t) {
                            if (e === t) return !0;
                            if (void 0 === e || void 0 === t) return !1;
                            return e.startTime === t.startTime && e.endTime === t.endTime
                        }(e.timing, t.timing)
                    }(e.executionSummary, t.executionSummary)) && (!n || n && i(e.metadata, t.metadata))
                }

                function i(e, t) {
                    if (e === t) return !0;
                    if (null == e || null == t) return !1;
                    if (typeof e != typeof t) return !1;
                    if ("object" != typeof e) return !1;
                    const o = Array.isArray(e), r = Array.isArray(t);
                    if (o !== r) return !1;
                    if (o && r) {
                        if (e.length !== t.length) return !1;
                        for (let n = 0; n < e.length; n++) if (!i(e[n], t[n])) return !1
                    }
                    if (n(e) && n(t)) {
                        const n = Object.keys(e), o = Object.keys(t);
                        if (n.length !== o.length) return !1;
                        if (n.sort(), o.sort(), !i(n, o)) return !1;
                        for (let o = 0; o < n.length; o++) {
                            const r = n[o];
                            if (!i(e[r], t[r])) return !1
                        }
                        return !0
                    }
                    return !1
                }

                function n(e) {
                    return null !== e && "object" == typeof e
                }

                e.computeDiff = function (e, i, n) {
                    const o = e.length, r = i.length;
                    let s = 0;
                    for (; s < r && s < o && t(e[s], i[s], n);) s++;
                    if (s < r && s < o) {
                        let a = o - 1, c = r - 1;
                        for (; a >= 0 && c >= 0 && t(e[a], i[c], n);) a--, c--;
                        const l = a + 1 - s, u = s === c + 1 ? void 0 : i.slice(s, c + 1);
                        return void 0 !== u ? {start: s, deleteCount: l, cells: u} : {start: s, deleteCount: l}
                    }
                    return s < r ? {start: s, deleteCount: 0, cells: i.slice(s)} : s < o ? {
                        start: s,
                        deleteCount: o - s
                    } : void 0
                }, e.isObjectLiteral = n
            }(u || (u = {})), function (e) {
                e.matchNotebook = function (e, t) {
                    if ("string" == typeof e) return "*" === e || t.notebookType === e;
                    if (void 0 !== e.notebookType && "*" !== e.notebookType && t.notebookType !== e.notebookType) return !1;
                    const i = t.uri;
                    if (void 0 !== e.scheme && "*" !== e.scheme && i.scheme !== e.scheme) return !1;
                    if (void 0 !== e.pattern) {
                        const t = new o.Minimatch(e.pattern, {noext: !0});
                        if (!t.makeRe()) return !1;
                        if (!t.match(i.fsPath)) return !1
                    }
                    return !0
                }
            }(d || (d = {})), function (e) {
                function t(e, t, i, n) {
                    return void 0 === t && void 0 === i ? {notebook: e, language: n} : {
                        notebook: {
                            notebookType: e,
                            scheme: t,
                            pattern: i
                        }, language: n
                    }
                }

                e.asDocumentSelector = function (e) {
                    const i = e.notebookSelector, n = [];
                    for (const e of i) {
                        const i = ("string" == typeof e.notebook ? e.notebook : e.notebook?.notebookType) ?? "*",
                            o = "string" == typeof e.notebook ? void 0 : e.notebook?.scheme,
                            r = "string" == typeof e.notebook ? void 0 : e.notebook?.pattern;
                        if (void 0 !== e.cells) for (const s of e.cells) n.push(t(i, o, r, s.language)); else n.push(t(i, o, r, void 0))
                    }
                    return n
                }
            }(p || (p = {})), function (e) {
                e.create = function (e) {
                    return {cells: e, uris: new Set(e.map((e => e.document.uri.toString())))}
                }
            }(h || (h = {}));

            class g {
                constructor(e, t) {
                    this.client = e, this.options = t, this.notebookSyncInfo = new Map, this.notebookDidOpen = new Set, this.disposables = [], this.selector = e.protocol2CodeConverter.asDocumentSelector(p.asDocumentSelector(t)), n.workspace.onDidOpenNotebookDocument((e => {
                        this.notebookDidOpen.add(e.uri.toString()), this.didOpen(e)
                    }), void 0, this.disposables);
                    for (const e of n.workspace.notebookDocuments) this.notebookDidOpen.add(e.uri.toString()), this.didOpen(e);
                    n.workspace.onDidChangeNotebookDocument((e => this.didChangeNotebookDocument(e)), void 0, this.disposables), !0 === this.options.save && n.workspace.onDidSaveNotebookDocument((e => this.didSave(e)), void 0, this.disposables), n.workspace.onDidCloseNotebookDocument((e => {
                        this.didClose(e), this.notebookDidOpen.delete(e.uri.toString())
                    }), void 0, this.disposables)
                }

                getState() {
                    for (const e of n.workspace.notebookDocuments) {
                        if (void 0 !== this.getMatchingCells(e)) return {
                            kind: "document",
                            id: "$internal",
                            registrations: !0,
                            matches: !0
                        }
                    }
                    return {kind: "document", id: "$internal", registrations: !0, matches: !1}
                }

                get mode() {
                    return "notebook"
                }

                handles(e) {
                    return n.languages.match(this.selector, e) > 0
                }

                didOpenNotebookCellTextDocument(e, t) {
                    if (0 === n.languages.match(this.selector, t.document)) return;
                    if (!this.notebookDidOpen.has(e.uri.toString())) return;
                    const i = this.notebookSyncInfo.get(e.uri.toString()), o = this.cellMatches(e, t);
                    if (void 0 !== i) {
                        const n = i.uris.has(t.document.uri.toString());
                        if (o && n || !o && !n) return;
                        if (o) {
                            const t = this.getMatchingCells(e);
                            if (void 0 !== t) {
                                const n = this.asNotebookDocumentChangeEvent(e, void 0, i, t);
                                void 0 !== n && this.doSendChange(n, t).catch((() => {
                                }))
                            }
                        }
                    } else o && this.doSendOpen(e, [t]).catch((() => {
                    }))
                }

                didChangeNotebookCellTextDocument(e, t) {
                    0 !== n.languages.match(this.selector, t.document) && this.doSendChange({
                        notebook: e,
                        cells: {textContent: [t]}
                    }, void 0).catch((() => {
                    }))
                }

                didCloseNotebookCellTextDocument(e, t) {
                    const i = this.notebookSyncInfo.get(e.uri.toString());
                    if (void 0 === i) return;
                    const n = t.document.uri, o = i.cells.findIndex((e => e.document.uri.toString() === n.toString()));
                    if (-1 !== o) if (0 === o && 1 === i.cells.length) this.doSendClose(e, i.cells).catch((() => {
                    })); else {
                        const t = i.cells.slice(), n = t.splice(o, 1);
                        this.doSendChange({
                            notebook: e,
                            cells: {structure: {array: {start: o, deleteCount: 1}, didClose: n}}
                        }, t).catch((() => {
                        }))
                    }
                }

                dispose() {
                    for (const e of this.disposables) e.dispose()
                }

                didOpen(e, t = this.getMatchingCells(e), i = this.notebookSyncInfo.get(e.uri.toString())) {
                    if (void 0 !== i) if (void 0 !== t) {
                        const n = this.asNotebookDocumentChangeEvent(e, void 0, i, t);
                        void 0 !== n && this.doSendChange(n, t).catch((() => {
                        }))
                    } else this.doSendClose(e, []).catch((() => {
                    })); else {
                        if (void 0 === t) return;
                        this.doSendOpen(e, t).catch((() => {
                        }))
                    }
                }

                didChangeNotebookDocument(e) {
                    const t = e.notebook, i = this.notebookSyncInfo.get(t.uri.toString());
                    if (void 0 === i) {
                        if (0 === e.contentChanges.length) return;
                        const n = this.getMatchingCells(t);
                        if (void 0 === n) return;
                        this.didOpen(t, n, i)
                    } else {
                        const n = this.getMatchingCells(t);
                        if (void 0 === n) return void this.didClose(t, i);
                        const o = this.asNotebookDocumentChangeEvent(e.notebook, e, i, n);
                        void 0 !== o && this.doSendChange(o, n).catch((() => {
                        }))
                    }
                }

                didSave(e) {
                    void 0 !== this.notebookSyncInfo.get(e.uri.toString()) && this.doSendSave(e).catch((() => {
                    }))
                }

                didClose(e, t = this.notebookSyncInfo.get(e.uri.toString())) {
                    if (void 0 === t) return;
                    const i = e.getCells().filter((e => t.uris.has(e.document.uri.toString())));
                    this.doSendClose(e, i).catch((() => {
                    }))
                }

                async sendDidOpenNotebookDocument(e) {
                    const t = this.getMatchingCells(e);
                    if (void 0 !== t) return this.doSendOpen(e, t)
                }

                async doSendOpen(e, t) {
                    const i = async (e, t) => {
                        const i = l.c2p.asNotebookDocument(e, t, this.client.code2ProtocolConverter),
                            n = t.map((e => this.client.code2ProtocolConverter.asTextDocumentItem(e.document)));
                        try {
                            await this.client.sendNotification(r.DidOpenNotebookDocumentNotification.type, {
                                notebookDocument: i,
                                cellTextDocuments: n
                            })
                        } catch (e) {
                            throw this.client.error("Sending DidOpenNotebookDocumentNotification failed", e), e
                        }
                    }, n = this.client.middleware?.notebooks;
                    return this.notebookSyncInfo.set(e.uri.toString(), h.create(t)), void 0 !== n?.didOpen ? n.didOpen(e, t, i) : i(e, t)
                }

                async sendDidChangeNotebookDocument(e) {
                    return this.doSendChange(e, void 0)
                }

                async doSendChange(e, t = this.getMatchingCells(e.notebook)) {
                    const i = async e => {
                        try {
                            await this.client.sendNotification(r.DidChangeNotebookDocumentNotification.type, {
                                notebookDocument: l.c2p.asVersionedNotebookDocumentIdentifier(e.notebook, this.client.code2ProtocolConverter),
                                change: l.c2p.asNotebookDocumentChangeEvent(e, this.client.code2ProtocolConverter)
                            })
                        } catch (e) {
                            throw this.client.error("Sending DidChangeNotebookDocumentNotification failed", e), e
                        }
                    }, n = this.client.middleware?.notebooks;
                    return void 0 !== e.cells?.structure && this.notebookSyncInfo.set(e.notebook.uri.toString(), h.create(t ?? [])), void 0 !== n?.didChange ? n?.didChange(e, i) : i(e)
                }

                async sendDidSaveNotebookDocument(e) {
                    return this.doSendSave(e)
                }

                async doSendSave(e) {
                    const t = async e => {
                        try {
                            await this.client.sendNotification(r.DidSaveNotebookDocumentNotification.type, {notebookDocument: {uri: this.client.code2ProtocolConverter.asUri(e.uri)}})
                        } catch (e) {
                            throw this.client.error("Sending DidSaveNotebookDocumentNotification failed", e), e
                        }
                    }, i = this.client.middleware?.notebooks;
                    return void 0 !== i?.didSave ? i.didSave(e, t) : t(e)
                }

                async sendDidCloseNotebookDocument(e) {
                    return this.doSendClose(e, this.getMatchingCells(e) ?? [])
                }

                async doSendClose(e, t) {
                    const i = async (e, t) => {
                        try {
                            await this.client.sendNotification(r.DidCloseNotebookDocumentNotification.type, {
                                notebookDocument: {uri: this.client.code2ProtocolConverter.asUri(e.uri)},
                                cellTextDocuments: t.map((e => this.client.code2ProtocolConverter.asTextDocumentIdentifier(e.document)))
                            })
                        } catch (e) {
                            throw this.client.error("Sending DidCloseNotebookDocumentNotification failed", e), e
                        }
                    }, n = this.client.middleware?.notebooks;
                    return this.notebookSyncInfo.delete(e.uri.toString()), void 0 !== n?.didClose ? n.didClose(e, t, i) : i(e, t)
                }

                asNotebookDocumentChangeEvent(e, t, i, n) {
                    if (void 0 !== t && t.notebook !== e) throw new Error("Notebook must be identical");
                    const o = {notebook: e};
                    let r;
                    if (void 0 !== t?.metadata && (o.metadata = l.c2p.asMetadata(t.metadata)), void 0 !== t?.cellChanges && t.cellChanges.length > 0) {
                        const e = [];
                        r = new Set(n.map((e => e.document.uri.toString())));
                        for (const i of t.cellChanges) !r.has(i.cell.document.uri.toString()) || void 0 === i.executionSummary && void 0 === i.metadata || e.push(i.cell);
                        e.length > 0 && (o.cells = o.cells ?? {}, o.cells.data = e)
                    }
                    if ((void 0 !== t?.contentChanges && t.contentChanges.length > 0 || void 0 === t) && void 0 !== i && void 0 !== n) {
                        const e = i.cells, t = n, r = u.computeDiff(e, t, !1);
                        let s, a;
                        if (void 0 !== r) {
                            s = void 0 === r.cells ? new Map : new Map(r.cells.map((e => [e.document.uri.toString(), e]))), a = 0 === r.deleteCount ? new Map : new Map(e.slice(r.start, r.start + r.deleteCount).map((e => [e.document.uri.toString(), e])));
                            for (const e of Array.from(a.keys())) s.has(e) && (a.delete(e), s.delete(e));
                            o.cells = o.cells ?? {};
                            const t = [], i = [];
                            if (s.size > 0 || a.size > 0) {
                                for (const e of s.values()) t.push(e);
                                for (const e of a.values()) i.push(e)
                            }
                            o.cells.structure = {array: r, didOpen: t, didClose: i}
                        }
                    }
                    return Object.keys(o).length > 1 ? o : void 0
                }

                getMatchingCells(e, t = e.getCells()) {
                    if (void 0 !== this.options.notebookSelector) for (const i of this.options.notebookSelector) if (void 0 === i.notebook || d.matchNotebook(i.notebook, e)) {
                        const n = this.filterCells(e, t, i.cells);
                        return 0 === n.length ? void 0 : n
                    }
                }

                cellMatches(e, t) {
                    const i = this.getMatchingCells(e, [t]);
                    return void 0 !== i && i[0] === t
                }

                filterCells(e, t, i) {
                    const n = void 0 !== i ? t.filter((e => {
                        const t = e.document.languageId;
                        return i.some((e => "*" === e.language || t === e.language))
                    })) : t;
                    return "function" == typeof this.client.clientOptions.notebookDocumentOptions?.filterCells ? this.client.clientOptions.notebookDocumentOptions.filterCells(e, n) : n
                }
            }

            class m {
                constructor(e) {
                    this.client = e, this.registrations = new Map, this.registrationType = r.NotebookDocumentSyncRegistrationType.type, n.workspace.onDidOpenTextDocument((e => {
                        if (e.uri.scheme !== m.CellScheme) return;
                        const [t, i] = this.findNotebookDocumentAndCell(e);
                        if (void 0 !== t && void 0 !== i) for (const e of this.registrations.values()) e instanceof g && e.didOpenNotebookCellTextDocument(t, i)
                    })), n.workspace.onDidChangeTextDocument((e => {
                        if (0 === e.contentChanges.length) return;
                        const t = e.document;
                        if (t.uri.scheme !== m.CellScheme) return;
                        const [i] = this.findNotebookDocumentAndCell(t);
                        if (void 0 !== i) for (const t of this.registrations.values()) t instanceof g && t.didChangeNotebookCellTextDocument(i, e)
                    })), n.workspace.onDidCloseTextDocument((e => {
                        if (e.uri.scheme !== m.CellScheme) return;
                        const [t, i] = this.findNotebookDocumentAndCell(e);
                        if (void 0 !== t && void 0 !== i) for (const e of this.registrations.values()) e instanceof g && e.didCloseNotebookCellTextDocument(t, i)
                    }))
                }

                getState() {
                    if (0 === this.registrations.size) return {
                        kind: "document",
                        id: this.registrationType.method,
                        registrations: !1,
                        matches: !1
                    };
                    for (const e of this.registrations.values()) {
                        const t = e.getState();
                        if ("document" === t.kind && !0 === t.registrations && !0 === t.matches) return {
                            kind: "document",
                            id: this.registrationType.method,
                            registrations: !0,
                            matches: !0
                        }
                    }
                    return {kind: "document", id: this.registrationType.method, registrations: !0, matches: !1}
                }

                fillClientCapabilities(e) {
                    const t = c(c(e, "notebookDocument"), "synchronization");
                    t.dynamicRegistration = !0, t.executionSummarySupport = !0
                }

                preInitialize(e) {
                    const t = e.notebookDocumentSync;
                    void 0 !== t && (this.dedicatedChannel = this.client.protocol2CodeConverter.asDocumentSelector(p.asDocumentSelector(t)))
                }

                initialize(e) {
                    const t = e.notebookDocumentSync;
                    if (void 0 === t) return;
                    const i = t.id ?? s.generateUuid();
                    this.register({id: i, registerOptions: t})
                }

                register(e) {
                    const t = new g(this.client, e.registerOptions);
                    this.registrations.set(e.id, t)
                }

                unregister(e) {
                    const t = this.registrations.get(e);
                    t && t.dispose()
                }

                dispose() {
                    for (const e of this.registrations.values()) e.dispose();
                    this.registrations.clear()
                }

                handles(e) {
                    if (e.uri.scheme !== m.CellScheme) return !1;
                    if (void 0 !== this.dedicatedChannel && n.languages.match(this.dedicatedChannel, e) > 0) return !0;
                    for (const t of this.registrations.values()) if (t.handles(e)) return !0;
                    return !1
                }

                getProvider(e) {
                    for (const t of this.registrations.values()) if (t.handles(e.document)) return t
                }

                findNotebookDocumentAndCell(e) {
                    const t = e.uri.toString();
                    for (const e of n.workspace.notebookDocuments) for (const i of e.getCells()) if (i.document.uri.toString() === t) return [e, i];
                    return [void 0, void 0]
                }
            }

            t.NotebookDocumentSyncFeature = m, m.CellScheme = "vscode-notebook-cell"
        }, 7800: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ProgressFeature = void 0;
            const n = i(3455), o = i(448);
            t.ProgressFeature = class {
                constructor(e) {
                    this._client = e, this.activeParts = new Set
                }

                getState() {
                    return {
                        kind: "window",
                        id: n.WorkDoneProgressCreateRequest.method,
                        registrations: this.activeParts.size > 0
                    }
                }

                fillClientCapabilities(e) {
                    var t, i;
                    (t = e, i = "window", void 0 === t[i] && (t[i] = Object.create(null)), t[i]).workDoneProgress = !0
                }

                initialize() {
                    const e = this._client, t = e => {
                        this.activeParts.delete(e)
                    };
                    e.onRequest(n.WorkDoneProgressCreateRequest.type, (e => {
                        this.activeParts.add(new o.ProgressPart(this._client, e.token, t))
                    }))
                }

                dispose() {
                    for (const e of this.activeParts) e.done();
                    this.activeParts.clear()
                }
            }
        }, 448: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ProgressPart = void 0;
            const n = i(9496), o = i(3455), r = i(2592);
            t.ProgressPart = class {
                constructor(e, t, i) {
                    this._client = e, this._token = t, this._reported = 0, this._infinite = !1, this._lspProgressDisposable = this._client.onProgress(o.WorkDoneProgress.type, this._token, (e => {
                        switch (e.kind) {
                            case"begin":
                                this.begin(e);
                                break;
                            case"report":
                                this.report(e);
                                break;
                            case"end":
                                this.done(), i && i(this)
                        }
                    }))
                }

                begin(e) {
                    this._infinite = void 0 === e.percentage, void 0 !== this._lspProgressDisposable && n.window.withProgress({
                        location: n.ProgressLocation.Window,
                        cancellable: e.cancellable,
                        title: e.title
                    }, (async (t, i) => {
                        if (void 0 !== this._lspProgressDisposable) return this._progress = t, this._cancellationToken = i, this._tokenDisposable = this._cancellationToken.onCancellationRequested((() => {
                            this._client.sendNotification(o.WorkDoneProgressCancelNotification.type, {token: this._token})
                        })), this.report(e), new Promise(((e, t) => {
                            this._resolve = e, this._reject = t
                        }))
                    }))
                }

                report(e) {
                    if (this._infinite && r.string(e.message)) void 0 !== this._progress && this._progress.report({message: e.message}); else if (r.number(e.percentage)) {
                        const t = Math.max(0, Math.min(e.percentage, 100)), i = Math.max(0, t - this._reported);
                        this._reported += i, void 0 !== this._progress && this._progress.report({
                            message: e.message,
                            increment: i
                        })
                    }
                }

                cancel() {
                    this.cleanup(), void 0 !== this._reject && (this._reject(), this._resolve = void 0, this._reject = void 0)
                }

                done() {
                    this.cleanup(), void 0 !== this._resolve && (this._resolve(), this._resolve = void 0, this._reject = void 0)
                }

                cleanup() {
                    void 0 !== this._lspProgressDisposable && (this._lspProgressDisposable.dispose(), this._lspProgressDisposable = void 0), void 0 !== this._tokenDisposable && (this._tokenDisposable.dispose(), this._tokenDisposable = void 0), this._progress = void 0, this._cancellationToken = void 0
                }
            }
        }, 8889: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0});
            const n = i(9496);

            class o extends n.CallHierarchyItem {
                constructor(e, t, i, n, o, r, s) {
                    super(e, t, i, n, o, r), void 0 !== s && (this.data = s)
                }
            }

            t.default = o
        }, 7317: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0});
            const n = i(9496);

            class o extends n.CodeAction {
                constructor(e, t) {
                    super(e), this.data = t
                }
            }

            t.default = o
        }, 4332: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0});
            const n = i(9496);

            class o extends n.CodeLens {
                constructor(e) {
                    super(e)
                }
            }

            t.default = o
        }, 6103: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0});
            const n = i(9496);

            class o extends n.CompletionItem {
                constructor(e) {
                    super(e)
                }
            }

            t.default = o
        }, 1761: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.createConverter = void 0;
            const n = i(9496), o = i(3455), r = i(2592), s = i(2570), a = i(6103), c = i(4332), l = i(8039),
                u = i(7317), d = i(3055), p = i(8889), h = i(696), g = i(3572), m = i(6598), f = i(3455);
            var v;
            !function (e) {
                e.is = function (e) {
                    let t = e;
                    return t && r.string(t.language) && r.string(t.value)
                }
            }(v || (v = {})), t.createConverter = function (e, t, i) {
                const y = e || (e => n.Uri.parse(e));

                function C(e) {
                    return y(e)
                }

                function w(e) {
                    let t = new d.ProtocolDiagnostic(S(e.range), e.message, P(e.severity), e.data);
                    if (void 0 !== e.code) if ("string" == typeof e.code || "number" == typeof e.code) o.CodeDescription.is(e.codeDescription) ? t.code = {
                        value: e.code,
                        target: C(e.codeDescription.href)
                    } : t.code = e.code; else if (d.DiagnosticCode.is(e.code)) {
                        t.hasDiagnosticCode = !0;
                        const i = e.code;
                        t.code = {value: i.value, target: C(i.target)}
                    }
                    return e.source && (t.source = e.source), e.relatedInformation && (t.relatedInformation = function (e) {
                        const t = new Array(e.length);
                        for (let i = 0; i < e.length; i++) {
                            const o = e[i];
                            t[i] = new n.DiagnosticRelatedInformation(q(o.location), o.message)
                        }
                        return t
                    }(e.relatedInformation)), Array.isArray(e.tags) && (t.tags = function (e) {
                        if (!e) return;
                        let t = [];
                        for (let i of e) {
                            let e = b(i);
                            void 0 !== e && t.push(e)
                        }
                        return t.length > 0 ? t : void 0
                    }(e.tags)), t
                }

                function b(e) {
                    switch (e) {
                        case o.DiagnosticTag.Unnecessary:
                            return n.DiagnosticTag.Unnecessary;
                        case o.DiagnosticTag.Deprecated:
                            return n.DiagnosticTag.Deprecated;
                        default:
                            return
                    }
                }

                function k(e) {
                    return e ? new n.Position(e.line, e.character) : void 0
                }

                function S(e) {
                    return e ? new n.Range(e.start.line, e.start.character, e.end.line, e.end.character) : void 0
                }

                async function D(e, t) {
                    return s.map(e, (e => new n.Range(e.start.line, e.start.character, e.end.line, e.end.character)), t)
                }

                function P(e) {
                    if (null == e) return n.DiagnosticSeverity.Error;
                    switch (e) {
                        case o.DiagnosticSeverity.Error:
                            return n.DiagnosticSeverity.Error;
                        case o.DiagnosticSeverity.Warning:
                            return n.DiagnosticSeverity.Warning;
                        case o.DiagnosticSeverity.Information:
                            return n.DiagnosticSeverity.Information;
                        case o.DiagnosticSeverity.Hint:
                            return n.DiagnosticSeverity.Hint
                    }
                    return n.DiagnosticSeverity.Error
                }

                function T(e) {
                    if (r.string(e)) return e;
                    switch (e.kind) {
                        case o.MarkupKind.Markdown:
                            return R(e.value);
                        case o.MarkupKind.PlainText:
                            return e.value;
                        default:
                            return `Unsupported Markup content received. Kind is: ${e.kind}`
                    }
                }

                function R(e) {
                    let r;
                    if (void 0 === e || "string" == typeof e) r = new n.MarkdownString(e); else switch (e.kind) {
                        case o.MarkupKind.Markdown:
                            r = new n.MarkdownString(e.value);
                            break;
                        case o.MarkupKind.PlainText:
                            r = new n.MarkdownString, r.appendText(e.value);
                            break;
                        default:
                            r = new n.MarkdownString, r.appendText(`Unsupported Markup content received. Kind is: ${e.kind}`)
                    }
                    return r.isTrusted = t, r.supportHtml = i, r
                }

                function _(e) {
                    if (e === o.CompletionItemTag.Deprecated) return n.CompletionItemTag.Deprecated
                }

                function x(e, t, i, s, c, l) {
                    const u = function (e) {
                        if (null == e) return [];
                        const t = [];
                        for (const i of e) {
                            const e = _(i);
                            void 0 !== e && t.push(e)
                        }
                        return t
                    }(e.tags), d = function (e) {
                        return o.CompletionItemLabelDetails.is(e.labelDetails) ? {
                            label: e.label,
                            detail: e.labelDetails.detail,
                            description: e.labelDetails.description
                        } : e.label
                    }(e), p = new a.default(d);
                    e.detail && (p.detail = e.detail), e.documentation && (p.documentation = T(e.documentation), p.documentationFormat = r.string(e.documentation) ? "$string" : e.documentation.kind), e.filterText && (p.filterText = e.filterText);
                    const h = function (e, t, i) {
                        const r = e.insertTextFormat ?? i;
                        if (void 0 !== e.textEdit || void 0 !== t) {
                            const [i, a] = void 0 !== e.textEdit ? (s = e.textEdit, o.InsertReplaceEdit.is(s) ? [{
                                inserting: S(s.insert),
                                replacing: S(s.replace)
                            }, s.newText] : [S(s.range), s.newText]) : [t, e.textEditText ?? e.label];
                            return r === o.InsertTextFormat.Snippet ? {
                                text: new n.SnippetString(a),
                                range: i,
                                fromEdit: !0
                            } : {text: a, range: i, fromEdit: !0}
                        }
                        return e.insertText ? r === o.InsertTextFormat.Snippet ? {
                            text: new n.SnippetString(e.insertText),
                            fromEdit: !1
                        } : {text: e.insertText, fromEdit: !1} : void 0;
                        var s
                    }(e, i, c);
                    if (h && (p.insertText = h.text, p.range = h.range, p.fromEdit = h.fromEdit), r.number(e.kind)) {
                        let [t, i] = (g = e.kind, o.CompletionItemKind.Text <= g && g <= o.CompletionItemKind.TypeParameter ? [g - 1, void 0] : [n.CompletionItemKind.Text, g]);
                        p.kind = t, i && (p.originalItemKind = i)
                    }
                    var g;
                    e.sortText && (p.sortText = e.sortText), e.additionalTextEdits && (p.additionalTextEdits = j(e.additionalTextEdits));
                    const m = void 0 !== e.commitCharacters ? r.stringArray(e.commitCharacters) ? e.commitCharacters : void 0 : t;
                    m && (p.commitCharacters = m.slice()), e.command && (p.command = z(e.command)), !0 !== e.deprecated && !1 !== e.deprecated || (p.deprecated = e.deprecated, !0 === e.deprecated && u.push(n.CompletionItemTag.Deprecated)), !0 !== e.preselect && !1 !== e.preselect || (p.preselect = e.preselect);
                    const f = e.data ?? l;
                    void 0 !== f && (p.data = f), u.length > 0 && (p.tags = u);
                    const v = e.insertTextMode ?? s;
                    return void 0 !== v && (p.insertTextMode = v, v === o.InsertTextMode.asIs && (p.keepWhitespace = !0)), p
                }

                function E(e) {
                    if (e) return new n.TextEdit(S(e.range), e.newText)
                }

                async function O(e, t) {
                    if (e) return s.map(e, E, t)
                }

                function j(e) {
                    if (!e) return;
                    const t = new Array(e.length);
                    for (let i = 0; i < e.length; i++) t[i] = E(e[i]);
                    return t
                }

                async function M(e, t) {
                    return s.mapAsync(e, I, t)
                }

                async function I(e, t) {
                    let i = new n.SignatureInformation(e.label);
                    return void 0 !== e.documentation && (i.documentation = T(e.documentation)), void 0 !== e.parameters && (i.parameters = await F(e.parameters, t)), void 0 !== e.activeParameter && (i.activeParameter = e.activeParameter), i
                }

                function F(e, t) {
                    return s.map(e, N, t)
                }

                function N(e) {
                    let t = new n.ParameterInformation(e.label);
                    return e.documentation && (t.documentation = T(e.documentation)), t
                }

                function q(e) {
                    return e ? new n.Location(y(e.uri), S(e.range)) : void 0
                }

                function L(e) {
                    if (!e) return;
                    let t = {
                        targetUri: y(e.targetUri),
                        targetRange: S(e.targetRange),
                        originSelectionRange: S(e.originSelectionRange),
                        targetSelectionRange: S(e.targetSelectionRange)
                    };
                    if (!t.targetSelectionRange) throw new Error("targetSelectionRange must not be undefined or null");
                    return t
                }

                async function A(e, t) {
                    if (e) {
                        if (r.array(e)) {
                            if (0 === e.length) return [];
                            if (o.LocationLink.is(e[0])) {
                                const i = e;
                                return s.map(i, L, t)
                            }
                            {
                                const i = e;
                                return s.map(i, q, t)
                            }
                        }
                        return o.LocationLink.is(e) ? [L(e)] : q(e)
                    }
                }

                function $(e) {
                    let t = new n.DocumentHighlight(S(e.range));
                    return r.number(e.kind) && (t.kind = U(e.kind)), t
                }

                function U(e) {
                    switch (e) {
                        case o.DocumentHighlightKind.Text:
                            return n.DocumentHighlightKind.Text;
                        case o.DocumentHighlightKind.Read:
                            return n.DocumentHighlightKind.Read;
                        case o.DocumentHighlightKind.Write:
                            return n.DocumentHighlightKind.Write
                    }
                    return n.DocumentHighlightKind.Text
                }

                function H(e) {
                    return e <= o.SymbolKind.TypeParameter ? e - 1 : n.SymbolKind.Property
                }

                function W(e) {
                    if (e === o.SymbolTag.Deprecated) return n.SymbolTag.Deprecated
                }

                function V(e) {
                    if (null == e) return;
                    const t = [];
                    for (const i of e) {
                        const e = W(i);
                        void 0 !== e && t.push(e)
                    }
                    return 0 === t.length ? void 0 : t
                }

                function K(e) {
                    const t = e.data, i = e.location,
                        o = void 0 === i.range || void 0 !== t ? new g.default(e.name, H(e.kind), e.containerName ?? "", void 0 === i.range ? y(i.uri) : new n.Location(y(e.location.uri), S(i.range)), t) : new n.SymbolInformation(e.name, H(e.kind), e.containerName ?? "", new n.Location(y(e.location.uri), S(i.range)));
                    return J(o, e), o
                }

                function B(e) {
                    let t = new n.DocumentSymbol(e.name, e.detail || "", H(e.kind), S(e.range), S(e.selectionRange));
                    if (J(t, e), void 0 !== e.children && e.children.length > 0) {
                        let i = [];
                        for (let t of e.children) i.push(B(t));
                        t.children = i
                    }
                    return t
                }

                function J(e, t) {
                    e.tags = V(t.tags), t.deprecated && (e.tags ? e.tags.includes(n.SymbolTag.Deprecated) || (e.tags = e.tags.concat(n.SymbolTag.Deprecated)) : e.tags = [n.SymbolTag.Deprecated])
                }

                function z(e) {
                    let t = {title: e.title, command: e.command};
                    return e.arguments && (t.arguments = e.arguments), t
                }

                const G = new Map;

                function X(e) {
                    if (null == e) return;
                    let t = G.get(e);
                    if (t) return t;
                    let i = e.split(".");
                    t = n.CodeActionKind.Empty;
                    for (let e of i) t = t.append(e);
                    return t
                }

                async function Y(e, t) {
                    if (null == e) return;
                    let i = new u.default(e.title, e.data);
                    return void 0 !== e.kind && (i.kind = X(e.kind)), void 0 !== e.diagnostics && (i.diagnostics = function (e) {
                        const t = new Array(e.length);
                        for (let i = 0; i < e.length; i++) t[i] = w(e[i]);
                        return t
                    }(e.diagnostics)), void 0 !== e.edit && (i.edit = await Z(e.edit, t)), void 0 !== e.command && (i.command = z(e.command)), void 0 !== e.isPreferred && (i.isPreferred = e.isPreferred), void 0 !== e.disabled && (i.disabled = {reason: e.disabled.reason}), i
                }

                function Q(e) {
                    if (!e) return;
                    let t = new c.default(S(e.range));
                    return e.command && (t.command = z(e.command)), void 0 !== e.data && null !== e.data && (t.data = e.data), t
                }

                async function Z(e, t) {
                    if (!e) return;
                    const i = new Map;
                    if (void 0 !== e.changeAnnotations) {
                        const n = e.changeAnnotations;
                        await s.forEach(Object.keys(n), (e => {
                            const t = function (e) {
                                if (void 0 === e) return;
                                return {
                                    label: e.label,
                                    needsConfirmation: !!e.needsConfirmation,
                                    description: e.description
                                }
                            }(n[e]);
                            i.set(e, t)
                        }), t)
                    }
                    const r = e => void 0 === e ? void 0 : i.get(e), a = new n.WorkspaceEdit;
                    if (e.documentChanges) {
                        const i = e.documentChanges;
                        await s.forEach(i, (e => {
                            if (o.CreateFile.is(e)) a.createFile(y(e.uri), e.options, r(e.annotationId)); else if (o.RenameFile.is(e)) a.renameFile(y(e.oldUri), y(e.newUri), e.options, r(e.annotationId)); else if (o.DeleteFile.is(e)) a.deleteFile(y(e.uri), e.options, r(e.annotationId)); else {
                                if (!o.TextDocumentEdit.is(e)) throw new Error(`Unknown workspace edit change received:\n${JSON.stringify(e, void 0, 4)}`);
                                {
                                    const t = y(e.textDocument.uri);
                                    for (const i of e.edits) o.AnnotatedTextEdit.is(i) ? a.replace(t, S(i.range), i.newText, r(i.annotationId)) : a.replace(t, S(i.range), i.newText)
                                }
                            }
                        }), t)
                    } else if (e.changes) {
                        const i = e.changes;
                        await s.forEach(Object.keys(i), (e => {
                            a.set(y(e), j(i[e]))
                        }), t)
                    }
                    return a
                }

                function ee(e) {
                    let t = S(e.range), i = e.target ? C(e.target) : void 0, n = new l.default(t, i);
                    return void 0 !== e.tooltip && (n.tooltip = e.tooltip), void 0 !== e.data && null !== e.data && (n.data = e.data), n
                }

                function te(e) {
                    return new n.Color(e.red, e.green, e.blue, e.alpha)
                }

                function ie(e) {
                    return new n.ColorInformation(S(e.range), te(e.color))
                }

                function ne(e) {
                    let t = new n.ColorPresentation(e.label);
                    return t.additionalTextEdits = j(e.additionalTextEdits), e.textEdit && (t.textEdit = E(e.textEdit)), t
                }

                function oe(e) {
                    if (e) switch (e) {
                        case o.FoldingRangeKind.Comment:
                            return n.FoldingRangeKind.Comment;
                        case o.FoldingRangeKind.Imports:
                            return n.FoldingRangeKind.Imports;
                        case o.FoldingRangeKind.Region:
                            return n.FoldingRangeKind.Region
                    }
                }

                function re(e) {
                    return new n.FoldingRange(e.startLine, e.endLine, oe(e.kind))
                }

                function se(e) {
                    return new n.SelectionRange(S(e.range), e.parent ? se(e.parent) : void 0)
                }

                function ae(e) {
                    return o.InlineValueText.is(e) ? new n.InlineValueText(S(e.range), e.text) : o.InlineValueVariableLookup.is(e) ? new n.InlineValueVariableLookup(S(e.range), e.variableName, e.caseSensitiveLookup) : new n.InlineValueEvaluatableExpression(S(e.range), e.expression)
                }

                async function ce(e, t) {
                    const i = "string" == typeof e.label ? e.label : await s.map(e.label, le, t),
                        n = new m.default(k(e.position), i);
                    return void 0 !== e.kind && (n.kind = e.kind), void 0 !== e.textEdits && (n.textEdits = await O(e.textEdits, t)), void 0 !== e.tooltip && (n.tooltip = ue(e.tooltip)), void 0 !== e.paddingLeft && (n.paddingLeft = e.paddingLeft), void 0 !== e.paddingRight && (n.paddingRight = e.paddingRight), void 0 !== e.data && (n.data = e.data), n
                }

                function le(e) {
                    const t = new n.InlayHintLabelPart(e.value);
                    return void 0 !== e.location && (t.location = q(e.location)), void 0 !== e.tooltip && (t.tooltip = ue(e.tooltip)), void 0 !== e.command && (t.command = z(e.command)), t
                }

                function ue(e) {
                    return "string" == typeof e ? e : R(e)
                }

                function de(e) {
                    if (null === e) return;
                    const t = new p.default(H(e.kind), e.name, e.detail || "", C(e.uri), S(e.range), S(e.selectionRange), e.data);
                    return void 0 !== e.tags && (t.tags = V(e.tags)), t
                }

                async function pe(e, t) {
                    return new n.CallHierarchyIncomingCall(de(e.from), await D(e.fromRanges, t))
                }

                async function he(e, t) {
                    return new n.CallHierarchyOutgoingCall(de(e.to), await D(e.fromRanges, t))
                }

                function ge(e) {
                    return new n.SemanticTokensEdit(e.start, e.deleteCount, void 0 !== e.data ? new Uint32Array(e.data) : void 0)
                }

                function me(e) {
                    if (null === e) return;
                    let t = new h.default(H(e.kind), e.name, e.detail || "", C(e.uri), S(e.range), S(e.selectionRange), e.data);
                    return void 0 !== e.tags && (t.tags = V(e.tags)), t
                }

                return G.set(o.CodeActionKind.Empty, n.CodeActionKind.Empty), G.set(o.CodeActionKind.QuickFix, n.CodeActionKind.QuickFix), G.set(o.CodeActionKind.Refactor, n.CodeActionKind.Refactor), G.set(o.CodeActionKind.RefactorExtract, n.CodeActionKind.RefactorExtract), G.set(o.CodeActionKind.RefactorInline, n.CodeActionKind.RefactorInline), G.set(o.CodeActionKind.RefactorRewrite, n.CodeActionKind.RefactorRewrite), G.set(o.CodeActionKind.Source, n.CodeActionKind.Source), G.set(o.CodeActionKind.SourceOrganizeImports, n.CodeActionKind.SourceOrganizeImports), {
                    asUri: C,
                    asDocumentSelector: function (e) {
                        const t = [];
                        for (const i of e) if ("string" == typeof i) t.push(i); else if (f.NotebookCellTextDocumentFilter.is(i)) if ("string" == typeof i.notebook) t.push({
                            notebookType: i.notebook,
                            language: i.language
                        }); else {
                            const e = i.notebook.notebookType ?? "*";
                            t.push({
                                notebookType: e,
                                scheme: i.notebook.scheme,
                                pattern: i.notebook.pattern,
                                language: i.language
                            })
                        } else f.TextDocumentFilter.is(i) && t.push({
                            language: i.language,
                            scheme: i.scheme,
                            pattern: i.pattern
                        });
                        return t
                    },
                    asDiagnostics: async function (e, t) {
                        return s.map(e, w, t)
                    },
                    asDiagnostic: w,
                    asRange: S,
                    asRanges: D,
                    asPosition: k,
                    asDiagnosticSeverity: P,
                    asDiagnosticTag: b,
                    asHover: function (e) {
                        if (e) return new n.Hover(function (e) {
                            if (r.string(e)) return R(e);
                            if (v.is(e)) return R().appendCodeblock(e.value, e.language);
                            if (Array.isArray(e)) {
                                let t = [];
                                for (let i of e) {
                                    let e = R();
                                    v.is(i) ? e.appendCodeblock(i.value, i.language) : e.appendMarkdown(i), t.push(e)
                                }
                                return t
                            }
                            return R(e)
                        }(e.contents), S(e.range))
                    },
                    asCompletionResult: async function (e, t, i) {
                        if (!e) return;
                        if (Array.isArray(e)) return s.map(e, (e => x(e, t)), i);
                        const r = e, {defaultRange: a, commitCharacters: c} = function (e, t) {
                                const i = e.itemDefaults?.editRange, n = e.itemDefaults?.commitCharacters ?? t;
                                return o.Range.is(i) ? {
                                    defaultRange: S(i),
                                    commitCharacters: n
                                } : void 0 !== i ? {
                                    defaultRange: {inserting: S(i.insert), replacing: S(i.replace)},
                                    commitCharacters: n
                                } : {defaultRange: void 0, commitCharacters: n}
                            }(r, t),
                            l = await s.map(r.items, (e => x(e, c, a, r.itemDefaults?.insertTextMode, r.itemDefaults?.insertTextFormat, r.itemDefaults?.data)), i);
                        return new n.CompletionList(l, r.isIncomplete)
                    },
                    asCompletionItem: x,
                    asTextEdit: E,
                    asTextEdits: O,
                    asSignatureHelp: async function (e, t) {
                        if (!e) return;
                        let i = new n.SignatureHelp;
                        return r.number(e.activeSignature) ? i.activeSignature = e.activeSignature : i.activeSignature = 0, r.number(e.activeParameter) ? i.activeParameter = e.activeParameter : i.activeParameter = 0, e.signatures && (i.signatures = await M(e.signatures, t)), i
                    },
                    asSignatureInformations: M,
                    asSignatureInformation: I,
                    asParameterInformations: F,
                    asParameterInformation: N,
                    asDeclarationResult: async function (e, t) {
                        if (e) return A(e, t)
                    },
                    asDefinitionResult: async function (e, t) {
                        if (e) return A(e, t)
                    },
                    asLocation: q,
                    asReferences: async function (e, t) {
                        if (e) return s.map(e, q, t)
                    },
                    asDocumentHighlights: async function (e, t) {
                        if (e) return s.map(e, $, t)
                    },
                    asDocumentHighlight: $,
                    asDocumentHighlightKind: U,
                    asSymbolKind: H,
                    asSymbolTag: W,
                    asSymbolTags: V,
                    asSymbolInformations: async function (e, t) {
                        if (e) return s.map(e, K, t)
                    },
                    asSymbolInformation: K,
                    asDocumentSymbols: async function (e, t) {
                        if (null != e) return s.map(e, B, t)
                    },
                    asDocumentSymbol: B,
                    asCommand: z,
                    asCommands: async function (e, t) {
                        if (e) return s.map(e, z, t)
                    },
                    asCodeAction: Y,
                    asCodeActionKind: X,
                    asCodeActionKinds: function (e) {
                        if (null != e) return e.map((e => X(e)))
                    },
                    asCodeActionResult: function (e, t) {
                        return s.mapAsync(e, (async e => o.Command.is(e) ? z(e) : Y(e, t)), t)
                    },
                    asCodeLens: Q,
                    asCodeLenses: async function (e, t) {
                        if (e) return s.map(e, Q, t)
                    },
                    asWorkspaceEdit: Z,
                    asDocumentLink: ee,
                    asDocumentLinks: async function (e, t) {
                        if (e) return s.map(e, ee, t)
                    },
                    asFoldingRangeKind: oe,
                    asFoldingRange: re,
                    asFoldingRanges: async function (e, t) {
                        if (e) return s.map(e, re, t)
                    },
                    asColor: te,
                    asColorInformation: ie,
                    asColorInformations: async function (e, t) {
                        if (e) return s.map(e, ie, t)
                    },
                    asColorPresentation: ne,
                    asColorPresentations: async function (e, t) {
                        if (e) return s.map(e, ne, t)
                    },
                    asSelectionRange: se,
                    asSelectionRanges: async function (e, t) {
                        return Array.isArray(e) ? s.map(e, se, t) : []
                    },
                    asInlineValue: ae,
                    asInlineValues: async function (e, t) {
                        return Array.isArray(e) ? s.map(e, ae, t) : []
                    },
                    asInlayHint: ce,
                    asInlayHints: async function (e, t) {
                        if (Array.isArray(e)) return s.mapAsync(e, ce, t)
                    },
                    asSemanticTokensLegend: function (e) {
                        return e
                    },
                    asSemanticTokens: async function (e, t) {
                        if (null != e) return new n.SemanticTokens(new Uint32Array(e.data), e.resultId)
                    },
                    asSemanticTokensEdit: ge,
                    asSemanticTokensEdits: async function (e, t) {
                        if (null != e) return new n.SemanticTokensEdits(e.edits.map(ge), e.resultId)
                    },
                    asCallHierarchyItem: de,
                    asCallHierarchyItems: async function (e, t) {
                        if (null !== e) return s.map(e, de, t)
                    },
                    asCallHierarchyIncomingCall: pe,
                    asCallHierarchyIncomingCalls: async function (e, t) {
                        if (null !== e) return s.mapAsync(e, pe, t)
                    },
                    asCallHierarchyOutgoingCall: he,
                    asCallHierarchyOutgoingCalls: async function (e, t) {
                        if (null !== e) return s.mapAsync(e, he, t)
                    },
                    asLinkedEditingRanges: async function (e, t) {
                        if (null != e) return new n.LinkedEditingRanges(await D(e.ranges, t), function (e) {
                            if (null == e) return;
                            return new RegExp(e)
                        }(e.wordPattern))
                    },
                    asTypeHierarchyItem: me,
                    asTypeHierarchyItems: async function (e, t) {
                        if (null !== e) return s.map(e, me, t)
                    },
                    asGlobPattern: function (e) {
                        if (r.string(e)) return e;
                        if (o.RelativePattern.is(e)) {
                            if (o.URI.is(e.baseUri)) return new n.RelativePattern(C(e.baseUri), e.pattern);
                            if (o.WorkspaceFolder.is(e.baseUri)) {
                                const t = n.workspace.getWorkspaceFolder(C(e.baseUri.uri));
                                return void 0 !== t ? new n.RelativePattern(t, e.pattern) : void 0
                            }
                        }
                    }
                }
            }
        }, 3055: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ProtocolDiagnostic = t.DiagnosticCode = void 0;
            const n = i(9496), o = i(2592);
            !function (e) {
                e.is = function (e) {
                    const t = e;
                    return null != t && (o.number(t.value) || o.string(t.value)) && o.string(t.target)
                }
            }(t.DiagnosticCode || (t.DiagnosticCode = {}));

            class r extends n.Diagnostic {
                constructor(e, t, i, n) {
                    super(e, t, i), this.data = n, this.hasDiagnosticCode = !1
                }
            }

            t.ProtocolDiagnostic = r
        }, 8039: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0});
            const n = i(9496);

            class o extends n.DocumentLink {
                constructor(e, t) {
                    super(e, t)
                }
            }

            t.default = o
        }, 6598: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0});
            const n = i(9496);

            class o extends n.InlayHint {
                constructor(e, t, i) {
                    super(e, t, i)
                }
            }

            t.default = o
        }, 696: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0});
            const n = i(9496);

            class o extends n.TypeHierarchyItem {
                constructor(e, t, i, n, o, r, s) {
                    super(e, t, i, n, o, r), void 0 !== s && (this.data = s)
                }
            }

            t.default = o
        }, 3572: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0});
            const n = i(9496);

            class o extends n.SymbolInformation {
                constructor(e, t, i, o, r) {
                    const s = !(o instanceof n.Uri);
                    super(e, t, i, s ? o : new n.Location(o, new n.Range(0, 0, 0, 0))), this.hasRange = s, void 0 !== r && (this.data = r)
                }
            }

            t.default = o
        }, 7101: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ReferencesFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151), s = i(2678);

            class a extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.ReferencesRequest.type)
                }

                fillClientCapabilities(e) {
                    (0, r.ensure)((0, r.ensure)(e, "textDocument"), "references").dynamicRegistration = !0
                }

                initialize(e, t) {
                    const i = this.getRegistrationOptions(t, e.referencesProvider);
                    i && this.register({id: s.generateUuid(), registerOptions: i})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideReferences: (e, t, i, n) => {
                            const r = this._client,
                                s = (e, t, i, n) => r.sendRequest(o.ReferencesRequest.type, r.code2ProtocolConverter.asReferenceParams(e, t, i), n).then((e => n.isCancellationRequested ? null : r.protocol2CodeConverter.asReferences(e, n)), (e => r.handleFailedRequest(o.ReferencesRequest.type, n, e, null))),
                                a = r.middleware;
                            return a.provideReferences ? a.provideReferences(e, t, i, n, s) : s(e, t, i, n)
                        }
                    };
                    return [this.registerProvider(t, i), i]
                }

                registerProvider(e, t) {
                    return n.languages.registerReferenceProvider(this._client.protocol2CodeConverter.asDocumentSelector(e), t)
                }
            }

            t.ReferencesFeature = a
        }, 1996: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.RenameFeature = void 0;
            const n = i(9496), o = i(3455), r = i(2678), s = i(2592), a = i(5151);

            class c extends a.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.RenameRequest.type)
                }

                fillClientCapabilities(e) {
                    let t = (0, a.ensure)((0, a.ensure)(e, "textDocument"), "rename");
                    t.dynamicRegistration = !0, t.prepareSupport = !0, t.prepareSupportDefaultBehavior = o.PrepareSupportDefaultBehavior.Identifier, t.honorsChangeAnnotations = !0
                }

                initialize(e, t) {
                    const i = this.getRegistrationOptions(t, e.renameProvider);
                    i && (s.boolean(e.renameProvider) && (i.prepareProvider = !1), this.register({
                        id: r.generateUuid(),
                        registerOptions: i
                    }))
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideRenameEdits: (e, t, i, n) => {
                            const r = this._client, s = (e, t, i, n) => {
                                let s = {
                                    textDocument: r.code2ProtocolConverter.asTextDocumentIdentifier(e),
                                    position: r.code2ProtocolConverter.asPosition(t),
                                    newName: i
                                };
                                return r.sendRequest(o.RenameRequest.type, s, n).then((e => n.isCancellationRequested ? null : r.protocol2CodeConverter.asWorkspaceEdit(e, n)), (e => r.handleFailedRequest(o.RenameRequest.type, n, e, null, !1)))
                            }, a = r.middleware;
                            return a.provideRenameEdits ? a.provideRenameEdits(e, t, i, n, s) : s(e, t, i, n)
                        }, prepareRename: e.prepareProvider ? (e, t, i) => {
                            const n = this._client, r = (e, t, i) => {
                                let r = {
                                    textDocument: n.code2ProtocolConverter.asTextDocumentIdentifier(e),
                                    position: n.code2ProtocolConverter.asPosition(t)
                                };
                                return n.sendRequest(o.PrepareRenameRequest.type, r, i).then((e => i.isCancellationRequested ? null : o.Range.is(e) ? n.protocol2CodeConverter.asRange(e) : this.isDefaultBehavior(e) ? !0 === e.defaultBehavior ? null : Promise.reject(new Error("The element can't be renamed.")) : e && o.Range.is(e.range) ? {
                                    range: n.protocol2CodeConverter.asRange(e.range),
                                    placeholder: e.placeholder
                                } : Promise.reject(new Error("The element can't be renamed."))), (e => {
                                    throw "string" == typeof e.message ? new Error(e.message) : new Error("The element can't be renamed.")
                                }))
                            }, s = n.middleware;
                            return s.prepareRename ? s.prepareRename(e, t, i, r) : r(e, t, i)
                        } : void 0
                    };
                    return [this.registerProvider(t, i), i]
                }

                registerProvider(e, t) {
                    return n.languages.registerRenameProvider(this._client.protocol2CodeConverter.asDocumentSelector(e), t)
                }

                isDefaultBehavior(e) {
                    const t = e;
                    return t && s.boolean(t.defaultBehavior)
                }
            }

            t.RenameFeature = c
        }, 3341: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.SelectionRangeFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151);

            class s extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.SelectionRangeRequest.type)
                }

                fillClientCapabilities(e) {
                    (0, r.ensure)((0, r.ensure)(e, "textDocument"), "selectionRange").dynamicRegistration = !0
                }

                initialize(e, t) {
                    const [i, n] = this.getRegistration(t, e.selectionRangeProvider);
                    i && n && this.register({id: i, registerOptions: n})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideSelectionRanges: (e, t, i) => {
                            const n = this._client, r = async (e, t, i) => {
                                const r = {
                                    textDocument: n.code2ProtocolConverter.asTextDocumentIdentifier(e),
                                    positions: await n.code2ProtocolConverter.asPositions(t, i)
                                };
                                return n.sendRequest(o.SelectionRangeRequest.type, r, i).then((e => i.isCancellationRequested ? null : n.protocol2CodeConverter.asSelectionRanges(e, i)), (e => n.handleFailedRequest(o.SelectionRangeRequest.type, i, e, null)))
                            }, s = n.middleware;
                            return s.provideSelectionRanges ? s.provideSelectionRanges(e, t, i, r) : r(e, t, i)
                        }
                    };
                    return [this.registerProvider(t, i), i]
                }

                registerProvider(e, t) {
                    return n.languages.registerSelectionRangeProvider(this._client.protocol2CodeConverter.asDocumentSelector(e), t)
                }
            }

            t.SelectionRangeFeature = s
        }, 6049: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.SemanticTokensFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151), s = i(2592);

            class a extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.SemanticTokensRegistrationType.type)
                }

                fillClientCapabilities(e) {
                    const t = (0, r.ensure)((0, r.ensure)(e, "textDocument"), "semanticTokens");
                    t.dynamicRegistration = !0, t.tokenTypes = [o.SemanticTokenTypes.namespace, o.SemanticTokenTypes.type, o.SemanticTokenTypes.class, o.SemanticTokenTypes.enum, o.SemanticTokenTypes.interface, o.SemanticTokenTypes.struct, o.SemanticTokenTypes.typeParameter, o.SemanticTokenTypes.parameter, o.SemanticTokenTypes.variable, o.SemanticTokenTypes.property, o.SemanticTokenTypes.enumMember, o.SemanticTokenTypes.event, o.SemanticTokenTypes.function, o.SemanticTokenTypes.method, o.SemanticTokenTypes.macro, o.SemanticTokenTypes.keyword, o.SemanticTokenTypes.modifier, o.SemanticTokenTypes.comment, o.SemanticTokenTypes.string, o.SemanticTokenTypes.number, o.SemanticTokenTypes.regexp, o.SemanticTokenTypes.operator, o.SemanticTokenTypes.decorator], t.tokenModifiers = [o.SemanticTokenModifiers.declaration, o.SemanticTokenModifiers.definition, o.SemanticTokenModifiers.readonly, o.SemanticTokenModifiers.static, o.SemanticTokenModifiers.deprecated, o.SemanticTokenModifiers.abstract, o.SemanticTokenModifiers.async, o.SemanticTokenModifiers.modification, o.SemanticTokenModifiers.documentation, o.SemanticTokenModifiers.defaultLibrary], t.formats = [o.TokenFormat.Relative], t.requests = {
                        range: !0,
                        full: {delta: !0}
                    }, t.multilineTokenSupport = !1, t.overlappingTokenSupport = !1, t.serverCancelSupport = !0, t.augmentsSyntaxTokens = !0, (0, r.ensure)((0, r.ensure)(e, "workspace"), "semanticTokens").refreshSupport = !0
                }

                initialize(e, t) {
                    this._client.onRequest(o.SemanticTokensRefreshRequest.type, (async () => {
                        for (const e of this.getAllProviders()) e.onDidChangeSemanticTokensEmitter.fire()
                    }));
                    const [i, n] = this.getRegistration(t, e.semanticTokensProvider);
                    i && n && this.register({id: i, registerOptions: n})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = s.boolean(e.full) ? e.full : void 0 !== e.full,
                        r = void 0 !== e.full && "boolean" != typeof e.full && !0 === e.full.delta,
                        a = new n.EventEmitter, c = i ? {
                            onDidChangeSemanticTokens: a.event, provideDocumentSemanticTokens: (e, t) => {
                                const i = this._client, n = i.middleware, r = (e, t) => {
                                    const n = {textDocument: i.code2ProtocolConverter.asTextDocumentIdentifier(e)};
                                    return i.sendRequest(o.SemanticTokensRequest.type, n, t).then((e => t.isCancellationRequested ? null : i.protocol2CodeConverter.asSemanticTokens(e, t)), (e => i.handleFailedRequest(o.SemanticTokensRequest.type, t, e, null)))
                                };
                                return n.provideDocumentSemanticTokens ? n.provideDocumentSemanticTokens(e, t, r) : r(e, t)
                            }, provideDocumentSemanticTokensEdits: r ? (e, t, i) => {
                                const n = this._client, r = n.middleware, s = (e, t, i) => {
                                    const r = {
                                        textDocument: n.code2ProtocolConverter.asTextDocumentIdentifier(e),
                                        previousResultId: t
                                    };
                                    return n.sendRequest(o.SemanticTokensDeltaRequest.type, r, i).then((async e => i.isCancellationRequested ? null : o.SemanticTokens.is(e) ? await n.protocol2CodeConverter.asSemanticTokens(e, i) : await n.protocol2CodeConverter.asSemanticTokensEdits(e, i)), (e => n.handleFailedRequest(o.SemanticTokensDeltaRequest.type, i, e, null)))
                                };
                                return r.provideDocumentSemanticTokensEdits ? r.provideDocumentSemanticTokensEdits(e, t, i, s) : s(e, t, i)
                            } : void 0
                        } : void 0, l = !0 === e.range ? {
                            provideDocumentRangeSemanticTokens: (e, t, i) => {
                                const n = this._client, r = n.middleware, s = (e, t, i) => {
                                    const r = {
                                        textDocument: n.code2ProtocolConverter.asTextDocumentIdentifier(e),
                                        range: n.code2ProtocolConverter.asRange(t)
                                    };
                                    return n.sendRequest(o.SemanticTokensRangeRequest.type, r, i).then((e => i.isCancellationRequested ? null : n.protocol2CodeConverter.asSemanticTokens(e, i)), (e => n.handleFailedRequest(o.SemanticTokensRangeRequest.type, i, e, null)))
                                };
                                return r.provideDocumentRangeSemanticTokens ? r.provideDocumentRangeSemanticTokens(e, t, i, s) : s(e, t, i)
                            }
                        } : void 0, u = [], d = this._client, p = d.protocol2CodeConverter.asSemanticTokensLegend(e.legend),
                        h = d.protocol2CodeConverter.asDocumentSelector(t);
                    return void 0 !== c && u.push(n.languages.registerDocumentSemanticTokensProvider(h, c, p)), void 0 !== l && u.push(n.languages.registerDocumentRangeSemanticTokensProvider(h, l, p)), [new n.Disposable((() => u.forEach((e => e.dispose())))), {
                        range: l,
                        full: c,
                        onDidChangeSemanticTokensEmitter: a
                    }]
                }
            }

            t.SemanticTokensFeature = a
        }, 9779: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.SignatureHelpFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151), s = i(2678);

            class a extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.SignatureHelpRequest.type)
                }

                fillClientCapabilities(e) {
                    let t = (0, r.ensure)((0, r.ensure)(e, "textDocument"), "signatureHelp");
                    t.dynamicRegistration = !0, t.signatureInformation = {documentationFormat: [o.MarkupKind.Markdown, o.MarkupKind.PlainText]}, t.signatureInformation.parameterInformation = {labelOffsetSupport: !0}, t.signatureInformation.activeParameterSupport = !0, t.contextSupport = !0
                }

                initialize(e, t) {
                    const i = this.getRegistrationOptions(t, e.signatureHelpProvider);
                    i && this.register({id: s.generateUuid(), registerOptions: i})
                }

                registerLanguageProvider(e) {
                    const t = {
                        provideSignatureHelp: (e, t, i, n) => {
                            const r = this._client,
                                s = (e, t, i, n) => r.sendRequest(o.SignatureHelpRequest.type, r.code2ProtocolConverter.asSignatureHelpParams(e, t, i), n).then((e => n.isCancellationRequested ? null : r.protocol2CodeConverter.asSignatureHelp(e, n)), (e => r.handleFailedRequest(o.SignatureHelpRequest.type, n, e, null))),
                                a = r.middleware;
                            return a.provideSignatureHelp ? a.provideSignatureHelp(e, t, n, i, s) : s(e, t, n, i)
                        }
                    };
                    return [this.registerProvider(e, t), t]
                }

                registerProvider(e, t) {
                    const i = this._client.protocol2CodeConverter.asDocumentSelector(e.documentSelector);
                    if (void 0 === e.retriggerCharacters) {
                        const o = e.triggerCharacters || [];
                        return n.languages.registerSignatureHelpProvider(i, t, ...o)
                    }
                    {
                        const o = {
                            triggerCharacters: e.triggerCharacters || [],
                            retriggerCharacters: e.retriggerCharacters || []
                        };
                        return n.languages.registerSignatureHelpProvider(i, t, o)
                    }
                }
            }

            t.SignatureHelpFeature = a
        }, 7686: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DidSaveTextDocumentFeature = t.WillSaveWaitUntilFeature = t.WillSaveFeature = t.DidChangeTextDocumentFeature = t.DidCloseTextDocumentFeature = t.DidOpenTextDocumentFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151), s = i(2570), a = i(2678);

            class c extends r.TextDocumentEventFeature {
                constructor(e, t) {
                    super(e, n.workspace.onDidOpenTextDocument, o.DidOpenTextDocumentNotification.type, (() => e.middleware.didOpen), (t => e.code2ProtocolConverter.asOpenTextDocumentParams(t)), (e => e), r.TextDocumentEventFeature.textDocumentFilter), this._syncedDocuments = t
                }

                get openDocuments() {
                    return this._syncedDocuments.values()
                }

                fillClientCapabilities(e) {
                    (0, r.ensure)((0, r.ensure)(e, "textDocument"), "synchronization").dynamicRegistration = !0
                }

                initialize(e, t) {
                    const i = e.resolvedTextDocumentSync;
                    t && i && i.openClose && this.register({
                        id: a.generateUuid(),
                        registerOptions: {documentSelector: t}
                    })
                }

                get registrationType() {
                    return o.DidOpenTextDocumentNotification.type
                }

                register(e) {
                    if (super.register(e), !e.registerOptions.documentSelector) return;
                    const t = this._client.protocol2CodeConverter.asDocumentSelector(e.registerOptions.documentSelector);
                    n.workspace.textDocuments.forEach((e => {
                        const i = e.uri.toString();
                        if (!this._syncedDocuments.has(i) && n.languages.match(t, e) > 0 && !this._client.hasDedicatedTextSynchronizationFeature(e)) {
                            const t = this._client.middleware,
                                n = e => this._client.sendNotification(this._type, this._createParams(e));
                            (t.didOpen ? t.didOpen(e, n) : n(e)).catch((e => {
                                this._client.error(`Sending document notification ${this._type.method} failed`, e)
                            })), this._syncedDocuments.set(i, e)
                        }
                    }))
                }

                notificationSent(e, t, i) {
                    super.notificationSent(e, t, i), this._syncedDocuments.set(e.uri.toString(), e)
                }
            }

            t.DidOpenTextDocumentFeature = c;

            class l extends r.TextDocumentEventFeature {
                constructor(e, t) {
                    super(e, n.workspace.onDidCloseTextDocument, o.DidCloseTextDocumentNotification.type, (() => e.middleware.didClose), (t => e.code2ProtocolConverter.asCloseTextDocumentParams(t)), (e => e), r.TextDocumentEventFeature.textDocumentFilter), this._syncedDocuments = t
                }

                get registrationType() {
                    return o.DidCloseTextDocumentNotification.type
                }

                fillClientCapabilities(e) {
                    (0, r.ensure)((0, r.ensure)(e, "textDocument"), "synchronization").dynamicRegistration = !0
                }

                initialize(e, t) {
                    let i = e.resolvedTextDocumentSync;
                    t && i && i.openClose && this.register({
                        id: a.generateUuid(),
                        registerOptions: {documentSelector: t}
                    })
                }

                notificationSent(e, t, i) {
                    super.notificationSent(e, t, i), this._syncedDocuments.delete(e.uri.toString())
                }

                unregister(e) {
                    const t = this._selectors.get(e);
                    super.unregister(e);
                    const i = this._selectors.values();
                    this._syncedDocuments.forEach((e => {
                        if (n.languages.match(t, e) > 0 && !this._selectorFilter(i, e) && !this._client.hasDedicatedTextSynchronizationFeature(e)) {
                            let t = this._client.middleware,
                                i = e => this._client.sendNotification(this._type, this._createParams(e));
                            this._syncedDocuments.delete(e.uri.toString()), (t.didClose ? t.didClose(e, i) : i(e)).catch((e => {
                                this._client.error(`Sending document notification ${this._type.method} failed`, e)
                            }))
                        }
                    }))
                }
            }

            t.DidCloseTextDocumentFeature = l;

            class u extends r.DynamicDocumentFeature {
                constructor(e) {
                    super(e), this._forcingDelivery = !1, this._changeData = new Map, this._onNotificationSent = new n.EventEmitter
                }

                get registrationType() {
                    return o.DidChangeTextDocumentNotification.type
                }

                fillClientCapabilities(e) {
                    (0, r.ensure)((0, r.ensure)(e, "textDocument"), "synchronization").dynamicRegistration = !0
                }

                initialize(e, t) {
                    let i = e.resolvedTextDocumentSync;
                    t && i && void 0 !== i.change && i.change !== o.TextDocumentSyncKind.None && this.register({
                        id: a.generateUuid(),
                        registerOptions: Object.assign({}, {documentSelector: t}, {syncKind: i.change})
                    })
                }

                register(e) {
                    e.registerOptions.documentSelector && (this._listener || (this._listener = n.workspace.onDidChangeTextDocument(this.callback, this)), this._changeData.set(e.id, {
                        syncKind: e.registerOptions.syncKind,
                        documentSelector: this._client.protocol2CodeConverter.asDocumentSelector(e.registerOptions.documentSelector)
                    }))
                }

                * getDocumentSelectors() {
                    for (const e of this._changeData.values()) yield e.documentSelector
                }

                async callback(e) {
                    if (0 === e.contentChanges.length) return;
                    const t = [];
                    for (const i of this._changeData.values()) if (n.languages.match(i.documentSelector, e.document) > 0 && !this._client.hasDedicatedTextSynchronizationFeature(e.document)) {
                        const n = this._client.middleware;
                        if (i.syncKind === o.TextDocumentSyncKind.Incremental) {
                            const i = async e => {
                                const t = this._client.code2ProtocolConverter.asChangeTextDocumentParams(e);
                                await this._client.sendNotification(o.DidChangeTextDocumentNotification.type, t), this.notificationSent(e, o.DidChangeTextDocumentNotification.type, t)
                            };
                            t.push(n.didChange ? n.didChange(e, (e => i(e))) : i(e))
                        } else if (i.syncKind === o.TextDocumentSyncKind.Full) {
                            const i = async e => {
                                const t = async e => {
                                    const t = this._client.code2ProtocolConverter.asChangeTextDocumentParams(e.document);
                                    await this._client.sendNotification(o.DidChangeTextDocumentNotification.type, t), this.notificationSent(e, o.DidChangeTextDocumentNotification.type, t)
                                };
                                return this._changeDelayer ? (this._changeDelayer.uri !== e.document.uri.toString() && (await this.forceDelivery(), this._changeDelayer.uri = e.document.uri.toString()), this._changeDelayer.delayer.trigger((() => t(e)))) : (this._changeDelayer = {
                                    uri: e.document.uri.toString(),
                                    delayer: new s.Delayer(200)
                                }, this._changeDelayer.delayer.trigger((() => t(e)), -1))
                            };
                            t.push(n.didChange ? n.didChange(e, (e => i(e))) : i(e))
                        }
                    }
                    return Promise.all(t).then(void 0, (e => {
                        throw this._client.error(`Sending document notification ${o.DidChangeTextDocumentNotification.type.method} failed`, e), e
                    }))
                }

                get onNotificationSent() {
                    return this._onNotificationSent.event
                }

                notificationSent(e, t, i) {
                    this._onNotificationSent.fire({original: e, type: t, params: i})
                }

                unregister(e) {
                    this._changeData.delete(e), 0 === this._changeData.size && this._listener && (this._listener.dispose(), this._listener = void 0)
                }

                dispose() {
                    void 0 !== this._changeDelayer && this._changeDelayer.delayer.cancel(), this._changeDelayer = void 0, this._forcingDelivery = !1, this._changeData.clear(), this._listener && (this._listener.dispose(), this._listener = void 0)
                }

                async forceDelivery() {
                    if (!this._forcingDelivery && this._changeDelayer) try {
                        return this._forcingDelivery = !0, this._changeDelayer.delayer.forceDelivery()
                    } finally {
                        this._forcingDelivery = !1
                    }
                }

                getProvider(e) {
                    for (const t of this._changeData.values()) if (n.languages.match(t.documentSelector, e) > 0) return {send: e => this.callback(e)}
                }
            }

            t.DidChangeTextDocumentFeature = u;

            class d extends r.TextDocumentEventFeature {
                constructor(e) {
                    super(e, n.workspace.onWillSaveTextDocument, o.WillSaveTextDocumentNotification.type, (() => e.middleware.willSave), (t => e.code2ProtocolConverter.asWillSaveTextDocumentParams(t)), (e => e.document), ((e, t) => r.TextDocumentEventFeature.textDocumentFilter(e, t.document)))
                }

                get registrationType() {
                    return o.WillSaveTextDocumentNotification.type
                }

                fillClientCapabilities(e) {
                    (0, r.ensure)((0, r.ensure)(e, "textDocument"), "synchronization").willSave = !0
                }

                initialize(e, t) {
                    let i = e.resolvedTextDocumentSync;
                    t && i && i.willSave && this.register({
                        id: a.generateUuid(),
                        registerOptions: {documentSelector: t}
                    })
                }
            }

            t.WillSaveFeature = d;

            class p extends r.DynamicDocumentFeature {
                constructor(e) {
                    super(e), this._selectors = new Map
                }

                getDocumentSelectors() {
                    return this._selectors.values()
                }

                get registrationType() {
                    return o.WillSaveTextDocumentWaitUntilRequest.type
                }

                fillClientCapabilities(e) {
                    (0, r.ensure)((0, r.ensure)(e, "textDocument"), "synchronization").willSaveWaitUntil = !0
                }

                initialize(e, t) {
                    let i = e.resolvedTextDocumentSync;
                    t && i && i.willSaveWaitUntil && this.register({
                        id: a.generateUuid(),
                        registerOptions: {documentSelector: t}
                    })
                }

                register(e) {
                    e.registerOptions.documentSelector && (this._listener || (this._listener = n.workspace.onWillSaveTextDocument(this.callback, this)), this._selectors.set(e.id, this._client.protocol2CodeConverter.asDocumentSelector(e.registerOptions.documentSelector)))
                }

                callback(e) {
                    if (r.TextDocumentEventFeature.textDocumentFilter(this._selectors.values(), e.document) && !this._client.hasDedicatedTextSynchronizationFeature(e.document)) {
                        let t = this._client.middleware,
                            i = e => this._client.sendRequest(o.WillSaveTextDocumentWaitUntilRequest.type, this._client.code2ProtocolConverter.asWillSaveTextDocumentParams(e)).then((async e => {
                                let t = await this._client.protocol2CodeConverter.asTextEdits(e);
                                return void 0 === t ? [] : t
                            }));
                        e.waitUntil(t.willSaveWaitUntil ? t.willSaveWaitUntil(e, i) : i(e))
                    }
                }

                unregister(e) {
                    this._selectors.delete(e), 0 === this._selectors.size && this._listener && (this._listener.dispose(), this._listener = void 0)
                }

                dispose() {
                    this._selectors.clear(), this._listener && (this._listener.dispose(), this._listener = void 0)
                }
            }

            t.WillSaveWaitUntilFeature = p;

            class h extends r.TextDocumentEventFeature {
                constructor(e) {
                    super(e, n.workspace.onDidSaveTextDocument, o.DidSaveTextDocumentNotification.type, (() => e.middleware.didSave), (t => e.code2ProtocolConverter.asSaveTextDocumentParams(t, this._includeText)), (e => e), r.TextDocumentEventFeature.textDocumentFilter), this._includeText = !1
                }

                get registrationType() {
                    return o.DidSaveTextDocumentNotification.type
                }

                fillClientCapabilities(e) {
                    (0, r.ensure)((0, r.ensure)(e, "textDocument"), "synchronization").didSave = !0
                }

                initialize(e, t) {
                    const i = e.resolvedTextDocumentSync;
                    if (t && i && i.save) {
                        const e = "boolean" == typeof i.save ? {includeText: !1} : {includeText: !!i.save.includeText};
                        this.register({
                            id: a.generateUuid(),
                            registerOptions: Object.assign({}, {documentSelector: t}, e)
                        })
                    }
                }

                register(e) {
                    this._includeText = !!e.registerOptions.includeText, super.register(e)
                }
            }

            t.DidSaveTextDocumentFeature = h
        }, 6372: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.TypeDefinitionFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151);

            class s extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.TypeDefinitionRequest.type)
                }

                fillClientCapabilities(e) {
                    (0, r.ensure)((0, r.ensure)(e, "textDocument"), "typeDefinition").dynamicRegistration = !0;
                    let t = (0, r.ensure)((0, r.ensure)(e, "textDocument"), "typeDefinition");
                    t.dynamicRegistration = !0, t.linkSupport = !0
                }

                initialize(e, t) {
                    let [i, n] = this.getRegistration(t, e.typeDefinitionProvider);
                    i && n && this.register({id: i, registerOptions: n})
                }

                registerLanguageProvider(e) {
                    const t = e.documentSelector, i = {
                        provideTypeDefinition: (e, t, i) => {
                            const n = this._client,
                                r = (e, t, i) => n.sendRequest(o.TypeDefinitionRequest.type, n.code2ProtocolConverter.asTextDocumentPositionParams(e, t), i).then((e => i.isCancellationRequested ? null : n.protocol2CodeConverter.asDefinitionResult(e, i)), (e => n.handleFailedRequest(o.TypeDefinitionRequest.type, i, e, null))),
                                s = n.middleware;
                            return s.provideTypeDefinition ? s.provideTypeDefinition(e, t, i, r) : r(e, t, i)
                        }
                    };
                    return [this.registerProvider(t, i), i]
                }

                registerProvider(e, t) {
                    return n.languages.registerTypeDefinitionProvider(this._client.protocol2CodeConverter.asDocumentSelector(e), t)
                }
            }

            t.TypeDefinitionFeature = s
        }, 6769: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.TypeHierarchyFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151);

            class s {
                constructor(e) {
                    this.client = e, this.middleware = e.middleware
                }

                prepareTypeHierarchy(e, t, i) {
                    const n = this.client, r = this.middleware, s = (e, t, i) => {
                        const r = n.code2ProtocolConverter.asTextDocumentPositionParams(e, t);
                        return n.sendRequest(o.TypeHierarchyPrepareRequest.type, r, i).then((e => i.isCancellationRequested ? null : n.protocol2CodeConverter.asTypeHierarchyItems(e, i)), (e => n.handleFailedRequest(o.TypeHierarchyPrepareRequest.type, i, e, null)))
                    };
                    return r.prepareTypeHierarchy ? r.prepareTypeHierarchy(e, t, i, s) : s(e, t, i)
                }

                provideTypeHierarchySupertypes(e, t) {
                    const i = this.client, n = this.middleware, r = (e, t) => {
                        const n = {item: i.code2ProtocolConverter.asTypeHierarchyItem(e)};
                        return i.sendRequest(o.TypeHierarchySupertypesRequest.type, n, t).then((e => t.isCancellationRequested ? null : i.protocol2CodeConverter.asTypeHierarchyItems(e, t)), (e => i.handleFailedRequest(o.TypeHierarchySupertypesRequest.type, t, e, null)))
                    };
                    return n.provideTypeHierarchySupertypes ? n.provideTypeHierarchySupertypes(e, t, r) : r(e, t)
                }

                provideTypeHierarchySubtypes(e, t) {
                    const i = this.client, n = this.middleware, r = (e, t) => {
                        const n = {item: i.code2ProtocolConverter.asTypeHierarchyItem(e)};
                        return i.sendRequest(o.TypeHierarchySubtypesRequest.type, n, t).then((e => t.isCancellationRequested ? null : i.protocol2CodeConverter.asTypeHierarchyItems(e, t)), (e => i.handleFailedRequest(o.TypeHierarchySubtypesRequest.type, t, e, null)))
                    };
                    return n.provideTypeHierarchySubtypes ? n.provideTypeHierarchySubtypes(e, t, r) : r(e, t)
                }
            }

            class a extends r.TextDocumentLanguageFeature {
                constructor(e) {
                    super(e, o.TypeHierarchyPrepareRequest.type)
                }

                fillClientCapabilities(e) {
                    (0, r.ensure)((0, r.ensure)(e, "textDocument"), "typeHierarchy").dynamicRegistration = !0
                }

                initialize(e, t) {
                    const [i, n] = this.getRegistration(t, e.typeHierarchyProvider);
                    i && n && this.register({id: i, registerOptions: n})
                }

                registerLanguageProvider(e) {
                    const t = this._client, i = new s(t);
                    return [n.languages.registerTypeHierarchyProvider(t.protocol2CodeConverter.asDocumentSelector(e.documentSelector), i), i]
                }
            }

            t.TypeHierarchyFeature = a
        }, 2570: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.forEach = t.mapAsync = t.map = t.clearTestMode = t.setTestMode = t.Semaphore = t.Delayer = void 0;
            const n = i(3455);
            t.Delayer = class {
                constructor(e) {
                    this.defaultDelay = e, this.timeout = void 0, this.completionPromise = void 0, this.onSuccess = void 0, this.task = void 0
                }

                trigger(e, t = this.defaultDelay) {
                    return this.task = e, t >= 0 && this.cancelTimeout(), this.completionPromise || (this.completionPromise = new Promise((e => {
                        this.onSuccess = e
                    })).then((() => {
                        this.completionPromise = void 0, this.onSuccess = void 0;
                        var e = this.task();
                        return this.task = void 0, e
                    }))), (t >= 0 || void 0 === this.timeout) && (this.timeout = (0, n.RAL)().timer.setTimeout((() => {
                        this.timeout = void 0, this.onSuccess(void 0)
                    }), t >= 0 ? t : this.defaultDelay)), this.completionPromise
                }

                forceDelivery() {
                    if (!this.completionPromise) return;
                    this.cancelTimeout();
                    let e = this.task();
                    return this.completionPromise = void 0, this.onSuccess = void 0, this.task = void 0, e
                }

                isTriggered() {
                    return void 0 !== this.timeout
                }

                cancel() {
                    this.cancelTimeout(), this.completionPromise = void 0
                }

                cancelTimeout() {
                    void 0 !== this.timeout && (this.timeout.dispose(), this.timeout = void 0)
                }
            };
            t.Semaphore = class {
                constructor(e = 1) {
                    if (e <= 0) throw new Error("Capacity must be greater than 0");
                    this._capacity = e, this._active = 0, this._waiting = []
                }

                lock(e) {
                    return new Promise(((t, i) => {
                        this._waiting.push({thunk: e, resolve: t, reject: i}), this.runNext()
                    }))
                }

                get active() {
                    return this._active
                }

                runNext() {
                    0 !== this._waiting.length && this._active !== this._capacity && (0, n.RAL)().timer.setImmediate((() => this.doRunNext()))
                }

                doRunNext() {
                    if (0 === this._waiting.length || this._active === this._capacity) return;
                    const e = this._waiting.shift();
                    if (this._active++, this._active > this._capacity) throw new Error("To many thunks active");
                    try {
                        const t = e.thunk();
                        t instanceof Promise ? t.then((t => {
                            this._active--, e.resolve(t), this.runNext()
                        }), (t => {
                            this._active--, e.reject(t), this.runNext()
                        })) : (this._active--, e.resolve(t), this.runNext())
                    } catch (t) {
                        this._active--, e.reject(t), this.runNext()
                    }
                }
            };
            let o = !1;
            t.setTestMode = function () {
                o = !0
            }, t.clearTestMode = function () {
                o = !1
            };

            class r {
                constructor(e = 15) {
                    this.yieldAfter = !0 === o ? Math.max(e, 2) : Math.max(e, 15), this.startTime = Date.now(), this.counter = 0, this.total = 0, this.counterInterval = 1
                }

                start() {
                    this.counter = 0, this.total = 0, this.counterInterval = 1, this.startTime = Date.now()
                }

                shouldYield() {
                    if (++this.counter >= this.counterInterval) {
                        const e = Date.now() - this.startTime, t = Math.max(0, this.yieldAfter - e);
                        if (this.total += this.counter, this.counter = 0, e >= this.yieldAfter || t <= 1) return this.counterInterval = 1, this.total = 0, !0;
                        switch (e) {
                            case 0:
                            case 1:
                                this.counterInterval = 2 * this.total
                        }
                    }
                    return !1
                }
            }

            t.map = async function (e, t, i, o) {
                if (0 === e.length) return [];
                const s = new Array(e.length), a = new r(o?.yieldAfter);

                function c(i) {
                    a.start();
                    for (let n = i; n < e.length; n++) if (s[n] = t(e[n]), a.shouldYield()) return o?.yieldCallback && o.yieldCallback(), n + 1;
                    return -1
                }

                let l = c(0);
                for (; -1 !== l && (void 0 === i || !i.isCancellationRequested);) l = await new Promise((e => {
                    (0, n.RAL)().timer.setImmediate((() => {
                        e(c(l))
                    }))
                }));
                return s
            }, t.mapAsync = async function (e, t, i, o) {
                if (0 === e.length) return [];
                const s = new Array(e.length), a = new r(o?.yieldAfter);

                async function c(n) {
                    a.start();
                    for (let r = n; r < e.length; r++) if (s[r] = await t(e[r], i), a.shouldYield()) return o?.yieldCallback && o.yieldCallback(), r + 1;
                    return -1
                }

                let l = await c(0);
                for (; -1 !== l && (void 0 === i || !i.isCancellationRequested);) l = await new Promise((e => {
                    (0, n.RAL)().timer.setImmediate((() => {
                        e(c(l))
                    }))
                }));
                return s
            }, t.forEach = async function (e, t, i, o) {
                if (0 === e.length) return;
                const s = new r(o?.yieldAfter);

                function a(i) {
                    s.start();
                    for (let n = i; n < e.length; n++) if (t(e[n]), s.shouldYield()) return o?.yieldCallback && o.yieldCallback(), n + 1;
                    return -1
                }

                let c = a(0);
                for (; -1 !== c && (void 0 === i || !i.isCancellationRequested);) c = await new Promise((e => {
                    (0, n.RAL)().timer.setImmediate((() => {
                        e(a(c))
                    }))
                }))
            }
        }, 2592: (e, t) => {
            "use strict";

            function i(e) {
                return "string" == typeof e || e instanceof String
            }

            function n(e) {
                return "function" == typeof e
            }

            function o(e) {
                return Array.isArray(e)
            }

            function r(e) {
                return e && n(e.then)
            }

            Object.defineProperty(t, "__esModule", {value: !0}), t.asPromise = t.thenable = t.typedArray = t.stringArray = t.array = t.func = t.error = t.number = t.string = t.boolean = void 0, t.boolean = function (e) {
                return !0 === e || !1 === e
            }, t.string = i, t.number = function (e) {
                return "number" == typeof e || e instanceof Number
            }, t.error = function (e) {
                return e instanceof Error
            }, t.func = n, t.array = o, t.stringArray = function (e) {
                return o(e) && e.every((e => i(e)))
            }, t.typedArray = function (e, t) {
                return Array.isArray(e) && e.every(t)
            }, t.thenable = r, t.asPromise = function (e) {
                return e instanceof Promise ? e : r(e) ? new Promise(((t, i) => {
                    e.then((e => t(e)), (e => i(e)))
                })) : Promise.resolve(e)
            }
        }, 2678: (e, t) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.generateUuid = t.parse = t.isUUID = t.v4 = t.empty = void 0;

            class i {
                constructor(e) {
                    this._value = e
                }

                asHex() {
                    return this._value
                }

                equals(e) {
                    return this.asHex() === e.asHex()
                }
            }

            class n extends i {
                constructor() {
                    super([n._randomHex(), n._randomHex(), n._randomHex(), n._randomHex(), n._randomHex(), n._randomHex(), n._randomHex(), n._randomHex(), "-", n._randomHex(), n._randomHex(), n._randomHex(), n._randomHex(), "-", "4", n._randomHex(), n._randomHex(), n._randomHex(), "-", n._oneOf(n._timeHighBits), n._randomHex(), n._randomHex(), n._randomHex(), "-", n._randomHex(), n._randomHex(), n._randomHex(), n._randomHex(), n._randomHex(), n._randomHex(), n._randomHex(), n._randomHex(), n._randomHex(), n._randomHex(), n._randomHex(), n._randomHex()].join(""))
                }

                static _oneOf(e) {
                    return e[Math.floor(e.length * Math.random())]
                }

                static _randomHex() {
                    return n._oneOf(n._chars)
                }
            }

            function o() {
                return new n
            }

            n._chars = ["0", "1", "2", "3", "4", "5", "6", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"], n._timeHighBits = ["8", "9", "a", "b"], t.empty = new i("00000000-0000-0000-0000-000000000000"), t.v4 = o;
            const r = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

            function s(e) {
                return r.test(e)
            }

            t.isUUID = s, t.parse = function (e) {
                if (!s(e)) throw new Error("invalid uuid");
                return new i(e)
            }, t.generateUuid = function () {
                return o().asHex()
            }
        }, 6053: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.WorkspaceFoldersFeature = t.arrayDiff = void 0;
            const n = i(2678), o = i(9496), r = i(3455);

            function s(e, t) {
                if (void 0 !== e) return e[t]
            }

            function a(e, t) {
                return e.filter((e => t.indexOf(e) < 0))
            }

            t.arrayDiff = a;
            t.WorkspaceFoldersFeature = class {
                constructor(e) {
                    this._client = e, this._listeners = new Map
                }

                getState() {
                    return {
                        kind: "workspace",
                        id: this.registrationType.method,
                        registrations: this._listeners.size > 0
                    }
                }

                get registrationType() {
                    return r.DidChangeWorkspaceFoldersNotification.type
                }

                fillInitializeParams(e) {
                    const t = o.workspace.workspaceFolders;
                    this.initializeWithFolders(t), e.workspaceFolders = void 0 === t ? null : t.map((e => this.asProtocol(e)))
                }

                initializeWithFolders(e) {
                    this._initialFolders = e
                }

                fillClientCapabilities(e) {
                    e.workspace = e.workspace || {}, e.workspace.workspaceFolders = !0
                }

                initialize(e) {
                    const t = this._client;
                    t.onRequest(r.WorkspaceFoldersRequest.type, (e => {
                        const i = () => {
                            const e = o.workspace.workspaceFolders;
                            if (void 0 === e) return null;
                            return e.map((e => this.asProtocol(e)))
                        }, n = t.middleware.workspace;
                        return n && n.workspaceFolders ? n.workspaceFolders(e, i) : i()
                    }));
                    const i = s(s(s(e, "workspace"), "workspaceFolders"), "changeNotifications");
                    let a;
                    "string" == typeof i ? a = i : !0 === i && (a = n.generateUuid()), a && this.register({
                        id: a,
                        registerOptions: void 0
                    })
                }

                sendInitialEvent(e) {
                    let t;
                    if (this._initialFolders && e) {
                        const i = a(this._initialFolders, e), n = a(e, this._initialFolders);
                        (n.length > 0 || i.length > 0) && (t = this.doSendEvent(n, i))
                    } else this._initialFolders ? t = this.doSendEvent([], this._initialFolders) : e && (t = this.doSendEvent(e, []));
                    void 0 !== t && t.catch((e => {
                        this._client.error(`Sending notification ${r.DidChangeWorkspaceFoldersNotification.type.method} failed`, e)
                    }))
                }

                doSendEvent(e, t) {
                    let i = {
                        event: {
                            added: e.map((e => this.asProtocol(e))),
                            removed: t.map((e => this.asProtocol(e)))
                        }
                    };
                    return this._client.sendNotification(r.DidChangeWorkspaceFoldersNotification.type, i)
                }

                register(e) {
                    let t = e.id, i = this._client, n = o.workspace.onDidChangeWorkspaceFolders((e => {
                        let t = e => this.doSendEvent(e.added, e.removed), n = i.middleware.workspace;
                        (n && n.didChangeWorkspaceFolders ? n.didChangeWorkspaceFolders(e, t) : t(e)).catch((e => {
                            this._client.error(`Sending notification ${r.DidChangeWorkspaceFoldersNotification.type.method} failed`, e)
                        }))
                    }));
                    this._listeners.set(t, n), this.sendInitialEvent(o.workspace.workspaceFolders)
                }

                unregister(e) {
                    let t = this._listeners.get(e);
                    void 0 !== t && (this._listeners.delete(e), t.dispose())
                }

                dispose() {
                    for (let e of this._listeners.values()) e.dispose();
                    this._listeners.clear()
                }

                asProtocol(e) {
                    return void 0 === e ? null : {uri: this._client.code2ProtocolConverter.asUri(e.uri), name: e.name}
                }
            }
        }, 5361: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.WorkspaceSymbolFeature = void 0;
            const n = i(9496), o = i(3455), r = i(5151), s = i(6157), a = i(2678);

            class c extends r.WorkspaceFeature {
                constructor(e) {
                    super(e, o.WorkspaceSymbolRequest.type)
                }

                fillClientCapabilities(e) {
                    let t = (0, r.ensure)((0, r.ensure)(e, "workspace"), "symbol");
                    t.dynamicRegistration = !0, t.symbolKind = {valueSet: s.SupportedSymbolKinds}, t.tagSupport = {valueSet: s.SupportedSymbolTags}, t.resolveSupport = {properties: ["location.range"]}
                }

                initialize(e) {
                    e.workspaceSymbolProvider && this.register({
                        id: a.generateUuid(),
                        registerOptions: !0 === e.workspaceSymbolProvider ? {workDoneProgress: !1} : e.workspaceSymbolProvider
                    })
                }

                registerLanguageProvider(e) {
                    const t = {
                        provideWorkspaceSymbols: (e, t) => {
                            const i = this._client,
                                n = (e, t) => i.sendRequest(o.WorkspaceSymbolRequest.type, {query: e}, t).then((e => t.isCancellationRequested ? null : i.protocol2CodeConverter.asSymbolInformations(e, t)), (e => i.handleFailedRequest(o.WorkspaceSymbolRequest.type, t, e, null))),
                                r = i.middleware;
                            return r.provideWorkspaceSymbols ? r.provideWorkspaceSymbols(e, t, n) : n(e, t)
                        }, resolveWorkspaceSymbol: !0 === e.resolveProvider ? (e, t) => {
                            const i = this._client,
                                n = (e, t) => i.sendRequest(o.WorkspaceSymbolResolveRequest.type, i.code2ProtocolConverter.asWorkspaceSymbol(e), t).then((e => t.isCancellationRequested ? null : i.protocol2CodeConverter.asSymbolInformation(e)), (e => i.handleFailedRequest(o.WorkspaceSymbolResolveRequest.type, t, e, null))),
                                r = i.middleware;
                            return r.resolveWorkspaceSymbol ? r.resolveWorkspaceSymbol(e, t, n) : n(e, t)
                        } : void 0
                    };
                    return [n.languages.registerWorkspaceSymbolProvider(t), t]
                }
            }

            t.WorkspaceSymbolFeature = c
        }, 2649: function (e, t, i) {
            "use strict";
            var n = this && this.__createBinding || (Object.create ? function (e, t, i, n) {
                void 0 === n && (n = i);
                var o = Object.getOwnPropertyDescriptor(t, i);
                o && !("get" in o ? !t.__esModule : o.writable || o.configurable) || (o = {
                    enumerable: !0,
                    get: function () {
                        return t[i]
                    }
                }), Object.defineProperty(e, n, o)
            } : function (e, t, i, n) {
                void 0 === n && (n = i), e[n] = t[i]
            }), o = this && this.__exportStar || function (e, t) {
                for (var i in e) "default" === i || Object.prototype.hasOwnProperty.call(t, i) || n(t, e, i)
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.SettingMonitor = t.LanguageClient = t.TransportKind = void 0;
            const r = i(2081), s = i(7147), a = i(1017), c = i(9496), l = i(2592), u = i(1912), d = i(4378),
                p = i(3610), h = i(6652), g = i(4866);
            o(i(3610), t), o(i(6498), t);
            const m = "^1.67.0";
            var f, v, y, C, w, b;
            !function (e) {
                e[e.stdio = 0] = "stdio", e[e.ipc = 1] = "ipc", e[e.pipe = 2] = "pipe", e[e.socket = 3] = "socket"
            }(f = t.TransportKind || (t.TransportKind = {})), function (e) {
                e.isSocket = function (e) {
                    const t = e;
                    return t && t.kind === f.socket && l.number(t.port)
                }
            }(v || (v = {})), function (e) {
                e.is = function (e) {
                    return l.string(e.command)
                }
            }(y || (y = {})), function (e) {
                e.is = function (e) {
                    return l.string(e.module)
                }
            }(C || (C = {})), function (e) {
                e.is = function (e) {
                    let t = e;
                    return t && void 0 !== t.writer && void 0 !== t.reader
                }
            }(w || (w = {})), function (e) {
                e.is = function (e) {
                    let t = e;
                    return t && void 0 !== t.process && "boolean" == typeof t.detached
                }
            }(b || (b = {}));

            class k extends u.BaseLanguageClient {
                constructor(e, t, i, n, o) {
                    let r, s, a, c, u;
                    l.string(t) ? (r = e, s = t, a = i, c = n, u = !!o) : (r = e.toLowerCase(), s = e, a = t, c = i, u = n), void 0 === u && (u = !1), super(r, s, c), this._serverOptions = a, this._forceDebug = u, this._isInDebugMode = u;
                    try {
                        this.checkVersion()
                    } catch (e) {
                        throw l.string(e.message) && this.outputChannel.appendLine(e.message), e
                    }
                }

                checkVersion() {
                    const e = h(c.version);
                    if (!e) throw new Error(`No valid VS Code version detected. Version string is: ${c.version}`);
                    if (e.prerelease && e.prerelease.length > 0 && (e.prerelease = []), !g(e, m)) throw new Error(`The language client requires VS Code version ${m} but received version ${c.version}`)
                }

                get isInDebugMode() {
                    return this._isInDebugMode
                }

                async restart() {
                    await this.stop(), this.isInDebugMode ? (await new Promise((e => setTimeout(e, 1e3))), await this.start()) : await this.start()
                }

                stop(e = 2e3) {
                    return super.stop(e).finally((() => {
                        if (this._serverProcess) {
                            const e = this._serverProcess;
                            this._serverProcess = void 0, void 0 !== this._isDetached && this._isDetached || this.checkProcessDied(e), this._isDetached = void 0
                        }
                    }))
                }

                checkProcessDied(e) {
                    e && void 0 !== e.pid && setTimeout((() => {
                        try {
                            void 0 !== e.pid && (process.kill(e.pid, 0), (0, d.terminate)(e))
                        } catch (e) {
                        }
                    }), 2e3)
                }

                handleConnectionClosed() {
                    this._serverProcess = void 0, super.handleConnectionClosed()
                }

                fillInitializeParams(e) {
                    super.fillInitializeParams(e), null === e.processId && (e.processId = process.pid)
                }

                createMessageTransports(e) {
                    function t(e, t) {
                        if (!e && !t) return;
                        const i = Object.create(null);
                        return Object.keys(process.env).forEach((e => i[e] = process.env[e])), t && (i.ELECTRON_RUN_AS_NODE = "1", i.ELECTRON_NO_ASAR = "1"), e && Object.keys(e).forEach((t => i[t] = e[t])), i
                    }

                    const i = ["--debug=", "--debug-brk=", "--inspect=", "--inspect-brk="],
                        n = ["--debug", "--debug-brk", "--inspect", "--inspect-brk"];

                    function o(e) {
                        if (null === e.stdin || null === e.stdout || null === e.stderr) throw new Error("Process created without stdio streams")
                    }

                    const s = this._serverOptions;
                    if (l.func(s)) return s().then((t => {
                        if (u.MessageTransports.is(t)) return this._isDetached = !!t.detached, t;
                        if (w.is(t)) return this._isDetached = !!t.detached, {
                            reader: new p.StreamMessageReader(t.reader),
                            writer: new p.StreamMessageWriter(t.writer)
                        };
                        {
                            let i;
                            return b.is(t) ? (i = t.process, this._isDetached = t.detached) : (i = t, this._isDetached = !1), i.stderr.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), {
                                reader: new p.StreamMessageReader(i.stdout),
                                writer: new p.StreamMessageWriter(i.stdin)
                            }
                        }
                    }));
                    let a, c = s;
                    return c.run || c.debug ? this._forceDebug || function () {
                        let e = process.execArgv;
                        return !!e && e.some((e => i.some((t => e.startsWith(t))) || n.some((t => e === t))))
                    }() ? (a = c.debug, this._isInDebugMode = !0) : (a = c.run, this._isInDebugMode = !1) : a = s, this._getServerWorkingDir(a.options).then((i => {
                        if (C.is(a) && a.module) {
                            let n = a, s = n.transport || f.stdio;
                            if (!n.runtime) {
                                let a;
                                return new Promise(((c, u) => {
                                    const d = (n.args && n.args.slice()) ?? [];
                                    s === f.ipc ? d.push("--node-ipc") : s === f.stdio ? d.push("--stdio") : s === f.pipe ? (a = (0, p.generateRandomPipeName)(), d.push(`--pipe=${a}`)) : v.isSocket(s) && d.push(`--socket=${s.port}`), d.push(`--clientProcessId=${process.pid.toString()}`);
                                    const h = n.options ?? Object.create(null);
                                    if (h.env = t(h.env, !0), h.execArgv = h.execArgv || [], h.cwd = i, h.silent = !0, s === f.ipc || s === f.stdio) {
                                        const t = r.fork(n.module, d || [], h);
                                        o(t), this._serverProcess = t, t.stderr.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), s === f.ipc ? (t.stdout.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), c({
                                            reader: new p.IPCMessageReader(this._serverProcess),
                                            writer: new p.IPCMessageWriter(this._serverProcess)
                                        })) : c({
                                            reader: new p.StreamMessageReader(t.stdout),
                                            writer: new p.StreamMessageWriter(t.stdin)
                                        })
                                    } else s === f.pipe ? (0, p.createClientPipeTransport)(a).then((t => {
                                        const i = r.fork(n.module, d || [], h);
                                        o(i), this._serverProcess = i, i.stderr.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), i.stdout.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), t.onConnected().then((e => {
                                            c({reader: e[0], writer: e[1]})
                                        }), u)
                                    }), u) : v.isSocket(s) && (0, p.createClientSocketTransport)(s.port).then((t => {
                                        const i = r.fork(n.module, d || [], h);
                                        o(i), this._serverProcess = i, i.stderr.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), i.stdout.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), t.onConnected().then((e => {
                                            c({reader: e[0], writer: e[1]})
                                        }), u)
                                    }), u)
                                }))
                            }
                            {
                                const o = [], a = n.options ?? Object.create(null);
                                a.execArgv && a.execArgv.forEach((e => o.push(e))), o.push(n.module), n.args && n.args.forEach((e => o.push(e)));
                                const c = Object.create(null);
                                c.cwd = i, c.env = t(a.env, !1);
                                const u = this._getRuntimePath(n.runtime, i);
                                let d;
                                if (s === f.ipc ? (c.stdio = [null, null, null, "ipc"], o.push("--node-ipc")) : s === f.stdio ? o.push("--stdio") : s === f.pipe ? (d = (0, p.generateRandomPipeName)(), o.push(`--pipe=${d}`)) : v.isSocket(s) && o.push(`--socket=${s.port}`), o.push(`--clientProcessId=${process.pid.toString()}`), s === f.ipc || s === f.stdio) {
                                    const t = r.spawn(u, o, c);
                                    return t && t.pid ? (this._serverProcess = t, t.stderr.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), s === f.ipc ? (t.stdout.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), Promise.resolve({
                                        reader: new p.IPCMessageReader(t),
                                        writer: new p.IPCMessageWriter(t)
                                    })) : Promise.resolve({
                                        reader: new p.StreamMessageReader(t.stdout),
                                        writer: new p.StreamMessageWriter(t.stdin)
                                    })) : S(t, `Launching server using runtime ${u} failed.`)
                                }
                                if (s === f.pipe) return (0, p.createClientPipeTransport)(d).then((t => {
                                    const i = r.spawn(u, o, c);
                                    return i && i.pid ? (this._serverProcess = i, i.stderr.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), i.stdout.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), t.onConnected().then((e => ({
                                        reader: e[0],
                                        writer: e[1]
                                    })))) : S(i, `Launching server using runtime ${u} failed.`)
                                }));
                                if (v.isSocket(s)) return (0, p.createClientSocketTransport)(s.port).then((t => {
                                    const i = r.spawn(u, o, c);
                                    return i && i.pid ? (this._serverProcess = i, i.stderr.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), i.stdout.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), t.onConnected().then((e => ({
                                        reader: e[0],
                                        writer: e[1]
                                    })))) : S(i, `Launching server using runtime ${u} failed.`)
                                }))
                            }
                        } else if (y.is(a) && a.command) {
                            const t = a, n = void 0 !== a.args ? a.args.slice(0) : [];
                            let o;
                            const s = a.transport;
                            if (s === f.stdio) n.push("--stdio"); else if (s === f.pipe) o = (0, p.generateRandomPipeName)(), n.push(`--pipe=${o}`); else if (v.isSocket(s)) n.push(`--socket=${s.port}`); else if (s === f.ipc) throw new Error("Transport kind ipc is not support for command executable");
                            const c = Object.assign({}, t.options);
                            if (c.cwd = c.cwd || i, void 0 === s || s === f.stdio) {
                                const i = r.spawn(t.command, n, c);
                                return i && i.pid ? (i.stderr.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), this._serverProcess = i, this._isDetached = !!c.detached, Promise.resolve({
                                    reader: new p.StreamMessageReader(i.stdout),
                                    writer: new p.StreamMessageWriter(i.stdin)
                                })) : S(i, `Launching server using command ${t.command} failed.`)
                            }
                            if (s === f.pipe) return (0, p.createClientPipeTransport)(o).then((i => {
                                const o = r.spawn(t.command, n, c);
                                return o && o.pid ? (this._serverProcess = o, this._isDetached = !!c.detached, o.stderr.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), o.stdout.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), i.onConnected().then((e => ({
                                    reader: e[0],
                                    writer: e[1]
                                })))) : S(o, `Launching server using command ${t.command} failed.`)
                            }));
                            if (v.isSocket(s)) return (0, p.createClientSocketTransport)(s.port).then((i => {
                                const o = r.spawn(t.command, n, c);
                                return o && o.pid ? (this._serverProcess = o, this._isDetached = !!c.detached, o.stderr.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), o.stdout.on("data", (t => this.outputChannel.append(l.string(t) ? t : t.toString(e)))), i.onConnected().then((e => ({
                                    reader: e[0],
                                    writer: e[1]
                                })))) : S(o, `Launching server using command ${t.command} failed.`)
                            }))
                        }
                        return Promise.reject(new Error("Unsupported server configuration " + JSON.stringify(s, null, 4)))
                    }))
                }

                _getRuntimePath(e, t) {
                    if (a.isAbsolute(e)) return e;
                    const i = this._mainGetRootPath();
                    if (void 0 !== i) {
                        const t = a.join(i, e);
                        if (s.existsSync(t)) return t
                    }
                    if (void 0 !== t) {
                        const i = a.join(t, e);
                        if (s.existsSync(i)) return i
                    }
                    return e
                }

                _mainGetRootPath() {
                    let e = c.workspace.workspaceFolders;
                    if (!e || 0 === e.length) return;
                    let t = e[0];
                    return "file" === t.uri.scheme ? t.uri.fsPath : void 0
                }

                _getServerWorkingDir(e) {
                    let t = e && e.cwd;
                    return t || (t = this.clientOptions.workspaceFolder ? this.clientOptions.workspaceFolder.uri.fsPath : this._mainGetRootPath()), t ? new Promise((e => {
                        s.lstat(t, ((i, n) => {
                            e(!i && n.isDirectory() ? t : void 0)
                        }))
                    })) : Promise.resolve(void 0)
                }

                getLocale() {
                    const e = process.env.VSCODE_NLS_CONFIG;
                    if (void 0 === e) return "en";
                    let t;
                    try {
                        t = JSON.parse(e)
                    } catch (e) {
                    }
                    return void 0 === t || "string" != typeof t.locale ? "en" : t.locale
                }
            }

            t.LanguageClient = k;

            function S(e, t) {
                return null === e ? Promise.reject(t) : new Promise(((i, n) => {
                    e.on("error", (e => {
                        n(`${t} ${e}`)
                    })), setImmediate((() => n(t)))
                }))
            }

            t.SettingMonitor = class {
                constructor(e, t) {
                    this._client = e, this._setting = t, this._listeners = []
                }

                start() {
                    return c.workspace.onDidChangeConfiguration(this.onDidChangeConfiguration, this, this._listeners), this.onDidChangeConfiguration(), new c.Disposable((() => {
                        this._client.needsStop() && this._client.stop()
                    }))
                }

                onDidChangeConfiguration() {
                    let e = this._setting.indexOf("."), t = e >= 0 ? this._setting.substr(0, e) : this._setting,
                        i = e >= 0 ? this._setting.substr(e + 1) : void 0,
                        n = i ? c.workspace.getConfiguration(t).get(i, !1) : c.workspace.getConfiguration(t);
                    n && this._client.needsStart() ? this._client.start().catch((e => this._client.error("Start failed after configuration change", e, "force"))) : !n && this._client.needsStop() && this._client.stop().catch((e => this._client.error("Stop failed after configuration change", e, "force")))
                }
            }
        }, 4378: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.terminate = void 0;
            const n = i(2081), o = i(1017), r = "win32" === process.platform, s = "darwin" === process.platform,
                a = "linux" === process.platform;
            t.terminate = function (e, t) {
                if (r) try {
                    let i = {stdio: ["pipe", "pipe", "ignore"]};
                    return t && (i.cwd = t), n.execFileSync("taskkill", ["/T", "/F", "/PID", e.pid.toString()], i), !0
                } catch (e) {
                    return !1
                } else {
                    if (!a && !s) return e.kill("SIGKILL"), !0;
                    try {
                        var i = (0, o.join)(__dirname, "terminateProcess.sh");
                        return !n.spawnSync(i, [e.pid.toString()]).error
                    } catch (e) {
                        return !1
                    }
                }
            }
        }, 6396: (e, t, i) => {
            "use strict";
            e.exports = i(2649)
        }, 8644: function (e, t, i) {
            "use strict";
            var n = this && this.__createBinding || (Object.create ? function (e, t, i, n) {
                void 0 === n && (n = i);
                var o = Object.getOwnPropertyDescriptor(t, i);
                o && !("get" in o ? !t.__esModule : o.writable || o.configurable) || (o = {
                    enumerable: !0,
                    get: function () {
                        return t[i]
                    }
                }), Object.defineProperty(e, n, o)
            } : function (e, t, i, n) {
                void 0 === n && (n = i), e[n] = t[i]
            }), o = this && this.__exportStar || function (e, t) {
                for (var i in e) "default" === i || Object.prototype.hasOwnProperty.call(t, i) || n(t, e, i)
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.LSPErrorCodes = t.createProtocolConnection = void 0, o(i(8862), t), o(i(4843), t), o(i(758), t), o(i(5512), t);
            var r = i(8922);
            Object.defineProperty(t, "createProtocolConnection", {
                enumerable: !0, get: function () {
                    return r.createProtocolConnection
                }
            }), function (e) {
                e.lspReservedErrorRangeStart = -32899, e.RequestFailed = -32803, e.ServerCancelled = -32802, e.ContentModified = -32801, e.RequestCancelled = -32800, e.lspReservedErrorRangeEnd = -32800
            }(t.LSPErrorCodes || (t.LSPErrorCodes = {}))
        }, 8922: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.createProtocolConnection = void 0;
            const n = i(8862);
            t.createProtocolConnection = function (e, t, i, o) {
                return n.ConnectionStrategy.is(o) && (o = {connectionStrategy: o}), (0, n.createMessageConnection)(e, t, i, o)
            }
        }, 758: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ProtocolNotificationType = t.ProtocolNotificationType0 = t.ProtocolRequestType = t.ProtocolRequestType0 = t.RegistrationType = t.MessageDirection = void 0;
            const n = i(8862);
            !function (e) {
                e.clientToServer = "clientToServer", e.serverToClient = "serverToClient", e.both = "both"
            }(t.MessageDirection || (t.MessageDirection = {}));
            t.RegistrationType = class {
                constructor(e) {
                    this.method = e
                }
            };

            class o extends n.RequestType0 {
                constructor(e) {
                    super(e)
                }
            }

            t.ProtocolRequestType0 = o;

            class r extends n.RequestType {
                constructor(e) {
                    super(e, n.ParameterStructures.byName)
                }
            }

            t.ProtocolRequestType = r;

            class s extends n.NotificationType0 {
                constructor(e) {
                    super(e)
                }
            }

            t.ProtocolNotificationType0 = s;

            class a extends n.NotificationType {
                constructor(e) {
                    super(e, n.ParameterStructures.byName)
                }
            }

            t.ProtocolNotificationType = a
        }, 576: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.CallHierarchyOutgoingCallsRequest = t.CallHierarchyIncomingCallsRequest = t.CallHierarchyPrepareRequest = void 0;
            const n = i(758);
            !function (e) {
                e.method = "textDocument/prepareCallHierarchy", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.CallHierarchyPrepareRequest || (t.CallHierarchyPrepareRequest = {})), function (e) {
                e.method = "callHierarchy/incomingCalls", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.CallHierarchyIncomingCallsRequest || (t.CallHierarchyIncomingCallsRequest = {})), function (e) {
                e.method = "callHierarchy/outgoingCalls", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.CallHierarchyOutgoingCallsRequest || (t.CallHierarchyOutgoingCallsRequest = {}))
        }, 2042: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ColorPresentationRequest = t.DocumentColorRequest = void 0;
            const n = i(758);
            !function (e) {
                e.method = "textDocument/documentColor", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.DocumentColorRequest || (t.DocumentColorRequest = {})), function (e) {
                e.method = "textDocument/colorPresentation", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.ColorPresentationRequest || (t.ColorPresentationRequest = {}))
        }, 5589: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ConfigurationRequest = void 0;
            const n = i(758);
            !function (e) {
                e.method = "workspace/configuration", e.messageDirection = n.MessageDirection.serverToClient, e.type = new n.ProtocolRequestType(e.method)
            }(t.ConfigurationRequest || (t.ConfigurationRequest = {}))
        }, 1322: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DeclarationRequest = void 0;
            const n = i(758);
            !function (e) {
                e.method = "textDocument/declaration", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.DeclarationRequest || (t.DeclarationRequest = {}))
        }, 4670: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DiagnosticRefreshRequest = t.WorkspaceDiagnosticRequest = t.DocumentDiagnosticRequest = t.DocumentDiagnosticReportKind = t.DiagnosticServerCancellationData = void 0;
            const n = i(8862), o = i(340), r = i(758);
            !function (e) {
                e.is = function (e) {
                    const t = e;
                    return t && o.boolean(t.retriggerRequest)
                }
            }(t.DiagnosticServerCancellationData || (t.DiagnosticServerCancellationData = {})), function (e) {
                e.Full = "full", e.Unchanged = "unchanged"
            }(t.DocumentDiagnosticReportKind || (t.DocumentDiagnosticReportKind = {})), function (e) {
                e.method = "textDocument/diagnostic", e.messageDirection = r.MessageDirection.clientToServer, e.type = new r.ProtocolRequestType(e.method), e.partialResult = new n.ProgressType
            }(t.DocumentDiagnosticRequest || (t.DocumentDiagnosticRequest = {})), function (e) {
                e.method = "workspace/diagnostic", e.messageDirection = r.MessageDirection.clientToServer, e.type = new r.ProtocolRequestType(e.method), e.partialResult = new n.ProgressType
            }(t.WorkspaceDiagnosticRequest || (t.WorkspaceDiagnosticRequest = {})), function (e) {
                e.method = "workspace/diagnostic/refresh", e.messageDirection = r.MessageDirection.clientToServer, e.type = new r.ProtocolRequestType0(e.method)
            }(t.DiagnosticRefreshRequest || (t.DiagnosticRefreshRequest = {}))
        }, 4644: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.WillDeleteFilesRequest = t.DidDeleteFilesNotification = t.DidRenameFilesNotification = t.WillRenameFilesRequest = t.DidCreateFilesNotification = t.WillCreateFilesRequest = t.FileOperationPatternKind = void 0;
            const n = i(758);
            !function (e) {
                e.file = "file", e.folder = "folder"
            }(t.FileOperationPatternKind || (t.FileOperationPatternKind = {})), function (e) {
                e.method = "workspace/willCreateFiles", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.WillCreateFilesRequest || (t.WillCreateFilesRequest = {})), function (e) {
                e.method = "workspace/didCreateFiles", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolNotificationType(e.method)
            }(t.DidCreateFilesNotification || (t.DidCreateFilesNotification = {})), function (e) {
                e.method = "workspace/willRenameFiles", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.WillRenameFilesRequest || (t.WillRenameFilesRequest = {})), function (e) {
                e.method = "workspace/didRenameFiles", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolNotificationType(e.method)
            }(t.DidRenameFilesNotification || (t.DidRenameFilesNotification = {})), function (e) {
                e.method = "workspace/didDeleteFiles", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolNotificationType(e.method)
            }(t.DidDeleteFilesNotification || (t.DidDeleteFilesNotification = {})), function (e) {
                e.method = "workspace/willDeleteFiles", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.WillDeleteFilesRequest || (t.WillDeleteFilesRequest = {}))
        }, 8727: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.FoldingRangeRequest = void 0;
            const n = i(758);
            !function (e) {
                e.method = "textDocument/foldingRange", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.FoldingRangeRequest || (t.FoldingRangeRequest = {}))
        }, 8355: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ImplementationRequest = void 0;
            const n = i(758);
            !function (e) {
                e.method = "textDocument/implementation", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.ImplementationRequest || (t.ImplementationRequest = {}))
        }, 3289: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.InlayHintRefreshRequest = t.InlayHintResolveRequest = t.InlayHintRequest = void 0;
            const n = i(758);
            !function (e) {
                e.method = "textDocument/inlayHint", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.InlayHintRequest || (t.InlayHintRequest = {})), function (e) {
                e.method = "inlayHint/resolve", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.InlayHintResolveRequest || (t.InlayHintResolveRequest = {})), function (e) {
                e.method = "workspace/inlayHint/refresh", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType0(e.method)
            }(t.InlayHintRefreshRequest || (t.InlayHintRefreshRequest = {}))
        }, 7677: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.InlineValueRefreshRequest = t.InlineValueRequest = void 0;
            const n = i(758);
            !function (e) {
                e.method = "textDocument/inlineValue", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.InlineValueRequest || (t.InlineValueRequest = {})), function (e) {
                e.method = "workspace/inlineValue/refresh", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType0(e.method)
            }(t.InlineValueRefreshRequest || (t.InlineValueRefreshRequest = {}))
        }, 5512: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.WorkspaceSymbolRequest = t.CodeActionResolveRequest = t.CodeActionRequest = t.DocumentSymbolRequest = t.DocumentHighlightRequest = t.ReferencesRequest = t.DefinitionRequest = t.SignatureHelpRequest = t.SignatureHelpTriggerKind = t.HoverRequest = t.CompletionResolveRequest = t.CompletionRequest = t.CompletionTriggerKind = t.PublishDiagnosticsNotification = t.WatchKind = t.RelativePattern = t.FileChangeType = t.DidChangeWatchedFilesNotification = t.WillSaveTextDocumentWaitUntilRequest = t.WillSaveTextDocumentNotification = t.TextDocumentSaveReason = t.DidSaveTextDocumentNotification = t.DidCloseTextDocumentNotification = t.DidChangeTextDocumentNotification = t.TextDocumentContentChangeEvent = t.DidOpenTextDocumentNotification = t.TextDocumentSyncKind = t.TelemetryEventNotification = t.LogMessageNotification = t.ShowMessageRequest = t.ShowMessageNotification = t.MessageType = t.DidChangeConfigurationNotification = t.ExitNotification = t.ShutdownRequest = t.InitializedNotification = t.InitializeErrorCodes = t.InitializeRequest = t.WorkDoneProgressOptions = t.TextDocumentRegistrationOptions = t.StaticRegistrationOptions = t.PositionEncodingKind = t.FailureHandlingKind = t.ResourceOperationKind = t.UnregistrationRequest = t.RegistrationRequest = t.DocumentSelector = t.NotebookCellTextDocumentFilter = t.NotebookDocumentFilter = t.TextDocumentFilter = void 0, t.TypeHierarchySubtypesRequest = t.TypeHierarchyPrepareRequest = t.MonikerRequest = t.MonikerKind = t.UniquenessLevel = t.WillDeleteFilesRequest = t.DidDeleteFilesNotification = t.WillRenameFilesRequest = t.DidRenameFilesNotification = t.WillCreateFilesRequest = t.DidCreateFilesNotification = t.FileOperationPatternKind = t.LinkedEditingRangeRequest = t.ShowDocumentRequest = t.SemanticTokensRegistrationType = t.SemanticTokensRefreshRequest = t.SemanticTokensRangeRequest = t.SemanticTokensDeltaRequest = t.SemanticTokensRequest = t.TokenFormat = t.CallHierarchyPrepareRequest = t.CallHierarchyOutgoingCallsRequest = t.CallHierarchyIncomingCallsRequest = t.WorkDoneProgressCancelNotification = t.WorkDoneProgressCreateRequest = t.WorkDoneProgress = t.SelectionRangeRequest = t.DeclarationRequest = t.FoldingRangeRequest = t.ColorPresentationRequest = t.DocumentColorRequest = t.ConfigurationRequest = t.DidChangeWorkspaceFoldersNotification = t.WorkspaceFoldersRequest = t.TypeDefinitionRequest = t.ImplementationRequest = t.ApplyWorkspaceEditRequest = t.ExecuteCommandRequest = t.PrepareRenameRequest = t.RenameRequest = t.PrepareSupportDefaultBehavior = t.DocumentOnTypeFormattingRequest = t.DocumentRangeFormattingRequest = t.DocumentFormattingRequest = t.DocumentLinkResolveRequest = t.DocumentLinkRequest = t.CodeLensRefreshRequest = t.CodeLensResolveRequest = t.CodeLensRequest = t.WorkspaceSymbolResolveRequest = void 0, t.DidCloseNotebookDocumentNotification = t.DidSaveNotebookDocumentNotification = t.DidChangeNotebookDocumentNotification = t.NotebookCellArrayChange = t.DidOpenNotebookDocumentNotification = t.NotebookDocumentSyncRegistrationType = t.NotebookDocument = t.NotebookCell = t.ExecutionSummary = t.NotebookCellKind = t.DiagnosticRefreshRequest = t.WorkspaceDiagnosticRequest = t.DocumentDiagnosticRequest = t.DocumentDiagnosticReportKind = t.DiagnosticServerCancellationData = t.InlayHintRefreshRequest = t.InlayHintResolveRequest = t.InlayHintRequest = t.InlineValueRefreshRequest = t.InlineValueRequest = t.TypeHierarchySupertypesRequest = void 0;
            const n = i(758), o = i(4843), r = i(340), s = i(8355);
            Object.defineProperty(t, "ImplementationRequest", {
                enumerable: !0, get: function () {
                    return s.ImplementationRequest
                }
            });
            const a = i(1145);
            Object.defineProperty(t, "TypeDefinitionRequest", {
                enumerable: !0, get: function () {
                    return a.TypeDefinitionRequest
                }
            });
            const c = i(4555);
            Object.defineProperty(t, "WorkspaceFoldersRequest", {
                enumerable: !0, get: function () {
                    return c.WorkspaceFoldersRequest
                }
            }), Object.defineProperty(t, "DidChangeWorkspaceFoldersNotification", {
                enumerable: !0, get: function () {
                    return c.DidChangeWorkspaceFoldersNotification
                }
            });
            const l = i(5589);
            Object.defineProperty(t, "ConfigurationRequest", {
                enumerable: !0, get: function () {
                    return l.ConfigurationRequest
                }
            });
            const u = i(2042);
            Object.defineProperty(t, "DocumentColorRequest", {
                enumerable: !0, get: function () {
                    return u.DocumentColorRequest
                }
            }), Object.defineProperty(t, "ColorPresentationRequest", {
                enumerable: !0, get: function () {
                    return u.ColorPresentationRequest
                }
            });
            const d = i(8727);
            Object.defineProperty(t, "FoldingRangeRequest", {
                enumerable: !0, get: function () {
                    return d.FoldingRangeRequest
                }
            });
            const p = i(1322);
            Object.defineProperty(t, "DeclarationRequest", {
                enumerable: !0, get: function () {
                    return p.DeclarationRequest
                }
            });
            const h = i(3721);
            Object.defineProperty(t, "SelectionRangeRequest", {
                enumerable: !0, get: function () {
                    return h.SelectionRangeRequest
                }
            });
            const g = i(591);
            Object.defineProperty(t, "WorkDoneProgress", {
                enumerable: !0, get: function () {
                    return g.WorkDoneProgress
                }
            }), Object.defineProperty(t, "WorkDoneProgressCreateRequest", {
                enumerable: !0, get: function () {
                    return g.WorkDoneProgressCreateRequest
                }
            }), Object.defineProperty(t, "WorkDoneProgressCancelNotification", {
                enumerable: !0, get: function () {
                    return g.WorkDoneProgressCancelNotification
                }
            });
            const m = i(576);
            Object.defineProperty(t, "CallHierarchyIncomingCallsRequest", {
                enumerable: !0, get: function () {
                    return m.CallHierarchyIncomingCallsRequest
                }
            }), Object.defineProperty(t, "CallHierarchyOutgoingCallsRequest", {
                enumerable: !0, get: function () {
                    return m.CallHierarchyOutgoingCallsRequest
                }
            }), Object.defineProperty(t, "CallHierarchyPrepareRequest", {
                enumerable: !0, get: function () {
                    return m.CallHierarchyPrepareRequest
                }
            });
            const f = i(4206);
            Object.defineProperty(t, "TokenFormat", {
                enumerable: !0, get: function () {
                    return f.TokenFormat
                }
            }), Object.defineProperty(t, "SemanticTokensRequest", {
                enumerable: !0, get: function () {
                    return f.SemanticTokensRequest
                }
            }), Object.defineProperty(t, "SemanticTokensDeltaRequest", {
                enumerable: !0, get: function () {
                    return f.SemanticTokensDeltaRequest
                }
            }), Object.defineProperty(t, "SemanticTokensRangeRequest", {
                enumerable: !0, get: function () {
                    return f.SemanticTokensRangeRequest
                }
            }), Object.defineProperty(t, "SemanticTokensRefreshRequest", {
                enumerable: !0, get: function () {
                    return f.SemanticTokensRefreshRequest
                }
            }), Object.defineProperty(t, "SemanticTokensRegistrationType", {
                enumerable: !0, get: function () {
                    return f.SemanticTokensRegistrationType
                }
            });
            const v = i(8069);
            Object.defineProperty(t, "ShowDocumentRequest", {
                enumerable: !0, get: function () {
                    return v.ShowDocumentRequest
                }
            });
            const y = i(757);
            Object.defineProperty(t, "LinkedEditingRangeRequest", {
                enumerable: !0, get: function () {
                    return y.LinkedEditingRangeRequest
                }
            });
            const C = i(4644);
            Object.defineProperty(t, "FileOperationPatternKind", {
                enumerable: !0, get: function () {
                    return C.FileOperationPatternKind
                }
            }), Object.defineProperty(t, "DidCreateFilesNotification", {
                enumerable: !0, get: function () {
                    return C.DidCreateFilesNotification
                }
            }), Object.defineProperty(t, "WillCreateFilesRequest", {
                enumerable: !0, get: function () {
                    return C.WillCreateFilesRequest
                }
            }), Object.defineProperty(t, "DidRenameFilesNotification", {
                enumerable: !0, get: function () {
                    return C.DidRenameFilesNotification
                }
            }), Object.defineProperty(t, "WillRenameFilesRequest", {
                enumerable: !0, get: function () {
                    return C.WillRenameFilesRequest
                }
            }), Object.defineProperty(t, "DidDeleteFilesNotification", {
                enumerable: !0, get: function () {
                    return C.DidDeleteFilesNotification
                }
            }), Object.defineProperty(t, "WillDeleteFilesRequest", {
                enumerable: !0, get: function () {
                    return C.WillDeleteFilesRequest
                }
            });
            const w = i(5203);
            Object.defineProperty(t, "UniquenessLevel", {
                enumerable: !0, get: function () {
                    return w.UniquenessLevel
                }
            }), Object.defineProperty(t, "MonikerKind", {
                enumerable: !0, get: function () {
                    return w.MonikerKind
                }
            }), Object.defineProperty(t, "MonikerRequest", {
                enumerable: !0, get: function () {
                    return w.MonikerRequest
                }
            });
            const b = i(2e3);
            Object.defineProperty(t, "TypeHierarchyPrepareRequest", {
                enumerable: !0, get: function () {
                    return b.TypeHierarchyPrepareRequest
                }
            }), Object.defineProperty(t, "TypeHierarchySubtypesRequest", {
                enumerable: !0, get: function () {
                    return b.TypeHierarchySubtypesRequest
                }
            }), Object.defineProperty(t, "TypeHierarchySupertypesRequest", {
                enumerable: !0, get: function () {
                    return b.TypeHierarchySupertypesRequest
                }
            });
            const k = i(7677);
            Object.defineProperty(t, "InlineValueRequest", {
                enumerable: !0, get: function () {
                    return k.InlineValueRequest
                }
            }), Object.defineProperty(t, "InlineValueRefreshRequest", {
                enumerable: !0, get: function () {
                    return k.InlineValueRefreshRequest
                }
            });
            const S = i(3289);
            Object.defineProperty(t, "InlayHintRequest", {
                enumerable: !0, get: function () {
                    return S.InlayHintRequest
                }
            }), Object.defineProperty(t, "InlayHintResolveRequest", {
                enumerable: !0, get: function () {
                    return S.InlayHintResolveRequest
                }
            }), Object.defineProperty(t, "InlayHintRefreshRequest", {
                enumerable: !0, get: function () {
                    return S.InlayHintRefreshRequest
                }
            });
            const D = i(4670);
            Object.defineProperty(t, "DiagnosticServerCancellationData", {
                enumerable: !0, get: function () {
                    return D.DiagnosticServerCancellationData
                }
            }), Object.defineProperty(t, "DocumentDiagnosticReportKind", {
                enumerable: !0, get: function () {
                    return D.DocumentDiagnosticReportKind
                }
            }), Object.defineProperty(t, "DocumentDiagnosticRequest", {
                enumerable: !0, get: function () {
                    return D.DocumentDiagnosticRequest
                }
            }), Object.defineProperty(t, "WorkspaceDiagnosticRequest", {
                enumerable: !0, get: function () {
                    return D.WorkspaceDiagnosticRequest
                }
            }), Object.defineProperty(t, "DiagnosticRefreshRequest", {
                enumerable: !0, get: function () {
                    return D.DiagnosticRefreshRequest
                }
            });
            const P = i(57);
            var T, R, _, x;
            Object.defineProperty(t, "NotebookCellKind", {
                enumerable: !0, get: function () {
                    return P.NotebookCellKind
                }
            }), Object.defineProperty(t, "ExecutionSummary", {
                enumerable: !0, get: function () {
                    return P.ExecutionSummary
                }
            }), Object.defineProperty(t, "NotebookCell", {
                enumerable: !0, get: function () {
                    return P.NotebookCell
                }
            }), Object.defineProperty(t, "NotebookDocument", {
                enumerable: !0, get: function () {
                    return P.NotebookDocument
                }
            }), Object.defineProperty(t, "NotebookDocumentSyncRegistrationType", {
                enumerable: !0, get: function () {
                    return P.NotebookDocumentSyncRegistrationType
                }
            }), Object.defineProperty(t, "DidOpenNotebookDocumentNotification", {
                enumerable: !0, get: function () {
                    return P.DidOpenNotebookDocumentNotification
                }
            }), Object.defineProperty(t, "NotebookCellArrayChange", {
                enumerable: !0, get: function () {
                    return P.NotebookCellArrayChange
                }
            }), Object.defineProperty(t, "DidChangeNotebookDocumentNotification", {
                enumerable: !0, get: function () {
                    return P.DidChangeNotebookDocumentNotification
                }
            }), Object.defineProperty(t, "DidSaveNotebookDocumentNotification", {
                enumerable: !0, get: function () {
                    return P.DidSaveNotebookDocumentNotification
                }
            }), Object.defineProperty(t, "DidCloseNotebookDocumentNotification", {
                enumerable: !0, get: function () {
                    return P.DidCloseNotebookDocumentNotification
                }
            }), function (e) {
                e.is = function (e) {
                    const t = e;
                    return r.string(t.language) || r.string(t.scheme) || r.string(t.pattern)
                }
            }(T = t.TextDocumentFilter || (t.TextDocumentFilter = {})), function (e) {
                e.is = function (e) {
                    const t = e;
                    return r.objectLiteral(t) && (r.string(t.notebookType) || r.string(t.scheme) || r.string(t.pattern))
                }
            }(R = t.NotebookDocumentFilter || (t.NotebookDocumentFilter = {})), function (e) {
                e.is = function (e) {
                    const t = e;
                    return r.objectLiteral(t) && (r.string(t.notebook) || R.is(t.notebook)) && (void 0 === t.language || r.string(t.language))
                }
            }(_ = t.NotebookCellTextDocumentFilter || (t.NotebookCellTextDocumentFilter = {})), function (e) {
                e.is = function (e) {
                    if (!Array.isArray(e)) return !1;
                    for (let t of e) if (!r.string(t) && !T.is(t) && !_.is(t)) return !1;
                    return !0
                }
            }(x = t.DocumentSelector || (t.DocumentSelector = {})), function (e) {
                e.method = "client/registerCapability", e.messageDirection = n.MessageDirection.serverToClient, e.type = new n.ProtocolRequestType(e.method)
            }(t.RegistrationRequest || (t.RegistrationRequest = {})), function (e) {
                e.method = "client/unregisterCapability", e.messageDirection = n.MessageDirection.serverToClient, e.type = new n.ProtocolRequestType(e.method)
            }(t.UnregistrationRequest || (t.UnregistrationRequest = {})), function (e) {
                e.Create = "create", e.Rename = "rename", e.Delete = "delete"
            }(t.ResourceOperationKind || (t.ResourceOperationKind = {})), function (e) {
                e.Abort = "abort", e.Transactional = "transactional", e.TextOnlyTransactional = "textOnlyTransactional", e.Undo = "undo"
            }(t.FailureHandlingKind || (t.FailureHandlingKind = {})), function (e) {
                e.UTF8 = "utf-8", e.UTF16 = "utf-16", e.UTF32 = "utf-32"
            }(t.PositionEncodingKind || (t.PositionEncodingKind = {})), function (e) {
                e.hasId = function (e) {
                    const t = e;
                    return t && r.string(t.id) && t.id.length > 0
                }
            }(t.StaticRegistrationOptions || (t.StaticRegistrationOptions = {})), function (e) {
                e.is = function (e) {
                    const t = e;
                    return t && (null === t.documentSelector || x.is(t.documentSelector))
                }
            }(t.TextDocumentRegistrationOptions || (t.TextDocumentRegistrationOptions = {})), function (e) {
                e.is = function (e) {
                    const t = e;
                    return r.objectLiteral(t) && (void 0 === t.workDoneProgress || r.boolean(t.workDoneProgress))
                }, e.hasWorkDoneProgress = function (e) {
                    const t = e;
                    return t && r.boolean(t.workDoneProgress)
                }
            }(t.WorkDoneProgressOptions || (t.WorkDoneProgressOptions = {})), function (e) {
                e.method = "initialize", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.InitializeRequest || (t.InitializeRequest = {})), function (e) {
                e.unknownProtocolVersion = 1
            }(t.InitializeErrorCodes || (t.InitializeErrorCodes = {})), function (e) {
                e.method = "initialized", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolNotificationType(e.method)
            }(t.InitializedNotification || (t.InitializedNotification = {})), function (e) {
                e.method = "shutdown", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType0(e.method)
            }(t.ShutdownRequest || (t.ShutdownRequest = {})), function (e) {
                e.method = "exit", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolNotificationType0(e.method)
            }(t.ExitNotification || (t.ExitNotification = {})), function (e) {
                e.method = "workspace/didChangeConfiguration", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolNotificationType(e.method)
            }(t.DidChangeConfigurationNotification || (t.DidChangeConfigurationNotification = {})), function (e) {
                e.Error = 1, e.Warning = 2, e.Info = 3, e.Log = 4
            }(t.MessageType || (t.MessageType = {})), function (e) {
                e.method = "window/showMessage", e.messageDirection = n.MessageDirection.serverToClient, e.type = new n.ProtocolNotificationType(e.method)
            }(t.ShowMessageNotification || (t.ShowMessageNotification = {})), function (e) {
                e.method = "window/showMessageRequest", e.messageDirection = n.MessageDirection.serverToClient, e.type = new n.ProtocolRequestType(e.method)
            }(t.ShowMessageRequest || (t.ShowMessageRequest = {})), function (e) {
                e.method = "window/logMessage", e.messageDirection = n.MessageDirection.serverToClient, e.type = new n.ProtocolNotificationType(e.method)
            }(t.LogMessageNotification || (t.LogMessageNotification = {})), function (e) {
                e.method = "telemetry/event", e.messageDirection = n.MessageDirection.serverToClient, e.type = new n.ProtocolNotificationType(e.method)
            }(t.TelemetryEventNotification || (t.TelemetryEventNotification = {})), function (e) {
                e.None = 0, e.Full = 1, e.Incremental = 2
            }(t.TextDocumentSyncKind || (t.TextDocumentSyncKind = {})), function (e) {
                e.method = "textDocument/didOpen", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolNotificationType(e.method)
            }(t.DidOpenTextDocumentNotification || (t.DidOpenTextDocumentNotification = {})), function (e) {
                e.isIncremental = function (e) {
                    let t = e;
                    return null != t && "string" == typeof t.text && void 0 !== t.range && (void 0 === t.rangeLength || "number" == typeof t.rangeLength)
                }, e.isFull = function (e) {
                    let t = e;
                    return null != t && "string" == typeof t.text && void 0 === t.range && void 0 === t.rangeLength
                }
            }(t.TextDocumentContentChangeEvent || (t.TextDocumentContentChangeEvent = {})), function (e) {
                e.method = "textDocument/didChange", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolNotificationType(e.method)
            }(t.DidChangeTextDocumentNotification || (t.DidChangeTextDocumentNotification = {})), function (e) {
                e.method = "textDocument/didClose", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolNotificationType(e.method)
            }(t.DidCloseTextDocumentNotification || (t.DidCloseTextDocumentNotification = {})), function (e) {
                e.method = "textDocument/didSave", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolNotificationType(e.method)
            }(t.DidSaveTextDocumentNotification || (t.DidSaveTextDocumentNotification = {})), function (e) {
                e.Manual = 1, e.AfterDelay = 2, e.FocusOut = 3
            }(t.TextDocumentSaveReason || (t.TextDocumentSaveReason = {})), function (e) {
                e.method = "textDocument/willSave", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolNotificationType(e.method)
            }(t.WillSaveTextDocumentNotification || (t.WillSaveTextDocumentNotification = {})), function (e) {
                e.method = "textDocument/willSaveWaitUntil", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.WillSaveTextDocumentWaitUntilRequest || (t.WillSaveTextDocumentWaitUntilRequest = {})), function (e) {
                e.method = "workspace/didChangeWatchedFiles", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolNotificationType(e.method)
            }(t.DidChangeWatchedFilesNotification || (t.DidChangeWatchedFilesNotification = {})), function (e) {
                e.Created = 1, e.Changed = 2, e.Deleted = 3
            }(t.FileChangeType || (t.FileChangeType = {})), function (e) {
                e.is = function (e) {
                    const t = e;
                    return r.objectLiteral(t) && (o.URI.is(t.baseUri) || o.WorkspaceFolder.is(t.baseUri)) && r.string(t.pattern)
                }
            }(t.RelativePattern || (t.RelativePattern = {})), function (e) {
                e.Create = 1, e.Change = 2, e.Delete = 4
            }(t.WatchKind || (t.WatchKind = {})), function (e) {
                e.method = "textDocument/publishDiagnostics", e.messageDirection = n.MessageDirection.serverToClient, e.type = new n.ProtocolNotificationType(e.method)
            }(t.PublishDiagnosticsNotification || (t.PublishDiagnosticsNotification = {})), function (e) {
                e.Invoked = 1, e.TriggerCharacter = 2, e.TriggerForIncompleteCompletions = 3
            }(t.CompletionTriggerKind || (t.CompletionTriggerKind = {})), function (e) {
                e.method = "textDocument/completion", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.CompletionRequest || (t.CompletionRequest = {})), function (e) {
                e.method = "completionItem/resolve", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.CompletionResolveRequest || (t.CompletionResolveRequest = {})), function (e) {
                e.method = "textDocument/hover", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.HoverRequest || (t.HoverRequest = {})), function (e) {
                e.Invoked = 1, e.TriggerCharacter = 2, e.ContentChange = 3
            }(t.SignatureHelpTriggerKind || (t.SignatureHelpTriggerKind = {})), function (e) {
                e.method = "textDocument/signatureHelp", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.SignatureHelpRequest || (t.SignatureHelpRequest = {})), function (e) {
                e.method = "textDocument/definition", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.DefinitionRequest || (t.DefinitionRequest = {})), function (e) {
                e.method = "textDocument/references", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.ReferencesRequest || (t.ReferencesRequest = {})), function (e) {
                e.method = "textDocument/documentHighlight", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.DocumentHighlightRequest || (t.DocumentHighlightRequest = {})), function (e) {
                e.method = "textDocument/documentSymbol", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.DocumentSymbolRequest || (t.DocumentSymbolRequest = {})), function (e) {
                e.method = "textDocument/codeAction", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.CodeActionRequest || (t.CodeActionRequest = {})), function (e) {
                e.method = "codeAction/resolve", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.CodeActionResolveRequest || (t.CodeActionResolveRequest = {})), function (e) {
                e.method = "workspace/symbol", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.WorkspaceSymbolRequest || (t.WorkspaceSymbolRequest = {})), function (e) {
                e.method = "workspaceSymbol/resolve", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.WorkspaceSymbolResolveRequest || (t.WorkspaceSymbolResolveRequest = {})), function (e) {
                e.method = "textDocument/codeLens", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.CodeLensRequest || (t.CodeLensRequest = {})), function (e) {
                e.method = "codeLens/resolve", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.CodeLensResolveRequest || (t.CodeLensResolveRequest = {})), function (e) {
                e.method = "workspace/codeLens/refresh", e.messageDirection = n.MessageDirection.serverToClient, e.type = new n.ProtocolRequestType0(e.method)
            }(t.CodeLensRefreshRequest || (t.CodeLensRefreshRequest = {})), function (e) {
                e.method = "textDocument/documentLink", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.DocumentLinkRequest || (t.DocumentLinkRequest = {})), function (e) {
                e.method = "documentLink/resolve", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.DocumentLinkResolveRequest || (t.DocumentLinkResolveRequest = {})), function (e) {
                e.method = "textDocument/formatting", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.DocumentFormattingRequest || (t.DocumentFormattingRequest = {})), function (e) {
                e.method = "textDocument/rangeFormatting", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.DocumentRangeFormattingRequest || (t.DocumentRangeFormattingRequest = {})), function (e) {
                e.method = "textDocument/onTypeFormatting", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.DocumentOnTypeFormattingRequest || (t.DocumentOnTypeFormattingRequest = {})), function (e) {
                e.Identifier = 1
            }(t.PrepareSupportDefaultBehavior || (t.PrepareSupportDefaultBehavior = {})), function (e) {
                e.method = "textDocument/rename", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.RenameRequest || (t.RenameRequest = {})), function (e) {
                e.method = "textDocument/prepareRename", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.PrepareRenameRequest || (t.PrepareRenameRequest = {})), function (e) {
                e.method = "workspace/executeCommand", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.ExecuteCommandRequest || (t.ExecuteCommandRequest = {})), function (e) {
                e.method = "workspace/applyEdit", e.messageDirection = n.MessageDirection.serverToClient, e.type = new n.ProtocolRequestType("workspace/applyEdit")
            }(t.ApplyWorkspaceEditRequest || (t.ApplyWorkspaceEditRequest = {}))
        }, 757: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.LinkedEditingRangeRequest = void 0;
            const n = i(758);
            !function (e) {
                e.method = "textDocument/linkedEditingRange", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.LinkedEditingRangeRequest || (t.LinkedEditingRangeRequest = {}))
        }, 5203: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.MonikerRequest = t.MonikerKind = t.UniquenessLevel = void 0;
            const n = i(758);
            !function (e) {
                e.document = "document", e.project = "project", e.group = "group", e.scheme = "scheme", e.global = "global"
            }(t.UniquenessLevel || (t.UniquenessLevel = {})), function (e) {
                e.$import = "import", e.$export = "export", e.local = "local"
            }(t.MonikerKind || (t.MonikerKind = {})), function (e) {
                e.method = "textDocument/moniker", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.MonikerRequest || (t.MonikerRequest = {}))
        }, 57: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DidCloseNotebookDocumentNotification = t.DidSaveNotebookDocumentNotification = t.DidChangeNotebookDocumentNotification = t.NotebookCellArrayChange = t.DidOpenNotebookDocumentNotification = t.NotebookDocumentSyncRegistrationType = t.NotebookDocument = t.NotebookCell = t.ExecutionSummary = t.NotebookCellKind = void 0;
            const n = i(4843), o = i(340), r = i(758);
            var s, a, c, l;
            !function (e) {
                e.Markup = 1, e.Code = 2, e.is = function (e) {
                    return 1 === e || 2 === e
                }
            }(s = t.NotebookCellKind || (t.NotebookCellKind = {})), function (e) {
                e.create = function (e, t) {
                    const i = {executionOrder: e};
                    return !0 !== t && !1 !== t || (i.success = t), i
                }, e.is = function (e) {
                    const t = e;
                    return o.objectLiteral(t) && n.uinteger.is(t.executionOrder) && (void 0 === t.success || o.boolean(t.success))
                }, e.equals = function (e, t) {
                    return e === t || null != e && null != t && (e.executionOrder === t.executionOrder && e.success === t.success)
                }
            }(a = t.ExecutionSummary || (t.ExecutionSummary = {})), function (e) {
                function t(e, i) {
                    if (e === i) return !0;
                    if (null == e || null == i) return !1;
                    if (typeof e != typeof i) return !1;
                    if ("object" != typeof e) return !1;
                    const n = Array.isArray(e), r = Array.isArray(i);
                    if (n !== r) return !1;
                    if (n && r) {
                        if (e.length !== i.length) return !1;
                        for (let n = 0; n < e.length; n++) if (!t(e[n], i[n])) return !1
                    }
                    if (o.objectLiteral(e) && o.objectLiteral(i)) {
                        const n = Object.keys(e), o = Object.keys(i);
                        if (n.length !== o.length) return !1;
                        if (n.sort(), o.sort(), !t(n, o)) return !1;
                        for (let o = 0; o < n.length; o++) {
                            const r = n[o];
                            if (!t(e[r], i[r])) return !1
                        }
                    }
                    return !0
                }

                e.create = function (e, t) {
                    return {kind: e, document: t}
                }, e.is = function (e) {
                    const t = e;
                    return o.objectLiteral(t) && s.is(t.kind) && n.DocumentUri.is(t.document) && (void 0 === t.metadata || o.objectLiteral(t.metadata))
                }, e.diff = function (e, i) {
                    const n = new Set;
                    return e.document !== i.document && n.add("document"), e.kind !== i.kind && n.add("kind"), e.executionSummary !== i.executionSummary && n.add("executionSummary"), void 0 === e.metadata && void 0 === i.metadata || t(e.metadata, i.metadata) || n.add("metadata"), void 0 === e.executionSummary && void 0 === i.executionSummary || a.equals(e.executionSummary, i.executionSummary) || n.add("executionSummary"), n
                }
            }(c = t.NotebookCell || (t.NotebookCell = {})), function (e) {
                e.create = function (e, t, i, n) {
                    return {uri: e, notebookType: t, version: i, cells: n}
                }, e.is = function (e) {
                    const t = e;
                    return o.objectLiteral(t) && o.string(t.uri) && n.integer.is(t.version) && o.typedArray(t.cells, c.is)
                }
            }(t.NotebookDocument || (t.NotebookDocument = {})), function (e) {
                e.method = "notebookDocument/sync", e.messageDirection = r.MessageDirection.clientToServer, e.type = new r.RegistrationType(e.method)
            }(l = t.NotebookDocumentSyncRegistrationType || (t.NotebookDocumentSyncRegistrationType = {})), function (e) {
                e.method = "notebookDocument/didOpen", e.messageDirection = r.MessageDirection.clientToServer, e.type = new r.ProtocolNotificationType(e.method), e.registrationMethod = l.method
            }(t.DidOpenNotebookDocumentNotification || (t.DidOpenNotebookDocumentNotification = {})), function (e) {
                e.is = function (e) {
                    const t = e;
                    return o.objectLiteral(t) && n.uinteger.is(t.start) && n.uinteger.is(t.deleteCount) && (void 0 === t.cells || o.typedArray(t.cells, c.is))
                }, e.create = function (e, t, i) {
                    const n = {start: e, deleteCount: t};
                    return void 0 !== i && (n.cells = i), n
                }
            }(t.NotebookCellArrayChange || (t.NotebookCellArrayChange = {})), function (e) {
                e.method = "notebookDocument/didChange", e.messageDirection = r.MessageDirection.clientToServer, e.type = new r.ProtocolNotificationType(e.method), e.registrationMethod = l.method
            }(t.DidChangeNotebookDocumentNotification || (t.DidChangeNotebookDocumentNotification = {})), function (e) {
                e.method = "notebookDocument/didSave", e.messageDirection = r.MessageDirection.clientToServer, e.type = new r.ProtocolNotificationType(e.method), e.registrationMethod = l.method
            }(t.DidSaveNotebookDocumentNotification || (t.DidSaveNotebookDocumentNotification = {})), function (e) {
                e.method = "notebookDocument/didClose", e.messageDirection = r.MessageDirection.clientToServer, e.type = new r.ProtocolNotificationType(e.method), e.registrationMethod = l.method
            }(t.DidCloseNotebookDocumentNotification || (t.DidCloseNotebookDocumentNotification = {}))
        }, 591: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.WorkDoneProgressCancelNotification = t.WorkDoneProgressCreateRequest = t.WorkDoneProgress = void 0;
            const n = i(8862), o = i(758);
            !function (e) {
                e.type = new n.ProgressType, e.is = function (t) {
                    return t === e.type
                }
            }(t.WorkDoneProgress || (t.WorkDoneProgress = {})), function (e) {
                e.method = "window/workDoneProgress/create", e.messageDirection = o.MessageDirection.serverToClient, e.type = new o.ProtocolRequestType(e.method)
            }(t.WorkDoneProgressCreateRequest || (t.WorkDoneProgressCreateRequest = {})), function (e) {
                e.method = "window/workDoneProgress/cancel", e.messageDirection = o.MessageDirection.clientToServer, e.type = new o.ProtocolNotificationType(e.method)
            }(t.WorkDoneProgressCancelNotification || (t.WorkDoneProgressCancelNotification = {}))
        }, 3721: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.SelectionRangeRequest = void 0;
            const n = i(758);
            !function (e) {
                e.method = "textDocument/selectionRange", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.SelectionRangeRequest || (t.SelectionRangeRequest = {}))
        }, 4206: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.SemanticTokensRefreshRequest = t.SemanticTokensRangeRequest = t.SemanticTokensDeltaRequest = t.SemanticTokensRequest = t.SemanticTokensRegistrationType = t.TokenFormat = void 0;
            const n = i(758);
            var o;
            !function (e) {
                e.Relative = "relative"
            }(t.TokenFormat || (t.TokenFormat = {})), function (e) {
                e.method = "textDocument/semanticTokens", e.type = new n.RegistrationType(e.method)
            }(o = t.SemanticTokensRegistrationType || (t.SemanticTokensRegistrationType = {})), function (e) {
                e.method = "textDocument/semanticTokens/full", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method), e.registrationMethod = o.method
            }(t.SemanticTokensRequest || (t.SemanticTokensRequest = {})), function (e) {
                e.method = "textDocument/semanticTokens/full/delta", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method), e.registrationMethod = o.method
            }(t.SemanticTokensDeltaRequest || (t.SemanticTokensDeltaRequest = {})), function (e) {
                e.method = "textDocument/semanticTokens/range", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method), e.registrationMethod = o.method
            }(t.SemanticTokensRangeRequest || (t.SemanticTokensRangeRequest = {})), function (e) {
                e.method = "workspace/semanticTokens/refresh", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType0(e.method)
            }(t.SemanticTokensRefreshRequest || (t.SemanticTokensRefreshRequest = {}))
        }, 8069: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.ShowDocumentRequest = void 0;
            const n = i(758);
            !function (e) {
                e.method = "window/showDocument", e.messageDirection = n.MessageDirection.serverToClient, e.type = new n.ProtocolRequestType(e.method)
            }(t.ShowDocumentRequest || (t.ShowDocumentRequest = {}))
        }, 1145: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.TypeDefinitionRequest = void 0;
            const n = i(758);
            !function (e) {
                e.method = "textDocument/typeDefinition", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.TypeDefinitionRequest || (t.TypeDefinitionRequest = {}))
        }, 2e3: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.TypeHierarchySubtypesRequest = t.TypeHierarchySupertypesRequest = t.TypeHierarchyPrepareRequest = void 0;
            const n = i(758);
            !function (e) {
                e.method = "textDocument/prepareTypeHierarchy", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.TypeHierarchyPrepareRequest || (t.TypeHierarchyPrepareRequest = {})), function (e) {
                e.method = "typeHierarchy/supertypes", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.TypeHierarchySupertypesRequest || (t.TypeHierarchySupertypesRequest = {})), function (e) {
                e.method = "typeHierarchy/subtypes", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolRequestType(e.method)
            }(t.TypeHierarchySubtypesRequest || (t.TypeHierarchySubtypesRequest = {}))
        }, 4555: (e, t, i) => {
            "use strict";
            Object.defineProperty(t, "__esModule", {value: !0}), t.DidChangeWorkspaceFoldersNotification = t.WorkspaceFoldersRequest = void 0;
            const n = i(758);
            !function (e) {
                e.method = "workspace/workspaceFolders", e.messageDirection = n.MessageDirection.serverToClient, e.type = new n.ProtocolRequestType0(e.method)
            }(t.WorkspaceFoldersRequest || (t.WorkspaceFoldersRequest = {})), function (e) {
                e.method = "workspace/didChangeWorkspaceFolders", e.messageDirection = n.MessageDirection.clientToServer, e.type = new n.ProtocolNotificationType(e.method)
            }(t.DidChangeWorkspaceFoldersNotification || (t.DidChangeWorkspaceFoldersNotification = {}))
        }, 340: (e, t) => {
            "use strict";

            function i(e) {
                return "string" == typeof e || e instanceof String
            }

            function n(e) {
                return Array.isArray(e)
            }

            Object.defineProperty(t, "__esModule", {value: !0}), t.objectLiteral = t.typedArray = t.stringArray = t.array = t.func = t.error = t.number = t.string = t.boolean = void 0, t.boolean = function (e) {
                return !0 === e || !1 === e
            }, t.string = i, t.number = function (e) {
                return "number" == typeof e || e instanceof Number
            }, t.error = function (e) {
                return e instanceof Error
            }, t.func = function (e) {
                return "function" == typeof e
            }, t.array = n, t.stringArray = function (e) {
                return n(e) && e.every((e => i(e)))
            }, t.typedArray = function (e, t) {
                return Array.isArray(e) && e.every(t)
            }, t.objectLiteral = function (e) {
                return null !== e && "object" == typeof e
            }
        }, 3455: function (e, t, i) {
            "use strict";
            var n = this && this.__createBinding || (Object.create ? function (e, t, i, n) {
                void 0 === n && (n = i);
                var o = Object.getOwnPropertyDescriptor(t, i);
                o && !("get" in o ? !t.__esModule : o.writable || o.configurable) || (o = {
                    enumerable: !0,
                    get: function () {
                        return t[i]
                    }
                }), Object.defineProperty(e, n, o)
            } : function (e, t, i, n) {
                void 0 === n && (n = i), e[n] = t[i]
            }), o = this && this.__exportStar || function (e, t) {
                for (var i in e) "default" === i || Object.prototype.hasOwnProperty.call(t, i) || n(t, e, i)
            };
            Object.defineProperty(t, "__esModule", {value: !0}), t.createProtocolConnection = void 0;
            const r = i(304);
            o(i(304), t), o(i(8644), t), t.createProtocolConnection = function (e, t, i, n) {
                return (0, r.createMessageConnection)(e, t, i, n)
            }
        }, 3610: (e, t, i) => {
            "use strict";
            e.exports = i(3455)
        }, 4843: (e, t, i) => {
            "use strict";
            var n, o, r, s, a, c, l, u, d, p, h, g, m, f, v, y, C, w, b, k, S, D, P, T, R, _, x, E;
            i.r(t), i.d(t, {
                AnnotatedTextEdit: () => P,
                ChangeAnnotation: () => S,
                ChangeAnnotationIdentifier: () => D,
                CodeAction: () => re,
                CodeActionContext: () => oe,
                CodeActionKind: () => ie,
                CodeActionTriggerKind: () => ne,
                CodeDescription: () => C,
                CodeLens: () => se,
                Color: () => d,
                ColorInformation: () => p,
                ColorPresentation: () => h,
                Command: () => b,
                CompletionItem: () => W,
                CompletionItemKind: () => q,
                CompletionItemLabelDetails: () => H,
                CompletionItemTag: () => A,
                CompletionList: () => V,
                CreateFile: () => R,
                DeleteFile: () => x,
                Diagnostic: () => w,
                DiagnosticRelatedInformation: () => f,
                DiagnosticSeverity: () => v,
                DiagnosticTag: () => y,
                DocumentHighlight: () => X,
                DocumentHighlightKind: () => G,
                DocumentLink: () => ce,
                DocumentSymbol: () => te,
                DocumentUri: () => n,
                EOL: () => Pe,
                FoldingRange: () => m,
                FoldingRangeKind: () => g,
                FormattingOptions: () => ae,
                Hover: () => B,
                InlayHint: () => Ce,
                InlayHintKind: () => ve,
                InlayHintLabelPart: () => ye,
                InlineValueContext: () => fe,
                InlineValueEvaluatableExpression: () => me,
                InlineValueText: () => he,
                InlineValueVariableLookup: () => ge,
                InsertReplaceEdit: () => $,
                InsertTextFormat: () => L,
                InsertTextMode: () => U,
                Location: () => l,
                LocationLink: () => u,
                MarkedString: () => K,
                MarkupContent: () => N,
                MarkupKind: () => F,
                OptionalVersionedTextDocumentIdentifier: () => M,
                ParameterInformation: () => J,
                Position: () => a,
                Range: () => c,
                RenameFile: () => _,
                SelectionRange: () => le,
                SemanticTokenModifiers: () => de,
                SemanticTokenTypes: () => ue,
                SemanticTokens: () => pe,
                SignatureInformation: () => z,
                SymbolInformation: () => Z,
                SymbolKind: () => Y,
                SymbolTag: () => Q,
                TextDocument: () => De,
                TextDocumentEdit: () => T,
                TextDocumentIdentifier: () => O,
                TextDocumentItem: () => I,
                TextEdit: () => k,
                URI: () => o,
                VersionedTextDocumentIdentifier: () => j,
                WorkspaceChange: () => Se,
                WorkspaceEdit: () => E,
                WorkspaceFolder: () => we,
                WorkspaceSymbol: () => ee,
                integer: () => r,
                uinteger: () => s
            }), function (e) {
                e.is = function (e) {
                    return "string" == typeof e
                }
            }(n || (n = {})), function (e) {
                e.is = function (e) {
                    return "string" == typeof e
                }
            }(o || (o = {})), function (e) {
                e.MIN_VALUE = -2147483648, e.MAX_VALUE = 2147483647, e.is = function (t) {
                    return "number" == typeof t && e.MIN_VALUE <= t && t <= e.MAX_VALUE
                }
            }(r || (r = {})), function (e) {
                e.MIN_VALUE = 0, e.MAX_VALUE = 2147483647, e.is = function (t) {
                    return "number" == typeof t && e.MIN_VALUE <= t && t <= e.MAX_VALUE
                }
            }(s || (s = {})), function (e) {
                e.create = function (e, t) {
                    return e === Number.MAX_VALUE && (e = s.MAX_VALUE), t === Number.MAX_VALUE && (t = s.MAX_VALUE), {
                        line: e,
                        character: t
                    }
                }, e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(t) && Te.uinteger(t.line) && Te.uinteger(t.character)
                }
            }(a || (a = {})), function (e) {
                e.create = function (e, t, i, n) {
                    if (Te.uinteger(e) && Te.uinteger(t) && Te.uinteger(i) && Te.uinteger(n)) return {
                        start: a.create(e, t),
                        end: a.create(i, n)
                    };
                    if (a.is(e) && a.is(t)) return {start: e, end: t};
                    throw new Error("Range#create called with invalid arguments[".concat(e, ", ").concat(t, ", ").concat(i, ", ").concat(n, "]"))
                }, e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(t) && a.is(t.start) && a.is(t.end)
                }
            }(c || (c = {})), function (e) {
                e.create = function (e, t) {
                    return {uri: e, range: t}
                }, e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(t) && c.is(t.range) && (Te.string(t.uri) || Te.undefined(t.uri))
                }
            }(l || (l = {})), function (e) {
                e.create = function (e, t, i, n) {
                    return {targetUri: e, targetRange: t, targetSelectionRange: i, originSelectionRange: n}
                }, e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(t) && c.is(t.targetRange) && Te.string(t.targetUri) && c.is(t.targetSelectionRange) && (c.is(t.originSelectionRange) || Te.undefined(t.originSelectionRange))
                }
            }(u || (u = {})), function (e) {
                e.create = function (e, t, i, n) {
                    return {red: e, green: t, blue: i, alpha: n}
                }, e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(t) && Te.numberRange(t.red, 0, 1) && Te.numberRange(t.green, 0, 1) && Te.numberRange(t.blue, 0, 1) && Te.numberRange(t.alpha, 0, 1)
                }
            }(d || (d = {})), function (e) {
                e.create = function (e, t) {
                    return {range: e, color: t}
                }, e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(t) && c.is(t.range) && d.is(t.color)
                }
            }(p || (p = {})), function (e) {
                e.create = function (e, t, i) {
                    return {label: e, textEdit: t, additionalTextEdits: i}
                }, e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(t) && Te.string(t.label) && (Te.undefined(t.textEdit) || k.is(t)) && (Te.undefined(t.additionalTextEdits) || Te.typedArray(t.additionalTextEdits, k.is))
                }
            }(h || (h = {})), function (e) {
                e.Comment = "comment", e.Imports = "imports", e.Region = "region"
            }(g || (g = {})), function (e) {
                e.create = function (e, t, i, n, o, r) {
                    var s = {startLine: e, endLine: t};
                    return Te.defined(i) && (s.startCharacter = i), Te.defined(n) && (s.endCharacter = n), Te.defined(o) && (s.kind = o), Te.defined(r) && (s.collapsedText = r), s
                }, e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(t) && Te.uinteger(t.startLine) && Te.uinteger(t.startLine) && (Te.undefined(t.startCharacter) || Te.uinteger(t.startCharacter)) && (Te.undefined(t.endCharacter) || Te.uinteger(t.endCharacter)) && (Te.undefined(t.kind) || Te.string(t.kind))
                }
            }(m || (m = {})), function (e) {
                e.create = function (e, t) {
                    return {location: e, message: t}
                }, e.is = function (e) {
                    var t = e;
                    return Te.defined(t) && l.is(t.location) && Te.string(t.message)
                }
            }(f || (f = {})), function (e) {
                e.Error = 1, e.Warning = 2, e.Information = 3, e.Hint = 4
            }(v || (v = {})), function (e) {
                e.Unnecessary = 1, e.Deprecated = 2
            }(y || (y = {})), function (e) {
                e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(t) && Te.string(t.href)
                }
            }(C || (C = {})), function (e) {
                e.create = function (e, t, i, n, o, r) {
                    var s = {range: e, message: t};
                    return Te.defined(i) && (s.severity = i), Te.defined(n) && (s.code = n), Te.defined(o) && (s.source = o), Te.defined(r) && (s.relatedInformation = r), s
                }, e.is = function (e) {
                    var t, i = e;
                    return Te.defined(i) && c.is(i.range) && Te.string(i.message) && (Te.number(i.severity) || Te.undefined(i.severity)) && (Te.integer(i.code) || Te.string(i.code) || Te.undefined(i.code)) && (Te.undefined(i.codeDescription) || Te.string(null === (t = i.codeDescription) || void 0 === t ? void 0 : t.href)) && (Te.string(i.source) || Te.undefined(i.source)) && (Te.undefined(i.relatedInformation) || Te.typedArray(i.relatedInformation, f.is))
                }
            }(w || (w = {})), function (e) {
                e.create = function (e, t) {
                    for (var i = [], n = 2; n < arguments.length; n++) i[n - 2] = arguments[n];
                    var o = {title: e, command: t};
                    return Te.defined(i) && i.length > 0 && (o.arguments = i), o
                }, e.is = function (e) {
                    var t = e;
                    return Te.defined(t) && Te.string(t.title) && Te.string(t.command)
                }
            }(b || (b = {})), function (e) {
                e.replace = function (e, t) {
                    return {range: e, newText: t}
                }, e.insert = function (e, t) {
                    return {range: {start: e, end: e}, newText: t}
                }, e.del = function (e) {
                    return {range: e, newText: ""}
                }, e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(t) && Te.string(t.newText) && c.is(t.range)
                }
            }(k || (k = {})), function (e) {
                e.create = function (e, t, i) {
                    var n = {label: e};
                    return void 0 !== t && (n.needsConfirmation = t), void 0 !== i && (n.description = i), n
                }, e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(t) && Te.string(t.label) && (Te.boolean(t.needsConfirmation) || void 0 === t.needsConfirmation) && (Te.string(t.description) || void 0 === t.description)
                }
            }(S || (S = {})), function (e) {
                e.is = function (e) {
                    var t = e;
                    return Te.string(t)
                }
            }(D || (D = {})), function (e) {
                e.replace = function (e, t, i) {
                    return {range: e, newText: t, annotationId: i}
                }, e.insert = function (e, t, i) {
                    return {range: {start: e, end: e}, newText: t, annotationId: i}
                }, e.del = function (e, t) {
                    return {range: e, newText: "", annotationId: t}
                }, e.is = function (e) {
                    var t = e;
                    return k.is(t) && (S.is(t.annotationId) || D.is(t.annotationId))
                }
            }(P || (P = {})), function (e) {
                e.create = function (e, t) {
                    return {textDocument: e, edits: t}
                }, e.is = function (e) {
                    var t = e;
                    return Te.defined(t) && M.is(t.textDocument) && Array.isArray(t.edits)
                }
            }(T || (T = {})), function (e) {
                e.create = function (e, t, i) {
                    var n = {kind: "create", uri: e};
                    return void 0 === t || void 0 === t.overwrite && void 0 === t.ignoreIfExists || (n.options = t), void 0 !== i && (n.annotationId = i), n
                }, e.is = function (e) {
                    var t = e;
                    return t && "create" === t.kind && Te.string(t.uri) && (void 0 === t.options || (void 0 === t.options.overwrite || Te.boolean(t.options.overwrite)) && (void 0 === t.options.ignoreIfExists || Te.boolean(t.options.ignoreIfExists))) && (void 0 === t.annotationId || D.is(t.annotationId))
                }
            }(R || (R = {})), function (e) {
                e.create = function (e, t, i, n) {
                    var o = {kind: "rename", oldUri: e, newUri: t};
                    return void 0 === i || void 0 === i.overwrite && void 0 === i.ignoreIfExists || (o.options = i), void 0 !== n && (o.annotationId = n), o
                }, e.is = function (e) {
                    var t = e;
                    return t && "rename" === t.kind && Te.string(t.oldUri) && Te.string(t.newUri) && (void 0 === t.options || (void 0 === t.options.overwrite || Te.boolean(t.options.overwrite)) && (void 0 === t.options.ignoreIfExists || Te.boolean(t.options.ignoreIfExists))) && (void 0 === t.annotationId || D.is(t.annotationId))
                }
            }(_ || (_ = {})), function (e) {
                e.create = function (e, t, i) {
                    var n = {kind: "delete", uri: e};
                    return void 0 === t || void 0 === t.recursive && void 0 === t.ignoreIfNotExists || (n.options = t), void 0 !== i && (n.annotationId = i), n
                }, e.is = function (e) {
                    var t = e;
                    return t && "delete" === t.kind && Te.string(t.uri) && (void 0 === t.options || (void 0 === t.options.recursive || Te.boolean(t.options.recursive)) && (void 0 === t.options.ignoreIfNotExists || Te.boolean(t.options.ignoreIfNotExists))) && (void 0 === t.annotationId || D.is(t.annotationId))
                }
            }(x || (x = {})), function (e) {
                e.is = function (e) {
                    var t = e;
                    return t && (void 0 !== t.changes || void 0 !== t.documentChanges) && (void 0 === t.documentChanges || t.documentChanges.every((function (e) {
                        return Te.string(e.kind) ? R.is(e) || _.is(e) || x.is(e) : T.is(e)
                    })))
                }
            }(E || (E = {}));
            var O, j, M, I, F, N, q, L, A, $, U, H, W, V, K, B, J, z, G, X, Y, Q, Z, ee, te, ie, ne, oe, re, se, ae, ce,
                le, ue, de, pe, he, ge, me, fe, ve, ye, Ce, we, be = function () {
                    function e(e, t) {
                        this.edits = e, this.changeAnnotations = t
                    }

                    return e.prototype.insert = function (e, t, i) {
                        var n, o;
                        if (void 0 === i ? n = k.insert(e, t) : D.is(i) ? (o = i, n = P.insert(e, t, i)) : (this.assertChangeAnnotations(this.changeAnnotations), o = this.changeAnnotations.manage(i), n = P.insert(e, t, o)), this.edits.push(n), void 0 !== o) return o
                    }, e.prototype.replace = function (e, t, i) {
                        var n, o;
                        if (void 0 === i ? n = k.replace(e, t) : D.is(i) ? (o = i, n = P.replace(e, t, i)) : (this.assertChangeAnnotations(this.changeAnnotations), o = this.changeAnnotations.manage(i), n = P.replace(e, t, o)), this.edits.push(n), void 0 !== o) return o
                    }, e.prototype.delete = function (e, t) {
                        var i, n;
                        if (void 0 === t ? i = k.del(e) : D.is(t) ? (n = t, i = P.del(e, t)) : (this.assertChangeAnnotations(this.changeAnnotations), n = this.changeAnnotations.manage(t), i = P.del(e, n)), this.edits.push(i), void 0 !== n) return n
                    }, e.prototype.add = function (e) {
                        this.edits.push(e)
                    }, e.prototype.all = function () {
                        return this.edits
                    }, e.prototype.clear = function () {
                        this.edits.splice(0, this.edits.length)
                    }, e.prototype.assertChangeAnnotations = function (e) {
                        if (void 0 === e) throw new Error("Text edit change is not configured to manage change annotations.")
                    }, e
                }(), ke = function () {
                    function e(e) {
                        this._annotations = void 0 === e ? Object.create(null) : e, this._counter = 0, this._size = 0
                    }

                    return e.prototype.all = function () {
                        return this._annotations
                    }, Object.defineProperty(e.prototype, "size", {
                        get: function () {
                            return this._size
                        }, enumerable: !1, configurable: !0
                    }), e.prototype.manage = function (e, t) {
                        var i;
                        if (D.is(e) ? i = e : (i = this.nextId(), t = e), void 0 !== this._annotations[i]) throw new Error("Id ".concat(i, " is already in use."));
                        if (void 0 === t) throw new Error("No annotation provided for id ".concat(i));
                        return this._annotations[i] = t, this._size++, i
                    }, e.prototype.nextId = function () {
                        return this._counter++, this._counter.toString()
                    }, e
                }(), Se = function () {
                    function e(e) {
                        var t = this;
                        this._textEditChanges = Object.create(null), void 0 !== e ? (this._workspaceEdit = e, e.documentChanges ? (this._changeAnnotations = new ke(e.changeAnnotations), e.changeAnnotations = this._changeAnnotations.all(), e.documentChanges.forEach((function (e) {
                            if (T.is(e)) {
                                var i = new be(e.edits, t._changeAnnotations);
                                t._textEditChanges[e.textDocument.uri] = i
                            }
                        }))) : e.changes && Object.keys(e.changes).forEach((function (i) {
                            var n = new be(e.changes[i]);
                            t._textEditChanges[i] = n
                        }))) : this._workspaceEdit = {}
                    }

                    return Object.defineProperty(e.prototype, "edit", {
                        get: function () {
                            return this.initDocumentChanges(), void 0 !== this._changeAnnotations && (0 === this._changeAnnotations.size ? this._workspaceEdit.changeAnnotations = void 0 : this._workspaceEdit.changeAnnotations = this._changeAnnotations.all()), this._workspaceEdit
                        }, enumerable: !1, configurable: !0
                    }), e.prototype.getTextEditChange = function (e) {
                        if (M.is(e)) {
                            if (this.initDocumentChanges(), void 0 === this._workspaceEdit.documentChanges) throw new Error("Workspace edit is not configured for document changes.");
                            var t = {uri: e.uri, version: e.version};
                            if (!(n = this._textEditChanges[t.uri])) {
                                var i = {textDocument: t, edits: o = []};
                                this._workspaceEdit.documentChanges.push(i), n = new be(o, this._changeAnnotations), this._textEditChanges[t.uri] = n
                            }
                            return n
                        }
                        if (this.initChanges(), void 0 === this._workspaceEdit.changes) throw new Error("Workspace edit is not configured for normal text edit changes.");
                        var n;
                        if (!(n = this._textEditChanges[e])) {
                            var o = [];
                            this._workspaceEdit.changes[e] = o, n = new be(o), this._textEditChanges[e] = n
                        }
                        return n
                    }, e.prototype.initDocumentChanges = function () {
                        void 0 === this._workspaceEdit.documentChanges && void 0 === this._workspaceEdit.changes && (this._changeAnnotations = new ke, this._workspaceEdit.documentChanges = [], this._workspaceEdit.changeAnnotations = this._changeAnnotations.all())
                    }, e.prototype.initChanges = function () {
                        void 0 === this._workspaceEdit.documentChanges && void 0 === this._workspaceEdit.changes && (this._workspaceEdit.changes = Object.create(null))
                    }, e.prototype.createFile = function (e, t, i) {
                        if (this.initDocumentChanges(), void 0 === this._workspaceEdit.documentChanges) throw new Error("Workspace edit is not configured for document changes.");
                        var n, o, r;
                        if (S.is(t) || D.is(t) ? n = t : i = t, void 0 === n ? o = R.create(e, i) : (r = D.is(n) ? n : this._changeAnnotations.manage(n), o = R.create(e, i, r)), this._workspaceEdit.documentChanges.push(o), void 0 !== r) return r
                    }, e.prototype.renameFile = function (e, t, i, n) {
                        if (this.initDocumentChanges(), void 0 === this._workspaceEdit.documentChanges) throw new Error("Workspace edit is not configured for document changes.");
                        var o, r, s;
                        if (S.is(i) || D.is(i) ? o = i : n = i, void 0 === o ? r = _.create(e, t, n) : (s = D.is(o) ? o : this._changeAnnotations.manage(o), r = _.create(e, t, n, s)), this._workspaceEdit.documentChanges.push(r), void 0 !== s) return s
                    }, e.prototype.deleteFile = function (e, t, i) {
                        if (this.initDocumentChanges(), void 0 === this._workspaceEdit.documentChanges) throw new Error("Workspace edit is not configured for document changes.");
                        var n, o, r;
                        if (S.is(t) || D.is(t) ? n = t : i = t, void 0 === n ? o = x.create(e, i) : (r = D.is(n) ? n : this._changeAnnotations.manage(n), o = x.create(e, i, r)), this._workspaceEdit.documentChanges.push(o), void 0 !== r) return r
                    }, e
                }();
            !function (e) {
                e.create = function (e) {
                    return {uri: e}
                }, e.is = function (e) {
                    var t = e;
                    return Te.defined(t) && Te.string(t.uri)
                }
            }(O || (O = {})), function (e) {
                e.create = function (e, t) {
                    return {uri: e, version: t}
                }, e.is = function (e) {
                    var t = e;
                    return Te.defined(t) && Te.string(t.uri) && Te.integer(t.version)
                }
            }(j || (j = {})), function (e) {
                e.create = function (e, t) {
                    return {uri: e, version: t}
                }, e.is = function (e) {
                    var t = e;
                    return Te.defined(t) && Te.string(t.uri) && (null === t.version || Te.integer(t.version))
                }
            }(M || (M = {})), function (e) {
                e.create = function (e, t, i, n) {
                    return {uri: e, languageId: t, version: i, text: n}
                }, e.is = function (e) {
                    var t = e;
                    return Te.defined(t) && Te.string(t.uri) && Te.string(t.languageId) && Te.integer(t.version) && Te.string(t.text)
                }
            }(I || (I = {})), function (e) {
                e.PlainText = "plaintext", e.Markdown = "markdown", e.is = function (t) {
                    var i = t;
                    return i === e.PlainText || i === e.Markdown
                }
            }(F || (F = {})), function (e) {
                e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(e) && F.is(t.kind) && Te.string(t.value)
                }
            }(N || (N = {})), function (e) {
                e.Text = 1, e.Method = 2, e.Function = 3, e.Constructor = 4, e.Field = 5, e.Variable = 6, e.Class = 7, e.Interface = 8, e.Module = 9, e.Property = 10, e.Unit = 11, e.Value = 12, e.Enum = 13, e.Keyword = 14, e.Snippet = 15, e.Color = 16, e.File = 17, e.Reference = 18, e.Folder = 19, e.EnumMember = 20, e.Constant = 21, e.Struct = 22, e.Event = 23, e.Operator = 24, e.TypeParameter = 25
            }(q || (q = {})), function (e) {
                e.PlainText = 1, e.Snippet = 2
            }(L || (L = {})), function (e) {
                e.Deprecated = 1
            }(A || (A = {})), function (e) {
                e.create = function (e, t, i) {
                    return {newText: e, insert: t, replace: i}
                }, e.is = function (e) {
                    var t = e;
                    return t && Te.string(t.newText) && c.is(t.insert) && c.is(t.replace)
                }
            }($ || ($ = {})), function (e) {
                e.asIs = 1, e.adjustIndentation = 2
            }(U || (U = {})), function (e) {
                e.is = function (e) {
                    var t = e;
                    return t && (Te.string(t.detail) || void 0 === t.detail) && (Te.string(t.description) || void 0 === t.description)
                }
            }(H || (H = {})), function (e) {
                e.create = function (e) {
                    return {label: e}
                }
            }(W || (W = {})), function (e) {
                e.create = function (e, t) {
                    return {items: e || [], isIncomplete: !!t}
                }
            }(V || (V = {})), function (e) {
                e.fromPlainText = function (e) {
                    return e.replace(/[\\`*_{}[\]()#+\-.!]/g, "\\$&")
                }, e.is = function (e) {
                    var t = e;
                    return Te.string(t) || Te.objectLiteral(t) && Te.string(t.language) && Te.string(t.value)
                }
            }(K || (K = {})), function (e) {
                e.is = function (e) {
                    var t = e;
                    return !!t && Te.objectLiteral(t) && (N.is(t.contents) || K.is(t.contents) || Te.typedArray(t.contents, K.is)) && (void 0 === e.range || c.is(e.range))
                }
            }(B || (B = {})), function (e) {
                e.create = function (e, t) {
                    return t ? {label: e, documentation: t} : {label: e}
                }
            }(J || (J = {})), function (e) {
                e.create = function (e, t) {
                    for (var i = [], n = 2; n < arguments.length; n++) i[n - 2] = arguments[n];
                    var o = {label: e};
                    return Te.defined(t) && (o.documentation = t), Te.defined(i) ? o.parameters = i : o.parameters = [], o
                }
            }(z || (z = {})), function (e) {
                e.Text = 1, e.Read = 2, e.Write = 3
            }(G || (G = {})), function (e) {
                e.create = function (e, t) {
                    var i = {range: e};
                    return Te.number(t) && (i.kind = t), i
                }
            }(X || (X = {})), function (e) {
                e.File = 1, e.Module = 2, e.Namespace = 3, e.Package = 4, e.Class = 5, e.Method = 6, e.Property = 7, e.Field = 8, e.Constructor = 9, e.Enum = 10, e.Interface = 11, e.Function = 12, e.Variable = 13, e.Constant = 14, e.String = 15, e.Number = 16, e.Boolean = 17, e.Array = 18, e.Object = 19, e.Key = 20, e.Null = 21, e.EnumMember = 22, e.Struct = 23, e.Event = 24, e.Operator = 25, e.TypeParameter = 26
            }(Y || (Y = {})), function (e) {
                e.Deprecated = 1
            }(Q || (Q = {})), function (e) {
                e.create = function (e, t, i, n, o) {
                    var r = {name: e, kind: t, location: {uri: n, range: i}};
                    return o && (r.containerName = o), r
                }
            }(Z || (Z = {})), function (e) {
                e.create = function (e, t, i, n) {
                    return void 0 !== n ? {name: e, kind: t, location: {uri: i, range: n}} : {
                        name: e,
                        kind: t,
                        location: {uri: i}
                    }
                }
            }(ee || (ee = {})), function (e) {
                e.create = function (e, t, i, n, o, r) {
                    var s = {name: e, detail: t, kind: i, range: n, selectionRange: o};
                    return void 0 !== r && (s.children = r), s
                }, e.is = function (e) {
                    var t = e;
                    return t && Te.string(t.name) && Te.number(t.kind) && c.is(t.range) && c.is(t.selectionRange) && (void 0 === t.detail || Te.string(t.detail)) && (void 0 === t.deprecated || Te.boolean(t.deprecated)) && (void 0 === t.children || Array.isArray(t.children)) && (void 0 === t.tags || Array.isArray(t.tags))
                }
            }(te || (te = {})), function (e) {
                e.Empty = "", e.QuickFix = "quickfix", e.Refactor = "refactor", e.RefactorExtract = "refactor.extract", e.RefactorInline = "refactor.inline", e.RefactorRewrite = "refactor.rewrite", e.Source = "source", e.SourceOrganizeImports = "source.organizeImports", e.SourceFixAll = "source.fixAll"
            }(ie || (ie = {})), function (e) {
                e.Invoked = 1, e.Automatic = 2
            }(ne || (ne = {})), function (e) {
                e.create = function (e, t, i) {
                    var n = {diagnostics: e};
                    return null != t && (n.only = t), null != i && (n.triggerKind = i), n
                }, e.is = function (e) {
                    var t = e;
                    return Te.defined(t) && Te.typedArray(t.diagnostics, w.is) && (void 0 === t.only || Te.typedArray(t.only, Te.string)) && (void 0 === t.triggerKind || t.triggerKind === ne.Invoked || t.triggerKind === ne.Automatic)
                }
            }(oe || (oe = {})), function (e) {
                e.create = function (e, t, i) {
                    var n = {title: e}, o = !0;
                    return "string" == typeof t ? (o = !1, n.kind = t) : b.is(t) ? n.command = t : n.edit = t, o && void 0 !== i && (n.kind = i), n
                }, e.is = function (e) {
                    var t = e;
                    return t && Te.string(t.title) && (void 0 === t.diagnostics || Te.typedArray(t.diagnostics, w.is)) && (void 0 === t.kind || Te.string(t.kind)) && (void 0 !== t.edit || void 0 !== t.command) && (void 0 === t.command || b.is(t.command)) && (void 0 === t.isPreferred || Te.boolean(t.isPreferred)) && (void 0 === t.edit || E.is(t.edit))
                }
            }(re || (re = {})), function (e) {
                e.create = function (e, t) {
                    var i = {range: e};
                    return Te.defined(t) && (i.data = t), i
                }, e.is = function (e) {
                    var t = e;
                    return Te.defined(t) && c.is(t.range) && (Te.undefined(t.command) || b.is(t.command))
                }
            }(se || (se = {})), function (e) {
                e.create = function (e, t) {
                    return {tabSize: e, insertSpaces: t}
                }, e.is = function (e) {
                    var t = e;
                    return Te.defined(t) && Te.uinteger(t.tabSize) && Te.boolean(t.insertSpaces)
                }
            }(ae || (ae = {})), function (e) {
                e.create = function (e, t, i) {
                    return {range: e, target: t, data: i}
                }, e.is = function (e) {
                    var t = e;
                    return Te.defined(t) && c.is(t.range) && (Te.undefined(t.target) || Te.string(t.target))
                }
            }(ce || (ce = {})), function (e) {
                e.create = function (e, t) {
                    return {range: e, parent: t}
                }, e.is = function (t) {
                    var i = t;
                    return Te.objectLiteral(i) && c.is(i.range) && (void 0 === i.parent || e.is(i.parent))
                }
            }(le || (le = {})), function (e) {
                e.namespace = "namespace", e.type = "type", e.class = "class", e.enum = "enum", e.interface = "interface", e.struct = "struct", e.typeParameter = "typeParameter", e.parameter = "parameter", e.variable = "variable", e.property = "property", e.enumMember = "enumMember", e.event = "event", e.function = "function", e.method = "method", e.macro = "macro", e.keyword = "keyword", e.modifier = "modifier", e.comment = "comment", e.string = "string", e.number = "number", e.regexp = "regexp", e.operator = "operator", e.decorator = "decorator"
            }(ue || (ue = {})), function (e) {
                e.declaration = "declaration", e.definition = "definition", e.readonly = "readonly", e.static = "static", e.deprecated = "deprecated", e.abstract = "abstract", e.async = "async", e.modification = "modification", e.documentation = "documentation", e.defaultLibrary = "defaultLibrary"
            }(de || (de = {})), function (e) {
                e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(t) && (void 0 === t.resultId || "string" == typeof t.resultId) && Array.isArray(t.data) && (0 === t.data.length || "number" == typeof t.data[0])
                }
            }(pe || (pe = {})), function (e) {
                e.create = function (e, t) {
                    return {range: e, text: t}
                }, e.is = function (e) {
                    var t = e;
                    return null != t && c.is(t.range) && Te.string(t.text)
                }
            }(he || (he = {})), function (e) {
                e.create = function (e, t, i) {
                    return {range: e, variableName: t, caseSensitiveLookup: i}
                }, e.is = function (e) {
                    var t = e;
                    return null != t && c.is(t.range) && Te.boolean(t.caseSensitiveLookup) && (Te.string(t.variableName) || void 0 === t.variableName)
                }
            }(ge || (ge = {})), function (e) {
                e.create = function (e, t) {
                    return {range: e, expression: t}
                }, e.is = function (e) {
                    var t = e;
                    return null != t && c.is(t.range) && (Te.string(t.expression) || void 0 === t.expression)
                }
            }(me || (me = {})), function (e) {
                e.create = function (e, t) {
                    return {frameId: e, stoppedLocation: t}
                }, e.is = function (e) {
                    var t = e;
                    return Te.defined(t) && c.is(e.stoppedLocation)
                }
            }(fe || (fe = {})), function (e) {
                e.Type = 1, e.Parameter = 2, e.is = function (e) {
                    return 1 === e || 2 === e
                }
            }(ve || (ve = {})), function (e) {
                e.create = function (e) {
                    return {value: e}
                }, e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(t) && (void 0 === t.tooltip || Te.string(t.tooltip) || N.is(t.tooltip)) && (void 0 === t.location || l.is(t.location)) && (void 0 === t.command || b.is(t.command))
                }
            }(ye || (ye = {})), function (e) {
                e.create = function (e, t, i) {
                    var n = {position: e, label: t};
                    return void 0 !== i && (n.kind = i), n
                }, e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(t) && a.is(t.position) && (Te.string(t.label) || Te.typedArray(t.label, ye.is)) && (void 0 === t.kind || ve.is(t.kind)) && void 0 === t.textEdits || Te.typedArray(t.textEdits, k.is) && (void 0 === t.tooltip || Te.string(t.tooltip) || N.is(t.tooltip)) && (void 0 === t.paddingLeft || Te.boolean(t.paddingLeft)) && (void 0 === t.paddingRight || Te.boolean(t.paddingRight))
                }
            }(Ce || (Ce = {})), function (e) {
                e.is = function (e) {
                    var t = e;
                    return Te.objectLiteral(t) && o.is(t.uri) && Te.string(t.name)
                }
            }(we || (we = {}));
            var De, Pe = ["\n", "\r\n", "\r"];
            !function (e) {
                function t(e, i) {
                    if (e.length <= 1) return e;
                    var n = e.length / 2 | 0, o = e.slice(0, n), r = e.slice(n);
                    t(o, i), t(r, i);
                    for (var s = 0, a = 0, c = 0; s < o.length && a < r.length;) {
                        var l = i(o[s], r[a]);
                        e[c++] = l <= 0 ? o[s++] : r[a++]
                    }
                    for (; s < o.length;) e[c++] = o[s++];
                    for (; a < r.length;) e[c++] = r[a++];
                    return e
                }

                e.create = function (e, t, i, n) {
                    return new Re(e, t, i, n)
                }, e.is = function (e) {
                    var t = e;
                    return !!(Te.defined(t) && Te.string(t.uri) && (Te.undefined(t.languageId) || Te.string(t.languageId)) && Te.uinteger(t.lineCount) && Te.func(t.getText) && Te.func(t.positionAt) && Te.func(t.offsetAt))
                }, e.applyEdits = function (e, i) {
                    for (var n = e.getText(), o = t(i, (function (e, t) {
                        var i = e.range.start.line - t.range.start.line;
                        return 0 === i ? e.range.start.character - t.range.start.character : i
                    })), r = n.length, s = o.length - 1; s >= 0; s--) {
                        var a = o[s], c = e.offsetAt(a.range.start), l = e.offsetAt(a.range.end);
                        if (!(l <= r)) throw new Error("Overlapping edit");
                        n = n.substring(0, c) + a.newText + n.substring(l, n.length), r = c
                    }
                    return n
                }
            }(De || (De = {}));
            var Te, Re = function () {
                function e(e, t, i, n) {
                    this._uri = e, this._languageId = t, this._version = i, this._content = n, this._lineOffsets = void 0
                }

                return Object.defineProperty(e.prototype, "uri", {
                    get: function () {
                        return this._uri
                    }, enumerable: !1, configurable: !0
                }), Object.defineProperty(e.prototype, "languageId", {
                    get: function () {
                        return this._languageId
                    }, enumerable: !1, configurable: !0
                }), Object.defineProperty(e.prototype, "version", {
                    get: function () {
                        return this._version
                    }, enumerable: !1, configurable: !0
                }), e.prototype.getText = function (e) {
                    if (e) {
                        var t = this.offsetAt(e.start), i = this.offsetAt(e.end);
                        return this._content.substring(t, i)
                    }
                    return this._content
                }, e.prototype.update = function (e, t) {
                    this._content = e.text, this._version = t, this._lineOffsets = void 0
                }, e.prototype.getLineOffsets = function () {
                    if (void 0 === this._lineOffsets) {
                        for (var e = [], t = this._content, i = !0, n = 0; n < t.length; n++) {
                            i && (e.push(n), i = !1);
                            var o = t.charAt(n);
                            i = "\r" === o || "\n" === o, "\r" === o && n + 1 < t.length && "\n" === t.charAt(n + 1) && n++
                        }
                        i && t.length > 0 && e.push(t.length), this._lineOffsets = e
                    }
                    return this._lineOffsets
                }, e.prototype.positionAt = function (e) {
                    e = Math.max(Math.min(e, this._content.length), 0);
                    var t = this.getLineOffsets(), i = 0, n = t.length;
                    if (0 === n) return a.create(0, e);
                    for (; i < n;) {
                        var o = Math.floor((i + n) / 2);
                        t[o] > e ? n = o : i = o + 1
                    }
                    var r = i - 1;
                    return a.create(r, e - t[r])
                }, e.prototype.offsetAt = function (e) {
                    var t = this.getLineOffsets();
                    if (e.line >= t.length) return this._content.length;
                    if (e.line < 0) return 0;
                    var i = t[e.line], n = e.line + 1 < t.length ? t[e.line + 1] : this._content.length;
                    return Math.max(Math.min(i + e.character, n), i)
                }, Object.defineProperty(e.prototype, "lineCount", {
                    get: function () {
                        return this.getLineOffsets().length
                    }, enumerable: !1, configurable: !0
                }), e
            }();
            !function (e) {
                var t = Object.prototype.toString;
                e.defined = function (e) {
                    return void 0 !== e
                }, e.undefined = function (e) {
                    return void 0 === e
                }, e.boolean = function (e) {
                    return !0 === e || !1 === e
                }, e.string = function (e) {
                    return "[object String]" === t.call(e)
                }, e.number = function (e) {
                    return "[object Number]" === t.call(e)
                }, e.numberRange = function (e, i, n) {
                    return "[object Number]" === t.call(e) && i <= e && e <= n
                }, e.integer = function (e) {
                    return "[object Number]" === t.call(e) && -2147483648 <= e && e <= 2147483647
                }, e.uinteger = function (e) {
                    return "[object Number]" === t.call(e) && 0 <= e && e <= 2147483647
                }, e.func = function (e) {
                    return "[object Function]" === t.call(e)
                }, e.objectLiteral = function (e) {
                    return null !== e && "object" == typeof e
                }, e.typedArray = function (e, t) {
                    return Array.isArray(e) && e.every(t)
                }
            }(Te || (Te = {}))
        }, 9992: e => {
            "use strict";
            e.exports = function (e) {
                e.prototype[Symbol.iterator] = function* () {
                    for (let e = this.head; e; e = e.next) yield e.value
                }
            }
        }, 852: (e, t, i) => {
            "use strict";

            function n(e) {
                var t = this;
                if (t instanceof n || (t = new n), t.tail = null, t.head = null, t.length = 0, e && "function" == typeof e.forEach) e.forEach((function (e) {
                    t.push(e)
                })); else if (arguments.length > 0) for (var i = 0, o = arguments.length; i < o; i++) t.push(arguments[i]);
                return t
            }

            function o(e, t, i) {
                var n = t === e.head ? new a(i, null, t, e) : new a(i, t, t.next, e);
                return null === n.next && (e.tail = n), null === n.prev && (e.head = n), e.length++, n
            }

            function r(e, t) {
                e.tail = new a(t, e.tail, null, e), e.head || (e.head = e.tail), e.length++
            }

            function s(e, t) {
                e.head = new a(t, null, e.head, e), e.tail || (e.tail = e.head), e.length++
            }

            function a(e, t, i, n) {
                if (!(this instanceof a)) return new a(e, t, i, n);
                this.list = n, this.value = e, t ? (t.next = this, this.prev = t) : this.prev = null, i ? (i.prev = this, this.next = i) : this.next = null
            }

            e.exports = n, n.Node = a, n.create = n, n.prototype.removeNode = function (e) {
                if (e.list !== this) throw new Error("removing node which does not belong to this list");
                var t = e.next, i = e.prev;
                return t && (t.prev = i), i && (i.next = t), e === this.head && (this.head = t), e === this.tail && (this.tail = i), e.list.length--, e.next = null, e.prev = null, e.list = null, t
            }, n.prototype.unshiftNode = function (e) {
                if (e !== this.head) {
                    e.list && e.list.removeNode(e);
                    var t = this.head;
                    e.list = this, e.next = t, t && (t.prev = e), this.head = e, this.tail || (this.tail = e), this.length++
                }
            }, n.prototype.pushNode = function (e) {
                if (e !== this.tail) {
                    e.list && e.list.removeNode(e);
                    var t = this.tail;
                    e.list = this, e.prev = t, t && (t.next = e), this.tail = e, this.head || (this.head = e), this.length++
                }
            }, n.prototype.push = function () {
                for (var e = 0, t = arguments.length; e < t; e++) r(this, arguments[e]);
                return this.length
            }, n.prototype.unshift = function () {
                for (var e = 0, t = arguments.length; e < t; e++) s(this, arguments[e]);
                return this.length
            }, n.prototype.pop = function () {
                if (this.tail) {
                    var e = this.tail.value;
                    return this.tail = this.tail.prev, this.tail ? this.tail.next = null : this.head = null, this.length--, e
                }
            }, n.prototype.shift = function () {
                if (this.head) {
                    var e = this.head.value;
                    return this.head = this.head.next, this.head ? this.head.prev = null : this.tail = null, this.length--, e
                }
            }, n.prototype.forEach = function (e, t) {
                t = t || this;
                for (var i = this.head, n = 0; null !== i; n++) e.call(t, i.value, n, this), i = i.next
            }, n.prototype.forEachReverse = function (e, t) {
                t = t || this;
                for (var i = this.tail, n = this.length - 1; null !== i; n--) e.call(t, i.value, n, this), i = i.prev
            }, n.prototype.get = function (e) {
                for (var t = 0, i = this.head; null !== i && t < e; t++) i = i.next;
                if (t === e && null !== i) return i.value
            }, n.prototype.getReverse = function (e) {
                for (var t = 0, i = this.tail; null !== i && t < e; t++) i = i.prev;
                if (t === e && null !== i) return i.value
            }, n.prototype.map = function (e, t) {
                t = t || this;
                for (var i = new n, o = this.head; null !== o;) i.push(e.call(t, o.value, this)), o = o.next;
                return i
            }, n.prototype.mapReverse = function (e, t) {
                t = t || this;
                for (var i = new n, o = this.tail; null !== o;) i.push(e.call(t, o.value, this)), o = o.prev;
                return i
            }, n.prototype.reduce = function (e, t) {
                var i, n = this.head;
                if (arguments.length > 1) i = t; else {
                    if (!this.head) throw new TypeError("Reduce of empty list with no initial value");
                    n = this.head.next, i = this.head.value
                }
                for (var o = 0; null !== n; o++) i = e(i, n.value, o), n = n.next;
                return i
            }, n.prototype.reduceReverse = function (e, t) {
                var i, n = this.tail;
                if (arguments.length > 1) i = t; else {
                    if (!this.tail) throw new TypeError("Reduce of empty list with no initial value");
                    n = this.tail.prev, i = this.tail.value
                }
                for (var o = this.length - 1; null !== n; o--) i = e(i, n.value, o), n = n.prev;
                return i
            }, n.prototype.toArray = function () {
                for (var e = new Array(this.length), t = 0, i = this.head; null !== i; t++) e[t] = i.value, i = i.next;
                return e
            }, n.prototype.toArrayReverse = function () {
                for (var e = new Array(this.length), t = 0, i = this.tail; null !== i; t++) e[t] = i.value, i = i.prev;
                return e
            }, n.prototype.slice = function (e, t) {
                (t = t || this.length) < 0 && (t += this.length), (e = e || 0) < 0 && (e += this.length);
                var i = new n;
                if (t < e || t < 0) return i;
                e < 0 && (e = 0), t > this.length && (t = this.length);
                for (var o = 0, r = this.head; null !== r && o < e; o++) r = r.next;
                for (; null !== r && o < t; o++, r = r.next) i.push(r.value);
                return i
            }, n.prototype.sliceReverse = function (e, t) {
                (t = t || this.length) < 0 && (t += this.length), (e = e || 0) < 0 && (e += this.length);
                var i = new n;
                if (t < e || t < 0) return i;
                e < 0 && (e = 0), t > this.length && (t = this.length);
                for (var o = this.length, r = this.tail; null !== r && o > t; o--) r = r.prev;
                for (; null !== r && o > e; o--, r = r.prev) i.push(r.value);
                return i
            }, n.prototype.splice = function (e, t, ...i) {
                e > this.length && (e = this.length - 1), e < 0 && (e = this.length + e);
                for (var n = 0, r = this.head; null !== r && n < e; n++) r = r.next;
                var s = [];
                for (n = 0; r && n < t; n++) s.push(r.value), r = this.removeNode(r);
                null === r && (r = this.tail), r !== this.head && r !== this.tail && (r = r.prev);
                for (n = 0; n < i.length; n++) r = o(this, r, i[n]);
                return s
            }, n.prototype.reverse = function () {
                for (var e = this.head, t = this.tail, i = e; null !== i; i = i.prev) {
                    var n = i.prev;
                    i.prev = i.next, i.next = n
                }
                return this.head = t, this.tail = e, this
            };
            try {
                i(9992)(n)
            } catch (e) {
            }
        }, 9496: e => {
            "use strict";
            e.exports = require("vscode")
        }, 2081: e => {
            "use strict";
            e.exports = require("child_process")
        }, 6113: e => {
            "use strict";
            e.exports = require("crypto")
        }, 7147: e => {
            "use strict";
            e.exports = require("fs")
        }, 1808: e => {
            "use strict";
            e.exports = require("net")
        }, 2037: e => {
            "use strict";
            e.exports = require("os")
        }, 1017: e => {
            "use strict";
            e.exports = require("path")
        }, 3837: e => {
            "use strict";
            e.exports = require("util")
        }
    }, t = {};

    function i(n) {
        var o = t[n];
        if (void 0 !== o) return o.exports;
        var r = t[n] = {exports: {}};
        return e[n].call(r.exports, r, r.exports, i), r.exports
    }

    i.d = (e, t) => {
        for (var n in t) i.o(t, n) && !i.o(e, n) && Object.defineProperty(e, n, {enumerable: !0, get: t[n]})
    }, i.o = (e, t) => Object.prototype.hasOwnProperty.call(e, t), i.r = e => {
        "undefined" != typeof Symbol && Symbol.toStringTag && Object.defineProperty(e, Symbol.toStringTag, {value: "Module"}), Object.defineProperty(e, "__esModule", {value: !0})
    };
    var n = i(7581);
    module.exports = n
})();
