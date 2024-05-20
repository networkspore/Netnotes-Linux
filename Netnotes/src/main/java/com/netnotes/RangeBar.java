package com.netnotes;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import com.devskiller.friendly_id.FriendlyId;
import com.utils.Utils;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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

    private WritableImage m_imgBuf = null;
    private PixelWriter m_pW = null;
    private PixelReader m_pR = null;

    private AtomicReference<Double> m_mouseLocation = new AtomicReference<Double>(0.0);
    private Future<?> m_lastExecution = null;
    
    public final static long EXECUTION_TIME = 200;

    private String m_networkId;

    public RangeBar(SimpleDoubleProperty width, SimpleDoubleProperty height,  ExecutorService executor) {
        super();
        m_networkId = FriendlyId.createFriendlyId();
        m_width = width;
        m_height = height;
    

        
        Runnable updateMouseY =()->{
            Utils.returnObject(null, executor, (onSucceeded)->{
                setRangeByY(m_mouseLocation.get(), getHeight());
                updateImage();
            }, (onFailed)->{});
        };
            
        EventHandler<? super MouseEvent> mouseMoveEventHandler = (mouseEvent) ->{

            if(mouseEvent.getY() > 0 && mouseEvent.getY() < getHeight()){
                m_mouseLocation.set(mouseEvent.getY());
                
                if (m_lastExecution == null || (m_lastExecution != null && m_lastExecution.isDone())) {

                    m_lastExecution = executor.submit(()->{
                        updateMouseY.run();
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
        
            BufferedButton setRangeBtn = new BufferedButton("/assets/checkmark-25.png");
            setRangeBtn.getBufferedImageView().setFitWidth(15);
            setRangeBtn.setOnAction(e->start());
            setRangeBtn.setId("circleGoBtn");
            setRangeBtn.setMaxWidth(20);
            setRangeBtn.setMaxHeight(20);

            Region btnSpacer = new Region();
            btnSpacer.setMinWidth(5);

            BufferedButton cancelRangeBtn = new BufferedButton("/assets/close-outline-white.png");
            cancelRangeBtn.getBufferedImageView().setFitWidth(15);
            cancelRangeBtn.setId("menuBarCircleBtn");
            cancelRangeBtn.setOnAction(e->stop());
            cancelRangeBtn.setMaxWidth(20);
            cancelRangeBtn.setMaxHeight(20);

            Text rangeText = new Text(String.format("%-14s", "Select range"));
            rangeText.setFont(App.titleFont);
            rangeText.setFill(App.txtColor);

            HBox chartRangeToolbox = new HBox(rangeText, setRangeBtn, btnSpacer, cancelRangeBtn);
            chartRangeToolbox.setId("bodyBox");
            chartRangeToolbox.setAlignment(Pos.CENTER_LEFT);
            chartRangeToolbox.setPadding(new Insets(0,5,0,5));
        
        return chartRangeToolbox;
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
 
        m_currentSelectionIndex = -1;
        reset();
    
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

        m_imgBuf = new WritableImage(width, height);
        m_pW = m_imgBuf.getPixelWriter();
        m_pR = m_imgBuf.getPixelReader();
        Drawing.fillArea(m_imgBuf, m_pR, m_pW, BG_RGB, 0, 0, width, height, false);
        setImage(m_imgBuf);
        setFitWidth(m_imgBuf.getWidth());
    }

    public void setBgImage(SimpleDoubleProperty w, SimpleDoubleProperty h) {
        int width = (int) w.get();
        int height = (int) h.get();

        setBgImage(width, height);
    }

    public SimpleBooleanProperty activeProperty() {
        return m_active;
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
        m_topVvalue.set(m_maxTop);
        m_bottomVvalue.set(m_minBot);
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

    private double[] m_topBotRange = new double[] {0,0};
   
    public double[] getTopBotRange(){
        return m_topBotRange;
    }
    public void setTopBotRange(double[] value){
        m_topBotRange = value;
    }

    public void updateImage() {
        
        if(m_imgBuf == null){
            setBgImage(m_width, m_height);
        }/*else{
            int w = (int) m_width.get();
            int h = (int) m_height.get();
            w = w < 1 ? 1 : w;
            h = h < 1 ? 1 : h;
            if(m_imgBuf.getWidth() != w || m_imgBuf.getHeight() != h){
                setBgImage(m_width, m_height);
            }else{
                Drawing.fillArea(m_imgBuf, m_pR, m_pW, BG_RGB, 0, 0, w , h, false);
            }
        }*/
      
        int width = (int) m_imgBuf.getWidth();
        int height = (int) m_imgBuf.getHeight();

        /*int btnTopX1 = 0;
        int btnTopY1 = 0;
        int btnTopX2 = width;
        int btnTopY2 = m_btnHeight;

        int btnBotX1 = 0;
        int btnBotY1 = height - m_btnHeight;
        int btnBotX2 = width;
        int btnBotY2 = height;*/
        // double imgScale = getScrollScale(height);
        int x1 = 2;
        int y1 = getY1(height);

        int x2 = width-4;
        int y2 = getY2(height);
        boolean settingRange = m_settingRange.get();

        if (!settingRange) {
            Drawing.fillArea(m_imgBuf,m_pR, m_pW, 0x00010101, 0, 0, width, height, false);

            Drawing.drawBar(1, (m_active.get() ? m_bg1active : m_settingRange.get() ? m_bg1setting : m_bg1), m_bg2, m_imgBuf,m_pR,m_pW, x1, y1, x2, y2);
            Drawing.drawBar(m_barRGB1, m_barRGB2, m_imgBuf,m_pR, m_pW, x1, y1, x2, y2);

            if (m_topVvalue.get() == 1 && m_bottomVvalue.get() == 0) {

                Drawing.fillArea(m_imgBuf,m_pR, m_pW, 0x50000000, x1, (height / 2) - 10, x2, (height / 2) + 10, false);

                Drawing.drawImageExact(m_imgBuf, m_pR, m_pW, m_collapseImage, m_collapseImage.getPixelReader(), (int) ((width/2) - (m_collapseImage.getWidth()/2)),(int)( (height / 2) - (m_collapseImage.getHeight() / 2)), false);
            }
        } else {
            Drawing.fillArea(m_imgBuf,m_pR, m_pW, 0x20ffffff, 2, 0, width-3, height, true);
            Drawing.fillArea(m_imgBuf,m_pR, m_pW, 0xff000000, 2, 0, width-3, height, true);
            //OkBtn
           /* boolean okBtnDown = m_currentSelectionIndex == 0;
            int okShadingRGB1 = m_shadingLightRGB;
            int okShadingRGB2 = m_shadingGreenRGB;
            int okRGB1 = m_btnTopBgColor1;
            int okRGB2 = m_btnTopBgColor2;
            int okRGB3 = m_btnTopBgColor3;
            int okRGB4 = m_btnTopBgColor4;

            if (okBtnDown) {
                okShadingRGB1 = m_shadingDarkRGB;
                okShadingRGB2 = m_shadingGreenRGB;

            }

            Drawing.drawBar(1, okShadingRGB1, okShadingRGB2, m_imgBuf, btnTopX1, btnTopY1, btnTopX2, btnTopY2);
            Drawing.drawBar(1, okRGB1, okRGB2, m_imgBuf, btnTopX1, btnTopY1, btnTopX2, btnTopY2);
            Drawing.drawBar(0, okRGB3, okRGB4, m_imgBuf, btnTopX1, btnTopY1, btnTopX2, btnTopY2);

            Drawing.fillArea(m_imgBuf, m_btnTopBorderColor2, btnTopX1, btnTopY1, btnTopX1 + 1, btnTopY2);
            Drawing.fillArea(m_imgBuf, m_btnTopBorderColor2, btnTopX1, btnTopY1, btnTopX2, btnTopY1 + 1);
            Drawing.fillArea(m_imgBuf, m_btnTopBorderColor, btnTopX2 - 1, btnTopY1, btnTopX2, btnTopY2);
            Drawing.fillArea(m_imgBuf, m_btnTopBorderColor, btnTopX1 + 1, btnTopY2 - 1, btnTopX2 - 1, btnTopY2);

            //Cancel Btn
            boolean cancelBtnDown = m_currentSelectionIndex == 1;
            int cancelShadingRGB1 = m_shadingRedRGB;
            int cancelShadingRGB2 = m_shadingLightRedRGB; //
            int cancelRGB1 = m_btnBotBgColor1;
            int cancelRGB2 = m_btnBotBgColor2;
            int cancelRGB3 = m_btnBotBgColor3;
            int cancelRGB4 = m_btnBotBgColor4;

            if (cancelBtnDown) {
                cancelShadingRGB1 = m_shadingDarkRGB;
                cancelShadingRGB2 = m_shadingRedRGB;
            }

            Drawing.drawBar(1, cancelShadingRGB1, cancelShadingRGB2, m_imgBuf, btnBotX1, btnBotY1, btnBotX2, btnBotY2);
            Drawing.drawBar(1, cancelRGB1, cancelRGB2, m_imgBuf, btnBotX1, btnBotY1, btnBotX2, btnBotY2);
            Drawing.drawBar(0, cancelRGB3, cancelRGB4, m_imgBuf, btnBotX1, btnBotY1, btnBotX2, btnBotY2);

            Drawing.fillArea(m_imgBuf, m_btnBotBorderColor2, btnBotX1, btnBotY1, btnBotX1 + 1, btnBotY2); //left
            Drawing.fillArea(m_imgBuf, m_btnBotBorderColor, btnBotX1, btnBotY1, btnBotX2, btnBotY1 + 1); //top
            Drawing.fillArea(m_imgBuf, m_btnBotBorderColor, btnBotX2 - 1, btnBotY1, btnBotX2, btnBotY2);
            Drawing.fillArea(m_imgBuf, m_btnBotBorderColor2, btnBotX1 + 1, btnBotY2 - 1, btnBotX2 - 1, btnBotY2);*/

            //RangeBar
            Drawing.drawBar(1,(m_active.get() ? m_bg1active : m_settingRange.get() ? m_bg1setting : m_bg1), m_bg2, m_imgBuf,m_pR, m_pW, x1, y1 + 1, x2, y2 - 1);
            Drawing.drawBar(m_barRGB1, m_barRGB2, m_imgBuf,m_pR, m_pW, x1, y1 + 1, x2, y2 - 1);

        }
       
    }
}
