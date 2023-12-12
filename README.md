# Erosio

A Java API for fast erosion

## Overview

This project is a Java implementation of [Scale Terrain Generation from Tectonic Uplift and Fluvial Erosion](https://inria.hal.science/hal-01262376/document) by Guillaume Cordonnier.
All described methods excluding 3D rendering are implemented with similar computation times.


Erosio's features include:
 - Fluvial erosion with control over uplift, initial height, erosion and precision parameters
 - Interpolation of the samples' heights
 - Reusable geometry inputs for fast repeated generation
 - Polygonal erosion domains


## Example

```java
// Define bounding coordinates
RectI bounds = new RectI(-256, -256, 256, 256);

Eroder.Settings settings = new Eroder.Settings(
        /*Uplift*/ p -> 1.0, /*Initial height*/ p -> 0.0,
        /*Erosion rate*/ 2.0, /*m:n ratio*/ 0.5,
        /*Max slope*/ (p, h) -> 30.0,
        /*Time step*/ 1, /*Max iterations*/ 10, /*Convergence threshold*/ 1E-2
);

// Generate geometry input (reusable)
VoronoiDelaunay voronoiDelaunay = new VoronoiDelaunay(
        bounds.toRectD(), /*Inverse sample density*/ 2, /*Seed*/ 2
);

// Erode
Eroder.Results results = Eroder.erode(settings, voronoiDelaunay);

// Interpolate height at a point (2.0, 3.0)
double heightNN = results.interpolateNearestNeighbor(2.0, 3.0);
//or
double heightIDW = results.interpolateInverseDistanceWeighting(2.0, 3.0, /*exponent*/ 2, /*radius*/ 5);
```

<p align="center">
    <img src="images/NN.png"  alt="NN interpolated heightmap" width="40%"/>
    <img src="images/IDW.png"  alt="IDW interpolated heightmap" width="40%"/>
    <br/>
    <em>Resulting heightmap computed in 10 seconds (IDW interp. on the right)</em>
</p>

## Usage

Erosio is available via GitHub Packages. Learn more about it
[here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
(Gradle) and [here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry) (Maven)

#### Gradle
```groovy
dependencies {
    implementation 'com.github.keyboardcat1:erosio:1.1'
}
```

#### Maven
```xml
<dependency>
  <groupId>com.github.keyboardcat1</groupId>
  <artifactId>erosio</artifactId>
  <version>1.1</version>
</dependency>
```


### Dependencies

This package makes heavy use of [The Tektosyne Library ](https://github.com/kynosarges/tektosyne) and as such is a required dependency.
Tektosyne is available via mavenCentral and JCenter:

#### Graddle
```groovy
dependencies {
    implementation 'org.kynosarges:tektosyne:6.2.0'
}
```

#### Maven
```xml
<dependency>
  <groupId>org.kynosarges</groupId>
  <artifactId>tektosyne</artifactId>
  <version>6.2.0</version>
  <scope>compile</scope>
</dependency>
```

