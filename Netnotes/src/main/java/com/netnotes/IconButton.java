package com.netnotes;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.TextAlignment;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;

public class IconButton extends Button {

    public class IconStyle {

        public static String ROW = "ROW";
        public static String ICON = "ICON";
    }
    public static double SMALL_PADDING = 5;
    public static double NORMAL_PADDING = 15;
    public static double NORMAL_IMAGE_WIDTH = 75;
    public static double SMALL_IMAGE_WIDTH = 30;

    public static double NORMAL_WIDTH = 90;

    public static Insets SMALL_INSETS = new Insets(SMALL_PADDING, SMALL_PADDING, SMALL_PADDING, SMALL_PADDING);
    public static Insets NORMAL_INSETS = new Insets(NORMAL_PADDING, NORMAL_PADDING, NORMAL_PADDING, NORMAL_PADDING);

    public final static String DEFAULT_CURRENT_ID = "iconBtnSelected";
    public final static String DEFAULT_ID = "iconBtn";
    public final static String ROW_DEFAULT_ID = "rowBtn";
    public final static String ROW_CURRENT_ID = "rowBtnSelected";

    private String m_defaultId = DEFAULT_ID;
    private String m_currentId = DEFAULT_CURRENT_ID;

    private String m_btnId = null;
    private Image m_icon;
    private double m_imageWidth = 75;
    private String m_name = "";
    private String m_iconStyle = IconStyle.ICON;
    private boolean m_open = false;

    private boolean m_multipleInstances = false;
    private ChangeListener<Boolean> m_focusListener;
    //private EventHandler<MouseEvent> m_mouseEventHandler;

    public IconButton() {
        super();

        setId("iconBtn");
        setFont(App.txtFont);
        enableActions();
    }

    public IconButton(Image icon) {
        this();
        setIcon(icon);
    }

    public IconButton(Image icon, String name) {
        this();
        setIcon(icon);
        setName(name);

    }

    public Image getAppIcon(){
        return m_icon;
    }

    public String getButtonId() {
        return m_btnId;
    }

    public void setButtonId(String buttonId) {
        m_btnId = buttonId;
    }

    public IconButton(Image image, String name, String iconStyle) {
        this(image, name);

        if (iconStyle.equals(IconStyle.ROW)) {
            m_iconStyle = iconStyle;
            m_name = name;
        
            setContentDisplay(ContentDisplay.LEFT);
            setAlignment(Pos.CENTER_LEFT);
            setText(m_name);
            setImageWidth(30);
            setId("rowBtn");
            setGraphicTextGap(10);
            m_defaultId = ROW_DEFAULT_ID;
            m_currentId = ROW_CURRENT_ID;
        } else {
            m_defaultId = DEFAULT_ID;
            m_currentId = DEFAULT_CURRENT_ID;
            setImageWidth(75);
            setContentDisplay(ContentDisplay.TOP);
            setTextAlignment(TextAlignment.CENTER);
        }

    }

    public void setIconStyle(String iconStyle) {
        m_iconStyle = iconStyle;
        if (iconStyle.equals(IconStyle.ROW)) {
            setContentDisplay(ContentDisplay.LEFT);
            setAlignment(Pos.CENTER_LEFT);
            setText(m_name);
            setImageWidth(30);
            setId("rowBtn");
            m_defaultId = ROW_DEFAULT_ID;
            m_currentId = ROW_CURRENT_ID;
        } else {

            setContentDisplay(ContentDisplay.TOP);
            setTextAlignment(TextAlignment.CENTER);
            setId("iconBtn");
            m_defaultId = DEFAULT_ID;
            m_currentId = DEFAULT_CURRENT_ID;
        }
    }

    private void startFocusCurrent() {
        m_focusListener = (obs, oldValue, newValue) -> setCurrent(newValue.booleanValue());
        focusedProperty().addListener(m_focusListener);
    }

    private void stopFocusCurrent() {
        focusedProperty().removeListener(m_focusListener);
    }

    public void enableActions() {
        setOnMouseClicked((event) -> onClick(event));
        startFocusCurrent();
    }

    public void disableActions() {
        setOnMouseClicked(null);
        stopFocusCurrent();
    }

    public void onClick(MouseEvent e) {
        if (!isFocused()) {
            Platform.runLater(() -> requestFocus());
        }
        if (e.getClickCount() == 2) {

            open();

        }
    }

    public void open() {
        m_open = true;
    }

    public void close() {
        m_open = false;
    }

    public boolean isOpen() {
        return m_open;
    }

    public void setOpen(boolean open){
        m_open = open;
    }

    public boolean getMultipleInstances() {
        return m_multipleInstances;
    }

    public void setMultipleInstances(boolean allowMultipleInstances) {
        m_multipleInstances = allowMultipleInstances;
    }

    public IconButton(String text) {
        this();
        setName(text);
    }

    public String getIconStyle() {
        return m_iconStyle;
    }

    public String getName() {
        return m_name;
    }
    
    public void setName(String name) {

        m_name = name;
        name = name.replace("\n", " ");
        java.awt.Font font = new java.awt.Font(getFont().getFamily(), java.awt.Font.PLAIN, (int) getFont().getSize());

        FontMetrics metrics = getFontMetrics(font);

        int stringWidth = metrics.stringWidth(name);
        double imageWidth = getMaxWidth() > 75 ? getMaxWidth() : 75; 
        if (stringWidth > imageWidth) {
            int indexOfSpace = name.indexOf(" ");

            if (indexOfSpace > 0) {
                String firstWord = name.substring(0, indexOfSpace);

                if (metrics.stringWidth(firstWord) > imageWidth) {
                    setText(truncateName(name, metrics));
                } else {

                    String text = firstWord + "\n";
                    String secondWord = name.substring(indexOfSpace + 1, name.length());

                    if (metrics.stringWidth(secondWord) > imageWidth) {
                        secondWord = truncateName(secondWord, metrics);
                    }
                    text = text + secondWord;
                    setText(text);
                }
            } else {

                setText(truncateName(name, metrics));
            }

        } else {
            setText(name);
        }
    }

    public String getTextSpaces() {

        String name = getName().replace("\n", " ");
        java.awt.Font font = new java.awt.Font(getFont().getFamily(), java.awt.Font.PLAIN, (int) getFont().getSize());

        FontMetrics metrics = getFontMetrics(font);

        int stringWidth = metrics.stringWidth(name);
        double imageWidth = 75;
        if (stringWidth > imageWidth) {
            int indexOfSpace = name.indexOf(" ");

            if (indexOfSpace > 0) {
                String firstWord = name.substring(0, indexOfSpace);

                if (metrics.stringWidth(firstWord) > imageWidth) {
                    setText(truncateName(name, metrics));
                } else {

                    String text = firstWord + "\n";
                    String secondWord = name.substring(indexOfSpace + 1, name.length());

                    if (metrics.stringWidth(secondWord) > imageWidth) {
                        secondWord = truncateName(secondWord, metrics);
                    }
                    text = text + secondWord;
                    return (text);
                }
            } else {

                return (truncateName(name, metrics));
            }

        } else {
            return (name);
        }
        return name;
    }

    public String truncateName(String name, FontMetrics metrics) {
        double imageWidth = getImageWidth();
        String truncatedString = name.substring(0, 5) + "..";
        if (name.length() > 3) {
            int i = name.length() - 3;
            truncatedString = name.substring(0, i) + "..";

            while (metrics.stringWidth(truncatedString) > imageWidth && i > 1) {
                i = i - 1;
                truncatedString = name.substring(0, i) + "..";

            }
        }
        return truncatedString;
    }

    public FontMetrics getFontMetrics(java.awt.Font font) {

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);
        return g2d.getFontMetrics();

    }

    public IconButton getButton() {
        return this;
    }

    private boolean m_current = false;

    public boolean isCurrent(){
        return m_current;
    }

    public void setCurrent(boolean value, String... idString) {
        m_current = value;
        if (idString != null && idString.length > 0) {
            m_defaultId = idString[1];

            if (idString.length > 1) {
                m_currentId = idString[0];
            }
        }

        if (value) {

            setId(m_currentId);
        } else {
            setId(m_defaultId);
        }
    }

    public static ImageView getIconView(Image image, double imageWidth) {
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(imageWidth);


            /*
        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setBrightness(-0.5);

        imageView.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {

            imageView.setEffect(colorAdjust);

        });
        imageView.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            imageView.setEffect(null);
        }); */
            return imageView;
        } else {
            return null;
        }
    }

    public Image getIcon() {
        return m_icon;
    }

    public void setIcon(Image icon) {
        m_icon = icon;
        if(icon != null){
            setGraphic(getIconView(icon, m_imageWidth));
        }else{
            setGraphic(null);
        }
    }

    public double getImageWidth() {
        return m_imageWidth;
    }

    public void setImageWidth(double imageWidth) {
        m_imageWidth = imageWidth;
        if(m_icon != null){
            setGraphic(getIconView(m_icon, m_imageWidth));
        }
    }

    

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return m_name != null ? m_name : "";
    }
}
