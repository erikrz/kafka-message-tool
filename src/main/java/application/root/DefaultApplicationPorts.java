package application.root;

import application.kafka.listener.Listeners;
import application.kafka.sender.KafkaMessageSender;

public class DefaultApplicationPorts implements ApplicationPorts {
    private final KafkaMessageSender sender;
    private Listeners listeners;

    DefaultApplicationPorts(KafkaMessageSender sender, Listeners listeners) {
        this.sender = sender;
        this.listeners = listeners;
    }

    @Override
    public KafkaMessageSender getSender() {
        return sender;
    }

    @Override
    public Listeners getListeners() {
        return listeners;
    }

    @Override
    public void stop() {
        listeners.stopAll();
    }

    @Override
    public void start() {

    }
}
