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

package iso.sim.server.executor.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.server.dto.run.KafkaConfigurationDto;
import iso.sim.server.dto.run.SimulationContextDto;
import iso.sim.server.dto.run.StartModelRunRequest;
import iso.sim.server.dto.run.TimeMode;
import iso.sim.server.dto.run.TimeModeDto;
import iso.sim.server.example.store.ClerkModel;
import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.runtime.RunHandle;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExampleClerkModelRunExecutorTest {
    @Test
    void passesRemoteKafkaTransportAndUsesRequestedInstanceAsClerkModelIdentity() {
        AtomicReference<ClerkModel> model = new AtomicReference<>();
        AtomicReference<ClerkRuntimeTransport> transport = new AtomicReference<>();
        ExampleClerkModelRunExecutor executor = new ExampleClerkModelRunExecutor((startedModel, startedTransport) -> {
            model.set(startedModel);
            transport.set(startedTransport);
            return new InProcessClerkRuntime().start(startedModel, startedTransport);
        });
        RunExecutionContext context = new RunExecutionContext("run-1", "example-clerk", new StartModelRunRequest(
            "run-1",
            new ObjectMapper().createObjectNode(),
            new KafkaConfigurationDto("kafka.example:9092", "simulation-topic", null, null, null),
            new SimulationContextDto("simulation-1", "clerk-7", "coordinator-1", new TimeModeDto(TimeMode.VIRTUAL_TIME)),
            null
        ));

        RunHandle handle = executor.start(context);

        assertEquals("clerk-7", model.get().getModelIdentifier());
        assertEquals("run-1", transport.get().runId());
        assertEquals("clerk-7", transport.get().receiverId());
        assertEquals("coordinator-1", transport.get().coordinatorReceiverId());
        assertEquals("simulation-topic", transport.get().kafka().getTopic());
        handle.stop();
        assertFalse(handle.isAlive());
    }
}
