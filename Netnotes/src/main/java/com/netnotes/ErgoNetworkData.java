package com.netnotes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.netnotes.IconButton.IconStyle;
import com.satergo.extra.AESEncryption;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ErgoNetworkData implements InstallerInterface {

    private File logFile = new File("netnotes-log.txt");

    public final static String[] INTALLABLE_NETWORK_IDS = new String[]{
        ErgoExplorers.NETWORK_ID,
        ErgoTokens.NETWORK_ID,
        ErgoNodes.NETWORK_ID,
        ErgoMarkets.NETWORK_ID,
        ErgoWallets.NETWORK_ID
    };

    private Stage m_manageStage = null;
    private double m_stageWidth = 750;
    private double m_stageHeight = 500;
   // private ArrayList<NoteInterface> m_networkList = new ArrayList<>();
    private ObservableList<NoteInterface> m_networkList = FXCollections.observableArrayList();

    private InstallableIcon m_focusedInstallable = null;

    private double m_leftColumnWidth = 200;
    private ErgoNetwork m_ergoNetwork;
    private File m_dataFile;

    private SimpleStringProperty m_iconStyle;
    private SimpleDoubleProperty m_gridWidth;

    private VBox m_installedVBox = new VBox();
    private VBox m_notInstalledVBox = new VBox();

    private final static long EXECUTION_TIME = 500;

    private ScheduledFuture<?> m_lastExecution = null;

    public ErgoNetworkData(String iconStyle, double gridWidth, ErgoNetwork ergoNetwork) {
      
        m_ergoNetwork = ergoNetwork;
        m_iconStyle = new SimpleStringProperty(iconStyle);
        m_gridWidth = new SimpleDoubleProperty(gridWidth);

        File appDir = ergoNetwork.getAppDir();

        if (!appDir.isDirectory()) {
            try {
                Files.createDirectory(appDir.toPath());
            } catch (IOException e) {

            }
        }

        m_dataFile = new File(appDir.getAbsolutePath() + "/" + "ergoNetworkData.dat");
        boolean isFile = m_dataFile.isFile();

        if (isFile) {
            readFile(m_ergoNetwork.getNetworksData().getAppData().appKeyProperty().get(), m_dataFile.toPath());
        }

        m_iconStyle.addListener((obs, oldVal, newVal) -> updateGrid());
        m_gridWidth.addListener((obs, oldVal, newVal) -> {
            if (!m_iconStyle.get().equals(IconStyle.ROW)) {
                updateGrid();
            }
        });
        m_ergoNetwork.getNetworksData().getAppData().appKeyProperty().addListener((obs,oldval,newval)->save());
    }

    public boolean isEmpty() {
        return m_networkList.size() == 0;
    }

    public SimpleStringProperty iconStyleProperty() {
        return m_iconStyle;
    }

    public SimpleDoubleProperty gridWidthProperty() {
        return m_gridWidth;
    }

    private void readFile(SecretKey appKey, Path filePath) {

        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(filePath);

            byte[] iv = new byte[]{
                fileBytes[0], fileBytes[1], fileBytes[2], fileBytes[3],
                fileBytes[4], fileBytes[5], fileBytes[6], fileBytes[7],
                fileBytes[8], fileBytes[9], fileBytes[10], fileBytes[11]
            };

            ByteBuffer encryptedData = ByteBuffer.wrap(fileBytes, 12, fileBytes.length - 12);

            try {
                JsonElement jsonElement = new JsonParser().parse(new String(AESEncryption.decryptData(iv, appKey, encryptedData)));
                if (jsonElement != null && jsonElement.isJsonObject()) {
                    openJson(jsonElement.getAsJsonObject());
                }
            } catch (InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException
                    | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {

            }

        } catch (IOException e) {
            try {
                Files.writeString(logFile.toPath(), "\nergNetData: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }

    }

    private void openJson(JsonObject json) {
        if (json != null) {

            JsonElement jsonArrayElement = json.get("networks");
            JsonElement stageElement = json.get("stage");

            if (stageElement != null && stageElement.isJsonObject()) {
                JsonObject stageObject = stageElement.getAsJsonObject();
                JsonElement widthElement = stageObject.get("width");
                JsonElement heightElement = stageObject.get("height");

                m_stageWidth = widthElement != null && widthElement.isJsonPrimitive() ? widthElement.getAsDouble() : m_stageWidth;
                m_stageHeight = heightElement != null && heightElement.isJsonPrimitive() ? heightElement.getAsDouble() : m_stageHeight;

            }

            JsonArray jsonArray = jsonArrayElement.getAsJsonArray();

            for (JsonElement element : jsonArray) {
                JsonObject jsonObject = element.getAsJsonObject();
                JsonElement networkIdElement = jsonObject.get("networkId");
                if (networkIdElement != null) {
                    String networkId = networkIdElement.getAsString();
                    NoteInterface network = null;
                    switch (networkId) {
                        case ErgoWallets.NETWORK_ID:
                            network = new ErgoWallets(this, jsonObject, m_ergoNetwork);
                            break;
                        case ErgoTokens.NETWORK_ID:
                            network = new ErgoTokens(this, jsonObject, m_ergoNetwork);
                            break;
                        case ErgoExplorers.NETWORK_ID:
                            network = new ErgoExplorers(jsonObject, m_ergoNetwork);
                            break;
                        case ErgoNodes.NETWORK_ID:

                            network = new ErgoNodes(jsonObject, m_ergoNetwork);

                            break;
                        case ErgoMarkets.NETWORK_ID:
                            network = new ErgoMarkets(jsonObject, m_ergoNetwork);
                            break;
                    }

                    if (network != null) {
                        addNoteInterface(network);
                    }
                }

            }

        }
    }
    private VBox m_gridBox = new VBox();

    public VBox getGridBox() {
        updateGrid();
        return m_gridBox;
    }

    private void updateGrid() {

        String currentIconStyle = m_iconStyle.get();
        m_gridBox.getChildren().clear();

        if (currentIconStyle.equals(IconStyle.ROW)) {

            for (int i = 0; i < m_networkList.size(); i++) {
                NoteInterface network = m_networkList.get(i);
                IconButton iconButton = network.getButton(currentIconStyle);
                iconButton.prefWidthProperty().bind(m_gridWidth);
                m_gridBox.getChildren().add(iconButton);
            }
        } else {

            double width = m_gridWidth.get();
            double imageWidth = 75;
            double cellPadding = 15;
            double cellWidth = imageWidth + (cellPadding * 2);
            //int numCells = m_networkList.size();

            int numCol = (int) Math.floor(width / cellWidth);
            //int numCol = floor == 0 ? 1 : floor;
            // currentNumCols.set(numCol);
         //   int numRows = numCells > 0 && numCol != 0 ? (int) Math.ceil(numCells / (double) numCol) : 1;

            ArrayList<HBox> rowsBoxes = new ArrayList<HBox>();

            ItemIterator grid = new ItemIterator();
            //j = row
            //i = col

            for (NoteInterface noteInterface : m_networkList) {
                if(rowsBoxes.size() < (grid.getJ() + 1)){
                    HBox newHBox = new HBox();
                    rowsBoxes.add(newHBox);
                    m_gridBox.getChildren().add(newHBox);
                }
                HBox rowBox = rowsBoxes.get(grid.getJ());
                rowBox.getChildren().add(noteInterface.getButton(currentIconStyle));

                if (grid.getI() < numCol) {
                    grid.setI(grid.getI() + 1);
                } else {
                    grid.setI(0);
                    grid.setJ(grid.getJ() + 1);
                }
            }

        }
     
    }

    private ArrayList<InstallableIcon> updateInstallables() {
        ArrayList<InstallableIcon> installables = new ArrayList<>();
        for (String networkId : INTALLABLE_NETWORK_IDS) {
            boolean installed;

            installed = getNetwork(networkId) != null;

            InstallableIcon installableIcon = new InstallableIcon(this, networkId, installed);

            installables.add(installableIcon);
        }
        return installables;
    }

    public void showwManageStage() {
        if (m_manageStage == null) {

            ArrayList<InstallableIcon> installables = updateInstallables();

            double stageWidth = m_stageWidth;
            double stageHeight = m_stageHeight;

            m_manageStage = new Stage();
            m_manageStage.setTitle("Manage - Ergo Network");
            m_manageStage.getIcons().add(ErgoNetwork.getSmallAppIcon());
            m_manageStage.setResizable(false);
            m_manageStage.initStyle(StageStyle.UNDECORATED);

            Button closeBtn = new Button();
            Button maximizeBtn = new Button();

            HBox titleBar = App.createTopBar(ErgoNetwork.getSmallAppIcon(), maximizeBtn, closeBtn, m_manageStage);


            Text headingText = new Text("Manage");
            headingText.setFont(App.txtFont);
            headingText.setFill(Color.WHITE);

            HBox headingBox = new HBox(headingText);
            headingBox.prefHeight(35);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(10, 10, 10, 10));
            headingBox.setId("headingBox");

            HBox headingPaddingBox = new HBox(headingBox);

            headingPaddingBox.setPadding(new Insets(5, 0, 2, 0));

            VBox headerBox = new VBox(headingPaddingBox);

            headerBox.setPadding(new Insets(0, 5, 0, 5));

            Region menuSpacer = new Region();
            HBox.setHgrow(menuSpacer, Priority.ALWAYS);

           

        

            Button installBtn = new Button("Install");
            installBtn.setFont(App.txtFont);

            Button installAllBtn =  new Button("All");
            installAllBtn.setMinWidth(60);
            installAllBtn.setFont(App.txtFont);

            Button removeBtn = new Button("Remove");
            removeBtn.setPrefWidth(m_leftColumnWidth);
            removeBtn.setId("menuBarBtn");

            Button removeAllBtn = new Button("All");
            removeAllBtn.setMinWidth(60);
            removeAllBtn.setId("menuBarBtn");

            Region vSpacerOne = new Region();
            VBox.setVgrow(vSpacerOne, Priority.ALWAYS);
   
          //  m_installedVBox.setId("bodyBox");
          /*  VBox installedPaddedVBox = new VBox(m_installedVBox, vSpacerOne);

            installedPaddedVBox.setId("bodyBox");
            VBox.setVgrow(installedPaddedVBox, Priority.ALWAYS);*/
            m_installedVBox.setId("bodyBox");
            m_installedVBox.setMaxWidth(m_leftColumnWidth);
            VBox.setVgrow(m_installedVBox, Priority.ALWAYS);

            Region leftSpacer = new Region();
            HBox.setHgrow(leftSpacer, Priority.ALWAYS);

            Region topSpacer = new Region();
            VBox.setVgrow(topSpacer, Priority.ALWAYS);

            Region installAllSpacer = new Region();
            installAllSpacer.setMinWidth(5);

            HBox addBox = new HBox(leftSpacer, installBtn, installAllSpacer, installAllBtn);
            addBox.setPadding(new Insets(15,2, 15,15));
            
            HBox.setHgrow(m_notInstalledVBox, Priority.ALWAYS);
            m_notInstalledVBox.setId("bodyRight");
         

            

            HBox rmvBox = new HBox(removeBtn, removeAllBtn);

            VBox leftSide = new VBox(m_installedVBox, rmvBox);
            leftSide.setPadding(new Insets(5));
            leftSide.setMaxWidth(m_leftColumnWidth + 20);
            
  
            VBox.setVgrow(leftSide, Priority.ALWAYS);

            VBox rightSide = new VBox(m_notInstalledVBox, addBox);
            rightSide.setPadding(new Insets(5,5,0,5));
            HBox.setHgrow(rightSide, Priority.ALWAYS);
          //  rightSide.setId("bodyRight");

            /*VBox rightSidePaddingBox = new VBox(rightSide);
            rightSidePaddingBox.setPadding(new Insets(0, 2, 0, 5));
            HBox.setHgrow(rightSidePaddingBox,Priority.ALWAYS);*/

            HBox columnsHBox = new HBox(leftSide, rightSide);
            VBox.setVgrow(columnsHBox, Priority.ALWAYS);
            columnsHBox.setId("bodyBox");
            columnsHBox.setPadding(new Insets(10, 10, 10, 10));

            VBox.setVgrow(m_notInstalledVBox, Priority.ALWAYS);

            Button okBtn = new Button("Ok");
            okBtn.setPadding(new Insets(5,25, 5, 25));

            HBox footerBar = new HBox(menuSpacer, okBtn);
            footerBar.setAlignment(Pos.CENTER_LEFT);
            footerBar.setPadding(new Insets(1, 0, 1, 0));
            HBox.setHgrow(footerBar, Priority.ALWAYS);

            VBox footerBox = new VBox(footerBar);
            footerBox.setPadding(new Insets(15));

            VBox bodyBox = new VBox(columnsHBox, footerBox);
            bodyBox.setPadding(new Insets(5));
            bodyBox.setId("bodyBox");
            HBox.setHgrow(bodyBox, Priority.ALWAYS);
            VBox.setVgrow(bodyBox,Priority.ALWAYS);

            VBox paddingBodyBox = new VBox(bodyBox);
            paddingBodyBox.setPadding(new Insets(0, 2,2,2));
            HBox.setHgrow(paddingBodyBox, Priority.ALWAYS);
            VBox.setVgrow(paddingBodyBox,Priority.ALWAYS);

            
            Region spacer = new Region();
            spacer.setMinHeight(2);

            VBox layoutBox = new VBox(titleBar,headerBox, paddingBodyBox, spacer);

            layoutBox.setPadding(new Insets(0, 2, 2, 2));
            Scene scene = new Scene(layoutBox, stageWidth, stageHeight);
            scene.setFill(null);
            scene.getStylesheets().add("/css/startWindow.css");
            m_manageStage.setScene(scene);

            closeBtn.setOnAction(e -> {
                m_manageStage.close();
                m_focusedInstallable = null;
                m_manageStage = null;
            });

            scene.focusOwnerProperty().addListener((e) -> {
                if (scene.focusOwnerProperty().get() instanceof InstallableIcon) {
                    InstallableIcon installable = (InstallableIcon) scene.focusOwnerProperty().get();

                    m_focusedInstallable = installable;
                } else {
                    if (scene.focusOwnerProperty().get() instanceof Button) {
                        Button focusedButton = (Button) scene.focusOwnerProperty().get();
                        String buttonString = focusedButton.getText();
                        if (!(buttonString.equals(installBtn.getText()) || buttonString.equals(removeBtn.getText()))) {

                            m_focusedInstallable = null;

                        }
                    }
                }

            });

            okBtn.setOnAction(e -> {
                closeBtn.fire();
            });

            installBtn.setOnAction(e -> {

                if (m_focusedInstallable != null && (!m_focusedInstallable.getInstalled())) {
                    installNetwork(m_focusedInstallable.getNetworkId());
                }
                m_focusedInstallable = null;
            });

            removeBtn.setOnAction(e -> {
                if (m_focusedInstallable != null && (m_focusedInstallable.getInstalled())) {
                    removeNetwork(m_focusedInstallable.getNetworkId());
                }
                m_focusedInstallable = null;
            });

            removeAllBtn.setOnAction(e -> {
                removeAll();
            });

            installAllBtn.setOnAction(e -> {
                addAll();
            });

            Runnable runSave = () -> {
                save();
            };

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });

            scene.widthProperty().addListener((obs, oldVal, newVal) -> {
                m_stageWidth = newVal.doubleValue();
                if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                    m_lastExecution.cancel(false);
                }
                m_lastExecution = executor.schedule(runSave, EXECUTION_TIME, TimeUnit.MILLISECONDS);
            });

            scene.heightProperty().addListener((obs, oldVal, newVal) -> {
                m_stageHeight = newVal.doubleValue();
                if (m_lastExecution != null && !(m_lastExecution.isDone())) {
                    m_lastExecution.cancel(false);
                }
                m_lastExecution = executor.schedule(runSave, EXECUTION_TIME, TimeUnit.MILLISECONDS);
            });

            m_manageStage.show();
          
            ResizeHelper.addResizeListener(m_manageStage, 400, 200, Double.MAX_VALUE, Double.MAX_VALUE);

            updateAvailableLists(installables);

            Platform.runLater(()->m_manageStage.requestFocus());
        } else {
            m_manageStage.show();
        }
      
    }

    private void updateAvailableLists(ArrayList<InstallableIcon> m_installables) {
        if (m_installables != null && m_installedVBox != null && m_notInstalledVBox != null) {
            m_installedVBox.getChildren().clear();

            m_notInstalledVBox.getChildren().clear();

            //  double listImageWidth = 30;
            //    double listImagePadding = 5;
            ItemIterator grid = new ItemIterator();

            double imageWidth = new IconButton().getImageWidth();
            double cellPadding = new IconButton().getPadding().getLeft();
            double cellWidth = imageWidth + cellPadding;
         //  double numCells = INTALLABLE_NETWORK_IDS.length - m_networkList.size();
            double boxWidth = m_stageWidth - (m_leftColumnWidth);

            int floor = (int) Math.floor(boxWidth / (cellWidth));
            int numCol = floor == 0 ? 1 : floor;
           // int numRows = numCells > 0 && numCol != 0 ? (int) Math.ceil(numCells / (double) numCol) : 1;

            ArrayList<HBox> rowsBoxes = new ArrayList<HBox>();
            //j = row
            //i = col

            for (InstallableIcon installable : m_installables) {
                if(installable.getInstalled()){
                    installable.prefWidthProperty().bind(m_installedVBox.widthProperty());
                    m_installedVBox.getChildren().add(installable);
                }else{
                    if(rowsBoxes.size() < (grid.getJ() + 1)){
                        HBox newHBox = new HBox();
                        rowsBoxes.add(newHBox);
                        m_notInstalledVBox.getChildren().add(newHBox);
                      
                    }
    
                    HBox rowBox = rowsBoxes.get(grid.getJ());

                    installable.setPrefWidth(IconButton.NORMAL_IMAGE_WIDTH);

                    rowBox.getChildren().add(installable);
    
                    if (grid.getI() < numCol) {
                        grid.setI(grid.getI() + 1);
                    } else {
                        grid.setI(0);
                        grid.setJ(grid.getJ() + 1);
                    }
                }

            }
        }
    }

    public void installNetwork(String networkID) {
        installNetwork(networkID, true);
    }

    public void installNetwork(String networkId, boolean update) {
        NoteInterface noteInterface = null;
        switch (networkId) {

            case ErgoTokens.NETWORK_ID:
                noteInterface = new ErgoTokens(this, m_ergoNetwork);
                break;
            case ErgoWallets.NETWORK_ID:
                noteInterface = new ErgoWallets(this, m_ergoNetwork);
                break;
            case ErgoExplorers.NETWORK_ID:
                noteInterface = new ErgoExplorers(m_ergoNetwork);
                break;
            case ErgoNodes.NETWORK_ID:
                noteInterface = new ErgoNodes(m_ergoNetwork);
                break;
            case ErgoMarkets.NETWORK_ID:
                noteInterface = new ErgoMarkets(m_ergoNetwork);
                break;
        }
        if (noteInterface != null) {
            addNoteInterface(noteInterface);
            if (update) {
                updateAvailableLists(updateInstallables());
                save();
                updateGrid();
            }
        }

    }

    

    public boolean addNoteInterface(NoteInterface noteInterface) {
        // int i = 0;

        String networkId = noteInterface.getNetworkId();

        if (getNetwork(networkId) == null) {

            m_networkList.add(noteInterface);

            noteInterface.addUpdateListener((obs, oldValue, newValue) -> save());

            return true;
        }
        return false;
    }

    public void removeNetwork(String networkId) {
        removeNetwork(networkId, true);
    }

    public void removeNetwork(String networkId, boolean save) {

        removeNoteInterface(networkId);

        updateAvailableLists(updateInstallables());
        updateGrid();
        if (save) {
            save();
        }

    }

    public boolean removeNoteInterface(String networkId) {
        boolean success = false;
        for (int i = 0; i < m_networkList.size(); i++) {
            NoteInterface noteInterface = m_networkList.get(i);
            if (networkId.equals(noteInterface.getNetworkId())) {
                m_networkList.remove(noteInterface);
                noteInterface.remove();

                success = true;
                break;
            }
        }

        return success;
    }

    public void shutdown() {

        for (int i = 0; i < m_networkList.size(); i++) {
            NoteInterface noteInterface = m_networkList.get(i);
            noteInterface.shutdown();
            m_networkList.remove(noteInterface);

        }
    }

    public NoteInterface getNetwork(String networkId) {
        if (networkId != null) {

            for (int i = 0; i < m_networkList.size(); i++) {
                NoteInterface network = m_networkList.get(i);

                if (network.getNetworkId().equals(networkId)) {
                    return network;
                }
            }

        }
        return null;
    }

    public void addAll() {
        for (String networkId : INTALLABLE_NETWORK_IDS) {
            if (getNetwork(networkId) == null) {
                installNetwork(networkId, false);
            }
        }
        updateAvailableLists(updateInstallables());
        updateGrid();
        save();
    }

    public void removeAll() {

        while (m_networkList.size() > 0) {
            NoteInterface noteInterface = m_networkList.get(0);
            m_networkList.remove(noteInterface);
            noteInterface.remove();
        }

        updateAvailableLists(updateInstallables());
        updateGrid();
        save();
    }

    public JsonObject getStageObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("width", m_stageWidth);
        jsonObject.addProperty("height", m_stageHeight);
        jsonObject.addProperty("iconStyle", m_iconStyle.get());
        return jsonObject;
    }

    public ErgoNetwork getErgoNetwork(){
        return m_ergoNetwork;
    }

    public void save() {
        JsonObject fileObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();

        for (NoteInterface noteInterface : m_networkList) {

            JsonObject jsonObj = noteInterface.getJsonObject();
            jsonArray.add(jsonObj);

        }

        fileObject.add("networks", jsonArray);
        fileObject.add("stage", getStageObject());
        String jsonString = fileObject.toString();

        //  byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);
        // String fileHexString = Hex.encodeHexString(bytes);
        try {

            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            byte[] iV = new byte[12];
            secureRandom.nextBytes(iV);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iV);

            cipher.init(Cipher.ENCRYPT_MODE, m_ergoNetwork.getNetworksData().getAppData().appKeyProperty().get(), parameterSpec);

            byte[] encryptedData = cipher.doFinal(jsonString.getBytes());

            try {

                if (m_dataFile.isFile()) {
                    Files.delete(m_dataFile.toPath());
                }

                FileOutputStream outputStream = new FileOutputStream(m_dataFile);
                FileChannel fc = outputStream.getChannel();

                ByteBuffer byteBuffer = ByteBuffer.wrap(iV);

                fc.write(byteBuffer);

                int written = 0;
                int bufferLength = 1024 * 8;

                while (written < encryptedData.length) {

                    if (written + bufferLength > encryptedData.length) {
                        byteBuffer = ByteBuffer.wrap(encryptedData, written, encryptedData.length - written);
                    } else {
                        byteBuffer = ByteBuffer.wrap(encryptedData, written, bufferLength);
                    }

                    written += fc.write(byteBuffer);
                }

                outputStream.close();

            } catch (IOException e) {
                try {
                    Files.writeString(logFile.toPath(), "\nergNetData: save():" + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e1) {

                }
            }

        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
            try {
                Files.writeString(logFile.toPath(), "\nergNetData Key error: " + e.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e1) {

            }
        }
    }

    public void addNetworkListener(ListChangeListener<? super NoteInterface> listener){
        
        m_networkList.addListener(listener);
    }

    public void removeNetworkListener(ListChangeListener<? super NoteInterface> listener){
        m_networkList.removeListener(listener);
    }

}
