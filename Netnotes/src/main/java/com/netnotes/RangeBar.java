package com.netnotes;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import com.devskiller.friendly_id.FriendlyId;
import com.utils.Utils;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;


public class RangeBar extends ImageView implements ControlInterface{

   public static int DEFAULT_BUTTON_HEIGHT = 0;

    private SimpleDoubleProperty m_height;
    private SimpleDoubleProperty m_width;

    public final static int MIN_WIDTH = 10;
    public final static int MIN_HEIGHT = 5;

    public final static int BG_RGB = 0xff000000;

    private double m_maxTop = 1;
    private SimpleDoubleProperty m_topVvalue = new SimpleDoubleProperty(1);
    private SimpleDoubleProperty m_bottomVvalue = new SimpleDoubleProperty(0);
    private SimpleBooleanProperty m_settingRange = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty m_active = new SimpleBooleanProperty(false);
    private RangeStyle m_rangeStyle = new RangeStyle(RangeStyle.AUTO); 

    private double m_minBot = 0;

    private int m_btnHeight = DEFAULT_BUTTON_HEIGHT;

    /*private int m_shadingLightRGB = 0xffffffff;
    private int m_shadingDarkRGB = 0xff000000;
    private int m_shadingGreenRGB = 0x9000ff00;
    private int m_shadingRedRGB = 0x90ff0000;
    private int m_shadingLightRedRGB = 0xff9A2A2A;

    private int m_btnTopBgColor1 = 0x404bbd94;
    private int m_btnTopBgColor2 = 0xff000000;
    private int m_btnTopBgColor3 = 0x004bbd94;
    private int m_btnTopBgColor4 = 0x104bbd94;
    private int m_btnTopBorderColor = 0xff028A0F;
    private int m_btnTopBorderColor2 = 0x70000000;

    private int m_btnBotBgColor1 = 0x50e96d71;
    private int m_btnBotBgColor2 = 0xff000000;
    private int m_btnBotBgColor3 = 0x00e96d71;
    private int m_btnBotBgColor4 = 0x10e96d71;
    private int m_btnBotBorderColor = 0xff9A2A2A;
    private int m_btnBotBorderColor2 = 0x70000000;*/
    

    private Image m_collapseImage = new Image("/assets/collapse-20.png");

    private int m_bg1 = 0x50ffffff;
    private int m_bg1active = 0x90ffffff;
    private int m_bg1setting = 0xC0ffffff;
    private int m_bg2 = 0x50000000;

    private int m_barRGB1 = 0x55333333;
    private int m_barRGB2 = 0x50ffffff;

    private int m_currentSelectionIndex = -1;

    private BufferedImage m_imgBuf = null;
    private WritableImage m_wImg = null;

    private AtomicReference<Double> m_mouseLocation = new AtomicReference<Double>(0.0);
    private Future<?> m_lastExecution = null;
    
    public final static long EXECUTION_TIME = 200;

    private String m_networkId;

    public RangeBar(SimpleDoubleProperty width, SimpleDoubleProperty height,  ExecutorService executor) {
        super();
        m_networkId = FriendlyId.createFriendlyId();
        m_width = width;
        m_height = height;
    


            
        EventHandler<? super MouseEvent> mouseMoveEventHandler = (mouseEvent) ->{

            if(mouseEvent.getY() > 0 && mouseEvent.getY() < getHeight()){
                m_mouseLocation.set(mouseEvent.getY());
                
                if (m_lastExecution == null || (m_lastExecution != null && m_lastExecution.isDone())) {

                    m_lastExecution = executor.submit(()->{
                        
                        Utils.returnObject(null, executor, (onSucceeded)->{
                            setRangeByY(m_mouseLocation.get(), getHeight());
                            updateImage();
                        }, (onFailed)->{});

                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                    
                        }
                    });
                }

                
            }       
        };
        
    
        setPreserveRatio(true);
        setBgImage( m_width, m_height);
        updateImage();
        
        setOnMousePressed((mouseEvent) ->{ 
            if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                
                onMousePressed(mouseEvent);
                if(m_settingRange.get()){
               
                    addEventFilter(MouseEvent.MOUSE_DRAGGED, mouseMoveEventHandler);
                }
            }
        });
        
        //setOnMouseDragged(mouseEvent -> onMouseMoved(mouseEvent));
        setOnMouseReleased((mouseEvent) -> {

            onMouseReleased(mouseEvent);
        
            removeEventFilter(MouseEvent.MOUSE_DRAGGED, mouseMoveEventHandler);
            
            
        });
        setId("rangeBar");
        
  

        
        m_height.addListener((obs, oldVal, newVal) -> {
            setBgImage(m_width, m_height);
            updateImage();
        });
        m_width.addListener((obs, oldVal, newVal) -> {
            setBgImage(m_width, m_height);
            updateImage();
        });

        
    }

    public String getNetworkId(){
        return m_networkId;
    }

    public HBox getControlBox(){
       

        HBox rangeStyleIsAutoIndicator = new HBox();
        rangeStyleIsAutoIndicator.setId("menuBarCircleBtn");
        rangeStyleIsAutoIndicator.setMinHeight(6);
        rangeStyleIsAutoIndicator.setMaxHeight(6);
        rangeStyleIsAutoIndicator.setMinWidth(6);
        rangeStyleIsAutoIndicator.setMaxWidth(6);

      

        Text styleText = new Text("Auto");
        styleText.setFont(App.titleFont);
        styleText.setFill(App.altColor);

        Region indicatorRegion = new Region();
        indicatorRegion.setMinWidth(5);

        HBox rangeStyleBtn = new HBox(rangeStyleIsAutoIndicator,indicatorRegion, styleText);
        rangeStyleBtn.setId("toggleBox");
        rangeStyleBtn.setPrefWidth(55);
        rangeStyleBtn.setPrefHeight(20);
        rangeStyleBtn.setAlignment(Pos.CENTER_LEFT);
        rangeStyleBtn.setPadding(new Insets(0,0,0,15));
        

   



        HBox rangeStyleBox = new HBox(rangeStyleBtn);
        //rangeStyleBox.setId("urlField");
        rangeStyleBox.setPrefHeight(25);
        
        VBox rangeStyleBtnBox = new VBox(rangeStyleBox);
        rangeStyleBtnBox.setAlignment(Pos.CENTER);
        rangeStyleBtnBox.setPadding(new Insets(0,5,0,10));
        

        
    
        BufferedButton setRangeBtn = new BufferedButton("/assets/checkmark-25.png");
        setRangeBtn.getBufferedImageView().setFitWidth(15);
       
        setRangeBtn.setId("circleGoBtn");
        setRangeBtn.setMaxWidth(20);
        setRangeBtn.setMaxHeight(20);

        Region btnSpacer = new Region();
        btnSpacer.setMinWidth(5);

        BufferedButton cancelRangeBtn = new BufferedButton("/assets/close-outline-white.png");
        cancelRangeBtn.getBufferedImageView().setFitWidth(15);
        cancelRangeBtn.setId("menuBarCircleBtn");
        
        cancelRangeBtn.setMaxWidth(20);
        cancelRangeBtn.setMaxHeight(20);

        Text rangeText = new Text(String.format("%-14s", "Select range"));
        rangeText.setFont(App.titleFont);
        rangeText.setFill(App.txtColor);


        Text topValueText = new Text("Top");
        topValueText.setFont(App.titleFont);
        topValueText.setFill(App.txtColor);

        TextField topValueTextField = new TextField();
        topValueTextField.setId("urlField");

        Runnable setTopVvalueByText = () ->{
            BigDecimal topChartValue = BigDecimal.valueOf(m_rangeStyle.getChartHeight()).divide(m_rangeStyle.getScale(), 13, RoundingMode.HALF_UP);
            BigDecimal topRangePrice = new BigDecimal(topValueTextField.getText());

            double tVv = topRangePrice.divide(topChartValue, 12, RoundingMode.HALF_UP).doubleValue();
            double bVv = m_bottomVvalue.get();
            
            setTopVvalue(Math.min(1, Math.max(tVv, bVv+.005)), true);
        };
        
       ChangeListener<String> topValueListener = (obs,oldval,newval)->{
            String number = newval.replaceAll("[^0-9.]", "");
            int index = number.indexOf(".");
            String leftSide = index != -1 ? number.substring(0, index + 1) : number;
            String rightSide = index != -1 ?  number.substring(index + 1) : "";
            rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
            rightSide = rightSide.length() > 20 ? rightSide.substring(0, 20) : rightSide;
            topValueTextField.setText(leftSide +  rightSide);

        
        };
        topValueTextField.textProperty().addListener(topValueListener);
        topValueTextField.setOnAction(e->setTopVvalueByText.run());

        Region tvtbRegion = new Region();
        tvtbRegion.setMinWidth(5);


        HBox topValueTextBox = new HBox(topValueText,tvtbRegion, topValueTextField);
        topValueTextBox.setAlignment(Pos.CENTER_LEFT);
        topValueTextBox.setPadding(new Insets(0,0,0,10));

        Text botValueText = new Text("Bottom");
        botValueText.setFont(App.titleFont);
        botValueText.setFill(App.txtColor);

        TextField botValueTextField = new TextField();
        botValueTextField.setId("urlField");

        Runnable setBotVvalueByText = () ->{
            BigDecimal botChartValue = BigDecimal.valueOf(m_rangeStyle.getChartHeight()).divide(m_rangeStyle.getScale(), 13, RoundingMode.HALF_UP);
            BigDecimal botRangePrice = new BigDecimal(botValueTextField.getText());

            double bVv = botRangePrice.divide(botChartValue, 12, RoundingMode.HALF_UP).doubleValue();
            double tVv = m_topVvalue.get();
            
            setBotVvalue(Math.min(tVv - .005, Math.max(bVv, 0)), true);
        };

        botValueTextField.setOnAction(e->setBotVvalueByText.run());

        ChangeListener<String> botChangeListener = (obs,oldval,newval)->{
            String number = newval.replaceAll("[^0-9.]", "");
            int index = number.indexOf(".");
            String leftSide = index != -1 ? number.substring(0, index + 1) : number;
            String rightSide = index != -1 ?  number.substring(index + 1) : "";
            rightSide = rightSide.length() > 0 ? rightSide.replaceAll("[^0-9]", "") : "";
            rightSide = rightSide.length() > 20 ? rightSide.substring(0, 20) : rightSide;
            botValueTextField.setText(leftSide +  rightSide);

            
        };
        botValueTextField.textProperty().addListener(botChangeListener);

        Region bvtbRegion = new Region();
        bvtbRegion.setMinWidth(5);

        HBox botValueTextBox = new HBox(botValueText, bvtbRegion, botValueTextField);
        botValueTextBox.setAlignment(Pos.CENTER_LEFT);
        botValueTextBox.setPadding(new Insets(0,10,0,10));

        Runnable setBotValueTextField = ()->{
           
            botValueTextField.setText(m_rangeStyle.getBotValue() + "");
    
        };
     

        Runnable setTopValueTextField = ()->{
           
            topValueTextField.setText(m_rangeStyle.getTopValue() + "");
            
        };
     
        setBotValueTextField.run();
        setTopValueTextField.run();
        
        ChangeListener<Number> tVvChangeListener = (obs,oldval,newval)->{
            setTopValueTextField.run();
        };

        ChangeListener<Number> bVvChangeListener = (obs,oldval,newval)->{
            setBotValueTextField.run();
        };
      
        Button removeRangeSetting = new Button();

        ChangeListener<Boolean> isRangeSetting = (obs,oldval,newval) ->{
            if(!newval){
                removeRangeSetting.fire();
            }
        };
    
        m_settingRange.addListener(isRangeSetting);
        m_topVvalue.addListener(tVvChangeListener);
        m_bottomVvalue.addListener(bVvChangeListener);

        Runnable removeListeners = () ->{
            m_topVvalue.removeListener(tVvChangeListener);
            m_bottomVvalue.removeListener(bVvChangeListener);
            m_settingRange.removeListener(isRangeSetting);
        };

        removeRangeSetting.setOnAction(e->{
            removeListeners.run();
        });

        cancelRangeBtn.setOnAction(e->{
            removeListeners.run();
            stop();
        });

        setRangeBtn.setOnAction(e->{
            removeListeners.run();
            start();
        });

        rangeStyleBtn.addEventFilter(MouseEvent.MOUSE_PRESSED, e->{
            rangeStyleIsAutoIndicator.setId("circleGoBtn"); 
            rangeStyleBtn.setId("toggleBoxPressed");
        });

        rangeStyleBtn.addEventFilter(MouseEvent.MOUSE_RELEASED, e->{
            rangeStyleIsAutoIndicator.setId("menuBarCircleBtn"); 
            rangeStyleBtn.setId("toggleBox");
        });

        rangeStyleBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, (e)->{
            m_rangeStyle.setStyle(RangeStyle.AUTO);
            removeListeners.run();
            start();
        });
        

        HBox chartRangeToolbox = new HBox(rangeText,topValueTextBox, botValueTextBox ,rangeStyleBtnBox,  setRangeBtn, btnSpacer, cancelRangeBtn);
        chartRangeToolbox.setId("bodyBox");
        chartRangeToolbox.setAlignment(Pos.CENTER_LEFT);
        chartRangeToolbox.setPadding(new Insets(0,5,0,5));
    
        return chartRangeToolbox;
    }

    public static BigDecimal getValueFromVvalue(double vValue, double height, BigDecimal scale){
        return BigDecimal.valueOf(vValue * height).divide(scale, 15, RoundingMode.HALF_UP);
    }

    public void toggle(){
        toggleSettingRange();
    }

    public boolean isActive(){
        return m_active.get();
    }
    
    public void toggleSettingRange() {
   
        m_settingRange.set(!m_settingRange.get());
        m_currentSelectionIndex = -1;
        updateImage();

    }

    public void cancel(){
        if(m_settingRange.get()){
            toggle();
        }
    }

    public boolean isCancelled(){
        return m_settingRange.get() == false;
    }

    public void start(){
        m_settingRange.set(false);
        m_active.set(true);
        m_currentSelectionIndex = -1;
        updateImage();        
    }
    
    public void stop(){
        m_rangeStyle.setStyle(RangeStyle.NONE);
        m_currentSelectionIndex = -1;
        m_settingRange.set(false);
        m_active.set(false);
        m_topVvalue.set(m_maxTop);
        m_bottomVvalue.set(m_minBot);
        updateImage();
        
        
    }

    public void setTopVvalue(double value, boolean update){
        m_topVvalue.set(value);
        if(update){
            updateImage();
        }
    }

    public void setBotVvalue(double value, boolean update){
        m_bottomVvalue.set(value);
        if(update){
            updateImage();
        }
    }

    public SimpleDoubleProperty rangeBarWidthProperty() {
        return m_width;
    }

    public SimpleDoubleProperty rangeBarHeightProperty() {
        return m_height;
    }

    public void setBgImage(double width, double height) {
        setBgImage((int) Math.floor(width), (int) Math.floor(height));
    }
    

    public void setBgImage(int width, int height) {
        width = width < MIN_WIDTH ? MIN_WIDTH : width;
        height = height < MIN_HEIGHT ? MIN_HEIGHT : height;

        m_imgBuf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    
        Drawing.fillArea(m_imgBuf, BG_RGB, 0, 0, width, height, false);
        
        setFitWidth(m_imgBuf.getWidth());
    }

    public void setBgImage(SimpleDoubleProperty w, SimpleDoubleProperty h) {
        int width = (int) w.get();
        int height = (int) h.get();

        setBgImage(width, height);
    }


    public SimpleDoubleProperty topVvalueProperty() {
        return m_topVvalue;
    }

    public SimpleDoubleProperty bottomVvalueProperty() {
        return m_bottomVvalue;
    }

    public SimpleBooleanProperty settingRangeProperty() {
        return m_settingRange;
    }

    public void onMouseReleased(MouseEvent mouseEvent) {

        switch (m_currentSelectionIndex) {
            case 0:
                //Ok button up
                m_settingRange.set(false);

                m_active.set(true);

                m_currentSelectionIndex = -1;
                updateImage();

                break;

            case 1:
                m_currentSelectionIndex = -1;
                reset();
                break;
            case 2:
            case 3:
                m_currentSelectionIndex = -1;
                break;
        }

    }

    public void onMouseMoved(MouseEvent mouseEvent) {
        if (m_currentSelectionIndex == 2 || m_currentSelectionIndex == 3) {
            
            double topValue = m_topVvalue.get();
            double bottomValue = m_bottomVvalue.get();
            m_currentSelectionIndex = selectMouseButton(mouseEvent, getHeight(), topValue, bottomValue, m_btnHeight);

            setRangeByMouse(mouseEvent, getHeight());
            updateImage();

        }
    }


    public void reset(boolean update) {
        m_settingRange.set(false);
        m_active.set(false);
        m_rangeStyle.setStyle(RangeStyle.AUTO);
      

        if (update) {
            updateImage();
        }
    }

    public void reset() {
        this.reset(true);
    }

    public void setRangeByMouse(MouseEvent event, double height) {
        if (m_settingRange.get()) {
            double mouseY = event.getY();
            double newVal = 1.0 - (double) (mouseY - m_btnHeight) / (height - (m_btnHeight * 2));
            double topValue = m_topVvalue.get();
            double bottomValue = m_bottomVvalue.get();

            //rangeBarTopDown
            if (m_currentSelectionIndex == 2) {

                if (newVal > bottomValue && newVal <= m_maxTop) {
                    m_topVvalue.set(newVal);
                }

            }

            //rangeBarBotDown
            if (m_currentSelectionIndex == 3) {

                if (newVal < topValue && newVal >= m_minBot) {
                    m_bottomVvalue.set(newVal);
                }

            }

        }

    }

    public void setRangeByY(double y, double height) {
        
            
            double newVal = 1.0 - (double) (y - m_btnHeight) / (height - (m_btnHeight * 2));
            double topValue = m_topVvalue.get();
            double bottomValue = m_bottomVvalue.get();

            //rangeBarTopDown
            if (m_currentSelectionIndex == 2) {

                if (newVal > bottomValue && newVal <= m_maxTop) {
                    
                    m_topVvalue.set(newVal);
                }

            }

            //rangeBarBotDown
            if (m_currentSelectionIndex == 3) {

                if (newVal < topValue && newVal >= m_minBot) {
                    m_bottomVvalue.set(newVal);
                }

            }

        

    }


    public static int selectMouseButton(MouseEvent mouseEvent, double height, double topVvalue, double botVvalue, double btnHeight) {

        double mouseY = mouseEvent.getY();

        if (mouseY < btnHeight) {

            return 0;

        } else {
            if (mouseY > height - btnHeight) {

                return 1;

            } else {

                double newVal = 1.0 - (double) (mouseY - btnHeight) / (height - (btnHeight * 2));

                double distanceToY1 = topVvalue > newVal ? topVvalue - newVal : topVvalue == newVal ? 0 : newVal - topVvalue;
                double distanceToY2 = botVvalue > newVal ? botVvalue - newVal : botVvalue == newVal ? 0 : newVal - botVvalue;

                if (distanceToY1 < distanceToY2) {
                    return 2;
                } else {
                    return 3;
                }

            }
        }

    }

    public double getHeight() {
        return Math.ceil(getImage().getHeight());
    }


    private void onMousePressed(MouseEvent event) {
    

            boolean settingRange = m_settingRange.get();
            if (!settingRange) {
                m_rangeStyle.setStyle(RangeStyle.USER);
                m_settingRange.set(true);
                m_currentSelectionIndex = -1;
            } else {
                double topValue = m_topVvalue.get();
                double bottomValue = m_bottomVvalue.get();
                m_currentSelectionIndex = selectMouseButton(event, getHeight(), topValue, bottomValue, m_btnHeight);

                setRangeByMouse(event, getHeight());
            }

            updateImage();
        
    }

    public double getScrollScale(int height) {
        //  (mouseY - m_btnHeight) / (height - (m_btnHeight * 2))
        return ((double) (height - (m_btnHeight * 2))) / m_maxTop;
    }

    public int getY1(int height) {
        double topValue = m_topVvalue.get();
        return (topValue == m_maxTop ? 0 : (int) Math.ceil(((height - (m_btnHeight * 2)) - (getScrollScale(height) * topValue)))) + m_btnHeight;
    }

    public int getY2(int height) {
        double botValue = m_bottomVvalue.get();
        return (botValue == m_minBot ? height - (m_btnHeight * 2) : (int) Math.ceil(((height - (m_btnHeight * 2)) - (getScrollScale(height) * botValue)))) + m_btnHeight;
    }

   
    public RangeStyle getRangeStyle(){
        return m_rangeStyle;
    }

    public SimpleBooleanProperty activeProperty(){
        return m_active;
    }

    public void updateImage() {
        
        if(m_imgBuf == null){
            setBgImage(m_width, m_height);
        }
      
        int width = (int) m_imgBuf.getWidth();
        int height = (int) m_imgBuf.getHeight();

        int x1 = 2;
        int y1 = getY1(height);

        int x2 = width-4;
        int y2 = getY2(height);
        boolean settingRange = m_settingRange.get();
        boolean active = isActive();

        if (!settingRange) {
            Drawing.fillArea(m_imgBuf, 0x00010101, 0, 0, width, height, false);

            Drawing.drawBar(1, (active ? m_bg1active : m_settingRange.get() ? m_bg1setting : m_bg1), m_bg2, m_imgBuf, x1, y1, x2, y2);
            Drawing.drawBar(m_barRGB1, m_barRGB2, m_imgBuf, x1, y1, x2, y2);

            if (m_topVvalue.get() == 1 && m_bottomVvalue.get() == 0) {

               // Drawing.fillArea(m_imgBuf,m_pR, m_pW, 0x50111111, x1, (height / 2) - 10, x2, (height / 2) + 10, false);

               // Drawing.drawImageExact(m_imgBuf, m_pR, m_pW, m_collapseImage, m_collapseImage.getPixelReader(), (int) ((width/2) - (m_collapseImage.getWidth()/2) ),(int)( (height / 2) - (m_collapseImage.getHeight() / 2)), true);
               //WritableImage img, PixelReader pR1, PixelWriter pW1, Image img2, int x1, int y1, int limitAlpha
               Drawing.drawImageLimit(m_imgBuf, m_collapseImage, (int) ((width/2) - (m_collapseImage.getWidth()/2) ),(int)( (height / 2) - (m_collapseImage.getHeight() / 2)), 0x70);
            }
        } else {
            Drawing.fillArea(m_imgBuf, 0x20ffffff, 2, 0, width-3, height, true);
            Drawing.fillArea(m_imgBuf, 0xff000000, 2, 0, width-3, height, true);
          
            //RangeBar
            Drawing.drawBar(1,(active ? m_bg1active : m_settingRange.get() ? m_bg1setting : m_bg1), m_bg2, m_imgBuf, x1, y1 + 1, x2, y2 - 1);
            Drawing.drawBar(m_barRGB1, m_barRGB2, m_imgBuf, x1, y1 + 1, x2, y2 - 1);
            int sliderHeight = 9;
            int topSliderY2 = y1 + sliderHeight;


            int botSliderY1 = y2 - sliderHeight;

            Drawing.fillArea(m_imgBuf, 0x80ffffff, x1, y1, width-3, topSliderY2, true);
            Drawing.fillArea(m_imgBuf, 0x50000000, x1, botSliderY1, width-3, y2, true);
        }
        setImage(SwingFXUtils.toFXImage(m_imgBuf, m_wImg));
    }
}
