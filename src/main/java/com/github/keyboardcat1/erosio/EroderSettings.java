package com.github.keyboardcat1.erosio;

import org.kynosarges.tektosyne.geometry.PointD;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The input settings for {@link Eroder} <br/>
 * Example geological values, i.e. for scales ~100km:
 * <ul>
 *     <li>uplift: <5.0 10^-4 m/y</li>
 *     <li>initial height: <2.0 10^3 m</li>
 *     <li>erosion rate: ~5.61 10^-7 1/y</li>
 *     <li>m:n ratio: ~0.50</li>
 *     <li>maximum slope: 30-60 degrees</li>
 *     <li>time step: 2.5 10^5 y</li>
 * </ul>
 *
 * @param upliftLambda          A 2D map returning the uplift at a point
 * @param initialHeightLambda   A 2D map returning the initial height at a point
 * @param erosionRateLambda     A 2D map returning the erosion rate at a point, generally varying with climate
 * @param mnRatio               A value between 0 and 1 controlling the nature of the erosion (see stream power equation)
 * @param maxSlopeDegreesLambda A 3D map (point and height) returning the maximum slope due to thermal erosion, in degrees, generally varying with stone type
 * @param timeStep              The simulated time taken between erosion cycles
 * @param maxIterations         The maximum number of erosion cycles
 * @param convergenceThreshold  The maximum height difference between two erosion cycles dictating when they should cease
 */
public record EroderSettings(Function<PointD, Double> upliftLambda, Function<PointD, Double> initialHeightLambda,
                             Function<PointD, Double> erosionRateLambda, double mnRatio,
                             BiFunction<PointD, Double, Double> maxSlopeDegreesLambda,
                             double timeStep, int maxIterations, double convergenceThreshold) {
}
