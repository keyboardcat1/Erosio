package com.github.keyboardcat1.erosio;

import org.kynosarges.tektosyne.geometry.PointD;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A directed graph linking each node to its upstream neighbors
 */
class StreamGraph extends Graph {
    /**
     * The roots of this stream graph
     */
    final Set<PointD> roots = new HashSet<>();
    /**
     * A mapping from each node to it's downstream neighbor
     */
    final Map<PointD, PointD> downstreamMap = new HashMap<>();
}