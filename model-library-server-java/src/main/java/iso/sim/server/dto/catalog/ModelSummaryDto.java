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

package iso.sim.server.dto.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelSummaryDto {
    private final String modelId;
    private final String name;
    private final String description;
    private final String implementationLanguage;

    @JsonCreator
    public ModelSummaryDto(
        @JsonProperty("modelId") String modelId,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("implementationLanguage") String implementationLanguage
    ) {
        this.modelId = modelId;
        this.name = name;
        this.description = description;
        this.implementationLanguage = implementationLanguage;
    }

    public String getModelId() { return modelId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImplementationLanguage() { return implementationLanguage; }
}
