package application.customfxwidgets.aboutwindow;

import java.io.IOException;

import application.constants.ApplicationConstants;
import application.utils.ApplicationVersionProvider;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import static application.customfxwidgets.CustomFxWidgetsLoader.loadAnchorPane;

public class AboutWindow extends AnchorPane {

    public static final int BIG_FONT_SIZE = 20;

    public static final int MEDIUM_FONT_SIZE = 15;

    private static final Font BIG_HELVETICA_FONT = Font.font("Helvetica", FontWeight.BOLD, BIG_FONT_SIZE);

    private static final Font MEDIUM_HELVETICA_FONT = Font.font("Helvetica", FontWeight.BOLD, MEDIUM_FONT_SIZE);
    private static final String FXML_FILE = "AboutWindow.fxml";

    private static final Hyperlink ORIGINAL_SOURCE_CODE_URL =
            new Hyperlink(ApplicationConstants.ORIGINAL_GITHUB_WEBSITE);

    private static final Hyperlink FORK_SOURCE_CODE_URL = new Hyperlink(ApplicationConstants.FORK_GITHUB_WEBSITE);
    private final Stage stage;
    private final Application fxApplication;

    @FXML
    private Button closeButton;

    @FXML
    private TextFlow textFlow;

    public AboutWindow(Window owner, Application fxApplication) throws IOException {
        this.fxApplication = fxApplication;
        this.stage = new Stage();
        loadAnchorPane(this, FXML_FILE);
        setupStage(owner);
        initAboutContent();
        configureLinks();
        configureCloseButton();
    }

    public void show() {
        stage.showAndWait();
    }

    private void configureCloseButton() {
        closeButton.setOnAction(e -> stage.close());
    }

    private void configureLinks() {
        ORIGINAL_SOURCE_CODE_URL.setOnAction(event -> {
            fxApplication.getHostServices().showDocument(ORIGINAL_SOURCE_CODE_URL.getText());
            event.consume();
        });
    }

    private void setupStage(Window owner) {
        final Scene scene = new Scene(this);
        scene.getStylesheets().add(
                AboutWindow.class.getClassLoader().getResource(ApplicationConstants.GLOBAL_CSS_FILE_NAME)
                        .toExternalForm());
        scene.setRoot(this);
        stage.setScene(scene);
        stage.setTitle("About...");
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
    }


    private void initAboutContent() {
        final Text appNameText = getAppNameText();
        final Text appVersionText = getAppVersionText();
        final Text originalAuthorText = getOriginalAuthorText();
        final Text forkAuthorText = getForkAuthorText();
        textFlow.setTextAlignment(TextAlignment.CENTER);
        textFlow.getChildren().addAll(appNameText, new Text("\n"),
                appVersionText, new Text("\n\n"),
                forkAuthorText, new Text("\n\n"),
                new Text("Fork Source: "), FORK_SOURCE_CODE_URL, new Text("\n\n"),
                originalAuthorText, new Text("\n\n"),
                new Text("Original Source: "), ORIGINAL_SOURCE_CODE_URL);

    }

    private Text getAppVersionText() {
        final Text text = new Text(String.format("[v %s]", ApplicationVersionProvider.get()));
        text.setFont(BIG_HELVETICA_FONT);
        text.setTextAlignment(TextAlignment.CENTER);
        return text;
    }

    private Text getForkAuthorText() {
        Text text2 = new Text("Fork Author: " + ApplicationConstants.FORK_AUTHOR);
        text2.setFont(MEDIUM_HELVETICA_FONT);
        return text2;
    }

    private Text getOriginalAuthorText() {
        Text text2 = new Text("Original Author: " + ApplicationConstants.ORIGINAL_AUTHOR);
        text2.setFont(MEDIUM_HELVETICA_FONT);
        return text2;
    }

    private Text getAppNameText() {
        Text appVersionText = new Text(ApplicationConstants.APPLICATION_NAME);
        appVersionText.setFont(BIG_HELVETICA_FONT);
        appVersionText.setTextAlignment(TextAlignment.CENTER);
        return appVersionText;
    }

}
