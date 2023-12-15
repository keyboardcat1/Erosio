import com.github.keyboardcat1.erosio.Eroder;
import com.github.keyboardcat1.erosio.VoronoiDelaunay;
import org.kynosarges.tektosyne.geometry.RectI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Demo {
    public static void main(String[] args) throws IOException {
        RectI bounds = new RectI(-256, -256, 256, 256);
        Eroder.Settings settings = new Eroder.Settings(
                p -> 1.0, p -> 0.0,
                2.0, 0.5,
                (p,h) -> 30.0,
                1, 10, 1E-2
        );
        VoronoiDelaunay voronoiDelaunay = new VoronoiDelaunay(bounds.toRectD(), 2, 2);

        Eroder.Results results = Eroder.erode(settings, voronoiDelaunay);

        BufferedImage image = new BufferedImage((int) bounds.width(), (int) bounds.height(), BufferedImage.TYPE_INT_RGB);
        for (int x = bounds.min.x; x < bounds.max.x; x++) for (int y = bounds.min.y; y < bounds.max.y; y++) {
            double value = results.interpolateInverseDistanceWeighting(x, y, 2, 5);
            int intensity = (int) (255 * value / results.maxHeight);
            image.setRGB(x - bounds.min.x, y - bounds.min.y, new Color(intensity, intensity, intensity).getRGB());
        }

        String path = "images/IDW.png";
        ImageIO.write(image, "PNG", new File(path));
    }
}
