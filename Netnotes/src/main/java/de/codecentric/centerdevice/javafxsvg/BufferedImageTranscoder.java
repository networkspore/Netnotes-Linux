package de.codecentric.centerdevice.javafxsvg;

import java.awt.image.BufferedImage;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

/**
 * Code based on: https://gist.github.com/ComFreek/b0684ac324c815232556
 * 
 * Many thanks to bb-generation for sharing this code!
 * 
 * License unfortunately unknown, but using this code is probably categorized as
 * "fair use" (because the code is in my opinion too simple to be licensed)
 * 
 * @author bb-generation
 * @see <a href="https://web.archive.org/web/20131215231214/http://bbgen.net/blog/2011/06/java-svg-to-bufferedimage/">java-
 *      svg-to-bufferedimage (as archived by archive.org)</a>
 */
public class BufferedImageTranscoder extends ImageTranscoder {

	private BufferedImage img = null;
	private final int type;

	public BufferedImageTranscoder(int type) {
		this.type = type;
	}

	@Override
	protected void setImageSize(float width, float height) {
		if (width > 0 && height > 0) {
			super.setImageSize(width, height);
		}
	}

	@Override
	public BufferedImage createImage(int width, int height) {
		BufferedImage bi = new BufferedImage(width, height, type);
		return bi;
	}

	@Override
	public void writeImage(BufferedImage img, TranscoderOutput to)
			throws TranscoderException {
		this.img = img;
	}

	public BufferedImage getBufferedImage() {
		return img;
	}
}
