package com.github.keyboardcat1.erosio.interpolation;

import com.github.keyboardcat1.erosio.EroderResults;
import org.kynosarges.tektosyne.geometry.LineD;
import org.kynosarges.tektosyne.geometry.PointD;
import org.kynosarges.tektosyne.geometry.RectD;
import org.kynosarges.tektosyne.subdivision.Subdivision;
import org.kynosarges.tektosyne.subdivision.SubdivisionEdge;
import org.kynosarges.tektosyne.subdivision.SubdivisionFace;

import java.util.*;

/**
 * Random-access interpolation based on a 3D mesh
 */
public class InterpolatorCPURasterizerRandomAccess extends Interpolator {
    private static final double EPSILON = 1E-12;

    private final double scale;
    private final Vec3 min;
    /**
     * Mix coefficient between linear interpolation and Phong tessellation
     */
    public final double alpha;

    private final CellMap<Triangle> cellMap;

    /**
     * Random-access interpolation based on a 3D mesh
     *
     * @param eroderResults The {@link EroderResults} to interpolate
     * @param cellCountX    The number of random-access grid cells on the x-axis 
     * @param cellCountY    The number of random-access grid cells on the y-axis 
     * @param alpha         Mix coefficient between linear interpolation and Phong tessellation
     */
    public InterpolatorCPURasterizerRandomAccess(EroderResults eroderResults, int cellCountX, int cellCountY, double alpha) {
        super(eroderResults);
        this.alpha = alpha;

        RectD rectBounds = eroderResults.eroderGeometry.rectBounds;
        scale = Math.max(Math.max(rectBounds.width(), rectBounds.height()),eroderResults.maxHeight-eroderResults.minHeight);
        min = new Vec3(rectBounds.min.x, rectBounds.min.y, eroderResults.minHeight);

        int minX = (int) rectBounds.min.x;
        int minY = (int) rectBounds.min.y;
        int maxX = (int) rectBounds.max.x;
        int maxY = (int) rectBounds.max.y;
        this.cellMap = new CellMap<>(minX, minY, maxX, maxY, cellCountX, cellCountY);

        ArrayList<LineD> eroderEdges = new ArrayList<>(eroderResults.eroderGeometry.graph.size());
        for (Map.Entry<PointD, Set<PointD>> entry : eroderResults.eroderGeometry.graph.entrySet()) {
            for (PointD neighbor : entry.getValue()) {
                eroderEdges.add(new LineD(entry.getKey(), neighbor));
            }
        }
        Subdivision subdivision = Subdivision.fromLines(
                eroderEdges.toArray(LineD[]::new),
                EPSILON
        );

        Map<PointD, Vec3> normals = new HashMap<>();
        for (SubdivisionFace face : subdivision.faces().values()) {
            List<SubdivisionEdge> edges = face.allCycleEdges();
            PointD A_xy = edges.get(0).origin();
            PointD B_xy = edges.get(1).origin();
            PointD C_xy = edges.get(2).origin();
            Vec3 A = new Vec3(A_xy.x, A_xy.y, eroderResults.heightMap.get(A_xy));
            Vec3 B = new Vec3(B_xy.x, B_xy.y, eroderResults.heightMap.get(B_xy));
            Vec3 C = new Vec3(C_xy.x, C_xy.y, eroderResults.heightMap.get(C_xy));
            Vec3 AB = B.sub(A);
            Vec3 AC = C.sub(A);
            Vec3 n = AB.cross(AC).normalize();
            normals.put(A_xy, normals.getOrDefault(A_xy, Vec3.ZERO).add(n));
            normals.put(B_xy, normals.getOrDefault(B_xy, Vec3.ZERO).add(n));
            normals.put(C_xy, normals.getOrDefault(C_xy, Vec3.ZERO).add(n));
        }

        Collection<SubdivisionFace> faces =  subdivision.faces().values();
        for (SubdivisionFace face : faces) {
            List<SubdivisionEdge> cycleEdges = face.allCycleEdges();

            PointD A_xy = cycleEdges.get(0).origin();
            PointD B_xy = cycleEdges.get(1).origin();
            PointD C_xy = cycleEdges.get(2).origin();
            double A_z = eroderResults.heightMap.get(A_xy);
            double B_z = eroderResults.heightMap.get(B_xy);
            double C_z = eroderResults.heightMap.get(C_xy);
            Vec3 A = new Vec3(A_xy.x, A_xy.y, A_z);
            Vec3 B = new Vec3(B_xy.x, B_xy.y, B_z);
            Vec3 C = new Vec3(C_xy.x, C_xy.y, C_z);

            Vec3 sA = toS(A);
            Vec3 sB = toS(B);
            Vec3 sC = toS(C);
            Vec3 nA = normals.get(A_xy).normalize();
            Vec3 nB = normals.get(B_xy).normalize();
            Vec3 nC = normals.get(C_xy).normalize();
            Vec3 sij = PI(sA, sB, nA).add(PI(sB, sA, nB));
            Vec3 sjk = PI(sB, sC, nB).add(PI(sC, sB, nC));
            Vec3 ski = PI(sC, sA, nC).add(PI(sA, sC, nA));

            Vec3 P1 = fromS(sO(sA,sB,sC,sij,sjk,ski,.5,.5,0));
            Vec3 P2 = fromS(sO(sA,sB,sC,sij,sjk,ski,0,.5,.5));
            Vec3 P3 = fromS(sO(sA,sB,sC,sij,sjk,ski,.5,0,.5));
            Vec3 P0 = fromS(sO(sA,sB,sC,sij,sjk,ski,1/3D,1/3D,1/3D));

            this.addTriangle(A,P1,P0);
            this.addTriangle(P1,B,P0);
            this.addTriangle(B,P2,P0);
            this.addTriangle(P2,C,P0);
            this.addTriangle(C,P3,P0);
            this.addTriangle(P3,A,P0);
        }
    }

    private void addTriangle(Vec3 v1, Vec3 v2, Vec3 v3) {
        PointD p1 = new PointD(v1.x, v1.y);
        PointD p2 = new PointD(v2.x, v2.y);
        PointD p3 = new PointD(v3.x, v3.y);
        Triangle triangle = new Triangle(p1, p2, p3, v1.z, v2.z, v3.z);
        int minX = (int) min(p1.x, p2.x, p3.x);
        int minY = (int) min(p1.y, p2.y, p3.y);
        int maxX = (int) max(p1.x, p2.x, p3.x);
        int maxY = (int) max(p1.y, p2.y, p3.y);
        this.cellMap.add(minX, minY, maxX, maxY, triangle);
    }

    private Vec3 toS(Vec3 V) {
        return V.sub(min).scale(2/scale).sub(Vec3.ONE);
    }
    private Vec3 fromS(Vec3 sV) {
        return sV.add(Vec3.ONE).scale(scale/2).add(min);
    }
    private Vec3 sO(Vec3 sA, Vec3 sB, Vec3 sC, Vec3 sij, Vec3 sjk, Vec3 ski, double u, double v, double w) {
        Vec3 sP = sA.scale(u*u).add(
                sB.scale(v*v)).add(
                sC.scale(w*w)).add(
                sij.scale(u*v)).add(
                sjk.scale(v*w)).add(
                ski.scale(w*u));
        Vec3 sQ = sA.scale(u).add(
                sB.scale(v)).add(
                sC.scale(w));
        return sP.scale(alpha).add(sQ.scale(1-alpha));
    }
    private Vec3 PI(Vec3 p, Vec3 q, Vec3 n_p) {
        return q.sub(n_p.scale(q.sub(p).dot(n_p)));
    }

    @Override
    public double interpolate(PointD point) {
        double x = point.x+50, y = point.y+50;
        List<Triangle> cell = this.cellMap.getCell((int)Math.floor(x), (int)Math.floor(y));
        if (cell.isEmpty()) throw new IndexOutOfBoundsException(point + " does not lie within the convex hull");
        return sample(x, y, cell);
    }

    private static double sample(double x, double y, List<Triangle> triangles) {
        for (Triangle triangle : triangles) {
            PointD A_xy = triangle.p1();
            PointD B_xy = triangle.p2();
            PointD C_xy = triangle.p3();

            double ABxAp = crossProductLength(A_xy, B_xy, x, y);
            double BCxBp = crossProductLength(B_xy, C_xy, x, y);
            double CAxCp = crossProductLength(C_xy, A_xy, x, y);
            if (ABxAp > 0 && BCxBp > 0 && CAxCp > 0) {
                double w = ABxAp / A_xy.crossProductLength(B_xy, C_xy);
                double v = CAxCp / C_xy.crossProductLength(A_xy, B_xy);
                double u = BCxBp / B_xy.crossProductLength(C_xy, A_xy);
                return u * triangle.z1() + v * triangle.z2() + w * triangle.z3();
            }
        }
        throw new IndexOutOfBoundsException(new PointD(x,y) + " does not lie within the convex hull");
    }

    private static double min(double d1, double d2, double d3) {
        return Math.min(d1, Math.min(d2, d3));
    }

    private static double max(double d1, double d2, double d3) {
        return Math.max(d1, Math.max(d2, d3));
    }

    // same as PointD.crossProductLength but allows us to skip the PointD allocation for b
    private static double crossProductLength(PointD self, PointD a, double bx, double by) {
        return ((a.x - self.x) * (by - self.y) - (bx - self.x) * (a.y - self.y));
    }

    private record Triangle(PointD p1, PointD p2, PointD p3, double z1, double z2, double z3) {
    }

    private static class CellMap<T> {
        private final int minX;
        private final int minY;
        private final int cellWidth;
        private final int cellHeight;
        private final int cellCountX;
        private final int cellCountY;
        private final List<T>[] cells;

        @SuppressWarnings("unchecked")
        public CellMap(int minX, int minY, int maxX, int maxY, int cellCountX, int cellCountY) {
            this.minX = minX;
            this.minY = minY;
            this.cellWidth = (maxX - minX) / cellCountX;
            this.cellHeight = (maxY - minY) / cellCountY;
            this.cellCountX = cellCountX;
            this.cellCountY = cellCountY;
            this.cells = new List[cellCountX * cellCountY];
        }

        public int getCellWidth() {
            return this.cellWidth;
        }

        public int getCellHeight() {
            return this.cellHeight;
        }

        public int getCellCountX() {
            return this.cellCountX;
        }

        public int getCellCountY() {
            return this.cellCountY;
        }

        public void add(int x0, int y0, int x1, int y1, T value) {
            int cellX0 = this.getCellX(x0) - this.getCellX(this.minX);
            int cellY0 = this.getCellY(y0) - this.getCellY(this.minY);
            int cellX1 = this.getCellX(x1) - this.getCellX(this.minX);
            int cellY1 = this.getCellY(y1) - this.getCellY(this.minY);
            int widthX = Math.abs(cellX1 - cellX0) + 1;
            int widthY = Math.abs(cellY1 - cellY0) + 1;

            for(int cellX = -1; cellX < widthX; cellX++) {
                for(int cellY = -1; cellY < widthY; cellY++) {
                    List<T> cell = this.getCell(cellX0 + cellX, cellY0 + cellY, true);
                    if(cell != null) {
                        cell.add(value);
                    }
                }
            }
        }

        public List<T> getCell(int x, int y) {
            int cellX = this.getCellX(x) - this.getCellX(this.minX);
            int cellY = this.getCellY(y) - this.getCellY(this.minY);
            List<T> cell = this.getCell(cellX, cellY, false);
            return cell != null ? cell : List.of();
        }

        private List<T> getCell(int x, int y, boolean initialize) {
            int index = this.index(x, y);
            if(index >= 0 && index < this.cells.length) {
                List<T> cell = this.cells[index];
                if(cell != null) {
                    return cell;
                }
                if(initialize) {
                    return this.cells[index] = new ArrayList<>();
                }
            }
            return null;
        }

        private int getCellX(int x) {
            return Math.floorDiv(x, this.cellWidth);
        }

        private int getCellY(int y) {
            return Math.floorDiv(y, this.cellHeight);
        }

        private int index(int x, int y) {
            return y * this.cellCountY + x;
        }
    }
}