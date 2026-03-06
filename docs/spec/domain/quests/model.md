# Quest model

- node: docs/spec/domain/quests/model.md
  - object: quest_definition
  - required_fields:
    - `id`: lowercase snake_case unique id
    - `npc_id`: quest giver npc id
    - `title`: string
    - `stages`: non-empty array
  - stage_fields:
    - `stage_id`: unique inside quest
    - `kind`: enum (`talk`, `distance`, `collect`, `kill`, `custom`)
    - `target`: integer >= 1 for counted stages
    - `next_stage_id`: nullable string
  - constraints:
    - quests are accepted only from dialogue `accept_quest` nodes
    - file lines MUST be <= 300
