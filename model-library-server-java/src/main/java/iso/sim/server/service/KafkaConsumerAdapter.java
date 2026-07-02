package iso.sim.server.service;

import java.time.Duration;
import java.util.List;

public interface KafkaConsumerAdapter extends AutoCloseable {
    List<String> poll(Duration timeout);

    @Override
    void close();
}
