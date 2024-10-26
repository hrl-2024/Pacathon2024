package com.buaisociety.pacman.entity.behavior;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.buaisociety.pacman.maze.Maze;
import com.buaisociety.pacman.maze.Tile;
import com.buaisociety.pacman.sprite.DebugDrawing;
import com.cjcrafter.neat.Client;
import com.buaisociety.pacman.entity.Direction;
import com.buaisociety.pacman.entity.Entity;
import com.buaisociety.pacman.entity.PacmanEntity;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

public class NeatPacmanBehavior implements Behavior {

    private final @NotNull Client client;
    private @Nullable PacmanEntity pacman;

    // Score modifiers help us maintain "multiple pools" of points.
    // This is great for training, because we can take away points from
    // specific pools of points instead of subtracting from all.
    private int scoreModifier = 0;

    private int numberUpdatesSincelastscone = 0;
    private int lastScore = 0;

    public NeatPacmanBehavior(@NotNull Client client) {
        this.client = client;
    }

    /**
     * Returns the desired direction that the entity should move towards.
     *
     * @param entity the entity to get the direction for
     * @return the desired direction for the entity
     */
    @NotNull
    @Override
    public Direction getDirection(@NotNull Entity entity) {
        if (pacman == null) {
            pacman = (PacmanEntity) entity;
        }

        // SPECIAL TRAINING CONDITIONS
        // TODO: Make changes here to help with your training...
        // END OF SPECIAL TRAINING CONDITIONS

        // If pacman got stuck (not collecting anything anymore), kill it
        int newScore = pacman.getMaze().getLevelManager().getScore();
        if (newScore > lastScore) {
            lastScore = newScore;
            numberUpdatesSincelastscone = 0;
        }

        // 60 updates per seconds * 10 seconds
        if (numberUpdatesSincelastscone++ > 60 * 10) {
            pacman.kill();
            return Direction.UP;
        }

        // We are going to use these directions a lot for different inputs. Get them all once for clarity and brevity
        Direction forward = pacman.getDirection();
        Direction left = pacman.getDirection().left();
        Direction right = pacman.getDirection().right();
        Direction behind = pacman.getDirection().behind();

        // Input nodes 1, 2, 3, and 4 show if the pacman can move in the forward, left, right, and behind directions
        boolean canMoveForward = pacman.canMove(forward);
        boolean canMoveLeft = pacman.canMove(left);
        boolean canMoveRight = pacman.canMove(right);
        boolean canMoveBehind = pacman.canMove(behind);

        // Nearest distance to Pallet:
        int distanceToNearestPelletForward = pacman.getDistanceToNearestPellet(forward);
        int distanceToNearestPelletLeft = pacman.getDistanceToNearestPellet(left);
        int distanceToNearestPelletRight = pacman.getDistanceToNearestPellet(right);
        int distanceToNearestPelletBehind = pacman.getDistanceToNearestPellet(behind);

        // Nearest distance to ghost:
        int distanceToGhostForward = pacman.getDistanceToNearestGhost(forward);
        int distanceToGhostLeft = pacman.getDistanceToNearestGhost(left);
        int distanceToGhostRight = pacman.getDistanceToNearestGhost(right);
        int distanceToGhostBehind = pacman.getDistanceToNearestGhost(behind);

        // One hot encoding for the direction of the closest power pellet
        int minDistance = Math.min(distanceToNearestPelletForward, Math.min(distanceToNearestPelletLeft, Math.min(distanceToNearestPelletRight, distanceToNearestPelletBehind)));
        boolean closestPalletIsForward = distanceToNearestPelletForward == minDistance;
        boolean closestPalletIsLeft = distanceToNearestPelletLeft == minDistance;
        boolean closestPalletIsRight = distanceToNearestPelletRight == minDistance;
        boolean closestPalletIsBehind = distanceToNearestPelletBehind == minDistance;

        // Get the fruit position
        Vector2i fruitPosition = pacman.getMaze().getFruitPosition();
        if (fruitPosition != null) {
            // Calculate direction to the fruit
            Tile pacmanTile = pacman.getMaze().getTile(pacman.getTilePosition());
            Tile fruitTile = pacman.getMaze().getTile(fruitPosition);
            Direction directionToFruit = pacmanTile.getDirectionTo(fruitTile);

            // Use the direction to the fruit in your NEAT algorithm inputs
            boolean fruitIsForward = directionToFruit == pacman.getDirection();
            boolean fruitIsLeft = directionToFruit == pacman.getDirection().left();
            boolean fruitIsRight = directionToFruit == pacman.getDirection().right();
            boolean fruitIsBehind = directionToFruit == pacman.getDirection().behind();
        }

        boolean ghostLeft = pacman.dfsCheckForGhost(left);
        boolean ghostRight = pacman.dfsCheckForGhost(right);
        boolean ghostForward = pacman.dfsCheckForGhost(forward);
        boolean ghostBehind = pacman.dfsCheckForGhost(behind);

        // Get the current score and number of pellets left
        int pelletsLeft = pacman.getMaze().getPelletsRemaining();

        // Check if Pacman should eat a PowerPellet
        boolean shouldEatPowerPellet = (distanceToGhostForward < 5 || distanceToGhostLeft < 5 || distanceToGhostRight < 5 || distanceToGhostBehind < 5);
        // Get closest power pellet direction and distance
        Pair<Integer, Direction> closestPowerPellet = pacman.getDistanceAndDirectionToNearestPelletAndGhost();
        int distanceToClosestPowerPellet = closestPowerPellet.getFirst();
        Direction directionToClosestPowerPellet = closestPowerPellet.getSecond();
        boolean closestPowerPelletIsForward = directionToClosestPowerPellet == forward;
        boolean closestPowerPelletIsLeft = directionToClosestPowerPellet == left;
        boolean closestPowerPelletIsRight = directionToClosestPowerPellet == right;
        boolean closestPowerPelletIsBehind = directionToClosestPowerPellet == behind;
        boolean isInSuperMode = pacman.isInSuperMode();

        float[] outputs = client.getCalculator().calculate(new float[]{
            canMoveForward ? 1f : 0f,
            canMoveLeft ? 1f : 0f,
            canMoveRight ? 1f : 0f,
            canMoveBehind ? 1f : 0f,
            closestPalletIsForward ? 1f : 0f,
            closestPalletIsLeft ? 1f : 0f,
            closestPalletIsRight ? 1f : 0f,
            closestPalletIsBehind ? 1f : 0f,
            ghostLeft ? 1f : 0f,
            ghostRight ? 1f : 0f,
            ghostForward ? 1f : 0f,
            ghostBehind ? 1f : 0f,
        }).join();

        //            closestPalletIsForward ? 1f : 0f,
        //            closestPalletIsLeft ? 1f : 0f,
        //            closestPalletIsRight ? 1f : 0f,
        //            closestPalletIsBehind ? 1f : 0f,

//        distanceToGhostForward,
//            distanceToGhostLeft,
//            distanceToGhostRight,
//            distanceToGhostBehind,
//        shouldEatPowerPellet ? 1f : 0f,
//            distanceToClosestPowerPellet,
//            closestPowerPelletIsForward ? 1f : 0f,
//            closestPowerPelletIsLeft ? 1f : 0f,
//            closestPowerPelletIsRight ? 1f : 0f,
//            closestPowerPelletIsBehind ? 1f : 0f,
//            isInSuperMode ? 1f : 0f,

        int index = 0;
        float max = outputs[0];
        for (int i = 1; i < outputs.length; i++) {
            if (outputs[i] > max) {
                max = outputs[i];
                index = i;
            }
        }

        Direction newDirection = switch (index) {
            case 0 -> pacman.getDirection();
            case 1 -> pacman.getDirection().left();
            case 2 -> pacman.getDirection().right();
            case 3 -> pacman.getDirection().behind();
            default -> throw new IllegalStateException("Unexpected value: " + index);
        };

        client.setScore(pacman.getMaze().getLevelManager().getScore() + scoreModifier);
        return newDirection;
    }

    @Override
    public void render(@NotNull SpriteBatch batch) {
        // TODO: You can render debug information here
        /*
        if (pacman != null) {
            DebugDrawing.outlineTile(batch, pacman.getMaze().getTile(pacman.getTilePosition()), Color.RED);
            DebugDrawing.drawDirection(batch, pacman.getTilePosition().x() * Maze.TILE_SIZE, pacman.getTilePosition().y() * Maze.TILE_SIZE, pacman.getDirection(), Color.RED);
        }
         */
    }
}
