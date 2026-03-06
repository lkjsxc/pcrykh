# Achievement entry organization

- node: docs/domain/achievements/entries/organization.md
  - directory_layout:
    - organize entries by category_id as directories under `achievements/entries/`
    - each JSON file contains exactly one achievement definition
  - naming:
    - file names SHOULD match the achievement `id`
    - ids MUST remain globally unique across all sources
  - ordering:
    - within a category, ordering is ascending `id`
    - category ordering is defined by category `order`
