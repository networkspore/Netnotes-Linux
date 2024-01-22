package com.netnotes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class NetworkTimer extends Network implements NoteInterface {

    public static String DESCRIPTION = "Network Timer enables timed updates for components.";
    public static String SUMMARY = "Create timers at various intervals and time spans.";
    public static String NAME = "Network Timer";
    public static String NETWORK_ID = "NETWORK_TIMER";
    private File logFile = new File("timer-log.txt");

    private ArrayList<TimerData> m_timersList = new ArrayList<TimerData>();

    private Stage m_timerStage = null;

    public NetworkTimer(NetworksData networksData) {
        super(getAppIcon(), NAME, NETWORK_ID, networksData);

        m_timersList.add(new TimerData(null, this));

    }

    public NetworkTimer(JsonObject jsonObject, NetworksData networksData) {
        super(getAppIcon(), NAME, NETWORK_ID, networksData);

    }

    @Override
    public void open() {
        if (m_timerStage == null) {
            String title = getName() + ": Timers";
            double timerStageWidth = 310;
            double timerStageHeight = 500;
            double buttonHeight = 100;

            m_timerStage = new Stage();
            m_timerStage.getIcons().add(getIcon());
            m_timerStage.setResizable(false);
            m_timerStage.initStyle(StageStyle.UNDECORATED);
            m_timerStage.setTitle(title);

            Button closeBtn = new Button();
            closeBtn.setOnAction(closeEvent -> {
                m_timerStage.close();
                m_timerStage = null;
            });

            HBox titleBox = App.createTopBar(getIcon(), title, closeBtn, m_timerStage);

            ImageView addImage = new ImageView(App.addImg);
            addImage.setFitHeight(10);
            addImage.setPreserveRatio(true);

            Tooltip addTip = new Tooltip("New");
            addTip.setShowDelay(new javafx.util.Duration(100));
            addTip.setFont(App.txtFont);

            VBox layoutVBox = new VBox(titleBox);
            layoutVBox.setPadding(new Insets(0, 5, 0, 5));
            VBox.setVgrow(layoutVBox, Priority.ALWAYS);

            Scene timerScene = new Scene(layoutVBox, timerStageWidth, timerStageHeight);
            timerScene.setFill(null);
            //  bodyBox.prefHeightProperty().bind(timerScene.heightProperty() - 40 - 100);
            timerScene.getStylesheets().add("/css/startWindow.css");
            m_timerStage.setScene(timerScene);

            VBox timerBox = getButtonGrid();

            Region growRegion = new Region();

            VBox.setVgrow(growRegion, Priority.ALWAYS);

            VBox bodyBox = new VBox(timerBox, growRegion);

            ScrollPane scrollPane = new ScrollPane(bodyBox);
            scrollPane.prefViewportWidthProperty().bind(timerScene.widthProperty());
            scrollPane.prefViewportHeightProperty().bind(timerScene.heightProperty().subtract(140));
            scrollPane.setId("bodyBox");

            Button addButton = new Button("New");
            // addButton.setGraphic(addImage);
            addButton.setId("menuBarBtn");
            addButton.setPadding(new Insets(2, 6, 2, 6));
            addButton.setTooltip(addTip);
            addButton.setPrefWidth(timerStageWidth / 2);
            addButton.setPrefHeight(buttonHeight);

            Tooltip removeTip = new Tooltip("Remove");
            removeTip.setShowDelay(new javafx.util.Duration(100));
            removeTip.setFont(App.txtFont);

            Button removeButton = new Button("Remove");
            // removeButton.setGraphic(addImage);
            removeButton.setId("menuBarBtnDisabled");
            removeButton.setPadding(new Insets(2, 6, 2, 6));
            removeButton.setTooltip(removeTip);
            removeButton.setDisable(true);
            removeButton.setPrefWidth(timerStageWidth / 2);
            removeButton.setPrefHeight(buttonHeight);

            HBox menuBox = new HBox(addButton, removeButton);
            menuBox.setId("blackMenu");
            menuBox.setAlignment(Pos.CENTER_LEFT);
            menuBox.setPadding(new Insets(5, 5, 5, 5));
            menuBox.setPrefHeight(buttonHeight);

            addButton.setOnAction(event -> {
                showAddTimerStage();
            });

            layoutVBox.getChildren().addAll(scrollPane, menuBox);

            Platform.runLater(() -> m_timerStage.show());
        } else {
            if (m_timerStage.isIconified()) {
                m_timerStage.setIconified(false);
            }

            Platform.runLater(() -> m_timerStage.show());
        }
    }

    private void showAddTimerStage() {

    }

    private VBox getButtonGrid() {
        return new VBox();
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/stopwatch-30.png");
    }

    public static Image getAppIcon() {
        return new Image("/assets/stopwatch.png");
    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        JsonElement subjectElement = note.get("subject");
        JsonElement timerIdElement = note.get("timerId");

        if (subjectElement != null && subjectElement.isJsonPrimitive()) {
            String subject = subjectElement.getAsString();

            try {
                Files.writeString(logFile.toPath(), "\n" + note.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {

            }

            switch (subject) {
                case "OPEN":
                    open();
                    return true; // break;

                case "UNSUBSCRIBE":

                    break;
                case "SUBSCRIBE":

                    break;
                case "GET_TIMERS":

                    getTimers(onSucceeded, onFailed);
                    return true;

            }

        }

        return false;
    }

    public JsonObject getTimersJson() {
        JsonObject timers = new JsonObject();
        timers.addProperty("subject", "TIMERS");
        timers.addProperty("networkId", getNetworkId());
        timers.add("availableTimers", getTimersJsonArray());
        return timers;
    }

    public void getTimers(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        Task<JsonObject> task = new Task<JsonObject>() {
            @Override
            public JsonObject call() {

                return getTimersJson();

            }

        };
        task.setOnFailed(onFailed);
        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public JsonArray getTimersJsonArray() {
        JsonArray timers = new JsonArray();

        for (TimerData timerData : m_timersList) {
            timers.add(timerData.getJsonObject());
        }

        return timers;
    }

}
