package com.netnotes;

import javafx.animation.PauseTransition;
/**
 * Netnotes
 *
 */

//import javafx.application.HostServices;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.WorkerStateEvent;

import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javafx.scene.input.KeyCode;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.reactfx.util.FxTimer;

import com.google.gson.JsonParseException;
import com.utils.Utils;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;

public class App extends Application {

    public static final String CURRENT_VERSION = "v0.4.0-beta";
 
    public static final String GITHUB_USER = "networkspore";
    public static final String GITHUB_PROJECT = "Netnotes-Linux";
    public static final String CMD_SHOW_APPSTAGE = "SHOW_APPSTAGE";
    public static final String CMD_SHUTDOWN = "EXIT";
    public static final long NOTE_EXECUTION_TIME = 100;
    public static final String notesFileName = "notes.dat";

    public final static double MENU_BAR_IMAGE_WIDTH = 18;

    public final static String ASSETS_DIRECTORY = "/assets";

    public final static String AUTORUN_WARNING = "The autorun feature requires an encrypted version of your private key to be saved to disk.\n\nNotice: In order to minimize the security risk the key will be encrypted using platform specific information. Updating or changing base system hardware, such as your motherboard or bios, may invalidate the key and require the autorun feature to be re-enabled in Netnotes settings.\n\n";
    
    public final static String RESOURCES_FILENAME = "resources.dat";
    public final static File logFile = new File("netnotes-log.txt");


    public static final int STOPPED = 0;
    public static final int STARTING = 1;
    public static final int STARTED = 2;
    public static final int ERROR = 3;
    public static final int UPDATED = 4;
    public static final int SHUTDOWN = 5;
    public static final int WARNING = 6;
    public static final int STATUS = 7;
    public static final int INFO = 8;
    public static final int SUCCESS = 9;
    public static final int STOPPING = 10;
    public static final int UPDATING = 11;
    
    public static final int LIST_CHANGED = 20;
    public static final int LIST_CHECKED = 21;
    public static final int LIST_UPDATED = 22;
    public static final int LIST_ITEM_ADDED = 23;
    public static final int LIST_ITEM_REMOVED = 24;
    public static final int LIST_DEFAULT_CHANGED= 25;



    public static final String STATIC_TYPE = "STATIC";


    public static final String WALLET_NETWORK = "WALLET_NETWORK";
    public static final String NODE_NETWORK = "NODE_NETWORK";
    public static final String TOKEN_NETWORK = "TOKENS_NETWORK";
    public static final String EXPLORER_NETWORK = "EXPLORER_NETWORK";
    public static final String MARKET_NETWORK = "MARKET_NETWORK";

    public static final String STATUS_STOPPED = "Stopped";
    public static final String STATUS_STARTED = "Started";
    public static final String STATUS_STARTING = "Starting";
    public static final String STATUS_UNAVAILABLE = "Unavailable";
    public static final String STATUS_AVAILABLE = "Available";
    public static final String STATUS_ERROR = "Error";

    public static final String LOCAL = "Local";
    public static final String CMD = "cmd";

    public static final String OFFLINE = "Offline";

    public static final String CHECK_MARK = "🗸";
    public static final String PLAY = "▶";
    public static final String STOP = "⏹";
    public static final String CIRCLE = "○";
    public static final String RADIO_BTN = "◉";

    public static final  int ROW_HEIGHT = 27;
    public static final  int MAX_ROW_HEIGHT = 20;
    
    //public members
    public static Font mainFont;
    public static Font txtFont;
    public static Font titleFont;
    
    public final static int DEFAULT_RGBA = 0x00000000;
    
    public static Color txtColor = Color.web("#cdd4da");
    public static Color altColor = Color.web("#777777");
    public static Color formFieldColor = new Color(.8, .8, .8, .9);

    public static Image icon = new Image("/assets/icon15.png");
    public static Image logo = new Image("/assets/icon256.png");
    public static Image waitingImage = new Image("/assets/spinning.gif");
    public static Image addImg = new Image("/assets/add-outline-white-40.png");
    public static Image closeImg = new Image("/assets/close-outline-white.png");
    public static Image minimizeImg = new Image("/assets/minimize-white-20.png");
    public static Image globeImg = new Image("/assets/globe-outline-white-120.png");
    public static Image globeImage30 = new Image("/assets/globe-outline-white-30.png");
    public static Image settingsImg = new Image("/assets/settings-outline-white-120.png");
    public static Image lockDocumentImg = new Image("/assets/document-lock.png");
    public static Image arrowRightImg = new Image("/assets/arrow-forward-outline-white-20.png");

    public static Image atImage = new Image("/assets/at-white-240.png");


    public static Image openImg = new Image("/assets/open-outline-white-20.png");
    public static Image diskImg = new Image("/assets/save-outline-white-20.png");

    public final static String SETTINGS_FILE_NAME = "settings.conf";
    public final static String NETWORKS_FILE_NAME = "networks.dat";


    private static volatile long m_rootsTimeStamp = 0;
    private static File[] m_roots = null;

    private NetworksData m_networksData;

    //private java.awt.SystemTray m_tray;
    //private java.awt.TrayIcon m_trayIcon;
    private final static long EXECUTION_TIME = 500;
    public final static double STAGE_WIDTH = 450;
    public final static double STAGE_HEIGHT = 250;
    //private Stage m_stage;
    
    
    private ScheduledFuture<?> m_lastExecution = null;

    @Override
    public void start(Stage appStage) {
        Font.loadFont(App.class.getResource("/assets/OCRAEXT.TTF").toExternalForm(),16);
        Font.loadFont(App.class.getResource("/assets/DejaVuSansMono.ttf").toExternalForm(),20);

        mainFont =  Font.font("OCR A Extended", FontWeight.BOLD, 20);
        txtFont = Font.font("OCR A Extended", 15);
        titleFont = Font.font("OCR A Extended", FontWeight.BOLD, 12);
  
        appStage.setResizable(false);
        appStage.initStyle(StageStyle.UNDECORATED);
        appStage.setTitle("Netnotes");
        appStage.getIcons().add(logo);

    
        try{
            AppData appData = new AppData();
            startApp(appData, appStage);
        }catch(JsonParseException | IOException e){
            setupApp(appStage);
        }

    }

    private Stage m_statusStage = null;
   

    public void setupApp(Stage appStage){
        Button closeBtn = new Button();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        createPassword(appStage, "Create Password - Netnotes", icon, logo, closeBtn, (onFinished)->{
            Object sourceObject = onFinished.getSource().getValue();

            if (sourceObject != null && sourceObject instanceof String) {
                String newPassword = (String) sourceObject;
                        
        
                if (!newPassword.equals("")) {
                    m_statusStage = App.getStatusStage("Starting up - Netnotes", "Starting up...");
                    m_statusStage.show();

                    FxTimer.runLater(Duration.ofMillis(100),()->{
                        try{
                            AppData appData = new AppData(newPassword);
                            m_statusStage.close();
                            m_statusStage = null;
                            openNetnotes(appData, appStage);
                            
                        }catch(NoSuchAlgorithmException | InvalidKeySpecException | IOException e){
                            Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.OK);
                            a.setTitle("Fatal Error");
                            a.setHeaderText("Fatal Error");
                            a.showAndWait();
                            shutdownNow();
                        }

                    });
                }else{
                    shutdownNow();
                }
            }

        }, executorService);
        appStage.show();
        closeBtn.setOnAction(e->{
            shutdownNow();
        });
    }


    public void startApp(AppData appData, Stage appStage) {
        
        appStage.setTitle("Netnotes - Enter Password");

        Button closeBtn = new Button();

        HBox titleBox = createTopBar(icon, "Netnotes - Enter Password", closeBtn, appStage);

        Button imageButton = createImageButton(logo, "Netnotes");

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text("Enter password:");
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

        PasswordField passwordField = new PasswordField();
        passwordField.setFont(txtFont);
        passwordField.setId("passField");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        Platform.runLater(() -> passwordField.requestFocus());

        HBox passwordBox = new HBox(passwordTxt, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setPadding(new Insets(20, 0, 0, 0));

        Button clickRegion = new Button();
        clickRegion.setPrefWidth(Double.MAX_VALUE);
        clickRegion.setId("transparentColor");
        clickRegion.setPrefHeight(500);

        clickRegion.setOnAction(e -> {
            passwordField.requestFocus();

        });

        VBox.setMargin(passwordBox, new Insets(5, 10, 0, 20));

        VBox layoutVBox = new VBox(titleBox, imageBox, passwordBox, clickRegion);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);

        Scene passwordScene = new Scene(layoutVBox, STAGE_WIDTH, STAGE_HEIGHT);
        passwordScene.setFill(null);
        passwordScene.getStylesheets().add("/css/startWindow.css");
        appStage.setScene(passwordScene);
        
        closeBtn.setOnAction(e -> {
            shutdownNow();
        });

       

        passwordField.setOnKeyPressed(e -> {
            Stage statusStage = getStatusStage("Verifying - Netnotes", "Verifying...");
            KeyCode keyCode = e.getCode();

            if (keyCode == KeyCode.ENTER) {

                if (passwordField.getText().length() < 6) {
                    passwordField.setText("");
                } else {
                  

                    statusStage.show();

                    FxTimer.runLater(Duration.ofMillis(100), () -> {
                        String password = passwordField.getText();
                        
                        Platform.runLater(() -> passwordField.setText(""));
                        byte[] hashBytes = appData.getAppKeyBytes();
                        BCrypt.Result result = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).verify(password.toCharArray(), hashBytes);
                        statusStage.close();
                        if (result.verified) {

                            try {
                                appData.createKey(password);
                                openNetnotes(appData, appStage);
                             
                            } catch (Exception e1) {
                                e1.printStackTrace();
                                try {
                                    Files.writeString(logFile.toPath(), "\nApp  errpr " + e1.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                } catch (IOException e2) {
                            
                                }
                            }

                        }
                    });
                }
            }
        });
       
    

 
       Platform.runLater(()->passwordField.requestFocus());
      
       
     
       passwordScene.focusOwnerProperty().addListener((obs, oldval, newval)->{
            
            Platform.runLater(()->passwordField.requestFocus());
            
        });
      

           appStage.show();
      
    }



    public static File[] getRoots(){
    
        if((System.currentTimeMillis() - m_rootsTimeStamp) > 1000){
            m_roots = File.listRoots();
            m_rootsTimeStamp = System.currentTimeMillis();
            return m_roots;
        }else{
            return m_roots;
        }
    }


    public static Stage getStatusStage(String title, String statusMessage) {
        Stage statusStage = new Stage();
        statusStage.setResizable(false);
        statusStage.initStyle(StageStyle.UNDECORATED);
        statusStage.setTitle("Netnotes - Verifying");
        statusStage.getIcons().add(logo);

        statusStage.setTitle(title);

        Label newTitleLbl = new Label(title);
        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));

        ImageView barIconView = new ImageView(icon);
        barIconView.setFitHeight(20);
        barIconView.setPreserveRatio(true);

        HBox newTopBar = new HBox(barIconView, newTitleLbl);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(10, 8, 10, 10));
        newTopBar.setId("topBar");

        ImageView waitingView = new ImageView(logo);
        waitingView.setFitHeight(135);
        waitingView.setPreserveRatio(true);

        HBox imageBox = new HBox(waitingView);
        HBox.setHgrow(imageBox, Priority.ALWAYS);
        imageBox.setAlignment(Pos.CENTER);

        Text statusTxt = new Text(statusMessage);
        statusTxt.setFill(txtColor);
        statusTxt.setFont(txtFont);

        VBox bodyVBox = new VBox(imageBox, statusTxt);

        VBox.setVgrow(bodyVBox, Priority.ALWAYS);
        VBox.setMargin(bodyVBox, new Insets(0, 20, 20, 20));

        VBox layoutVBox = new VBox(newTopBar, bodyVBox);

        Scene statusScene = new Scene(layoutVBox, 420, 215);
        statusScene.setFill(null);
        statusScene.getStylesheets().add("/css/startWindow.css");
        
        statusStage.setScene(statusScene);


        return statusStage;
    }

    static class Delta {

        double x, y;
    }

    // private static int createTries = 0;
    private void openNetnotes(AppData appData,  Stage appStage) {


        m_networksData = new NetworksData(appData);    

        loadMainStage(appStage);
        

        
       
    }

    /*public static HBox createShrinkTopBar(Image iconImage, String titleString, Button maximizeBtn,  Button shrinkBtn, Button closeBtn, Stage theStage, SimpleBooleanProperty isShrunk, AppData appData, ExecutorService execService) {
      

        ImageView barIconView = new ImageView(iconImage);
        barIconView.setFitWidth(20);
        barIconView.setPreserveRatio(true);

        // Rectangle2D logoRect = new Rectangle2D(30,30,30,30);
        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label newTitleLbl = new Label(titleString);
        newTitleLbl.setFont(App.titleFont);
        newTitleLbl.setTextFill(App.txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));



        shrinkBtn.setGraphic(isShrunk.get() ? IconButton.getIconView(new Image("/assets/unshrink-30.png"), 20) : IconButton.getIconView(new Image("/assets/shrink-30.png"), 20));
        shrinkBtn.setPadding(new Insets(0, 0, 0, 0));
        shrinkBtn.setId("toolBtn");
        shrinkBtn.setOnAction(e->{
           

            if(isShrunk.get()){
                final Scene originalScene = theStage.getScene();
                final double originalHeight = originalScene.getHeight();
            
                final double passSceneWidth = 600;
                final double passSceneHeight = 305;
                ResizeHelper.addResizeListener(theStage,passSceneWidth,passSceneHeight, passSceneWidth,passSceneHeight);
      
                theStage.setHeight(passSceneHeight);
                verifyAppKey(theStage, appData, execService, (onSucceeded)->{
   
                    theStage.setScene(originalScene);
                    isShrunk.set(false);
                    
                }, ()->{
         
                    theStage.setScene(originalScene);
                    theStage.setHeight(originalHeight);
                    ResizeHelper.addResizeListener(theStage,400 , originalHeight, Double.MAX_VALUE, originalHeight);
                });
            }else{
                 isShrunk.set(true);
            }
           
        });

        //  HBox.setHgrow(titleLbl2, Priority.ALWAYS);
        ImageView closeImage = App.highlightedImageView(App.closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        closeBtn.setGraphic(closeImage);
        closeBtn.setPadding(new Insets(0, 5, 0, 3));
        closeBtn.setId("closeBtn");

        ImageView minimizeImage = App.highlightedImageView(App.minimizeImg);
        minimizeImage.setFitHeight(20);
        minimizeImage.setFitWidth(20);
        minimizeImage.setPreserveRatio(true);

        Button minimizeBtn = new Button();
        minimizeBtn.setId("toolBtn");
        minimizeBtn.setGraphic(minimizeImage);
        minimizeBtn.setPadding(new Insets(0, 2, 1, 2));
        minimizeBtn.setOnAction(minEvent -> {
            theStage.setIconified(true);
        });

        maximizeBtn.setId("toolBtn");
        maximizeBtn.setGraphic(IconButton.getIconView(new Image("/assets/maximize-white-30.png"), 20));
        maximizeBtn.setPadding(new Insets(0, 3, 0, 3));

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer, minimizeBtn, maximizeBtn, shrinkBtn, closeBtn);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(7, 8, 10, 10));
        newTopBar.setId("topBar");

        isShrunk.addListener((obs, oldVal, newVal)->{
            shrinkBtn.setGraphic(isShrunk.get() ? IconButton.getIconView(new Image("/assets/unshrink-30.png"), 20) : IconButton.getIconView(new Image("/assets/shrink-30.png"), 20));
            if(newVal){
                if(newTopBar.getChildren().contains(maximizeBtn)){
                    newTopBar.getChildren().remove(maximizeBtn);
                }
            }else{
                if(!newTopBar.getChildren().contains(maximizeBtn)){
                    newTopBar.getChildren().add(4, maximizeBtn);
                }
            }
        });

        Delta dragDelta = new Delta();

        newTopBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = theStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = theStage.getY() - mouseEvent.getScreenY();
            }
        });
        newTopBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                theStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                theStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });

        return newTopBar;
    }*/
    

  

    public void createAutorunKeyDialog(Stage appStage, boolean isNewKey, Runnable newKey, Runnable disableAutorun){

        TextField inpuTextField = new TextField();
        Button closeBtn = new Button();
        showGetTextInput(!isNewKey ? "Autrun key invalid. " : "" + "Create autorun key? (Y/n)", "Autorun - Setup", logo, inpuTextField, closeBtn, appStage);
        closeBtn.setOnAction(e->{
                shutdownNow();
            });

        inpuTextField.setOnKeyPressed(e -> {

            KeyCode keyCode = e.getCode();

            if (keyCode == KeyCode.ENTER || keyCode == KeyCode.Y) {
                newKey.run();
            }else{
                if(keyCode == KeyCode.N){
                    disableAutorun.run();
                }else{
                    inpuTextField.setText("");
                }
            }
        });
      
    }

    
    
    public static void verifyAppKey(Stage passwordStage, AppData appData, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded, Runnable onAbort) {
       
        String title = "Enter Password - Netnotes";

        passwordStage.setTitle(title);


        Button closeBtn = new Button();

        HBox titleBox = createTopBar(icon, title, closeBtn, passwordStage);

        ImageView btnImageView = new ImageView(icon);
        btnImageView.setFitHeight(100);
        btnImageView.setPreserveRatio(true);

        Label textField = new Label("Netnotes");
        textField.setFont(mainFont);
        textField.setPadding(new Insets(15,0,0,15));
        

        VBox imageBox = new VBox(btnImageView, textField);
        imageBox.setAlignment(Pos.CENTER);

 
        Text passwordTxt = new Text("Enter password:");
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

        PasswordField passwordField = new PasswordField();
        passwordField.setFont(txtFont);
        passwordField.setId("passField");
        HBox.setHgrow(passwordField, Priority.ALWAYS);


        HBox passwordBox = new HBox(passwordTxt, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setPadding(new Insets(20, 0, 0, 0));

    
        Button clickRegion = new Button();
        clickRegion.setPrefWidth(Double.MAX_VALUE);
        clickRegion.setId("transparentColor");
        clickRegion.setPrefHeight(500);

        clickRegion.setOnAction(e -> {
            passwordField.requestFocus();

        });

        VBox.setMargin(passwordBox, new Insets(5, 10, 0, 20));

        VBox layoutVBox = new VBox(titleBox, imageBox, passwordBox, clickRegion);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);

        Scene passwordScene = new Scene(layoutVBox, STAGE_WIDTH, STAGE_HEIGHT);
        passwordScene.setFill(null);
        passwordScene.getStylesheets().add("/css/startWindow.css");
        passwordStage.setScene(passwordScene);

       
         Stage statusStage = getStatusStage("Verifying - Netnotes", "Verifying...");

        passwordField.setOnKeyPressed(e -> {

            KeyCode keyCode = e.getCode();

            if (keyCode == KeyCode.ENTER) {

                if (passwordField.getText().length() < 6) {
                    passwordField.setText("");
                } else {
                   
                    statusStage.show();
                    
                    FxTimer.runLater(Duration.ofMillis(100), () -> {
                        String password = passwordField.getText();
                        BCrypt.Result result = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).verify(password.toCharArray(), appData.getAppKeyBytes());
                        Platform.runLater(() ->{
                            passwordField.setText("");
                            
                            if (result.verified) {
                            
                                Utils.returnObject((Object)password, execService, onSucceeded, (onFailed)->{});
                          
                                
                            }
                            statusStage.close();
                        } );

                    });
                }
            }
        });

        passwordScene.focusOwnerProperty().addListener((obs, oldval, newval)->{
            if(!(newval != null && newval instanceof TextField)){
                Platform.runLater(()->passwordField.requestFocus());
            }
        });


        closeBtn.setOnAction(e -> {
            
            
            onAbort.run();
        });

        passwordStage.setOnCloseRequest(e->{
            closeBtn.fire();
        });

  

        passwordStage.show();
        Platform.runLater(() ->passwordField.requestFocus());

    }

    
    private VBox m_contentBox;
    private HBox m_footerBox = new HBox();
    private HBox m_titleBox = new HBox();
    private HBox m_menuBox;
    private ScrollPane m_staticContent;

    public final static double DEFAULT_STATIC_WIDTH = 500;

    private SimpleDoubleProperty m_staticContentWidth = new SimpleDoubleProperty(DEFAULT_STATIC_WIDTH +5);
    private SimpleDoubleProperty m_menuWidth = new SimpleDoubleProperty(55);
    private void loadMainStage(Stage appStage) {
   
        Button closeBtn = new Button();
        Button maximizeBtn = new Button();

        appStage.setTitle("Netnotes");

        //getScene().getWindow().getX()

        m_titleBox = createTopBar(icon, maximizeBtn, closeBtn, appStage);

        
        //m_headerBox.setPadding(new Insets(0, 2, 2, 2));
      

        m_staticContent = new ScrollPane();
       
       // m_staticContent.setPrefWidth(350);
        //m_staticContent.setPrefHeight(220);

 

        m_contentBox = new VBox();
        VBox.setVgrow(m_contentBox, Priority.ALWAYS);
        HBox.setHgrow(m_contentBox, Priority.ALWAYS);


  
        
        m_menuBox = new HBox();
        VBox.setVgrow(m_menuBox, Priority.ALWAYS);

        m_menuBox.setPadding(new Insets(2, 0, 2, 0));
        m_menuBox.setId("appMenuBox");
        m_menuBox.minWidthProperty().bind(m_menuWidth.add(5));
        m_menuBox.maxWidthProperty().bind(m_menuWidth.add(5));
        VBox.setVgrow(m_menuBox, Priority.ALWAYS);


  

        HBox mainHbox = new HBox(m_menuBox, m_contentBox);
        VBox.setVgrow(mainHbox, Priority.ALWAYS);
        HBox.setHgrow(mainHbox, Priority.ALWAYS);
    


      
        VBox layout = new VBox(m_titleBox, mainHbox, m_footerBox);
        VBox.setVgrow(layout, Priority.ALWAYS);
        layout.setPadding(new Insets(0, 2, 2, 2));

        Scene appScene = new Scene(layout, m_networksData.getStageWidth(), m_networksData.getStageHeight());
        appScene.setFill(null);
        appScene.getStylesheets().add("/css/startWindow.css");

 
        appStage.setScene(appScene);
        

        appScene.getWindow().centerOnScreen();

        m_networksData.createMenu(appStage, m_menuWidth, m_menuBox, m_staticContent, m_contentBox);

        m_networksData.menuTabProperty().addListener((obs,oldval,newval)->{
            if(newval != null){
                if(!mainHbox.getChildren().contains(m_staticContent)){
                    mainHbox.getChildren().add(1, m_staticContent);
                }
            }else{
                if(mainHbox.getChildren().contains(m_staticContent)){
                    mainHbox.getChildren().remove(m_staticContent);
                }
            }
        });

        ResizeHelper.addResizeListener(appStage, 600, 250, Double.MAX_VALUE, Double.MAX_VALUE);


        m_staticContent.prefViewportWidthProperty().bind(m_staticContentWidth);
        m_staticContent.prefViewportHeightProperty().bind(appScene.heightProperty().subtract(m_titleBox.heightProperty()).subtract(m_footerBox.heightProperty()));
    

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        Runnable save = () -> {
            m_networksData.save();
        };

        appScene.widthProperty().addListener((obs, oldval, newVal) -> {
            m_networksData.setStageWidth(newVal.doubleValue());
      
            if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                m_lastExecution.cancel(false);
            }

            m_lastExecution = executor.schedule(save, EXECUTION_TIME, TimeUnit.MILLISECONDS);
        });
        appScene.heightProperty().addListener((obs, oldval, newVal) -> {
            m_networksData.setStageHeight(newVal.doubleValue());

            if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                m_lastExecution.cancel(false);
            }

            m_lastExecution = executor.schedule(save, EXECUTION_TIME, TimeUnit.MILLISECONDS);
        });

        maximizeBtn.setOnAction(maxEvent -> {
            boolean maximized = appStage.isMaximized();
            m_networksData.setStageMaximized(!maximized);

            if (!maximized) {
                m_networksData.setStagePrevWidth(appStage.getWidth());
                m_networksData.setStagePrevHeight(appStage.getHeight());
                
            }
             
            appStage.setMaximized(!maximized);

        });

        closeBtn.setOnAction(e -> {
            if(m_networksData.getStageMaximized()){
                maximizeBtn.fire();
        
                FxTimer.runLater(Duration.ofMillis(150), ()->{
                    m_networksData.save();
                    m_networksData.shutdown();
                
                    appStage.close();
                    shutdownNow();
                });
            }else{
                m_networksData.shutdown();
                appStage.close();
                shutdownNow();
            }
           
        });

        appStage.setOnCloseRequest(e -> {
            closeBtn.fire();
        });


    

       
    }
    



    

 
    public static void showStatusStage(Stage statusStage, String title, String statusMessage) {
       
        statusStage.setTitle(title);

        Label newTitleLbl = new Label(title);
        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));

        ImageView barIconView = new ImageView(icon);
        barIconView.setFitHeight(20);
        barIconView.setPreserveRatio(true);

        HBox newTopBar = new HBox(barIconView, newTitleLbl);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(10, 8, 10, 10));
        newTopBar.setId("topBar");

        ImageView waitingView = new ImageView(logo);
        waitingView.setFitHeight(135);
        waitingView.setPreserveRatio(true);

        HBox imageBox = new HBox(waitingView);
        HBox.setHgrow(imageBox, Priority.ALWAYS);
        imageBox.setAlignment(Pos.CENTER);

        Text statusTxt = new Text(statusMessage);
        statusTxt.setFill(txtColor);
        statusTxt.setFont(txtFont);

        VBox bodyVBox = new VBox(imageBox, statusTxt);

        VBox.setVgrow(bodyVBox, Priority.ALWAYS);
        VBox.setMargin(bodyVBox, new Insets(0, 20, 20, 20));

        VBox layoutVBox = new VBox(newTopBar, bodyVBox);

        Scene statusScene = new Scene(layoutVBox, 420, 215);
        statusScene.setFill(null);
        statusScene.getStylesheets().add("/css/startWindow.css");

        statusStage.setScene(statusScene);
        statusStage.show();
    }

    public static File getFile(String title, Stage owner, FileChooser.ExtensionFilter... extensionFilters) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        //  fileChooser.setInitialFileName(initialFileName);

        fileChooser.getExtensionFilters().addAll(extensionFilters);
        File file = fileChooser.showOpenDialog(owner);
        return file;
    }

    
    public static Stage createPassword(String topTitle, Image windowLogo, Image mainLogo, Button closeBtn, ExecutorService execService, EventHandler<WorkerStateEvent> onSucceeded) {
        Stage passwordStage = new Stage();
        passwordStage.initStyle(StageStyle.UNDECORATED);
       
        passwordStage.getIcons().add(windowLogo);
        passwordStage.setTitle(topTitle);

      
        HBox titleBox = createTopBar(icon, topTitle, closeBtn, passwordStage);

        Button imageBtn = App.createImageButton(mainLogo, "Create Password");
        imageBtn.setGraphicTextGap(20);
        HBox imageBox = new HBox(imageBtn);
        imageBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text("Enter password:");
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

        PasswordField passwordField = new PasswordField();

        passwordField.setFont(txtFont);
        passwordField.setId("passField");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        PasswordField createPassField2 = new PasswordField();
        HBox.setHgrow(createPassField2, Priority.ALWAYS);
        createPassField2.setId("passField");

        Button enterButton = new Button("[enter]");
        enterButton.setId("toolBtn");

        HBox passwordBox = new HBox(passwordTxt, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setPadding(new Insets(0,10,0,0));

        Button clickRegion = new Button();
        clickRegion.setMaxWidth(Double.MAX_VALUE);
        clickRegion.setId("transparentColor");
        clickRegion.setPrefHeight(Double.MAX_VALUE);

        clickRegion.setOnAction(e -> {
            passwordField.requestFocus();
        });

        VBox bodyBox = new VBox(passwordBox, clickRegion);
        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        Platform.runLater(() -> passwordField.requestFocus());

        VBox passwordVBox = new VBox(titleBox, imageBox, bodyBox);

        Scene passwordScene = new Scene(passwordVBox, STAGE_WIDTH, STAGE_HEIGHT);
        passwordScene.setFill(null);
        passwordScene.getStylesheets().add("/css/startWindow.css");
        passwordStage.setScene(passwordScene);

        closeBtn.setOnAction(e -> {
            passwordField.setText("");
            passwordStage.close();
        });

        passwordField.textProperty().addListener((obs,oldval,newval)->{
            if(passwordField.getPromptText().length() > 0){
                passwordField.setPromptText("");
            }
            if(newval.length() > 0){
                if(!passwordBox.getChildren().contains(enterButton)){
                    passwordBox.getChildren().add(enterButton);
                }
            }else{
                if(passwordBox.getChildren().contains(enterButton)){
                    passwordBox.getChildren().remove(enterButton);
                }
            }
        });

        Text reenterTxt = new Text("Confirm password:");
        reenterTxt.setFill(txtColor);
        reenterTxt.setFont(txtFont);

        Button enter2 = new Button("[enter]");
        enter2.setId("toolBtn");

        HBox secondPassBox = new HBox(reenterTxt, createPassField2);
        secondPassBox.setAlignment(Pos.CENTER_LEFT);
        secondPassBox.setPadding(new Insets(0,10,0,0));

        createPassField2.textProperty().addListener((obs,oldval,newval)->{
            if(newval.length() > 0){
                if(!secondPassBox.getChildren().contains(enter2)){
                    secondPassBox.getChildren().add(enter2);
                }
            }else{
                if(secondPassBox.getChildren().contains(enter2)){
                    secondPassBox.getChildren().remove(enter2);
                }
            }
        });

        

        enterButton.setOnAction(e1 -> {

       
            // createPassField.setText("");

            bodyBox.getChildren().removeAll(passwordBox, clickRegion);

 
            

            Platform.runLater(() -> createPassField2.requestFocus());

     

            bodyBox.getChildren().addAll(secondPassBox, clickRegion);

            clickRegion.setOnAction(regionEvent -> {
                createPassField2.requestFocus();
            });

           

        });
     
        passwordField.setOnAction(e->enterButton.fire());
        
        enter2.setOnAction(e->{
            String passStr = passwordField.getText();

            if (passStr.equals(createPassField2.getText())) {

                Utils.returnObject(passStr,execService, onSucceeded, e2 -> {
                    closeBtn.fire();
                });
            } else {
                bodyBox.getChildren().clear();
                createPassField2.setText("");
                passwordField.setText("");
                passwordField.setPromptText("(password mis-match)");
    
                bodyBox.getChildren().addAll(passwordBox, clickRegion);
    
            }
        });
        
        createPassField2.setOnAction((e)->enter2.fire());

        return passwordStage;
    }

    public static void createPassword(Stage passwordStage, String topTitle, Image windowLogo, Image mainLogo, Button closeBtn, EventHandler<WorkerStateEvent> onSucceeded, ExecutorService execService) {
        
        passwordStage.setTitle(topTitle);

      
        HBox titleBox = createTopBar(icon, topTitle, closeBtn, passwordStage);

        Button imageBtn = App.createImageButton(mainLogo, "Password");
        imageBtn.setGraphicTextGap(20);
        HBox imageBox = new HBox(imageBtn);
        imageBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text("Create password:");
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

        PasswordField passwordField = new PasswordField();

        passwordField.setFont(txtFont);
        passwordField.setId("passField");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        PasswordField createPassField2 = new PasswordField();
        HBox.setHgrow(createPassField2, Priority.ALWAYS);
        createPassField2.setId("passField");

        Button enterButton = new Button("[enter]");
        enterButton.setId("toolBtn");

        HBox passwordBox = new HBox(passwordTxt, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setPadding(new Insets(0, 10,0,0));



        VBox bodyBox = new VBox(passwordBox);
        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        Platform.runLater(() -> passwordField.requestFocus());

        VBox passwordVBox = new VBox(titleBox, imageBox, bodyBox);

        Scene passwordScene = new Scene(passwordVBox, STAGE_WIDTH, STAGE_HEIGHT);
        passwordScene.setFill(null);
        passwordScene.getStylesheets().add("/css/startWindow.css");
        passwordStage.setScene(passwordScene);

        closeBtn.setOnAction(e -> {
            passwordField.setText("");
            passwordStage.close();
        });

        Text reenterTxt = new Text("Confirm password:");
        reenterTxt.setFill(txtColor);
        reenterTxt.setFont(txtFont);

  

        Button enter2 = new Button("[enter]");
        enter2.setId("toolBtn");

        HBox secondPassBox = new HBox(reenterTxt, createPassField2);
        secondPassBox.setAlignment(Pos.CENTER_LEFT);
        secondPassBox.setPadding(new Insets(0,10,0,0));

        enterButton.setOnAction(e->{

           // String passStr = passwordField.getText();
            // createPassField.setText("");

            bodyBox.getChildren().remove(passwordBox);


            bodyBox.getChildren().add(secondPassBox);

            createPassField2.requestFocus();
            
        });

        passwordField.setOnKeyPressed(e->{
            if(passwordField.getPromptText().length() > 0){
                passwordField.setPromptText("");
            }
        });

        passwordField.textProperty().addListener((obs,oldval,newval) -> {
            
            if(passwordField.getText().length() == 0){
                if(passwordBox.getChildren().contains(enterButton)){
                    passwordBox.getChildren().remove(enterButton);
                }
            }else{
                if(!passwordBox.getChildren().contains(enterButton)){
                    passwordBox.getChildren().add(enterButton);
                }
            }
        });

        createPassField2.textProperty().addListener((obs,oldval,newval)->{
            if(createPassField2.getText().length() == 0){
                if(secondPassBox.getChildren().contains(enter2)){
                    secondPassBox.getChildren().remove(enter2);
                }
            }else{
                if(!secondPassBox.getChildren().contains(enter2)){
                    secondPassBox.getChildren().add(enter2);
                }
            }
        });

        passwordField.setOnAction(e->enterButton.fire());
        
        Tooltip errorToolTip = new Tooltip("Password mis-match");
        

        enter2.setOnAction(e->{
            String passStr = passwordField.getText();
            if (passStr.equals(createPassField2.getText())) {

                Utils.returnObject(passStr,execService, onSucceeded, e1 -> {
                    closeBtn.fire();
                });
            } else {
                bodyBox.getChildren().clear();
                createPassField2.setText("");
                passwordField.setText("");
                
                
    
                bodyBox.getChildren().add(passwordBox);
                passwordField.requestFocus();

                Point2D p = passwordBox.localToScene(0.0, 0.0);
       

                errorToolTip.show(
                    passwordBox,  
                    p.getX() + passwordBox.getScene().getX() + passwordBox.getScene().getWindow().getX() + passwordBox.getLayoutBounds().getWidth()-150, 
                    (p.getY()+ passwordBox.getScene().getY() + passwordBox.getScene().getWindow().getY())-passwordBox.getLayoutBounds().getHeight()
                );
                
                PauseTransition pt = new PauseTransition(javafx.util.Duration.millis(1600));
                pt.setOnFinished(ptE->{
                    errorToolTip.hide();
                });
                pt.play();
            }
        });

        createPassField2.setOnAction(e->{
            enter2.fire();
        });
        
    }

    public String getNowTimeString() {
        LocalTime time = LocalTime.now();

        DateTimeFormatter formater = DateTimeFormatter.ofPattern("hh:mm:ss a");

        return formater.format(time);
    }

    
    

    public static void showGetTextInput(String prompt, String title, Image img, TextField textField, Button closeBtn, Stage textInputStage) {
        
        textInputStage.setTitle(title);
  

        HBox titleBox = createTopBar(icon, title, closeBtn, textInputStage);

        Button imageButton = createImageButton(img, title);

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text promptTxt = new Text("" + prompt + ":");
        promptTxt.setFill(txtColor);
        promptTxt.setFont(txtFont);


        textField.setFont(txtFont);
        textField.setId("textField");



        HBox.setHgrow(textField, Priority.ALWAYS);

        Platform.runLater(() -> textField.requestFocus());

        HBox passwordBox = new HBox(promptTxt, textField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);

    
        VBox.setMargin(passwordBox, new Insets(5, 10, 0, 20));

        VBox layoutVBox = new VBox(titleBox, imageBox, passwordBox);
        VBox.setVgrow(layoutVBox, Priority.ALWAYS);

        Scene textInputScene = new Scene(layoutVBox, STAGE_WIDTH, STAGE_HEIGHT);
        textInputScene.setFill(null);
        textInputScene.getStylesheets().add("/css/startWindow.css");

        textInputStage.setScene(textInputScene);

        textInputScene.focusOwnerProperty().addListener((obs, oldVal,newVal)->{
            if(!(newVal instanceof TextField)){
                Platform.runLater(()->textField.requestFocus());
            }
        });

        textInputStage.show();


    }

    public static HBox createTopBar(Image iconImage, Button fillRightBtn, Button maximizeBtn, Button closeBtn, Stage theStage) {

        ImageView barIconView = new ImageView(iconImage);
        barIconView.setFitWidth(20);
        barIconView.setPreserveRatio(true);

        // Rectangle2D logoRect = new Rectangle2D(30,30,30,30);
        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label newTitleLbl = new Label(theStage.titleProperty().get());
        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));
        newTitleLbl.textProperty().bind(theStage.titleProperty());

        //  HBox.setHgrow(titleLbl2, Priority.ALWAYS);
        ImageView closeImage = highlightedImageView(closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        closeBtn.setGraphic(closeImage);
        closeBtn.setPadding(new Insets(0, 5, 0, 3));
        closeBtn.setId("closeBtn");

        ImageView minimizeImage = highlightedImageView(minimizeImg);
        minimizeImage.setFitHeight(20);
        minimizeImage.setFitWidth(20);
        minimizeImage.setPreserveRatio(true);

        Button minimizeBtn = new Button();
        minimizeBtn.setId("toolBtn");
        minimizeBtn.setGraphic(minimizeImage);
        minimizeBtn.setPadding(new Insets(0, 2, 1, 2));
        minimizeBtn.setOnAction(minEvent -> {
            theStage.setIconified(true);
        });

        maximizeBtn.setId("toolBtn");
        maximizeBtn.setGraphic(IconButton.getIconView(new Image("/assets/maximize-white-30.png"), 20));
        maximizeBtn.setPadding(new Insets(0, 3, 0, 3));

        fillRightBtn.setId("toolBtn");
        fillRightBtn.setGraphic(IconButton.getIconView(new Image("/assets/fillRight.png"), 20));
        fillRightBtn.setPadding(new Insets(0, 3, 0, 3));

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer, minimizeBtn, fillRightBtn, maximizeBtn, closeBtn);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(7, 8, 10, 10));
        newTopBar.setId("topBar");

        Delta dragDelta = new Delta();

        newTopBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = theStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = theStage.getY() - mouseEvent.getScreenY();
            }
        });
        newTopBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                theStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                theStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });

        return newTopBar;
    }

    public static HBox createTopBar(Image iconImage, Button maximizeBtn, Button closeBtn, Stage theStage) {

        ImageView barIconView = new ImageView(iconImage);
        barIconView.setFitWidth(20);
        barIconView.setPreserveRatio(true);

        // Rectangle2D logoRect = new Rectangle2D(30,30,30,30);
        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label newTitleLbl = new Label(theStage.titleProperty().get());
        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));
        newTitleLbl.textProperty().bind(theStage.titleProperty());

        //  HBox.setHgrow(titleLbl2, Priority.ALWAYS);
        ImageView closeImage = highlightedImageView(closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        closeBtn.setGraphic(closeImage);
        closeBtn.setPadding(new Insets(0, 5, 0, 3));
        closeBtn.setId("closeBtn");

        ImageView minimizeImage = highlightedImageView(minimizeImg);
        minimizeImage.setFitHeight(20);
        minimizeImage.setFitWidth(20);
        minimizeImage.setPreserveRatio(true);

        Button minimizeBtn = new Button();
        minimizeBtn.setId("toolBtn");
        minimizeBtn.setGraphic(minimizeImage);
        minimizeBtn.setPadding(new Insets(0, 2, 1, 2));
        minimizeBtn.setOnAction(minEvent -> {
            theStage.setIconified(true);
        });

        maximizeBtn.setId("toolBtn");
        maximizeBtn.setGraphic(IconButton.getIconView(new Image("/assets/maximize-white-30.png"), 20));
        maximizeBtn.setPadding(new Insets(0, 3, 0, 3));

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer, minimizeBtn, maximizeBtn, closeBtn);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(7, 8, 10, 10));
        newTopBar.setId("topBar");

        Delta dragDelta = new Delta();

        newTopBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = theStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = theStage.getY() - mouseEvent.getScreenY();
            }
        });
        newTopBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                theStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                theStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });

        return newTopBar;
    }

    

    public static HBox createTopBar(Image iconImage, String titleString, Button maximizeBtn, Button closeBtn, Stage theStage) {

        ImageView barIconView = new ImageView(iconImage);
        barIconView.setFitWidth(20);
        barIconView.setPreserveRatio(true);

        // Rectangle2D logoRect = new Rectangle2D(30,30,30,30);
        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label newTitleLbl = new Label(titleString);
        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));

        //  HBox.setHgrow(titleLbl2, Priority.ALWAYS);
        ImageView closeImage = highlightedImageView(closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        closeBtn.setGraphic(closeImage);
        closeBtn.setPadding(new Insets(0, 5, 0, 3));
        closeBtn.setId("closeBtn");

        ImageView minimizeImage = highlightedImageView(minimizeImg);
        minimizeImage.setFitHeight(20);
        minimizeImage.setFitWidth(20);
        minimizeImage.setPreserveRatio(true);

        Button minimizeBtn = new Button();
        minimizeBtn.setId("toolBtn");
        minimizeBtn.setGraphic(minimizeImage);
        minimizeBtn.setPadding(new Insets(0, 2, 1, 2));
        minimizeBtn.setOnAction(minEvent -> {
            theStage.setIconified(true);
        });

        maximizeBtn.setId("toolBtn");
        maximizeBtn.setGraphic(IconButton.getIconView(new Image("/assets/maximize-white-30.png"), 20));
        maximizeBtn.setPadding(new Insets(0, 3, 0, 3));

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer, minimizeBtn, maximizeBtn, closeBtn);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(7, 8, 10, 10));
        newTopBar.setId("topBar");

        Delta dragDelta = new Delta();

        newTopBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = theStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = theStage.getY() - mouseEvent.getScreenY();
            }
        });
        newTopBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                theStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                theStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });

        return newTopBar;
    }

    public static HBox createTopBar(Image iconImage, String titleString, Button closeBtn, Stage theStage) {

        ImageView barIconView = new ImageView(iconImage);
        barIconView.setFitWidth(20);
        barIconView.setPreserveRatio(true);

        // Rectangle2D logoRect = new Rectangle2D(30,30,30,30);
        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label newTitleLbl = new Label(titleString);
        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));

        //  HBox.setHgrow(titleLbl2, Priority.ALWAYS);
        ImageView closeImage = highlightedImageView(closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        closeBtn.setGraphic(closeImage);
        closeBtn.setPadding(new Insets(0, 5, 0, 3));
        closeBtn.setId("closeBtn");

        ImageView minimizeImage = highlightedImageView(minimizeImg);
        minimizeImage.setFitHeight(20);
        minimizeImage.setFitWidth(20);
        minimizeImage.setPreserveRatio(true);

        Button minimizeBtn = new Button();
        minimizeBtn.setId("toolBtn");
        minimizeBtn.setGraphic(minimizeImage);
        minimizeBtn.setPadding(new Insets(0, 2, 1, 2));
        minimizeBtn.setOnAction(minEvent -> {
            theStage.setIconified(true);
        });

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer, minimizeBtn, closeBtn);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(7, 8, 10, 10));
        newTopBar.setId("topBar");

        Delta dragDelta = new Delta();

        newTopBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = theStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = theStage.getY() - mouseEvent.getScreenY();
            }
        });
        newTopBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                theStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                theStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });

        return newTopBar;
    }

    public static HBox createTopBar(Image iconImage, Label newTitleLbl, Button closeBtn, Stage theStage) {

        ImageView barIconView = new ImageView(iconImage);
        barIconView.setFitWidth(20);
        barIconView.setPreserveRatio(true);

        // Rectangle2D logoRect = new Rectangle2D(30,30,30,30);
        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        newTitleLbl.setFont(titleFont);
        newTitleLbl.setTextFill(txtColor);
        newTitleLbl.setPadding(new Insets(0, 0, 0, 10));

        //  HBox.setHgrow(titleLbl2, Priority.ALWAYS);
        ImageView closeImage = highlightedImageView(closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        closeBtn.setGraphic(closeImage);
        closeBtn.setPadding(new Insets(0, 5, 0, 3));
        closeBtn.setId("closeBtn");

        ImageView minimizeImage = highlightedImageView(minimizeImg);
        minimizeImage.setFitHeight(20);
        minimizeImage.setFitWidth(20);
        minimizeImage.setPreserveRatio(true);

        Button minimizeBtn = new Button();
        minimizeBtn.setId("toolBtn");
        minimizeBtn.setGraphic(minimizeImage);
        minimizeBtn.setPadding(new Insets(0, 2, 1, 2));
        minimizeBtn.setOnAction(minEvent -> {
            theStage.setIconified(true);
        });

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer, minimizeBtn, closeBtn);
        newTopBar.setAlignment(Pos.CENTER_LEFT);
        newTopBar.setPadding(new Insets(7, 8, 10, 10));
        newTopBar.setId("topBar");

        Delta dragDelta = new Delta();

        newTopBar.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = theStage.getX() - mouseEvent.getScreenX();
                dragDelta.y = theStage.getY() - mouseEvent.getScreenY();
            }
        });
        newTopBar.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                theStage.setX(mouseEvent.getScreenX() + dragDelta.x);
                theStage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });

        return newTopBar;
    }

    public static Button createImageButton(Image image, String name) {
        ImageView btnImageView = new ImageView(image);
        btnImageView.setFitHeight(100);
        btnImageView.setPreserveRatio(true);

        Button imageBtn = new Button(name);
        imageBtn.setGraphic(btnImageView);
        imageBtn.setId("startImageBtn");
        imageBtn.setFont(mainFont);
        imageBtn.setContentDisplay(ContentDisplay.TOP);

        return imageBtn;
    }

    private static void shutdownNow() {

        Platform.exit();
        System.exit(0);
    }

    public static ImageView highlightedImageView(Image image) {

        ImageView imageView = new ImageView(image);

        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setBrightness(-0.5);

        imageView.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {

            imageView.setEffect(colorAdjust);

        });
        imageView.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            imageView.setEffect(null);
        });

        return imageView;
    }

    public static String confirmPassword(String topTitle,Image windowLogo, Image smallLogo, String windowSubTitle, String information) {

        

        Stage passwordStage = new Stage();

        passwordStage.setTitle(topTitle);

        passwordStage.getIcons().add(windowLogo);
        passwordStage.setResizable(false);
        passwordStage.initStyle(StageStyle.UNDECORATED);

        Button closeBtn = new Button();

        HBox titleBox = createTopBar(smallLogo, topTitle, closeBtn, passwordStage);

        Button imageButton = createImageButton(windowLogo, windowSubTitle);

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text("Enter password:");
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

        PasswordField passwordField = new PasswordField();
        passwordField.setFont(txtFont);
        passwordField.setId("passField");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        Platform.runLater(() -> passwordField.requestFocus());

        HBox passwordBox = new HBox(passwordTxt, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);

        Button clickRegion = new Button();
        clickRegion.setMaxWidth(Double.MAX_VALUE);
        clickRegion.setId("transparentColor");
        clickRegion.setPrefHeight(Double.MAX_VALUE);

        clickRegion.setOnAction(e -> {
            passwordField.requestFocus();
        });

        VBox bodyBox = new VBox(passwordBox, clickRegion);
        VBox.setMargin(bodyBox, new Insets(5, 10, 0, 20));
        VBox.setVgrow(bodyBox, Priority.ALWAYS);

        VBox layoutVBox = new VBox(titleBox, imageBox, bodyBox);
        

        Scene passwordScene = new Scene(layoutVBox, STAGE_WIDTH, STAGE_HEIGHT);
        passwordScene.setFill(null);
        passwordScene.getStylesheets().add("/css/startWindow.css");
        passwordStage.setScene(passwordScene);

        closeBtn.setOnAction(e -> {
            passwordBox.getChildren().remove(passwordField);
            passwordField.setDisable(true);
            passwordStage.close();
        });

        passwordField.setOnKeyPressed(e -> {

            KeyCode keyCode = e.getCode();
            if (keyCode == KeyCode.ENTER) {

                if (passwordField.getText().length() < 6) {
                    passwordField.setText("");
                } else {

                    passwordStage.close();
                }
            }
        });

        passwordStage.showAndWait();

        return passwordField.getText().equals("") ? null : passwordField.getText();
    }

    /*private void addAppToTray() {
        try {

            java.awt.Toolkit.getDefaultToolkit();

            BufferedImage imgBuf = SwingFXUtils.fromFXImage(new Image("/assets/icon15.png"), null);

            m_tray = java.awt.SystemTray.getSystemTray();

            m_trayIcon = new java.awt.TrayIcon((java.awt.Image) imgBuf, "Netnotes");
            m_trayIcon.setActionCommand("show");

            m_trayIcon.addActionListener(event -> Platform.runLater(() -> {
                ActionEvent aEv = event;
                if (aEv.getActionCommand().equals("show")) {
                    m_networksData.show();
                }
              
            }));

            java.awt.MenuItem openItem = new java.awt.MenuItem("Show Netnotes");
            openItem.addActionListener(event -> Platform.runLater(() -> m_networksData.show()));

            java.awt.Font defaultFont = java.awt.Font.decode(null);
            java.awt.Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);
            openItem.setFont(boldFont);

            java.awt.MenuItem exitItem = new java.awt.MenuItem("Close");
            exitItem.addActionListener(event -> Platform.runLater(() -> {
                m_networksData.shutdown();
                m_tray.remove(m_trayIcon);
                shutdownNow();
            }));

            final java.awt.PopupMenu popup = new java.awt.PopupMenu();
            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);
            m_trayIcon.setPopupMenu(popup);

            m_tray.add(m_trayIcon);

        } catch (java.awt.AWTException e) {
            try {
                Files.writeString(logFile.toPath(), "\nAWT - trayItem: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {
              
            }
            
        }

    }*/

    public static Scene getProgressScene(Image icon, String headingString, String titleContextString, String fileName, ProgressBar progressBar, Stage stage, Button closeBtn) {

        double defaultRowHeight = 40;
       


        Text fileNameProgressText = new Text(fileName + " (" + String.format("%.1f", progressBar.getProgress() * 100) + "%)");
        fileNameProgressText.setFill(txtColor);
        fileNameProgressText.setFont(txtFont);

        Label titleBoxLabel = new Label();
        titleBoxLabel.setTextFill(txtColor);
        titleBoxLabel.setFont(txtFont);
        titleBoxLabel.textProperty().bind(fileNameProgressText.textProperty());

        HBox titleBox = createTopBar(icon, titleBoxLabel, closeBtn, stage);

        Text headingText = new Text(headingString);
        headingText.setFont(txtFont);
        headingText.setFill(txtColor);

        HBox headingBox = new HBox(headingText);
        headingBox.prefHeight(defaultRowHeight);
        headingBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        headingBox.setPadding(new Insets(10, 10, 10, 10));
        headingBox.setId("headingBox");

        HBox headingPaddingBox = new HBox(headingBox);

        headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));

        VBox headerBox = new VBox(titleBox, headingPaddingBox);

        headerBox.setPadding(new Insets(0, 5, 0, 5));

        //Region progressLeftRegion = new Region();
        //progressLeftRegion.minWidthProperty().bind(stage.widthProperty().multiply(0.15));
        progressBar.prefWidthProperty().bind(stage.widthProperty().multiply(0.7));

        //  Region bodyTopRegion = new Region();
        HBox progressAlignmentBox = new HBox(progressBar);
        //  HBox.setHgrow(progressAlignmentBox, Priority.ALWAYS);
        progressAlignmentBox.setAlignment(Pos.CENTER);

       

        progressBar.progressProperty().addListener((obs, oldVal, newVal) -> {
            fileNameProgressText.setText(fileName + " (" + String.format("%.1f", newVal.doubleValue() * 100) + "%)");
        });

        stage.titleProperty().bind(Bindings.concat(fileNameProgressText.textProperty(), " - ", titleContextString));

        HBox fileNameProgressBox = new HBox(fileNameProgressText);
        fileNameProgressBox.setAlignment(Pos.CENTER);
        fileNameProgressBox.setPadding(new Insets(20, 0, 0, 0));

        VBox colorBox = new VBox(progressAlignmentBox, fileNameProgressBox);
        colorBox.setId("bodyBox");
        HBox.setHgrow(colorBox, Priority.ALWAYS);
        colorBox.setPadding(new Insets(40, 0, 15, 0));

        VBox bodyBox = new VBox(colorBox);
        bodyBox.setId("bodyBox");
        bodyBox.setPadding(new Insets(15));
        bodyBox.setAlignment(Pos.CENTER);

        VBox bodyPaddingBox = new VBox(bodyBox);
        bodyPaddingBox.setPadding(new Insets(5, 5, 5, 5));

        Region footerSpacer = new Region();
        footerSpacer.setMinHeight(5);

        VBox footerBox = new VBox(footerSpacer);
        VBox layoutBox = new VBox(headerBox, bodyPaddingBox, footerBox);
        Scene scene = new Scene(layoutBox, STAGE_WIDTH, STAGE_HEIGHT);
        scene.setFill(null);
        scene.getStylesheets().add("/css/startWindow.css");

        // bodyTopRegion.minHeightProperty().bind(stage.heightProperty().subtract(30).divide(2).subtract(progressAlignmentBox.heightProperty()).subtract(fileNameProgressBox.heightProperty().divide(2)));
        bodyBox.prefHeightProperty().bind(stage.heightProperty().subtract(headerBox.heightProperty()).subtract(footerBox.heightProperty()).subtract(10));
        return scene;
    }

    public static String getShutdownCmdObject(String id) {

        return "{\"id\": \"" + id + "\", \"type\": \"CMD\", \"cmd\": \"" + CMD_SHUTDOWN + "\", \"timeStamp\": " + System.currentTimeMillis() + "}";
    }
}
