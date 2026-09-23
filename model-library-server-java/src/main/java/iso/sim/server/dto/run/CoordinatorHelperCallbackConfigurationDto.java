/*
 * Sim Model Library Copyright (C) 2026 simlytics.cloud LLC and
 * Sim Model Library contributors.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Optional control-plane callback configuration for a separately deployed coordinator helper.
 * It is deliberately independent of the simulation Kafka transport.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoordinatorHelperCallbackConfigurationDto {
    private final String endpoint;
    private final String token;

    @JsonCreator
    public CoordinatorHelperCallbackConfigurationDto(
        @JsonProperty("endpoint") String endpoint,
        @JsonProperty("token") String token
    ) {
        this.endpoint = endpoint;
        this.token = token;
    }

    public String getEndpoint() { return endpoint; }
    public String getToken() { return token; }
}
