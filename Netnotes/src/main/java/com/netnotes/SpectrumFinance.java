package com.netnotes;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.utils.Utils;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SpectrumFinance extends Network implements NoteInterface {

    public static String DESCRIPTION = "Spectrum Finance is a cross-chain decentralized exchange (DEX).";
    public static String SUMMARY = "";
    public static String NAME = "Spectrum Finance";
    public final static String NETWORK_ID = "SPECTRUM_FINANCE";

    public static String API_URL = "https://api.spectrum.fi";

    private File logFile = new File("netnotes-log.txt");

    public static java.awt.Color POSITIVE_HIGHLIGHT_COLOR = new java.awt.Color(0xff3dd9a4, true);
    public static java.awt.Color POSITIVE_COLOR = new java.awt.Color(0xff028A0F, true);

    public static java.awt.Color NEGATIVE_COLOR = new java.awt.Color(0xff9A2A2A, true);
    public static java.awt.Color NEGATIVE_HIGHLIGHT_COLOR = new java.awt.Color(0xffe96d71, true);
    public static java.awt.Color NEUTRAL_COLOR = new java.awt.Color(0x111111);

    
    private File m_appDir = null;
    private File m_dataFile = null;
    private File m_dataDir = null;

    private Stage m_appStage = null;


    private SimpleObjectProperty<JsonObject> m_cmdObjectProperty = new SimpleObjectProperty<>(null);

   


    public SpectrumFinance(NetworksData networksData) {
        this(null, networksData);
        setup(null);
        addListeners();
    }

    public SpectrumFinance(JsonObject jsonObject, NetworksData networksData) {
        super(getAppIcon(), NAME, NETWORK_ID, networksData);

        setup(jsonObject);
        addListeners();
    }

    public void addListeners(){
       
    }

    public SimpleObjectProperty<JsonObject> cmdObjectProperty() {

        return m_cmdObjectProperty;
    }



  

    public File getAppDir() {
        return m_appDir;
    }

    public static Image getAppIcon() {
        return new Image("/assets/spectrum-150.png");
    }

    public static Image getSmallAppIcon() {
        return new Image("/assets/spectrumFinance.png");
    }

    public File getDataFile() {
        return m_dataFile;
    }


    private void setup(JsonObject jsonObject) {


        String fileString = null;
        String appDirFileString = null;
        if (jsonObject != null) {
            JsonElement appDirElement = jsonObject.get("appDir");
      
            JsonElement dataFileElement = jsonObject.get("dataFile");

            fileString = dataFileElement == null ? null : dataFileElement.toString();

            appDirFileString = appDirElement == null ? null : appDirElement.getAsString();

        }

        m_appDir = appDirFileString == null ? new File(getNetworksData().getAppDir().getAbsolutePath() + "/" + NAME) : new File(appDirFileString);

        if (!m_appDir.isDirectory()) {

            try {
                Files.createDirectories(m_appDir.toPath());
            } catch (IOException e) {
                Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.CLOSE);
                a.show();
            }

        }

   
        m_dataFile = new File(fileString == null ? m_appDir.getAbsolutePath() + "/" + NAME + ".dat" : fileString);
        m_dataDir = new File(m_appDir.getAbsolutePath() + "/data");
        if(!m_dataDir.isDirectory()){
            try {
                Files.createDirectories(m_dataDir.toPath());
            } catch (IOException e) {
          
            }
        }
    }

    public void open() {
        super.open();
      
        showAppStage();
    }

    public Stage getAppStage() {
        return m_appStage;
    }

    public File getDataDir(){
        
        return m_dataDir;
    }



    private void showAppStage() {
        if (m_appStage == null) {
            

            SpectrumDataList spectrumData = new SpectrumDataList(this);

    
      

            double appStageWidth = 450;
            double appStageHeight = 600;

            m_appStage = new Stage();
            m_appStage.getIcons().add(SpectrumFinance.getSmallAppIcon());
            m_appStage.initStyle(StageStyle.UNDECORATED);
            m_appStage.setTitle(NAME);

            Button closeBtn = new Button();

            Runnable runClose = () -> {

        
                spectrumData.closeAll();
                spectrumData.removeUpdateListener();

                m_appStage = null;

            };

            closeBtn.setOnAction(closeEvent -> {
                m_appStage.close();
                runClose.run();
            });

            Button maxBtn = new Button();

            HBox titleBox = App.createTopBar(getSmallAppIcon(), maxBtn, closeBtn, m_appStage);
            titleBox.setPadding(new Insets(7, 8, 5, 10));

            m_appStage.titleProperty().bind(Bindings.concat(NAME, " - ", spectrumData.statusProperty()));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Tooltip refreshTip = new Tooltip("Refresh");
            refreshTip.setShowDelay(new javafx.util.Duration(100));
            refreshTip.setFont(App.txtFont);

         

            BufferedMenuButton sortTypeButton = new BufferedMenuButton("/assets/filter.png", App.MENU_BAR_IMAGE_WIDTH);

            MenuItem sortLiquidityItem = new MenuItem(SpectrumSort.SortType.LIQUIDITY_VOL);
            MenuItem sortBaseVolItem = new MenuItem(SpectrumSort.SortType.BASE_VOL);
            MenuItem sortQuoteVolItem = new MenuItem(SpectrumSort.SortType.QUOTE_VOL);
            MenuItem sortLastPriceItem = new MenuItem(SpectrumSort.SortType.LAST_PRICE);
          
            sortTypeButton.getItems().addAll(sortLiquidityItem, sortBaseVolItem, sortQuoteVolItem, sortLastPriceItem);

            Runnable updateSortTypeSelected = () ->{
                sortLiquidityItem.setId(null);
                sortBaseVolItem.setId(null);
                sortQuoteVolItem.setId(null);
                sortLastPriceItem.setId(null);

                switch(spectrumData.getSortMethod().getType()){
                    case SpectrumSort.SortType.LIQUIDITY_VOL:
                        sortLiquidityItem.setId("selectedMenuItem");
                    break;
                    case SpectrumSort.SortType.BASE_VOL:
                        sortBaseVolItem.setId("selectedMenuItem");
                    break;
                    case SpectrumSort.SortType.QUOTE_VOL:
                        sortQuoteVolItem.setId("selectedMenuItem");
                    break;
                    case SpectrumSort.SortType.LAST_PRICE:
                        sortLastPriceItem.setId("selectedMenuItem");
                    break;
                }

                spectrumData.sort();
                spectrumData.updateGridBox();
                spectrumData.getLastUpdated().set(LocalDateTime.now());
            };

           // updateSortTypeSelected.run();

            sortLiquidityItem.setOnAction(e->{
                SpectrumSort sortMethod = spectrumData.getSortMethod();
                sortMethod.setType(sortLiquidityItem.getText());
                updateSortTypeSelected.run();
            });

            sortBaseVolItem.setOnAction(e->{
                SpectrumSort sortMethod = spectrumData.getSortMethod();
                sortMethod.setType(sortBaseVolItem.getText());
                updateSortTypeSelected.run();
            });

            sortQuoteVolItem.setOnAction(e->{
                SpectrumSort sortMethod = spectrumData.getSortMethod();
                sortMethod.setType(sortQuoteVolItem.getText());
                updateSortTypeSelected.run();
            });

            sortLastPriceItem.setOnAction(e->{
                SpectrumSort sortMethod = spectrumData.getSortMethod();
                sortMethod.setType(sortLastPriceItem.getText());
                updateSortTypeSelected.run();
            });


            BufferedButton sortDirectionButton = new BufferedButton(spectrumData.getSortMethod().isAsc() ? "/assets/sortAsc.png" : "/assets/sortDsc.png", App.MENU_BAR_IMAGE_WIDTH);
            sortDirectionButton.setOnAction(e->{
                SpectrumSort sortMethod = spectrumData.getSortMethod();
                sortMethod.setDirection(sortMethod.isAsc() ? SpectrumSort.SortDirection.DSC : SpectrumSort.SortDirection.ASC);
                sortDirectionButton.setImage(new Image(sortMethod.isAsc() ? "/assets/sortAsc.png" : "/assets/sortDsc.png"));
                spectrumData.sort();
                spectrumData.updateGridBox();
                spectrumData.getLastUpdated().set(LocalDateTime.now());
            });

            BufferedButton swapTargetButton = new BufferedButton(spectrumData.getSortMethod().isTargetSwapped()? "/assets/targetSwapped.png" : "/assets/targetStandard.png", App.MENU_BAR_IMAGE_WIDTH);
            swapTargetButton.setOnAction(e->{
                SpectrumSort sortMethod = spectrumData.getSortMethod();
                sortMethod.setSwapTarget(sortMethod.isTargetSwapped() ? SpectrumSort.SwapMarket.STANDARD : SpectrumSort.SwapMarket.SWAPPED);
                swapTargetButton.setImage(new Image(spectrumData.getSortMethod().isTargetSwapped()? "/assets/targetSwapped.png" : "/assets/targetStandard.png"));
                spectrumData.sort();
                spectrumData.updateGridBox();
                spectrumData.getLastUpdated().set(LocalDateTime.now());

            });


            BufferedButton refreshBtn = new BufferedButton("/assets/refresh-white-30.png", App.MENU_BAR_IMAGE_WIDTH);
            refreshBtn.setId("menuBtn");
            refreshBtn.setOnAction(e -> {
                refreshBtn.setDisable(true);
                refreshBtn.setImage(new Image("/assets/sync-30.png"));
                spectrumData.updateMarkets();
            });


            TextField searchField = new TextField();
            searchField.setPromptText("Search");
            searchField.setId("urlField");
            searchField.setPrefWidth(200);
            searchField.setPadding(new Insets(2, 10, 3, 10));
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                spectrumData.setSearchText(searchField.getText());
            });

            Region menuBarRegion = new Region();
            HBox.setHgrow(menuBarRegion, Priority.ALWAYS);

            HBox menuBar = new HBox(sortTypeButton,sortDirectionButton,swapTargetButton, menuBarRegion, searchField, refreshBtn);
            HBox.setHgrow(menuBar, Priority.ALWAYS);
            menuBar.setAlignment(Pos.CENTER_LEFT);
            menuBar.setId("menuBar");
            menuBar.setPadding(new Insets(1, 10, 1, 5));

            VBox favoritesVBox = spectrumData.getFavoriteGridBox();

            ScrollPane favoriteScroll = new ScrollPane(favoritesVBox);
            favoriteScroll.setPadding(new Insets(5, 0, 5, 5));
            favoriteScroll.setId("bodyBox");

            VBox chartList = spectrumData.getGridBox();

            ScrollPane scrollPane = new ScrollPane(chartList);
            scrollPane.setPadding(SMALL_INSETS);
            scrollPane.setId("bodyBox");

            VBox bodyPaddingBox = new VBox(scrollPane);
            bodyPaddingBox.setPadding(SMALL_INSETS);

            Font smallerFont = Font.font("OCR A Extended", 10);

            Text lastUpdatedTxt = new Text("Updated ");
            lastUpdatedTxt.setFill(App.formFieldColor);
            lastUpdatedTxt.setFont(smallerFont);

            TextField lastUpdatedField = new TextField();
            lastUpdatedField.setEditable(false);
            lastUpdatedField.setId("formField");
            lastUpdatedField.setFont(smallerFont);
            lastUpdatedField.setPrefWidth(165);

            HBox lastUpdatedBox = new HBox(lastUpdatedTxt, lastUpdatedField);
            lastUpdatedBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(lastUpdatedBox, Priority.ALWAYS);

            VBox footerVBox = new VBox(lastUpdatedBox);
            HBox.setHgrow(footerVBox, Priority.ALWAYS);
            footerVBox.setPadding(SMALL_INSETS);

            VBox headerVBox = new VBox(titleBox);
            headerVBox.setPadding(new Insets(0, 5, 0, 5));
            headerVBox.setAlignment(Pos.TOP_CENTER);

            VBox layoutBox = new VBox(headerVBox, bodyPaddingBox, footerVBox);

            HBox menuPaddingBox = new HBox(menuBar);
            menuPaddingBox.setPadding(new Insets(0, 0, 0, 0));

            spectrumData.statusProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !(newVal.equals("Loading..."))) {
                    if (!headerVBox.getChildren().contains(menuPaddingBox)) {
                        headerVBox.getChildren().add(1, menuPaddingBox);

                    }

                } else {
                    if (headerVBox.getChildren().contains(menuPaddingBox)) {
                        headerVBox.getChildren().remove(menuPaddingBox);

                    }

                }
            });

            favoritesVBox.getChildren().addListener((Change<? extends Node> changeEvent) -> {
                int numFavorites = favoritesVBox.getChildren().size();
                if (numFavorites > 0) {
                    if (!headerVBox.getChildren().contains(favoriteScroll)) {

                        headerVBox.getChildren().add(favoriteScroll);
                        menuPaddingBox.setPadding(new Insets(0, 0, 5, 0));
                    }
                    int favoritesHeight = numFavorites * 40 + 21;
                    favoriteScroll.setPrefViewportHeight(favoritesHeight > 175 ? 175 : favoritesHeight);
                } else {
                    if (headerVBox.getChildren().contains(favoriteScroll)) {

                        headerVBox.getChildren().remove(favoriteScroll);
                        menuPaddingBox.setPadding(new Insets(0, 0, 0, 0));
                    }
                }
            });

            Rectangle rect = getNetworksData().getMaximumWindowBounds();
           
            Scene appScene = new Scene(layoutBox, appStageWidth, appStageHeight);
            appScene.setFill(null);
            appScene.getStylesheets().add("/css/startWindow.css");
            m_appStage.setScene(appScene);
            m_appStage.show();

            bodyPaddingBox.prefWidthProperty().bind(m_appStage.widthProperty());
            scrollPane.prefViewportWidthProperty().bind(m_appStage.widthProperty());

            favoriteScroll.prefViewportWidthProperty().bind(m_appStage.widthProperty());

            scrollPane.prefViewportHeightProperty().bind(m_appStage.heightProperty().subtract(headerVBox.heightProperty()).subtract(footerVBox.heightProperty()));

            chartList.prefWidthProperty().bind(scrollPane.prefViewportWidthProperty().subtract(40));
            favoritesVBox.prefWidthProperty().bind(favoriteScroll.prefViewportWidthProperty().subtract(40));

            spectrumData.getLastUpdated().addListener((obs, oldVal, newVal) -> {
                refreshBtn.setDisable(false);
                refreshBtn.setImage(new Image("/assets/refresh-white-30.png"));
                String dateString = Utils.formatDateTimeString(newVal);

                lastUpdatedField.setText(dateString);
            });

            double maxWidth = rect.getWidth() / 2;

            ResizeHelper.addResizeListener(m_appStage, 250, 300, maxWidth, rect.getHeight());

            maxBtn.setOnAction(e -> {
                if (m_appStage.getX() != 0 || m_appStage.getY() != 0 || m_appStage.getHeight() != rect.getHeight()) {

                    m_appStage.setX(0);
                    m_appStage.setY(0);
                    m_appStage.setHeight(rect.getHeight());
                } else {
                    m_appStage.setWidth(appStageWidth);
                    m_appStage.setHeight(appStageHeight);

                    m_appStage.setX((rect.getWidth() / 2) - (appStageWidth / 2));
                    m_appStage.setY((rect.getHeight() / 2) - (appStageHeight / 2));
                }

            });

            m_cmdObjectProperty.addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    JsonElement subjectElement = newVal.get("subject");
                    if (subjectElement != null && subjectElement.isJsonPrimitive()) {
                        switch (subjectElement.getAsString()) {
                            case "MAXIMIZE_STAGE_LEFT":
                                m_appStage.setX(0);
                                m_appStage.setY(0);
                                m_appStage.setHeight(rect.getHeight());
                                m_appStage.show();
                                break;
                        }
                    }
                }
            });

            getNetworksData().timeCycleProperty().addListener((obs,oldval,newval)->refreshBtn.fire());

            m_appStage.setOnCloseRequest(e -> runClose.run());

        } else {
            m_appStage.show();
        }
    }


    @Override
    public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {

    
        return true;
    }


    public boolean getCMCMarkets(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/price-tracking/cmc/markets";
        /*try {
            Files.writeString(logFile.toPath(), "\ngetting url: " + urlString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        Utils.getUrlJsonArray(urlString, onSucceeded, onFailed, null);

        return false;
    }
    
    public boolean getTickers(EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/price-tracking/cg/tickers";
        /*try {
            Files.writeString(logFile.toPath(), "\ngetting url: " + urlString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        Utils.getUrlJsonArray(urlString, onSucceeded, onFailed, null);

        return false;
    }
    //
    public boolean getPoolChart(String poolId,long from, long to, EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        String urlString = API_URL + "/v1/amm/pool/" + poolId + "/chart?from="+from+"&to=" + to;
        /*try {
            Files.writeString(logFile.toPath(), "\ngetting url: " + urlString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {

        }*/
        Utils.getUrlJsonArray(urlString, onSucceeded, onFailed, null);

        return false;
    }

    @Override
    public IconButton getButton(String iconStyle) {

        IconButton iconButton = new IconButton(iconStyle.equals(IconStyle.ROW) ? getSmallAppIcon() : getAppIcon(), getName(), iconStyle) {
            @Override
            public void open() {
                getOpen();
            }
        };

        return iconButton;
    }
}
