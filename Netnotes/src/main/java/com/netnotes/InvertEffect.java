package com.netnotes;

import java.awt.image.BufferedImage;

import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;


public class InvertEffect extends Effects {

   // private static File logFile = new File("InvertEffect-log.txt");
    public static String NAME = "INVERT";

    private double m_amount = 1.0;

    public InvertEffect(double amount) {
        super(NAME);
        m_amount = amount;
    }

    public InvertEffect(String id, double amount) {
        super(id, NAME);
        m_amount = amount;
        
    }

    @Override
    public void applyEffect(WritableImage img) {
        invertRGB(img, m_amount);
    }

    public static void invertRGB(WritableImage img, double amount) {
        PixelReader pR = img.getPixelReader();
        PixelWriter pW = img.getPixelWriter();
        amount = amount > 1 ? 1 : amount < - 1 ? -1 : amount;

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgba = pR.getArgb(x, y);

                int a = (rgba >> 24) & 0xff;
                int r = (rgba >> 16) & 0xff;
                int g = (rgba >> 8) & 0xff;
                int b = rgba & 0xff;

                int inv = (int) (0xff * amount);

                r = Math.abs(inv - r);
                g = Math.abs(inv - g);
                b = Math.abs(inv - b);

                int p = (a << 24) | (r << 16) | (g << 8) | b;

                pW.setArgb(x, y, p);
            }
        }
    }

    public static void invertRGB(BufferedImage img, double amount) {

        amount = amount > 1 ? 1 : amount < - 1 ? -1 : amount;

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgba = img.getRGB(x, y);

                int a = (rgba >> 24) & 0xff;
                int r = (rgba >> 16) & 0xff;
                int g = (rgba >> 8) & 0xff;
                int b = rgba & 0xff;

                int inv = (int) (0xff * amount);

                r = Math.abs(inv - r);
                g = Math.abs(inv - g);
                b = Math.abs(inv - b);

                int p = (a << 24) | (r << 16) | (g << 8) | b;

                img.setRGB(x, y, p);
            }
        }
    }
}
