package application.root;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.skin.TextAreaSkin;

public class FxTextAreaWrapper implements TextAreaWrapper {

    private final MenuItem saveToFilePopupMenuItem = new MenuItem("Save to file");
    private final TextArea fxTextArea;

    public FxTextAreaWrapper(TextArea fxTextArea) {
        this.fxTextArea = fxTextArea;
    }

    @Override
    public void setText(String text) {
        fxTextArea.setText(text);
    }

    @Override
    public void appendText(String text) {
        fxTextArea.appendText(text);
    }

    @Override
    public void clear() {
        fxTextArea.clear();
    }

    @Override
    public Node asNode() {
        return fxTextArea;
    }

    @Override
    public void setPopupSaveToAction(Executable saveContentToFile) {
        saveToFilePopupMenuItem.setOnAction(event -> saveContentToFile.execute());
        TextAreaSkin customContextSkin = new TextAreaSkin(fxTextArea);
        fxTextArea.setSkin(customContextSkin);
        if (fxTextArea.getContextMenu() == null) {
            fxTextArea.setContextMenu(new ContextMenu(new SeparatorMenuItem(), saveToFilePopupMenuItem));
        } else {
            fxTextArea.getContextMenu().getItems().add(new SeparatorMenuItem());
            fxTextArea.getContextMenu().getItems().add(saveToFilePopupMenuItem);
        }
    }
}
