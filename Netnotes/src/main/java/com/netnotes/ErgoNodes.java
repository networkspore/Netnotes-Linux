package com.netnotes;

import java.awt.Rectangle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;

import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoNodes extends Network implements NoteInterface {

  //  private File logFile = new File("netnotes-log.txt");

    public final static String NAME = "Ergo Nodes";
    public final static String DESCRIPTION = "Ergo Nodes allows you to configure your access to the Ergo blockchain";
    public final static String NETWORK_ID = "ERGO_NODES";
    public final static String SUMMARY = "";

    public final static int MAINNET_PORT = 9053;
    public final static int TESTNET_PORT = 9052;
    public final static int EXTERNAL_PORT = 9030;

    private ScheduledFuture<?> m_lastExecution = null;

    private File m_appDir = null;
    private File m_dataFile = null;

    private ErgoNodesList m_ergoNodesList;

    public ErgoNodes(ErgoNetwork ergoNetwork) {
        super(getAppIcon(), NAME, NETWORK_ID, ergoNetwork);
        m_appDir = new File(ergoNetwork.getAppDir() + "/" + NAME);
        
        m_dataFile = new File(m_appDir.getAbsolutePath() + "/" + NAME + ".dat");
   
        setStageWidth(750);
        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }

            
        
        m_ergoNodesList = new ErgoNodesList(this);}
        getLastUpdated().set(LocalDateTime.now());
      
      
    }

    public ErgoNodes(JsonObject jsonObject, ErgoNetwork ergoNetwork) {
        super(getAppIcon(), NAME, NETWORK_ID, ergoNetwork);
        m_appDir = new File(ergoNetwork.getAppDir() + "/" + NAME);
        m_dataFile = new File(m_appDir.getAbsolutePath() + "/" + NAME + ".dat");
        m_ergoNodesList = new ErgoNodesList(this);

        openJson(jsonObject);


    }

    public static Image getAppIcon() {
        return new Image("/assets/ergoNodes-100.png");
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/ergoNodes-30.png");
    }

    public File getDataFile() {
        return m_dataFile;
    }

    public File getAppDir() {
        return m_appDir;
    }


    private void openJson(JsonObject json) {

        // JsonElement directoriesElement = json == null ? null : json.get("directories");
        JsonElement stageElement = json == null ? null : json.get("stage");


        /*  if (directoriesElement != null && directoriesElement.isJsonObject()) {
            JsonObject directoriesObject = directoriesElement.getAsJsonObject();
            if (directoriesObject != null) {
                JsonElement appDirElement = directoriesObject.get("app");

                m_appDir = appDirElement == null ? new File(ErgoNetwork.ERGO_NETWORK_DIR.getCanonicalPath() + "/" + NAME) : new File(appDirElement.getAsString());

            }
        } */
        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }

        }

    

        if (stageElement != null && stageElement.isJsonObject()) {
            JsonObject stageObject = stageElement.getAsJsonObject();

            JsonElement widthElement = stageObject.get("width");
            JsonElement heightElement = stageObject.get("height");
            JsonElement stagePrevWidthElement = stageObject.get("prevWidth");
            JsonElement stagePrevHeightElement = stageObject.get("prevHeight");
            JsonElement stageMaximizedElement = stageObject.get("maximized");

            boolean maximized = stageMaximizedElement != null && stageMaximizedElement.isJsonPrimitive() ? stageMaximizedElement.getAsBoolean() : false;

            if (!maximized) {
                setStageWidth(widthElement != null && widthElement.isJsonPrimitive() ? widthElement.getAsDouble() : SMALL_STAGE_WIDTH);
                setStageHeight(heightElement != null && heightElement.isJsonPrimitive() ? heightElement.getAsDouble() : DEFAULT_STAGE_HEIGHT);
            } else {
                double prevWidth = stagePrevWidthElement != null && stagePrevWidthElement.isJsonPrimitive() ? stagePrevWidthElement.getAsDouble() : SMALL_STAGE_WIDTH;
                double prevHeight = stagePrevHeightElement != null && stagePrevHeightElement.isJsonPrimitive() ? stagePrevHeightElement.getAsDouble() : DEFAULT_STAGE_HEIGHT;

                setStageWidth(prevWidth);
                setStageHeight(prevHeight);

                setStagePrevWidth(prevWidth);
                setStagePrevHeight(prevHeight);

            }
        }

    }

    @Override
    public void open() {
        showStage();
    }

    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

       /* JsonElement subjecElement = note.get("subject");
        JsonElement networkTypeElement = note.get("networkType");
        JsonElement nodeIdElement = note.get("nodeId"); */

        return false;
    }

    @Override
    public IconButton getButton(String iconStyle) {

        IconButton iconButton = new IconButton(iconStyle.equals(IconStyle.ROW) ? getSmallAppIcon() : getAppIcon(), iconStyle.equals(IconStyle.ROW) ? getName() : getText(), iconStyle) {
            @Override
            public void open() {
                getOpen();
            }
        };

        if (iconStyle.equals(IconStyle.ROW)) {
            iconButton.setContentDisplay(ContentDisplay.LEFT);
            iconButton.setImageWidth(30);
        } else {
            iconButton.setContentDisplay(ContentDisplay.TOP);
            iconButton.setTextAlignment(TextAlignment.CENTER);
        }

        return iconButton;
    }

    private Stage m_stage = null;

    public void showStage() {
        if (m_stage == null) {
            String title = getName();

           // double buttonHeight = 100;

            m_stage = new Stage();
            m_stage.getIcons().add(getIcon());
            m_stage.setResizable(false);
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(title);

            Button closeBtn = new Button();

            Button maximizeBtn = new Button();

            HBox titleBox = App.createTopBar(getSmallAppIcon(), maximizeBtn, closeBtn, m_stage);

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setId("bodyBox");

        

            BufferedButton addBtn = new BufferedButton("/assets/add-outline-white-40.png", App.MENU_BAR_IMAGE_WIDTH);
            addBtn.setOnAction((e) -> m_ergoNodesList.showAddNodeStage());
    
            
            HBox menuBar = new HBox(addBtn);
            menuBar.setId("menuBar");
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setPadding(new Insets(1,5,1,5));
            HBox.setHgrow(menuBar, Priority.ALWAYS);
       
            HBox menuBarPadding = new HBox(menuBar);
            menuBarPadding.setId("darkBox");
            HBox.setHgrow(menuBarPadding, Priority.ALWAYS);
            menuBarPadding.setPadding(new Insets(0,0,4,0));

            VBox bodyBox = new VBox(menuBarPadding, scrollPane);
            bodyBox.setPadding(new Insets(0,3,2,3));
            VBox layoutBox = new VBox(titleBox, bodyBox);
  
            Runnable updateMenuBar = () -> Platform.runLater(() -> {
                String selectedId = m_ergoNodesList.selectedIdProperty().get();
                if (selectedId == null) {
                    menuBar.getChildren().clear();
                    menuBar.getChildren().add(addBtn);   
                    
                } else {
                    ErgoNodeData ergNodeData = m_ergoNodesList.getErgoNodeData(selectedId);

                    if (ergNodeData == null) {
                        m_ergoNodesList.selectedIdProperty().set(null);
                    } else {
                        menuBar.getChildren().clear();
                        HBox nodeDataBar = ergNodeData.getMenuBar();
                        menuBar.getChildren().addAll(addBtn, nodeDataBar);
                        
                    }
                }
            });

            m_ergoNodesList.selectedIdProperty().addListener((obs, oldval, newVal) -> updateMenuBar.run());
            updateMenuBar.run();
          
            Scene mainScene = new Scene(layoutBox, getStageWidth(), getStageHeight());
            mainScene.setFill(null);
            mainScene.getStylesheets().add("/css/startWindow.css");
            m_stage.setScene(mainScene);

            Rectangle rect = getNetworksData().getMaximumWindowBounds();

            scrollPane.prefViewportWidthProperty().bind(mainScene.widthProperty().subtract(4));
            scrollPane.prefViewportHeightProperty().bind(mainScene.heightProperty().subtract(titleBox.heightProperty()).subtract(menuBarPadding.heightProperty()).subtract(5));
            scrollPane.setPadding(new Insets(5, 5, 5, 5));
            scrollPane.setOnMouseClicked(e -> {
                getErgoNodesList().setselectedId(null);
            });

            SimpleDoubleProperty gridWidth = new SimpleDoubleProperty(m_stage.getWidth());
            SimpleDoubleProperty scrollWidth = new SimpleDoubleProperty(0);
            gridWidth.bind(mainScene.widthProperty().subtract(20));
            m_stage.show();

            VBox gridBox = m_ergoNodesList.getGridBox(gridWidth, scrollWidth);

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });

            Runnable setUpdated = () -> {
                getLastUpdated().set(LocalDateTime.now());
            };

            mainScene.widthProperty().addListener((obs, oldVal, newVal) -> {
                setStageWidth(newVal.doubleValue());

                if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                    m_lastExecution.cancel(false);
                }

                m_lastExecution = executor.schedule(setUpdated, EXECUTION_TIME, TimeUnit.MILLISECONDS);
            });

            mainScene.heightProperty().addListener((obs, oldVal, newVal) -> {
                setStageHeight(newVal.doubleValue());

                if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                    m_lastExecution.cancel(false);
                }

                m_lastExecution = executor.schedule(setUpdated, EXECUTION_TIME, TimeUnit.MILLISECONDS);
            });

            maximizeBtn.setOnAction(maxEvent -> {
                boolean maximized = m_stage.isMaximized();

                setStageMaximized(!maximized);

                if (!maximized) {
                    setStagePrevWidth(m_stage.getWidth());
                    setStagePrevHeight(m_stage.getHeight());
                }
                setUpdated.run();
                m_stage.setMaximized(!maximized);
            });

            ResizeHelper.addResizeListener(m_stage, 300, 300, rect.getWidth(), rect.getHeight());

            scrollPane.setContent(gridBox);

            m_stage.setOnCloseRequest(e -> {
                shutdownNowProperty().set(LocalDateTime.now());
            });

            closeBtn.setOnAction(closeEvent -> {
                shutdownNowProperty().set(LocalDateTime.now());
            });
            shutdownNowProperty().addListener((obs, oldVal, newVal) -> {
                m_ergoNodesList.shutdown();
                m_stage.close();
                m_stage = null;
            });

            Runnable updateScrollWidth = () -> {
                double val = gridBox.heightProperty().doubleValue();
                if (val > scrollPane.prefViewportHeightProperty().doubleValue()) {
                    scrollWidth.set(40);
                } else {
                    scrollWidth.set(0);
                }
            };

            gridBox.heightProperty().addListener((obs, oldVal, newVal) -> updateScrollWidth.run());

            

            updateScrollWidth.run();

            if (getStageMaximized()) {
                m_stage.setMaximized(true);
            }

        } else {
            if (m_stage.isIconified()) {
                m_stage.setIconified(false);
            }
            m_stage.show();
            m_stage.toFront();
        }

    }

    @Override
    public JsonObject getJsonObject() {
        JsonObject json = super.getJsonObject();
        json.add("stage", getStageJson());
       
        return json;
    }

    public ErgoNodesList getErgoNodesList() {
        return m_ergoNodesList;
    }

    @Override
    public void remove() {
        super.remove();
        if (m_ergoNodesList != null && m_ergoNodesList.getErgoLocalNode() != null) {
            m_ergoNodesList.getErgoLocalNode().stop();
        }
    }
}
