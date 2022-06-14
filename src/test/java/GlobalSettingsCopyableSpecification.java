import org.testng.annotations.Test;

import application.persistence.GlobalSettings;
import autofixture.publicinterface.Any;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalSettingsCopyableSpecification {
    @Test
    public void shouldCopyAllFieldsFromOneGlobalSettingsObjectToAnother() {
        // GIVEN
        GlobalSettings a = Any.anonymous(GlobalSettings.class);
        GlobalSettings toBeFilled = new GlobalSettings();

        // WHEN
        toBeFilled.fillFrom(a);

        // THEN
        assertThat(toBeFilled).isEqualTo(a).usingRecursiveComparison();
    }

    @Test
    public void shouldIgnoreNullSettingsWhileFilling() {
        // GIVEN

        GlobalSettings toBeFilled = new GlobalSettings();

        // WHEN / THEN
        toBeFilled.fillFrom(null);
    }
}
