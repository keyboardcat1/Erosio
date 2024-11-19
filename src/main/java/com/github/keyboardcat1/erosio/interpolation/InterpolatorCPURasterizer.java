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

public class InterpolatorCPURasterizer extends Interpolator {
    private static final double EPSILON = 1E-12;
    private final double[][] grid;
    private final boolean[][] written;

    private final double scale;
    private final Vec3 min;

    /**
     * A pixel's dimension in erosion basis on a hypothetical screen
     */
    public final double pixelSize;
    /**
     * Mix coefficient between linear interpolation and Phong tessellation
     */
    public final double alpha;

    /**
     * Graphics-based interpolation based on triangle rendering
     *
     * @param eroderResults The {@link EroderResults} to interpolate
     * @param pixelSize     A pixel's dimension in erosion basis on a hypothetical screen
     * @param alpha         Mix coefficient between linear interpolation and Phong tessellation
     */
    public InterpolatorCPURasterizer(EroderResults eroderResults, double pixelSize, double alpha) {
        super(eroderResults);
        this.pixelSize = pixelSize;
        this.alpha = alpha;

        RectD rectBounds = eroderResults.eroderGeometry.rectBounds;
        scale = Math.max(Math.max(rectBounds.width(), rectBounds.height()),eroderResults.maxHeight-eroderResults.minHeight);
        min = new Vec3(rectBounds.min.x, rectBounds.min.y, eroderResults.minHeight);

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

            rasterize(A,P1,P0);
            rasterize(P1,B,P0);
            rasterize(B,P2,P0);
            rasterize(P2,C,P0);
            rasterize(C,P3,P0);
            rasterize(P3,A,P0);
        }
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
        PointI index = toIndex(point);
        if (index.x>=grid.length || index.y>=grid[0].length ||  !written[index.x][index.y])
            throw new IndexOutOfBoundsException(point + " does not lie within the convex hull");
        return grid[index.x][index.y];
    }

    private void rasterize(Vec3 A, Vec3 B, Vec3 C) {
        PointD A_xy = new PointD(A.x, A.y);
        PointD B_xy = new PointD(B.x, B.y);
        PointD C_xy = new PointD(C.x, C.y);
        RectD rect = RectD.circumscribe(A_xy, B_xy, C_xy);
        PointI snapMin =  toIndex(rect.min);
        PointI snapMax = toIndex(rect.max);
        for (int x=snapMin.x; x<=snapMax.x; x++) for (int y=snapMin.y; y<=snapMax.y; y++) {
            if (x<0 || x>=grid.length || y<0 || y>=grid[0].length) continue;
            PointD p = toPoint(new PointI(x,y));
            double ABxAp = A_xy.crossProductLength(B_xy, p);
            double BCxBp = B_xy.crossProductLength(C_xy, p);
            double CAxCp = C_xy.crossProductLength(A_xy, p);
            if (ABxAp>0 && BCxBp>0 && CAxCp>0) {
                double w = ABxAp/A_xy.crossProductLength(B_xy,C_xy);
                double v = CAxCp/C_xy.crossProductLength(A_xy, B_xy);
                double u = BCxBp/B_xy.crossProductLength(C_xy,A_xy);
                grid[x][y] = u*A.z+v*B.z+w*C.z;
                written[x][y] = true;
            }
        }
    }

    private PointI toIndex(PointD p) {
        return new PointI((int)Math.floor((p.x-min.x)/pixelSize),
                (int)Math.floor((p.y-min.y)/pixelSize));
    }
    private PointD toPoint(PointI index) {
        return new PointD(pixelSize*index.x+min.x+pixelSize/2,
                pixelSize*index.y+min.y+pixelSize/2);
    }


    public static class Vec3 {

        static final Vec3 ZERO = new Vec3(0, 0, 0);
        static final Vec3 ONE = new Vec3(1, 1, 1);

        final double x;
        final double y;
        final double z;

        Vec3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Vec3 neg() {
            return new Vec3(-x, -y, -z);
        }

        Vec3 add(Vec3 o) {
            return new Vec3(x + o.x, y + o.y, z + o.z);
        }

        Vec3 sub(Vec3 o) {
            return new Vec3(x - o.x, y - o.y, z - o.z);
        }

        Vec3 mul(Vec3 o) {
            return new Vec3(x * o.x, y * o.y, z * o.z);
        }

        Vec3 scale(double t) {
            return new Vec3(x * t, y * t, z * t);
        }

        Vec3 div(double t) {
            return new Vec3(x / t, y / t, z / t);
        }

        double dot(Vec3 o) {
            return x * o.x + y * o.y + z * o.z;
        }

        Vec3 cross(Vec3 o) {
            return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
        }

        double lengthSquared() {
            return x*x + y*y + z*z;
        }

        double length() {
            return Math.sqrt(lengthSquared());
        }

        Vec3 normalize() {
            return div(length());
        }

        boolean isNearZero() {
            double e = 0.0000008;
            return Math.abs(x) < e && Math.abs(y) < e && Math.abs(z) < e;
        }

        PointD toPoint() {
            return new PointD(x,y);
        }

        public String toString() { return String.format("[%.2f,%.2f,%.2f]", x, y, z); }
    }
}
