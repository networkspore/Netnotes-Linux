package com.netnotes;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Optional;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoMarkets extends Network implements NoteInterface {

    public final static String DESCRIPTION = "Get updates from suported markets.";
    public final static String SUMMARY = "";
    public final static String NAME = "Ergo Markets";
    public final static String NETWORK_ID = "ERGO_MARKETS";

    //private File logFile = new File("netnotes-log.txt");

    private File m_dataFile = null;
    private File m_appDir = null;
    private Stage m_stage = null;
    
    private final SimpleLongProperty m_timeStampProperty = new SimpleLongProperty(0);


    public ErgoMarkets(NoteInterface noteInterface) {
        super(getAppIcon(), NAME, NETWORK_ID, noteInterface);
        setStageWidth(300);
        setStageHeight(500);
        setup(null);
        getLastUpdated().set(LocalDateTime.now());
    }

    public ErgoMarkets(JsonObject json, NoteInterface noteInterface) {
        super(getAppIcon(), NAME, NETWORK_ID, noteInterface);
        setup(json);
    }

    private void setup(JsonObject json) {
        File ergoNetworkDir = new File(getNetworksData().getAppData().getAppDir().getAbsolutePath() + "/" + ErgoNetwork.NAME);

    
        m_appDir = new File(ergoNetworkDir.getAbsolutePath() + "/" + NAME);
        m_dataFile = new File(m_appDir.getAbsolutePath() + "/" + NAME + ".dat");

        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }

   

            ErgoMarketsList marketsList = new ErgoMarketsList(this);
            marketsList.add(new KucoinErgoMarketsData(marketsList));
            marketsList.add(new SpectrumErgoMarketsData(marketsList));
            marketsList.defaultIdProperty().set(SpectrumFinance.NETWORK_ID);
            marketsList.save();

        } 
        getNetworksData().getAppData().appKeyProperty().addListener((obs, oldVal, newVal) -> {( 
            
            new ErgoMarketsList(oldVal, this)).save();
        
        });
    }

    @Override
    public void open() {
        showStage();
    }

    public SimpleLongProperty timeStampProperty(){
        return m_timeStampProperty;
    }

    public static Image getAppIcon() {
        return new Image("/assets/ergoChart.png");
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/ergoChart-30.png");
    }

    public File getDataFile() {
        return m_dataFile;
    }

    public void showStage() {
        if (m_stage == null) {
            String title = getName();
            ErgoMarketsList marketsList = new ErgoMarketsList(this);
            
            double buttonHeight = 100;

            m_stage = new Stage();
            m_stage.getIcons().add(getIcon());
            m_stage.setResizable(false);
            m_stage.initStyle(StageStyle.UNDECORATED);
            m_stage.setTitle(title);

            Button closeBtn = new Button();

            Button maximizeButton = new Button();

            HBox titleBox = App.createTopBar(getSmallAppIcon(), maximizeButton, closeBtn, m_stage);
            Region menuSpacer = new Region();
            HBox.setHgrow(menuSpacer, Priority.ALWAYS);

            HBox menuBar = new HBox(menuSpacer);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 0, 1, 5));

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setId("bodyBox");

             HBox menuBarPadding = new HBox(menuBar);
            menuBarPadding.setId("darkBox");
            HBox.setHgrow(menuBarPadding, Priority.ALWAYS);
            menuBarPadding.setPadding(new Insets(0,2,4,2));

            
            Button addButton = new Button("Add");
            addButton.setId("menuBarBtn");
            addButton.setPadding(new Insets(2, 6, 2, 6));
            addButton.setPrefWidth(getStageWidth() / 2);
            addButton.setPrefHeight(buttonHeight);
            addButton.setOnAction(e->{
                marketsList.showAddStage();
            });

            Button removeButton = new Button("Remove");
            removeButton.setId("menuBarBtnDisabled");
            removeButton.setPadding(new Insets(2, 6, 2, 6));
            removeButton.setDisable(true);
            removeButton.setPrefWidth(getStageWidth() / 2);
            removeButton.setPrefHeight(buttonHeight);
            removeButton.setOnAction(e->{
                String selectedId = marketsList.selectedIdProperty().get();
                if(selectedId != null){
                    ErgoMarketsData marketData = marketsList.getMarketsData(selectedId);
                    if(marketData != null){
                        String info = "Would you like to remove: '" + marketData.getName() + "'\n\n";
                        Alert a = new Alert(AlertType.NONE, info, ButtonType.YES, ButtonType.NO);
                        a.initOwner(m_stage);
                        a.setHeaderText("Remove");

                        Optional<ButtonType> result = a.showAndWait();

                        if(result.isPresent() && result.get() == ButtonType.YES){
                            marketsList.remove(selectedId);
                       
                        }
                    }
                }
            });

            marketsList.selectedIdProperty().addListener((obs,oldval,newval)->{
                if(newval != null){
                    removeButton.setId("menuBarBtn");
                    removeButton.setDisable(false);
                }else{
                    removeButton.setId("menuBarBtnDisabled");
                    removeButton.setDisable(true);
                }
            });

            HBox menuBox = new HBox(addButton, removeButton);
            menuBox.setId("blackMenu");
            menuBox.setAlignment(Pos.CENTER_LEFT);
            menuBox.setPadding(new Insets(5, 5, 5, 5));
            menuBox.setPrefHeight(buttonHeight);

            VBox layoutBox = new VBox(titleBox, menuBar, scrollPane, menuBox);

            Scene mainScene = new Scene(layoutBox, getStageWidth(), getStageHeight());
            mainScene.setFill(null);
            mainScene.getStylesheets().add("/css/startWindow.css");
            m_stage.setScene(mainScene);
           scrollPane.prefViewportWidthProperty().bind(mainScene.widthProperty());
            scrollPane.prefViewportHeightProperty().bind(mainScene.heightProperty().subtract(140));
            scrollPane.setPadding(new Insets(5, 5, 5, 5));

            SimpleDoubleProperty gridWidth = new SimpleDoubleProperty(m_stage.getWidth());
            SimpleDoubleProperty scrollWidth = new SimpleDoubleProperty(0);
            gridWidth.bind(mainScene.widthProperty().subtract(15));

            VBox gridBox = marketsList.getGridBox(gridWidth, scrollWidth);

            scrollPane.setContent(gridBox);
            scrollPane.setOnMouseClicked(e->{
                marketsList.selectedIdProperty().set(null);
            });
            ResizeHelper.addResizeListener(m_stage, 300, 300, Double.MAX_VALUE, Double.MAX_VALUE);
            Runnable doClose = ()->{
                marketsList.shutdown();
                m_stage = null;
            };
            m_stage.setOnCloseRequest(e -> {
                doClose.run();
            });

            closeBtn.setOnAction(closeEvent -> {
              
                m_stage.close();
                doClose.run();
            });
            shutdownNowProperty().addListener((obs, oldVal, newVal) -> {
        
                m_stage.close();
                doClose.run();
            });

            gridBox.heightProperty().addListener((obs, oldVal, newVal) -> {
                double val = newVal.doubleValue();
                if (val > scrollPane.prefViewportHeightProperty().doubleValue()) {
                    scrollWidth.set(40);
                } else {
                    scrollWidth.set(0);
                }
            });

            addButton.prefWidthProperty().bind(mainScene.widthProperty().divide(2));
            removeButton.prefWidthProperty().bind(mainScene.widthProperty().divide(2));
            m_stage.show();
        } else {
            if (m_stage.isIconified()) {
                m_stage.setIconified(false);
            }
            m_stage.show();
            m_stage.toFront();
        }

    }

    @Override
    public IconButton getButton(String iconStyle) {

        IconButton iconButton = new IconButton(iconStyle.equals(IconStyle.ROW) ? getSmallAppIcon() : getAppIcon(), iconStyle.equals(IconStyle.ROW) ? getName() : getText(), iconStyle) {
            @Override
            public void open() {
                getOpen();
            }
        };

        return iconButton;
    }

    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

        JsonElement subjecElement = note.get("subject");

        if (subjecElement != null) {
            switch (subjecElement.getAsString()) {
              
            
            }
        }
        return false;
    }

    public ErgoMarketsList getErgoMarketsList(){
        return new ErgoMarketsList(this);
    }
}
