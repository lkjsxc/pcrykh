# NPC affinity

- node: docs/domain/npc/affinity.md
  - per_player_state:
    - tracked per `(player_uuid, npc_id)`
  - fields:
    - `affinity`: integer in [-100, 100]
    - `last_updated_at_epoch_ms`: integer
  - mutation_sources:
    - dialogue transition `affinity_delta`
    - quest completion `affinity_reward`
    - admin commands
  - rules:
    - clamp after every mutation
    - persistence is mandatory
    - affinity updates are atomic with dialogue/quest state savepoints
