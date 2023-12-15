package com.github.keyboardcat1.erosio;

import org.kynosarges.tektosyne.geometry.*;
import org.kynosarges.tektosyne.subdivision.Subdivision;

/**
 * A wrapper for {@link VoronoiResults} and the parameters used to generate it
 */
public final class VoronoiDelaunay {
    /**
     * The bounding coordinates of a polygonal region
     */
    public final PointD[] boundingPolygon;
    /**
     * The computed Voronoi tesselation
     */
    public final VoronoiResults voronoiResults;
    /**
     * The computed Delaunay triangulation
     */
    public final Subdivision delaunaySubdivision;
    /**
     * The minimum distance between two nodes in the stream graph
     */
    public final double inverseSampleDensity;
    /**
     * The seed randomizing the sample points
     */
    public final long seed;

    /**
     * @param bounds               The bounding coordinates of a rectangular region
     * @param inverseSampleDensity The minimum distance between two nodes in the stream graph
     * @param seed                 A seed randomizing the sample points
     */
    public VoronoiDelaunay(RectD bounds, double inverseSampleDensity, long seed) {
        this(new PointD[]{
                bounds.min, bounds.min.add(new PointD(bounds.width(), 0)),
                bounds.max, bounds.max.subtract(new PointD(bounds.width(), 0)),
        }, inverseSampleDensity, seed);
    }

    /**
     * @param boundingPolygon      The bounding coordinates of a polygonal region
     * @param inverseSampleDensity The minimum distance between two nodes in the stream graph
     * @param seed                 A seed randomizing the sample points
     */
    public VoronoiDelaunay(PointD[] boundingPolygon, double inverseSampleDensity, long seed) {
        RectD bounds = RectD.circumscribe(boundingPolygon);
        PointD[] points = PoissonDiskSampler.sample(bounds, inverseSampleDensity, seed)
                .stream().filter(p -> GeoUtils.pointInPolygon(p, boundingPolygon) != PolygonLocation.OUTSIDE)
                .toList().toArray(new PointD[0]);
        this.boundingPolygon = boundingPolygon;
        voronoiResults = Voronoi.findAll(points, bounds);
        delaunaySubdivision = voronoiResults.toDelaunaySubdivision(true);
        this.inverseSampleDensity = inverseSampleDensity;
        this.seed = seed;
    }
}
