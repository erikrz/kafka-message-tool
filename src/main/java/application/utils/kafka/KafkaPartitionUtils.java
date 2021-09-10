package application.utils.kafka;

import java.nio.charset.Charset;

import org.apache.kafka.common.utils.Utils;

public class KafkaPartitionUtils {

    public static int partition(String key, int numPartitions) {
        if (key == null) {
            return -1;
        }

        return Utils.toPositive(Utils.murmur2(key.getBytes(Charset.defaultCharset()))) % numPartitions;
    }
}
