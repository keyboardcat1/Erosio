package com.github.keyboardcat1.erosio;

import org.kynosarges.tektosyne.geometry.PointD;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The input settings for {@link Eroder}
 *
 * @param upliftLambda          A surface function taking in a {@link PointD} and returning the uplift at that 2D point
 * @param initialHeightLambda   A surface function taking in a {@link PointD} and returning the initial height at that 2D point
 * @param erosionRate           A coefficient controlling how much water cuts in an erosion cycle
 * @param mnRatio               A value between 0 and 1 controlling the nature of the erosion (see stream power equation)
 * @param maxSlopeDegreesLambda A volume function taking in a {@link PointD} and a height and returning the maximum slope due to thermal erosion in degrees at that 3D point
 * @param timeStep              The simulated time taken between erosion cycles
 * @param maxIterations         The maximum number of erosion cycles
 * @param convergenceThreshold  The maximum height difference between two erosion cycles dictating when they should cease
 */
public record EroderSettings(Function<PointD, Double> upliftLambda, Function<PointD, Double> initialHeightLambda,
                             double erosionRate, double mnRatio,
                             BiFunction<PointD, Double, Double> maxSlopeDegreesLambda,
                             double timeStep, int maxIterations, double convergenceThreshold) {
}
