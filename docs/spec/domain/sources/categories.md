# Category sources

- node: docs/config/sources/categories.md
  - purpose:
    - define category metadata in standalone JSON files
  - source_binding:
    - `category_sources` in [../runtime/runtime-config.md](../runtime/runtime-config.md)
  - resolution:
    - each entry is a path relative to the plugin data folder
    - directories are scanned recursively for `.json` files
    - files are loaded in lexical order
  - file_rules:
    - each file defines exactly one category object
    - each file MUST remain under 300 lines
    - filenames SHOULD match the category `id`
  - schema:
    - [domain/achievements/catalog/categories.md](../../domain/achievements/catalog/categories.md)
