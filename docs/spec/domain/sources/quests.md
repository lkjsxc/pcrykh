# Quest sources

- node: docs/spec/domain/sources/quests.md
  - purpose:
    - define quest definitions used by villager story progression
  - source_binding:
    - `quest_sources` in [../../architecture/runtime-config.md](../../architecture/runtime-config.md)
  - resolution:
    - each entry is a path relative to the plugin data folder
    - directories are scanned recursively for `.json` files
    - files are loaded in lexical order
  - file_rules:
    - each file MUST remain under 300 lines
    - each file contains one quest definition or an array of quest definitions
    - each quest `npc_id` MUST reference an existing npc id
