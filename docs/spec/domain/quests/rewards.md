# Quest rewards

- node: docs/spec/domain/quests/rewards.md
  - reward_types:
    - `ap`: integer
    - `items`: array of `{material, amount}`
    - `commands`: array of server command templates
    - `affinity`: integer delta applied to quest giver npc
  - rules:
    - rewards are granted exactly once per quest completion
    - duplicate completion attempts MUST be idempotent
    - once completion state is persisted, later accept or complete attempts for the same quest MUST not grant additional rewards
