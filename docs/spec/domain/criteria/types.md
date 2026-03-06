# Criteria types

- node: docs/spec/domain/criteria/types.md
  - common_fields:
    - `type` (string, required)
    - `count` (integer, required, `>= 1`)
    - `constraints` (object, required)
  - block_break:
    - schema:
      ```json
      {
        "type": "block_break",
        "materials": ["string"],
        "count": 1,
        "constraints": {}
      }
      ```
    - rules:
      - `materials` MUST be a non-empty array of strings
  - item_craft:
    - schema:
      ```json
      {
        "type": "item_craft",
        "item": "string",
        "count": 1,
        "constraints": {}
      }
      ```
    - rules:
      - `item` MUST be a non-empty string
  - entity_kill:
    - schema:
      ```json
      {
        "type": "entity_kill",
        "entities": ["string"],
        "count": 1,
        "constraints": {}
      }
      ```
    - rules:
      - `entities` MUST be a non-empty array of strings
  - fish_catch:
    - schema:
      ```json
      {
        "type": "fish_catch",
        "items": ["string"],
        "count": 1,
        "constraints": {}
      }
      ```
    - rules:
      - `items` MUST be a non-empty array of strings
  - item_enchant:
    - schema:
      ```json
      {
        "type": "item_enchant",
        "items": ["string"],
        "count": 1,
        "constraints": {}
      }
      ```
    - rules:
      - `items` MUST be a non-empty array of strings
  - movement:
    - schema:
      ```json
      {
        "type": "movement",
        "mode": "walk",
        "count": 1,
        "constraints": {}
      }
      ```
    - rules:
      - `mode` MUST be a non-empty string or `modes` MUST be a non-empty array of strings
      - valid modes: `walk`, `sprint`, `sneak`, `swim`, `jump`, `ethereal_wing`, `boat`
      - user-facing aliases map to canonical modes: `running` -> `sprint`, `crouching` -> `sneak`, `elytra` -> `ethereal_wing`
      - `count` is distance in blocks for `walk`, `sprint`, `sneak`, `swim`, `ethereal_wing`, and `boat`
      - `count` is total jump count for `jump`
      - `ethereal_wing` requires gliding with elytra equipped
