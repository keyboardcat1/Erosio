package com.github.keyboardcat1.erosio.geometries;

import com.github.keyboardcat1.erosio.EroderGeometry;
import org.kynosarges.tektosyne.geometry.*;
import org.kynosarges.tektosyne.subdivision.Subdivision;
import org.kynosarges.tektosyne.subdivision.SubdivisionEdge;

import java.util.*;

/**
 * A natural-looking geometry based on Poisson disk sampling and Delaunay triangulation
 */
public class EroderGeometryNatural extends EroderGeometry {
    /**
     * The bounding coordinates of a polygonal region
     */
    public final PointD[] boundingPolygon;

    /**
     * The minimum distance between two nodes in the stream graph
     */
    public final double inverseSampleDensity;
    /**
     * The seed randomizing the sample points
     */
    public final long seed;

    /**
     * A natural-looking geometry based on Poisson disk sampling and Delaunay triangulation
     *
     * @param boundingPolygon      The bounding coordinates of a polygonal region
     * @param inverseSampleDensity The minimum distance between two nodes in the stream graph
     * @param seed                 A seed randomizing the sample points
     */
    public EroderGeometryNatural(PointD[] boundingPolygon, double inverseSampleDensity, long seed) {
        super(boundingPolygon, inverseSampleDensity);
        this.boundingPolygon = boundingPolygon;
        this.inverseSampleDensity = inverseSampleDensity;
        this.seed = seed;

        RectD bounds = RectD.circumscribe(boundingPolygon);
        PointD[] points = PoissonDiskSampler.sample(bounds, inverseSampleDensity, seed)
                .stream().filter(p -> GeoUtils.pointInPolygon(p, boundingPolygon) != PolygonLocation.OUTSIDE)
                .toList().toArray(new PointD[0]);

        VoronoiResults voronoiResults = Voronoi.findAll(points, bounds);
        Subdivision delaunaySubdivision = voronoiResults.toDelaunaySubdivision(true);

        for (SubdivisionEdge edge : delaunaySubdivision.edges().values()) {
            PointD A = edge.origin();
            PointD B = edge.destination();
            graph.putIfAbsent(A, new HashSet<>());
            graph.get(A).add(B);
            graph.putIfAbsent(B, new HashSet<>());
            graph.get(B).add(A);
        }

        for (int i = 0; i < voronoiResults.generatorSites.length; i++)
            areaMap.put(voronoiResults.generatorSites[i], Math.abs(GeoUtils.polygonArea(voronoiResults.voronoiRegions()[i])));

    }

    private static class PoissonDiskSampler {
        public static Set<PointD> sample(RectD bounds, double r, long seed) {
            return sample(bounds, r, 30, seed);

        }

        public static Set<PointD> sample(RectD bounds, double r, int k, long seed) {
            Random random = new Random(seed);
            double width = bounds.width();
            double height = bounds.height();
            double cellSize = r / Math.sqrt(2);
            int rows = (int) Math.ceil(width / cellSize);
            int cols = (int) Math.ceil(height / cellSize);
            RectI gridBounds = new RectI(0, 0, cols - 1, rows - 1);

            List<List<PointD>> grid = new ArrayList<>();
            for (int i = 0; i < cols; i++) {
                grid.add(new ArrayList<>());
                for (int j = 0; j < rows; j++) {
                    grid.get(i).add(null);
                }
            }

            List<PointD> active = new ArrayList<>();
            PointD x_0 = new PointD(random.nextDouble(bounds.min.x, bounds.max.x), random.nextDouble(bounds.min.y, bounds.max.y));
            grid.get(toGrid(x_0, bounds, cellSize).x).set(toGrid(x_0, bounds, cellSize).y, x_0);
            active.add(x_0);

            while (!active.isEmpty()) {
                int randomIndex = random.nextInt(active.size());
                PointD x_i = active.get(randomIndex);
                boolean found = false;
                for (int n = 0; n < k; n++) {
                    PointD sample = samplePointAround(x_i, bounds, r, random);
                    if (grid.get(toGrid(sample, bounds, cellSize).x).get(toGrid(sample, bounds, cellSize).y) != null)
                        continue;
                    if (hasNeighbours(sample, grid, r, bounds, gridBounds, cellSize)) continue;
                    found = true;
                    grid.get(toGrid(sample, bounds, cellSize).x).set(toGrid(sample, bounds, cellSize).y, sample);
                    active.add(sample);
                }
                if (!found) active.remove(randomIndex);
            }

            Set<PointD> points = new HashSet<>();
            for (int i = 0; i < cols; i++)
                for (int j = 0; j < rows; j++)
                    if (grid.get(i).get(j) != null)
                        points.add(grid.get(i).get(j));

            return points;
        }

        private static PointI toGrid(PointD point, RectD bounds, double cellSize) {
            PointD fromMin = point.subtract(bounds.min);
            return new PointI((int) (Math.floor(fromMin.x / cellSize)), (int) (Math.floor(fromMin.y / cellSize)));
        }

        private static PointD samplePointAround(PointD point, RectD bounds, double r, Random random) {
            PointD sample;
            do {
                double randomAngle = random.nextDouble(2 * Math.PI);
                double randomRadius = random.nextDouble(r, 2 * r);
                double offsX = (Math.cos(randomAngle) * randomRadius);
                double offsY = (Math.sin(randomAngle) * randomRadius);
                sample = point.add(new PointD(offsX, offsY));
            } while (!bounds.contains(sample));
            return sample;
        }

        private static boolean hasNeighbours(PointD point, List<List<PointD>> grid, double r, RectD bounds, RectI gridBounds, double cellSize) {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    if (x == 0 && y == 0) continue;
                    PointI gridPoint = new PointI(x, y).add(toGrid(point, bounds, cellSize));
                    if (!gridBounds.contains(gridPoint)) continue;
                    PointD neighbour = grid.get(gridPoint.x).get(gridPoint.y);
                    if (neighbour == null) continue;
                    if (neighbour.subtract(point).length() < r) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

}
