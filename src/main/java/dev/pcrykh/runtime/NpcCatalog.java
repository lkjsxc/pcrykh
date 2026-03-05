package dev.pcrykh.runtime;

import dev.pcrykh.domain.NpcDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NpcCatalog {
    private final List<NpcDefinition> npcs;
    private final Map<String, NpcDefinition> byId = new HashMap<>();

    public NpcCatalog(List<NpcDefinition> npcs) {
        this.npcs = List.copyOf(npcs);
        for (NpcDefinition npc : npcs) {
            if (npc.id() == null || npc.id().isBlank()) {
                throw new ConfigException("npc id must be non-empty");
            }
            if (byId.putIfAbsent(npc.id(), npc) != null) {
                throw new ConfigException("Duplicate npc id: " + npc.id());
            }
        }
    }

    public List<NpcDefinition> npcs() {
        return Collections.unmodifiableList(npcs);
    }

    public NpcDefinition get(String id) {
        return byId.get(id);
    }
}
