package com.netnotes;

import javafx.scene.control.MenuButton;
import javafx.scene.image.Image;

public class BufferedMenuButton extends MenuButton {

    private BufferedImageView m_imgBufView;

    public BufferedMenuButton() {
        this("/assets/menu-outline-30.png");
    }

    public BufferedMenuButton(String urlString) {
        this("", urlString, 30);
    }

    public BufferedMenuButton(String name, String urlString) {
        this(name, urlString, 30);
    }

    public BufferedMenuButton(String urlString, double imageWidth) {
        super();
        m_imgBufView = new BufferedImageView(new Image(urlString), imageWidth);
        setGraphic(m_imgBufView);

        /*setId("menuBtn");*/
        setOnMousePressed((event) -> {
            m_imgBufView.applyInvertEffect(.6);
            show();
        });
        setOnMouseReleased((event) -> {
            m_imgBufView.clearEffects();
        });

    }

    public BufferedMenuButton(String name, String urlString, double imageWidth) {
        super(name);
        m_imgBufView = new BufferedImageView(new Image(urlString), imageWidth);
        setGraphic(m_imgBufView);

        /*setId("menuBtn");*/
        setOnMousePressed((event) -> {
            m_imgBufView.applyInvertEffect(.6);
            show();
        });
        setOnMouseReleased((event) -> {
            m_imgBufView.clearEffects();
        });

    }

    public BufferedImageView getBufferedImageView() {
        return m_imgBufView;
    }

    public void setImage(Image image) {

        m_imgBufView.setDefaultImage(image);

    }
}
