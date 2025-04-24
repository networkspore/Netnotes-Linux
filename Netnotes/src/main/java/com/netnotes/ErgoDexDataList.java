package com.netnotes;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netnotes.engine.networks.ergo.ErgoBox;
import io.netnotes.engine.NetworksData;
import io.netnotes.engine.NoteConstants;
import io.netnotes.engine.NoteInterface;
import io.netnotes.engine.NoteMsgInterface;
import io.netnotes.engine.PriceQuote;
import io.netnotes.engine.Stages;
import io.netnotes.engine.Utils;
import io.netnotes.friendly_id.FriendlyId;
import com.google.gson.JsonArray;

import org.reactfx.util.FxTimer;
import org.reactfx.util.Timer;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ErgoDexDataList  {

    public final static String LOADING = "Loading...";
    public final static String NETWORK_ID = "spectrumDataList";
    public final static int LOAD_DELAY = 3000;
    private ErgoDex m_ergodex;
    private String m_locationId;

    private VBox m_gridBox;

    private ArrayList<ErgoDexMarketItem> m_marketsList = new ArrayList<ErgoDexMarketItem>();


    private SimpleStringProperty m_statusMsg = new SimpleStringProperty("Starting...");

    private ErgpDexSort m_sortMethod = new ErgpDexSort();
    private String m_searchText = null;

    private SimpleBooleanProperty m_isInvert;
   

    private int m_connectionStatus = NoteConstants.STOPPED;
    
    public static final int MIN_BTN_IMG_WIDTH = 350;
    

    private SimpleDoubleProperty m_gridWidth;
    private SimpleDoubleProperty m_gridHeight;
    private SimpleObjectProperty<TimeSpan> m_timeSpanObject;
    private SimpleObjectProperty<HBox> m_currentBox;

        
    private java.awt.Font m_headingFont = new java.awt.Font("OCR A Extended", java.awt.Font.PLAIN, 18);
    private java.awt.Font m_labelFont = new java.awt.Font("OCR A Extended", java.awt.Font.BOLD, 16);
    private int m_labelAscent;
    private int m_labelLeading;
    private int m_labelHeight;
    private FontMetrics m_labelMetrics;
    private BufferedImage m_labelImg = null;
    private Graphics2D m_labelG2d = null;

    private NoteMsgInterface m_spectrumMsgInterface = null;
    private Stage m_appStage;
    private SimpleObjectProperty<NoteInterface> m_ergoNetworkInterfaceProperty = new SimpleObjectProperty<>(null);
    private ScrollPane m_scrollPane;
    private TextField m_updatedField;
    private VBox m_layoutBox;
    private double m_defaultCharSize;
    private VBox m_loadingBox;
    private SimpleBooleanProperty m_isNetworkEnabled = new SimpleBooleanProperty(false);
    
    public ErgoDexDataList(String locationId,Stage appStage, ErgoDex ergoDex, SimpleBooleanProperty isInvert, SimpleDoubleProperty gridWidth, SimpleDoubleProperty gridHeight, TextField updatedField,  SimpleObjectProperty<TimeSpan> timeSpanObject, SimpleObjectProperty<NoteInterface> networkInterface, ScrollPane scrollPane) {
        m_currentBox = new SimpleObjectProperty<>(null);

        m_locationId = locationId;
        m_ergodex = ergoDex;
        m_appStage = appStage;
        m_isInvert = isInvert;
        m_scrollPane = scrollPane;
        m_updatedField = updatedField;
        m_ergoNetworkInterfaceProperty = networkInterface;
        m_gridWidth = new SimpleDoubleProperty();

        m_gridWidth.bind(gridWidth.subtract(22));

        m_gridHeight = gridHeight;
        m_timeSpanObject = timeSpanObject;

        m_defaultCharSize = Utils.computeTextWidth(Stages.txtFont, " ");

        m_layoutBox = new VBox();
        m_layoutBox.setId("darkBox");

        m_loadingBox = new VBox();
        m_loadingBox.setId("darkBox");
        m_loadingBox.prefWidthProperty().bind(m_gridWidth.add(5));
        setLoadingBox();
       

        m_gridBox = new VBox();
        HBox.setHgrow(m_gridBox,Priority.ALWAYS);
        updateFont();
       
        m_isInvert.addListener((obs,oldval,newval)->{
            ErgpDexSort sortMethod = getSortMethod();
            sortMethod.setSwapTarget(newval ?  ErgpDexSort.SwapMarket.SWAPPED : ErgpDexSort.SwapMarket.STANDARD);
            sort();
            updateGrid();
            
        });
        addDexistener();

        
        m_isNetworkEnabled.bind(Bindings.createObjectBinding(()->m_ergoNetworkInterfaceProperty.get() != null, m_ergoNetworkInterfaceProperty));
    }





    public void setLoadingBox(){
        ImageView imgView = new ImageView(m_ergodex.getAppIcon());
        imgView.setPreserveRatio(true);
        imgView.setFitWidth(150);

        Button loadingBtn = new Button(ErgoDex.NAME);
        loadingBtn.setTextFill(Color.WHITE);
        loadingBtn.setGraphicTextGap(30);
        loadingBtn.setId("startImageBtn");
        loadingBtn.setGraphicTextGap(15);
        loadingBtn.setGraphic(imgView);
        loadingBtn.setContentDisplay(ContentDisplay.TOP);
        loadingBtn.setPadding(new Insets(0,0,30,0));

        ProgressBar progressBar = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setPrefWidth(200);

        HBox progressBarBox = new HBox(progressBar);
        HBox.setHgrow(progressBarBox,Priority.ALWAYS);
        progressBarBox.setAlignment(Pos.CENTER);

        VBox imageBox = new VBox(loadingBtn, progressBarBox);
        imageBox.setId("transparentColor");
        imageBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(imageBox, Priority.ALWAYS);
      

        Label statusLabel = new Label(m_statusMsg.get());
        statusLabel.setId("italicFont");
   
        
        m_statusMsg.addListener((obs,oldVal,newval)->{
            
            switch(newval){
                case LOADING:

                    if(!progressBarBox.getChildren().contains(progressBar)){
                        progressBarBox.getChildren().add(progressBar);
                    }

                    statusLabel.setText( LOADING);

                    return;
                case NoteConstants.STATUS_ERROR:
                    
                    statusLabel.setText( m_errMsg);

                    if(progressBarBox.getChildren().contains(progressBar)){
                        progressBarBox.getChildren().remove(progressBar);
                    }
                    return;
            }

            statusLabel.setText( LOADING);
            
        });
        
        progressBarBox.setPadding(new Insets(0,0,0,0));

        VBox statusLabelBox = new VBox( statusLabel);
        HBox.setHgrow(statusLabelBox, Priority.ALWAYS);
        statusLabelBox.setAlignment(Pos.CENTER);


        imageBox.prefHeightProperty().bind(m_gridHeight);

        //imageBox.setPadding(new Insets(0,0,40,0));

        m_loadingBox.getChildren().addAll(imageBox,statusLabelBox);
        m_layoutBox.getChildren().clear();
        m_layoutBox.getChildren().add(m_loadingBox);
    }



    public double defaultCharacterSize(){
        return m_defaultCharSize;
    }

    public SimpleObjectProperty<HBox> currentBoxProperty(){
        return m_currentBox;
    }

    public ScrollPane getScrollPane(){
        return m_scrollPane;
    }

    public SimpleObjectProperty<NoteInterface> ergoInterfaceProperty(){
        return m_ergoNetworkInterfaceProperty;
    }

    public boolean isNetworkEnabled(){
        return m_isNetworkEnabled.get();
    }

    public ReadOnlyBooleanProperty isNetworkEnabledProperty(){
        return m_isNetworkEnabled;
    }
    
    public Stage appStage(){
        return m_appStage;
    }



    public String getLocationId(){
        return m_locationId;
    }


    public void addDexistener(){

        m_spectrumMsgInterface = new NoteMsgInterface() {
            private String m_id = FriendlyId.createFriendlyId();
           
            public String getId(){
                return m_id;
            }

            public void sendMessage(int code, long timeStamp, String poolId, Number num){
            
                switch(code){
                    case NoteConstants.LIST_CHANGED:
                    case NoteConstants.LIST_UPDATED:
                        m_statusMsg.set(NoteConstants.STATUS_STARTED);
                        updateMarkets(m_ergodex.marketsList());
                        m_connectionStatus = NoteConstants.STARTED;
                    

                    case NoteConstants.STOPPED:

                    break;
                    case NoteConstants.STARTED:
                        m_connectionStatus = NoteConstants.STARTED;
                    case NoteConstants.ERROR:
                        //   JsonElement msgElement = json != null ? json.get("msg") : null;
                        m_connectionStatus = NoteConstants.ERROR;
                        
                        
                    //  getLastUpdated().set(LocalDateTime.now());
                    break;
                } 
                
            }
        
            public void sendMessage(int code, long timestamp, String networkId, String msg){
                if(code == NoteConstants.ERROR){
                    m_connectionStatus = NoteConstants.ERROR;
                    m_errMsg = msg;
                    m_statusMsg.set(NoteConstants.STATUS_ERROR);
                }
            }

    

        };
        
        if(m_ergodex.getConnectionStatus() == NoteConstants.STARTED && m_ergodex.marketsList().size() > 0){
            updateMarkets(m_ergodex.marketsList());
            m_connectionStatus = NoteConstants.STARTED;
        }

        m_ergodex.addMsgListener(m_spectrumMsgInterface);
  
        
    }

    private String m_errMsg = "";

    public Image getSmallAppIcon(){ 
        return null;
    }

    public SimpleDoubleProperty gridWidthProperty(){
        return m_gridWidth;
    };
    public SimpleDoubleProperty gridHeightProperty(){
        return m_gridHeight;
    }
    public SimpleObjectProperty<TimeSpan> timeSpanObjectProperty(){
        return m_timeSpanObject;
    } 




    public void updateFont(){
    
        m_labelImg = new BufferedImage(1,1, BufferedImage.TYPE_INT_ARGB);
        m_labelG2d = m_labelImg.createGraphics();
        m_labelG2d.setFont(m_labelFont);
        m_labelMetrics = m_labelG2d.getFontMetrics();
        m_labelLeading = m_labelMetrics.getLeading();
        m_labelAscent = m_labelMetrics.getAscent();
        m_labelHeight = m_labelMetrics.getHeight();
        m_labelG2d.dispose();
        m_labelG2d = null;
        m_labelImg = null;
    }

    public FontMetrics getLabelMetrics(){
        return m_labelMetrics;
    }
    public int getLabelAscent(){
        return m_labelAscent;
    }
    public int getLabelLeading(){
        return m_labelLeading;
    }
    public int getLabelHeight(){
        return m_labelHeight;
    }

    public java.awt.Font getLabelFont(){
        return m_labelFont;
    }

    public java.awt.Font getHeadingFont(){
        return m_headingFont;
    }

    
 

    public SimpleBooleanProperty isInvertProperty(){
        return m_isInvert;
    }


    public void updateMarkets(ArrayList<ErgoDexMarketData> marketsArray) {
        
        int updateSize = marketsArray.size() ;
        
        SimpleBooleanProperty update = new SimpleBooleanProperty(false);

        for(int i = 0; i< updateSize ; i++){
            
            ErgoDexMarketData marketData = marketsArray.get(i);
        

        
            ErgoDexMarketItem item = getMarketItem(marketData.getId());
            if(item == null){
                ErgoDexMarketItem newItem = new ErgoDexMarketItem( marketData, getSpectrumDataList());
                m_marketsList.add(newItem);
                newItem.init();
                update.set(true);
            }
            
        }
        
        if(update.get() || m_connectionStatus == NoteConstants.ERROR){
            sort();
            updateGrid();
        }
                
        m_updatedField.setText(Utils.formatDateTimeString( LocalDateTime.now()));
    }



    public SimpleStringProperty statusMsgProperty() {
        return m_statusMsg;
    }



    public ErgoDexMarketItem getMarketItem(String id) {
        if (id != null) {
            for (ErgoDexMarketItem item : m_marketsList) {
                if (item.getId().equals(id)) {
                    return item;
                }
            }
        }
        return null;
    }

    public ErgoDexDataList getSpectrumDataList(){
        return this;
    }

    public NetworksData getNetworksData(){
        return m_ergodex.getNetworksData();
    }



    public VBox getLayoutBox() {
        
        updateGrid();

        return m_layoutBox;
    }


    public void update(){
        m_gridBox.getChildren().clear();  
        if (m_searchText == null) {
                
            int numCells = m_marketsList.size() > 100 ? 100 : m_marketsList.size();
            for (int i = 0; i < numCells; i++) {
                ErgoDexMarketItem marketItem = m_marketsList.get(i);

                HBox rowBox = marketItem.getRowBox();
                marketItem.setItemIndex(i);
                m_gridBox.getChildren().add(rowBox);

            }
                
            
            
        } else {

            // List<SpectrumMarketItem> searchResultsList = m_marketsList.stream().filter(marketItem -> marketItem.getSymbol().contains(m_searchText.toUpperCase())).collect(Collectors.toList());
            doSearch( onSuccess -> {
                WorkerStateEvent event = onSuccess;
                Object sourceObject = event.getSource().getValue();

                if (sourceObject instanceof ErgoDexMarketItem[]) {
                    ErgoDexMarketItem[] searchResults = (ErgoDexMarketItem[]) sourceObject;
                    int numResults = searchResults.length > 100 ? 100 : searchResults.length;
                    m_gridBox.getChildren().clear();
                    for (int i = 0; i < numResults; i++) {
                        ErgoDexMarketItem marketItem = searchResults[i];

                        HBox rowBox = marketItem.getRowBox();
                        marketItem.setItemIndex(i);
                        m_gridBox.getChildren().add(rowBox);
                    }
                }
            }, onFailed -> {
            });

        }
    }
    private Timer m_timer = null;
    public void updateGrid(){
        
         
        if (m_marketsList.size() > 0) {
            update();
            
            if(!m_layoutBox.getChildren().contains(m_gridBox)){
                if(m_timer == null){
                    m_timer = FxTimer.runLater(Duration.ofMillis(LOAD_DELAY),()->{
                        m_layoutBox.getChildren().clear();
                        m_layoutBox.getChildren().add(m_gridBox);
                    });
                }
            }

           
        } 

      
    }

    public void sort(){
        sort(true);
    }

    public void sort(boolean doSave) {
        String type = m_sortMethod.getType();
        boolean isAsc = m_sortMethod.isAsc();
        boolean swapped = m_sortMethod.isTargetSwapped();
        switch (type) {
            case ErgpDexSort.SortType.LIQUIDITY_VOL:
                if (isAsc) {
                    Collections.sort(m_marketsList, Comparator.comparing(ErgoDexMarketItem::getLiquidityUSD));

                } else {
                    Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(ErgoDexMarketItem::getLiquidityUSD)));
                }
                break;
            case ErgpDexSort.SortType.LAST_PRICE:
                
                if (isAsc) {
                    Collections.sort(m_marketsList, Comparator.comparing(ErgoDexMarketItem::getLastPrice));

                } else {
                    Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(ErgoDexMarketItem::getLastPrice)));
                }
                break;
            case ErgpDexSort.SortType.BASE_VOL:
                if(!swapped){
                    if (isAsc) {
                        Collections.sort(m_marketsList, Comparator.comparing(ErgoDexMarketItem::getBaseVolume));
                    } else {
                        Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(ErgoDexMarketItem::getBaseVolume)));
                    }
                }else{
                    if (isAsc) {
                        Collections.sort(m_marketsList, Comparator.comparing(ErgoDexMarketItem::getQuoteVolume));
                    } else {
                        Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(ErgoDexMarketItem::getQuoteVolume)));
                    }
                }
                break;
            case ErgpDexSort.SortType.QUOTE_VOL:
                if(!swapped){
                    if (isAsc) {
                        Collections.sort(m_marketsList, Comparator.comparing(ErgoDexMarketItem::getQuoteVolume));
                    } else {
                        Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(ErgoDexMarketItem::getQuoteVolume)));
                    }
                }else{
                    if (isAsc) {
                        Collections.sort(m_marketsList, Comparator.comparing(ErgoDexMarketItem::getBaseVolume));
                    } else {
                        Collections.sort(m_marketsList, Collections.reverseOrder(Comparator.comparing(ErgoDexMarketItem::getBaseVolume)));
                    }
                }
                break;
        

        }
    
        //int maxItems = m_marketsList.size() > 100 ? 100 : m_marketsList.size();
       // m_statusMsg.set("Top "+maxItems+" - " + m_sortMethod.getType() + " " + (m_sortMethod.isAsc() ? "(Low to High)" : "(High to Low)"));
     
    }

    public ErgpDexSort getSortMethod(){
        return m_sortMethod;
    }

    public void setSortMethod(ErgpDexSort sortMethod){
        m_sortMethod = sortMethod;
        sort();
      
    }

    public void setSearchText(String text) {
        m_searchText = text.equals("") ? null : text;

        updateGrid();

    }

    public String getSearchText() {
        return m_searchText;
    }

    private void doSearch( EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed) {
        Task<ErgoDexMarketItem[]> task = new Task<ErgoDexMarketItem[]>() {
            @Override
            public ErgoDexMarketItem[] call() {
                List<ErgoDexMarketItem> searchResultsList = m_marketsList.stream().filter(marketItem -> marketItem.getSymbol().toUpperCase().contains(m_searchText.toUpperCase())).collect(Collectors.toList());

                ErgoDexMarketItem[] results = new ErgoDexMarketItem[searchResultsList.size()];

                searchResultsList.toArray(results);

                return results;
                

                
                
            }
        };

        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    
 



 

    /*public void loadPoolTest(){
        Utils.getStringFromResource("/ergoDex/contracts/pool/N2TPoolSample.hex",getNetworksData().getExecService(), (onSucceded)->{
            Object sourceValue = onSucceded.getSource().getValue();
            if(sourceValue != null && sourceValue instanceof String){
                String n2TSampleHex = (String) sourceValue;
                
                getUnspentByErgoTree(n2TSampleHex, (onResult)->{
                    Object onResultValue = onResult.getSource().getValue();
                    if(onResultValue != null && onResultValue instanceof JsonObject){
                        JsonObject json = (JsonObject) onResultValue;
                     
                        JsonElement itemsElement = json.get("items");
                        if(itemsElement != null && itemsElement.isJsonArray()){
                            getBoxesFromJsonArrayElement(itemsElement, getNetworksData().getExecService(), (onBoxes)->{

                            }, onBoxesFailed->{

                            });
                        }
                    }
                }, (onFailed)->{
                    try {
                        Files.writeString(NoteConstants.logFile.toPath(), "failed: " + onFailed.getSource().getException().toString() + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
    
                    }
                });
                
            }
        }, onFailed->{

        });
    }*/

    public static Future<?> getBoxesFromJsonArrayElement(JsonElement jsonArrayElement, ExecutorService execService,  EventHandler<WorkerStateEvent> onSucceeded, EventHandler<WorkerStateEvent> onFailed){
        Task<Object> task = new Task<Object>() {
            @Override
            public Object call() throws Exception{
                
                JsonArray boxArray = jsonArrayElement.getAsJsonArray();
                int size = boxArray.size();

                ErgoDexBoxInfo[] dexPools = new ErgoDexBoxInfo[size];
                for(int i = 0; i< size ; i++){
                    JsonElement jsonElement = boxArray.get(i);
                    ErgoBox box = jsonElement != null && !jsonElement.isJsonNull() && jsonElement.isJsonObject() ? new ErgoBox(jsonElement.getAsJsonObject()) : null;
                    
                    dexPools[i] = new ErgoDexBoxInfo(box);
                }

                return null;
            }
        };
        task.setOnFailed(onFailed);

        task.setOnSucceeded(onSucceeded);

        return execService.submit(task);
    }

    public PriceQuote getTokenQuoteInErg(String tokenId){
        JsonObject note = NoteConstants.getCmdObject("getTokenQuoteInErg");
        note.addProperty("locationId", getLocationId());
        note.addProperty("tokenId", tokenId);
        Object obj = getErgoDex().sendNote(note);
        return obj != null && obj instanceof PriceQuote ? (PriceQuote) obj : null;
    }

    public PriceQuote getErgoQuoteInToken(String quoteId){
        JsonObject note = NoteConstants.getCmdObject("getQuoteById");
        note.addProperty("locationId", getLocationId());
        note.addProperty("quoteId", quoteId);
        Object obj = getErgoDex().sendNote(note);
        return obj != null && obj instanceof PriceQuote ? (PriceQuote) obj : null;
    }

    
    public PriceQuote getQuoteById(String baseId, String quoteId){
        JsonObject note = NoteConstants.getCmdObject("getQuoteById");
        note.addProperty("locationId", getLocationId());
        note.addProperty("baseId", baseId);
        note.addProperty("quoteId", quoteId);
        Object obj = getErgoDex().sendNote(note);
        return obj != null && obj instanceof PriceQuote ? (PriceQuote) obj : null;
    }

    /*
     export class N2TAmmPoolsParser implements FromBox<AmmPool> {
        from(box: ErgoBox): AmmPool | undefined {
            const r4 = box.additionalRegisters[RegisterId.R4]
            if (box.assets.length == 3 && r4) {
                const nft = box.assets[0].tokenId
                const lp = AssetAmount.fromToken(box.assets[1])
                const assetX = AssetAmount.native(box.value)
                const assetY = AssetAmount.fromToken(box.assets[2])
                const feeNum = deserializeConstant(r4)
                if (feeNum instanceof Int32Constant) 
                    return new AmmPool(nft, lp, assetX, assetY, feeNum.value)
            }
        return undefined
        }
        
        fromMany(boxes: ErgoBox[]): AmmPool[] {
            const pools = []
            for (const box of boxes) {
                const pool = this.from(box)
            if (pool) pools.push(pool)
        }
        return pools
    }
     */


    public ErgoDex getErgoDex() {
        return m_ergodex;
    }

 

    public void shutdown(){
 
        m_connectionStatus = NoteConstants.STOPPED;

        m_marketsList.forEach((item)->{
            item.shutdown();
        });
       
        statusMsgProperty().set(NoteConstants.STATUS_STOPPED);

        m_ergodex.removeMsgListener(m_spectrumMsgInterface);
        m_spectrumMsgInterface = null;

    }
    
}
