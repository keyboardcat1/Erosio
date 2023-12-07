package com.github.keyboardcat1.erosio;

import org.kynosarges.tektosyne.geometry.GeoUtils;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.PolygonLocation;
import org.kynosarges.tektosyne.subdivision.Subdivision;
import org.kynosarges.tektosyne.subdivision.SubdivisionEdge;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The main fluvial erosion class
 *
 * @see <a href="https://inria.hal.science/hal-01262376/document">Large Scale Terrain Generation from Tectonic Uplift and
 * Fluvial Erosion</a> by Guillaume Cordonnier
 */
public class Eroder {

    /**
     * Computes an eroded heightmap
     *
     * @param settings        The parameters of the erosion algorithm
     * @param voronoiDelaunay The Voronoi tessellated and Delaunay triangulated area to erode
     * @return An eroded heightmap along with computational details
     */
    public static Results erode(Settings settings, VoronoiDelaunay voronoiDelaunay) {
        Graph baseGraph = buildBaseGraph(voronoiDelaunay.delaunaySubdivision);

        Map<PointD, Double> heightMap = new HashMap<>(baseGraph.size());
        Map<PointD, Double> upliftMap = new HashMap<>(baseGraph.size());
        Map<PointD, Double> areaMap = new HashMap<>(baseGraph.size());
        for (int i = 0; i < baseGraph.size(); i++) {
            PointD p = voronoiDelaunay.voronoiResults.generatorSites[i];
            heightMap.put(p, settings.initialHeightLambda().apply(p));
            upliftMap.put(p, settings.upliftLambda().apply(p));
            areaMap.put(p, Math.abs(GeoUtils.polygonArea(voronoiDelaunay.voronoiResults.voronoiRegions()[i])));
        }


        PointD[] convexHull = GeoUtils.convexHull(voronoiDelaunay.delaunaySubdivision.vertices().keySet().toArray(new PointD[0]));
        Set<PointD> potentialDrains = voronoiDelaunay.delaunaySubdivision.nodes().stream()
                .filter(node -> GeoUtils.pointInPolygon(node, convexHull) == PolygonLocation.VERTEX)
                .collect(Collectors.toSet());


        boolean converged = false;
        StreamGraph streamGraph = null;
        Map<PointD, java.lang.Double> drainageMap = null;
        for (int i = 0; i < settings.maxIterations() && !converged; i++) {
            streamGraph = buildInitialStreamGraph(baseGraph, heightMap);
            Set<PointD> drains = new HashSet<>(streamGraph.roots);
            drains.retainAll(potentialDrains);
            delakefyStreamGraph(streamGraph, voronoiDelaunay.delaunaySubdivision, heightMap, drains);
            drainageMap = getDrainageMap(streamGraph, areaMap);
            Map<PointD, Double> newHeightMap = computeNewHeightMap(heightMap, upliftMap, drainageMap, streamGraph, settings, voronoiDelaunay.inverseSampleDensity);
            converged = true;
            for (PointD point : newHeightMap.keySet())
                if (Math.abs(newHeightMap.get(point) - heightMap.get(point)) > settings.convergenceThreshold())
                    converged = false;
            heightMap = newHeightMap;
        }

        return new Results(heightMap, streamGraph, drainageMap, voronoiDelaunay);
    }


    private static Graph buildBaseGraph(Subdivision delaunaySubdivision) {
        Graph out = new Graph();
        for (SubdivisionEdge edge : delaunaySubdivision.edges().values()) {
            PointD A = edge.origin();
            PointD B = edge.destination();
            out.putIfAbsent(A, new HashSet<>());
            out.get(A).add(B);
            out.putIfAbsent(B, new HashSet<>());
            out.get(B).add(A);
        }
        return out;
    }

    private static StreamGraph buildInitialStreamGraph(Graph baseGraph, Map<PointD, Double> heightMap) {
        Function<PointD, PointD> getLowestNeighbor = point -> {
            PointD lowest = point;
            for (PointD neighbor : baseGraph.get(point))
                if (heightMap.get(neighbor) < heightMap.get(lowest))
                    lowest = neighbor;
            return lowest;
        };
        final StreamGraph out = new StreamGraph();
        for (PointD point : baseGraph.keySet()) {
            out.putIfAbsent(point, new HashSet<>());
            PointD lowest = getLowestNeighbor.apply(point);
            out.putIfAbsent(lowest, new HashSet<>());
            if (!lowest.equals(point)) {
                out.get(lowest).add(point);
                out.downstreamMap.put(point, lowest);
            } else
                out.roots.add(point);
        }

        return out;
    }

    private static void delakefyStreamGraph(StreamGraph streamGraph, Subdivision delaunaySubdivision, Map<PointD, Double> heightMap, Set<PointD> drains) {
        if (drains.containsAll(streamGraph.roots))
            return;

        LakePassMap lakePassMap = getLakePassMap(streamGraph, delaunaySubdivision, heightMap);
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

    private static LakePassMap getLakePassMap(StreamGraph streamGraph, Subdivision delaunaySubdivision, Map<PointD, Double> heightMap) {
        Function<PointD, PointD> getRoot = point -> {
            PointD node = point;
            PointD downstreamNode;
            while ((downstreamNode = streamGraph.downstreamMap.get(node)) != null) node = downstreamNode;
            return node;
        };
        Map<PointD, PointD> rootMap = streamGraph.keySet().stream().collect(Collectors.toMap(p -> p, getRoot));

        LakePassMap out = new LakePassMap();
        for (PointD node : streamGraph.keySet()) {
            PointD nodeRoot = rootMap.get(node);
            for (PointD neighbor : delaunaySubdivision.getNeighbors(node)) {
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
                                                           Map<PointD, Double> drainageMap, StreamGraph streamGraph,
                                                           Settings settings, double inverseSampleDensity) {
        final Map<PointD, Double> out = new HashMap<>(streamGraph.size());
        for (PointD drain : streamGraph.roots)
            computeNewHeightMap(oldHeightMap, upliftMap, drainageMap, streamGraph, settings, inverseSampleDensity, out, drain, null);
        return out;
    }

    private static void computeNewHeightMap(Map<PointD, Double> oldHeightMap, Map<PointD, Double> upliftMap,
                                            Map<PointD, Double> drainageMap, StreamGraph streamGraph,
                                            Settings settings, double inverseSampleDensity,
                                            Map<PointD, Double> out, PointD current, PointD downstream) {
        double distance;
        double downstreamHeight;
        if (Objects.isNull(downstream)) {
            distance = inverseSampleDensity;
            downstreamHeight = 0;
        } else {
            distance = current.subtract(downstream).length();
            downstreamHeight = out.get(downstream);
        }
        double oldHeight = oldHeightMap.get(current);
        double uplift = upliftMap.get(current);
        double drainageArea = drainageMap.get(current);
        double m = settings.mnRatio();
        double k = settings.erosionCoefficientLambda().apply(current);
        double dt = settings.timeStep();
        double maxSlope = Math.tan(Math.toDegrees(settings.maxSlopeDegrees()));

        double erosionImportance = k * Math.pow(drainageArea, m) / distance;
        double newHeight = (oldHeight + dt * (uplift + erosionImportance * downstreamHeight)) / (1 + erosionImportance * dt);
        double slope = (newHeight - downstreamHeight) / distance;
        if (Math.abs(slope) > maxSlope) newHeight = distance * maxSlope;
        out.put(current, newHeight);

        for (PointD neighbor : streamGraph.get(current))
            computeNewHeightMap(oldHeightMap, upliftMap, drainageMap, streamGraph, settings, inverseSampleDensity, out, neighbor, current);
    }


    /**
     * The input settings for {@link Eroder}
     *
     * @param upliftLambda             A function taking in a {@link PointD} and returning the uplift at that point
     * @param initialHeightLambda      A function taking in a {@link PointD} and returning the initial height at that point
     * @param erosionCoefficientLambda A function taking in a {@link PointD} and returning a coefficient controlling how deep water cuts
     * @param mnRatio                  A value between 0 and 1 controlling the nature of the erosion (see stream power equation)
     * @param maxSlopeDegrees          The maximum slope due to thermal erosion in degrees
     * @param timeStep                 The simulated time taken between each height update
     * @param maxIterations            The maximum number of height updates
     * @param convergenceThreshold     The maximum height difference between two updates dictating when they should cease
     */
    public record Settings(Function<PointD, Double> upliftLambda, Function<PointD, Double> initialHeightLambda,
                           Function<PointD, Double> erosionCoefficientLambda, double mnRatio, double maxSlopeDegrees,
                           double timeStep, int maxIterations, double convergenceThreshold) {
    }

    /**
     * The output of {@link Eroder}
     *
     * @param heightMap       A mapping from each stream node to its height
     * @param streamGraph     The {@link StreamGraph} used in the last erosion iteration
     * @param drainageMap     A mapping from each stream node to the volume of water that passes by it
     * @param voronoiDelaunay The eroded Voronoi tessellated and Delaunay triangulated area
     */
    public record Results(Map<PointD, Double> heightMap, StreamGraph streamGraph, Map<PointD, Double> drainageMap,
                          VoronoiDelaunay voronoiDelaunay) {


        /**
         * Interpolates the height of a point by nearest neighbor
         *
         * @param x The X coordinate of the point
         * @param y The Y coordinate of that point
         * @return The interpolated height at the point
         */
        public double interpolateNearestNeighbor(double x, double y) {
            return interpolateNearestNeighbor(new PointD(x, y));
        }

        /**
         * Interpolates the height of a point by nearest neighbor
         *
         * @param point The point to interpolate at
         * @return The interpolated height at the point
         */
        public double interpolateNearestNeighbor(PointD point) {
            return heightMap.get(voronoiDelaunay.delaunaySubdivision.findNearestNode(point));
        }

        /**
         * Interpolated the height of a point with Inverse Distance Weighted
         *
         * @param x     The X coordinate of the point
         * @param y     The Y coordinate of that point
         * @param alpha The IDW inverse exponent parameter
         * @return The interpolated height at the point
         */
        public double interpolateInverseDistanceWeighted(double x, double y, double alpha) {
            return interpolateInverseDistanceWeighted(new PointD(x, y), alpha);
        }

        /**
         * Interpolated the height of a point with Inverse Distance Weighted
         *
         * @param point The point to interpolate at
         * @param alpha The IDW inverse exponent parameter
         * @return The interpolated height at the point
         */
        public double interpolateInverseDistanceWeighted(PointD point, double alpha) {
            double numerator = 0;
            double denominator = 0;
            for (PointD vertex : voronoiDelaunay.delaunaySubdivision.getNeighbors(voronoiDelaunay.delaunaySubdivision.findNearestNode(point))) {
                numerator += heightMap.get(vertex) * Math.pow(point.subtract(vertex).length(), alpha);
                denominator += Math.pow(point.subtract(vertex).length(), alpha);
            }
            return numerator / denominator;
        }
    }
}
