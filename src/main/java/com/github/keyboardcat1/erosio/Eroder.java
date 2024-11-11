package com.github.keyboardcat1.erosio;

import org.kynosarges.tektosyne.geometry.GeoUtils;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.PolygonLocation;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The main fluvial erosion class
 *
 * @see <a href="https://inria.hal.science/hal-01262376/document">Large Scale Terrain Generation from Tectonic Uplift and
 * Fluvial Erosion</a> by Guillaume Cordonnier
 */
public final class Eroder {

    /**
     * Computes an eroded heightmap
     *
     * @param settings        The parameters of the erosion algorithm
     * @param eroderGeometry The Voronoi tessellated and Delaunay triangulated area to erode
     * @return An eroded heightmap along with computational details
     */
    public static EroderResults erode(EroderSettings settings, EroderGeometry eroderGeometry) {
        Map<PointD, Double> heightMap = new HashMap<>(eroderGeometry.graph.size());
        Map<PointD, Double> upliftMap = new HashMap<>(eroderGeometry.graph.size());
        Map<PointD, Double> erosionRateMap = new HashMap<>(eroderGeometry.graph.size());
        for (PointD point : eroderGeometry.graph.keySet()) {
            heightMap.put(point, settings.initialHeightLambda().apply(point));
            upliftMap.put(point, settings.upliftLambda().apply(point));
            erosionRateMap.put(point, settings.erosionRateLambda().apply(point));
        }

        PointD[] convexHull = GeoUtils.convexHull(eroderGeometry.graph.keySet().toArray(new PointD[0]));
        Set<PointD> potentialDrains = eroderGeometry.graph.keySet().stream()
                .filter(node -> GeoUtils.pointInPolygon(node, convexHull) == PolygonLocation.VERTEX)
                .collect(Collectors.toSet());
        boolean converged = false;
        StreamGraph streamGraph = null;
        Map<PointD, java.lang.Double> drainageMap = null;
        int i=0;
        for (i = 0; i < settings.maxIterations() && !converged; i++) {
            streamGraph = buildInitialStreamGraph(eroderGeometry.graph, heightMap);
            Set<PointD> drains = new HashSet<>(streamGraph.roots);
            drains.retainAll(potentialDrains);
            delakefyStreamGraph(streamGraph, eroderGeometry.graph, heightMap, drains);
            drainageMap = getDrainageMap(streamGraph, eroderGeometry.areaMap);
            Map<PointD, Double> newHeightMap = computeNewHeightMap(heightMap, upliftMap, drainageMap, erosionRateMap, streamGraph, settings, eroderGeometry.minDistance);
            converged = true;
            for (PointD point : newHeightMap.keySet())
                if (Math.abs(newHeightMap.get(point) - heightMap.get(point)) > settings.convergenceThreshold()) {
                        converged = false;
                        break;
                    }
            heightMap = newHeightMap;
        }

        assert streamGraph != null;
        return new EroderResults(heightMap, getEroderEdges(streamGraph, drainageMap), eroderGeometry, converged ? i : -1);
    }


    private static StreamGraph buildInitialStreamGraph(Map<PointD, Set<PointD>> graph, Map<PointD, Double> heightMap) {
        Function<PointD, PointD> getLowestNeighbor = point -> {
            PointD lowest = point;
            for (PointD neighbor : graph.get(point))
                if (heightMap.get(neighbor) < heightMap.get(lowest))
                    lowest = neighbor;
            return lowest;
        };
        final StreamGraph out = new StreamGraph();
        for (PointD point : graph.keySet()) {
            out.putIfAbsent(point, new HashSet<>());
            PointD lowest = getLowestNeighbor.apply(point);
            out.putIfAbsent(lowest, new HashSet<>());
            if (!lowest.equals(point))
                out.get(lowest).add(point);
            else
                out.roots.add(point);
        }

        return out;
    }

    private static void delakefyStreamGraph(StreamGraph streamGraph, Map<PointD, Set<PointD>> graph, Map<PointD, Double> heightMap, Set<PointD> drains) {
        if (drains.containsAll(streamGraph.roots))
            return;

        LakePassMap lakePassMap = getLakePassMap(streamGraph, graph, heightMap);
        SortedSet<LakePass> candidates = new TreeSet<>();
        for (PointD drain : drains) {
            candidates.addAll(lakePassMap.getTo(drain).values());
            lakePassMap.removeAll(lakePassMap.getFrom(drain).values());
        }

        while (!candidates.isEmpty()) {
            LakePass active = candidates.first();
            candidates.addAll(lakePassMap.getTo(active.rootFrom()).values());
            candidates.removeAll(lakePassMap.getFrom(active.rootFrom()).values());
            lakePassMap.removeAll(lakePassMap.getFrom(active.rootFrom()).values());
            candidates.remove(active);

            streamGraph.get(active.passTo()).add(active.rootFrom());
            streamGraph.roots.remove(active.rootFrom());
        }
    }

    private static LakePassMap getLakePassMap(StreamGraph streamGraph, Map<PointD, Set<PointD>> graph, Map<PointD, Double> heightMap) {
        Map<PointD, PointD> rootMap = new HashMap<>();
        Queue<Map.Entry<PointD, PointD>> rootQueue = new ArrayDeque<>(
                streamGraph.roots.stream().collect(Collectors.toMap(k -> k, v -> v)).entrySet()
        );
        while (!rootQueue.isEmpty()) {
            Map.Entry<PointD, PointD> current = rootQueue.poll();
            rootMap.put(current.getKey(), current.getValue());
            for (PointD neighbor : streamGraph.get(current.getKey())) {
                rootQueue.add(new AbstractMap.SimpleImmutableEntry<>(neighbor, current.getValue()));
            }
        }

        final LakePassMap out = new LakePassMap();
        for (PointD node : streamGraph.keySet()) {
            PointD nodeRoot = rootMap.get(node);
            for (PointD neighbor : graph.get(node)) {
                PointD neighborRoot = rootMap.get(neighbor);
                if (nodeRoot == neighborRoot) continue;
                double passHeight = Math.max(heightMap.get(node), heightMap.get(neighbor));
                if (Objects.isNull(out.getFrom(nodeRoot)) || Objects.isNull(out.getFrom(nodeRoot).get(neighborRoot)) ||
                        passHeight < out.getFrom(nodeRoot).get(neighborRoot).passHeight()) {
                    LakePass lp = new LakePass(nodeRoot, neighborRoot, node, neighbor, passHeight);
                    out.put(lp);
                    out.put(LakePassMap.anti(lp));
                }
            }
        }
        return out;
    }

    private static Map<PointD, Double> getDrainageMap(StreamGraph streamGraph, Map<PointD, Double> areaMap) {
        final Map<PointD, Double> out = new HashMap<>(streamGraph.size());
        for (PointD root : streamGraph.roots)
            getDrainageMap(streamGraph, areaMap, out, root);
        return out;
    }

    private static double getDrainageMap(StreamGraph streamGraph, Map<PointD, Double> areaMap,
                                         Map<PointD, Double> out, PointD current) {
        double currentArea = areaMap.get(current);
        for (PointD neighbor : streamGraph.get(current))
            currentArea += getDrainageMap(streamGraph, areaMap, out, neighbor);
        out.put(current, currentArea);
        return currentArea;
    }

    private static Map<PointD, Double> computeNewHeightMap(Map<PointD, Double> oldHeightMap, Map<PointD, Double> upliftMap,
                                                           Map<PointD, Double> drainageMap, Map<PointD, Double> erosionRateMap,
                                                           StreamGraph streamGraph, EroderSettings settings, double minDistance) {
        final Map<PointD, Double> out = new HashMap<>(streamGraph.size());
        Queue<Map.Entry<PointD, PointD>> downstreamQueue = new ArrayDeque<>(
                streamGraph.roots.stream().collect(Collectors.toMap(k -> k, v -> PointD.EMPTY)).entrySet()
        );

        while (!downstreamQueue.isEmpty()) {
            Map.Entry<PointD, PointD> entry = downstreamQueue.poll();
            PointD current = entry.getKey();
            PointD downstream = entry.getValue();

            double distance;
            double downstreamHeight;
            if (downstream == PointD.EMPTY) {
                distance = minDistance;
                downstreamHeight = 0;
            } else {
                distance = current.subtract(downstream).length();
                downstreamHeight = out.get(downstream);
            }
            double oldHeight = oldHeightMap.get(current);
            double uplift = upliftMap.get(current);
            double drainageArea = drainageMap.get(current);
            double m = settings.mnRatio();
            double k = erosionRateMap.get(current);
            double dt = settings.timeStep();

            double erosionImportance = k * Math.pow(drainageArea, m) / distance;
            double newHeight = (oldHeight + dt * (uplift + erosionImportance * downstreamHeight)) / (1 + erosionImportance * dt);
            double slope = (newHeight - downstreamHeight) / distance;
            double maxSlope = Math.tan(Math.toRadians(settings.maxSlopeDegreesLambda().apply(current, newHeight)));
            if (Math.abs(slope) > maxSlope) newHeight = downstreamHeight + distance * maxSlope;
            out.put(current, newHeight);

            for (PointD neighbor : streamGraph.get(current))
                downstreamQueue.add(new AbstractMap.SimpleImmutableEntry<>(neighbor, current));
        }
        return out;
    }

    private static Set<EroderEdge> getEroderEdges(StreamGraph streamGraph, Map<PointD, Double> drainageMap) {
        Set<EroderEdge> out = new HashSet<>();
        streamGraph.forEach((node, neighbors) -> {
            for (PointD neighbor : neighbors)
                out.add(new EroderEdge(node, neighbor, drainageMap.get(neighbor), drainageMap.get(node)));
        });
        return out;
    }

    private static class StreamGraph extends HashMap<PointD, Set<PointD>> {
        public final Set<PointD> roots = new HashSet<>();
    }

    private record LakePass(PointD rootFrom, PointD rootTo, PointD passFrom, PointD passTo,
                            double passHeight) implements Comparable<LakePass> {
        @Override
        public int compareTo(LakePass lakePass) {
            if (passHeight != lakePass.passHeight)
                return Double.compare(passHeight, lakePass.passHeight);
            else if (!Objects.equals(this, lakePass))
                return Double.compare(this.hashCode(), lakePass.hashCode());
            else
                return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LakePass lakePass = (LakePass) o;
            return Double.compare(passHeight, lakePass.passHeight) == 0 && Objects.equals(rootFrom, lakePass.rootFrom) && Objects.equals(rootTo, lakePass.rootTo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rootFrom, rootTo, passHeight);
        }
    }

    private static class LakePassMap {
        private final Map<PointD, Map<PointD, LakePass>> lakePassesByFrom = new HashMap<>();
        private final Map<PointD, Map<PointD, LakePass>> lakePassesByTo = new HashMap<>();

        public static LakePass anti(LakePass lakePass) {
            return new LakePass(lakePass.rootTo(), lakePass.rootFrom(), lakePass.passTo(), lakePass.passFrom(), lakePass.passHeight());
        }

        public Map<PointD, LakePass> getFrom(PointD pointD) {
            return lakePassesByFrom.get(pointD);
        }

        public Map<PointD, LakePass> getTo(PointD pointD) {
            return lakePassesByTo.get(pointD);
        }

        public void put(LakePass lakePass) {
            lakePassesByFrom.putIfAbsent(lakePass.rootFrom(), new HashMap<>());
            lakePassesByFrom.get(lakePass.rootFrom()).put(lakePass.rootTo(), lakePass);
            lakePassesByTo.putIfAbsent(lakePass.rootTo(), new HashMap<>());
            lakePassesByTo.get(lakePass.rootTo()).put(lakePass.rootFrom(), lakePass);
        }

        public void remove(LakePass lakePass) {
            if (!Objects.isNull(lakePassesByFrom.get(lakePass.rootFrom())))
                lakePassesByFrom.get(lakePass.rootFrom()).remove(lakePass.rootTo());
            if (!Objects.isNull(lakePassesByTo.get(lakePass.rootTo())))
                lakePassesByTo.get(lakePass.rootTo()).remove(lakePass.rootFrom());
        }

        public void removeAll(Collection<LakePass> lakePasses) {
            for (LakePass lakePass : new HashSet<>(lakePasses))
                remove(lakePass);
        }
    }
}
