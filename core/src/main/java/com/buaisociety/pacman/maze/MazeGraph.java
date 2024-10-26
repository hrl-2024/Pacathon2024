package com.buaisociety.pacman.maze;

import com.buaisociety.pacman.entity.Direction;

import java.util.*;

public class MazeGraph {
    private final Map<Tile, List<Tile>> adjList = new HashMap<>();

    public MazeGraph(Maze maze) {
        for (int y = 0; y < maze.getDimensions().y(); y++) {
            for (int x = 0; x < maze.getDimensions().x(); x++) {
                Tile tile = maze.getTile(x, y);
                if (tile.getState().isPassable()) {
                    List<Tile> neighbors = new ArrayList<>();
                    for (Direction direction : Direction.values()) {
                        Tile neighbor = tile.getNeighbor(direction);
                        if (neighbor.getState().isPassable()) {
                            neighbors.add(neighbor);
                        }
                    }
                    adjList.put(tile, neighbors);
                }
            }
        }
    }

    public Map<Tile, List<Tile>> getAdjList() {
        return adjList;
    }
}
