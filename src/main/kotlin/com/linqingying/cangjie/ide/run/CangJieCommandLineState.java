package com.linqingying.cangjie.ide.run;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.ArgumentFileFilter;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.target.*;
import com.intellij.execution.target.local.LocalTargetEnvironment;
import com.intellij.execution.target.local.LocalTargetEnvironmentRequest;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.projectRoots.JdkUtil;
import com.intellij.openapi.util.registry.Registry;
import com.linqingying.cangjie.cjpm.toolchain.CjToolchainBaseKt;
import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public class CangJieCommandLineState extends CommandLineState implements TargetEnvironmentAwareRunProfileState {
    private static final Logger LOG = Logger.getInstance(CangJieCommandLineState.class);
    //    @Nullable
//    private volatile TargetDebuggerConnection myTargetDebuggerConnection;
    @NotNull
    protected final CjpmCommandConfiguration myConfiguration;

    @NotNull
    protected final CjpmCommandConfiguration.CleanConfiguration.Ok myConfig;
    //    private JavaParameters myParams;
    private TargetEnvironmentRequest myTargetEnvironmentRequest;
    private TargetedCommandLineBuilder myCommandLine;


    public CangJieCommandLineState(@NotNull ExecutionEnvironment environment, @NotNull CjpmCommandConfiguration configuration
            , CjpmCommandConfiguration.CleanConfiguration.@NotNull Ok config) {
        super(environment);
        myConfiguration = configuration;
        myConfig = config;
    }


//    @Override
//    public JavaParameters getJavaParameters() throws ExecutionException {
//        if (myParams == null) {
//            myParams = isReadActionRequired() ? ReadAction.compute(this::createJavaParameters) : createJavaParameters();
//        }
//        return myParams;
//    }

    @NotNull
    protected CjpmCommandConfiguration getConfiguration() {
        return myConfiguration;
    }

//    public void clear() {
//        myParams = null;
//    }

    protected boolean isReadActionRequired() {
        return true;
    }

    @Override
    @NotNull
    protected OSProcessHandler startProcess() throws ExecutionException {
        TargetEnvironment remoteEnvironment = getEnvironment().getPreparedTargetEnvironment(this, TargetProgressIndicator.EMPTY);
        TargetedCommandLineBuilder targetedCommandLineBuilder = getTargetedCommandLine();
        TargetedCommandLine targetedCommandLine = targetedCommandLineBuilder.build();
        Process process = remoteEnvironment.createProcess(targetedCommandLine, new EmptyProgressIndicator());

        Map<String, String> content = targetedCommandLineBuilder.getUserData(JdkUtil.COMMAND_LINE_CONTENT);
        if (content != null) {
            content.forEach((key, value) -> addConsoleFilters(new ArgumentFileFilter(key, value)));
        }
        OSProcessHandler handler = createProcessHandler(remoteEnvironment, targetedCommandLineBuilder, targetedCommandLine, process);
        ProcessTerminatedListener.attach(handler);
        CangJieRunConfigurationExtensionManager.INSTANCE.attachExtensionsToProcess(getConfiguration(), handler, getRunnerSettings());
        return handler;
    }
    protected @NotNull OSProcessHandler createProcessHandler(GeneralCommandLine commandLine) throws ExecutionException {
        return new KillableColoredProcessHandler.Silent(commandLine);
    }
    protected @NotNull OSProcessHandler createProcessHandler(TargetEnvironment remoteEnvironment,
                                                             TargetedCommandLineBuilder targetedCommandLineBuilder,
                                                             TargetedCommandLine targetedCommandLine,
                                                             Process process) throws ExecutionException {
        return new KillableColoredProcessHandler.Silent(process,
                targetedCommandLine.getCommandPresentation(remoteEnvironment),
                targetedCommandLine.getCharset(),
                targetedCommandLineBuilder.getFilesToDeleteOnTermination());
    }
//    protected abstract JavaParameters createJavaParameters() throws ExecutionException;

//    @Override
//    public TargetEnvironmentRequest createCustomTargetEnvironmentRequest() {
//        try {
//            JavaParameters parameters = getJavaParameters();
//            WslTargetEnvironmentConfiguration config = checkCreateWslConfiguration(parameters.getJdk());
//            return config == null ? null : new WslTargetEnvironmentRequest(config);
//        }
//        catch (ExecutionException e) {
//            // ignore
//        }
//        return null;
//    }

    protected boolean ansiColoringEnabled() {
        return true;
    }

    protected boolean shouldPrepareDebuggerConnection() {
        return true;
    }

    @Override
    public void prepareTargetEnvironmentRequest(
            @NotNull TargetEnvironmentRequest request,
            @NotNull TargetProgressIndicator targetProgressIndicator) throws ExecutionException {
        targetProgressIndicator.addSystemLine(ExecutionBundle.message("progress.text.prepare.target.requirements"));

        myTargetEnvironmentRequest = request;

//        TargetDebuggerConnection targetDebuggerConnection =
//                shouldPrepareDebuggerConnection() ? TargetDebuggerConnectionUtil.prepareDebuggerConnection(this, request) : null;
//        myTargetDebuggerConnection = targetDebuggerConnection;

        myCommandLine = createTargetedCommandLine(myTargetEnvironmentRequest);

//        if (targetDebuggerConnection != null) {
//            Objects.requireNonNull(request).getTargetPortBindings().add(targetDebuggerConnection.getDebuggerPortRequest());
//        }
    }

    @Override
    public void handleCreatedTargetEnvironment(@NotNull TargetEnvironment environment,
                                               @NotNull TargetProgressIndicator targetProgressIndicator) {
//        TargetDebuggerConnection targetDebuggerConnection = myTargetDebuggerConnection;
//        if (targetDebuggerConnection != null) {
//            targetDebuggerConnection.resolveRemoteConnection(environment);
//        }
    }

//    @Nullable
//    @Override
//    public RemoteConnection createRemoteConnection(ExecutionEnvironment environment) {
//        TargetDebuggerConnection targetDebuggerConnection = myTargetDebuggerConnection;
//        if (targetDebuggerConnection != null) {
//            return targetDebuggerConnection.getResolvedRemoteConnection();
//        }
//        else {
//            return null;
//        }
//    }

//    @Override
//    public boolean isPollConnection() {
//        return true;
//    }

    public synchronized @Nullable TargetEnvironmentRequest getTargetEnvironmentRequest() {
        return myTargetEnvironmentRequest;
    }

    @NotNull
    protected synchronized TargetedCommandLineBuilder getTargetedCommandLine() {
        if (myCommandLine != null) {
            // In a correct implementation that uses the new API this condition is always true.
            return myCommandLine;
        }

        if (RunTargetsEnabled.get() &&
                !(getEnvironment().getTargetEnvironmentRequest() instanceof LocalTargetEnvironmentRequest)) {
            LOG.error("Command line hasn't been built yet. " +
                    "Probably you need to run environment#getPreparedTargetEnvironment first, " +
                    "or it return the environment from the previous run session");
        }
        try {
            // force re-prepareTargetEnvironment in order to drop previous environment
            getEnvironment().prepareTargetEnvironment(this, TargetProgressIndicator.EMPTY);
            return myCommandLine;
        } catch (ExecutionException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }

    @NotNull
    protected TargetedCommandLineBuilder createTargetedCommandLine(@NotNull TargetEnvironmentRequest request)
            throws ExecutionException {
//        SimpleJavaParameters javaParameters = getJavaParameters();
//        if (!javaParameters.isDynamicClasspath()) {
//            javaParameters.setUseDynamicClasspath(getEnvironment().getProject());
//        }
//        return javaParameters.toCommandLine(request);

        return CjToolchainBaseKt.cjpm(myConfig.getToolchain()).toTargetedCommandLineBuilder(getConfiguration().getProject(), request, myConfig.getCmd());


    }

    protected GeneralCommandLine createCommandLine() throws ExecutionException {
        boolean redirectErrorStream = Registry.is("run.processes.with.redirectedErrorStream", false);
        LocalTargetEnvironmentRequest request = new LocalTargetEnvironmentRequest();
        TargetedCommandLineBuilder targetedCommandLineBuilder = createTargetedCommandLine(request);
        LocalTargetEnvironment environment = request.prepareEnvironment(TargetProgressIndicator.EMPTY);
        return environment
                .createGeneralCommandLine(targetedCommandLineBuilder.build())
                .withRedirectErrorStream(redirectErrorStream);
    }

    public boolean shouldAddJavaProgramRunnerActions() {
        return true;
    }
}
