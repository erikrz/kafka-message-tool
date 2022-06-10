package application.customfxwidgets.topicpropertieswindow;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import application.constants.ApplicationConstants;
import application.customfxwidgets.ConfigEntriesView;
import application.kafka.cluster.TopicsOffsetInfo;
import application.utils.GuiUtils;
import application.utils.TableUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import static application.customfxwidgets.CustomFxWidgetsLoader.loadAnchorPane;


public final class TopicPropertiesWindow extends AnchorPane {
    private static final String FXML_FILE = "TopicPropertiesView.fxml";
    private static TopicPropertiesWindow instance;
    ObservableList<TopicsOffsetInfo> privateInfos = FXCollections.observableArrayList();
    private Stage stage = new Stage();
    @FXML
    private Button closeButton;
    @FXML
    private Label titleLabel;
    @FXML
    private AnchorPane topicPropertiesAnchorPane;
    @FXML
    private AnchorPane consumerGroupsAnchorPane;
    @FXML
    private TableColumn<TopicsOffsetInfo, String> consumerGroupColumn;
    @FXML
    private TableColumn<TopicsOffsetInfo, String> partitionColumn;
    @FXML
    private TableColumn<TopicsOffsetInfo, String> beginOffsetColumn;
    @FXML
    private TableColumn<TopicsOffsetInfo, String> endOffsetColumn;
    @FXML
    private TableColumn<TopicsOffsetInfo, String> totalColumn;
    @FXML
    private TableView<TopicsOffsetInfo> topicOffsetsTableView;
    @FXML
    private TableColumn<TopicsOffsetInfo, String> currentOffsetColumn;
    @FXML
    private TableColumn<TopicsOffsetInfo, String> lagColumn;
    private double stageWidth = -1d;
    private double stageHeight = -1d;
    private ObservableList<TopicsOffsetInfo> parentInfos;
    private String topicName;
    private ConfigEntriesView entriesView;
    private ObservableList<TopicsOffsetInfo> observablesOffsetsFromCaller;

    private TopicPropertiesWindow(ObservableList<TopicsOffsetInfo> topicOffsetsInfo) throws IOException {

        observablesOffsetsFromCaller = topicOffsetsInfo;

        loadAnchorPane(this, FXML_FILE);
        configureTable();


        topicOffsetsInfo.addListener(new ListChangeListener<TopicsOffsetInfo>() {
            @Override
            public void onChanged(Change<? extends TopicsOffsetInfo> c) {
                refresh(topicName, entriesView, observablesOffsetsFromCaller);
            }
        });
    }

    public static TopicPropertiesWindow get(String topicName,
                                            ConfigEntriesView entriesView,
                                            ObservableList<TopicsOffsetInfo> topicOffsetsInfo) throws IOException {
        if (instance == null) {
            instance = new TopicPropertiesWindow(topicOffsetsInfo);
        }
        instance.refresh(topicName, entriesView, topicOffsetsInfo);
        return instance;
    }

    public void show() {
        restoreWidthAndHeight();
        stage.show();
    }

    private void refresh(String topicName,
                         ConfigEntriesView entriesView,
                         ObservableList<TopicsOffsetInfo> topicOffsetsInfo) {
        this.topicName = topicName;
        this.entriesView = entriesView;

        setupTitleLabel(topicName);
        setupTopicPropertiesAnchorPane(entriesView);
        refreshTopicOffsetsView(topicOffsetsInfo);

    }

    private void refreshTopicOffsetsView(ObservableList<TopicsOffsetInfo> topicOffsetsInfo) {
        final List<TopicsOffsetInfo> filtered = topicOffsetsInfo
            .stream()
            .filter(e -> e.getTopicName().equals(this.topicName))
            .collect(Collectors.toList());
        privateInfos.setAll(filtered);
        Platform.runLater(() -> {
            topicOffsetsTableView.getSortOrder().clear();
            topicOffsetsTableView.getSortOrder().add(consumerGroupColumn);
        });
    }

    private void configureTable() {
        consumerGroupColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getConsumerGroup()));
        partitionColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getPartition()));
        beginOffsetColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getBeginOffset()));
        endOffsetColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getEndOffset()));
        totalColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getTopicPartitionMsgCount()));

        currentOffsetColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getCurrentOffset()));
        lagColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLag()));

        topicOffsetsTableView.setItems(privateInfos);
        topicOffsetsTableView.getSortOrder().add(consumerGroupColumn);
        TableUtils.installCopyPasteHandlerForSingleCell(topicOffsetsTableView);
        TableUtils.autoResizeColumns(topicOffsetsTableView);
    }

    private void setTableContent(List<TopicsOffsetInfo> topicOffsetsInfo) {
        privateInfos.setAll(topicOffsetsInfo);
    }

    private void restoreWidthAndHeight() {
        if (stageWidth > 0) {
            stage.setWidth(stageWidth);
        }

        if (stageHeight > 0) {
            stage.setHeight(stageHeight);
        }
    }

    @FXML
    private void initialize() {
        GuiUtils.addApplicationIcon(stage);
        initializeStage();
    }

    private void setupTopicPropertiesAnchorPane(ConfigEntriesView entriesView) {
        topicPropertiesAnchorPane.getChildren().clear();
        topicPropertiesAnchorPane.getChildren().add(entriesView);
    }

    private void setupTitleLabel(String topicName) {
        titleLabel.setText(String.format("Information for topic '%s'", topicName));
        stage.setTitle(titleLabel.getText());
    }

    @FXML
    private void closeOnAction() {
        stage.close();
    }


    private void initializeStage() {
        final Scene scene = new Scene(this);
        scene.getStylesheets().add(TopicPropertiesWindow.class.getClassLoader().getResource(
                ApplicationConstants.GLOBAL_CSS_FILE_NAME).toExternalForm());
        scene.setRoot(this);
        stage.setScene(scene);
        stage.centerOnScreen();


        stage.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                stageWidth = newValue.doubleValue();
            }
        });

        stage.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                stageHeight = newValue.doubleValue();
            }
        });
    }

}
