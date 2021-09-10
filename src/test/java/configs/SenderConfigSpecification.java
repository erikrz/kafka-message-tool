package configs;

import org.testng.annotations.Test;

import application.model.modelobjects.KafkaSenderConfig;

import static org.assertj.core.api.Assertions.assertThat;

public class SenderConfigSpecification {
    @Test
    public void shouldAssignNewUuid() {
        final KafkaSenderConfig config = new KafkaSenderConfig();
        final String oldUuid = config.getUuid();
        config.assignNewUuid();
        assertThat(oldUuid).isNotEqualTo(config.getUuid());
    }
    @Test
    public void shouldReturnObjectTypeName() {
        assertThat(new KafkaSenderConfig().getObjectTypeName()).isEqualTo("Message sender configuration");
    }
}
