package dev.pcrykh.domain;

public record DialogueNode(
        String id,
        String text,
        String nextNodeId,
        boolean saveCheckpoint,
        boolean acceptQuest,
        int affinityDelta
) {
}
