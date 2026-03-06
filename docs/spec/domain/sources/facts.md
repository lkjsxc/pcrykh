# Fact sources

- node: docs/config/sources/facts.md
  - purpose:
    - define fact pack inputs used for runtime chat facts
  - source_binding:
    - `facts_sources` in [../runtime/runtime-config.md](../runtime/runtime-config.md)
  - resolution:
    - each entry is a path relative to the plugin data folder
    - directories are scanned recursively for `.json` files
    - files are loaded in lexical order
  - file_rules:
    - each file is a fact pack JSON object
    - each file MUST remain under 300 lines
  - schema:
    - [domain/facts/structure.md](../../domain/facts/structure.md)
