# NPC affinity

- node: docs/spec/domain/npc/affinity.md
  - per_player_state:
    - tracked per `(player_uuid, npc_id)`
  - fields:
    - `affinity`: integer in [-100, 100]
    - `last_saved_node_id`: string
    - `dialogue_visits`: integer
    - `active_quest_id`: string
  - mutation_sources:
    - dialogue transition `affinity_delta`
    - quest completion `affinity_reward`
    - admin commands
  - rules:
    - clamp after every mutation
    - persistence is mandatory
    - affinity updates are atomic with dialogue/quest state savepoints
