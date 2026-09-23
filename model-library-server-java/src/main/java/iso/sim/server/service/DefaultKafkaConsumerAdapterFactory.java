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
import iso.sim.server.executor.RunExecutionContext;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.header.Header;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class DefaultKafkaConsumerAdapterFactory implements KafkaConsumerAdapterFactory {
    private static final Logger logger = Logger.getLogger(DefaultKafkaConsumerAdapterFactory.class.getName());

    @Override
    public KafkaConsumerAdapter create(RunExecutionContext context, String topic, String consumerGroup) {
        KafkaConfigurationDto kafkaConfig = context.getRequest().getKafka();
        Properties properties = buildConsumerProperties(kafkaConfig, consumerGroup);

        String runId = context.getRunId();
        logger.info(() -> "Creating Kafka consumer for runId=" + runId
            + ", topic=" + topic
            + ", consumerGroup=" + consumerGroup
            + ", bootstrapServers=" + kafkaConfig.getBootstrapServers()
            + ", autoOffsetReset=" + properties.getProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(List.of(topic));
        logger.info(() -> "Subscribed Kafka consumer for runId=" + runId + " to topic=" + topic);
        return new KafkaConsumerAdapter() {
            @Override
            public List<KafkaConsumerRecord> poll(Duration timeout) {
                ConsumerRecords<String, String> records = consumer.poll(timeout);
                List<KafkaConsumerRecord> values = new ArrayList<>();
                for (ConsumerRecord<String, String> record : records) {
                    values.add(toKafkaConsumerRecord(record));
                }

                return values;
            }

            @Override
            public void close() {
                consumer.close();
            }
        };
    }

    static KafkaConsumerRecord toKafkaConsumerRecord(ConsumerRecord<String, String> record) {
        Map<String, byte[]> headers = new java.util.LinkedHashMap<>();
        for (Header header : record.headers()) {
            headers.put(header.key(), header.value());
        }
        return new KafkaConsumerRecord(record.key(), record.value(), headers);
    }

    Properties buildConsumerProperties(KafkaConfigurationDto kafkaConfig, String consumerGroup) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        if (kafkaConfig.getSecurityProtocol() != null && !kafkaConfig.getSecurityProtocol().isBlank()) {
            properties.put("security.protocol", kafkaConfig.getSecurityProtocol());
        }
        if (kafkaConfig.getSaslMechanism() != null && !kafkaConfig.getSaslMechanism().isBlank()) {
            properties.put("sasl.mechanism", kafkaConfig.getSaslMechanism());
        }
        if (kafkaConfig.getProperties() != null) {
            for (Map.Entry<String, String> entry : kafkaConfig.getProperties().entrySet()) {
                if (ConsumerConfig.GROUP_ID_CONFIG.equals(entry.getKey())) {
                    logger.warning(() -> "Ignoring Kafka property override for group.id; using derived consumerGroup=" + consumerGroup);
                    continue;
                }
                properties.put(entry.getKey(), entry.getValue());
            }
        }
        return properties;
    }
}
