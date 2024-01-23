package com.netnotes;

/**
 * Netnotes
 *
 */
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;

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
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javafx.scene.input.KeyCode;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.reactfx.util.FxTimer;

import com.netnotes.IconButton.IconStyle;
import com.google.gson.JsonParseException;
import com.utils.Utils;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;

public class App extends Application {

    public static final String CMD_SHOW_APPSTAGE = "SHOW_APPSTAGE";
    public static final long NOTE_EXECUTION_TIME = 100;
    public static final String notesFileName = "notes.dat";

    public final static double MENU_BAR_IMAGE_WIDTH = 20;

    public final static String ASSETS_DIRECTORY = "/assets";

    public static final String GET_DATA = "GET_DATA";

    public final static String AUTORUN_WARNING = "The autorun feature requires an encrypted version of your private key to be saved to disk.\n\nNotice: In order to minimize the security risk the key will be encrypted using platform specific information. Updating or changing base system hardware, such as your motherboard or bios, may invalidate the key and require the autorun feature to be re-enabled in Netnotes settings.\n\n";
    
    private File logFile = new File("netnotes-log.txt");


    //public members
    public static Font mainFont;
    public static Font txtFont;
    public static Font titleFont;
    
    public static Color txtColor = Color.web("#cdd4da");
    public static Color altColor = Color.web("#777777");
    public static Color formFieldColor = new Color(.8, .8, .8, .9);

    public static Image icon = new Image("/assets/icon15.png");
    public static Image logo = new Image("/assets/icon256.png");
    public static Image ergoLogo = new Image("/assets/ergo-network.png");
    public static Image waitingImage = new Image("/assets/spinning.gif");
    public static Image addImg = new Image("/assets/add-outline-white-40.png");
    public static Image closeImg = new Image("/assets/close-outline-white.png");
    public static Image minimizeImg = new Image("/assets/minimize-white-20.png");
    public static Image globeImg = new Image("/assets/globe-outline-white-120.png");
    public static Image settingsImg = new Image("/assets/settings-outline-white-120.png");
    public static Image lockDocumentImg = new Image("/assets/document-lock.png");
    public static Image arrowRightImg = new Image("/assets/arrow-forward-outline-white-20.png");
    public static Image ergoWallet = new Image("/assets/ergo-wallet.png");
    public static Image atImage = new Image("/assets/at-white-240.png");
   
    public static Image ergoExplorerImg = new Image("/assets/ergo-explorer.png");
    public static Image kucoinImg = new Image("/assets/kucoin-100.png");

    public static Image walletLockImg20 = new Image("/assets/wallet-locked-outline-white-20.png");

    public static Image openImg = new Image("/assets/open-outline-white-20.png");
    public static Image diskImg = new Image("/assets/save-outline-white-20.png");

    public final static String SETTINGS_FILE_NAME = "settings.conf";
    public final static String NETWORKS_FILE_NAME = "networks.dat";


    private NetworksData m_networksData;

    private HostServices m_networkServices = getHostServices();
    private java.awt.SystemTray m_tray;
    private java.awt.TrayIcon m_trayIcon;
    private final static long EXECUTION_TIME = 500;
    //private Stage m_stage;
    
    
    private ScheduledFuture<?> m_lastExecution = null;




    @Override
    public void start(Stage appStage) {
        Font.loadFont(App.class.getResource("/assets/OCRAEXT.TTF").toExternalForm(),12);
        mainFont =  Font.font("OCR A Extended", FontWeight.BOLD, 25);
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

    public void setupApp(Stage appStage){
        Button closeBtn = new Button();
        createPassword(appStage, "Create Password - Netnotes", icon, logo, closeBtn, (onFinished)->{
            Object sourceObject = onFinished.getSource().getValue();

            if (sourceObject != null && sourceObject instanceof String) {
                String newPassword = (String) sourceObject;

                if (!newPassword.equals("")) {
                    try{
                        openNetnotes(new AppData(newPassword), appStage);
                    }catch(NoSuchAlgorithmException | InvalidKeySpecException | IOException e){
                        Alert a = new Alert(AlertType.NONE, e.toString(), ButtonType.OK);
                        a.setTitle("Fatal Error");
                        a.setHeaderText("Fatal Error");
                        a.showAndWait();
                        shutdownNow();
                    }
                }else{
                    shutdownNow();
                }
            }

        });
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

        Text passwordTxt = new Text("> Enter password:");
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

        Scene passwordScene = new Scene(layoutVBox, 600, 300);
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
        
        Rectangle screenRect = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        statusStage.setX((screenRect.getWidth()/2) - (statusScene.getWidth()/2));
        statusStage.setY((screenRect.getHeight()/2) - (statusScene.getHeight()/2));

        return statusStage;
    }

    static class Delta {

        double x, y;
    }

    // private static int createTries = 0;
    private void openNetnotes(AppData appData,  Stage appStage) {

        File networksFile = new File(appData.getAppDir().getAbsolutePath() + "/" + NETWORKS_FILE_NAME);

        boolean isNetworksFile = networksFile.isFile();

        m_networksData = new NetworksData(appData, m_networkServices, networksFile, isNetworksFile);    

        m_networksData.cmdSwitchProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                com.grack.nanojson.JsonObject cmdObject = m_networksData.cmdSwitchProperty().get();
                String type = cmdObject.getString("type");

                if (type != null) {
                    if (type.equals("CMD")) {
                        String cmd = cmdObject.getString("cmd");
                        
                        switch (cmd) {

                            default:
                        }
                        
                    }
                }
            }
        });

      

        loadMainStage(appStage, isNetworksFile);
        
        m_networksData.show();
        
       
    }

    public static HBox createShrinkTopBar(Image iconImage, String titleString, Button maximizeBtn,  Button shrinkBtn, Button closeBtn, Stage theStage, SimpleBooleanProperty isShrunk, AppData appData) {
      

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
                Rectangle rect = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                final double passSceneWidth = 600;
                final double passSceneHeight = 305;
                ResizeHelper.addResizeListener(theStage,passSceneWidth,passSceneHeight, passSceneWidth,passSceneHeight);
      
                theStage.setHeight(passSceneHeight);
                verifyAppKey(theStage, appData, (onSucceeded)->{
   
                    theStage.setScene(originalScene);
                    isShrunk.set(false);
                    
                }, ()->{
         
                    theStage.setScene(originalScene);
                    theStage.setHeight(originalHeight);
                    ResizeHelper.addResizeListener(theStage,400 , originalHeight, rect.getWidth(), originalHeight);
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
    }
    
    public static void verifyAppKey(Runnable runnable, byte[] appkey) {

        String title = "Netnotes - Enter Password";

        Stage passwordStage = new Stage();
        passwordStage.getIcons().add(App.logo);
        passwordStage.setResizable(false);
        passwordStage.initStyle(StageStyle.UNDECORATED);
        passwordStage.setTitle(title);

        Button closeBtn = new Button();

        HBox titleBox = App.createTopBar(App.icon, title, closeBtn, passwordStage);

        Button imageButton = App.createImageButton(App.logo, "Netnotes");

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text("> Enter password:");
        passwordTxt.setFill(App.txtColor);
        passwordTxt.setFont(App.txtFont);

        PasswordField passwordField = new PasswordField();
        passwordField.setFont(App.txtFont);
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

        Scene passwordScene = new Scene(layoutVBox, 600, 290);
        passwordScene.setFill(null);
        passwordScene.getStylesheets().add("/css/startWindow.css");
        passwordStage.setScene(passwordScene);

        Stage statusStage = App.getStatusStage("Verifying - Netnotes", "Verifying...");

        passwordField.setOnKeyPressed(e -> {

            KeyCode keyCode = e.getCode();

            if (keyCode == KeyCode.ENTER) {

                if (passwordField.getText().length() < 6) {
                    passwordField.setText("");
                } else {
          
                    statusStage.show();
                    
                    FxTimer.runLater(Duration.ofMillis(100), () -> {

                        BCrypt.Result result = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).verify(passwordField.getText().toCharArray(), appkey);
                        Platform.runLater(() -> passwordField.setText(""));
                        statusStage.close();
                        if (result.verified) {
                            passwordStage.close();

                            runnable.run();

                        }

                    });
                }
            }
        });

        closeBtn.setOnAction(e -> {
            passwordStage.close();

        });

        passwordScene.focusOwnerProperty().addListener((obs, oldval, newVal) -> {
            if (newVal != null && !(newVal instanceof PasswordField)) {
                Platform.runLater(() -> passwordField.requestFocus());
            }
        });
             passwordStage.show();
            passwordStage.toFront();
        Platform.runLater(() ->{
       
        
            passwordField.requestFocus();}
        );
    }

    public void verifyAppKey(Runnable runnable) {

        String title = "Netnotes - Enter Password";

        Stage passwordStage = new Stage();
        passwordStage.getIcons().add(logo);
        passwordStage.setResizable(false);
        passwordStage.initStyle(StageStyle.UNDECORATED);
        passwordStage.setTitle(title);

        Button closeBtn = new Button();

        HBox titleBox = createTopBar(icon, title, closeBtn, passwordStage);

        Button imageButton = createImageButton(logo, "Netnotes");

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text("> Enter password:");
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

        Scene passwordScene = new Scene(layoutVBox, 600, 320);
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

                        BCrypt.Result result = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).verify(passwordField.getText().toCharArray(), m_networksData.getAppData().getAppKeyBytes());
                        Platform.runLater(() -> passwordField.setText(""));
                        statusStage.close();
                        if (result.verified) {
                            passwordStage.close();

                            runnable.run();

                        }

                    });
                }
            }
        });

        closeBtn.setOnAction(e -> {
            passwordStage.close();

        });

        passwordScene.focusOwnerProperty().addListener((obs, oldval, newVal) -> {
            if (newVal != null && !(newVal instanceof PasswordField)) {
                Platform.runLater(() -> passwordField.requestFocus());
            }
        });
        passwordStage.show();
 
        Platform.runLater(() ->{
       
        
            passwordField.requestFocus();}
        );
    }

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

    
    public static void verifyAppKey(Stage passwordStage, AppData appData, EventHandler<WorkerStateEvent> onSucceeded, Runnable onAbort) {
       
        String title = "Enter Password - Netnotes";

        passwordStage.setTitle(title);


        Button closeBtn = new Button();

        HBox titleBox = createTopBar(icon, title, closeBtn, passwordStage);

        Button imageButton = createImageButton(logo, "Netnotes");

        

        HBox imageBox = new HBox(imageButton);
        imageBox.setAlignment(Pos.CENTER);

 
        Text passwordTxt = new Text("> Enter password:");
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

        Scene passwordScene = new Scene(layoutVBox, 600, 320);
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
                                Utils.returnObject((Object)password, onSucceeded, (onFailed)->{});
                          
                                
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


    private void loadMainStage(Stage appStage, boolean isNetworksFile) {
   
        Button closeBtn = new Button();
        Button settingsBtn = new Button();
        Button networksBtn = new Button();
        Button maximizeBtn = new Button();

        appStage.setTitle("Netnotes: Networks");

        HBox titleBox = createTopBar(icon, maximizeBtn, closeBtn, appStage);

        VBox menuBox = createMenu(settingsBtn, networksBtn);
        menuBox.setId("appMenuBox");
        networksBtn.setId("activeMenuBtn");
        VBox.setVgrow(menuBox, Priority.ALWAYS);

        VBox bodyVBox = new VBox();
        HBox.setHgrow(bodyVBox, Priority.ALWAYS);
        VBox.setVgrow(bodyVBox, Priority.ALWAYS);
        bodyVBox.setId("bodyBox");
        bodyVBox.setPadding(new Insets(0, 5, 5, 5));

        Region vBar = new Region();
        VBox.setVgrow(vBar, Priority.ALWAYS);
        vBar.setPrefWidth(2);
        vBar.setId("vGradient");

        VBox headerBox = new VBox();
        headerBox.setPadding(new Insets(0, 2, 5, 2));
        headerBox.setId("bodyBox");

        VBox bodyBox = new VBox(headerBox, bodyVBox);
        HBox.setHgrow(bodyBox, Priority.ALWAYS);
        VBox.setVgrow(bodyBox,Priority.ALWAYS);

        HBox mainHbox = new HBox(menuBox, vBar, bodyBox);
        VBox.setVgrow(mainHbox, Priority.ALWAYS);

        VBox layout = new VBox(titleBox, mainHbox);
        VBox.setVgrow(layout, Priority.ALWAYS);
        layout.setPadding(new Insets(0, 2, 2, 2));

        Scene appScene = new Scene(layout, m_networksData.getStageWidth(), m_networksData.getStageHeight());
        appScene.setFill(null);
        appScene.getStylesheets().add("/css/startWindow.css");
        // appStage.setScene(appScene);

        settingsBtn.setOnAction(e -> {
            networksBtn.setId("menuBtn");
            settingsBtn.setId("activeMenuBtn");
            headerBox.getChildren().clear();
            showSettings(appStage, bodyVBox);
        });

        networksBtn.setOnAction(e -> {
            networksBtn.setId("activeMenuBtn");
            settingsBtn.setId("menuBtn");
            showNetworks(appScene, headerBox, bodyVBox);
        });

        appStage.setScene(appScene);
        showNetworks(appScene, headerBox, bodyVBox);

        Rectangle rect = m_networksData.getMaximumWindowBounds();
        ResizeHelper.addResizeListener(appStage, 400, 200, rect.getWidth(), rect.getHeight());

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            }
        });
        Runnable save = () -> {
            m_networksData.save();

        };

        appScene.widthProperty().addListener((obs, oldval, newVal) -> {
            m_networksData.setStageWidth(newVal.doubleValue());
            m_networksData.updateNetworksGrid();
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
            m_networksData.shutdown();
            appStage.close();
            shutdownNow();
        });

        appStage.setOnCloseRequest(e -> {
            closeBtn.fire();
        });


        if (m_networksData.getStageMaximized()) {

            appStage.setMaximized(true);
        }
        if (!isNetworksFile) {

            m_networksData.showManageNetworkStage();
            
        }
    }
    

    private void showNetworks(Scene appScene, VBox header, VBox bodyVBox) {
        bodyVBox.setPadding(new Insets(0,2,2,0));
        bodyVBox.setId("darkBox");
        bodyVBox.getChildren().clear();

        Tooltip addTip = new Tooltip("Networks");
        addTip.setShowDelay(new javafx.util.Duration(100));
        addTip.setFont(App.txtFont);

        BufferedButton manageButton = new BufferedButton("assets/filter.png", App.MENU_BAR_IMAGE_WIDTH);
        manageButton.setTooltip(addTip);
        manageButton.setOnAction(e -> m_networksData.showManageNetworkStage());

        Region menuSpacer = new Region();
        HBox.setHgrow(menuSpacer, Priority.ALWAYS);

        Tooltip gridTypeToolTip = new Tooltip("Toggle: List view");
        gridTypeToolTip.setShowDelay(new javafx.util.Duration(50));
        gridTypeToolTip.setHideDelay(new javafx.util.Duration(200));

        BufferedButton toggleGridTypeButton = new BufferedButton("/assets/list-outline-white-25.png", App.MENU_BAR_IMAGE_WIDTH);
        toggleGridTypeButton.setTooltip(gridTypeToolTip);
  

        HBox menuBar = new HBox(manageButton, menuSpacer, toggleGridTypeButton);
        HBox.setHgrow(menuBar, Priority.ALWAYS);
        menuBar.setAlignment(Pos.CENTER_LEFT);
        menuBar.setId("menuBar");
        menuBar.setPadding(new Insets(1, 5, 1, 5));

        HBox menuBarPadding = new HBox(menuBar);
        menuBarPadding.setId("darkBox");
        HBox.setHgrow(menuBarPadding, Priority.ALWAYS);
        menuBarPadding.setPadding(new Insets(0,0,4,2));

        header.getChildren().clear();
        header.setPadding(new Insets(0,1,0,1));
        header.setId("darkBox");
        header.getChildren().add(menuBarPadding);

        VBox gridBox = m_networksData.getNetworksBox();


        ScrollPane scrollPane = new ScrollPane(gridBox);
        scrollPane.setId("bodyBox");
        scrollPane.setPadding(new Insets(5));
        scrollPane.prefViewportWidthProperty().bind(appScene.widthProperty().subtract(90));
        scrollPane.prefViewportHeightProperty().bind(appScene.heightProperty().subtract(menuBar.heightProperty().get()).subtract(50));

        VBox scrollPanePadding = new VBox(scrollPane);
        VBox.setVgrow(scrollPanePadding,Priority.ALWAYS);
        HBox.setHgrow(scrollPanePadding, Priority.ALWAYS);
        scrollPanePadding.setPadding(new Insets(0,0,0,3));
        bodyVBox.getChildren().addAll(scrollPanePadding);

        toggleGridTypeButton.setOnAction(e -> {

            m_networksData.iconStyleProperty().set(m_networksData.iconStyleProperty().get().equals(IconStyle.ROW) ? IconStyle.ICON : IconStyle.ROW);

        });

        /*
        addButton.setOnAction(clickEvent -> {
            Network newNetwork = showNetworkStage(null);

            refreshNetworksGrid(gridBox);
        });*/
    }

    private void showSettings(Stage appStage, VBox bodyVBox) {
        bodyVBox.getChildren().clear();

        

        Button settingsButton = createImageButton(logo, "Settings");

        HBox settingsBtnBox = new HBox(settingsButton);
        settingsBtnBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text(String.format("%-12s", "  Password:"));
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

    

        Button passwordBtn = new Button("(click to update)");
        passwordBtn.setFont(txtFont);
        passwordBtn.setId("toolBtn");
        passwordBtn.setOnAction(e -> {
            Button closeBtn = new Button();
            verifyAppKey(()->{
                Stage passwordStage = createPassword("Netnotes - Password", logo, logo, closeBtn, (onSuccess) -> {
                    Object sourceObject = onSuccess.getSource().getValue();

                    if (sourceObject != null && sourceObject instanceof String) {
                        String newPassword = (String) sourceObject;

                        if (!newPassword.equals("")) {

                            Stage statusStage = getStatusStage("Netnotes - Saving...", "Saving...");
                            statusStage.show();
                            FxTimer.runLater(Duration.ofMillis(100), () -> {
                                String hash = Utils.getBcryptHashString(newPassword);
                                Platform.runLater(()->{
                                    try {

                                        m_networksData.getAppData().setAppKey(hash);
                                        m_networksData.getAppData().createKey(newPassword);
                                    } catch ( Exception e1) {
                                        try {
                                            Files.writeString(logFile.toPath(), "App createPassword: " +  e1.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                        } catch (IOException e2) {
                                        
                                        }
                                        Alert a = new Alert(AlertType.NONE, "Error: Password not changed.", ButtonType.CLOSE);
                                        a.setTitle("Error: Password not changed.");
                                        a.initOwner(appStage);
                                        a.show();
                                    }
                                });
                                statusStage.close();

                            });
                        } else {
                            Alert a = new Alert(AlertType.NONE, "Netnotes: Passwod not change.\n\nCanceled by user.", ButtonType.CLOSE);
                            a.setTitle("Netnotes: Password not changed");
                            a.initOwner(appStage);
                            a.show();
                        }
                    }
                    closeBtn.fire();
                });
                passwordStage.show();
            });
        });

        HBox passwordBox = new HBox(passwordTxt, passwordBtn);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        passwordBox.setPadding(new Insets(10, 0, 0, 20));
        passwordBox.setMinHeight(30);

        Text updatesTxt = new Text(String.format("%-12s", "  Updates:"));
        updatesTxt.setFill(txtColor);
        updatesTxt.setFont(txtFont);

        Button updatesBtn = new Button("Check for updates");
        updatesBtn.setFont(txtFont);
        updatesBtn.setId("toolBtn");
      
        HBox updatesBox = new HBox(updatesTxt, updatesBtn);
        updatesBox.setAlignment(Pos.CENTER_LEFT);
        updatesBox.setPadding(new Insets(0, 0, 0, 20));
        updatesBox.setMinHeight(30);

        
        VBox settingsVBox = new VBox(passwordBox, updatesBox);
        HBox.setHgrow(settingsVBox, Priority.ALWAYS);

        settingsVBox.setAlignment(Pos.CENTER_LEFT);
        settingsVBox.setPadding(new Insets(15,0,15,0));


        bodyVBox.getChildren().addAll(settingsBtnBox, settingsVBox);
    }

    private VBox createMenu(Button settingsBtn, Button networksBtn) {
        double menuSize = 35;

        ImageView networkImageView = highlightedImageView(globeImg);
        networkImageView.setFitHeight(menuSize);
        networkImageView.setPreserveRatio(true);

        Tooltip networkToolTip = new Tooltip("Networks");
        networkToolTip.setShowDelay(new javafx.util.Duration(100));
        networksBtn.setGraphic(networkImageView);
        networksBtn.setId("menuBtn");
        networksBtn.setTooltip(networkToolTip);

        ImageView settingsImageView = highlightedImageView(settingsImg);
        settingsImageView.setFitHeight(menuSize);
        settingsImageView.setPreserveRatio(true);

        Tooltip settingsTooltip = new Tooltip("Settings");
        settingsTooltip.setShowDelay(new javafx.util.Duration(100));
        settingsBtn.setGraphic(settingsImageView);
        settingsBtn.setId("menuBtn");
        settingsBtn.setTooltip(settingsTooltip);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox menuBox = new VBox(networksBtn, spacer, settingsBtn);
        VBox.setVgrow(menuBox, Priority.ALWAYS);
        menuBox.setId("menuBox");
        menuBox.setPadding(new Insets(2, 0, 2, 2));

        return menuBox;
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
        Rectangle screenRect = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        statusStage.setX((screenRect.getWidth()/2) - (statusScene.getWidth()/2));
        statusStage.setY((screenRect.getHeight()/2) - (statusScene.getHeight()/2));
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

    
    public static Stage createPassword(String topTitle, Image windowLogo, Image mainLogo, Button closeBtn, EventHandler<WorkerStateEvent> onSucceeded) {
        Stage passwordStage = new Stage();
        passwordStage.initStyle(StageStyle.UNDECORATED);
       
        passwordStage.getIcons().add(windowLogo);
        passwordStage.setTitle(topTitle);

      
        HBox titleBox = createTopBar(icon, topTitle, closeBtn, passwordStage);

        Button imageBtn = App.createImageButton(mainLogo, "Create Password");
        imageBtn.setGraphicTextGap(20);
        HBox imageBox = new HBox(imageBtn);
        imageBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text("> Enter password:");
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

        PasswordField passwordField = new PasswordField();

        passwordField.setFont(txtFont);
        passwordField.setId("passField");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        PasswordField createPassField2 = new PasswordField();
        HBox.setHgrow(createPassField2, Priority.ALWAYS);
        createPassField2.setId("passField");

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

        Platform.runLater(() -> passwordField.requestFocus());

        VBox passwordVBox = new VBox(titleBox, imageBox, bodyBox);

        Scene passwordScene = new Scene(passwordVBox, 600, 300);
        passwordScene.setFill(null);
        passwordScene.getStylesheets().add("/css/startWindow.css");
        passwordStage.setScene(passwordScene);

        closeBtn.setOnAction(e -> {
            passwordField.setText("");
            passwordStage.close();
        });

        passwordField.setOnKeyPressed(e1 -> {

            KeyCode keyCode = e1.getCode();

            if ((keyCode == KeyCode.ENTER || keyCode == KeyCode.TAB)) {

                String passStr = passwordField.getText();
                // createPassField.setText("");

                bodyBox.getChildren().removeAll(passwordBox, clickRegion);

                Text reenterTxt = new Text("> Confirm password:");
                reenterTxt.setFill(txtColor);
                reenterTxt.setFont(txtFont);

                Platform.runLater(() -> createPassField2.requestFocus());

                HBox secondPassBox = new HBox(reenterTxt, createPassField2);
                secondPassBox.setAlignment(Pos.CENTER_LEFT);

                bodyBox.getChildren().addAll(secondPassBox, clickRegion);

                clickRegion.setOnAction(regionEvent -> {
                    createPassField2.requestFocus();
                });

                createPassField2.setOnKeyPressed(pressEvent -> {

                    KeyCode keyCode2 = pressEvent.getCode();

                    if ((keyCode2 == KeyCode.ENTER)) {

                        if (passStr.equals(createPassField2.getText())) {

                            Utils.returnObject(passStr, onSucceeded, e -> {
                                closeBtn.fire();
                            });
                        } else {
                            bodyBox.getChildren().clear();
                            createPassField2.setText("");
                            passwordField.setText("");

                            secondPassBox.getChildren().clear();
                            bodyBox.getChildren().addAll(passwordBox, clickRegion);

                        }
                    }
                });

            }
        });
        return passwordStage;
    }

    public static void createPassword(Stage passwordStage, String topTitle, Image windowLogo, Image mainLogo, Button closeBtn, EventHandler<WorkerStateEvent> onSucceeded) {
        
        passwordStage.setTitle(topTitle);

      
        HBox titleBox = createTopBar(icon, topTitle, closeBtn, passwordStage);

        Button imageBtn = App.createImageButton(mainLogo, "Password");
        imageBtn.setGraphicTextGap(20);
        HBox imageBox = new HBox(imageBtn);
        imageBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text("> Create password:");
        passwordTxt.setFill(txtColor);
        passwordTxt.setFont(txtFont);

        PasswordField passwordField = new PasswordField();

        passwordField.setFont(txtFont);
        passwordField.setId("passField");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        PasswordField createPassField2 = new PasswordField();
        HBox.setHgrow(createPassField2, Priority.ALWAYS);
        createPassField2.setId("passField");

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

        Platform.runLater(() -> passwordField.requestFocus());

        VBox passwordVBox = new VBox(titleBox, imageBox, bodyBox);

        Scene passwordScene = new Scene(passwordVBox, 600, 300);
        passwordScene.setFill(null);
        passwordScene.getStylesheets().add("/css/startWindow.css");
        passwordStage.setScene(passwordScene);

        closeBtn.setOnAction(e -> {
            passwordField.setText("");
            passwordStage.close();
        });

        passwordField.setOnKeyPressed(e1 -> {

            KeyCode keyCode = e1.getCode();

            if ((keyCode == KeyCode.ENTER || keyCode == KeyCode.TAB)) {

                String passStr = passwordField.getText();
                // createPassField.setText("");

                bodyBox.getChildren().removeAll(passwordBox, clickRegion);

                Text reenterTxt = new Text("> Confirm password:");
                reenterTxt.setFill(txtColor);
                reenterTxt.setFont(txtFont);

                Platform.runLater(() -> createPassField2.requestFocus());

                HBox secondPassBox = new HBox(reenterTxt, createPassField2);
                secondPassBox.setAlignment(Pos.CENTER_LEFT);

                bodyBox.getChildren().addAll(secondPassBox, clickRegion);

                clickRegion.setOnAction(regionEvent -> {
                    createPassField2.requestFocus();
                });

                createPassField2.setOnKeyPressed(pressEvent -> {

                    KeyCode keyCode2 = pressEvent.getCode();

                    if ((keyCode2 == KeyCode.ENTER)) {

                        if (passStr.equals(createPassField2.getText())) {

                            Utils.returnObject(passStr, onSucceeded, e -> {
                                closeBtn.fire();
                            });
                        } else {
                            bodyBox.getChildren().clear();
                            createPassField2.setText("");
                            passwordField.setText("");

                            secondPassBox.getChildren().clear();
                            bodyBox.getChildren().addAll(passwordBox, clickRegion);

                        }
                    }
                });

            }
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

        Text promptTxt = new Text("> " + prompt + ":");
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

        Scene textInputScene = new Scene(layoutVBox, 600, 330);
        textInputScene.setFill(null);
        textInputScene.getStylesheets().add("/css/startWindow.css");

        textInputStage.setScene(textInputScene);

        textInputScene.focusOwnerProperty().addListener((obs, oldVal,newVal)->{
            if(!(newVal instanceof TextField)){
                Platform.runLater(()->textField.requestFocus());
            }
        });
        Rectangle screenRect = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        textInputStage.setX((screenRect.getWidth()/2) - (textInputScene.getWidth()/2));
        textInputStage.setY((screenRect.getHeight()/2) - (textInputScene.getHeight()/2));
      
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

        HBox newTopBar = new HBox(barIconView, newTitleLbl, spacer, fillRightBtn, minimizeBtn, maximizeBtn, closeBtn);
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
        btnImageView.setFitHeight(135);
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

        Text passwordTxt = new Text("> Enter password:");
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
        

        Scene passwordScene = new Scene(layoutVBox, 600, 290);
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

    private void addAppToTray() {
        try {

            java.awt.Toolkit.getDefaultToolkit();

            BufferedImage imgBuf = SwingFXUtils.fromFXImage(new Image("/assets/icon15.png"), null);

            m_tray = java.awt.SystemTray.getSystemTray();

            m_trayIcon = new java.awt.TrayIcon((java.awt.Image) imgBuf, "Netnotes");
            m_trayIcon.setActionCommand("show");

            m_trayIcon.addActionListener(event -> Platform.runLater(() -> {
                if (event.getActionCommand().equals("show")) {
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

    }

    public static Scene getProgressScene(Image icon, String headingString, String titleContextString, SimpleStringProperty fileName, ProgressBar progressBar, Stage stage) {

        double defaultRowHeight = 40;
        Button closeBtn = new Button();


        Text fileNameProgressText = new Text(fileName.get() + " (" + String.format("%.1f", progressBar.getProgress() * 100) + "%)");
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
            fileNameProgressText.setText(fileName.get() + " (" + String.format("%.1f", newVal.doubleValue() * 100) + "%)");
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
        Scene coreFileProgressScene = new Scene(layoutBox, 600, 260);
        coreFileProgressScene.setFill(null);
        coreFileProgressScene.getStylesheets().add("/css/startWindow.css");

        // bodyTopRegion.minHeightProperty().bind(stage.heightProperty().subtract(30).divide(2).subtract(progressAlignmentBox.heightProperty()).subtract(fileNameProgressBox.heightProperty().divide(2)));
        bodyBox.prefHeightProperty().bind(stage.heightProperty().subtract(headerBox.heightProperty()).subtract(footerBox.heightProperty()).subtract(10));
        return coreFileProgressScene;
    }
}
