package com.github.keyboardcat1.erosio;

import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.RectD;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The geometry underlying a stream graph
 */
public abstract class EroderGeometry {
    /**
     * The bounding coordinates of a polygonal region
     */
    public final PointD[] boundingPolygon;
    /**
     * The minimum distance between two nodes, setting the resolution
     */
    public final double minDistance;
    /**
     * The base graph defining if water flows between nodes by setting neighbors
     */
    public final Map<PointD, Set<PointD>> graph = new HashMap<>();


    /**
     * The mapping from every node to the surface area of the region closest to it
     */
    protected final Map<PointD, Double> areaMap = new HashMap<>();

    /**
     * The geometry underlying a stream graph
     *
     * @param boundingPolygon The bounding coordinates of a polygonal region
     * @param minDistance     The minimum distance between two nodes, setting the resolution
     */
    public EroderGeometry(PointD[] boundingPolygon, double minDistance) {
        this.boundingPolygon = boundingPolygon;
        this.minDistance = minDistance;
    }

    /**
     * The number of nodes in the graph
     *
     * @return The number of nodes in the graph
     */
    public int nodeCount() {
        return graph.size();
    }

    /**
     * Converts a {@link RectD} to a polygon
     *
     * @param rectD The RectD to be converted
     * @return A polygon to be used as a bounding polygon
     */
    public static PointD[] RectDtoPolygon(RectD rectD) {
        return new PointD[]{rectD.min, rectD.min.add(new PointD(rectD.width(), 0.0)), rectD.max, rectD.max.subtract(new PointD(rectD.width(), 0.0))};
    } 
}
