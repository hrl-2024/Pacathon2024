package com.buaisociety.pacman.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.buaisociety.pacman.entity.behavior.AggressiveChaseBehavior;
import com.buaisociety.pacman.entity.behavior.Behavior;
import com.buaisociety.pacman.maze.*;
import com.buaisociety.pacman.sprite.GrayscaleSpriteSheet;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2d;
import org.joml.Vector2i;

public class PacmanEntity extends Entity {

    private final GrayscaleSpriteSheet spriteSheet;
    private int animationFrame;

    private final @NotNull Vector2i spawnPixel;
    private final @NotNull Behavior behavior;
    private int freezeTicks;
    private boolean isAlive = true;

    // For graph traversal: the maze graph and the shortest pathfinder
    private final MazeGraph graph;
    private final ShortestPathFinder pathFinder;

    public PacmanEntity(@NotNull Maze maze, @NotNull Config config) {
        super(maze, EntityType.PACMAN);

        this.behavior = config.behavior;
        this.spawnPixel = config.spawnPixel;
        reset();

        // This sprite sheet is 3x4 tiled sprite sheet, each tile is 20x20 pixels
        this.spriteSheet = config.spriteSheet;
        this.spriteSheet.setColors(Color.CLEAR, Color.YELLOW);

        this.graph = new MazeGraph(maze);
        this.pathFinder = new ShortestPathFinder(graph);
    }

    public int getDistanceToNearestPellet(Direction direction) {
        Tile startTile = maze.getTile(getTilePosition());
        Tile neighborTile = startTile.getNeighbor(direction);
        if (!neighborTile.getState().isPassable()) {
            return Integer.MAX_VALUE;
        }
        return pathFinder.getDistanceToNearestPellet(neighborTile);
    }

    public int getDistanceToNearestGhost(Direction direction) {
        Tile startTile = maze.getTile(getTilePosition());
        Tile neighborTile = startTile.getNeighbor(direction);
        if (!neighborTile.getState().isPassable()) {
            return Integer.MAX_VALUE;
        }
        return pathFinder.getDistanceToNearestGhost(neighborTile);
    }

    @Override
    public void reset() {
        animationFrame = 0;
        freezeTicks = 0;
        isAlive = true;

        setPosition(new Vector2d(spawnPixel));
        this.direction = Direction.UP;
    }

    @Override
    public double getSpeed() {
        // These constants are taken straight out of the Pacman game
        int level = maze.getLevelManager().getLevel();

        // Allow a handicap to delay the speed changes per-level
        // makes the game easier
        level = Math.max(1, level - maze.getLevelManager().getConfig().handicap);

        if (maze.getFrightenedTimer() > 0) {
            if (level >= 5) {
                return BASE_SPEED * 1.00;
            } else if (level >= 2) {
                return BASE_SPEED * 0.95;
            } else {
                return BASE_SPEED * 0.90;
            }
        } else {
            if (level >= 21) {
                return BASE_SPEED * 0.90;
            } else if (level >= 5) {
                return BASE_SPEED * 1.00;
            } else if (level >= 2) {
                return BASE_SPEED * 0.90;
            } else {
                return BASE_SPEED * 0.80;
            }
        }
    }

    public Pair<Integer, Direction> getDistanceAndDirectionToNearestPelletAndGhost() {
        Tile startTile = maze.getTile(getTilePosition());
        return pathFinder.getDistanceAndDirectionToNearestPowerPellet(startTile);
    }

    public boolean isInSuperMode() {
        return maze.getFrightenedTimer() > 0;
    }

    @Override
    public @NotNull Behavior getBehavior() {
        return behavior;
    }

    /**
     * Returns true if the pacman is alive (has not been killed by a ghost).
     *
     * @return true if the pacman is alive.
     */
    public boolean isAlive() {
        return isAlive;
    }

    public void kill() {
        isAlive = false;
    }

    @Override
    public void update() {
        super.update();

        // Pacman will freeze for a few ticks when eating pellets and power pellets
        if (freezeTicks > 0) {
            freezeTicks--;
            return;
        }

        // Increment the animation frame
        if (ticksAlive % 2 == 0)
            animationFrame++;

        Behavior behavior = getBehavior();
        direction = behavior.getDirection(this);
        if (canMove(direction)) {
            move(direction, getSpeed(), true);
        }

        // Eat pellets
        Tile tile = maze.getTile(maze.toTileCoords(position));
        TileState pellet = maze.eatPellet(this, tile);
        if (pellet == TileState.PELLET) {
            freezeTicks += 1;
        } else if (pellet == TileState.POWER_PELLET) {
            freezeTicks += 3;
        }
    }

    @Override
    public void render(@NotNull SpriteBatch batch) {
        behavior.render(batch);

        // Render the current tile of the sprite sheet
        switch (animationFrame % 4) {
            case 0 -> spriteSheet.setCurrentTile(0, direction.ordinal());
            case 1, 3 -> spriteSheet.setCurrentTile(1, direction.ordinal());
            case 2 -> spriteSheet.setCurrentTile(2, direction.ordinal());
        }
        int pixelX = (int) position.x() - spriteSheet.getTileSize().x() / 2 + 1;
        int pixelY = (int) position.y() - spriteSheet.getTileSize().y() / 2 + 1;
        spriteSheet.render(batch, pixelX, pixelY);
    }

    @Override
    public void dispose() {
        spriteSheet.dispose();
    }


    public static class Config {
        public @NotNull Behavior behavior = new AggressiveChaseBehavior();
        public @NotNull Vector2i spawnPixel = new Vector2i();
        public @NotNull GrayscaleSpriteSheet spriteSheet = new GrayscaleSpriteSheet(new Texture("sprites/pacman-sprite.png"), 20);
    }
}
