package com.netnotes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.netnotes.IconButton.IconStyle;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AppsTab extends AppBox implements TabInterface  {
    public static final int PADDING = 10;
    public static final String NAME = "Apps";

    private final Button m_menuBtn;
    private final NetworksData m_networksData;

    private SimpleBooleanProperty m_current = new SimpleBooleanProperty(true);
    private SimpleStringProperty m_iconStyle;
    private HBox m_headerBox;
    private VBox m_listBox;

    public AppsTab(Stage appStage, NetworksData networksData, SimpleDoubleProperty widthObject, Button menuBtn){
        super(NAME);
        m_menuBtn = menuBtn;
        m_networksData = networksData;
        m_iconStyle = new SimpleStringProperty(IconStyle.ICON);
        
        minWidthProperty().bind(widthObject);
        maxWidthProperty().bind(widthObject);
        setAlignment(Pos.TOP_CENTER);

        BufferedButton itemStyleBtn = new BufferedButton("/assets/list-outline-white-25.png", App.MENU_BAR_IMAGE_WIDTH);
        itemStyleBtn.setOnAction(e->{
            if(m_iconStyle.get().equals(IconStyle.ICON)){
                m_iconStyle.set(IconStyle.ROW);
            }else{
                m_iconStyle.set(IconStyle.ICON);
            }
        });

        m_iconStyle.addListener((obs,oldval,newval)->update());
        
        m_headerBox = new HBox(itemStyleBtn);
        HBox.setHgrow(m_headerBox, Priority.ALWAYS);
        m_headerBox.setAlignment(Pos.CENTER_RIGHT);
        m_headerBox.setPadding(new Insets(2));

        m_listBox = new VBox();
        HBox.setHgrow(m_listBox, Priority.ALWAYS);
        m_listBox.setPadding(new Insets(PADDING));
        m_listBox.setAlignment(Pos.TOP_CENTER);

        getChildren().addAll(m_headerBox, m_listBox);
    }

    public HashMap<String, NoteInterface> appsMap(){
        return m_networksData.appsMap();
    }

    @Override
    public void sendMessage(int code, long timestamp,String networkId, String msg){
    
    }

    public void update(){
        
        double minSize = m_listBox.widthProperty().get() - 110;
        minSize = minSize < 110 ? 100 : minSize;

        int numCells = appsMap().size();
        double width = widthProperty().get();
        width = width < minSize ? width : minSize;
        
        String currentIconStyle = m_iconStyle.get();
        
        m_listBox.getChildren().clear();

        if (numCells != 0) {

           
            if (currentIconStyle.equals(IconStyle.ROW)) {
                for (Map.Entry<String, NoteInterface> entry : appsMap().entrySet()) {
                    NoteInterface noteInterface = entry.getValue();
            
                    IconButton iconButton = new IconButton(noteInterface.getAppIcon(), noteInterface.getName(), IconStyle.ROW);
                    iconButton.setPrefWidth(width);
                    m_listBox.getChildren().add(iconButton);
                }
            } else {

                double imageWidth = 75;
                double cellPadding = 15;
                double cellWidth = imageWidth + (cellPadding * 2);

                int floor = (int) Math.floor(width / cellWidth);
                int numCol = floor == 0 ? 1 : floor;
                // currentNumCols.set(numCol);
              //  int numRows = numCells > 0 && numCol != 0 ? (int) Math.ceil(numCells / (double) numCol) : 1;

              ArrayList<HBox> rowsBoxes = new ArrayList<HBox>();

              ItemIterator grid = new ItemIterator();
              //j = row
              //i = col
  
              for (Map.Entry<String, NoteInterface> entry : appsMap().entrySet()) {
                    NoteInterface noteInterface = entry.getValue();

                    if(rowsBoxes.size() < (grid.getJ() + 1)){
                        HBox newHBox = new HBox();
                        rowsBoxes.add(newHBox);
                        m_listBox.getChildren().add(newHBox);
                    }

                    HBox rowBox = rowsBoxes.get(grid.getJ());

                    IconButton iconButton = new IconButton(noteInterface.getAppIcon(), noteInterface.getName(), IconStyle.ICON);

                    rowBox.getChildren().add(iconButton);
    
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

  
    public String getName(){
        return NAME;
    }
   
    public void setCurrent(boolean value){
        m_menuBtn.setId(value ? "activeMenuBtn" : "menuTabBtn");
        m_current.set(value);
    }

    
    public boolean getCurrent(){
        return m_current.get();
    } 


    private SimpleStringProperty m_titleProperty = new SimpleStringProperty(NAME);

    public SimpleStringProperty titleProperty(){
        return m_titleProperty;
    }

    public void shutdown(){
        this.prefWidthProperty().unbind();
    }

    @Override
    public void sendMessage(int code, long timestamp, String networkId, Number number) {
        if(networkId != null && networkId.equals(NetworksData.NETWORK_ID)){
            update();
        }
    }
}

    

