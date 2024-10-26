// ShortestPathFinder.java
package com.buaisociety.pacman.maze;

import com.buaisociety.pacman.entity.Direction;
import com.buaisociety.pacman.entity.Entity;
import com.buaisociety.pacman.entity.GhostEntity;
import kotlin.Pair;

import java.util.*;

public class ShortestPathFinder {
    private final MazeGraph mazeGraph;
    private final int pelletWeight; // Alpha parameter to control scoring emphasis on pellets

    public ShortestPathFinder(MazeGraph mazeGraph, int pelletWeight) {
        this.mazeGraph = mazeGraph;
        this.pelletWeight = pelletWeight;
    }

    // Returns a normalized score between 0 and 1 for the direction from startTile,
    // where 1 is the best (shortest path with highest points) and 0 is the worst.
    public float getNormalizedScoreToNearestPellet(Tile startTile) {
        // Step 1: Collect raw scores for each direction
        List<Integer> rawScores = new ArrayList<>();

        for (Tile directionTile : getAdjacentTiles(startTile)) {
            rawScores.add(computeRawScore(directionTile));
        }

        // Step 2: Normalize scores
        int minScore = Collections.min(rawScores);
        int maxScore = Collections.max(rawScores);

        // Calculate and return normalized scores for each direction
        return (float) rawScores.stream()
            .mapToDouble(score -> normalizeScore(score, minScore, maxScore))
            .max()  // Select the best score among all directions
            .orElse(0); // Default to 0 if no scores found
    }

    // Compute raw score for a direction using BFS to the nearest pellet or super pellet
    private int computeRawScore(Tile startTile) {
        Queue<Tile> queue = new LinkedList<>();
        Set<Tile> visited = new HashSet<>();

        Map<Tile, Integer> distance = new HashMap<>();
        Map<Tile, Integer> points = new HashMap<>();

        queue.add(startTile);
        visited.add(startTile);
        distance.put(startTile, 0);
        points.put(startTile, startTile.getState() == TileState.PELLET ? 1 : 0);

        Integer minDistanceToTarget = null;
        int maxPointsCollected = 0;
        boolean foundSuperPellet = false;

        Integer nearestRegularPelletDistance = null;
        int nearestRegularPelletPoints = 0;

        while (!queue.isEmpty()) {
            Tile current = queue.poll();
            int currentDistance = distance.get(current);
            int currentPoints = points.get(current);

            // Check if current tile is a super pellet or regular pellet
            if (current.getState() == TileState.POWER_PELLET || current.getState() == TileState.PELLET) {
                if (current.getState() == TileState.POWER_PELLET) {
                    foundSuperPellet = true;
                } else if (nearestRegularPelletDistance == null || currentDistance < nearestRegularPelletDistance) {
                    // Save the nearest regular pellet distance and points if we haven't found a super pellet
                    nearestRegularPelletDistance = currentDistance;
                    nearestRegularPelletPoints = currentPoints;
                }

                // Update target only if this is the first valid target or if it has a shorter distance
                // or more points for the same distance
                if (minDistanceToTarget == null ||
                    currentDistance < minDistanceToTarget ||
                    (currentDistance == minDistanceToTarget && currentPoints > maxPointsCollected)) {
                    minDistanceToTarget = currentDistance;
                    maxPointsCollected = currentPoints;
                }

                // Stop searching if we've found the nearest super pellet
                if (foundSuperPellet && currentDistance <= minDistanceToTarget) {
                    break;
                }
            }

            // Explore neighbors
            for (Tile neighbor : mazeGraph.getAdjList().get(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);

                    int newDistance = currentDistance + 1;
                    int newPoints = currentPoints + (neighbor.getState() == TileState.PELLET ? 1 : 0);

                    queue.add(neighbor);
                    distance.put(neighbor, newDistance);
                    points.put(neighbor, newPoints);
                }
            }
        }

        // Use the nearest regular pellet distance and points if no super pellet was found
        int finalDistance = foundSuperPellet ? minDistanceToTarget : (nearestRegularPelletDistance != null ? nearestRegularPelletDistance : Integer.MAX_VALUE);
        int finalPoints = foundSuperPellet ? maxPointsCollected : nearestRegularPelletPoints;

        return finalDistance - pelletWeight * finalPoints;
    }

    // Normalize score to the range [0, 1] where 1 is best and 0 is worst
    private double normalizeScore(int score, int minScore, int maxScore) {
        if (maxScore == minScore) {
            return 1.0;  // Avoid division by zero; all directions are equally favorable
        }
        return 1.0 - ((double) (score - minScore) / (maxScore - minScore));
    }

    // Get adjacent tiles (up, down, left, right) from the current tile
    private List<Tile> getAdjacentTiles(Tile startTile) {
        return new ArrayList<>(mazeGraph.getAdjList().get(startTile));
    }

    // Calculate distance to the nearest pellet (regular or super) from the start tile
    public int getDistanceToNearestPellet(Tile startTile) {
        Queue<Tile> queue = new LinkedList<>();
        Set<Tile> visited = new HashSet<>();
        Map<Tile, Integer> distance = new HashMap<>();

        queue.add(startTile);
        visited.add(startTile);
        distance.put(startTile, 0);

        while (!queue.isEmpty()) {
            Tile current = queue.poll();
            int currentDistance = distance.get(current);

            if (current.getState() == TileState.PELLET || current.getState() == TileState.POWER_PELLET) {
                return currentDistance;
            }

            for (Tile neighbor : mazeGraph.getAdjList().get(current)) {
                if (!visited.contains(neighbor)) {
                    queue.add(neighbor);
                    visited.add(neighbor);
                    distance.put(neighbor, currentDistance + 1);
                }
            }
        }

        return Integer.MAX_VALUE; // No pellet found
    }

    // Calculate distance to the nearest ghost from the start tile
    public int getDistanceToNearestGhost(Tile startTile) {
        Queue<Tile> queue = new LinkedList<>();
        Set<Tile> visited = new HashSet<>();
        Map<Tile, Integer> distance = new HashMap<>();

        queue.add(startTile);
        visited.add(startTile);
        distance.put(startTile, 0);

        while (!queue.isEmpty()) {
            Tile current = queue.poll();
            int currentDistance = distance.get(current);

            // Check if the current tile has a ghost
            for (Entity entity : current.getMaze().getEntities()) {
                if (entity instanceof GhostEntity && entity.getTilePosition().equals(current.getPosition())) {
                    return currentDistance;
                }
            }

            for (Tile neighbor : mazeGraph.getAdjList().get(current)) {
                if (!visited.contains(neighbor)) {
                    queue.add(neighbor);
                    visited.add(neighbor);
                    distance.put(neighbor, currentDistance + 1);
                }
            }
        }

        return Integer.MAX_VALUE; // No ghost found
    }

    public Pair<Integer, Direction> getDistanceAndDirectionToNearestPowerPellet(Tile startTile) {
        Queue<Tile> queue = new LinkedList<>();
        Set<Tile> visited = new HashSet<>();
        Map<Tile, Integer> distance = new HashMap<>();
        Map<Tile, Direction> directionMap = new HashMap<>();

        queue.add(startTile);
        visited.add(startTile);
        distance.put(startTile, 0);

        for (Direction direction : Direction.values()) {
            Tile neighbor = startTile.getNeighbor(direction);
            if (neighbor.getState().isPassable()) {
                queue.add(neighbor);
                visited.add(neighbor);
                distance.put(neighbor, 1);
                directionMap.put(neighbor, direction);
            }
        }

        while (!queue.isEmpty()) {
            Tile current = queue.poll();
            int currentDistance = distance.get(current);

            if (current.getState() == TileState.POWER_PELLET) {
                Direction initialDirection = directionMap.get(current);
                return new Pair<>(currentDistance, initialDirection);
            }

            for (Tile neighbor : getAdjacentTiles(current)) {
                if (!visited.contains(neighbor) && neighbor.getState().isPassable()) {
                    queue.add(neighbor);
                    visited.add(neighbor);
                    distance.put(neighbor, currentDistance + 1);
                    directionMap.put(neighbor, directionMap.getOrDefault(current, null));
                }
            }
        }

        return new Pair<>(Integer.MAX_VALUE, null); // No PowerPellet found
    }
}
