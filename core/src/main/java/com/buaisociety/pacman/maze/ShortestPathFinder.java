package com.buaisociety.pacman.maze;

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
}
