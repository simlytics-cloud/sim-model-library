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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeModeDto {
    private final TimeMode mode;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Double realTimeFactor;

    @JsonCreator
    public TimeModeDto(
        @JsonProperty("mode") TimeMode mode,
        @JsonProperty("realTimeFactor") Double realTimeFactor
    ) {
        if (mode == null) {
            throw new IllegalArgumentException("mode is required");
        }
        if (mode == TimeMode.SCALED_REAL_TIME && realTimeFactor == null) {
            throw new IllegalArgumentException("realTimeFactor is required when mode is scaled-real-time");
        }
        this.mode = mode;
        this.realTimeFactor = mode == TimeMode.SCALED_REAL_TIME ? realTimeFactor : null;
    }

    public TimeModeDto(TimeMode mode) {
        this(mode, null);
    }

    public TimeMode getMode() { return mode; }
    public Double getRealTimeFactor() { return realTimeFactor; }
}
