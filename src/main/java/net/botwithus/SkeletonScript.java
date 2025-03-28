package net.botwithus;

import net.botwithus.api.game.world.Traverse;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.config.ScriptConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SkeletonScript extends LoopingScript {
    // Configuration constants
    private static final int OBSTACLE_TIMEOUT_MS = 20_000;
    private static final int BASE_INTERACTION_DELAY = 300;
    private static final int MAX_RANDOM_DELAY = 600;
    private static final int TARGET_AREA_SIZE = 3;
    private int lapsCompleted = 0;

    private BotState botState = BotState.IDLE;
    private boolean isCoolCheckbox = true;
    private final Random random = new Random();
    final AgilityCourseHandler agilityHandler;

    public int getLapsCompleted() {

        return lapsCompleted;

    }

    public void incrementLapCount() {
        lapsCompleted++;
    }


    enum BotState {
        IDLE,
        SKILLING
    }

    public SkeletonScript(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        this.sgc = new SkeletonScriptGraphicsContext(getConsole(), this);
        this.agilityHandler = new AgilityCourseHandler();
    }

    @Override
    public void onLoop() {
        final LocalPlayer player = Client.getLocalPlayer();
        if (!shouldExecute(player)) {
            Execution.delay(random.nextLong(3000, 7000));
            return;
        }

        if (botState == BotState.SKILLING) {
            agilityHandler.handleCourse(player);
            Execution.delay(random.nextLong(100, 300));
        }
    }

    private boolean shouldExecute(LocalPlayer player) {
        return player != null
                && Client.getGameState() == Client.GameState.LOGGED_IN
                && botState != BotState.IDLE;
    }

    /**
     * Handles agility course navigation and obstacle interaction logic
     */
    public static class AgilityCourseHandler {
        public String getCurrentObstacleName() {
            return courseObstacles.get(currentObstacleIndex).getName();
        }

        /**
         * Represents an agility course obstacle with interaction requirements
         */
        private static class Obstacle {
            private final String name;
            private final String action;
            private final Area targetArea;
            private final Coordinate expectedPosition;

            public Obstacle(String name, String action, Coordinate expectedPosition) {
                this.name = name;
                this.action = action;
                this.expectedPosition = expectedPosition;
                this.targetArea = new Area.Rectangular(
                        expectedPosition,
                        TARGET_AREA_SIZE,
                        TARGET_AREA_SIZE
                );
            }

            public String getName() { return name; }
            public String getAction() { return action; }
            public Area getTargetArea() { return targetArea; }
            public Coordinate getExpectedPosition() { return expectedPosition; }
        }

        private final List<Obstacle> courseObstacles;
        private int currentObstacleIndex = 0;
        private boolean isHandlingObstacle = false;
        private long actionTimeout = 0;
        private final Random random = new Random();

        public AgilityCourseHandler() {
            this.courseObstacles = new ArrayList<>(List.of(
                    new Obstacle("Rope swing", "Swing-on", new Coordinate(2551, 3549, 0)),
                    new Obstacle("Log balance", "Walk-across", new Coordinate(2541, 3546, 0)),
                    new Obstacle("Wall", "Run-up", new Coordinate(2538, 3545, 2)),
                    new Obstacle("Wall", "Climb-up", new Coordinate(2536, 3546, 3)),
                    new Obstacle("Spring device", "Fire", new Coordinate(2532, 3553, 3)),
                    new Obstacle("Balance beam", "Cross", new Coordinate(2536, 3553, 3)),
                    new Obstacle("Gap", "Jump-over", new Coordinate(2538, 3553, 2)),
                    new Obstacle("Roof", "Slide-down", new Coordinate(2544, 3553, 0))
            ));
        }

        /**
         * Handles the complete agility course cycle for a player
         * @param player The local player to handle course navigation for
         */
        public void handleCourse(LocalPlayer player) {
            final Obstacle current = courseObstacles.get(currentObstacleIndex);
            final Coordinate playerPos = player.getCoordinate();

            if (isHandlingObstacle) {
                handleObstacleCompletion(playerPos, current);
                return;
            }

            if (shouldSkipInteraction(player, current)) {
                return;
            }

            if (!isInPreviousObstacleArea(playerPos)) {
                handlePositionRecovery(current, playerPos);
                return;
            }

            attemptObstacleInteraction(current, player);
        }

        private void handleObstacleCompletion(Coordinate playerPos, Obstacle current) {
            if (current.getTargetArea().contains(playerPos)
                    || System.currentTimeMillis() > actionTimeout) {
                isHandlingObstacle = false;
                currentObstacleIndex = (currentObstacleIndex + 1) % courseObstacles.size();
                ScriptConsole.println("[AGILITY] Progressing to: "
                        + courseObstacles.get(currentObstacleIndex).getName());
            }
        }

        private boolean shouldSkipInteraction(LocalPlayer player, Obstacle current) {
            return player.isMoving()
                    || (!current.getAction().equals("Jump-over") && player.getAnimationId() != -1);
        }

        private boolean isInPreviousObstacleArea(Coordinate playerPos) {
            return currentObstacleIndex == 0
                    || courseObstacles.get(currentObstacleIndex - 1)
                    .getTargetArea().contains(playerPos);
        }

        private void handlePositionRecovery(Obstacle current, Coordinate playerPos) {
            ScriptConsole.println("[NAVIGATION] Moving to required position from: " + playerPos);
            Traverse.to(current.getExpectedPosition());
        }

        private void attemptObstacleInteraction(Obstacle current, LocalPlayer player) {
            SceneObjectQuery query = SceneObjectQuery.newQuery().name(current.getName());
            SceneObject obstacle = query.results().nearest();

            if (obstacle != null && obstacle.interact(current.getAction())) {
                ScriptConsole.println("[ACTION] Attempting: "
                        + current.getAction() + " " + current.getName());
                isHandlingObstacle = true;
                actionTimeout = System.currentTimeMillis() + OBSTACLE_TIMEOUT_MS;
                Execution.delay(BASE_INTERACTION_DELAY + random.nextInt(MAX_RANDOM_DELAY));
            } else {
                ScriptConsole.println("[ERROR] Failed to interact with: " + current.getName()
                        + " at position: " + player.getCoordinate());
                Execution.delay(1000);
            }
        }
    }

    // State management methods
    public BotState getBotState() { return botState; }
    public void setBotState(BotState botState) { this.botState = botState; }

}