package com.github.keyboardcat1.erosio.interpolation;

import com.github.keyboardcat1.erosio.EroderResults;
import org.kynosarges.tektosyne.geometry.GeoUtils;
import org.kynosarges.tektosyne.geometry.PointD;

import java.util.List;

/**
 * A class interpolating by nearest neighbor
 */
public class InterpolatorNN extends Interpolator {

    /**
     * A class interpolating by nearest neighbor
     *
     * @param eroderResults The {@link EroderResults} to interpolate
     */
    public InterpolatorNN(EroderResults eroderResults) {
        super(eroderResults);
    }

    @Override
    public double interpolate(PointD point) {
        List<PointD> neighbors = quadTree.findRange(point, eroderResults.eroderGeometry.minDistance * 1.5).keySet().stream().toList();
        var result = eroderResults.heightMap.get(neighbors.get(GeoUtils.nearestPoint(neighbors, point)));
        return Double.isNaN(result) ? 0 : result;
    }
}
