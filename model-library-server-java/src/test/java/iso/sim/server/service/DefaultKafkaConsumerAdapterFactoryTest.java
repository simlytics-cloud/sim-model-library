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

import iso.sim.server.dto.run.KafkaConfigurationDto;
import iso.sim.server.dto.run.KafkaSaslMechanism;
import iso.sim.server.dto.run.KafkaSecurityProtocol;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultKafkaConsumerAdapterFactoryTest {

    @Test
    void buildConsumerPropertiesIgnoresGroupIdOverrideAndKeepsOtherProperties() {
        DefaultKafkaConsumerAdapterFactory factory = new DefaultKafkaConsumerAdapterFactory();
        KafkaConfigurationDto kafkaConfiguration = new KafkaConfigurationDto(
            "kafka:9092",
            "topic",
            KafkaSecurityProtocol.SASL_SSL,
            KafkaSaslMechanism.SCRAM_SHA_512,
            Map.of(
                ConsumerConfig.GROUP_ID_CONFIG, "attempted-override",
                ConsumerConfig.CLIENT_ID_CONFIG, "client-42"
            )
        );

        Properties properties = factory.buildConsumerProperties(kafkaConfiguration, "run-1:receiver-1");

        assertEquals("run-1:receiver-1", properties.getProperty(ConsumerConfig.GROUP_ID_CONFIG));
        assertEquals("client-42", properties.getProperty(ConsumerConfig.CLIENT_ID_CONFIG));
        assertEquals("kafka:9092", properties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals("SASL_SSL", properties.getProperty("security.protocol"));
        assertEquals("SCRAM-SHA-512", properties.getProperty("sasl.mechanism"));
    }

    @Test
    void rejectsInconsistentSaslConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new KafkaConfigurationDto(
            "kafka:9092", "topic", KafkaSecurityProtocol.SASL_SSL, null, null
        ));
        assertThrows(IllegalArgumentException.class, () -> new KafkaConfigurationDto(
            "kafka:9092", "topic", KafkaSecurityProtocol.SSL, KafkaSaslMechanism.PLAIN, null
        ));
    }

    @Test
    void preservesKafkaRecordKeyAndHeadersForEarlyRunFiltering() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "run-1", "{\"messageType\":\"x\"}");
        record.headers().add("X-Run-Id", "run-1".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        KafkaConsumerRecord adapted = DefaultKafkaConsumerAdapterFactory.toKafkaConsumerRecord(record);

        assertEquals("run-1", adapted.key());
        assertEquals("run-1", adapted.headerValue("X-Run-Id"));
        assertArrayEquals("run-1".getBytes(java.nio.charset.StandardCharsets.UTF_8), adapted.headers().get("X-Run-Id"));
    }
}