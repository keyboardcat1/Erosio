package com.github.keyboardcat1.erosio.interpolation;

import com.github.keyboardcat1.erosio.EroderResults;
import org.kynosarges.tektosyne.QuadTree;
import org.kynosarges.tektosyne.geometry.GeoUtils;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.RectD;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An abstract {@link Interpolator} with range utilities
 */
public abstract class SpontaneousInterpolator extends Interpolator{
    /**
     * The {@link EroderResults} to interpolate
     */
    protected final EroderResults eroderResults;

    private QuadTree<Double> quadTree;

    /**
     * The base interpolation class
     *
     * @param eroderResults The {@link EroderResults} to interpolate
     */
    public SpontaneousInterpolator(EroderResults eroderResults) {
        super(eroderResults);
        this.eroderResults = eroderResults;
    }

    /**
     * Finds all sample points lying within a radius
     *
     * @param point  a {@link PointD} indicating the center of the search radius
     * @param radius the radius to search
     * @return a {@link Map} containing all {@link PointD} lying within the radius
     */
    protected final Set<PointD> getRange(PointD point, double radius) {
        if (Objects.isNull(this.quadTree))
            this.quadTree = new QuadTree<>(RectD.circumscribe(eroderResults.eroderGeometry.boundingPolygon), eroderResults.heightMap);
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
