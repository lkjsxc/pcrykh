package dev.pcrykh.domain;

import java.util.List;

public record NpcDefinition(
        String id,
        String displayName,
        String world,
        double x,
        double y,
        double z,
        String profession,
        String questId,
        String startNodeId,
        List<DialogueNode> dialogueNodes
) {
}
