# Achievement catalog structure

- node: docs/domain/achievements/catalog/structure.md
  - grouping:
    - achievements are grouped by `category_id`
    - categories are ordered by `categories.order`
  - sorting:
    - within each category, achievements are ordered by ascending `id`
  - sources:
    - the catalog is assembled from achievement entries and achievement packs
    - achievement entries are single achievement objects
    - achievement packs are arrays of achievement objects
    - each achievement source file MUST remain under 300 lines
    - category files provide ordering and icon metadata
  - invariants:
    - category list MUST be non-empty
    - every achievement MUST reference a category in the merged catalog
    - every achievement `id` MUST be globally unique
