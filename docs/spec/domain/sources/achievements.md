# Achievement sources

- node: docs/config/sources/achievements.md
  - purpose:
    - define achievement inputs used to build the runtime catalog
  - source_binding:
    - `achievement_sources` in [../runtime/runtime-config.md](../runtime/runtime-config.md)
  - resolution:
    - each entry is a path relative to the plugin data folder
    - directories are scanned recursively for `.json` files
    - files are loaded in lexical order
  - file_rules:
    - each file MUST remain under 300 lines
    - each file is either a single achievement entry or an achievement pack
    - entries contain one achievement definition object
    - packs contain arrays of achievement definition objects
  - pack_format:
    - each element MUST conform to [domain/achievements/catalog/model.md](../../domain/achievements/catalog/model.md)
  - entry_format:
    - the object MUST conform to [domain/achievements/catalog/model.md](../../domain/achievements/catalog/model.md)
