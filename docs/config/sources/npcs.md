# NPC sources

- node: docs/config/sources/npcs.md
  - purpose:
    - define npc + dialogue graph inputs used by villager runtime
  - source_binding:
    - `npc_sources` in [../runtime/runtime-config.md](../runtime/runtime-config.md)
  - resolution:
    - each entry is a path relative to the plugin data folder
    - directories are scanned recursively for `.json` files
    - files are loaded in lexical order
  - file_rules:
    - each file MUST remain under 300 lines
    - each file contains one npc definition or an array of npc definitions
    - each referenced dialogue graph id MUST be resolvable
