package application.kafka.cluster;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import application.constants.ApplicationConstants;
import application.customfxwidgets.consumergroupview.ConsumerGroupDetailRecord;
import application.exceptions.ClusterConfigurationError;
import application.kafka.dto.AssignedConsumerInfo;
import application.kafka.dto.ClusterNodeInfo;
import application.kafka.dto.TopicAggregatedSummary;
import application.kafka.dto.TopicAlterableProperties;
import application.kafka.dto.TopicToAdd;
import application.kafka.dto.UnassignedConsumerInfo;
import application.logging.Logger;
import application.utils.AppUtils;
import application.utils.HostPortValue;
import application.utils.HostnameUtils;
import javafx.collections.ObservableList;

import static application.constants.ApplicationConstants.APPLICATION_NAME;
import static java.util.Collections.singleton;

public class DefaultKafkaClusterProxy implements KafkaClusterProxy {
    public static final String NOT_FOUND_STRING = "NOT_FOUND";

    private final HostPortValue hostPort;
    private final ClusterStateSummary clusterSummary = new ClusterStateSummary();
    private final ClusterNodesProperties clusterNodesProperties = new ClusterNodesProperties();
    private final Map<Node, NodeApiVersionsInfo> brokerApiVersions = new HashMap<>();
    private Admin admin;
    private TopicAdmin topicAdmin;

    @Override
    public void refresh(TopicAdmin topicAdmin, Admin admin)
            throws ClusterConfigurationError, InterruptedException, ExecutionException, TimeoutException {
        closeOldDependencies();
        clearClusterSummary();
        assignNewDependencies(topicAdmin, admin);
        throwIfInvalidConfigMakesClusterUnusable();
        fetchClusterStateSummary();
    }

    @Override
    public TopicAlterableProperties getAlterableTopicProperties(String topicName) {
        final TopicAlterableProperties t = new TopicAlterableProperties(topicName);
        t.setRetentionMilliseconds(
                Integer.parseUnsignedInt(
                        clusterSummary.getTopicPropertyByName(topicName,
                                TopicConfig.RETENTION_MS_CONFIG)));
        return t;
    }

    @Override
    public void updateTopic(TopicAlterableProperties topicDetails) {
        AlterConfigOp op = new AlterConfigOp(new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG,
                String.valueOf(topicDetails.getRetentionMilliseconds())), AlterConfigOp.OpType.SET);
        Map<ConfigResource, Collection<AlterConfigOp>> configMap = new HashMap<>();
        configMap.put(new ConfigResource(ConfigResource.Type.TOPIC, topicDetails.getTopicName()),
                Collections.singletonList(op));
        admin.incrementalAlterConfigs(configMap);
    }

    private void assignNewDependencies(TopicAdmin topicAdmin, Admin admin) {
        this.topicAdmin = topicAdmin;
        this.admin = admin;
    }

    public DefaultKafkaClusterProxy(HostPortValue hostPort) {
        Logger.trace("New DefaultKafkaClusterProxy: real Hash : " + AppUtils.realHash(this));
        this.hostPort = hostPort;
    }

    @Override
    public void reportInvalidClusterConfigurationTo(Consumer<String> problemReporter) {
        StringBuilder builder = new StringBuilder();
        getInconsistentBrokerPropertiesErrorMessage().ifPresent(builder::append);
        final String errorMsg = builder.toString();
        if (!StringUtils.isBlank(errorMsg)) {
            problemReporter.accept(errorMsg);
        }
    }


    public void closeOldDependencies() {
        if (admin != null) {
            Logger.trace("Closing kafka admin proxy");
            admin.close(Duration.ofMillis(ApplicationConstants.CLOSE_CONNECTION_TIMEOUT_MS));
        }
        Logger.trace("Closing done");
    }

    @Override
    public Set<AssignedConsumerInfo> getConsumersForTopic(String topicName) {
        return clusterSummary.getConsumersForTopic(topicName);
    }

    @Override
    public Set<UnassignedConsumerInfo> getUnassignedConsumersInfo() {
        return clusterSummary.getUnassignedConsumersInfo();
    }

    @Override
    public Set<ClusterNodeInfo> getNodesInfo() {
        return clusterSummary.getNodesInfo();
    }

    @Override
    public ObservableList<TopicsOffsetInfo> getTopicOffsetsInfo() {
        return clusterSummary.getTopicOffsetInfo();
    }

    @Override
    public Set<TopicAggregatedSummary> getAggregatedTopicSummary() {
        return clusterSummary.getAggregatedTopicSummary();
    }

    @Override
    public List<ConsumerGroupDetailRecord> getConsumerGroupDetails() {
        return clusterSummary.getConsumerGroupsDetails();

    }

    @Override
    public int partitionsForTopic(String topicName) {
        return clusterSummary.partitionsForTopic(topicName);
    }


    @Override
    public TriStateConfigEntryValue isTopicAutoCreationEnabled() {
        if (clusterNodesProperties.isEmpty()) {
            return TriStateConfigEntryValue.False;
        }
        return clusterNodesProperties.topicAutoCreationEnabled();
    }

    @Override
    public TriStateConfigEntryValue isTopicDeletionEnabled() {
        if (clusterNodesProperties.isEmpty()) {
            return TriStateConfigEntryValue.False;
        }
        return clusterNodesProperties.topicDeletionEnabled();
    }

    @Override
    public boolean hasTopic(String topicName) {
        return clusterSummary.hasTopic(topicName);
    }

    @Override
    public Set<ConfigEntry> getTopicProperties(String topicName) {
        return clusterSummary.getTopicProperties(topicName);
    }

    @Override
    public void createTopic(TopicToAdd topicToAdd) throws Exception {
        topicAdmin.createNewTopic(topicToAdd);
    }

    @Override
    public void deleteTopic(String topicName) throws Exception {
        topicAdmin.deleteTopic(topicName);
    }

    private static Optional<Long> getOptionalOffsetForPartition(Map<TopicPartition, OffsetAndMetadata> offsets,
                                                                TopicPartition topicPartition) {
        Logger.trace(String.format("Searching for offset for %s in %s", topicPartition, offsets));
        if (!offsets.containsKey(topicPartition)) {
            Logger.trace("Offset not found");
            return Optional.empty();
        }

        final OffsetAndMetadata obj = offsets.get(topicPartition);
        try {
            Logger.trace(String.format("Found : %s", obj.offset()));
            return Optional.of(obj.offset());
        } catch (Exception e) {
            Logger.trace(String.format("Offset could not be interpreted as Long ('%s')", obj));
            return Optional.empty();
        }

    }

    private static String getOffsetForPartition(Map<TopicPartition, OffsetAndMetadata> offsets,
                                                TopicPartition topicPartition) {
        final Optional<Long> optLong = getOptionalOffsetForPartition(offsets, topicPartition);
        if (!optLong.isPresent()) {
            return NOT_FOUND_STRING;
        }
        return String.valueOf(optLong.get());
    }

    private boolean doesNodeSupportDescribeConfigApi(Node node) {
        final NodeApiVersionsInfo info = brokerApiVersions.getOrDefault(node, null);
        return info != null && info.doesApiSupportDescribeConfig();
    }

    private void fetchClusterStateSummary() {
        describeCluster();
        describeTopics();
        describeConsumers();
    }

    private void describeTopics() {
        topicAdmin.describeTopics(clusterSummary::addTopicInfo);
    }

    private void clearClusterSummary() {
        clusterSummary.clear();
        clusterNodesProperties.clear();
    }

    private Optional<String> getInconsistentBrokerPropertiesErrorMessage() {
        final Set<String> misconfiguredProperties = clusterNodesProperties.getAllPropertiesThatDiffersBetweenNodes();
        if (!misconfiguredProperties.isEmpty()) {
            final String msg = String.format("Cluster configuration is inconsistent!%n" +
                    "Below properties are different between nodes but should be the same:%n%n" +
                    "[%s] ", String.join(", ",
                    misconfiguredProperties.stream()
                            .map(e -> String.format("%s", e))
                            .toArray(String[]::new)));
            return Optional.of(msg);
        }
        return Optional.empty();
    }

    private void throwIfInvalidConfigMakesClusterUnusable() throws ClusterConfigurationError {
        try {
            Logger.trace("calling kafkaAdminClient.findAllBrokers() ");
            final Collection<Node> nodes = admin.describeCluster().nodes().get();
            final List<String> advertisedListeners = new ArrayList<>();
            for (Node node : nodes) {
                final String host1 = node.host();
                final int port = node.port();
                final String advertisedListener = String.format("%s:%d", host1, port);
                Logger.debug("Found advertised listener: " + advertisedListener);
                advertisedListeners.add(advertisedListener);

                Logger.trace(String.format("Checking if advertised listener '%s' is reachable", host1));
                if (HostnameUtils.isHostnameReachable(host1, ApplicationConstants.HOSTNAME_REACHABLE_TIMEOUT_MS)) {
                    Logger.trace("Yes");
                    return;
                }
                Logger.trace("No");
            }
            final String msg = String.format("Cluster config for 'advertised.listeners' is invalid.%n%n" +
                            "* None of advertised listeners '%s' are reachable from outside world.%n" +
                            "* Producers/consumers will be unable to use this kafka cluster " +
                            "(e.g. will not connect properly).%n" +
                            "* This application (%s) cannot fetch broker configuration", advertisedListeners,
                    APPLICATION_NAME);
            throw new ClusterConfigurationError(msg);
        } catch (RuntimeException e) {
            Logger.trace(e);
            e.printStackTrace();
            throw e;

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private List<String> getConsumerGroupIds() {
        try {
            return admin.listConsumerGroups().all().get().stream().map(ConsumerGroupListing::groupId)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private Map<TopicPartition, OffsetAndMetadata> getPartitionsForConsumerGroup(String consumerGroup) {

        try {
            Map<TopicPartition, OffsetAndMetadata> topicPartitionOffsetAndMetadataMap =
                    admin.listConsumerGroupOffsets(consumerGroup).partitionsToOffsetAndMetadata().get();
            Logger.debug(String.format("Fetched partitions for consumer group '%s' -> '%s'", consumerGroup,
                    topicPartitionOffsetAndMetadataMap));
            return topicPartitionOffsetAndMetadataMap;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return Collections.emptyMap();
    }

    private void describeConsumers() {

        clusterSummary.setConsumerGroupIds(getConsumerGroupIds());
        final List<TopicsOffsetInfo> topicOffsetsInfo = new ArrayList<>();

        clusterSummary.getConsumerGroupIds().forEach(consumerGroupId -> {
            final Map<TopicPartition, OffsetAndMetadata> offsetForPartition =
                    getPartitionsForConsumerGroup(consumerGroupId);
            final List<TopicsOffsetInfo> topicOffsetsFor = getTopicOffsetsFor(consumerGroupId, offsetForPartition);
            topicOffsetsInfo.addAll(topicOffsetsFor);
            Collection<MemberDescription> summaries;
            try {
                summaries = admin.describeConsumerGroups(Collections.singletonList(consumerGroupId))
                        .all().get().values().stream()
                        .map(ConsumerGroupDescription::members)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toCollection(LinkedList::new));
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                summaries = Collections.emptyList();
            }
            summaries.forEach(consumerSummary -> {
                Logger.debug("Consumer summary " + consumerSummary);

                final Set<TopicPartition> topicPartitions = consumerSummary.assignment().topicPartitions();
                if (topicPartitions.isEmpty()) {
                    final UnassignedConsumerInfo consumerInfo =
                            getUnassignedConsumerInfo(consumerGroupId, consumerSummary);
                    clusterSummary.addUnassignedConsumerInfo(consumerInfo);
                } else {
                    topicPartitions.forEach(topicPartition -> {
                        final AssignedConsumerInfo consumerInfo = getAssignedConsumerInfo(consumerGroupId,
                                offsetForPartition,
                                consumerSummary,
                                topicPartition);

                        clusterSummary.addAssignedConsumerInfo(consumerInfo);

                    });
                }
            });
        });
        clusterSummary.setTopicOffsetInfo(topicOffsetsInfo);
    }

    private List<TopicsOffsetInfo> getTopicOffsetsFor(String consumerGroupId,
                                                      Map<TopicPartition, OffsetAndMetadata> topicPartitionsCurrentOffset) {
        final Set<TopicPartition> topicPartitions = topicPartitionsCurrentOffset.keySet();
        final List<TopicsOffsetInfo> result = new ArrayList<>();

        final KafkaConsumer<String, String> consumer = createOffsetInfoConsumerFor(consumerGroupId);
        final Map<TopicPartition, Long> beggingOffsets = consumer.beginningOffsets(topicPartitions);
        final Map<TopicPartition, Long> endOffsets = consumer.endOffsets(topicPartitions);

        for (Map.Entry<TopicPartition, Long> entry : beggingOffsets.entrySet()) {

            final TopicPartition topicPartition = entry.getKey();

            if (!endOffsets.containsKey(topicPartition)) {
                continue;
            }

            String currentOffset = NOT_FOUND_STRING;
            String lag = NOT_FOUND_STRING;

            final Optional<Long> optionalOffsetForPartition =
                    getOptionalOffsetForPartition(topicPartitionsCurrentOffset,
                            topicPartition);

            final String topicName = topicPartition.topic();
            final String partition = String.valueOf(topicPartition.partition());
            final Long startOffsetLong = entry.getValue();
            final String beggingOffset = String.valueOf(startOffsetLong);
            final Long endOffsetLong = endOffsets.get(topicPartition);
            final String endOffset = String.valueOf(endOffsetLong);
            final String msgCount = String.valueOf(endOffsetLong - startOffsetLong);

            if (optionalOffsetForPartition.isPresent()) {
                final Long currentOffsetLong = optionalOffsetForPartition.get();
                currentOffset = String.valueOf(currentOffsetLong);
                lag = String.valueOf(endOffsetLong - currentOffsetLong);
            }

            final TopicsOffsetInfo topicsOffsetInfo = new TopicsOffsetInfo(topicName,
                    beggingOffset,
                    endOffset,
                    consumerGroupId,
                    partition,
                    msgCount,
                    currentOffset,
                    lag);
            result.add(topicsOffsetInfo);
        }

        Logger.debug("Topic offsets: " + result);
        return result;
    }

    private KafkaConsumer<String, String> createOffsetInfoConsumerFor(String consumerGroupIds) {
        final Properties props = new Properties();
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupIds);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, hostPort.toHostString());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(props);
    }

    private AssignedConsumerInfo getAssignedConsumerInfo(String consumerGroupId,
                                                         Map<TopicPartition, OffsetAndMetadata> offsetForPartition,
                                                         MemberDescription consumerSummary,
                                                         TopicPartition topicPartition) {
        return AssignedConsumerInfo.builder()
                .consumerGroupId(consumerGroupId)
                .consumerId(consumerSummary.consumerId())
                .clientId(consumerSummary.clientId())
                .host(consumerSummary.host())
                .topic(topicPartition.topic())
                .partition(String.valueOf(topicPartition.partition()))
                .offset(getOffsetForPartition(offsetForPartition, topicPartition))
                .build();
    }

    private UnassignedConsumerInfo getUnassignedConsumerInfo(String consumerGroupId,
                                                             MemberDescription consumerSummary) {
        return UnassignedConsumerInfo.builder()
                .consumerGroupId(consumerGroupId)
                .consumerId(consumerSummary.consumerId())
                .clientId(consumerSummary.clientId())
                .host(consumerSummary.host())
                .build();
    }

    private void describeCluster() {
        final DescribeClusterResult describeClusterResult = admin.describeCluster();
        describeClusterResult.clusterId().whenComplete((id, throwable) -> clusterSummary.setClusterId(id));
        describeClusterResult.controller().whenComplete((node, throwable) -> {
            final int controllerNodeId = node.id();
            describeClusterResult.nodes().whenComplete((nodes, throwable1) -> describeNodes(nodes, controllerNodeId));
        });
    }

    private void describeNodes(Collection<Node> nodes, int controllerNodeId) {
        for (Node node : nodes) {
            saveApiVersionsForNodes(node);
        }
        for (Node node : nodes) {
            describeNodeConfig(controllerNodeId, node);
        }
    }

    private void describeNodeConfig(int controllerNodeId, Node node) {
        if (!doesNodeSupportDescribeConfigApi(node)) {
            Logger.warn(String.format("Node '%s' does not support describeConfig api. Cannot show cluster properties",
                    node));
            return;
        }
        final var brokerConfigResource = new ConfigResource(ConfigResource.Type.BROKER, String.valueOf(node.id()));
        DescribeConfigsResult configs = admin.describeConfigs(singleton(brokerConfigResource));
        configs.all().whenComplete((configResourceConfigMap, throwable) ->
                configResourceConfigMap.forEach((configResource, config) -> {
                    var clusterNodeInfo = new ClusterNodeInfo(node.id() == controllerNodeId,
                            node.idString(), new HashSet<>(config.entries()));
                    clusterSummary.addNodeInfo(clusterNodeInfo);
                    clusterNodeInfo.getEntries().forEach(entry ->
                            clusterNodesProperties.addConfigEntry(entry.name(), entry.value()));
                }));
    }

    private void saveApiVersionsForNodes(Node node) {
        final var configResource = new ConfigResource(ConfigResource.Type.BROKER, node.idString());
        admin.describeConfigs(Collections.singletonList(configResource))
                .all().whenComplete((configResourceConfigMap, throwable) -> {
                    Optional<ConfigEntry> protocolVersion = configResourceConfigMap.values().stream()
                            .map(Config::entries)
                            .flatMap(Collection::stream)
                            .filter(configEntry -> configEntry.name().equals("inter.broker.protocol.version"))
                            .findFirst();
                    if (protocolVersion.isPresent()) {
                        brokerApiVersions.put(node, new NodeApiVersionsInfo(protocolVersion.get()));
                        printApiVersionForNode(node, protocolVersion.get());
                    }
                });
    }

    private void printApiVersionForNode(Node node, ConfigEntry configEntry) {
        Logger.debug(
                String.format("%n### Api version for node %s ###%nProtocol version: '%s'", node, configEntry.value()));
    }
}


