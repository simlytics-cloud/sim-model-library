/*
 * Sim Model Library Copyright (C) 2026 simlytics.cloud LLC and
 * Sim Model Library contributors.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package iso.sim.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.server.catalog.CatalogRepository;
import iso.sim.server.catalog.ModelCatalogService;
import iso.sim.server.dto.run.CoordinatorHelperCallbackConfigurationDto;
import iso.sim.server.dto.run.KafkaConfigurationDto;
import iso.sim.server.dto.run.SimulationContextDto;
import iso.sim.server.dto.run.StartModelRunRequest;
import iso.sim.server.dto.run.TimeMode;
import iso.sim.server.dto.run.TimeModeDto;
import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.executor.RunExecutor;
import iso.sim.server.runtime.NoopRunHandle;
import iso.sim.server.runtime.RunResourceRegistry;
import iso.sim.server.service.InvalidRunRequestException;
import iso.sim.server.service.RemoteRunnerEventType;
import iso.sim.server.service.RunAlreadyExistsException;
import iso.sim.server.service.RunReadinessResult;
import iso.sim.server.service.RunService;
import iso.sim.server.store.InMemoryRunStatusStore;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ModelCatalogService catalog = new ModelCatalogService(
        new CatalogRepository(List.of(testCatalogPath()), objectMapper)
    );

    @Test
    void requiresCoordinatorSuppliedIdentityAndReportsRemoteRunnerFacts() {
        List<RemoteRunnerEventType> events = new ArrayList<>();
        RunService service = service(context -> new NoopRunHandle(context.getRunId()), events);

        service.startRun("irpsystem.irpmodel.Vehicle", request("run-1"));

        assertEquals("locally-ready", service.getRunStatus("run-1").getStatus());
        assertEquals(
            List.of(
                RemoteRunnerEventType.REMOTE_RUNNER_ACCEPTED,
                RemoteRunnerEventType.REMOTE_RUNNER_STARTING,
                RemoteRunnerEventType.REMOTE_RUNNER_READY
            ),
            events
        );
    }

    @Test
    void rejectsAllDuplicateRunIdsAtomically() {
        RunService service = service(context -> new NoopRunHandle(context.getRunId()), new ArrayList<>());
        service.startRun("irpsystem.irpmodel.Vehicle", request("run-duplicate"));

        assertThrows(
            RunAlreadyExistsException.class,
            () -> service.startRun("irpsystem.irpmodel.Vehicle", request("run-duplicate"))
        );
    }

    @Test
    void rejectsMissingCoordinatorSuppliedFields() {
        RunService service = service(context -> new NoopRunHandle(context.getRunId()), new ArrayList<>());
        StartModelRunRequest invalid = new StartModelRunRequest(
            null, objectMapper.createObjectNode(), null, null, null
        );

        InvalidRunRequestException exception = assertThrows(
            InvalidRunRequestException.class,
            () -> service.startRun("irpsystem.irpmodel.Vehicle", invalid)
        );
        assertTrue(exception.getMessage().contains("runId"));
    }

    @Test
    void requiresDirectKafkaConfigurationAndValidatesOptionalCoordinatorHelperAsASeparateCallback() {
        RunService service = service(context -> new NoopRunHandle(context.getRunId()), new ArrayList<>());
        StartModelRunRequest missingKafka = new StartModelRunRequest(
            "run-no-kafka",
            objectMapper.createObjectNode(),
            null,
            new SimulationContextDto("simulation-1", "instance-1", "coordinator-1", new TimeModeDto(TimeMode.VIRTUAL_TIME)),
            null
        );
        InvalidRunRequestException missingKafkaException = assertThrows(
            InvalidRunRequestException.class,
            () -> service.startRun("irpsystem.irpmodel.Vehicle", missingKafka)
        );
        assertTrue(missingKafkaException.getMessage().contains("'kafka'"));

        StartModelRunRequest incompleteCallback = new StartModelRunRequest(
            "run-incomplete-callback",
            objectMapper.createObjectNode(),
            new KafkaConfigurationDto("kafka:9092", "topic", null, null, null),
            new SimulationContextDto("simulation-1", "instance-1", "coordinator-1", new TimeModeDto(TimeMode.VIRTUAL_TIME)),
            new CoordinatorHelperCallbackConfigurationDto("http://helper.example", null)
        );
        InvalidRunRequestException callbackException = assertThrows(
            InvalidRunRequestException.class,
            () -> service.startRun("irpsystem.irpmodel.Vehicle", incompleteCallback)
        );
        assertTrue(callbackException.getMessage().contains("coordinatorHelper"));
    }

    @Test
    void rejectsUnsafeReceiverIdentitiesBeforeDerivingKafkaConsumerGroups() {
        RunService service = service(context -> new NoopRunHandle(context.getRunId()), new ArrayList<>());
        StartModelRunRequest unsafe = new StartModelRunRequest(
            "run-safe",
            objectMapper.createObjectNode(),
            new KafkaConfigurationDto("kafka:9092", "topic", null, null, null),
            new SimulationContextDto("simulation-1", "instance:unsafe", "coordinator-1", new TimeModeDto(TimeMode.VIRTUAL_TIME)),
            null
        );

        InvalidRunRequestException exception = assertThrows(
            InvalidRunRequestException.class,
            () -> service.startRun("irpsystem.irpmodel.Vehicle", unsafe)
        );
        assertTrue(exception.getMessage().contains("modelInstanceId"));
    }

    @Test
    void localCancellationStopsTheHandleAndReportsOnlyLocalStop() {
        List<RemoteRunnerEventType> events = new ArrayList<>();
        RunService service = service(context -> new NoopRunHandle(context.getRunId()), events);
        service.startRun("irpsystem.irpmodel.Vehicle", request("run-stop"));

        service.cancelRun("run-stop");

        assertEquals("locally-stopped", service.getRunStatus("run-stop").getStatus());
        assertEquals(RemoteRunnerEventType.REMOTE_RUNNER_STOPPED, events.getLast());
    }

    private RunService service(RunExecutor executor, List<RemoteRunnerEventType> events) {
        return new RunService(
            catalog,
            executor,
            new InMemoryRunStatusStore(),
            new RunResourceRegistry(),
            context -> (runContext, handle) -> RunReadinessResult.ready("ready"),
            (context, handle) -> new NoopRunHandle(context.getRunId()),
            (context, eventType, detail) -> events.add(eventType)
        );
    }

    private StartModelRunRequest request(String runId) {
        return new StartModelRunRequest(
            runId,
            objectMapper.createObjectNode(),
            new KafkaConfigurationDto("kafka:9092", "topic", null, null, null),
            new SimulationContextDto("simulation-1", "instance-1", "coordinator-1", new TimeModeDto(TimeMode.VIRTUAL_TIME)),
            null
        );
    }

    private static String testCatalogPath() {
        return Path.of(Objects.requireNonNull(RunServiceTest.class.getResource("/model-catalog.json")).getPath()).toString();
    }
}
