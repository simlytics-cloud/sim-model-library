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

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentSimulationTimeDto {
    private final BigDecimal value;
    private final String sourceMessageType;
    private final String sourceMessageId;
    private final String updatedAt;

    @JsonCreator
    public CurrentSimulationTimeDto(
        @JsonProperty("value") BigDecimal value,
        @JsonProperty("sourceMessageType") String sourceMessageType,
        @JsonProperty("sourceMessageId") String sourceMessageId,
        @JsonProperty("updatedAt") String updatedAt
    ) {
        this.value = value;
        this.sourceMessageType = sourceMessageType;
        this.sourceMessageId = sourceMessageId;
        this.updatedAt = updatedAt;
    }

    public BigDecimal getValue() { return value; }
    public String getSourceMessageType() { return sourceMessageType; }
    public String getSourceMessageId() { return sourceMessageId; }
    public String getUpdatedAt() { return updatedAt; }
}
