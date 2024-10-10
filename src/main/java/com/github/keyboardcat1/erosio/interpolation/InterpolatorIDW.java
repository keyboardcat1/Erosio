package com.github.keyboardcat1.erosio.interpolation;

import com.github.keyboardcat1.erosio.EroderResults;
import org.kynosarges.tektosyne.geometry.PointD;

import java.util.Map;

/**
 * A class interpolating with Inverse Distance Weighting
 */
public class InterpolatorIDW extends Interpolator {
    /**
     * The exponent in the IDW weight
     */
    public final double exponent;
    /**
     * The interpolation radius to sample points from
     */
    public final double radius;

    /**
     * A class interpolating with Inverse Distance Weighting
     *
     * @param eroderResults The {@link EroderResults} to interpolate
     * @param exponent      The exponent in the IDW weight
     * @param radius        The interpolation radius to sample points from
     */
    public InterpolatorIDW(EroderResults eroderResults, double exponent, double radius) {
        super(eroderResults);
        this.exponent = exponent;
        this.radius = radius;
    }

    @Override
    public double interpolate(PointD point) {
        double numerator = 0.0D;
        double denominator = 0.0D;
        for (Map.Entry<PointD, Double> entry : quadTree.findRange(point, radius).entrySet()) {
            PointD node = entry.getKey();
            double height = entry.getValue();
            double weight = Math.pow(point.subtract(node).lengthSquared(), exponent * -0.5D);
            numerator += height * weight;
            denominator += weight;
        }
        return numerator / denominator;
    }
}
