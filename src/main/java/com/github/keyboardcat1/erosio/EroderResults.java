package com.github.keyboardcat1.erosio;

import org.kynosarges.tektosyne.geometry.PointD;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The output of {@link Eroder}
 */
public class EroderResults {
    /**
     * The mapping from each stream node to its height
     */
    public final Map<PointD, Double> heightMap;
    /**
     * The maximum height in the heightmap
     */
    public final double maxHeight;
    /**
     * The minimum height in the heightmap
     */
    public final double minHeight;
    /**
     * The set of {@link EroderEdge}s forming the fluvial network
     */
    public final Set<EroderEdge> eroderEdges;
    /**
     * The {@link EroderGeometry} passed as input
     */
    public final EroderGeometry eroderGeometry;

    EroderResults(Map<PointD, Double> heightMap, Set<EroderEdge> eroderEdges, EroderGeometry eroderGeometry) {
        this.heightMap = heightMap;
        this.eroderEdges = eroderEdges;
        this.eroderGeometry = eroderGeometry;

        Optional<Double> max = heightMap.values().stream().max(Double::compareTo);
        assert max.isPresent();
        this.maxHeight = max.get();
        Optional<Double> min = heightMap.values().stream().min(Double::compareTo);
        this.minHeight = min.get();
    }
}