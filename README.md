# Orbit Tracker

A small reactive service for satellite position calculation.

This project is a work-in-progress backend service that calculates satellite coordinates and trajectories using CelesTrak data. It uses the Orekit library for orbital mechanics and is built on a reactive stack.

## Features

* Reactive Stack: Built with Spring WebFlux and Project Reactor for non-blocking operations.

* Orbital Calculation: Uses Orekit for satellite propagation and coordinate transformations.

* Caching: TLE data is cached for 90 minutes to minimize external API calls.

* Async Processing: Heavy calculations are handled on background threads to maintain responsiveness.

### Technical Details
* Reference Frames: Handles transformations between TEME (TLE default) and ITRF (Geodetic) frames.

* Resource Management: Frames and Earth models are initialized as Singleton Beans to avoid unnecessary object creation.

* Concurrency: Uses ConcurrentHashMap and Mono.cache for basic thread-safe caching of processed data.

### Tech Stack
* Java 17

* Spring Boot 3

* Orekit

* Lombok

### Current Endpoints

```bash
POST /api/v1/satellites/satellitePosition 
```
Returns current latitude, longitude, and altitude for a specific satellite.

```bash
POST /api/v1/satellites/trajectory 
```
Returns a list of 100 points representing the satellite's path over one orbital period.

### Status
[x] Basic TLE client and caching.

[x] Position and trajectory calculation logic.

[x] Reactive thread management.

[ ] Unit and integration tests.

[ ] Frontend integration with Resium.
