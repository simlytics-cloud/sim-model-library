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

package iso.sim.server.service;

import com.typesafe.config.Config;
import iso.sim.server.dto.run.KafkaDefaultsResponse;

import java.util.LinkedHashMap;
import java.util.Map;

public class KafkaDefaultsConfig {
    private final String topic;
    private final Map<String, String> properties;

    public KafkaDefaultsConfig(String topic, Map<String, String> properties) {
        this.topic = topic;
        this.properties = properties;
    }

    public static KafkaDefaultsConfig from(Config config) {
        String propertiesPath = "model.library.run-config.kafka-defaults.properties";
        Map<String, String> properties = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : config.getConfig(propertiesPath).root().unwrapped().entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof String stringValue)) {
                throw new IllegalArgumentException("Kafka default property '" + entry.getKey() + "' must be a string");
            }
            properties.put(entry.getKey(), stringValue);
        }
        return new KafkaDefaultsConfig(
            config.getString("model.library.run-config.kafka-defaults.topic"),
            properties
        );
    }

    public KafkaDefaultsResponse toResponse() {
        return new KafkaDefaultsResponse(topic, properties);
    }
}