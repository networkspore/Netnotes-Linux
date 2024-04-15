package com.netnotes;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

import java.util.ArrayList;

public class BufferedImageView extends ImageView {
    private Image m_defaultImg = null;
    private WritableImage m_img;
    private ArrayList<Effects> m_effects = new ArrayList<Effects>();

    public BufferedImageView() {
        super();
        m_img = null;
        setPreserveRatio(true);
    } 
    
    public BufferedImageView(double fitWidth){
        this();
        setPreserveRatio(true);
        setFitWidth(fitWidth);
    }

    public BufferedImageView(Image image, double imageWidth) {
        super();
       
        setDefaultImage(image, imageWidth);



    }



    public BufferedImageView(Image image, boolean fitWidth) {
        super();
        setDefaultImage(image);

        setPreserveRatio(true);
        if (fitWidth) {
            setFitWidth(image.getWidth());
        }

    }

    public BufferedImageView(Image image) {
        super();
        setDefaultImage(image);
    }

    public void setDefaultImage(Image image) {
        if(image != null){
            m_defaultImg = image;
            m_img = new WritableImage((int) image.getWidth(),(int) image.getHeight());
          
            Drawing.drawImageExact(m_img, image, 0, 0, false);
            updateImage();
            setImage(m_img);
        }else{
            m_img = null;
            m_defaultImg = null;
            setImage(null);
        }
       // setImage(m_img);
        
    }

    public void setDefaultImage(Image img, double fitWidth) {
        setDefaultImage(img);
     
    
        setFitWidth(fitWidth);
        setPreserveRatio(true);
    }

    public Effects getEffect(String id) {
        if (id != null && m_effects.size() > 0) {
            for (int i = 0; i < m_effects.size(); i++) {
                Effects effect = m_effects.get(i);

                if (effect.getId().equals(id)) {
                    return effect;
                }
            }
        }
        return null;
    }

    public Effects getFirstNameEffect(String name) {
        if (m_effects.size() == 0) {
            return null;
        }

        for (int i = 0; i < m_effects.size(); i++) {
            Effects effect = m_effects.get(i);
            if (effect.getName().equals(name)) {
                return effect;
            }

        }

        return null;
    }

    public void applyInvertEffect(double amount) {

        m_effects.add(new InvertEffect(amount));
        updateImage();

    }

    public void applyInvertEffect(String id, double amount) {
        if (getEffect(id) == null) {
            m_effects.add(new InvertEffect(id, amount));
            updateImage();
        }
    }

    public void removeEffect(String id) {
        if (id != null && m_effects.size() > 0) {
            for (int i = 0; i < m_effects.size(); i++) {
                if (m_effects.get(i).getId().equals(id)) {
                    m_effects.remove(i);
                    updateImage();
                }
            }
        }
    }

    public void addEffect(Effects effect) {
        m_effects.add(effect);
    }

    public void applyEffect(Effects effect) {
        m_effects.add(effect);
        updateImage();
    }

    public void addEffect(int index, Effects effect, boolean update) {
        m_effects.add(effect);
        updateImage();
    }

    public void clearEffects() {
        m_effects.clear();
        updateImage();
    }

    public void updateImage() {
      //  if(m_img == null && m_defaultImg != null){
          //  setDefaultImage(m_defaultImg);
       // }
        if (m_img != null && m_defaultImg != null) {
        
            //Drawing.clearImage(m_img);
            Drawing.drawImageExact(m_img, m_defaultImg, 0, 0, false);
            
           
            if (m_effects.size() > 0) {
           

                for (int i = 0; i < m_effects.size(); i++) {
                    Effects effect = m_effects.get(i);
                    effect.applyEffect(m_img);
                }

            }
        } 
    }

    public Image getBaseImage() {
        return m_img;
    }

    public void setBufferedImage(WritableImage imgBuf) {
        m_img = imgBuf;
        if (m_img != null) {
            if (m_effects.size() > 0) {

                for (int i = 0; i < m_effects.size(); i++) {
                    Effects effect = m_effects.get(i);
                    effect.applyEffect(imgBuf);
                }


            }
            setImage(m_img);
        } else {
            setImage(null);
        }
    }
}
