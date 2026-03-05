package dev.pcrykh.quest;

public record QuestEntry(
    String id,
    String title,
    String npcId,
    String stage,
    int progress,
    int target,
    boolean completed
) {
}
