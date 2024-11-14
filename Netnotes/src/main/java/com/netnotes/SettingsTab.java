package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

import org.reactfx.util.FxTimer;

import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class SettingsTab extends VBox implements TabInterface  {
    public final static String NAME = "Settings";
 
    private SimpleDoubleProperty m_widthObject = null;
    private final Button m_menuBtn;

    private SimpleBooleanProperty m_current = new SimpleBooleanProperty(true);
    
    public boolean getCurrent(){
        return m_current.get();
    } 

    public void setCurrent(boolean value){
        m_current.set(value);
        m_menuBtn.setId(value ? "activeMenuBtn" : "menuTabBtn");
    }

    public String getType(){
        return App.STATIC_TYPE;
    }

    public boolean isStatic(){
        return true;
    }
    

    public SettingsTab(Stage appStage, NetworksData networksData, AppData appData, SimpleDoubleProperty widthObject, Button menuBtn){
        super();
        m_menuBtn = menuBtn;
        m_widthObject = widthObject;
      

        Button settingsButton = App.createImageButton(App.logo, "Settings");

        HBox settingsBtnBox = new HBox(settingsButton);
        settingsBtnBox.setAlignment(Pos.CENTER);

        Text passwordTxt = new Text(String.format("%-18s", "  Password:"));
        passwordTxt.setFill(App.txtColor);
        passwordTxt.setFont(App.txtFont);



        Button passwordBtn = new Button("(click to update)");
     
        passwordBtn.setAlignment(Pos.CENTER_LEFT);
        passwordBtn.setId("toolBtn");
        passwordBtn.setOnAction(e -> {
            Button closeBtn = new Button();
            networksData.verifyAppKey(()->{
                Stage passwordStage = App.createPassword("Netnotes - Password", App.logo, App.logo, closeBtn, networksData.getExecService(), (onSuccess) -> {
                    Object sourceObject = onSuccess.getSource().getValue();

                    if (sourceObject != null && sourceObject instanceof String) {
                        String newPassword = (String) sourceObject;

                        if (!newPassword.equals("")) {

                            Stage statusStage = App.getStatusStage("Netnotes - Saving...", "Saving...");
                            statusStage.show();
                            FxTimer.runLater(Duration.ofMillis(100), () -> {
                                final String hash = Utils.getBcryptHashString(newPassword);
                                Platform.runLater(()->{
                                    try {

                                        appData.setAppKey(hash);
                                        appData.createKey(newPassword);
                                    } catch ( Exception e1) {
                                        try {
                                            Files.writeString(App.logFile.toPath(), "App createPassword: " +  e1.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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
        passwordBox.setPadding(new Insets(10, 0, 10, 10));
        passwordBox.setMinHeight(30);

        Tooltip checkForUpdatesTip = new Tooltip();
        checkForUpdatesTip.setShowDelay(new javafx.util.Duration(100));

        String checkImageUrlString = "/assets/checkmark-25.png";
        

        BufferedButton checkForUpdatesToggle = new BufferedButton(appData.getUpdates() ? checkImageUrlString : null, App.MENU_BAR_IMAGE_WIDTH);
        checkForUpdatesToggle.setTooltip(checkForUpdatesTip);

        checkForUpdatesToggle.setOnAction(e->{
            boolean wasUpdates = appData.getUpdates();
            
            wasUpdates = !wasUpdates;

            checkForUpdatesToggle.setImage(wasUpdates ? new Image(checkImageUrlString) : null);

            try {
                appData.setUpdates(wasUpdates);
            } catch (IOException e1) {
                Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.CANCEL);
                a.setTitle("Error: File IO");
                a.setHeaderText("Error");
                a.initOwner(appStage);
                a.show();
            }
        });

        Text versionTxt = new Text(String.format("%-18s", "  Version:"));
        versionTxt.setFill(App.txtColor);
        versionTxt.setFont(App.txtFont);
        //LATEST_RELEASE_URL

        TextField versionField = new TextField(App.CURRENT_VERSION);
        versionField.setFont(App.txtFont);
        versionField.setId("formField");
        versionField.setEditable(false);
        HBox.setHgrow(versionField, Priority.ALWAYS);
   
        HBox versionBox = new HBox(versionTxt, versionField);
        versionBox.setPadding(new Insets(10,10,5,10));
        versionBox.setAlignment(Pos.CENTER_LEFT);

        Text fileTxt = new Text(String.format("%-18s", "  File:"));
        fileTxt.setFill(App.txtColor);
        fileTxt.setFont(App.txtFont);
        //LATEST_RELEASE_URL

        TextField fileField = new TextField(appData.getAppFile().getName());
        fileField.setFont(App.txtFont);
        fileField.setEditable(false);
        fileField.setId("formField");
        HBox.setHgrow(fileField, Priority.ALWAYS);
   
        HBox fileBox = new HBox(fileTxt, fileField);
        HBox.setHgrow(fileBox, Priority.ALWAYS);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        fileBox.setPadding(new Insets(5,10,5,10));
    
        Text hashTxt = new Text(String.format("%-18s", "  Hash (Blake-2b):"));
        hashTxt.setFill(App.txtColor);
        hashTxt.setFont(App.txtFont);
        //LATEST_RELEASE_URL

        TextField hashField = new TextField(appData.appHashData().getHashStringHex());
        hashField.setFont(App.txtFont);
        hashField.setEditable(false);
        hashField.setId("formField");
        HBox.setHgrow(hashField, Priority.ALWAYS);
   
        HBox hashBox = new HBox(hashTxt, hashField);
        HBox.setHgrow(hashBox, Priority.ALWAYS);
        hashBox.setPadding(new Insets(5,10,5,10));
        hashBox.setAlignment(Pos.CENTER_LEFT);

        Text passwordHeading = new Text("Password");
        passwordHeading.setFont(App.txtFont);
        passwordHeading.setFill(App.txtColor);

        HBox passHeadingBox = new HBox(passwordHeading);
        HBox.setHgrow(passHeadingBox,Priority.ALWAYS);
        passHeadingBox.setId("headingBox");
        passHeadingBox.setPadding(new Insets(5));

        VBox passwordSettingsBox = new VBox(passHeadingBox, passwordBox);
        passwordSettingsBox.setId("bodyBox");

        Text appHeading = new Text("App");
        appHeading.setFont(App.txtFont);
        appHeading.setFill(App.txtColor);

        HBox appHeadingBox = new HBox(appHeading);
        HBox.setHgrow(appHeadingBox,Priority.ALWAYS);
        appHeadingBox.setId("headingBox");
        appHeadingBox.setPadding(new Insets(5));

        VBox appSettingsBox = new VBox(appHeadingBox, versionBox, fileBox, hashBox);
        appSettingsBox.setId("bodyBox");
        

        Text latestVersionTxt = new Text(String.format("%-18s", "  Version:"));
        latestVersionTxt.setFill(App.txtColor);
        latestVersionTxt.setFont(App.txtFont);
        //LATEST_RELEASE_URL

        Button latestVersionField = new Button("(Click to get latest info.)");
        latestVersionField.setFont(App.txtFont);
        latestVersionField.setId("formField");
        HBox.setHgrow(latestVersionField, Priority.ALWAYS);
   
        HBox latestVersionBox = new HBox(latestVersionTxt, latestVersionField);
        latestVersionBox.setPadding(new Insets(10,10,5,10));
        latestVersionBox.setAlignment(Pos.CENTER_LEFT);

        Text latestURLTxt = new Text(String.format("%-18s", "  Url:"));
        latestURLTxt.setFill(App.txtColor);
        latestURLTxt.setFont(App.txtFont);
        //LATEST_RELEASE_URL

        TextField latestURLField = new TextField();
        latestURLField.setFont(App.txtFont);
        latestURLField.setEditable(false);
        latestURLField.setId("formField");
        HBox.setHgrow(latestURLField, Priority.ALWAYS);
   
        HBox latestURLBox = new HBox(latestURLTxt, latestURLField);
        HBox.setHgrow(latestURLBox, Priority.ALWAYS);
        latestURLBox.setAlignment(Pos.CENTER_LEFT);
        latestURLBox.setPadding(new Insets(5,10,5,10));

        Text latestNameTxt = new Text(String.format("%-18s", "  File name:"));
        latestNameTxt.setFill(App.txtColor);
        latestNameTxt.setFont(App.txtFont);
        //LATEST_RELEASE_URL

        TextField latestNameField = new TextField();
        latestNameField.setFont(App.txtFont);
        latestNameField.setEditable(false);
        latestNameField.setId("formField");
        HBox.setHgrow(latestNameField, Priority.ALWAYS);
   
        HBox latestNameBox = new HBox(latestNameTxt, latestNameField);
        HBox.setHgrow(latestNameBox, Priority.ALWAYS);
        latestNameBox.setAlignment(Pos.CENTER_LEFT);
        latestNameBox.setPadding(new Insets(5,10,5,10));
    
        Text latestHashTxt = new Text(String.format("%-18s", "  Hash (Blake-2b):"));
        latestHashTxt.setFill(App.txtColor);
        latestHashTxt.setFont(App.txtFont);
        //LATEST_RELEASE_URL

        TextField latestHashField = new TextField();
        latestHashField.setFont(App.txtFont);
        latestHashField.setEditable(false);
        latestHashField.setId("formField");
        HBox.setHgrow(latestHashField, Priority.ALWAYS);
   
        HBox latestHashBox = new HBox(latestHashTxt, latestHashField);
        HBox.setHgrow(latestHashBox, Priority.ALWAYS);
        latestHashBox.setPadding(new Insets(5,10,5,10));
        latestHashBox.setAlignment(Pos.CENTER_LEFT);

        
        Text latestHeading = new Text("Latest");
        latestHeading.setFont(App.txtFont);
        latestHeading.setFill(App.txtColor);

        Region latestHeadingSpacer = new Region();
        HBox.setHgrow(latestHeadingSpacer, Priority.ALWAYS);

        Button downloadLatestBtn = new Button("Download");
        
        SimpleObjectProperty<UpdateInformation> updateInfoProperty = new SimpleObjectProperty<>();

        updateInfoProperty.addListener((obs,oldval,newval)->{
        
            latestHashField.setText(newval.getJarHashData().getHashStringHex());
            latestVersionField.setText(newval.getTagName());
            latestNameField.setText(newval.getJarName());
            latestURLField.setText(newval.getJarUrl());
        
        });
        
        

        Button getInfoBtn = new Button("Update");
        getInfoBtn.setId("checkBtn");
        getInfoBtn.setOnAction(e->{
            appData.checkForUpdates(networksData.getExecService(), updateInfoProperty);         
        });
        downloadLatestBtn.setOnAction(e->{
            SimpleObjectProperty<UpdateInformation> downloadInformation = new SimpleObjectProperty<>();
            UpdateInformation updateInfo = updateInfoProperty.get();
            File appDir = appData.getAppDir();
            if(updateInfo != null && updateInfo.getJarHashData() != null){
            
                HashData appHashData = updateInfo.getJarHashData();
                String appName = updateInfo.getJarName();
                String urlString = updateInfo.getJarUrl();
             
                HashDataDownloader dlder = new HashDataDownloader(App.logo, urlString, appName, appDir, appHashData, HashDataDownloader.Extensions.getJarFilter());
                dlder.start(networksData.getExecService());

            }else{
                downloadInformation.addListener((obs,oldval,newval)->{
                    if(newval != null){
                        updateInfoProperty.set(newval);

                        String urlString = newval.getJarUrl();
                        if(urlString.startsWith("http")){  
                            HashData latestHashData = newval.getJarHashData();
                            HashDataDownloader dlder = new HashDataDownloader(App.logo, urlString, latestNameField.getText(),appDir, latestHashData, HashDataDownloader.Extensions.getJarFilter());
                            dlder.start(networksData.getExecService());
                        }
                    }
                });
                appData.checkForUpdates(networksData.getExecService(), downloadInformation);
            }
        });

        latestVersionField.setOnAction(e->{
            getInfoBtn.fire();
        });

        HBox latestHeadingBox = new HBox(latestHeading, latestHeadingSpacer, getInfoBtn);
        HBox.setHgrow(latestHeadingBox,Priority.ALWAYS);
        latestHeadingBox.setId("headingBox");
        latestHeadingBox.setPadding(new Insets(5,10,5,10));
        latestHeadingBox.setAlignment(Pos.CENTER_LEFT);
     
        
       
        HBox downloadLatestBox = new HBox(downloadLatestBtn);
        downloadLatestBox.setAlignment(Pos.CENTER_RIGHT);
        downloadLatestBox.setPadding(new Insets(5,15,10,10));

        VBox latestSettingsBox = new VBox(latestHeadingBox, latestVersionBox, latestNameBox, latestURLBox, latestHashBox, downloadLatestBox);
        latestSettingsBox.setId("bodyBox");
        
        
        Region settingsSpacer1 = new Region();
        settingsSpacer1.setMinHeight(15);

        Region settingsSpacer2 = new Region();
        settingsSpacer2.setMinHeight(15);

        VBox settingsVBox = new VBox(settingsBtnBox, passwordSettingsBox, settingsSpacer1, appSettingsBox, settingsSpacer2, latestSettingsBox);
        HBox.setHgrow(settingsVBox, Priority.ALWAYS);

        settingsVBox.setAlignment(Pos.CENTER_LEFT);
        settingsVBox.setPadding(new Insets(5,10,5,5));



        HBox.setHgrow(settingsVBox, Priority.ALWAYS);

        getChildren().add(settingsVBox);

     
        prefWidthProperty().bind(widthObject);

    }

   private SimpleStringProperty m_titleProperty = new SimpleStringProperty(NAME);

    public SimpleStringProperty titleProperty(){
        return m_titleProperty;
    }
    
    public String getTabId(){
        return NAME;
    }

    public String getName(){
        return NAME;
    }

    public static String createTabId(String parent){
        return NAME +":" + parent;
    }

    public void shutdown(){
        this.prefWidthProperty().unbind();
        
    }
}
