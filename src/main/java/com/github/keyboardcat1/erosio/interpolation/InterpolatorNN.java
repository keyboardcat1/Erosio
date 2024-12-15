package com.github.keyboardcat1.erosio.interpolation;

import com.github.keyboardcat1.erosio.EroderResults;
import org.kynosarges.tektosyne.geometry.PointD;

/**
 * Interpolation by nearest neighbor
 */
public class InterpolatorNN extends SpontaneousInterpolator {

    /**
     * Interpolation by nearest neighbor
     *
     * @param eroderResults The {@link EroderResults} to interpolate
     */
    public InterpolatorNN(EroderResults eroderResults) {
        super(eroderResults);
    }

    @Override
    public double interpolate(PointD point) {
        PointD closest = getClosest(point);
        var result = eroderResults.heightMap.get(closest);
        return Double.isNaN(result) ? 0 : result;
    }
}
