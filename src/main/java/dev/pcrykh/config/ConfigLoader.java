package dev.pcrykh.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class ConfigLoader {
    public static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> QUEST_STAGE_KINDS = Set.of("talk", "distance", "collect", "kill", "custom");
    private static final Set<String> COUNTED_QUEST_STAGE_KINDS = Set.of("distance", "collect", "kill");

    public LoadedRuntime load(Path dataDirectory) throws IOException, ValidationException {
        Path configPath = dataDirectory.resolve("config.json");
        requireExists(configPath, "config.json");
        requireLineLimit(configPath, "config.json");

        JsonNode root = readJson(configPath);
        PcrykhConfig config = parseConfig(root, configPath);

        List<Path> categoryFiles = resolveSources(dataDirectory, config.categorySources(), "category_sources");
        List<Path> achievementFiles = resolveSources(dataDirectory, config.achievementSources(), "achievement_sources");
        List<Path> factFiles = resolveSources(dataDirectory, config.factsSources(), "facts_sources");
        List<Path> npcFiles = resolveSources(dataDirectory, config.npcSources(), "npc_sources");
        List<Path> questFiles = resolveSources(dataDirectory, config.questSources(), "quest_sources");

        Map<String, CategoryDefinition> categories = loadCategories(categoryFiles);
        List<AchievementDefinition> achievements = loadAchievements(achievementFiles, categories);
        List<String> facts = loadFacts(factFiles);
        Map<String, NpcDefinition> npcs = loadNpcs(npcFiles);
        List<QuestDefinition> quests = loadQuests(questFiles, npcs);

        List<CategoryDefinition> orderedCategories = new ArrayList<>(categories.values());
        orderedCategories.sort(Comparator.comparingInt(CategoryDefinition::order).thenComparing(CategoryDefinition::id));

        achievements.sort(Comparator
                .comparingInt((AchievementDefinition achievement) -> categories.get(achievement.categoryId()).order())
                .thenComparing(AchievementDefinition::id));
        List<NpcDefinition> orderedNpcs = new ArrayList<>(npcs.values());
        orderedNpcs.sort(Comparator.comparing(NpcDefinition::id));
        quests.sort(Comparator.comparing(QuestDefinition::id));

        return new LoadedRuntime(
                config,
                new CatalogSummary(categories.size(), achievements.size(), facts.size(), npcs.size(), quests.size()),
                facts,
                orderedCategories,
                achievements,
            orderedNpcs,
                quests
        );
    }

    private PcrykhConfig parseConfig(JsonNode root, Path configPath) throws ValidationException {
        requireObject(root, configPath, "root");

        String specVersion = requireText(root, configPath, "spec_version");
        if (!specVersion.startsWith("5.")) {
            throw new ValidationException("config.json must use a 5.x spec_version");
        }

        JsonNode commandsNode = requireObjectField(root, configPath, "commands");
        String rootCommand = requireText(commandsNode, configPath, "commands.root");
        if (!"pcrykh".equals(rootCommand)) {
            throw new ValidationException("commands.root must be 'pcrykh'");
        }

        JsonNode runtimeNode = requireObjectField(root, configPath, "runtime");
        JsonNode autosaveNode = requireObjectField(runtimeNode, configPath, "runtime.autosave");
        JsonNode chatNode = requireObjectField(runtimeNode, configPath, "runtime.chat");
        JsonNode actionBarNode = requireObjectField(runtimeNode, configPath, "runtime.action_bar");
        JsonNode priorityNode = requireObjectField(actionBarNode, configPath, "runtime.action_bar.priority");
        JsonNode dialogueNode = requireObjectField(runtimeNode, configPath, "runtime.dialogue");
        JsonNode persistenceNode = requireObjectField(runtimeNode, configPath, "runtime.persistence");
        JsonNode playerStateNode = requireObjectField(persistenceNode, configPath, "runtime.persistence.player_state");

        return new PcrykhConfig(
                specVersion,
                new PcrykhConfig.Commands(rootCommand),
                new PcrykhConfig.Runtime(
                        new PcrykhConfig.Autosave(
                                requireBoolean(autosaveNode, configPath, "runtime.autosave.enabled"),
                                requirePositiveInt(autosaveNode, configPath, "runtime.autosave.interval_seconds")
                        ),
                        new PcrykhConfig.Chat(
                                requireBoolean(chatNode, configPath, "runtime.chat.announce_achievements"),
                                requireBoolean(chatNode, configPath, "runtime.chat.facts_enabled"),
                                requirePositiveInt(chatNode, configPath, "runtime.chat.facts_interval_seconds"),
                                requireText(chatNode, configPath, "runtime.chat.prefix")
                        ),
                        new PcrykhConfig.ActionBar(
                                requireBoolean(actionBarNode, configPath, "runtime.action_bar.progress_enabled"),
                                new PcrykhConfig.Priority(
                                        requireBoolean(priorityNode, configPath, "runtime.action_bar.priority.enabled"),
                                        requirePositiveInt(priorityNode, configPath, "runtime.action_bar.priority.display_interval_ticks"),
                                        requirePositiveInt(priorityNode, configPath, "runtime.action_bar.priority.cooldown_ticks"),
                                        requireBoolean(priorityNode, configPath, "runtime.action_bar.priority.preempt_on_higher_priority")
                                )
                        ),
                        new PcrykhConfig.Dialogue(
                                requirePositiveInt(dialogueNode, configPath, "runtime.dialogue.timeout_seconds"),
                                requireBoolean(dialogueNode, configPath, "runtime.dialogue.freeze_villager")
                        ),
                        new PcrykhConfig.Persistence(
                                new PcrykhConfig.PlayerState(
                                        requireBoolean(playerStateNode, configPath, "runtime.persistence.player_state.enabled"),
                                        requireText(playerStateNode, configPath, "runtime.persistence.player_state.directory")
                                )
                        )
                ),
                requireTextArray(root, configPath, "category_sources"),
                requireTextArray(root, configPath, "achievement_sources"),
                requireTextArray(root, configPath, "facts_sources"),
                requireTextArray(root, configPath, "npc_sources"),
                requireTextArray(root, configPath, "quest_sources")
        );
    }

    private List<Path> resolveSources(Path dataDirectory, List<String> sources, String label) throws IOException, ValidationException {
        List<Path> files = new ArrayList<>();
        for (String source : sources) {
            Path resolved = dataDirectory.resolve(source).normalize();
            requireExists(resolved, label + " entry '" + source + "'");

            if (Files.isDirectory(resolved)) {
                try (Stream<Path> stream = Files.walk(resolved)) {
                    stream.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                            .sorted(Comparator.naturalOrder())
                            .forEach(files::add);
                }
            } else if (resolved.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
                files.add(resolved);
            }
        }

        if (files.isEmpty()) {
            throw new ValidationException(label + " resolved to no json files");
        }
        return files;
    }

    private Map<String, CategoryDefinition> loadCategories(List<Path> files) throws IOException, ValidationException {
        Map<String, CategoryDefinition> categories = new HashMap<>();
        for (Path file : files) {
            requireLineLimit(file, file.toString());
            JsonNode node = readJson(file);
            requireObject(node, file, "category");

            CategoryDefinition category = new CategoryDefinition(
                    requireText(node, file, "id"),
                    requireText(node, file, "name"),
                    requireInt(node, file, "order"),
                    requireText(node, file, "icon")
            );

            CategoryDefinition existing = categories.putIfAbsent(category.id(), category);
            if (existing != null && !existing.equals(category)) {
                throw new ValidationException("Conflicting category definition for id: " + category.id());
            }
        }
        return categories;
    }

    private List<AchievementDefinition> loadAchievements(List<Path> files, Map<String, CategoryDefinition> categories) throws IOException, ValidationException {
        List<AchievementDefinition> achievements = new ArrayList<>();
        Set<String> ids = new HashSet<>();

        for (Path file : files) {
            requireLineLimit(file, file.toString());
            JsonNode node = readJson(file);
            List<JsonNode> entries = node.isArray() ? toList(node) : List.of(node);
            for (JsonNode entry : entries) {
                requireObject(entry, file, "achievement");
                String id = requireText(entry, file, "id");
                if (!ids.add(id)) {
                    throw new ValidationException("Duplicate achievement id: " + id);
                }
                String categoryId = requireText(entry, file, "category_id");
                if (!categories.containsKey(categoryId)) {
                    throw new ValidationException("Unknown achievement category id: " + categoryId);
                }
                String icon = requireText(entry, file, "icon");
                String title = requireText(entry, file, "title");
                String description = requireText(entry, file, "description");
                int amount = requirePositiveInt(entry, file, "amount");
                JsonNode criteria = requireObjectField(entry, file, "criteria");
                requireText(criteria, file, "type");
                int count = requirePositiveInt(criteria, file, "count");
                requireObjectField(criteria, file, "constraints");
                if (count != amount) {
                    throw new ValidationException("criteria.count must match amount for achievement " + id);
                }
                JsonNode reward = requireObjectField(entry, file, "reward");
                int ap = requireInt(reward, file, "ap");

                achievements.add(new AchievementDefinition(id, categoryId, icon, title, description, amount, ap));
            }
        }

        return achievements;
    }

    private List<String> loadFacts(List<Path> files) throws IOException, ValidationException {
        List<String> facts = new ArrayList<>();
        for (Path file : files) {
            requireLineLimit(file, file.toString());
            JsonNode node = readJson(file);
            JsonNode factNodes = requireArrayField(node, file, "facts");
            if (factNodes.isEmpty()) {
                throw new ValidationException("Fact pack must contain at least one fact: " + file);
            }
            for (JsonNode factNode : factNodes) {
                if (!factNode.isTextual() || factNode.asText().isBlank()) {
                    throw new ValidationException("Facts must be non-empty strings in " + file);
                }
                facts.add(factNode.asText());
            }
        }
        return facts;
    }

    private Map<String, NpcDefinition> loadNpcs(List<Path> files) throws IOException, ValidationException {
        Map<String, NpcDefinition> npcs = new HashMap<>();
        for (Path file : files) {
            requireLineLimit(file, file.toString());
            JsonNode node = readJson(file);
            List<JsonNode> entries = node.isArray() ? toList(node) : List.of(node);
            for (JsonNode entry : entries) {
                requireObject(entry, file, "npc");
                String id = requireText(entry, file, "id");
                if (npcs.containsKey(id)) {
                    throw new ValidationException("Duplicate npc id: " + id);
                }
                String displayName = requireText(entry, file, "display_name");
                if (!"VILLAGER".equals(requireText(entry, file, "entity_type"))) {
                    throw new ValidationException("npc.entity_type must be VILLAGER for " + id);
                }
                requireText(entry, file, "profession");
                requireText(entry, file, "world");
                JsonNode position = requireObjectField(entry, file, "position");
                requireNumber(position, file, "x");
                requireNumber(position, file, "y");
                requireNumber(position, file, "z");

                String graphId = requireText(entry, file, "dialogue_graph_id");
                String questAcceptNodeId = requireText(entry, file, "quest_accept_node_id");
                JsonNode graph = requireObjectField(entry, file, "dialogue_graph");
                if (!graphId.equals(requireText(graph, file, "id"))) {
                    throw new ValidationException("npc dialogue_graph.id must match dialogue_graph_id for " + id);
                }

                String startNodeId = requireText(graph, file, "start_node_id");
                JsonNode nodes = requireObjectField(graph, file, "nodes");
                if (nodes.get(startNodeId) == null) {
                    throw new ValidationException("dialogue graph start node missing for " + id);
                }

                JsonNode questNode = nodes.get(questAcceptNodeId);
                if (questNode == null) {
                    throw new ValidationException("quest_accept_node_id is missing from dialogue graph for " + id);
                }
                if (!"accept_quest".equals(requireText(questNode, file, "type"))) {
                    throw new ValidationException("quest_accept_node_id must point to an accept_quest node for " + id);
                }

                String profession = requireText(entry, file, "profession");
                String world = requireText(entry, file, "world");
                Position positionValue = new Position(
                        requireDouble(position, file, "x"),
                        requireDouble(position, file, "y"),
                        requireDouble(position, file, "z")
                );

                Map<String, DialogueNodeDefinition> dialogueNodes = new HashMap<>();
                nodes.fieldNames().forEachRemaining(nodeId -> {
                    JsonNode nodeValue = nodes.get(nodeId);
                    String type = nodeValue.path("type").asText();
                    String text = nodeValue.path("text").asText("");
                    String toNodeId = nodeValue.hasNonNull("to_node_id") ? nodeValue.get("to_node_id").asText() : null;
                    boolean saveCheckpoint = nodeValue.path("save_checkpoint").asBoolean(false);
                    int affinityDelta = nodeValue.path("affinity_delta").asInt(0);
                    dialogueNodes.put(nodeId, new DialogueNodeDefinition(nodeId, type, text, toNodeId, saveCheckpoint, affinityDelta));
                });

                npcs.put(id, new NpcDefinition(
                        id,
                        displayName,
                        profession,
                        world,
                        positionValue,
                        graphId,
                        questAcceptNodeId,
                        new DialogueGraphDefinition(graphId, startNodeId, dialogueNodes)
                ));
            }
        }
        return npcs;
    }

    private List<QuestDefinition> loadQuests(List<Path> files, Map<String, NpcDefinition> npcs) throws IOException, ValidationException {
        List<QuestDefinition> quests = new ArrayList<>();
        Set<String> ids = new HashSet<>();

        for (Path file : files) {
            requireLineLimit(file, file.toString());
            JsonNode node = readJson(file);
            List<JsonNode> entries = node.isArray() ? toList(node) : List.of(node);
            for (JsonNode entry : entries) {
                requireObject(entry, file, "quest");
                String id = requireText(entry, file, "id");
                if (!ids.add(id)) {
                    throw new ValidationException("Duplicate quest id: " + id);
                }
                String npcId = requireText(entry, file, "npc_id");
                if (!npcs.containsKey(npcId)) {
                    throw new ValidationException("Unknown quest npc_id: " + npcId);
                }
                String title = requireText(entry, file, "title");
                JsonNode stages = requireArrayField(entry, file, "stages");
                if (stages.isEmpty()) {
                    throw new ValidationException("Quest must contain at least one stage: " + id);
                }

                Set<String> stageIds = new HashSet<>();
                int defaultTarget = 1;
                boolean targetAssigned = false;
                for (JsonNode stage : stages) {
                    String stageId = requireText(stage, file, "stage_id");
                    if (!stageIds.add(stageId)) {
                        throw new ValidationException("Duplicate stage_id in quest " + id + ": " + stageId);
                    }
                    String kind = requireText(stage, file, "kind");
                    if (!QUEST_STAGE_KINDS.contains(kind)) {
                        throw new ValidationException("Unsupported quest stage kind: " + kind);
                    }
                    if (COUNTED_QUEST_STAGE_KINDS.contains(kind)) {
                        int target = requirePositiveInt(stage, file, "target");
                        if (!targetAssigned) {
                            defaultTarget = target;
                            targetAssigned = true;
                        }
                    }
                }

                quests.add(new QuestDefinition(id, npcId, title, defaultTarget));
            }
        }

        return quests;
    }

    private JsonNode readJson(Path path) throws IOException, ValidationException {
        try {
            return MAPPER.readTree(Files.readString(path));
        } catch (JsonProcessingException exception) {
            throw new ValidationException("Invalid JSON in " + path, exception);
        }
    }

    private void requireExists(Path path, String label) throws ValidationException {
        if (!Files.exists(path)) {
            throw new ValidationException("Missing required path: " + label);
        }
    }

    private void requireLineLimit(Path path, String label) throws IOException, ValidationException {
        try (Stream<String> lines = Files.lines(path)) {
            if (lines.count() > 300) {
                throw new ValidationException(label + " exceeds the 300-line limit");
            }
        }
    }

    private void requireObject(JsonNode node, Path path, String label) throws ValidationException {
        if (!node.isObject()) {
            throw new ValidationException(label + " must be a JSON object in " + path);
        }
    }

    private JsonNode requireObjectField(JsonNode node, Path path, String field) throws ValidationException {
        JsonNode value = node.get(lastSegment(field));
        if (value == null || !value.isObject()) {
            throw new ValidationException("Missing object field " + field + " in " + path);
        }
        return value;
    }

    private JsonNode requireArrayField(JsonNode node, Path path, String field) throws ValidationException {
        JsonNode value = node.get(lastSegment(field));
        if (value == null || !value.isArray()) {
            throw new ValidationException("Missing array field " + field + " in " + path);
        }
        return value;
    }

    private List<String> requireTextArray(JsonNode node, Path path, String field) throws ValidationException {
        JsonNode array = requireArrayField(node, path, field);
        if (array.isEmpty()) {
            throw new ValidationException(field + " must be non-empty in " + path);
        }

        List<String> values = new ArrayList<>();
        for (JsonNode entry : array) {
            if (!entry.isTextual() || entry.asText().isBlank()) {
                throw new ValidationException(field + " must only contain non-empty strings in " + path);
            }
            values.add(entry.asText());
        }
        return values;
    }

    private String requireText(JsonNode node, Path path, String field) throws ValidationException {
        JsonNode value = node.get(lastSegment(field));
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new ValidationException("Missing text field " + field + " in " + path);
        }
        return value.asText();
    }

    private boolean requireBoolean(JsonNode node, Path path, String field) throws ValidationException {
        JsonNode value = node.get(lastSegment(field));
        if (value == null || !value.isBoolean()) {
            throw new ValidationException("Missing boolean field " + field + " in " + path);
        }
        return value.asBoolean();
    }

    private int requireInt(JsonNode node, Path path, String field) throws ValidationException {
        JsonNode value = node.get(lastSegment(field));
        if (value == null || !value.isInt()) {
            throw new ValidationException("Missing integer field " + field + " in " + path);
        }
        return value.asInt();
    }

    private int requirePositiveInt(JsonNode node, Path path, String field) throws ValidationException {
        int value = requireInt(node, path, field);
        if (value < 1) {
            throw new ValidationException(field + " must be >= 1 in " + path);
        }
        return value;
    }

    private void requireNumber(JsonNode node, Path path, String field) throws ValidationException {
        JsonNode value = node.get(field);
        if (value == null || !value.isNumber()) {
            throw new ValidationException("Missing numeric field " + field + " in " + path);
        }
    }

    private double requireDouble(JsonNode node, Path path, String field) throws ValidationException {
        JsonNode value = node.get(field);
        if (value == null || !value.isNumber()) {
            throw new ValidationException("Missing numeric field " + field + " in " + path);
        }
        return value.asDouble();
    }

    private List<JsonNode> toList(JsonNode array) {
        List<JsonNode> values = new ArrayList<>();
        array.forEach(values::add);
        return values;
    }

    private String lastSegment(String field) {
        int index = field.lastIndexOf('.') + 1;
        return field.substring(index);
    }

    public record CatalogSummary(int categories, int achievements, int facts, int npcs, int quests) {
    }

    public record CategoryDefinition(String id, String name, int order, String icon) {
    }

    public record AchievementDefinition(String id, String categoryId, String icon, String title, String description, int amount, int ap) {
    }

        public record Position(double x, double y, double z) {
        }

        public record DialogueNodeDefinition(String id, String type, String text, String toNodeId, boolean saveCheckpoint, int affinityDelta) {
        }

        public record DialogueGraphDefinition(String id, String startNodeId, Map<String, DialogueNodeDefinition> nodes) {
        }

        public record NpcDefinition(
            String id,
            String displayName,
            String profession,
            String world,
            Position position,
            String dialogueGraphId,
            String questAcceptNodeId,
            DialogueGraphDefinition dialogueGraph
        ) {
    }

    public record QuestDefinition(String id, String npcId, String title, int target) {
    }

    public record LoadedRuntime(
            PcrykhConfig config,
            CatalogSummary summary,
            List<String> facts,
            List<CategoryDefinition> categories,
            List<AchievementDefinition> achievements,
            List<NpcDefinition> npcs,
            List<QuestDefinition> quests
    ) {
    }
}