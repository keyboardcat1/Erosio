package com.github.keyboardcat1.erosio.interpolation;

import com.github.keyboardcat1.erosio.EroderResults;
import org.kynosarges.tektosyne.QuadTree;
import org.kynosarges.tektosyne.geometry.GeoUtils;
import org.kynosarges.tektosyne.geometry.PointD;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The base interpolation class
 */
public abstract class Interpolator {
    /**
     * The {@link EroderResults} to interpolate
     */
    public EroderResults eroderResults;

    private final QuadTree<Double> quadTree;

    /**
     * The base interpolation class
     *
     * @param eroderResults The {@link EroderResults} to interpolate
     */
    public Interpolator(EroderResults eroderResults) {
        this.quadTree = new QuadTree<>(eroderResults.eroderGeometry.bounds, eroderResults.heightMap);
        this.eroderResults = eroderResults;
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



    /**
     * Finds all sample points lying within a radius
     *
     * @param point  a {@link PointD} indicating the center of the search radius
     * @param radius the radius to search
     * @return a {@link Map} containing all {@link PointD} lying within the radius
     */
    protected final Set<PointD> getRange(PointD point, double radius) {
        return quadTree.findRange(point, radius).keySet();
    }

    /**
     *  Finds closest sample point to a given point
     *
     * @param point a {@link PointD} indicating the center of the search radius
     * @return the closest sample point to the given point
     */
    protected final PointD getClosest(PointD point) {
        List<PointD> neighbors = getRange(point, eroderResults.eroderGeometry.minDistance*1.5).stream().toList();
        return neighbors.get(GeoUtils.nearestPoint(neighbors, point));
    }
}
