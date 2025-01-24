package com.netnotes;

import java.io.File;
import java.util.EventListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import com.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.stage.FileChooser.ExtensionFilter;

public class HashDataDownloader {
    
    public static class Extensions{
        public static final ExtensionFilter getJsonFilter(){
            return new FileChooser.ExtensionFilter("text/json", "*.json");
        }
        public static final ExtensionFilter getJarFilter(){
            return new FileChooser.ExtensionFilter("application/x-java-archive", "*.jar");
        }
    }

    private String m_urlString;
    private String m_fileName;
    private HashData m_fileHash = null;
    private HashData m_expectedHash;
    private ExtensionFilter[] m_filters;
    private Stage m_stage = null;
    private Image m_image = null;
    private Throwable m_failedEvent = null;
    private File m_saveFile = null;
    private File m_dlDir = null;

    public HashDataDownloader(Image logo, String urlString, String filename, File dlDir, HashData expectedHash, ExtensionFilter... filters ){
        m_image = logo;
        m_urlString = urlString;
        m_fileName = filename;
        m_expectedHash = expectedHash;
        m_filters = filters;
        m_dlDir = dlDir;
    }

    public File getDownloadDir(){
        return m_dlDir;
    }

    public void setDownloadDir(File dir){
        m_dlDir = dir;
    }

    public void start(ExecutorService execService){
        if(m_stage == null){
            Stage m_stage = new Stage();
            m_stage.getIcons().add(m_image);
            m_stage.setResizable(false);
            m_stage.initStyle(StageStyle.UNDECORATED);
            Button closeBtn = new Button();

            Runnable complete = () ->{
                m_stage.titleProperty().unbind();
                m_stage.setTitle("Download " + (m_fileHash == null ? " failed: " : "complete: ") + m_fileName);
                Button completeCloseBtn = new Button();
                HBox topBar = App.createTopBar(m_image, m_fileName, completeCloseBtn, m_stage);

                completeCloseBtn.setOnAction(e->{
                    m_stage.close();
                });

                Text headerTxt = new Text(m_fileHash == null ? "Failed" : "Complete");
                headerTxt.setFill(App.txtColor);
                headerTxt.setFont(App.txtFont);

                HBox headerBox = new HBox(headerTxt);
                HBox.setHgrow(headerBox,Priority.ALWAYS);
                headerBox.setId("headingBox");
                headerBox.setPadding(new Insets(5,10,5,10));
                headerBox.setAlignment(Pos.CENTER_LEFT);

                Tooltip navTooltip = new Tooltip("Open in File Explorer");
                navTooltip.setShowDelay(new Duration(100));

                BufferedButton navBtn = new BufferedButton("/assets/navigate-outline-white-30.png", App.MENU_BAR_IMAGE_WIDTH);
                navBtn.setId("titleBtn");
                navBtn.setTooltip(navTooltip);
                navBtn.setOnAction(e -> {
                    try {
                        Utils.openDir(m_saveFile);
                    } catch (Exception e1) {
                        Alert a = new Alert(AlertType.NONE, e1.toString(), ButtonType.OK);
                        a.setTitle("Error");
                        a.initOwner(m_stage);
                        a.setHeaderText("Error");
                        a.show();
                    }
                });
                    
        
                Text nameTxt = new Text(String.format("%-18s", "  File:"));
                nameTxt.setFill(App.txtColor);
                nameTxt.setFont(App.txtFont);
                //_RELEASE_url

                TextField nameField = new TextField(m_saveFile.getAbsolutePath());
                nameField.setFont(App.txtFont);
                nameField.setEditable(false);
                nameField.setId("formField");
                HBox.setHgrow(nameField, Priority.ALWAYS);
        
                HBox nameBox = new HBox(nameTxt, nameField, navBtn);
                HBox.setHgrow(nameBox, Priority.ALWAYS);
                nameBox.setAlignment(Pos.CENTER_LEFT);
                nameBox.setPadding(new Insets(10));
            
                Text hashTxt = new Text(String.format("%-18s", "  Hash (Blake-2b):"));
                hashTxt.setFill(App.txtColor);
                hashTxt.setFont(App.txtFont);
                //_RELEASE_url

                TextField hashField = new TextField(m_fileHash.getHashStringHex());
                hashField.setFont(App.txtFont);
                hashField.setEditable(false);
                hashField.setId("formField");
                HBox.setHgrow(hashField, Priority.ALWAYS);
        
                HBox hashBox = new HBox(hashTxt, hashField);
                HBox.setHgrow(hashBox, Priority.ALWAYS);
                hashBox.setPadding(new Insets(10));
                hashBox.setAlignment(Pos.CENTER_LEFT);

                Text expHashTxt = new Text(String.format("%-18s", "  Expected hash:"));
                expHashTxt.setFill(App.txtColor);
                expHashTxt.setFont(App.txtFont);
                //_RELEASE_url

                TextField expHashField = new TextField(m_expectedHash.getHashStringHex());
                expHashField.setFont(App.txtFont);
                expHashField.setEditable(false);
                expHashField.setId("formField");
                HBox.setHgrow(expHashField, Priority.ALWAYS);
        
                HBox expHashBox = new HBox(expHashTxt, expHashField);
                HBox.setHgrow(expHashBox, Priority.ALWAYS);
                expHashBox.setPadding(new Insets(10));
                expHashBox.setAlignment(Pos.CENTER_LEFT);


        

                VBox bodyVBox = new VBox(headerBox);
                VBox.setVgrow(bodyVBox, Priority.ALWAYS);
                bodyVBox.setId("bodyBox");

                if(m_failedEvent != null){
                    Text errorTxt = new Text(String.format("%-18s", "  Download error:"));
                    errorTxt.setFill(App.txtColor);
                    errorTxt.setFont(App.txtFont);
                    //_RELEASE_url
            
                    TextField errorField = new TextField(m_failedEvent.toString());
                    errorField.setFont(App.txtFont);
                    errorField.setEditable(false);
                    errorField.setId("formField");
                    HBox.setHgrow(errorField, Priority.ALWAYS);
            
                    HBox errorBox = new HBox(errorTxt, errorField);
                    HBox.setHgrow(errorBox, Priority.ALWAYS);
                    errorBox.setAlignment(Pos.CENTER_LEFT);
                    errorBox.setPadding(new Insets(5,10,5,10));

                    bodyVBox.getChildren().add(errorBox);
                }else{
                    if(m_fileHash != null){
                        bodyVBox.getChildren().addAll(nameBox,hashBox, expHashBox);
                    }else{
                        Text errorTxt = new Text(String.format("%-18s", "  Download error: "));
                        errorTxt.setFill(App.txtColor);
                        errorTxt.setFont(App.txtFont);
                        //_RELEASE_url
                
                        TextField errorField = new TextField("File corrupt.");
                        errorField.setFont(App.txtFont);
                        errorField.setEditable(false);
                        errorField.setId("formField");
                        HBox.setHgrow(errorField, Priority.ALWAYS);
                
                        HBox errorBox = new HBox(errorTxt, errorField);
                        HBox.setHgrow(errorBox, Priority.ALWAYS);
                        errorBox.setAlignment(Pos.CENTER_LEFT);
                        errorBox.setPadding(new Insets(5,10,5,10));

                        bodyVBox.getChildren().add(errorBox);
                    }
                }

                Text isVerified = new Text(m_expectedHash.getHashStringHex().equals(m_fileHash.getHashStringHex()) ? "Hash verified" : "Invalid hash");
                isVerified.setFont(App.txtFont);
                isVerified.setFill(App.altColor);

                Region okRegion  =new Region();
                HBox.setHgrow(okRegion, Priority.ALWAYS);

                Button okBtn = new Button("  Ok  ") ;

                HBox okBox = new HBox(isVerified, okRegion, okBtn);
                okBox.setAlignment(Pos.CENTER_LEFT);
                okBox.setPadding(new Insets(5,15,10,10));
                bodyVBox.getChildren().add(okBox);
                bodyVBox.setId("highlightRow");

                HBox layoutPaddingBox = new HBox(bodyVBox);
                HBox.setHgrow(bodyVBox, Priority.ALWAYS);
                VBox.setVgrow(bodyVBox, Priority.ALWAYS);
                layoutPaddingBox.setPadding(new Insets(5,10,5,10));

                VBox layoutVBox = new VBox(topBar, layoutPaddingBox);

                Scene statusScene = new Scene(layoutVBox, 600, 265);
                statusScene.setFill(null);
                statusScene.getStylesheets().add("/css/startWindow.css");
                
            

                m_stage.setScene(statusScene);
                m_stage.setOnCloseRequest(e->{
                    Platform.runLater(()->close());
                });
            };

     

            ProgressBar progressBar = new ProgressBar();
        
            Scene progressScene = App.getFileProgressScene(m_image, "Downloading", "Setup - Netnotes", m_fileName, progressBar, m_stage, closeBtn);
            m_stage.setScene(progressScene);

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Location");
            chooser.getExtensionFilters().addAll(m_filters);
            chooser.setInitialDirectory(m_dlDir);
            chooser.setInitialFileName(m_fileName);
            m_saveFile = chooser.showSaveDialog(m_stage);
            AtomicBoolean cancel = new AtomicBoolean(false);
            if(m_saveFile != null){
                m_stage.show();

                Utils.getUrlFileHash(m_urlString, m_saveFile, execService, (onSucceeded)->{
                    WorkerStateEvent workerStateEvent = onSucceeded;
                    Object sourceObject = workerStateEvent.getSource().getValue();

                    if(sourceObject != null && sourceObject instanceof HashData){
                        m_fileHash = (HashData) sourceObject;

                        complete.run();
                    }

                }, (onFailed)->{
                    m_failedEvent = onFailed.getSource().getException();
                    complete.run();
                },progressBar, cancel);

            }else{
                close();
            }
            closeBtn.setOnAction(e->{
                cancel.set(true);
                close();
                
            });
        }else{
            
             
                if(m_stage != null){
                    m_stage.show();
               
                    Platform.runLater(()->m_stage.requestFocus());
                }
            

        }
    }




    public void close(){
        if(m_stage != null){
            m_stage.close();
            m_stage = null;
        }
    }

    public String getUrlString(){
        return m_urlString;
    }

    public void setUrlString(String urlString){
        m_urlString = urlString;
    }

    public String getFileName(){
        return m_fileName;
    }

    public void setFileName(String fileName){
        m_fileName = fileName;
    }

    public HashData getFileHash(){
        return m_fileHash;
    }

    public void setHashData(HashData fileHash){
        m_fileHash = fileHash;
    }


    public HashData getExpectedHash(){
        return m_expectedHash;
    }

    public void setExpectedHash(HashData expectedHashData){
        m_expectedHash = expectedHashData;
    }

    public ExtensionFilter[] getFilters(){
        return m_filters;
    }

    public void setFilters(ExtensionFilter[] filters){
        m_filters = filters;
    }
    
    public Stage getStage(){
        return m_stage;
    }

    public void setStage(Stage stage){
        m_stage = stage;
    }
    
    public Image getImage(){
        return m_image;
    }

    public void setImage(Image img){
        m_image = img;
    }

    public Throwable getFailedEvent(){
        return m_failedEvent;
    }

    public void setFailedEvent(Throwable throwable){
        m_failedEvent = throwable;
    }
}

