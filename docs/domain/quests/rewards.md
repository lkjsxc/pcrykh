# Quest rewards

- node: docs/domain/quests/rewards.md
  - reward_types:
    - `ap`: integer
    - `items`: array of `{material, amount}`
    - `commands`: array of server command templates
    - `affinity`: integer delta applied to quest giver npc
  - rules:
    - rewards are granted exactly once per quest completion
    - duplicate completion attempts MUST be idempotent
