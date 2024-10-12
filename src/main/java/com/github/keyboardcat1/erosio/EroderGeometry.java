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
     * The region bounding the erosion
     */
    public final RectD bounds;
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
     * @param bounds      The region bounding the erosion
     * @param minDistance The minimum distance between two nodes, setting the resolution
     */
    public EroderGeometry(RectD bounds, double minDistance) {
        this.bounds = bounds;
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

}
