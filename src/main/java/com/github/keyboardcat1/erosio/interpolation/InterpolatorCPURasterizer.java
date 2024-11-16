package com.github.keyboardcat1.erosio.interpolation;

import com.github.keyboardcat1.erosio.EroderResults;
import org.kynosarges.tektosyne.geometry.LineD;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.PointI;
import org.kynosarges.tektosyne.geometry.RectD;
import org.kynosarges.tektosyne.subdivision.Subdivision;
import org.kynosarges.tektosyne.subdivision.SubdivisionEdge;
import org.kynosarges.tektosyne.subdivision.SubdivisionFace;

import java.util.*;

public class InterpolatorCPURasterizer extends Interpolator{
    private static final double EPSILON = 1E-12;
    private final RectD rectBounds;
    private final double[][] grid;
    private final boolean[][] written;

    /**
     * A pixel's dimension in erosion basis on a hypothetical screen
     */
    public final double pixelSize;



    /**
     * Interpolation on a grid using triangles
     *
     * @param eroderResults The {@link EroderResults} to interpolate
     * @param pixelSize     A pixel's dimension in erosion basis on a hypothetical screen
     *
     */
    public InterpolatorCPURasterizer(EroderResults eroderResults, double pixelSize) {
        super(eroderResults);
        this.pixelSize = pixelSize;

        rectBounds = RectD.circumscribe(eroderResults.eroderGeometry.boundingPolygon);
        int dimX = (int)(rectBounds.width()/pixelSize);
        int dimY = (int)(rectBounds.height()/pixelSize);
        grid = new double[dimX][dimY];
        written = new boolean[dimX][dimY];

        ArrayList<LineD> eroderEdges = new ArrayList<>(eroderResults.eroderGeometry.graph.size());
        for (Map.Entry<PointD, Set<PointD>> entry : eroderResults.eroderGeometry.graph.entrySet()) {
            for (PointD neighbor : entry.getValue()) {
                eroderEdges.add(new LineD(entry.getKey(), neighbor));
            }
        }
        Collection<SubdivisionFace> faces = Subdivision.fromLines(
                eroderEdges.toArray(LineD[]::new),
                EPSILON
        ).faces().values();

        for (SubdivisionFace face : faces) {
            List<SubdivisionEdge> cycleEdges = face.allCycleEdges();
            PointD A = cycleEdges.get(0).origin();
            PointD B = cycleEdges.get(1).origin();
            PointD C = cycleEdges.get(2).origin();
            double hA = eroderResults.heightMap.get(A);
            double hB = eroderResults.heightMap.get(B);
            double hC = eroderResults.heightMap.get(C);
            rasterize(A,B,C, hA, hB,hC);
        }
    }

    @Override
    public double interpolate(PointD point) {
        PointI index = toIndex(point);
        if (index.x>=grid.length || index.y>=grid[0].length ||  !written[index.x][index.y])
            throw new IndexOutOfBoundsException(point + " does not lie within the convex hull");
        return grid[index.x][index.y];
    }

    private void rasterize(PointD A,PointD B,PointD C, double heightA, double heightB, double heightC ) {
        RectD rect = RectD.circumscribe(A, B, C);
        PointI snapMin =  toIndex(rect.min);
        PointI snapMax = toIndex(rect.max);
        for (int x=snapMin.x; x<=snapMax.x; x++) for (int y=snapMin.y; y<=snapMax.y; y++) {
            PointD p = toPoint(new PointI(x,y));
            double ABxAp = A.crossProductLength(B, p);
            double BCxBp = B.crossProductLength(C, p);
            double CAxCp = C.crossProductLength(A, p);
            if (ABxAp>0 && BCxBp>0 && CAxCp>0) {
                double w = ABxAp/A.crossProductLength(B,C);
                double v = CAxCp/C.crossProductLength(A, B);
                double u = BCxBp/B.crossProductLength(C,A);
                if (x<grid.length && y<grid[0].length) {
                    grid[x][y] = u*heightA+v*heightB+w*heightC;
                    written[x][y] = true;
                }
            }
        }
    }

    private PointI toIndex(PointD p) {
        return new PointI((int)Math.floor((p.x-rectBounds.min.x)/pixelSize),
                (int)Math.floor((p.y-rectBounds.min.y)/pixelSize));
    }
    private PointD toPoint(PointI index) {
        return new PointD(pixelSize*index.x+rectBounds.min.x+pixelSize/2,
                        pixelSize*index.y+rectBounds.min.y+pixelSize/2);
    }
}
