package com.github.keyboardcat1.erosio;

import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.RectD;
import org.kynosarges.tektosyne.geometry.Voronoi;
import org.kynosarges.tektosyne.geometry.VoronoiResults;
import org.kynosarges.tektosyne.subdivision.Subdivision;

/**
 * A wrapper for {@link VoronoiResults} and the parameters used to generate it
 */
public final class VoronoiDelaunay {
    public final RectD bounds;
    public final VoronoiResults voronoiResults;
    public final Subdivision delaunaySubdivision;
    public final double inverseSampleDensity;
    public final long seed;

    /**
     * @param bounds               The bounding coordinated of a rectangular region
     * @param inverseSampleDensity The minimum distance between two nodes in the stream graph
     * @param seed                 A seed randomizing the sample points
     */
    public VoronoiDelaunay(RectD bounds, double inverseSampleDensity, long seed) {
        PointD[] points = PoissonDiskSampler.sample(bounds, inverseSampleDensity, seed).toArray(new PointD[0]);
        this.bounds = bounds;
        voronoiResults = Voronoi.findAll(points, bounds);
        delaunaySubdivision = voronoiResults.toDelaunaySubdivision(true);
        this.inverseSampleDensity = inverseSampleDensity;
        this.seed = seed;
    }
}
