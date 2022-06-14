package application.kafka.cluster;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

import com.google.common.base.Throwables;

import application.constants.ApplicationConstants;
import application.exceptions.TopicAlreadyExistsError;
import application.exceptions.TopicMarkedForDeletionError;
import application.kafka.dto.ClusterTopicInfo;
import application.kafka.dto.TopicToAdd;
import application.logging.Logger;
import application.utils.AppUtils;
import kafka.server.KafkaConfig;

import static java.util.Collections.singleton;

public class TopicAdmin {

    private final Admin kafkaClientsAdminClient;

    TopicAdmin(Admin kafkaClientsAdminClient) {
        this.kafkaClientsAdminClient = kafkaClientsAdminClient;
    }

    public void deleteTopic(String topicName) throws Exception {

        Logger.trace(String.format("Deleting topic '%s'", topicName));
        final DeleteTopicsResult result = kafkaClientsAdminClient.deleteTopics(Collections.singletonList(topicName));
        for (Map.Entry<String, KafkaFuture<Void>> entry : result.topicNameValues().entrySet()) {
            entry.getValue().get(ApplicationConstants.DELETE_TOPIC_FUTURE_GET_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
    }

    public void createNewTopic(TopicToAdd topicToAdd) throws Exception {
        final String topicName = topicToAdd.getTopicName();
        Logger.trace(String.format("Creating topic '%s' [partitions:%d, replication factor:%d, cleanup policy:%s]",
                topicName,
                topicToAdd.getPartitions(),
                topicToAdd.getReplicationFactor(),
                topicToAdd.getCleanupPolicy()));

        final NewTopic newTopic = new NewTopic(topicName,
                topicToAdd.getPartitions(),
                (short) topicToAdd.getReplicationFactor());
        newTopic.configs(topicConfigsMapFromTopicToAdd(topicToAdd));

        final CreateTopicsResult result = kafkaClientsAdminClient.createTopics(Collections.singletonList(newTopic));
        interpretCreateTopicResult(topicName, result);
    }

    private void interpretCreateTopicResult(String topicName, CreateTopicsResult result) throws Exception {
        for (Map.Entry<String, KafkaFuture<Void>> entry : result.values().entrySet()) {
            try {
                entry.getValue().get(ApplicationConstants.FUTURE_GET_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                Logger.trace(String.format("Topic '%s' created", entry.getKey()));
            } catch (InterruptedException | ExecutionException e) {
                if (Throwables.getRootCause(e) instanceof TopicExistsException) {
                    if (!topicExistsCheckWithClusterQuery(entry.getKey(), kafkaClientsAdminClient)) {
                        final String msg = String.format("Topic '%s' already exists but is marked for deletion.%n%n" +
                                        "!!! Note !!!%nIf broker property '%s' is set to 'false' it will NEVER be deleted",
                                entry.getKey(),
                                KafkaConfig.DeleteTopicEnableProp());
                        Logger.trace(msg);
                        throw new TopicMarkedForDeletionError(msg);
                    } else {
                        final String msg = String.format("Topic '%s' already exist", topicName);
                        Logger.trace(msg);
                        throw new TopicAlreadyExistsError(msg);
                    }
                }
            }
        }
    }


    private static Map<String, String> topicConfigsMapFromTopicToAdd(TopicToAdd topicToAdd) {
        final Map<String, String> configs = new HashMap<>();
        if (topicToAdd.getCleanupPolicy() == TopicCleanupPolicy.COMPACT) {
            configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT);
        }
        return configs;
    }

    private static boolean topicExistsCheckWithClusterQuery(String topicName,
                                                            Admin
                                                                    kafkaClientsAdminClient) throws Exception {

        try {
            final DescribeTopicsResult result = kafkaClientsAdminClient.describeTopics(singleton(topicName));
            result.allTopicNames().get(ApplicationConstants.FUTURE_GET_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return true;
        } catch (ExecutionException e) {
            if (Throwables.getRootCause(e) instanceof UnknownTopicOrPartitionException) {
                return false;
            } else {
                throw e;
            }
        }
    }

    public void describeTopics(Consumer<ClusterTopicInfo> topicInfoConsumer) {
        final ListTopicsResult listTopicsResult =
                kafkaClientsAdminClient.listTopics(new ListTopicsOptions().listInternal(false));
        listTopicsResult.listings().whenComplete((topicListings, throwable) -> {
            if (throwable != null) {
                Logger.error("Exception when reading list of topics: ", throwable);
            }
            if (topicListings != null) {
                Logger.debug(String.format("describeTopics.listings %s", topicListings));
            }
        });

        listTopicsResult.names().whenComplete((topicNames, throwable) -> {
            final DescribeTopicsResult describeTopicsResult = kafkaClientsAdminClient.describeTopics(topicNames);
            describeTopicsResult.allTopicNames().whenComplete((stringTopicDescriptionMap, throwable1) -> {
                for (Map.Entry<String, TopicDescription> entry : stringTopicDescriptionMap.entrySet()) {
                    final TopicDescription topicDescription = entry.getValue();
                    final var configResource =
                            new ConfigResource(ConfigResource.Type.TOPIC, topicDescription.name());
                    final DescribeConfigsResult topicConfigEntries =
                            kafkaClientsAdminClient.describeConfigs(Collections.singleton(configResource));
                    final Set<ConfigEntry> entries = new HashSet<>();
                    topicConfigEntries.all().whenComplete((configResourceConfigMap, throwable2) -> {
                        final var gatheredEntries = configResourceConfigMap.get(configResource).entries();
                        if (gatheredEntries != null) {
                            entries.addAll(gatheredEntries);
                            Logger.debug(String.format("Config entries for topic '%s' : %n%s", topicDescription.name(),
                                    AppUtils.configEntriesToPrettyString(entries)));
                        }
                    });
                    final ClusterTopicInfo clusterTopicInfo = new ClusterTopicInfo(topicDescription.name(),
                            topicDescription.partitions(), entries);
                    topicInfoConsumer.accept(clusterTopicInfo);
                }
            });
        });
    }
}
