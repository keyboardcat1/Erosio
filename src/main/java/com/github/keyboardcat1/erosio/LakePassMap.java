package com.github.keyboardcat1.erosio;

import org.kynosarges.tektosyne.geometry.PointD;

import java.util.*;

class LakePassMap {
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
