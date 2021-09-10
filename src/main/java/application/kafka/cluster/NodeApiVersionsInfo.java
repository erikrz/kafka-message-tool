package application.kafka.cluster;

import org.apache.kafka.clients.admin.ConfigEntry;


public class NodeApiVersionsInfo {

    private final ConfigEntry protocolVersion;

    public NodeApiVersionsInfo(ConfigEntry configEntry) {
        this.protocolVersion = configEntry;
    }

    public boolean doesApiSupportDescribeConfig() {
        if (protocolVersion.value().startsWith("0.11")) {
            return true;
        }
        return Integer.parseInt(protocolVersion.value().substring(0, 1)) > 0;
    }
}
