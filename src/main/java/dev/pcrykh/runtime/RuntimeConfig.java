package dev.pcrykh.runtime;

import java.util.List;

public class RuntimeConfig {
    private final String specVersion;
    private final String commandRoot;
    private final AutosaveConfig autosave;
    private final ChatConfig chat;
    private final ActionBarConfig actionBar;
    private final DialogueConfig dialogue;
    private final PersistenceConfig persistence;
    private final List<String> facts;
    private final List<String> factsSources;
    private final List<String> categorySources;
    private final List<String> achievementSources;
    private final List<String> npcSources;
    private final List<String> questSources;

    public RuntimeConfig(
            String specVersion,
            String commandRoot,
            AutosaveConfig autosave,
            ChatConfig chat,
            ActionBarConfig actionBar,
            DialogueConfig dialogue,
            PersistenceConfig persistence,
            List<String> facts,
            List<String> factsSources,
            List<String> categorySources,
            List<String> achievementSources,
            List<String> npcSources,
            List<String> questSources
    ) {
        this.specVersion = specVersion;
        this.commandRoot = commandRoot;
        this.autosave = autosave;
        this.chat = chat;
        this.actionBar = actionBar;
        this.dialogue = dialogue;
        this.persistence = persistence;
        this.facts = facts;
        this.factsSources = factsSources;
        this.categorySources = categorySources;
        this.achievementSources = achievementSources;
        this.npcSources = npcSources;
        this.questSources = questSources;
    }

    public String specVersion() {
        return specVersion;
    }

    public String commandRoot() {
        return commandRoot;
    }

    public AutosaveConfig autosave() {
        return autosave;
    }

    public ChatConfig chat() {
        return chat;
    }

    public ActionBarConfig actionBar() {
        return actionBar;
    }

    public DialogueConfig dialogue() {
        return dialogue;
    }

    public PersistenceConfig persistence() {
        return persistence;
    }

    public List<String> facts() {
        return facts;
    }

    public List<String> factsSources() {
        return factsSources;
    }

    public List<String> categorySources() {
        return categorySources;
    }

    public List<String> achievementSources() {
        return achievementSources;
    }

    public List<String> npcSources() {
        return npcSources;
    }

    public List<String> questSources() {
        return questSources;
    }

    public static class AutosaveConfig {
        private final boolean enabled;
        private final int intervalSeconds;

        public AutosaveConfig(boolean enabled, int intervalSeconds) {
            this.enabled = enabled;
            this.intervalSeconds = intervalSeconds;
        }

        public boolean enabled() {
            return enabled;
        }

        public int intervalSeconds() {
            return intervalSeconds;
        }
    }

    public static class ChatConfig {
        private boolean announceAchievements;
        private boolean factsEnabled;
        private final int factsIntervalSeconds;
        private final String prefix;

        public ChatConfig(boolean announceAchievements, boolean factsEnabled, int factsIntervalSeconds, String prefix) {
            this.announceAchievements = announceAchievements;
            this.factsEnabled = factsEnabled;
            this.factsIntervalSeconds = factsIntervalSeconds;
            this.prefix = prefix;
        }

        public boolean announceAchievements() {
            return announceAchievements;
        }

        public void setAnnounceAchievements(boolean announceAchievements) {
            this.announceAchievements = announceAchievements;
        }

        public boolean factsEnabled() {
            return factsEnabled;
        }

        public void setFactsEnabled(boolean factsEnabled) {
            this.factsEnabled = factsEnabled;
        }

        public int factsIntervalSeconds() {
            return factsIntervalSeconds;
        }

        public String prefix() {
            return prefix;
        }
    }

    public static class ActionBarConfig {
        private boolean progressEnabled;
        private PriorityConfig priority;

        public ActionBarConfig(boolean progressEnabled, PriorityConfig priority) {
            this.progressEnabled = progressEnabled;
            this.priority = priority;
        }

        public boolean progressEnabled() {
            return progressEnabled;
        }

        public void setProgressEnabled(boolean progressEnabled) {
            this.progressEnabled = progressEnabled;
        }

        public PriorityConfig priority() {
            return priority;
        }

        public void setPriority(PriorityConfig priority) {
            this.priority = priority;
        }
    }

    public static class PriorityConfig {
        private final boolean enabled;
        private final int displayIntervalTicks;
        private final int cooldownTicks;
        private final boolean preemptOnHigherPriority;

        public PriorityConfig(boolean enabled, int displayIntervalTicks, int cooldownTicks, boolean preemptOnHigherPriority) {
            this.enabled = enabled;
            this.displayIntervalTicks = displayIntervalTicks;
            this.cooldownTicks = cooldownTicks;
            this.preemptOnHigherPriority = preemptOnHigherPriority;
        }

        public boolean enabled() {
            return enabled;
        }

        public int displayIntervalTicks() {
            return displayIntervalTicks;
        }

        public int cooldownTicks() {
            return cooldownTicks;
        }

        public boolean preemptOnHigherPriority() {
            return preemptOnHigherPriority;
        }
    }

    public static class DialogueConfig {
        private final int timeoutSeconds;
        private final boolean freezeVillager;

        public DialogueConfig(int timeoutSeconds, boolean freezeVillager) {
            this.timeoutSeconds = timeoutSeconds;
            this.freezeVillager = freezeVillager;
        }

        public int timeoutSeconds() {
            return timeoutSeconds;
        }

        public boolean freezeVillager() {
            return freezeVillager;
        }
    }

    public static class PersistenceConfig {
        private final PlayerStateConfig playerState;

        public PersistenceConfig(PlayerStateConfig playerState) {
            this.playerState = playerState;
        }

        public PlayerStateConfig playerState() {
            return playerState;
        }
    }

    public static class PlayerStateConfig {
        private final boolean enabled;
        private final String directory;

        public PlayerStateConfig(boolean enabled, String directory) {
            this.enabled = enabled;
            this.directory = directory;
        }

        public boolean enabled() {
            return enabled;
        }

        public String directory() {
            return directory;
        }
    }
}
