package application.kafka.cluster;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;

import application.exceptions.ClusterConfigurationError;
import application.logging.Logger;
import application.utils.HostPortValue;

public class KafkaClusterProxyFactory {

    public static KafkaClusterProxy create(HostPortValue hostPort,
                                                  KafkaClusterProxy previous) throws ClusterConfigurationError,
                                                                                            ExecutionException,
                                                                                            TimeoutException,
                                                                                            InterruptedException {
        KafkaClusterProxy proxy = null;
        if (previous != null) {
            Logger.trace(String.format("[Proxy create] Reusing already existing broker proxy for '%s'", hostPort.toHostString()));
            proxy = previous;
        } else {
            Logger.trace(String.format("[Proxy create] Creating new broker proxy for '%s'", hostPort.toHostString()));
            proxy = new DefaultKafkaClusterProxy(hostPort);
        }

        reinitialize(hostPort, proxy);
        return proxy;

    }

    public static void reinitialize(HostPortValue hostPort, KafkaClusterProxy proxy) throws ClusterConfigurationError,
                                                                                                   ExecutionException,
                                                                                                   TimeoutException,
                                                                                                   InterruptedException {
        final Admin kafkaClientAdminClient = createKafkaClientAdminClient(hostPort);
        final TopicAdmin topicAdmin = new TopicAdmin(kafkaClientAdminClient);
        proxy.refresh(topicAdmin, kafkaClientAdminClient);
    }

    private static Admin createKafkaClientAdminClient(HostPortValue hostPort) {
        final Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, hostPort.toHostString());
        return Admin.create(props);
    }
}
