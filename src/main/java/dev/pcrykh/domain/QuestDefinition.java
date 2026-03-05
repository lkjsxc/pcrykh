package dev.pcrykh.domain;

public record QuestDefinition(
        String id,
        String npcId,
        String title,
        String description,
        int affinityReward
) {
}
