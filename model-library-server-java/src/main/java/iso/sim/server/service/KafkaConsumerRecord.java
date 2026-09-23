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

package iso.sim.server.service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public record KafkaConsumerRecord(String key, String value, Map<String, byte[]> headers) {
    public KafkaConsumerRecord {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public String headerValue(String name) {
        byte[] value = headers.get(name);
        return value == null ? null : new String(value, StandardCharsets.UTF_8);
    }
}
