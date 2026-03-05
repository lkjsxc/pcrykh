# Change records

- node: docs/governance/changes/change-records.md
  - purpose:
    - record current-spec changes for LLM ingestion
    - records describe what is now true, not historical compatibility
  - record_format:
    - yaml:
      ```yaml
      - id: "CR-YYYYMMDD-NNN"
        date: "YYYY-MM-DD"
        summary: "string"
        files:
          - "path/to/file.md"
        notes: "optional"
      ```
  - rules:
    - records MUST be pruned when they no longer describe current-spec behavior
    - records MUST include all modified spec files for the change
    - backward compatibility notes MUST be omitted
  - records:
    - yaml:
      ```yaml
      - id: "CR-20260305-001"
        date: "2026-03-05"
        summary: "Removed deprecated non-canonical specs and registered conflict resolutions C-011/C-012."
        files:
          - "docs/governance/conflicts/contradictions.md"
          - "docs/governance/conflicts/index.md"
          - "docs/governance/conflicts/resolutions.md"
          - "docs/config/category-files.md"
          - "docs/config/runtime-config.md"
          - "docs/domain/achievements/categories.md"
          - "docs/domain/achievements/model.md"
          - "docs/domain/achievements/rewards.md"
          - "docs/domain/achievements/structure.md"
          - "docs/domain/achievements/templates.md"
          - "docs/domain/achievements/ideas-001-100.md"
          - "docs/domain/achievements/ideas-101-200.md"
          - "docs/domain/achievements/ideas-201-300.md"
          - "docs/domain/achievements/ideas-301-400.md"
          - "docs/domain/achievements/ideas-401-500.md"
          - "docs/governance/changes/change-records.md"
        notes: "Deprecated specs were deleted immediately per policy; no archive retained."
      - id: "CR-20260305-002"
        date: "2026-03-05"
        summary: "Upgraded canonical spec to v5 with villager dialogue/quest systems and action-bar priority semantics."
        files:
          - "docs/domain/README.md"
          - "docs/domain/npc/README.md"
          - "docs/domain/npc/model.md"
          - "docs/domain/npc/affinity.md"
          - "docs/domain/npc/dialogue/README.md"
          - "docs/domain/npc/dialogue/state-machine.md"
          - "docs/domain/npc/dialogue/protocol.md"
          - "docs/domain/quests/README.md"
          - "docs/domain/quests/model.md"
          - "docs/domain/quests/progression.md"
          - "docs/domain/quests/rewards.md"
          - "docs/runtime/README.md"
          - "docs/runtime/action-bar.md"
          - "docs/runtime/achievements.md"
          - "docs/runtime/lifecycle.md"
          - "docs/runtime/loader.md"
          - "docs/runtime/villagers.md"
          - "docs/config/runtime/runtime-config.md"
          - "docs/config/runtime/validation.md"
          - "docs/config/sources/README.md"
          - "docs/config/sources/npcs.md"
          - "docs/config/sources/quests.md"
          - "docs/foundation/scope.md"
          - "docs/foundation/versioning.md"
          - "docs/governance/changes/change-records.md"
        notes: "Breaking spec update with no migration guarantees."
      - id: "CR-20260305-003"
        date: "2026-03-05"
        summary: "Aligned villager interruption rollback and persisted quest/npc progression fields with runtime implementation."
        files:
          - "docs/domain/quests/progression.md"
          - "docs/domain/npc/affinity.md"
          - "docs/runtime/villagers.md"
          - "docs/governance/changes/change-records.md"
        notes: "Checkpoint semantics now explicitly discard unsaved deltas on timeout interruption."
  ```
