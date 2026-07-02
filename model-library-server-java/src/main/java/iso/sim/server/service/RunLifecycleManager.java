package iso.sim.server.service;

import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.executor.RunExecutor;
import iso.sim.server.runtime.CompositeRunHandle;
import iso.sim.server.runtime.RunHandle;
import iso.sim.server.runtime.RunResourceRegistry;

import java.util.List;

public class RunLifecycleManager {
    private final RunExecutor runExecutor;
    private final RunLifecycleService runLifecycleService;
    private final RunResourceRegistry runResourceRegistry;
    private final RunTerminalCoordinator runTerminalCoordinator;
    private final RunReadinessProbeSelector runReadinessProbeSelector;
    private final RunMonitor runMonitor;

    public RunLifecycleManager(
        RunExecutor runExecutor,
        RunLifecycleService runLifecycleService,
        RunResourceRegistry runResourceRegistry,
        RunTerminalCoordinator runTerminalCoordinator,
        RunReadinessProbeSelector runReadinessProbeSelector,
        RunMonitor runMonitor
    ) {
        this.runExecutor = runExecutor;
        this.runLifecycleService = runLifecycleService;
        this.runResourceRegistry = runResourceRegistry;
        this.runTerminalCoordinator = runTerminalCoordinator;
        this.runReadinessProbeSelector = runReadinessProbeSelector;
        this.runMonitor = runMonitor;
    }

    public void startRun(RunExecutionContext context) {
        String runId = context.getRunId();
        runLifecycleService.markStarting(runId, "Starting run resources");

        try {
            RunHandle handle = runExecutor.start(context);
            runResourceRegistry.register(runId, handle);

            RunReadinessProbe runReadinessProbe = runReadinessProbeSelector.select(context);
            RunReadinessResult readiness = runReadinessProbe.awaitReady(context, handle);
            if (readiness.isReady()) {
                runLifecycleService.markReady(runId, readiness.getMessage());
                RunHandle monitorHandle = runMonitor.startMonitoring(context, handle);
                runResourceRegistry.register(runId, new CompositeRunHandle(runId, List.of(handle, monitorHandle)));
                return;
            }

            runTerminalCoordinator.failRun(runId, "Run readiness failed: " + readiness.getMessage());
        } catch (RuntimeException ex) {
            runTerminalCoordinator.failRun(runId, "Run failed to start: " + ex.getMessage());
        }
    }

    public void completeRun(String runId, String message) {
        runTerminalCoordinator.completeRun(runId, message);
    }

    public void failRun(String runId, String message) {
        runTerminalCoordinator.failRun(runId, message);
    }
}
