/*
 * Sim Model Library Copyright (C) 2026 simlytics.cloud LLC and
 * Sim Model Library contributors.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License
 *  is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing permissions and limitations under
 *  the License.
 *
 */

package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StartModelRunRequest {
    private final String runId;
    private final JsonNode initializationParameters;
    private final KafkaConfigurationDto kafka;
    private final SimulationContextDto simulation;
    private final CoordinatorHelperCallbackConfigurationDto coordinatorHelper;

    @JsonCreator
    public StartModelRunRequest(
        @JsonProperty("runId") String runId,
        @JsonProperty("initializationParameters") JsonNode initializationParameters,
        @JsonProperty("kafka") KafkaConfigurationDto kafka,
        @JsonProperty("simulation") SimulationContextDto simulation,
        @JsonProperty("coordinatorHelper") CoordinatorHelperCallbackConfigurationDto coordinatorHelper
    ) {
        this.runId = runId;
        this.initializationParameters = initializationParameters;
        this.kafka = kafka;
        this.simulation = simulation;
        this.coordinatorHelper = coordinatorHelper;
    }

    public String getRunId() { return runId; }
    public JsonNode getInitializationParameters() { return initializationParameters; }
    public KafkaConfigurationDto getKafka() { return kafka; }
    public SimulationContextDto getSimulation() { return simulation; }
    public CoordinatorHelperCallbackConfigurationDto getCoordinatorHelper() { return coordinatorHelper; }
}
