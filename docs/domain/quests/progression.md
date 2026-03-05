# Quest progression

- node: docs/domain/quests/progression.md
  - per_player_fields:
    - `accepted_quests`: set
    - `active_quest_by_npc`: map of npc_id -> quest_id
    - `quest_stage`: map of quest_id -> stage_id
    - `quest_counters`: map of quest_id -> integer
    - `completed_quests`: set
  - rules:
    - one active quest per npc per player
    - stage transition occurs when objective target is met
    - stage and counter updates MUST be persisted at autosave and quit
    - completion emits reward payload and optional affinity reward
