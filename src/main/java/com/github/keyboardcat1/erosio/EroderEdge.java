package com.github.keyboardcat1.erosio;

import org.kynosarges.tektosyne.geometry.PointD;

/**
 * A river segment on a fluvial network
 *
 * @param origin The point where water flows from
 * @param destination The point where water flows to
 * @param volumeOrigin The volume of water that flows through the origin
 * @param volumeDestination The volume of water that flows through the destination
 */
public record EroderEdge(PointD origin, PointD destination, double volumeOrigin, double volumeDestination) {
}
