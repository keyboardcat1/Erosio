import com.github.keyboardcat1.erosio.Eroder;
import com.github.keyboardcat1.erosio.EroderGeometry;
import com.github.keyboardcat1.erosio.EroderResults;
import com.github.keyboardcat1.erosio.EroderSettings;
import com.github.keyboardcat1.erosio.geometries.EroderGeometryNatural;
import com.github.keyboardcat1.erosio.interpolation.Interpolator;
import com.github.keyboardcat1.erosio.interpolation.InterpolatorGaussianKernel;
import org.kynosarges.tektosyne.geometry.RectI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Demo {
    public static void main(String[] args) throws IOException {
        RectI bounds = new RectI(-256, -256, 256, 256);
        EroderSettings settings = new EroderSettings(
                p -> 1.0, p -> 0.0,
                p -> 2.0, 0.5,
                (p,h) -> 30.0,
                1, 10, 1E-2
        );
        EroderGeometry eroderGeometry = new EroderGeometryNatural(EroderGeometry.RectDtoPolygon(bounds.toRectD()), 2, 2);
        EroderResults results = Eroder.erode(settings, eroderGeometry);

        Interpolator interpolator = new InterpolatorGaussianKernel(results, 2.5, 1E-6);
        // Interpolator interpolator = new InterpolatorIDW(results, 2.5, 10);
        // Interpolator interpolator = new InterpolatorNN(results);
        // Interpolator interpolator = new InterpolatorKriging(results, InterpolatorKriging.Model.EXPONENTIAL, 1, 10, 10, 1);
        // Interpolator interpolator = new InterpolatorCPURasterizer(results, 1, 1); // interpolate with try-catch block! (see Demo_geo)

        BufferedImage image = new BufferedImage((int) bounds.width(), (int) bounds.height(), BufferedImage.TYPE_INT_RGB);
        for (int x = bounds.min.x; x < bounds.max.x; x++) for (int y = bounds.min.y; y < bounds.max.y; y++) {
            double value = interpolator.interpolate(x, y)  - results.minHeight;
            int intensity = (int) (255 * value / (results.maxHeight - results.minHeight));
            image.setRGB(x - bounds.min.x, y - bounds.min.y, new Color(intensity, intensity, intensity).getRGB());
        }

        String path = "out.png";
        ImageIO.write(image, "PNG", new File(path));
    }
}
