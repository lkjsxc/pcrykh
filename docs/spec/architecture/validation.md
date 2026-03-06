# Runtime config validation

- node: docs/spec/architecture/validation.md
  - order:
    - file existence and line-count limit
    - JSON parse
    - required keys and `commands.root`
    - `spec_version` validation
    - runtime section validation for `autosave`, `chat`, `action_bar.priority`, `dialogue`, and `persistence`
    - source resolution for categories, achievements, facts, npcs, and quests
    - category validation and duplicate category id rejection
    - achievement validation and duplicate achievement id rejection
    - npc validation, dialogue graph integrity checks, and duplicate npc id rejection
    - quest validation, quest-to-npc linkage checks, and duplicate quest id rejection
  - failure_behavior:
    - any validation failure disables plugin startup
    - failures MUST log the offending file path and field or object context when available
