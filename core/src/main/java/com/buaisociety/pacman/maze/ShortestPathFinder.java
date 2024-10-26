// ShortestPathFinder.java
package com.buaisociety.pacman.maze;

import com.buaisociety.pacman.entity.Direction;
import com.buaisociety.pacman.entity.Entity;
import com.buaisociety.pacman.entity.EntityType;
import com.buaisociety.pacman.entity.GhostEntity;
import kotlin.Pair;

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

    private List<Tile> getAdjacentTiles(Tile startTile) {
        return new ArrayList<>(mazeGraph.getAdjList().get(startTile));
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

    public boolean dfsCheckForGhost(Tile startTile, Tile directionTile) {
        Set<Tile> visited = new HashSet<>();
        visited.add(startTile); // Mark Pacman's current position as visited
        Tile current = directionTile; // Start from the tile in the specified direction

        int counter = 0;

        // Traverse through the hallway until a root is found
        while (getUnvisitedNeighborCount(current, visited) == 1) {
            counter++;
            // Check for a ghost immediately upon visiting each tile
            if (containsGhost(current)) {
                System.out.printf("Ghost found at %s\n", current.getPosition());
                return true;
            }

            // Move to the next tile in the hallway
            for (Tile neighbor : mazeGraph.getAdjList().get(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    current = neighbor;
                    break;
                }
            }
        }

        if (counter > 8) {
            return false; // No ghost found within 8 tiles
        }

        // Check the current tile (root) for a ghost
        if (containsGhost(current)) {
            System.out.printf("Ghost found at %s\n", current.getPosition());
            return true;
        }

        // Start a countdown when a root is reached
        int countdown = 2;
        Stack<Tile> stack = new Stack<>();
        stack.push(current);

        while (!stack.isEmpty() && countdown > 0) {
            Tile tile = stack.pop();
            visited.add(tile);

            // Check for a ghost on every visited tile during countdown
            if (containsGhost(tile)) {
                System.out.printf("Ghost found at %s\n", tile.getPosition());
                return true;
            }

            // Add unvisited neighbors to the stack
            for (Tile neighbor : mazeGraph.getAdjList().get(tile)) {
                if (!visited.contains(neighbor)) {
                    stack.push(neighbor);
                    visited.add(neighbor);
                }
            }

            // Decrease the countdown after expanding one depth level
            countdown--;
        }

        return false; // No ghost found within two tiles from the root
    }



    private boolean containsGhost(Tile t){
        for (Entity entity : t.getMaze().getEntities()) {
            if (entity instanceof GhostEntity && entity.getTilePosition().equals(t.getPosition())) {
                return true;
            }
        }
        return false;
    }

    private int getUnvisitedNeighborCount(Tile tile, Set<Tile> visited) {
        int count = 0;
        for (Tile neighbor : mazeGraph.getAdjList().get(tile)) {
            if (!visited.contains(neighbor)) {
                count++;
            }
        }
        return count;
    }
}
