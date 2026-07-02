package iso.sim.server.service;

import iso.sim.server.executor.RunExecutionContext;

public interface KafkaConsumerAdapterFactory {
    KafkaConsumerAdapter create(RunExecutionContext context, String topic, String consumerGroup);
}
