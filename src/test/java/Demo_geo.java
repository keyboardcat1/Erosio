import com.github.keyboardcat1.erosio.Eroder;
import com.github.keyboardcat1.erosio.EroderGeometry;
import com.github.keyboardcat1.erosio.EroderResults;
import com.github.keyboardcat1.erosio.EroderSettings;
import com.github.keyboardcat1.erosio.geometries.EroderGeometryNatural;
import com.github.keyboardcat1.erosio.interpolation.Interpolator;
import com.github.keyboardcat1.erosio.interpolation.InterpolatorCPURasterizer;
import org.kynosarges.tektosyne.geometry.RectD;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Demo_geo {
    public static void main(String[] args) throws IOException {
        RectD bounds = new RectD(-256e2, -256e2, 256e2, 256e2);
        EroderSettings settings = new EroderSettings(
                p -> 0.2e-4*Math.sin(p.x/256e2)*Math.sin(p.y/512e2)+0.2e-4, p->0D,
                p -> 5.61e-7, 0.50,
                ((pointD, aDouble) -> 30.0),
                2.5e5, 50, 0
        );
        EroderGeometry eroderGeometry = new EroderGeometryNatural(EroderGeometry.RectDtoPolygon(bounds), .5e3, 2);
        EroderResults results = Eroder.erode(settings, eroderGeometry);

        System.out.printf("node count: %d\nconverged: %d\nmin->max height: %f->%f",
                eroderGeometry.nodeCount(), results.converged, results.minHeight, results.maxHeight);

        Interpolator interpolator = new InterpolatorCPURasterizer(results, 100, 0);

        double scale = 512/bounds.width();
        BufferedImage image = new BufferedImage((int)(scale*bounds.width()), (int)(scale* bounds.height()), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < (int)(scale*bounds.width()); x++) for (int y = 0; y < (int)(scale* bounds.height()); y++) {
            try {
                double value = interpolator.interpolate(x/scale+bounds.min.x, y/scale+bounds.min.y) - results.minHeight;
                int intensity = (int) (255 * value / (results.maxHeight - results.minHeight+1));
                image.setRGB(x, y, new Color(intensity, intensity, intensity).getRGB());
            } catch (IndexOutOfBoundsException e) {
                image.setRGB(x,y, new Color(128, 0,0).getRGB());
            }
        }

        String path = "out.png";
        ImageIO.write(image, "PNG", new File(path));
    }
}
