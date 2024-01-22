package com.netnotes;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.embed.swing.SwingFXUtils;

import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class RangeBar extends BufferedImageView {

    public static int DEFAULT_BUTTON_HEIGHT = 18;

    private SimpleDoubleProperty m_height;
    private SimpleDoubleProperty m_width;

    private double m_maxTop = 1;
    private SimpleDoubleProperty m_topVvalue = new SimpleDoubleProperty(1);
    private SimpleDoubleProperty m_bottomVvalue = new SimpleDoubleProperty(0);
    private SimpleBooleanProperty m_settingRange = new SimpleBooleanProperty(false);
    private SimpleBooleanProperty m_active = new SimpleBooleanProperty(false);

    private double m_minBot = 0;

    private int m_btnHeight = DEFAULT_BUTTON_HEIGHT;

    private int m_shadingLightRGB = 0xffffffff;
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
    private int m_btnBotBorderColor2 = 0x70000000;

    private Image m_collapseImage = new Image("/assets/collapse-20.png");

    private int m_bg1 = 0x80ffffff;
    private int m_bg2 = 0x50000000;

    private int m_barRGB1 = 0x55333333;
    private int m_barRGB2 = 0x50ffffff;

    private int m_currentSelectionIndex = -1;

    public RangeBar(SimpleDoubleProperty width, SimpleDoubleProperty height) {
        super(SwingFXUtils.toFXImage(getBgImage(width, height), null));

        m_width = width;
        m_height = height;

        setDefaultImage(getBgImage(m_width.get() < 1 ? 1 : m_width.get(), m_height.get() < 1 ? 1 : m_height.get()));

        m_height.addListener((obs, oldVal, newVal) -> {
            setDefaultImage(getBgImage(m_width.get() < 1 ? 1 : m_width.get(), m_height.get() < 1 ? 1 : m_height.get()));
        });
        m_width.addListener((obs, oldVal, newVal) -> {
            setDefaultImage(getBgImage(m_width.get() < 1 ? 1 : m_width.get(), m_width.get() < 1 ? 1 : m_width.get()));

        });
        setPreserveRatio(false);
        setOnMousePressed((mouseEvent) -> onMousePressed(mouseEvent));
        setOnMouseDragged(mouseEvent -> onMouseMoved(mouseEvent));
        setOnMouseReleased((mouseEvent) -> onMouseReleased(mouseEvent));

        setId("rangeBar");
    }

    public SimpleDoubleProperty rangeBarWidthProperty() {
        return m_width;
    }

    public SimpleDoubleProperty rangeBarHeightProperty() {
        return m_height;
    }

    public static Image getBgImage(double width, double height) {
        return getBgImage((int) Math.ceil(width), (int) Math.ceil(height));
    }

    public static Image getBgImage(int width, int height) {
        BufferedImage barImageBuf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Drawing.fillArea(barImageBuf, 0x00010101, 0, 0, width, height);
        return SwingFXUtils.toFXImage(barImageBuf, null);
    }

    public static BufferedImage getBgImage(SimpleDoubleProperty w, SimpleDoubleProperty h) {
        int width = (int) Math.ceil(w.get());
        int height = (int) Math.ceil(h.get());

        BufferedImage barImageBuf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        return barImageBuf;
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

            setRangeByMouse(mouseEvent, getHeight());

            updateImage();

        }
    }
    private boolean m_isMouseEntered = false;
    public void onMouseEntered(MouseEvent mouseEvent){
        m_isMouseEntered = true;
        
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

    public static int selectMouseButton(MouseEvent mouseEvent, double height, double topVvalue, double botVvalue, double btnHeight) {

        double mouseY = mouseEvent.getY();

        if (mouseY <= btnHeight) {

            return 0;

        } else {
            if (mouseY >= height - btnHeight) {

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
        return Math.ceil(getBaseImage().getHeight());
    }

    public void toggleSettingRange() {

        m_settingRange.set(!m_settingRange.get());
        m_currentSelectionIndex = -1;
        updateImage();

    }

    private void onMousePressed(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {

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

    @Override
    public void updateImage() {

        BufferedImage imgBuf = getBgImage(m_width, m_height);
        int height = imgBuf.getHeight();
        int width = imgBuf.getWidth();

        int btnTopX1 = (width / 2) - 2;
        int btnTopY1 = 0;
        int btnTopX2 = (width / 2) + 3;
        int btnTopY2 = m_btnHeight;

        int btnBotX1 = (width / 2) - 2;
        int btnBotY1 = height - m_btnHeight;
        int btnBotX2 = (width / 2) + 3;
        int btnBotY2 = height;

        // double imgScale = getScrollScale(height);
        int x1 = (width / 2) - 2;
        int y1 = getY1(height);

        int x2 = (width / 2) + 2;
        int y2 = getY2(height);
        boolean settingRange = m_settingRange.get();

        if (!settingRange) {
            Drawing.fillArea(imgBuf, 0x00000000, 0, 0, width, height);

            Drawing.drawBar(1, m_bg1, m_bg2, imgBuf, x1, y1, x2, y2);
            Drawing.drawBar(m_barRGB1, m_barRGB2, imgBuf, x1, y1, x2, y2);

            if (m_topVvalue.get() == 1 && m_bottomVvalue.get() == 0) {

                Drawing.fillArea(imgBuf, 0x50000000, x1, (height / 2) - 10, x2, (height / 2) + 10, false);

                Graphics2D g2d = imgBuf.createGraphics();

                BufferedImage moveableImage = SwingFXUtils.fromFXImage(m_collapseImage, null);
                int mvImgWidth = width - 2;
                
                g2d.drawImage(moveableImage, ((width/2) - (mvImgWidth/2)) + 1 , (height / 2) - (26 / 2), mvImgWidth, 26, null);

            }
        } else {
            Drawing.fillArea(imgBuf, 0x20ffffff, 0, 0, width, height);
            Drawing.fillArea(imgBuf, 0xff000000, (width / 2) - 1, m_btnHeight + 1, (width / 2) + 1, height - (m_btnHeight + 1));
            //OkBtn
            boolean okBtnDown = m_currentSelectionIndex == 0;
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

            Drawing.drawBar(1, okShadingRGB1, okShadingRGB2, imgBuf, btnTopX1, btnTopY1, btnTopX2, btnTopY2);
            Drawing.drawBar(1, okRGB1, okRGB2, imgBuf, btnTopX1, btnTopY1, btnTopX2, btnTopY2);
            Drawing.drawBar(0, okRGB3, okRGB4, imgBuf, btnTopX1, btnTopY1, btnTopX2, btnTopY2);

            Drawing.fillArea(imgBuf, m_btnTopBorderColor2, btnTopX1, btnTopY1, btnTopX1 + 1, btnTopY2);
            Drawing.fillArea(imgBuf, m_btnTopBorderColor2, btnTopX1, btnTopY1, btnTopX2, btnTopY1 + 1);
            Drawing.fillArea(imgBuf, m_btnTopBorderColor, btnTopX2 - 1, btnTopY1, btnTopX2, btnTopY2);
            Drawing.fillArea(imgBuf, m_btnTopBorderColor, btnTopX1 + 1, btnTopY2 - 1, btnTopX2 - 1, btnTopY2);

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

            Drawing.drawBar(1, cancelShadingRGB1, cancelShadingRGB2, imgBuf, btnBotX1, btnBotY1, btnBotX2, btnBotY2);
            Drawing.drawBar(1, cancelRGB1, cancelRGB2, imgBuf, btnBotX1, btnBotY1, btnBotX2, btnBotY2);
            Drawing.drawBar(0, cancelRGB3, cancelRGB4, imgBuf, btnBotX1, btnBotY1, btnBotX2, btnBotY2);

            Drawing.fillArea(imgBuf, m_btnBotBorderColor2, btnBotX1, btnBotY1, btnBotX1 + 1, btnBotY2); //left
            Drawing.fillArea(imgBuf, m_btnBotBorderColor, btnBotX1, btnBotY1, btnBotX2, btnBotY1 + 1); //top
            Drawing.fillArea(imgBuf, m_btnBotBorderColor, btnBotX2 - 1, btnBotY1, btnBotX2, btnBotY2);
            Drawing.fillArea(imgBuf, m_btnBotBorderColor2, btnBotX1 + 1, btnBotY2 - 1, btnBotX2 - 1, btnBotY2);

            //RangeBar
            Drawing.drawBar(1, m_bg1, m_bg2, imgBuf, x1, y1 + 1, x2, y2 - 1);
            Drawing.drawBar(m_barRGB1, m_barRGB2, imgBuf, x1, y1 + 1, x2, y2 - 1);

        }

        super.updateImage(imgBuf);
    }
}
