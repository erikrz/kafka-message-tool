package application.kafka.cluster;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConfigEntry;

import application.customfxwidgets.consumergroupview.ConsumerGroupDetailRecord;
import application.exceptions.ClusterConfigurationError;
import application.kafka.dto.AssignedConsumerInfo;
import application.kafka.dto.ClusterNodeInfo;
import application.kafka.dto.TopicAggregatedSummary;
import application.kafka.dto.TopicAlterableProperties;
import application.kafka.dto.TopicToAdd;
import application.kafka.dto.UnassignedConsumerInfo;
import javafx.collections.ObservableList;

public interface KafkaClusterProxy {

    void reportInvalidClusterConfigurationTo(Consumer<String> problemReporter);

    void createTopic(TopicToAdd topicToAdd) throws Exception;

    void deleteTopic(String topicName) throws Exception;

    TriStateConfigEntryValue isTopicAutoCreationEnabled();

    TriStateConfigEntryValue isTopicDeletionEnabled();

    boolean hasTopic(String topicName);

    Set<ConfigEntry> getTopicProperties(String topicName);

    Set<AssignedConsumerInfo> getConsumersForTopic(String topicName);

    Set<UnassignedConsumerInfo> getUnassignedConsumersInfo();

    Set<ClusterNodeInfo> getNodesInfo();

    ObservableList<TopicsOffsetInfo> getTopicOffsetsInfo();

    Set<TopicAggregatedSummary> getAggregatedTopicSummary();

    List<ConsumerGroupDetailRecord> getConsumerGroupDetails();

    int partitionsForTopic(String topicName);

    void refresh(TopicAdmin topicAdmin,
                 Admin kafkaClientAdminClient) throws
            ClusterConfigurationError,
            InterruptedException,
            ExecutionException,
            TimeoutException;

    TopicAlterableProperties getAlterableTopicProperties(String topicName);

    void updateTopic(TopicAlterableProperties topicDetails);
}
