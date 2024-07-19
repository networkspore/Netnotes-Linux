package com.netnotes;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.ergoplatform.appkit.Mnemonic;
import org.ergoplatform.appkit.MnemonicValidationException;
import org.ergoplatform.appkit.SecretString;

import com.devskiller.friendly_id.FriendlyId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.satergo.Wallet;
import com.utils.Utils;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ErgoWalletsRowArea extends VBox implements TabInterface {
    private SimpleStringProperty m_titleProperty = new SimpleStringProperty("Ergo Wallets");
    private String m_tabId;
    private boolean m_current = false;
    private NoteMsgInterface m_walletMsgInterface = null;
    private ErgoNetworkData m_ergoNetworkData;
    private NoteMsgInterface m_ergoWalletsMsgInterface;
    private NoteMsgInterface m_ergoTokensMsgInterface;

    private SimpleBooleanProperty showWallet = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty showBalance = new SimpleBooleanProperty(false);

    private SimpleObjectProperty<AppBox> m_currentBox = new SimpleObjectProperty<>(null);

    private SimpleObjectProperty<ConfigBox> m_configBox = new SimpleObjectProperty<>(null);
    private VBox m_mainBox;
    private final String walletBtnDefaultString = "[select]";
    private final String lockString = "Locked ";

    private Stage m_appStage;

    public ErgoWalletsRowArea(Stage appStage, ErgoNetworkData ergoNetworkData) {
        super();
        VBox.setVgrow(this, Priority.ALWAYS);
        m_ergoNetworkData = ergoNetworkData;
        m_tabId = FriendlyId.createFriendlyId();

        m_appStage = appStage;

        ImageView walletIconView = new ImageView(m_ergoNetworkData.getErgoWallets().getSmallAppIcon());
        walletIconView.setPreserveRatio(true);
        walletIconView.setFitHeight(18);

        LockField lockBox = new LockField(lockString, "≬", "Address ", "Enter Password");
        lockBox.setPadding(new Insets(2, 5, 2, 0));
        lockBox.setAlignment(Pos.CENTER_LEFT);
        lockBox.setMaxHeight(15);
        HBox.setHgrow(lockBox, Priority.ALWAYS);

        TextField walletField = new TextField();
        HBox.setHgrow(walletField, Priority.ALWAYS);

        Label toggleShowBalance = new Label(showBalance.get() ? "⏷ " : "⏵ ");
        toggleShowBalance.setId("caretBtn");
        toggleShowBalance.setMinWidth(25);

        walletField.setMaxHeight(40);
        walletField.setAlignment(Pos.CENTER_LEFT);
        walletField.setText(walletBtnDefaultString);

        walletField.setMinWidth(90);
        walletField.setEditable(false);
        walletField.setId("hand");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ImageView closeImage = App.highlightedImageView(App.closeImg);
        closeImage.setFitHeight(20);
        closeImage.setFitWidth(20);
        closeImage.setPreserveRatio(true);

        Label toggleShowWallets = new Label(showWallet.get() ? "⏷ " : "⏵ ");
        toggleShowWallets.setId("caretBtn");
        toggleShowWallets.setMinWidth(25);
        ContextMenu walletMenu = new ContextMenu();

        Label openWalletBtn = new Label("⏷");
        openWalletBtn.setId("lblBtn");

        Tooltip optionsTooltip = new Tooltip("Options");
        optionsTooltip.setShowDelay(Duration.millis(100));

        ContextMenu walletContextMenu = new ContextMenu();

        Label walletMenuBtn = new Label("⋮");
        walletMenuBtn.setId("lblBtn");
        walletMenuBtn.setTooltip(optionsTooltip);
        walletMenuBtn.setFocusTraversable(true);

        Text walletTopLabel = new Text("Wallet");
        walletTopLabel.setFont(App.txtFont);
        walletTopLabel.setFill(App.txtColor);

        MenuItem openWalletMenuItem = new MenuItem("⇲   Open...");

        MenuItem newWalletMenuItem = new MenuItem("⇱   New");

        MenuItem restoreWalletMenuItem = new MenuItem("⟲   Restore");

        MenuItem removeWalletMenuItem = new MenuItem("🗑   Remove");

        Runnable openWalletMenu = () -> {
            Point2D p = walletMenuBtn.localToScene(0.0, 0.0);

            walletContextMenu.show(walletMenuBtn,
                    p.getX() + walletMenuBtn.getScene().getX() + walletMenuBtn.getScene().getWindow().getX(),
                    (p.getY() + walletMenuBtn.getScene().getY() + walletMenuBtn.getScene().getWindow().getY()
                            + walletMenuBtn.getLayoutBounds().getHeight()));

        };

        walletMenuBtn.setOnMouseClicked(e -> openWalletMenu.run());
        walletMenuBtn.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                openWalletMenu.run();
            }
        });

        walletContextMenu.getItems().addAll(newWalletMenuItem, openWalletMenuItem, restoreWalletMenuItem,
                removeWalletMenuItem);

        Runnable openWallet = () -> {

            FileChooser openFileChooser = new FileChooser();
            openFileChooser.setTitle("Select wallet (*.erg)");
            openFileChooser.setInitialDirectory(AppData.HOME_DIRECTORY);
            openFileChooser.getExtensionFilters().add(ErgoWallets.ergExt);
            openFileChooser.setSelectedExtensionFilter(ErgoWallets.ergExt);

            File walletFile = openFileChooser.showOpenDialog(appStage);

            if (walletFile != null) {
                String fileName = walletFile.getName();
                fileName = fileName.endsWith(".erg") ? fileName.substring(0, fileName.length() - 4) : fileName;

                JsonObject note = Utils.getCmdObject("openWallet");
                note.addProperty("networkId", m_ergoNetworkData.getId());
                note.addProperty("path", walletFile.getAbsolutePath());

                Object result = m_ergoNetworkData.getErgoWallets().sendNote(note);

                if (result != null && result instanceof JsonObject) {
                    JsonObject walletJson = (JsonObject) result;
                    JsonElement idElement = walletJson.get("id");
                    JsonElement configIdElement = walletJson.get("configId");
                    if (idElement != null && idElement.isJsonPrimitive() && configIdElement != null
                            && configIdElement.isJsonPrimitive()) {
                        String id = walletJson.get("id").getAsString();
                        String configId = walletJson.get("configId").getAsString();

                        JsonObject getWalletObject = Utils.getCmdObject("getWalletById");

                        getWalletObject.addProperty("networkId", m_ergoNetworkData.getId());
                        getWalletObject.addProperty("id", id);
                        NoteInterface walletData = (NoteInterface) ergoNetworkData.getErgoWallets()
                                .sendNote(getWalletObject);
                        if (walletData != null) {

                            m_ergoNetworkData.selectedWallet().set(walletData);
                            showWallet.set(true);

                            m_configBox.set(new ConfigBox(configId));
                        }
                    }

                }
            }
        };
        Runnable newWallet = () -> {
            NewWalletBox newWalletBox = new NewWalletBox(true, (onCompleted) -> {
                Object obj = onCompleted.getSource().getValue();
                m_currentBox.set(null);

                if (obj != null && obj instanceof JsonObject) {
                    JsonObject walletJson = (JsonObject) obj;
                    JsonElement idElement = walletJson.get("id");
                    JsonElement configIdElement = walletJson.get("configId");

                    if (idElement != null && idElement.isJsonPrimitive()) {
                        String id = walletJson.get("id").getAsString();
                        String configId = configIdElement != null && configIdElement.isJsonPrimitive()? configIdElement.getAsString() : null;

                        JsonObject getWalletObject = Utils.getCmdObject("getWalletById");

                        getWalletObject.addProperty("networkId", m_ergoNetworkData.getId());
                        getWalletObject.addProperty("id", id);
                        NoteInterface walletData = (NoteInterface) m_ergoNetworkData.getErgoWallets()
                                .sendNote(getWalletObject);

                        if (walletData != null) {
                            
                            m_ergoNetworkData.selectedWallet().set(walletData);
                            showWallet.set(true);
                            if(configId != null){
                                m_configBox.set(new ConfigBox(configId));
                            }
                        }
                    }
                    
                }
            });
            m_currentBox.set(newWalletBox);
        };

        Runnable restoreWallet = () -> {
            NewWalletBox newWalletBox = new NewWalletBox(false, (onCompleted) -> {
                Object obj = onCompleted.getSource().getValue();
                m_currentBox.set(null);

                if (obj != null && obj instanceof JsonObject) {
                    JsonObject walletJson = (JsonObject) obj;
                    JsonElement idElement = walletJson.get("id");
                    JsonElement configIdElement = walletJson.get("configId");

                    if (idElement != null && idElement.isJsonPrimitive()) {
                        String id = walletJson.get("id").getAsString();
                        String configId = configIdElement != null && configIdElement.isJsonPrimitive()? configIdElement.getAsString() : null;

                        JsonObject getWalletObject = Utils.getCmdObject("getWalletById");

                        getWalletObject.addProperty("networkId", m_ergoNetworkData.getId());
                        getWalletObject.addProperty("id", id);
                        NoteInterface walletData = (NoteInterface) m_ergoNetworkData.getErgoWallets()
                                .sendNote(getWalletObject);

                        if (walletData != null) {
                            
                            m_ergoNetworkData.selectedWallet().set(walletData);
                            showWallet.set(true);
                            if(configId != null){
                                m_configBox.set(new ConfigBox(configId));
                            }
                        }
                    }
                    
                }
            });
            m_currentBox.set(newWalletBox);
        };


        openWalletMenuItem.setOnAction(e -> openWallet.run());
        newWalletMenuItem.setOnAction(e -> newWallet.run());
        restoreWalletMenuItem.setOnAction(e -> restoreWallet.run());
        removeWalletMenuItem.setOnAction(e -> {
        });

        Label configBtn = new Label("⚙");
        configBtn.setId("lblBtn");

        HBox walletFieldBox = new HBox(walletField, openWalletBtn);
        HBox.setHgrow(walletFieldBox, Priority.ALWAYS);
        walletFieldBox.setAlignment(Pos.CENTER_LEFT);
        walletFieldBox.setId("bodyBox");
        walletFieldBox.setPadding(new Insets(0, 1, 0, 0));
        walletFieldBox.setMaxHeight(18);

        HBox walletMenuBtnPadding = new HBox(walletMenuBtn);
        walletMenuBtnPadding.setPadding(new Insets(0, 0, 0, 5));

        HBox walletBtnBox = new HBox(walletFieldBox, walletMenuBtnPadding);
        walletBtnBox.setPadding(new Insets(0, 2, 0, 5));
        HBox.setHgrow(walletBtnBox, Priority.ALWAYS);

        walletBtnBox.setAlignment(Pos.CENTER_LEFT);

        Runnable showWalletMenu = () -> {

            Point2D p = walletBtnBox.localToScene(0.0, 0.0);
            walletMenu.setPrefWidth(walletBtnBox.getLayoutBounds().getWidth());

            walletMenu.show(walletBtnBox,
                    5 + p.getX() + walletBtnBox.getScene().getX() + walletBtnBox.getScene().getWindow().getX(),
                    (p.getY() + walletBtnBox.getScene().getY() + walletBtnBox.getScene().getWindow().getY())
                            + walletBtnBox.getLayoutBounds().getHeight() - 1);
        };
        /*
         * openWalletMenuItem.setOnAction(e->m_walletActionProperty.set("Open"));
         * newWalletMenuItem.setOnAction(e->m_walletActionProperty.set("New"));
         * restoreWalletMenuItem.setOnAction(e->m_walletActionProperty.set("Restore"));
         * removeWalletMenuItem.setOnAction(e->m_walletActionProperty.set("Remove"));
         * 
         */

        MenuItem openWalletItem = new MenuItem("[Open]");
        openWalletItem.setOnAction(e -> openWallet.run());

        MenuItem newWalletItem = new MenuItem("[New]");
        newWalletItem.setOnAction(e -> newWallet.run());

        MenuItem restoreWalletItem = new MenuItem("[Restore]                ");
        restoreWalletItem.setOnAction(e -> restoreWallet.run());

        MenuItem removeWalletItem = new MenuItem("[Remove]");
        removeWalletItem.setOnAction(e -> {
        });

        Runnable showOpenWalletMenu = () -> {
            JsonObject note = Utils.getCmdObject("getWallets");
            note.addProperty("networkId", m_ergoNetworkData.getId());

            JsonArray walletIds = (JsonArray) ergoNetworkData.getErgoWallets().sendNote(note);

            walletMenu.getItems().clear();
            if (walletIds != null) {

                for (JsonElement element : walletIds) {
                    if (element != null && element instanceof JsonObject) {
                        JsonObject json = element.getAsJsonObject();

                        String name = json.get("name").getAsString();
                        String id = json.get("id").getAsString();

                        MenuItem walletItem = new MenuItem(String.format("%-50s", " " + name));

                        walletItem.setOnAction(action -> {
                            JsonObject getWalletObject = Utils.getCmdObject("getWalletById");
                            getWalletObject.addProperty("networkId", m_ergoNetworkData.getId());
                            getWalletObject.addProperty("id", id);
                            NoteInterface walletData = (NoteInterface) ergoNetworkData.getErgoWallets()
                                    .sendNote(getWalletObject);
                            lockBox.setLocked();
                            m_ergoNetworkData.selectedWallet().set(walletData);
                            showWallet.set(true);
                        });

                        walletMenu.getItems().add(walletItem);
                    }
                }

            }

            walletMenu.getItems().addAll(newWalletItem, openWalletItem, restoreWalletItem, removeWalletItem);
            showWalletMenu.run();

        };

        openWalletBtn.setOnMouseClicked(e -> {
            showOpenWalletMenu.run();
        });

        walletField.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            showOpenWalletMenu.run();
        });

        HBox walletLabelBox = new HBox(walletTopLabel);
        walletLabelBox.setPadding(new Insets(0, 5, 0, 5));
        walletLabelBox.setAlignment(Pos.CENTER_LEFT);

        HBox walletsTopBar = new HBox(toggleShowWallets, walletIconView, walletLabelBox, walletBtnBox);
        walletsTopBar.setAlignment(Pos.CENTER_LEFT);
        walletsTopBar.setPadding(new Insets(2));

        VBox walletBodyBox = new VBox();
        walletBodyBox.setPadding(new Insets(0, 0, 0, 5));
        walletBodyBox.setId("networkBox");

        VBox walletBodyPaddingBox = new VBox();
        HBox.setHgrow(walletBodyPaddingBox, Priority.ALWAYS);

        VBox walletLayoutBox = new VBox(walletsTopBar, walletBodyPaddingBox);
        HBox.setHgrow(walletLayoutBox, Priority.ALWAYS);

        m_mainBox = new VBox(walletLayoutBox);
        m_mainBox.setPadding(new Insets(0));

        Button disableWalletBtn = new Button();
        disableWalletBtn.setId("toolBtn");
        disableWalletBtn.setGraphic(closeImage);
        disableWalletBtn.setPadding(new Insets(0, 1, 0, 1));
        disableWalletBtn.setOnAction(e -> {
            showWallet.set(false);

            lockBox.setLocked();
            m_ergoNetworkData.selectedWallet().set(null);
        });

        ScrollPane walletBoxScroll = new ScrollPane();

        m_mainBox.prefWidthProperty()
                .bind(Bindings.createObjectBinding(() -> walletBoxScroll.viewportBoundsProperty().get().getWidth() - 2,
                        walletBoxScroll.viewportBoundsProperty()));

        m_currentBox.addListener((obs, oldval, newval) -> {
            m_mainBox.getChildren().clear();
            if (newval != null) {
                m_mainBox.getChildren().add(newval);
            } else {
                m_mainBox.getChildren().add(walletLayoutBox);
            }

        });

        walletBoxScroll.setContent(m_mainBox);

        m_currentBox.set(null);

        showWallet.addListener((obs, oldval, newval) -> {

            toggleShowWallets.setText(newval ? "⏷ " : "⏵ ");

            if (newval) {
                if (!walletBodyPaddingBox.getChildren().contains(walletBodyBox)) {
                    walletBodyPaddingBox.getChildren().add(walletBodyBox);
                }
            } else {
                if (walletBodyPaddingBox.getChildren().contains(walletBodyBox)) {
                    walletBodyPaddingBox.getChildren().remove(walletBodyBox);
                }
            }
        });

        ContextMenu addressesMenu = new ContextMenu();

        ImageView addressIconView = new ImageView(new Image(ErgoWallets.getSmallAppIconString()));
        addressIconView.setFitWidth(20);
        addressIconView.setPreserveRatio(true);

        toggleShowWallets.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (m_ergoNetworkData.selectedWallet().get() != null) {
                showWallet.set(!showWallet.get());
            } else {
                showOpenWalletMenu.run();
            }
        });

        lockBox.setOnMenu((e) -> {
            NoteInterface selectedWallet = m_ergoNetworkData.selectedWallet().get();
            addressesMenu.getItems().clear();

            if (selectedWallet != null && lockBox.getLockId() != null) {
                JsonObject json = Utils.getCmdObject("getAddresses");
                json.addProperty("accessId", lockBox.getLockId());
                json.addProperty("networkId", m_ergoNetworkData.getId());

                Object successObject = selectedWallet.sendNote(json);

                if (successObject != null && successObject instanceof JsonArray) {

                    JsonArray jsonArray = (JsonArray) successObject;

                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonElement element = jsonArray.get(i);
                        if (element != null && element.isJsonObject()) {

                            JsonObject jsonObj = element.getAsJsonObject();
                            String address = jsonObj.get("address").getAsString();

                            MenuItem addressMenuItem = new MenuItem(address);
                            addressMenuItem.setOnAction(e1 -> {
                                lockBox.setText(address);
                            });

                            addressesMenu.getItems().add(addressMenuItem);
                        }
                    }

                    Point2D p = lockBox.localToScene(0.0, 0.0);

                    addressesMenu.show(lockBox,
                            p.getX() + lockBox.getScene().getX() + lockBox.getScene().getWindow().getX() + 90,
                            (p.getY() + lockBox.getScene().getY() + lockBox.getScene().getWindow().getY())
                                    + lockBox.getLayoutBounds().getHeight() - 1);
                }
            }
        });

        showBalance.addListener((obs, oldval, newval) -> {
            toggleShowBalance.setText(newval ? "⏷ " : "⏵ ");
        });

        Tooltip addAdrBtnTooltip = new Tooltip("Add");
        addAdrBtnTooltip.setShowDelay(Duration.millis(100));

        BufferedButton addAdrBtn = new BufferedButton("/assets/add-30.png", App.MENU_BAR_IMAGE_WIDTH);
        addAdrBtn.setTooltip(addAdrBtnTooltip);

        HBox adrBtnsBox = new HBox(addAdrBtn);
        adrBtnsBox.setAlignment(Pos.CENTER_LEFT);

        HBox adrBtnBoxes = new HBox();
        adrBtnBoxes.setAlignment(Pos.CENTER_LEFT);

        configBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            NoteInterface noteInterface = m_ergoNetworkData.selectedWallet().get();

            if (m_configBox.get() == null && noteInterface != null) {

                JsonObject note = Utils.getCmdObject("getConfigId");
                note.addProperty("networkId", m_ergoNetworkData.getId());

                noteInterface.sendNote(note, (onSuccess) -> {
                    Object obj = onSuccess.getSource().getValue();
                    if (obj != null && obj instanceof JsonObject) {
                        JsonObject json = (JsonObject) obj;
                        m_configBox.set(new ConfigBox(json.get("configId").getAsString()));
                    }
                }, (onFailed) -> {
                }, null);

            } else {
                m_configBox.set(null);
            }
        });

        HBox addressBtnsBox = new HBox(toggleShowBalance, lockBox, adrBtnBoxes);
        addressBtnsBox.setPadding(new Insets(2, 0, 2, 0));
        addressBtnsBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(addressBtnsBox, Priority.ALWAYS);

        toggleShowBalance.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (lockBox.getUnLocked()) {
                showBalance.set(!showBalance.get());
            } else {
                lockBox.requestFocus();
            }
        });

        VBox walletBalanceBox = new VBox();
        walletBalanceBox.setPadding(new Insets(2, 0, 2, 5));
        walletBalanceBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(walletBalanceBox, Priority.ALWAYS);

        HBox actionBox = new HBox();
        HBox.setHgrow(actionBox, Priority.ALWAYS);

        VBox adrBox = new VBox(actionBox, addressBtnsBox, walletBalanceBox);
        adrBox.setPadding(new Insets(0, 2, 5, 5));
        HBox.setHgrow(adrBox, Priority.ALWAYS);
        adrBox.setAlignment(Pos.CENTER_LEFT);

        adrBox.setPadding(new Insets(2));
        ///
        m_configBox.addListener((obs, oldval, newval) -> {
            actionBox.getChildren().clear();
            if (oldval != null) {
                oldval.shutdown();
            }

            if (newval != null) {
                actionBox.getChildren().add((Pane) newval);
            }

        });

        VBox selectedAddressBox = new VBox(adrBox);
        HBox.setHgrow(selectedAddressBox, Priority.ALWAYS);
        selectedAddressBox.setAlignment(Pos.TOP_LEFT);

        ErgoWalletAmountBoxes amountBoxes = new ErgoWalletAmountBoxes(m_ergoNetworkData.getErgoTokens(), true);

        VBox balancePaddingBox = new VBox(amountBoxes);
        balancePaddingBox.setPadding(new Insets(0, 0, 0, 0));
        HBox.setHgrow(balancePaddingBox, Priority.ALWAYS);

        ChangeListener<String> lockListener = (obs, oldval, newval) -> {
            NoteInterface noteInterface = m_ergoNetworkData.selectedWallet().get();

            if (oldval != null) {

                if (m_walletMsgInterface != null && m_walletMsgInterface.getId().equals(oldval)) {
                    noteInterface.removeMsgListener(m_walletMsgInterface);
                    m_walletMsgInterface = null;
                }

                showBalance.set(false);
            }

            if (newval != null && noteInterface != null) {
                JsonObject json = Utils.getCmdObject("getAddresses");
                json.addProperty("accessId", newval);
                json.addProperty("networkId", m_ergoNetworkData.getId());

                Object successObject = noteInterface.sendNote(json);

                if (successObject != null && successObject instanceof JsonArray) {

                    JsonArray jsonArray = (JsonArray) successObject;
                    if (jsonArray.size() > 0) {
                        JsonObject adr0 = jsonArray.get(0).getAsJsonObject();
                        JsonElement addressElement = adr0.get("address");
                        String address = addressElement.getAsString();

                        lockBox.setText(address);
                        showBalance.set(true);
                    } else {
                        lockBox.setLocked();
                    }
                }
                // && nameElement!= null && nameElement.isJsonPrimitive() && addressElement !=
                // null && addressElement.isJsonPrimitive()
                // m_name = nameElement.getAsString();
                // m_address = addressElement.getAsString();
                // m_index = indexElement.getAsInt();
                if (!adrBtnBoxes.getChildren().contains(adrBtnsBox)) {
                    adrBtnBoxes.getChildren().add(adrBtnsBox);
                }
            } else {
                if (adrBtnBoxes.getChildren().contains(adrBtnsBox)) {
                    adrBtnBoxes.getChildren().remove(adrBtnsBox);
                }

            }
        };

        lockBox.lockId().addListener(lockListener);

        Runnable setWallet = () -> {
            NoteInterface noteInterface = m_ergoNetworkData.selectedWallet().get();

            if (noteInterface == null) {
                walletField.setText(walletBtnDefaultString);

                if (walletBodyBox.getChildren().contains(selectedAddressBox)) {
                    walletBodyBox.getChildren().remove(selectedAddressBox);
                }

                if (walletFieldBox.getChildren().contains(disableWalletBtn)) {
                    walletFieldBox.getChildren().remove(disableWalletBtn);
                }
                if (walletFieldBox.getChildren().contains(configBtn)) {
                    walletFieldBox.getChildren().remove(configBtn);
                }

            } else {
                walletField.setText(noteInterface.getName());

                if (!walletBodyBox.getChildren().contains(selectedAddressBox)) {
                    walletBodyBox.getChildren().add(selectedAddressBox);
                }
                if (!walletFieldBox.getChildren().contains(configBtn)) {
                    walletFieldBox.getChildren().add(configBtn);
                }

                if (!walletFieldBox.getChildren().contains(disableWalletBtn)) {
                    walletFieldBox.getChildren().add(disableWalletBtn);
                }
                showWallet.set(true);
            }

        };

        m_ergoNetworkData.selectedWallet().addListener((obs, oldVal, newVal) -> {
            lockBox.setLocked();
            m_configBox.set(null);
            setWallet.run();
        });

        showBalance.addListener((obs, oldval, newval) -> {
            if (newval) {
                if (!walletBalanceBox.getChildren().contains(balancePaddingBox)) {
                    walletBalanceBox.getChildren().add(balancePaddingBox);
                }

            } else {
                if (walletBalanceBox.getChildren().contains(balancePaddingBox)) {
                    walletBalanceBox.getChildren().remove(balancePaddingBox);
                }
            }
        });

        VBox.setVgrow(this, Priority.ALWAYS);

        getChildren().add(walletBoxScroll);

        walletBoxScroll.prefViewportHeightProperty().bind(heightProperty());
        walletBoxScroll.prefViewportWidthProperty().bind(widthProperty());

        Runnable updateAmounts = () -> {
            String lockText = lockBox.getText();
            String address = lockText.equals(lockString) ? null : lockText;
            String lockId = lockBox.getLockId();

            NoteInterface selectedWallet = m_ergoNetworkData.selectedWallet().get();

            long timeStamp = System.currentTimeMillis();

            if (address != null && selectedWallet != null && lockId != null) {

                JsonObject note = Utils.getCmdObject("getBalance");
                note.addProperty("address", address);
                note.addProperty("accessId", lockBox.getLockId());
                note.addProperty("networkId", m_ergoNetworkData.getId());

                Object obj = selectedWallet.sendNote(note);

                if (obj != null && obj instanceof JsonObject) {
                    JsonObject json = (JsonObject) obj;
                    if (json != null) {
                        amountBoxes.update(json, timeStamp);
                    }
                }
            } else {

                amountBoxes.clear();
            }

        };

        lockBox.setOnAction(e -> {

            NoteInterface noteInterface = m_ergoNetworkData.selectedWallet().get();

            JsonObject getWalletObject = Utils.getCmdObject("getAccessId");
            getWalletObject.addProperty("networkId", m_ergoNetworkData.getId());
            getWalletObject.addProperty("password", lockBox.getPassString());

            Object successObject = noteInterface.sendNote(getWalletObject);

            if (successObject != null) {
                JsonObject json = (JsonObject) successObject;
                // addressesDataObject.set(json);

                String accessId = json.get("accessId").getAsString();

                m_walletMsgInterface = new NoteMsgInterface() {
                    private final String m_accessId = accessId;

                    public String getId() {
                        return m_accessId;
                    }

                    public void sendMessage(String networkId, int code, long timestamp) {

                    }

                    public void sendMessage(int code, long timestamp) {

                    }

                    public void sendMessage(int code, long timestamp, String networkId) {

                        switch (code) {
                            case App.UPDATED:
                                if (networkId != null && networkId.equals(lockBox.getText())) {

                                    updateAmounts.run();

                                }
                                break;
                        }

                    }

                    public void sendMessage(String networkId, int code, long timestamp, String msg) {
                        // updated String address = networkId;
                    }

                    public void sendMessage(String networkId, int code, long timestamp, JsonObject json) {
                    }
                };

                noteInterface.addMsgListener(m_walletMsgInterface);

                lockBox.setUnlocked(accessId);

            } else {
                lockBox.resetPass();
            }

        });

        m_ergoWalletsMsgInterface = new NoteMsgInterface() {

            private String m_interfaceId = FriendlyId.createFriendlyId();

            public String getId() {
                return m_interfaceId;
            }

            public void sendMessage(String networkId, int code, long timestamp) {

            }

            public void sendMessage(int code, long timestamp) {

            }

            public void sendMessage(int code, long timestamp, String networkId) {
                if (networkId != null) {
                    NoteInterface noteInterface = m_ergoNetworkData.selectedWallet().get();

                    if (noteInterface != null && networkId.equals(noteInterface.getNetworkId())) {
                        walletField.setText(noteInterface.getName());
                        if (m_configBox.get() != null) {
                            m_configBox.get().sendMessage(ErgoWallets.NETWORK_ID, code, timestamp, networkId);
                        }
                    }

                }

            }

            public void sendMessage(String networkId, int code, long timestamp, String msg) {

            }

            public void sendMessage(String networkId, int code, long timestamp, JsonObject json) {

            }
        };

        m_ergoNetworkData.getErgoWallets().addMsgListener(m_ergoWalletsMsgInterface);
        lockBox.textProperty().addListener((obs, oldval, newval) -> updateAmounts.run());

        m_ergoTokensMsgInterface = new NoteMsgInterface() {

            private String m_interfaceId = FriendlyId.createFriendlyId();

            public String getId() {
                return m_interfaceId;
            }

            public void sendMessage(String networkId, int code, long timestamp) {

            }

            public void sendMessage(int code, long timestamp) {
                amountBoxes.updateTokens();
            }

            public void sendMessage(int code, long timestamp, String networkId) {
               
                amountBoxes.updateToken(networkId);

            }

            public void sendMessage(String networkId, int code, long timestamp, String msg) {

            }

            public void sendMessage(String networkId, int code, long timestamp, JsonObject json) {

            }
        };

        m_ergoNetworkData.getErgoTokens().addMsgListener(m_ergoTokensMsgInterface);

        setWallet.run();
    }

    public String getTabId() {
        return m_tabId;
    }

    public String getName() {
        return "Ergo Wallets";
    }

    public void shutdown() {
        if (m_ergoWalletsMsgInterface != null) {
            m_ergoNetworkData.getErgoWallets().removeMsgListener(m_ergoWalletsMsgInterface);
            m_ergoWalletsMsgInterface = null;
        }
        if (m_ergoNetworkData != null && m_walletMsgInterface != null) {

            if (m_ergoNetworkData.getErgoWallets() != null) {
                m_ergoNetworkData.getErgoWallets().removeMsgListener(m_walletMsgInterface);
            }
            m_walletMsgInterface = null;
        }
    }

    public void setCurrent(boolean value) {
        m_current = value;
    }

    public boolean getCurrent() {
        return m_current;
    }

    public String getType() {
        return "RowArea";
    }

    public boolean isStatic() {
        return false;
    }

    public SimpleStringProperty titleProperty() {
        return m_titleProperty;
    }

    private class ConfigBox extends VBox implements SimpleNoteInterface {

        private SimpleStringProperty m_name = new SimpleStringProperty();
        private SimpleStringProperty m_fileName = new SimpleStringProperty();
        private String m_configId;

        public void shutdown() {

        }

        public boolean sendNote(JsonObject note, EventHandler<WorkerStateEvent> onSucceeded,
                EventHandler<WorkerStateEvent> onFailed) {
            return false;
        }

        public Object sendNote(JsonObject note) {
            NoteInterface noteInterface = m_ergoNetworkData.selectedWallet().get();
            JsonElement subjectElement = note.get("subject");

            if (subjectElement != null && subjectElement.isJsonPrimitive() && noteInterface != null) {
                switch (subjectElement.getAsString()) {
                    case App.GET_DATA:

                        break;
                }
            } else {
                m_configBox.set(null);
            }
            return null;
        }

        public void sendMessage(String parentId, int code, long timeStamp, String id) {
            switch (parentId) {
                case ErgoWallets.NETWORK_ID:
                    updateBalance();
                    break;
                case ErgoTokens.NETWORK_ID:
                    updateTokens();
                    break;
            }
        }

        private void updateBalance() {
            NoteInterface noteInterface = m_ergoNetworkData.selectedWallet().get();

            JsonObject note = Utils.getCmdObject("getFileData");
            note.addProperty("networkId", m_ergoNetworkData.getId());
            note.addProperty("configId", m_configId);

            Object obj = noteInterface.sendNote(note);
            if (obj != null && obj instanceof JsonObject) {
                JsonObject json = (JsonObject) obj;
                m_name.set(noteInterface.getName());
                m_fileName.set(json.get("name").getAsString());
            }

        }

        private void updateTokens() {

        }

        public ConfigBox(String configId) {
            m_configId = configId;
            Label toggleShowSettings = new Label("⏷ ");
            toggleShowSettings.setId("caretBtn");

            Label settingsLbl = new Label("⚙");
            settingsLbl.setId("logoBox");

            Text settingsText = new Text("Config");
            settingsText.setFont(App.txtFont);
            settingsText.setFill(App.txtColor);

            Tooltip walletInfoTooltip = new Tooltip("Wallet in use");

            HBox settingsBtnsBox = new HBox(toggleShowSettings, settingsLbl, settingsText);
            HBox.setHgrow(settingsBtnsBox, Priority.ALWAYS);
            settingsBtnsBox.setAlignment(Pos.CENTER_LEFT);

            Label walletNameText = new Label("Name ");
            // walletNameText.setFill(App.txtColor);
            walletNameText.setFont(App.txtFont);
            walletNameText.setMinWidth(70);

            TextField walletNameField = new TextField();
            HBox.setHgrow(walletNameField, Priority.ALWAYS);

            walletNameField.setEditable(false);

            Label editNameLabel = new Label("✎");
            editNameLabel.setId("lblBtn");

            Button walletNameEnterBtn = new Button("[enter]");
            walletNameEnterBtn.setMinHeight(15);
            walletNameEnterBtn.setText("[enter]");
            walletNameEnterBtn.setId("toolBtn");
            walletNameEnterBtn.setPadding(new Insets(0, 5, 0, 5));
            walletNameEnterBtn.setFocusTraversable(false);

            walletNameField.setOnAction(e -> walletNameEnterBtn.fire());

            Button walletFileEnterBtn = new Button("[enter]");
            walletFileEnterBtn.setText("[enter]");
            walletFileEnterBtn.setId("toolBtn");
            walletFileEnterBtn.setPadding(new Insets(0, 5, 0, 5));
            walletFileEnterBtn.setFocusTraversable(false);

            HBox walletNameFieldBox = new HBox(walletNameField, editNameLabel);
            HBox.setHgrow(walletNameFieldBox, Priority.ALWAYS);
            walletNameFieldBox.setId("bodyBox");
            walletNameFieldBox.setPadding(new Insets(0, 5, 0, 0));
            walletNameFieldBox.setMaxHeight(18);
            walletNameFieldBox.setAlignment(Pos.CENTER_LEFT);

            Label walletNameLbl = new Label("⊢");
            walletNameLbl.setId("logoBtn");

            HBox walletNameBox = new HBox(walletNameLbl, walletNameText, walletNameFieldBox);
            walletNameBox.setAlignment(Pos.CENTER_LEFT);
            walletNameBox.setPadding(new Insets(2, 0, 2, 0));

            Label walletFileText = new Label("File");
            walletFileText.setFont(App.txtFont);
            walletFileText.setMinWidth(70);

            TextField walletFileField = new TextField();
            walletFileField.setEditable(false);
            HBox.setHgrow(walletFileField, Priority.ALWAYS);
            walletFileField.setOnAction(e -> walletFileEnterBtn.fire());

            Label walletFileOpenLbl = new Label("…");
            walletFileOpenLbl.setId("lblBtn");

            Label walletFileEditLbl = new Label("✎");
            walletFileEditLbl.setId("lblBtn");

            HBox walletFileFieldBox = new HBox(walletFileField, walletFileOpenLbl, walletFileEditLbl);
            HBox.setHgrow(walletFileFieldBox, Priority.ALWAYS);
            walletFileFieldBox.setId("bodyBox");
            walletFileFieldBox.setAlignment(Pos.CENTER_LEFT);
            walletFileFieldBox.setMaxHeight(18);
            walletFileFieldBox.setPadding(new Insets(0, 5, 0, 0));

            walletFileOpenLbl.setOnMouseClicked(e -> {
                NoteInterface noteInterface = m_ergoNetworkData.selectedWallet().get();

                if (noteInterface != null) {
                    JsonObject note = Utils.getCmdObject("getFileData");
                    note.addProperty("configId", m_configId);
                    note.addProperty("networkId", m_ergoNetworkData.getId());

                    Object obj = noteInterface.sendNote(note);

                    JsonObject json = obj != null && obj instanceof JsonObject ? (JsonObject) obj : null;
                    boolean isFile = json != null ? json.get("isFile").getAsBoolean() : false;
                    String path = isFile ? json.get("path").getAsString() : null;

                    File currentFile = path != null ? new File(path) : null;
                    File currentDir = currentFile != null ? currentFile.getParentFile() : AppData.HOME_DIRECTORY;
                    // String fileName = currentFile != null ? currentFile.getName() :
                    // noteInteface.getNetworkId() + ".erg";

                    FileChooser openFileChooser = new FileChooser();
                    openFileChooser.setTitle("Select wallet (*.erg)");
                    openFileChooser.setInitialDirectory(currentDir);
                    openFileChooser.getExtensionFilters().add(ErgoWallets.ergExt);
                    openFileChooser.setSelectedExtensionFilter(ErgoWallets.ergExt);

                    File walletFile = openFileChooser.showOpenDialog(m_appStage);

                    if (walletFile != null) {
                        JsonObject updateFileObject = Utils.getCmdObject("updateFile");
                        updateFileObject.addProperty("file", walletFile.getAbsolutePath());
                        updateFileObject.addProperty("networkId", m_ergoNetworkData.getId());
                        updateFileObject.addProperty("configId", m_configId);
                        noteInterface.sendNote(updateFileObject);
                    }
                }
            });

            Label walletFileLbl = new Label("⊢");
            walletFileLbl.setId("logoBtn");

            toggleShowSettings.setOnMouseClicked(e -> {
                m_configBox.set(null);
            });

            HBox walletFileBox = new HBox(walletFileLbl, walletFileText, walletFileFieldBox);
            walletFileBox.setAlignment(Pos.CENTER_LEFT);
            walletFileBox.setPadding(new Insets(2, 0, 2, 0));

            VBox settingsBodyBox = new VBox(walletNameBox, walletFileBox);
            settingsBodyBox.setPadding(new Insets(0, 5, 0, 30));
            HBox.setHgrow(settingsBodyBox, Priority.ALWAYS);

            getChildren().addAll(settingsBtnsBox, settingsBodyBox);
            HBox.setHgrow(this, Priority.ALWAYS);

            Runnable setWalletConfigInfo = () -> {
                NoteInterface noteInterface = m_ergoNetworkData.selectedWallet().get();

                if (noteInterface == null) {
                    m_configBox.set(null);
                    return;
                }
                JsonObject note = Utils.getCmdObject("getFileData");
                note.addProperty("networkId", m_ergoNetworkData.getId());
                note.addProperty("configId", m_configId);

                Object obj = noteInterface.sendNote(note);
                if (obj != null && obj instanceof JsonObject) {
                    JsonObject json = (JsonObject) obj;

                    String filePath = json.get("path").getAsString();

                    if (json.get("isFile").getAsBoolean()) {
                        File walletFile = new File(filePath);
                        walletFileField.setText(walletFile.getName());
                    } else {
                        walletFileField.setText("(File not found) " + filePath);
                    }

                } else {
                    walletFileField.setText("(Unable to retreive wallet info) ");
                }
                walletNameField.setText(noteInterface.getName());
            };

            Runnable setWalletSettingsNonEditable = () -> {

                if (walletNameField.isEditable()) {
                    walletNameField.setEditable(false);
                    if (walletNameFieldBox.getChildren().contains(walletNameEnterBtn)) {
                        walletNameFieldBox.getChildren().remove(walletNameEnterBtn);
                    }
                }
                if (walletFileField.isEditable()) {
                    walletFileField.setEditable(false);
                    if (walletFileFieldBox.getChildren().contains(walletFileEnterBtn)) {
                        walletFileFieldBox.getChildren().remove(walletFileEnterBtn);
                    }
                }

                setWalletConfigInfo.run();
            };

            editNameLabel.setOnMouseClicked(e -> {
                NoteInterface noteInterface = m_ergoNetworkData.selectedWallet().get();
                if (noteInterface == null) {
                    m_configBox.set(null);
                    return;
                }

                boolean isOpen = noteInterface != null && noteInterface.getConnectionStatus() != 0;

                if (isOpen) {
                    walletInfoTooltip.setText("Wallet in use");
                    walletInfoTooltip.show(editNameLabel, e.getScreenX(), e.getScreenY());
                    PauseTransition pt = new PauseTransition(Duration.millis(1600));
                    pt.setOnFinished(ptE -> {
                        walletInfoTooltip.hide();
                    });
                    pt.play();
                    setWalletSettingsNonEditable.run();
                } else {
                    if (walletNameField.isEditable()) {
                        setWalletSettingsNonEditable.run();
                    } else {
                        if (!walletNameFieldBox.getChildren().contains(walletNameEnterBtn)) {
                            walletNameFieldBox.getChildren().add(1, walletNameEnterBtn);
                        }
                        walletNameField.setEditable(true);
                        walletNameField.requestFocus();

                    }
                }
            });

            walletNameField.focusedProperty().addListener((obs, oldval, newval) -> {
                if (!newval) {
                    if (walletNameField.isEditable()) {
                        setWalletSettingsNonEditable.run();
                    }
                }
            });

            walletNameEnterBtn.setOnAction(e -> {
                NoteInterface noteInterface = m_ergoNetworkData.selectedWallet().get();
                if (noteInterface == null) {
                    setWalletSettingsNonEditable.run();
                    m_configBox.set(null);
                    return;
                }

                String name = walletNameField.getText();

                JsonObject json = Utils.getCmdObject("updateName");
                json.addProperty("name", name);
                json.addProperty("networkId", m_ergoNetworkData.getId());
                json.addProperty("configId", m_configId);
                JsonObject updatedObj = (JsonObject) noteInterface.sendNote(json);
                if (updatedObj != null) {
                    JsonElement codeElement = updatedObj.get("code");
                    JsonElement msgElement = updatedObj.get("msg");

                    int code = codeElement != null && codeElement.isJsonPrimitive() ? codeElement.getAsInt() : -1;
                    String msg = msgElement != null && msgElement.isJsonPrimitive() ? msgElement.getAsString() : null;
                    if (code != App.WARNING) {
                        Point2D p = walletNameField.localToScene(0.0, 0.0);
                        walletInfoTooltip.setText(msg != null ? msg : "Error");
                        walletInfoTooltip.show(walletFileEditLbl,
                                p.getX() + walletFileField.getScene().getX()
                                        + walletFileField.getScene().getWindow().getX()
                                        + walletFileField.getLayoutBounds().getWidth(),
                                (p.getY() + walletFileField.getScene().getY()
                                        + walletFileField.getScene().getWindow().getY()) - 30);
                        PauseTransition pt = new PauseTransition(Duration.millis(1600));
                        pt.setOnFinished(ptE -> {
                            walletInfoTooltip.hide();
                        });
                        pt.play();
                    }
                    setWalletSettingsNonEditable.run();
                }
            });

            walletFileEditLbl.setOnMouseClicked(e -> {
                NoteInterface noteInterface = m_ergoNetworkData.selectedWallet().get();
                if (noteInterface == null) {
                    m_configBox.set(null);
                    return;
                }

                boolean isOpen = noteInterface != null && noteInterface.getConnectionStatus() != 0;

                if (isOpen) {
                    walletInfoTooltip.setText("Wallet in use");
                    walletInfoTooltip.show(walletFileEditLbl, e.getScreenX(), e.getScreenY());
                    PauseTransition pt = new PauseTransition(Duration.millis(1600));
                    pt.setOnFinished(ptE -> {
                        walletInfoTooltip.hide();
                    });
                    pt.play();
                    setWalletSettingsNonEditable.run();
                } else {
                    if (walletFileField.isEditable()) {
                        setWalletSettingsNonEditable.run();
                    } else {
                        if (!walletFileFieldBox.getChildren().contains(walletFileEnterBtn)) {
                            walletFileFieldBox.getChildren().add(1, walletFileEnterBtn);
                        }
                        walletFileField.setEditable(true);
                        JsonObject note = Utils.getCmdObject("getFileData");
                        note.addProperty("configId", m_configId);
                        note.addProperty("networkId", m_ergoNetworkData.getId());

                        Object obj = noteInterface.sendNote(note);
                        if (obj != null && obj instanceof JsonObject) {
                            JsonObject json = (JsonObject) obj;
                            walletFileField.setText(json.get("path").getAsString());
                        }
                        walletFileField.requestFocus();

                    }
                }
            });

            walletFileField.focusedProperty().addListener((obs, oldval, newval) -> {
                if (walletFileField.isEditable() && !newval) {
                    setWalletSettingsNonEditable.run();
                }
            });

            walletFileEnterBtn.setOnAction(e -> {
                NoteInterface noteInterface = m_ergoNetworkData.selectedWallet().get();
                if (noteInterface == null) {
                    setWalletSettingsNonEditable.run();
                    m_configBox.set(null);
                    return;
                }

                String fileString = walletFileField.getText();

                if (fileString.length() > 0 && Utils.findPathPrefixInRoots(fileString)) {

                    JsonObject note = Utils.getCmdObject("updateFile");
                    note.addProperty("file", fileString);
                    note.addProperty("networkId", m_ergoNetworkData.getId());
                    note.addProperty("configId", m_configId);
                    Object obj = noteInterface.sendNote(note);
                    if (obj != null && obj instanceof JsonObject) {
                        JsonObject resultObject = (JsonObject) obj;
                        JsonElement codeElement = resultObject.get("code");
                        JsonElement msgElement = resultObject.get("msg");
                        if (codeElement != null && msgElement != null) {
                            int code = codeElement.getAsInt();
                            String msg = codeElement.getAsString();
                            if (code != App.WARNING) {
                                Point2D p = walletNameField.localToScene(0.0, 0.0);
                                walletInfoTooltip.setText(msg != null ? msg : "Error");
                                walletInfoTooltip.show(walletFileEditLbl,
                                        p.getX() + walletFileField.getScene().getX()
                                                + walletFileField.getScene().getWindow().getX()
                                                + walletFileField.getLayoutBounds().getWidth(),
                                        (p.getY() + walletFileField.getScene().getY()
                                                + walletFileField.getScene().getWindow().getY()) - 30);
                                PauseTransition pt = new PauseTransition(Duration.millis(1600));
                                pt.setOnFinished(ptE -> {
                                    walletInfoTooltip.hide();
                                });
                                pt.play();
                            }
                            setWalletSettingsNonEditable.run();
                        } else {
                            setWalletSettingsNonEditable.run();
                            m_configBox.set(null);
                        }

                    } else {

                    }

                } else {
                    Point2D p = walletFileField.localToScene(0.0, 0.0);

                    walletInfoTooltip.setText("File not found");

                    walletInfoTooltip.show(walletFileField,
                            p.getX() + walletFileField.getScene().getX() + walletFileField.getScene().getWindow().getX()
                                    + walletFileField.getLayoutBounds().getWidth(),
                            (p.getY() + walletFileField.getScene().getY()
                                    + walletFileField.getScene().getWindow().getY()) - 30);
                    PauseTransition pt = new PauseTransition(Duration.millis(1600));
                    pt.setOnFinished(ptE -> {
                        walletInfoTooltip.hide();
                    });
                    pt.play();
                }
            });

            m_name.addListener((obs, oldval, newval) -> {
                if (newval != null) {
                    walletNameField.setText(newval);
                }
            });

            m_fileName.addListener((obs, oldval, newval) -> {
                if (newval != null) {
                    walletFileField.setText(newval);
                }
            });

            updateBalance();

        }

    }

    private class NewWalletBox extends AppBox {


        public NewWalletBox(boolean isNew, EventHandler<WorkerStateEvent> onSucceeded) {
            super();


            Label backButton = new Label("🠜");
            backButton.setId("lblBtn");

            backButton.setOnMouseClicked(e -> {
                m_currentBox.set(null);
            });

            Label headingText = new Label((isNew ? "New" : "Restore") + " wallet");
            headingText.setFont(App.txtFont);
            headingText.setPadding(new Insets(0,0,0,15));

            HBox headingBox = new HBox(backButton, headingText);
            headingBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox, Priority.ALWAYS);
            headingBox.setPadding(new Insets(10, 15, 0, 15));
     
            VBox headerBox = new VBox(headingBox);
            headerBox.setPadding(new Insets(0, 5, 0, 0));

            TextArea mnemonicField = new TextArea(isNew ? Mnemonic.generateEnglishMnemonic() : "");
            mnemonicField.setFont(App.txtFont);
            mnemonicField.setId("textFieldCenter");
            mnemonicField.setEditable(!isNew);
            mnemonicField.setWrapText(true);
            mnemonicField.setPrefRowCount(4);
            mnemonicField.setPromptText("Enter Mnemonic");
            HBox.setHgrow(mnemonicField, Priority.ALWAYS);

            Platform.runLater(() -> mnemonicField.requestFocus());

            HBox mnemonicFieldBox = new HBox(mnemonicField);
            mnemonicFieldBox.setId("bodyBox");
            mnemonicFieldBox.setPadding(new Insets(15, 0,0,0));

            VBox mnemonicBox = new VBox(mnemonicFieldBox);
            mnemonicBox.setAlignment(Pos.CENTER);
            mnemonicBox.setPadding(new Insets(20, 30, 0, 30));

            Region hBar = new Region();
            hBar.setPrefWidth(400);
            hBar.setPrefHeight(2);
            hBar.setMinHeight(2);
            hBar.setId("hGradient");

            HBox gBox = new HBox(hBar);
            gBox.setAlignment(Pos.CENTER);
            gBox.setPadding(new Insets(0, 0, 0, 0));

            Button nextBtn = new Button("Next");


            HBox nextBox = new HBox(nextBtn);
            nextBox.setAlignment(Pos.CENTER);
            nextBox.setPadding(new Insets(20, 0, 20, 0));

            VBox bodyBox = new VBox(gBox, mnemonicBox, nextBox);
            VBox.setMargin(bodyBox, new Insets(10, 10, 0, 20));
            VBox.setVgrow(bodyBox, Priority.ALWAYS);

            VBox layoutVBox = new VBox(headerBox, bodyBox);

            mnemonicFieldBox.setMaxWidth(900);
            HBox.setHgrow(mnemonicFieldBox, Priority.ALWAYS);

            getChildren().add(layoutVBox);


            Label backButton2 = new Label("🠜");
            backButton2.setId("lblBtn");

            backButton2.setOnMouseClicked(e -> {
                getChildren().clear();
                getChildren().add(layoutVBox);
            });

            Label headingText2 = new Label("Create password");
            headingText2.setFont(App.txtFont);
            headingText2.setPadding(new Insets(0,0,0,15));

            HBox headingBox2 = new HBox(backButton2, headingText2);
            headingBox2.prefHeight(40);
            headingBox2.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(headingBox2, Priority.ALWAYS);
            headingBox2.setPadding(new Insets(10, 15, 0, 15));
           

            VBox headerBox2 = new VBox(headingBox2);
            headerBox2.setPadding(new Insets(0, 5, 2, 0));

            PasswordField passField = new PasswordField();
            passField.setPromptText("Enter Password");
            passField.setAlignment(Pos.CENTER);
            HBox.setHgrow(passField, Priority.ALWAYS);

            HBox passFieldBox = new HBox(passField);
            passFieldBox.setId("bodyBox");
            passFieldBox.setPrefHeight(20);
            passFieldBox.setMaxWidth(200);
            passFieldBox.setAlignment(Pos.CENTER);

            VBox passBox = new VBox(passFieldBox);
            passBox.setAlignment(Pos.CENTER);
            passBox.setPadding(new Insets(30, 30, 0, 30));

            PasswordField passField2 = new PasswordField();
            passField2.setPromptText("Re-Enter Password");
            HBox.setHgrow(passField2, Priority.ALWAYS);
            passField2.setAlignment(Pos.CENTER);

            HBox passFieldBox2 = new HBox(passField2);
            passFieldBox2.setPrefHeight(20);
            passFieldBox2.setMaxWidth(200);
            passFieldBox2.setId("bodyBox");
            passFieldBox2.setAlignment(Pos.CENTER);

            VBox passBox2 = new VBox(passFieldBox2);
            
            passBox2.setAlignment(Pos.CENTER);
            passBox2.setPadding(new Insets(20, 30, 20, 30));

            VBox passwordsBox = new VBox(passBox, passBox2);
            passwordsBox.setAlignment(Pos.CENTER);
            HBox.setHgrow(passwordsBox,Priority.ALWAYS);

            Region hBar2 = new Region();
            hBar2.setPrefWidth(400);
            hBar2.setPrefHeight(2);
            hBar2.setMinHeight(2);
            hBar2.setId("hGradient");

            HBox gBox2 = new HBox(hBar2);
            gBox2.setAlignment(Pos.CENTER);
            gBox2.setPadding(new Insets(0, 0, 0, 0));

            Tooltip passwordsTip = new Tooltip();

            Button createBtn = new Button("Create");

            passField2.setOnAction(e->createBtn.fire());

            HBox createBox = new HBox(createBtn);
            createBox.setAlignment(Pos.CENTER);
            createBox.setPadding(new Insets(10, 0, 20, 0));


            VBox bodyBox2 = new VBox(gBox2, passwordsBox, createBox);
            VBox.setMargin(bodyBox2, new Insets(10, 10, 0, 20));
            VBox.setVgrow(bodyBox2, Priority.ALWAYS);

            VBox layoutVBox2 = new VBox(headingBox2, bodyBox2);

 
            Runnable createWallet = () -> {

                String password = passField.getText();

                if (password.length() > 3 && password.equals(passField2.getText())) {

                    FileChooser saveFileChooser = new FileChooser();
                    saveFileChooser.setTitle("Save wallet (*.erg)");
                    saveFileChooser.setInitialDirectory(AppData.HOME_DIRECTORY);
                    saveFileChooser.getExtensionFilters().add(ErgoWallets.ergExt);
                    saveFileChooser.setSelectedExtensionFilter(ErgoWallets.ergExt);

                    File walletFile = saveFileChooser.showSaveDialog(m_appStage);

                    if (walletFile != null) {

                        boolean isDotErg = walletFile != null && walletFile.getAbsolutePath().endsWith(".erg");
                        walletFile = isDotErg ? walletFile : new File(walletFile.getAbsolutePath() + ".erg"); 

                        Wallet.create(walletFile.toPath(), Mnemonic.create(SecretString.create(mnemonicField.getText()),
                                SecretString.create(password)), walletFile.getName(), password.toCharArray());

                        mnemonicField.setText("-");
                        passField.setText("");
                        passField2.setText(" "  );

                        if (walletFile.isFile()) {
                            JsonObject note = Utils.getCmdObject("openWallet");
                            note.addProperty("networkId", m_ergoNetworkData.getId());
                            note.addProperty("path", walletFile.getAbsolutePath());

                            Object result = m_ergoNetworkData.getErgoWallets().sendNote(note);

                            if (result != null && result instanceof JsonObject) {
                                JsonObject walletJson = (JsonObject) result;
                                JsonElement idElement = walletJson.get("id");
                                if(idElement != null && idElement.isJsonPrimitive()){
                                    Utils.returnObject(walletJson, onSucceeded, null);
                                }

                            }
                        }

                    }

                }else{
                    Point2D p = createBtn.localToScene(0.0, 0.0);
                    passwordsTip.setText(password.length() < 6  ? "Password too weak" :  "Passwords do not match" );
                    passwordsTip.show(createBtn,
                            p.getX() + createBtn.getScene().getX()
                                    + createBtn.getScene().getWindow().getX() - 60,
                            (p.getY() + createBtn.getScene().getY()
                                    + createBtn.getScene().getWindow().getY()) - 40);
                    PauseTransition pt = new PauseTransition(Duration.millis(1600));
                    pt.setOnFinished(ptE -> {
                        passwordsTip.hide();
                    });
                    pt.play();
                }
            };

            nextBtn.setOnAction(e -> {
                String mnemonic =  mnemonicField.getText().trim();
                String[] words = mnemonic.split("\\s+");
                List<String> items = Arrays.asList(words);
                int size = mnemonic.equals("") ? 0 : items.size();
                final int mnemonicSize = 15;
                if(size == mnemonicSize){
                    try{
                        Mnemonic.checkEnglishMnemonic(items);
                        getChildren().clear();
                        getChildren().add(layoutVBox2);
                    }catch(MnemonicValidationException validationEx){
                        Point2D p = nextBtn.localToScene(0.0, 0.0);
                        passwordsTip.setText(validationEx.toString());
                        passwordsTip.show(nextBtn,
                                p.getX() + nextBtn.getScene().getX()
                                        + nextBtn.getScene().getWindow().getX() - 60,
                                (p.getY() + nextBtn.getScene().getY()
                                        + nextBtn.getScene().getWindow().getY()) - 40);
                        PauseTransition pt = new PauseTransition(Duration.millis(1600));
                        pt.setOnFinished(ptE -> {
                            passwordsTip.hide();
                        });
                        pt.play();
                    }
                }else{
                    Point2D p = nextBtn.localToScene(0.0, 0.0);
                    passwordsTip.setText(items.size() + " of " + mnemonicSize + " words");
                    passwordsTip.show(nextBtn,
                            p.getX() + nextBtn.getScene().getX()
                                    + nextBtn.getScene().getWindow().getX() - 60,
                            (p.getY() + nextBtn.getScene().getY()
                                    + nextBtn.getScene().getWindow().getY()) - 40);
                    PauseTransition pt = new PauseTransition(Duration.millis(1600));
                    pt.setOnFinished(ptE -> {
                        passwordsTip.hide();
                    });
                    pt.play();
                }
                
            });


            createBtn.setOnAction(e -> createWallet.run());

        }
    }


}

/*
 * //New
 * // Scene mnemonicScene = createMnemonicScene(friendlyId,
 * walletNameField.getText(), nodeId, explorerId,
 * marketsId,tokenMarketIdProperty.get(), tokensEnabled, networkType, stage);
 * 
 * //Restore
 * 
 * restoreWalletBtn.setOnAction(clickEvent -> {
 * String seedPhrase = restoreMnemonicStage();
 * if (!seedPhrase.equals("")) {
 * Button passBtn = new Button();
 * Stage passwordStage = App.createPassword(m_ergoWallet.getName() +
 * " - Restore wallet: Password", m_ergoWallet.getIcon(),
 * m_ergoWallet.getAppIcon(), passBtn,
 * m_ergoWallet.getNetworksData().getExecService(), onSuccess -> {
 * Object sourceObject = onSuccess.getSource().getValue();
 * 
 * if (sourceObject != null && sourceObject instanceof String) {
 * 
 * String passwordString = (String) sourceObject;
 * if (!passwordString.equals("")) {
 * Mnemonic mnemonic = Mnemonic.create(SecretString.create(seedPhrase),
 * SecretString.create(passwordString));
 * 
 * FileChooser saveFileChooser = new FileChooser();
 * // saveFileChooser.setInitialDirectory(getWalletsDirectory());
 * saveFileChooser.setTitle("Save: Wallet file");
 * saveFileChooser.getExtensionFilters().add(ErgoWallets.ergExt);
 * saveFileChooser.setSelectedExtensionFilter(ErgoWallets.ergExt);
 * 
 * File walletFile = saveFileChooser.showSaveDialog(appStage);
 * 
 * if (walletFile != null) {
 * 
 * try {
 * 
 * 
 * Wallet.create(walletFile.toPath(), mnemonic, seedPhrase,
 * passwordString.toCharArray());
 * 
 * NetworkType networkType = selectedNetworkType.get();
 * String nodeId = selectedNodeData.get() == null ? null :
 * selectedNodeData.get().getId();
 * String explorerId = selectedExplorerData.get() == null ? null :
 * selectedExplorerData.get().getId();
 * String marketsId = marketIdProperty.get();
 * boolean tokensEnabled =ergoTokensEnabledProperty.get();
 * 
 * ErgoWalletData walletData = new ErgoWalletData(friendlyId,
 * walletNameField.getText(), walletFile, networkType, m_ergoWallet);
 * add(walletData);
 * 
 * 
 * 
 * } catch (Exception e1) {
 * Alert a = new Alert(AlertType.NONE, "Wallet creation: Cannot be saved.\n\n" +
 * e1.toString(), ButtonType.OK);
 * a.initOwner(appStage);
 * a.show();
 * }
 * }
 * 
 * }
 * 
 * passBtn.fire();
 * }
 * });
 * passwordStage.show();
 * }
 * 
 * });
 */