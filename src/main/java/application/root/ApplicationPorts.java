package application.root;

import application.kafka.listener.Listeners;
import application.kafka.sender.KafkaMessageSender;

public interface ApplicationPorts extends Restartable {
    KafkaMessageSender getSender();

    Listeners getListeners();

}
