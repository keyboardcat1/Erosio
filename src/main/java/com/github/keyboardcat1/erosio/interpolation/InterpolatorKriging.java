package com.github.keyboardcat1.erosio.interpolation;

import com.github.keyboardcat1.erosio.EroderResults;
import org.ejml.simple.SimpleMatrix;
import org.kynosarges.tektosyne.geometry.PointD;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A class interpolating with Kriging interpolation
 */
public class InterpolatorKriging extends Interpolator {
    /**
     * The variogram {@link Model} used to calculate variances
     */
    public final Model model;
    /**
     * The nugget parameter in the variogram formula
     */
    public final double nugget;
    /**
     * The sill parameter in the variogram formula
     */
    public final double sill;
    /**
     * The range parameter in the variogram formula
     */
    public final double range;
    /**
     * The <i>a<i/> coefficient in the exponential and gaussian variogram formulas
     */
    public final double a;


    /**
     * A class interpolating with Kriging interpolation
     *
     * @param eroderResults The {@link EroderResults} to interpolate
     * @param model         The variogram {@link Model} used to calculate variances
     * @param nugget        The nugget parameter in the variogram formula
     * @param sill          The sill parameter in the variogram formula
     * @param range         The range parameter in the variogram formula
     * @param a             The <i>a<i/> coefficient in the exponential and gaussian variogram formulas
     */
    public InterpolatorKriging(EroderResults eroderResults, Model model, double nugget, double sill, double range, double a) {
        super(eroderResults);
        this.model = model;
        this.nugget = nugget;
        this.sill = sill;
        this.range = range;
        this.a = a;
    }

    @Override
    public double interpolate(PointD point) {
        List<Map.Entry<PointD, Double>> neighbors = quadTree.findRange(point, range).entrySet().stream().toList();

        SimpleMatrix matrix = new SimpleMatrix(neighbors.size() + 1, neighbors.size() + 1);
        double[] edge = new double[neighbors.size() + 1];
        Arrays.fill(edge, 1);
        edge[edge.length - 1] = 0;
        matrix.setRow(neighbors.size(), 0, edge);
        matrix.setColumn(neighbors.size(), 0, edge);
        for (int i = 0; i <= neighbors.size(); i++)
            for (int j = 0; j <= neighbors.size(); j++) {
                double lagDistance = i < neighbors.size() && j < neighbors.size() ?
                        neighbors.get(i).getKey().subtract(neighbors.get(j).getKey()).length() :
                        matrix.get(i, j);
                matrix.set(i, j, semiVariance(lagDistance));
            }

        SimpleMatrix vector = new SimpleMatrix(neighbors.size() + 1, 1);
        for (int i = 0; i < neighbors.size(); i++)
            vector.set(i, 0, neighbors.get(i).getKey().subtract(point).length());
        vector.set(neighbors.size(), 0, 1);

        SimpleMatrix weights = matrix.solve(vector);

        double out = 0.0D;
        for (int i = 0; i < neighbors.size(); i++)
            out += weights.get(i, 0) * neighbors.get(i).getValue();

        if (out < eroderResults.minHeight)
            return eroderResults.minHeight;
        else return Math.min(out, eroderResults.maxHeight);
    }


    private double semiVariance(double h) {
        switch (model) {
            case EXPONENTIAL -> {
                return (sill - nugget)
                        * (1 - Math.exp(-h / (range * a)))
                        + nugget * (h > 0 ? 1 : 0);
            }
            case SPHERICAL -> {
                return (sill - nugget)
                        * (((3 * h) / (2 * range) - (h * h * h) / (2 * range * range * range)) * (h > 0 && h < range ? 1 : 0) + (h >= range ? 1 : 0))
                        + nugget * (h > 0 ? 1 : 0);
            }
            case GAUSSIAN -> {
                return (sill - nugget)
                        * (1 - Math.exp(-(h * h / (range * range * a))))
                        + nugget * (h > 0 ? 1 : 0);
            }
        }

        return Double.NaN;
    }

    /**
     * The variogram models that can be used to calculate variances
     *
     * @see <a href=https://en.wikipedia.org/wiki/Variogram#Variogram_models>Variogram models<a/>
     */
    public enum Model {
        EXPONENTIAL,
        SPHERICAL,
        GAUSSIAN
    }
}
