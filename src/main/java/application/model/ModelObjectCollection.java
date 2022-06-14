package application.model;

import java.util.List;
import java.util.Optional;

import javafx.collections.ObservableList;

public interface ModelObjectCollection<ModelObject> {
    ObservableList<ModelObject> getObservables();

    boolean hasObjectWithUuid(String uuid);

    boolean add(ModelObject o);

    Optional<ModelObject> getByUuid(String uuid);

    List<String> getNames();
}
