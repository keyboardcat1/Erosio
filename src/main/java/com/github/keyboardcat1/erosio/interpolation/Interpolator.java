package com.github.keyboardcat1.erosio.interpolation;

import com.github.keyboardcat1.erosio.EroderResults;
import org.kynosarges.tektosyne.geometry.PointD;

/**
 * The base interpolation class
 */
public abstract class Interpolator {

    /**
     * The base interpolation class
     *
     * @param eroderResults The {@link EroderResults} to interpolate
     */
    public Interpolator(EroderResults eroderResults) {
    }

    /**
     * Interpolates the height of a point
     *
     * @param x The X coordinate of the point
     * @param y The Y coordinate of that point
     * @return The interpolated height at the point
     */
    public double interpolate(double x, double y) {
        return interpolate(new PointD(x, y));
    }

    /**
     * Interpolates the height of a point by nearest neighbor
     *
     * @param point The point to interpolate at
     * @return The interpolated height at the point
     */
    public abstract double interpolate(PointD point);
}
