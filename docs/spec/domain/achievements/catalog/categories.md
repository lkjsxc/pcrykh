# Achievement categories

- node: docs/domain/achievements/catalog/categories.md
  - category_definition:
    - schema:
      ```json
      {
        "id": "string",
        "name": "string",
        "order": 0,
        "icon": "string"
      }
      ```
    - rules:
      - all fields are required
      - `id` MUST be unique within the merged catalog
      - `order` is an integer; lower numbers sort first
  - merging:
    - categories from all category files are merged by `id`
    - duplicate `id` values with differing fields are fatal
