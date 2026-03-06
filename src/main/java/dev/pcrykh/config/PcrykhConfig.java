package dev.pcrykh.config;

import java.util.List;

public record PcrykhConfig(
        String specVersion,
        Commands commands,
        Runtime runtime,
        List<String> categorySources,
        List<String> achievementSources,
        List<String> factsSources,
        List<String> npcSources,
        List<String> questSources
) {
    public record Commands(String root) {
    }

    public record Runtime(
            Autosave autosave,
            Chat chat,
            ActionBar actionBar,
            Dialogue dialogue,
            Persistence persistence
    ) {
    }

    public record Autosave(boolean enabled, int intervalSeconds) {
    }

    public record Chat(boolean announceAchievements, boolean factsEnabled, int factsIntervalSeconds, String prefix) {
    }

    public record ActionBar(boolean progressEnabled, Priority priority) {
    }

    public record Priority(boolean enabled, int displayIntervalTicks, int cooldownTicks, boolean preemptOnHigherPriority) {
    }

    public record Dialogue(int timeoutSeconds, boolean freezeVillager) {
    }

    public record Persistence(PlayerState playerState) {
    }

    public record PlayerState(boolean enabled, String directory) {
    }
}