package net.botwithus;

import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;

public class SkeletonScriptGraphicsContext extends ScriptGraphicsContext {

    private SkeletonScript script;

    public SkeletonScriptGraphicsContext(ScriptConsole scriptConsole, SkeletonScript script) {
        super(scriptConsole);
        this.script = script;
    }

    @Override
    public void drawSettings() {
        if (ImGui.Begin("DivaAdvBarb", ImGuiWindowFlag.None.getValue())) {
            if (ImGui.BeginTabBar("My bar", ImGuiWindowFlag.None.getValue())) {
                if (ImGui.BeginTabItem("Settings", ImGuiWindowFlag.None.getValue())) {
                    ImGui.Text("Click start to start/ Stop to stop");


                    // Display current state
                    ImGui.Text("Current State: " + script.getBotState());

                    // Add start/stop buttons
                    if (ImGui.Button("Start Agility Course")) {
                        script.setBotState(SkeletonScript.BotState.SKILLING);
                    }
                    ImGui.SameLine();
                    if (ImGui.Button("Stop")) {
                        script.setBotState(SkeletonScript.BotState.IDLE);
                    }


                    ImGui.Separator();


                    // Existing checkbox


                    ImGui.EndTabItem();
                }
                if (ImGui.BeginTabItem("Stats", ImGuiWindowFlag.None.getValue())) {
                    ImGui.Text("Laps Completed: %d", script.getLapsCompleted());

                    ImGui.Separator();
                    ImGui.Text("Current Obstacle: %s",
                            script.agilityHandler.getCurrentObstacleName());
                    ImGui.EndTabItem();
                }
                ImGui.EndTabBar();
            }
            ImGui.End();
        }
    }

    @Override
    public void drawOverlay() {
        super.drawOverlay();
    }
}