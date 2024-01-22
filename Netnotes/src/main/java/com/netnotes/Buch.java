package com.netnotes;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.math.BigInteger;

import scala.util.Try;
import scorex.util.encode.Base58;

public class Buch {

    int coverWidth;
    int coverHeight;

    double m_chance;

    public Buch(String address) {

        Try<byte[]> bytes = Base58.decode(address);

        m_addressBytes = bytes.get();

        int sqrt = (int) Math.sqrt(m_addressBytes.length);

        coverWidth = sqrt / 2;
        coverHeight = sqrt / 2;

        int len = m_addressBytes.length;
        double max = len * 256;
        double total = 0;
        for (byte b : m_addressBytes) {
            total += (double) b;
        }
        m_chance = total / max;

        randomCover();
        randomMirror();
        randomColors();
    }

    int width = coverWidth;
    int height = coverHeight;

    static Color pageColor = new Color(226, 207, 167);
    Color background, foreground;

    int[][] cover = new int[coverHeight][coverWidth];
    static double foregroundChance = 0.5;
    static double bookmarkChance = 0.3;
    static double mirrorChance = 0.7;

    private byte[] m_addressBytes;

    public void randomColors() {
        background = addressColor();
        do {
            foreground = randomColor();
        } while (similarColor(background, foreground));
    }

    // Checks if two colors are similar
    public boolean similarColor(Color a, Color b) {
        double distance = Math.pow(a.getRed() - b.getRed(), 2) + Math.pow(a.getGreen() - b.getGreen(), 2) + Math.pow(a.getBlue() - b.getBlue(), 2);
        return distance < 5000;
    }

    // Creates random array with 0s and 1s
    public void randomCover() {
        int e = 1;
        for (int i = 0; i < cover.length; i++) {
            for (int j = 0; j < cover[i].length; j++) {
                if (e + 4 < m_addressBytes.length) {
                    double a = (int) m_addressBytes[e] + (int) m_addressBytes[e + 1] + (int) m_addressBytes[e + 2] + (int) m_addressBytes[e + 3];

                    cover[i][j] = a / (256 * 4) < foregroundChance ? 1 : 0;
                    e += 4;
                } else {
                    cover[i][j] = Math.random() < foregroundChance ? 1 : 0;

                }
            }
        }
    }

    public void randomMirror() {

        double mirrorChance = m_chance;
        if (mirrorChance < mirrorChance) {
            mirrorVertically();
        }
        if (mirrorChance < mirrorChance) {
            mirrorHorizontally();
        }
    }

    public void mirrorVertically() {
        for (int i = 0; i < cover.length; i++) {
            for (int j = 0; j < cover[i].length / 2; j++) {
                cover[i][j] = cover[i][cover[i].length - 1 - j];
            }
        }
    }

    public void mirrorHorizontally() {
        for (int i = 0; i < cover.length / 2; i++) {
            for (int j = 0; j < cover[i].length; j++) {
                cover[i][j] = cover[cover.length - 1 - i][j];
            }
        }
    }

    public Color addressColor() {
        return new Color((int) (m_chance * 0x1000000));
    }

    public Color randomColor() {
        return new Color((int) (Math.random() * 0x1000000));
    }

    public BufferedImage getImage() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();

        // Cover
        for (int i = 0; i < cover.length; i++) {
            for (int j = 0; j < cover[i].length; j++) {
                int rgb;
                if (cover[i][j] == 1) {
                    rgb = foreground.getRGB();
                } else {
                    rgb = background.getRGB();
                }
                image.setRGB(2 + j, 1 + i, rgb);
            }
        }

        return image;
    }
}
