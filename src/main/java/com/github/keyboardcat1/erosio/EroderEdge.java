package com.github.keyboardcat1.erosio;

import org.kynosarges.tektosyne.geometry.PointD;

/**
 * A river segment on a fluvial network
 */
public final class EroderEdge {
    /**
     * The point where water flows from
     */
    public final PointD origin;
    /**
     * The point where water flows to
     */
    public final PointD destination;
    /**
     * The volume of water that flows through the origin
     */
    public final double volumeOrigin;
    /**
     * The volume of water that flows through the destination
     */
    public final double volumeDestination;

    EroderEdge(PointD origin, PointD destination, double volumeOrigin, double volumeDestination) {
        this.origin = origin;
        this.destination = destination;
        this.volumeOrigin = volumeOrigin;
        this.volumeDestination = volumeDestination;
    }
}
