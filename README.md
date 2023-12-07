# Erosio

A Java API for fast erosion

---

## Description

This project is a Java implementation of [Scale Terrain Generation from Tectonic Uplift and Fluvial Erosion](https://inria.hal.science/hal-01262376/document) by Guillaume Cordonnier.

This erosion algorithm takes in an uplift and initial height map to produce eroded terrain with a realistic hydrology network.
All the examples below compute under 10 seconds.

## Usage

### Example

```java
// Define bounding coordinates
RectI bounds = new RectI(-256, -256, 256, 256);

Eroder.Settings settings = new Eroder.Settings(
        /*Uplift*/ p -> 1.0, /*Initial height*/ p -> 0.0,
        /*Erosion factor*/ p -> 2.0, /*mnRatio*/ 0.5, /*Max slope*/ 60,
        /*Time step*/ 1, /*Max iterations*/ 10, /*Convergence threshold*/ 1E-2
);

// Generate a Voronoi tesselation and a Delaunay triangulation (reusable)
VoronoiDelaunay voronoiDelaunay = new VoronoiDelaunay(
        bounds.toRectD(), /*Inverse sample density*/ 2, /*Seed*/ 2
);

// Erode
Eroder.Results results = Eroder.erode(settings, voronoiDelaunay);

// Interpolate height at a point (2.0, 3.0)
double heightNN = results.interpolateNearestNeighbor(2.0, 3.0);
//or
double heightIDW = results.interpolateInverseDistanceWeighted(2.0, 3.0);
```

<p align="center">
    <img src="images/NN.png"  alt="NN interpolated heightmap" width="40%"/>
    <img src="images/IDW.png"  alt="IDW interpolated heightmap" width="40%"/>
    <br/>
    <em>Resulting heightmap for the settings above (IDW on the right)</em>
</p>

#
