# Quest progression

- node: docs/spec/domain/quests/progression.md
  - per_player_fields:
    - `quest.accepted`: boolean
    - `quest.completed`: boolean
    - `quest.stage`: string
    - `quest.progress`: integer
    - `quest.accepted_at_epoch_ms`: integer
    - `npc.active_quest_id`: string
  - rules:
    - one active quest per npc per player
    - `accept_quest` transition sets `quest.accepted=true` and `quest.stage=accepted`
    - stage and counter updates MUST be persisted at autosave and quit
    - completion emits reward payload and optional affinity reward
    - GUI `target` is derived from the active stage: use `stage.target` for counted stages and implicit target `1` for `talk` or `custom` stages
    - GUI `progress` for non-counted stages is `0` before completion and `1` after completion
