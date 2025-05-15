package com.netnotes;

/**
 * Netnotes
 *
 */

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.gson.JsonParseException;

import io.netnotes.engine.AppData;
import io.netnotes.engine.AppInterface;
import io.netnotes.engine.HostServicesInterface;
import io.netnotes.engine.Network;
import io.netnotes.engine.NetworkInformation;
import io.netnotes.engine.NetworksData;
import io.netnotes.engine.Stages;
import io.netnotes.engine.Version;
import io.netnotes.engine.adapters.notes.NotesAdapter;
import io.netnotes.engine.apps.AppConstants;
import io.netnotes.engine.networks.ergo.ErgoNetwork;
import io.netnotes.engine.apps.ergoDex.ErgoDex;
import io.netnotes.engine.adapters.Adapter;

import org.ergoplatform.sdk.SecretString;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;


public class App extends Application {

    public static final Version CURRENT_VERSION = new Version("0.5.0");
 
    public static final String GITHUB_USER = "networkspore";
    public static final String GITHUB_PROJECT = "Netnotes-Linux";
    
    private NetworksData m_networksData;

    @Override
    public void start(Stage appStage) {
        Stages.initStages();

        appStage.setResizable(false);
        appStage.initStyle(StageStyle.UNDECORATED);
        appStage.setTitle("Netnotes");
        appStage.getIcons().add(Stages.logo);
        ExecutorService executorService = Executors.newScheduledThreadPool(1);
        ScheduledExecutorService schedualedExecutor = Executors.newScheduledThreadPool(1);
        try{
            AppData appData = new AppData(executorService, schedualedExecutor);
            Stages.enterPassword("Netnotes - Enter Password", appData, appStage, closeEvent -> {
                shutdownNow();
            }, enterEvent ->{ 
                Object sourceObj = enterEvent.getSource();
                if(sourceObj != null && sourceObj instanceof TextField){
                    TextField passwordField = (TextField) sourceObj;
                    if(passwordField.getText().length() > 0 && m_statusStage == null){
                        SecretString pass = SecretString.create(passwordField.getText());
                    
                        passwordField.setText("");
                        m_statusStage = Stages.getStatusStage("Verifying - Netnotes", "Verifying...");
                        m_statusStage.show();
                        Task<Object> task = new Task<Object>() {
                            @Override
                            public Object call() throws NoSuchAlgorithmException, InvalidKeySpecException {
                                
                                
                                byte[] hashBytes = appData.getAppKeyBytes();
                                BCrypt.Result result = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)).verify(pass.getData(), hashBytes);
                            
                                return result;
                            }
                        };

                        task.setOnSucceeded(onSucceeded->{
                            Object obj = onSucceeded.getSource().getValue();
                            if(obj != null && obj instanceof BCrypt.Result && ((BCrypt.Result) obj).verified){
                                
                                appData.createKey(pass, onCreated->{
                                    pass.erase();
                                    m_statusStage.close();
                                    m_statusStage = null;
                                    openNetnotes(appData, appStage);
                                }, onException->{
                                    m_statusStage.close();
                                    m_statusStage = null;
                                    Object ex = onException.getSource().getException();
                                    String msg = ex != null && ex instanceof Exception ? ((Exception) ex).toString() : "Unable to verify password";
                                    Alert a = new Alert(AlertType.ERROR, msg, ButtonType.OK);
                                    a.show();
                                    try {
                                        Files.writeString(AppConstants.LOG_FILE.toPath(), "Verification error: " + msg + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                            
                            
                            }else{
                                m_statusStage.close();
                                m_statusStage = null;
                            }
                            
                        });
                        task.setOnFailed(onFailed->{
                            m_statusStage.close();
                            m_statusStage = null;
                            Object ex = onFailed.getSource().getException();
                            String msg = ex != null && ex instanceof Exception ? ((Exception) ex).toString() : "Unable to verify password,";
                            Alert a = new Alert(AlertType.ERROR, msg, ButtonType.OK);
                            a.show();
                            try {
                                Files.writeString(AppConstants.LOG_FILE.toPath(), "Verification error: " + msg + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                
                        });
                
                        executorService.submit(task);
                        
                    }
                }
            });
            
        }catch(NullPointerException | JsonParseException | IOException e){
            setupApp(appStage, executorService, schedualedExecutor);
        }
    
    }

    private Stage m_statusStage = null;

    public void setupApp(Stage appStage, ExecutorService execService, ScheduledExecutorService scheduledExecutorService){
        Button closeBtn = new Button();

        Stages.createPassword(appStage, "Create Password - Netnotes", Stages.icon, Stages.logo, closeBtn, execService, (onFinished)->{
            Object sourceObject = onFinished.getSource().getValue();

            if (sourceObject != null && sourceObject instanceof SecretString) {
                SecretString newPassword = (SecretString) sourceObject;
                        
        
                if (!newPassword.equals("") && m_statusStage == null) {
                    m_statusStage = Stages.getStatusStage("Starting up - Netnotes", "Starting up...");
                    m_statusStage.show();

                    Task<Object> task = new Task<Object>() {
                        @Override
                        public Object call() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
                            return new AppData(newPassword, execService, scheduledExecutorService);
                        }
                    };

                    task.setOnSucceeded(onSucceeded->{
                        m_statusStage.close();
                        m_statusStage = null;
                        Object obj = onSucceeded.getSource().getValue();

                        if(obj != null && obj instanceof AppData){
                            newPassword.erase();
                            openNetnotes((AppData) obj, appStage);
                        }
                    });
                    task.setOnFailed(onFailed->{
                        Object ex = onFailed.getSource().getException();
                        String msg = ex != null && ex instanceof Exception ? ((Exception) ex).toString() : "Unable to verify password";
                     
                        Alert a = new Alert(AlertType.NONE, msg, ButtonType.OK);
                        a.setTitle("Fatal Error");
                        a.setHeaderText("Fatal Error");
                        a.showAndWait();
                        shutdownNow(); 
                    });
                      
          
                    execService.submit(task);
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
    
    private void openNetnotes(AppData appData,  Stage appStage) {

        appStage.setIconified(true);
        Stages.initStages();

        m_networksData = new NetworksData(appData, appStage, new HostServicesInterface() {
            public void showDocument(String url){
                getHostServices().showDocument(url);
            }

            public String getCodeBase(){
                return getHostServices().getCodeBase();
            }

            public String getDocumentBase(){
                return getHostServices().getDocumentBase();
            }

            public String resolveURI(String base, String rel){
                return getHostServices().resolveURI(base, rel);
            }


            
        });

        AppInterface appInterface = new AppInterface() {
            public void shutdown(){
                App.shutdownNow();
            }
            public String[] getDefaultAppIds(){
                return new String[]{ ErgoDex.NETWORK_ID};
            }
            public String[] getDefaultNetworkIds(){
                return new String[]{ErgoNetwork.NETWORK_ID };
            }
            public Network createApp(String networkId, String locationId){
                switch (networkId) {
                    
                    /*case KucoinExchange.NETWORK_ID:
                        return new KucoinExchange(this), false);
                     */
                    case ErgoDex.NETWORK_ID:
                        
                        
                        return new ErgoDex(m_networksData, locationId);
    
                    default:
                        return null;    
                }
                
            }
            public Network createNetwork(String networkId, String locationId){
                switch (networkId) {
                    
                    case ErgoNetwork.NETWORK_ID:
                        
                       
                        return new ErgoNetwork(m_networksData, locationId);                         
                    default:
                        return null;
                }
            }

            public NetworkInformation[] getSupportedNetworks(){
                return  new NetworkInformation[]{ 
                    ErgoNetwork.getNetworkInformation()
                };
            }

            public NetworkInformation[] getSupportedApps(){
                return new NetworkInformation[]{   
                    ErgoDex.getNetworkInformation()
                };
            }

            public Version getCurrentVersion(){
                return CURRENT_VERSION;
            }

            public String getGitHubUser(){
                return GITHUB_USER;
            }
            public String getGitHubProject(){
                return GITHUB_PROJECT;
            }
            @Override
            public Adapter createAdapter(String networkId) {
                switch(networkId){
                    case NotesAdapter.NETWORK_ID:
                        return new NotesAdapter(m_networksData);
                }
                return null;
            }
            @Override
            public String[] getDefaultAdapters() {
                return new String[]{ NotesAdapter.NETWORK_ID};
            }
            @Override
            public NetworkInformation[] getSupportedAdapters() {
                return new NetworkInformation[] { NotesAdapter.getNetworkInformation()};
            }
            @Override
            public void addAppResource(String arg0) throws IOException {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'addAppResource'");
            }
            @Override
            public void removeAppResource(String arg0) throws IOException {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'removeAppResource'");
            }
        };

        m_networksData.init(appInterface);

        appStage.setIconified(false);
    }
    
    public static void shutdownNow() {

        Platform.exit();
        System.exit(0);
    }


}
