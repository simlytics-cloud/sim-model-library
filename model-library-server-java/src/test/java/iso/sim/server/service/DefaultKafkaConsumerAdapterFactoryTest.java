package iso.sim.server.service;

import iso.sim.server.dto.run.KafkaConfigurationDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultKafkaConsumerAdapterFactoryTest {

    @Test
    void buildConsumerPropertiesIgnoresGroupIdOverrideAndKeepsOtherProperties() {
        DefaultKafkaConsumerAdapterFactory factory = new DefaultKafkaConsumerAdapterFactory();
        KafkaConfigurationDto kafkaConfiguration = new KafkaConfigurationDto(
            "kafka:9092",
            "topic",
            "SASL_SSL",
            "PLAIN",
            Map.of(
                ConsumerConfig.GROUP_ID_CONFIG, "attempted-override",
                ConsumerConfig.CLIENT_ID_CONFIG, "client-42"
            )
        );

        Properties properties = factory.buildConsumerProperties(kafkaConfiguration, "run-1:receiver-1");

        assertEquals("run-1:receiver-1", properties.getProperty(ConsumerConfig.GROUP_ID_CONFIG));
        assertEquals("client-42", properties.getProperty(ConsumerConfig.CLIENT_ID_CONFIG));
        assertEquals("kafka:9092", properties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
    }
}