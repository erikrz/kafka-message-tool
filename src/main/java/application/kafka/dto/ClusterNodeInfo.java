package application.kafka.dto;

import java.util.Set;

import org.apache.kafka.clients.admin.ConfigEntry;

public class ClusterNodeInfo {

    private boolean isController;
    private String nodeId;
    private Set<ConfigEntry> entries;

    public ClusterNodeInfo(boolean isController, String nodeId, Set<ConfigEntry> entries) {
        this.isController = isController;
        this.nodeId = nodeId;
        this.entries = entries;
    }

    public Set<ConfigEntry> getEntries() {
        return entries;
    }

    public String getNodeId() {
        return nodeId;
    }

    public boolean isController() {
        return isController;
    }
}
