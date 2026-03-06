# Achievement model

- node: docs/domain/achievements/catalog/model.md
  - achievement_definition:
    - schema:
      ```json
      {
        "id": "string",
        "category_id": "string",
        "icon": "string",
        "title": "string",
        "description": "string",
        "amount": 1,
        "criteria": { "...": "CriteriaDefinition" },
        "reward": { "...": "RewardDefinition" },
        "name": "string"
      }
      ```
    - rules:
      - required fields: `id`, `category_id`, `icon`, `title`, `description`, `amount`, `criteria`, `reward`
      - `name` is optional; when omitted, it defaults to `title`
      - `id` MUST be globally unique across the generated catalog
      - `category_id` MUST exist in the merged category catalog
      - `amount` MUST be an integer `>= 1`
      - `criteria` MUST conform to the canonical criteria types and include `constraints`
      - `criteria.count` is derived from `amount`; if present it MUST match `amount`
      - `reward` MUST conform to the canonical reward definition
