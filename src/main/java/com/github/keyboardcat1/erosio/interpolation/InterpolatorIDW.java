package com.github.keyboardcat1.erosio.interpolation;

import com.github.keyboardcat1.erosio.EroderResults;
import org.kynosarges.tektosyne.geometry.PointD;

import java.util.concurrent.atomic.AtomicReference;

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
        AtomicReference<Double> numerator = new AtomicReference<>(0.0D);
        AtomicReference<Double> denominator = new AtomicReference<>(0.0D);
        quadTree.findRange(point, radius).forEach((node, height) -> {
            double W = Math.pow(point.subtract(node).lengthSquared(), exponent * -0.5D);
            numerator.updateAndGet(v -> v + eroderResults.heightMap.get(node) * W);
            denominator.updateAndGet(v -> v + W);
        });
        return numerator.get() / denominator.get();
    }
}
