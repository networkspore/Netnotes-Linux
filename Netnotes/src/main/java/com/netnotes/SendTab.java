package com.netnotes;


import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;


public class SendTab extends VBox implements TabInterface {
    
    public final static String NAME = "Send";

    private ListChangeListener<ErgoTransaction> m_watchedTxListChanged = null;


    private SimpleBooleanProperty m_current = new SimpleBooleanProperty(true);
    public boolean getCurrent(){
        return m_current.get();
    } 

    public void setCurrent(boolean value){
        m_current.set(value);
    }

    public String getType(){
        return App.STATIC_TYPE;
    }

    public boolean isStatic(){
        return getType().equals(App.STATIC_TYPE);
    }

    private final AddressData m_addressData;
    private VBox m_summaryBox;
    public SendTab(Scene scene, AddressData addressData, SimpleDoubleProperty widthObject, VBox summaryBox){
        super();
        m_addressData = addressData;
        summaryBox.getChildren().clear();

        m_summaryBox = summaryBox;

        int imageWidth = 15;

        prefWidthProperty().bind(widthObject);
     //   getChildren().addAll();
        setPadding(new Insets(10));

        /////
    
        Text toText = new Text("To     ");
        toText.setFont(App.txtFont);
        toText.setFill(App.txtColor);

        AddressBox toAddressEnterBox = new AddressBox(new AddressInformation(""), scene, addressData.getNetworkType());

        toAddressEnterBox.setMinHeight(32);

        HBox toAddressBox = new HBox(toText, toAddressEnterBox);
        toAddressBox.setPadding(new Insets(0, 15, 10, 30));
        toAddressBox.setAlignment(Pos.CENTER_LEFT);
        toAddressBox.setId("darkBox");

        getChildren().addAll(toAddressBox);
    }

    public String getTabId(){
        return NAME + ":" + m_addressData.getAddress().toString();
    }

    public String getName(){
        return NAME;
    }

    public static String createTabId(AddressData addressData){
        return NAME + ":" + addressData.getAddress().toString();
    }


    public void shutdown(){
        m_summaryBox.getChildren().clear();
        prefWidthProperty().unbind();
        if(m_watchedTxListChanged != null){
            m_addressData.watchedTxList().removeListener(m_watchedTxListChanged);
            m_watchedTxListChanged = null;
        }
    }

    private SimpleStringProperty m_titleProperty = new SimpleStringProperty(NAME);

    public SimpleStringProperty titleProperty(){
        return m_titleProperty;
    }
}
