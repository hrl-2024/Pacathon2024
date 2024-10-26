// ShortestPathFinder.java
package com.buaisociety.pacman.maze;

import com.buaisociety.pacman.entity.Entity;
import com.buaisociety.pacman.entity.GhostEntity;

import java.util.*;

public class ShortestPathFinder {
    private final MazeGraph mazeGraph;

    public ShortestPathFinder(MazeGraph mazeGraph) {
        this.mazeGraph = mazeGraph;
    }

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
}
