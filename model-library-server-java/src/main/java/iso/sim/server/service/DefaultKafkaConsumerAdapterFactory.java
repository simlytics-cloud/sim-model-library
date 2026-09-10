package iso.sim.server.service;

import iso.sim.server.dto.run.KafkaConfigurationDto;
import iso.sim.server.executor.RunExecutionContext;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

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
            public List<String> poll(Duration timeout) {
                ConsumerRecords<String, String> records = consumer.poll(timeout);
                List<String> values = new ArrayList<>();
                for (ConsumerRecord<String, String> record : records) {
                    values.add(record.value());
                }
                return values;
            }

            @Override
            public void close() {
                consumer.close();
            }
        };
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
