package com.github.keyboardcat1.erosio.geometries;

import com.github.keyboardcat1.erosio.EroderGeometry;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.RectD;

import java.util.HashSet;

/**
 * A simple and fast grid geometry
 */
public class EroderGeometryGrid extends EroderGeometry {
    private static final double EPSILON = 1E-12;

    /**
     * A simple and fast grid geometry
     *
     * @param bounds      The region bounding the erosion
     * @param minDistance The minimum distance between two nodes, setting the resolution
     */
    public EroderGeometryGrid(RectD bounds, double minDistance) {
        super(bounds, minDistance + EPSILON);
        minDistance = minDistance + EPSILON;
        int horizontalCount = (int) (bounds.width() / minDistance);
        int verticalCount = (int) (bounds.height() / minDistance);
        double horizontalMargin = (bounds.width() - horizontalCount * minDistance) / 2;
        double verticalMargin = (bounds.height() - verticalCount * minDistance) / 2;
        for (int x = 0; x <= horizontalCount; x++)
            for (int y = 0; y <= verticalCount; y++) {
                PointD node = new PointD(x * minDistance + horizontalMargin, y * minDistance + verticalMargin).add(bounds.min);
                graph.put(node, new HashSet<>(4));
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        if (dx * dy != 0) continue;
                        PointD neighbor = new PointD((x + dx) * minDistance + horizontalMargin, (y + dy) * minDistance + verticalMargin).add(bounds.min);
                        if (!bounds.contains(neighbor)) continue;
                        graph.get(node).add(neighbor);
                    }
            }

        for (PointD node : graph.keySet()) {
            areaMap.put(node, minDistance * minDistance);
        }
    }
}
