# Runtime config validation

- node: docs/config/runtime/validation.md
  - order:
    - file existence and line-count limit
    - JSON parse
    - required keys and `commands.root`
    - `spec_version` validation
    - source resolution for categories, achievements, facts, npcs, and quests
    - runtime section validation for `action_bar.priority`, `dialogue`, and `persistence`
  - failure_behavior:
    - any validation failure disables plugin startup
