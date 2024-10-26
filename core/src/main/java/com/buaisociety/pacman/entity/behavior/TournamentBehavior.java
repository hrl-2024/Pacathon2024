package com.buaisociety.pacman.entity.behavior;

import com.buaisociety.pacman.entity.Direction;
import com.buaisociety.pacman.entity.Entity;
import com.buaisociety.pacman.entity.PacmanEntity;
import com.cjcrafter.neat.compute.Calculator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TournamentBehavior implements Behavior {

    private final Calculator calculator;
    private @Nullable PacmanEntity pacman;

    private int previousScore = 0;
    private int framesSinceScoreUpdate = 0;

    private int numberUpdatesSincelastscone = 0;
    private int lastScore = 0;

    public TournamentBehavior(Calculator calculator) {
        this.calculator = calculator;
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
        // --- DO NOT REMOVE ---
        if (pacman == null) {
            pacman = (PacmanEntity) entity;
        }

        int newScore = pacman.getMaze().getLevelManager().getScore();
        if (previousScore != newScore) {
            previousScore = newScore;
            framesSinceScoreUpdate = 0;
        } else {
            framesSinceScoreUpdate++;
        }

        if (framesSinceScoreUpdate > 60 * 40) {
            pacman.kill();
            framesSinceScoreUpdate = 0;
        }
        // --- END OF DO NOT REMOVE ---

        // TODO: Put all your code for info into the neural network here

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

        // One hot encoding for the direction of the closest power pellet
        int minDistance = Math.min(distanceToNearestPelletForward, Math.min(distanceToNearestPelletLeft, Math.min(distanceToNearestPelletRight, distanceToNearestPelletBehind)));
        boolean closestPalletIsForward = distanceToNearestPelletForward == minDistance;
        boolean closestPalletIsLeft = distanceToNearestPelletLeft == minDistance;
        boolean closestPalletIsRight = distanceToNearestPelletRight == minDistance;
        boolean closestPalletIsBehind = distanceToNearestPelletBehind == minDistance;

        float[] inputs = new float[] {
            // TODO: Add your inputs here
            canMoveForward ? 1f : 0f,
            canMoveLeft ? 1f : 0f,
            canMoveRight ? 1f : 0f,
            canMoveBehind ? 1f : 0f,
            closestPalletIsForward ? 1f : 0f,
            closestPalletIsLeft ? 1f : 0f,
            closestPalletIsRight ? 1f : 0f,
            closestPalletIsBehind ? 1f : 0f,
        };
        float[] outputs = calculator.calculate(inputs).join();

        // Chooses the maximum output as the direction to go... feel free to change this ofc!
        // Adjust this to whatever you used in the NeatPacmanBehavior.class
        int index = 0;
        float max = outputs[0];
        for (int i = 1; i < outputs.length; i++) {
            if (outputs[i] > max) {
                max = outputs[i];
                index = i;
            }
        }

        return switch (index) {
            case 0 -> pacman.getDirection();
            case 1 -> pacman.getDirection().left();
            case 2 -> pacman.getDirection().right();
            case 3 -> pacman.getDirection().behind();
            default -> throw new IllegalStateException("Unexpected value: " + index);
        };
    }
}
