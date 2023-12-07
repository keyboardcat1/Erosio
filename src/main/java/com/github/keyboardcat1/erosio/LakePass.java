package com.github.keyboardcat1.erosio;

import org.kynosarges.tektosyne.geometry.PointD;

import java.util.Objects;

record LakePass(PointD rootFrom, PointD rootTo, PointD passFrom, PointD passTo,
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

    @Override
    public String toString() {
        return "LakePass{" +
                "rootFrom=" + rootFrom +
                ", passTo=" + passTo +
                ", passHeight=" + passHeight +
                '}';
    }
}