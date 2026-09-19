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

public class KafkaDefaultsConfig {
    private final String bootstrapServers;
    private final String topic;
    private final String securityProtocol;
    private final String saslMechanism;

    public KafkaDefaultsConfig(
        String bootstrapServers,
        String topic,
        String securityProtocol,
        String saslMechanism
    ) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
        this.securityProtocol = securityProtocol;
        this.saslMechanism = saslMechanism;
    }

    public static KafkaDefaultsConfig from(Config config) {
        return new KafkaDefaultsConfig(
            config.getString("model.library.run-config.kafka-defaults.bootstrap-servers"),
            config.getString("model.library.run-config.kafka-defaults.topic"),
            config.getString("model.library.run-config.kafka-defaults.security-protocol"),
            config.getString("model.library.run-config.kafka-defaults.sasl-mechanism")
        );
    }

    public KafkaDefaultsResponse toResponse() {
        return new KafkaDefaultsResponse(bootstrapServers, topic, securityProtocol, saslMechanism);
    }
}