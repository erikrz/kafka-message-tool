package application.controllers.confighandlers;

import java.util.Optional;

import application.constants.ApplicationConstants;
import application.controllers.ControllerProvider;
import application.controllers.helpers.ListViewActionsHandler;
import application.controllers.helpers.TabPaneSelectionInformer;
import application.controllers.helpers.TemplateGuiActionsHandler;
import application.customfxwidgets.listenerconfig.ListenerConfigView;
import application.customfxwidgets.listenerconfig.ToFileSaver;
import application.kafka.listener.Listeners;
import application.model.FromPojoConverter;
import application.model.ModelDataProxy;
import application.model.ToPojoConverter;
import application.model.modelobjects.KafkaListenerConfig;
import application.model.modelobjects.KafkaTopicConfig;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;

public class ListenerConfigGuiActionsHandler extends TemplateGuiActionsHandler<KafkaListenerConfig> {

    private final ListViewActionsHandler<KafkaListenerConfig> listViewActionsHandler;
    private final ModelDataProxy modelDataProxy;
    private final ControllerProvider controllerProvider;
    private final AnchorPane parentPane;
    private final FromPojoConverter fromPojoConverter;
    private final ToFileSaver toFileSaver;
    private final ListView<KafkaTopicConfig> topicConfigs;
    private final Listeners activeConsumers;

    public ListenerConfigGuiActionsHandler(TabPaneSelectionInformer tabSelectionInformer,
                                           ListViewActionsHandler<KafkaListenerConfig> listViewActionsHandler,
                                           ModelDataProxy modelDataProxy,
                                           ControllerProvider controllerProvider,
                                           AnchorPane parentPane,
                                           ListView<KafkaTopicConfig> topicConfigs,
                                           Listeners activeConsumers,
                                           ToFileSaver toFileSaver) {
        super(tabSelectionInformer, listViewActionsHandler);

        this.listViewActionsHandler = listViewActionsHandler;
        this.modelDataProxy = modelDataProxy;
        this.controllerProvider = controllerProvider;
        this.parentPane = parentPane;
        this.topicConfigs = topicConfigs;
        this.activeConsumers = activeConsumers;
        this.fromPojoConverter = new FromPojoConverter(modelDataProxy);
        this.toFileSaver = toFileSaver;
    }


    @Override
    protected void loadController(KafkaListenerConfig config) {

        final ListenerConfigView controller = controllerProvider.getListenerConfigGuiController(config,
                parentPane,
                activeConsumers,
                listViewActionsHandler::refresh,
                topicConfigs.getItems(),
                toFileSaver);
        controller.display();
    }


    @Override
    protected void addToModelData() {
        final String configName = ApplicationConstants.DEFAULT_LISTENER_CONFIG_NAME;
        KafkaListenerConfig newConfig = new KafkaListenerConfig(configName);
        final Optional<KafkaListenerConfig> selectedModelObject = listViewActionsHandler.getSelectedModelObject();
        if (selectedModelObject.isPresent()) {
            newConfig = fromPojoConverter.fromPojo(ToPojoConverter.toPojoFrom(selectedModelObject.get()));
            newConfig.assignNewUuid();
        }
        newConfig.setName(configName);

        modelDataProxy.addConfig(newConfig);
    }


}
