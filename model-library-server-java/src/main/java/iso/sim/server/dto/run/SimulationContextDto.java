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

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimulationContextDto {
    private final String simulationId;
    private final String modelInstanceId;
    private final String coordinatorId;
    private final TimeModeDto timeMode;

    @JsonCreator
    public SimulationContextDto(
        @JsonProperty("simulationId") String simulationId,
        @JsonProperty("modelInstanceId") String modelInstanceId,
        @JsonProperty("coordinatorId") String coordinatorId,
        @JsonProperty("timeMode") TimeModeDto timeMode
    ) {
        this.simulationId = simulationId;
        this.modelInstanceId = modelInstanceId;
        this.coordinatorId = coordinatorId;
        this.timeMode = timeMode;
    }

    public String getSimulationId() { return simulationId; }
    public String getModelInstanceId() { return modelInstanceId; }
    public String getCoordinatorId() { return coordinatorId; }
    public TimeModeDto getTimeMode() { return timeMode; }
}
