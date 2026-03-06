# Achievement entry format

- node: docs/domain/achievements/entries/format.md
  - entry_file:
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
      - `name` is optional; when omitted it defaults to `title`
      - `amount` MUST be an integer `>= 1`
      - `criteria` MUST include `constraints` and MUST be compatible with `amount`
      - `reward` MUST conform to reward rules
  - progression:
    - general action achievements MUST be declared in five levels: I, II, III, IV, V
    - each subsequent level MUST require approximately 1.8x the previous `amount`
