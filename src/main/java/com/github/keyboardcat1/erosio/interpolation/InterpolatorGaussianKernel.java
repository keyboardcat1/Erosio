package com.github.keyboardcat1.erosio.interpolation;

import com.github.keyboardcat1.erosio.EroderResults;
import org.kynosarges.tektosyne.geometry.PointD;

import java.util.Map;
import java.util.Set;

/**
 * A class interpolating with gaussian kernels
 */
public class InterpolatorGaussianKernel extends Interpolator {
    private static final double SQRT2PI = Math.sqrt(2*Math.PI);

    public final double stddevInverseCoefficient;
    public final double normalizedError;

    private final double d_epsilon;

    /**
     * A class interpolating with gaussian kernels
     *
     * @param stddevInverseCoefficient gaussian distribution inverse standard deviation
     * @param normalizedError error margin proportion (small)
     * @param eroderResults The {@link EroderResults} to interpolate
     */
    public InterpolatorGaussianKernel(EroderResults eroderResults, double stddevInverseCoefficient, double normalizedError) {
        super(eroderResults);
        this.stddevInverseCoefficient = stddevInverseCoefficient;
        this.normalizedError = normalizedError;

        double total = 0;
        int N = 0;
        for (Map.Entry<PointD, Set<PointD>> entry : eroderResults.eroderGeometry.graph.entrySet()) {
            for (PointD neighbor : entry.getValue()) {
                N += 1;
                total += entry.getKey().subtract(neighbor).length();
            }
        }
        double average = total/N;

        this.d_epsilon = average / stddevInverseCoefficient * Math.sqrt(2*Math.abs(Math.log(normalizedError)));
    }

    @Override
    public double interpolate(PointD point) {
        double numerator = 0.0D;
        double denominator = 0.0D;
        for (PointD node : getRange(point, d_epsilon)) {
            double height = eroderResults.heightMap.get(node);
            double distance = point.subtract(node).length();
            double weight = normalDist(distance, 0, Math.pow((distance/stddevInverseCoefficient), 2));
            numerator += height * weight;
            denominator += weight;
        }
        return numerator / denominator;
    }

    private double normalDist(double x, double m, double v) {
        return (1/(v*SQRT2PI)) * Math.exp(-Math.pow(x-m,2)/(2*v));
    }
}
