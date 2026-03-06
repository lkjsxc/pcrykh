# Runtime config

- node: docs/spec/architecture/runtime-config.md
  - file:
    - name: `config.json`
    - max_lines: 300
    - authority: runtime input
  - required_top_level_keys:
    - `spec_version`
    - `commands.root` (MUST be `pcrykh`)
    - `runtime.autosave`
    - `runtime.chat`
    - `runtime.action_bar`
    - `runtime.dialogue`
    - `runtime.persistence`
    - `category_sources` (non-empty list)
    - `achievement_sources` (non-empty list)
    - `facts_sources` (non-empty list)
    - `npc_sources` (non-empty list)
    - `quest_sources` (non-empty list)
  - top_level_schema:
    - json:
      ```json
      {
        "spec_version": "5.0",
        "commands": {
          "root": "pcrykh"
        },
        "runtime": {
          "autosave": {
            "enabled": true,
            "interval_seconds": 60
          },
          "chat": {
            "announce_achievements": true,
            "facts_enabled": true,
            "facts_interval_seconds": 180,
            "prefix": "[Pcrykh] "
          },
          "action_bar": {
            "progress_enabled": true,
            "priority": {
              "enabled": true,
              "display_interval_ticks": 20,
              "cooldown_ticks": 10,
              "preempt_on_higher_priority": true
            }
          },
          "dialogue": {
            "timeout_seconds": 15,
            "freeze_villager": true
          },
          "persistence": {
            "player_state": {
              "enabled": true,
              "directory": "data/players"
            }
          }
        },
        "facts_sources": ["facts/packs"],
        "category_sources": ["achievements/categories"],
        "achievement_sources": ["achievements/entries"],
        "npc_sources": ["npcs"],
        "quest_sources": ["quests"]
      }
      ```
  - rules:
    - `spec_version` MUST start with `5.`
    - `category_sources` MUST be a non-empty array
    - `achievement_sources` MUST be a non-empty array
    - `facts_sources` MUST be a non-empty array
    - `npc_sources` MUST be a non-empty array
    - `quest_sources` MUST be a non-empty array
  - category_source_resolution:
    - each entry in `category_sources` is a path relative to the plugin data folder
    - if the entry is a directory, all `.json` files under it (recursive) are loaded in lexical order
    - if the entry is a file, it is loaded directly
  - achievement_source_resolution:
    - each entry in `achievement_sources` is a path relative to the plugin data folder
    - if the entry is a directory, all `.json` files under it (recursive) are loaded in lexical order
    - if the entry is a file, it is loaded directly
  - facts_source_resolution:
    - each entry in `facts_sources` is a path relative to the plugin data folder
    - if the entry is a directory, all `.json` files under it (recursive) are loaded in lexical order
    - if the entry is a file, it is loaded directly
  - npc_source_resolution:
    - each entry in `npc_sources` is a path relative to the plugin data folder
    - if the entry is a directory, all `.json` files under it (recursive) are loaded in lexical order
    - if the entry is a file, it is loaded directly
  - quest_source_resolution:
    - each entry in `quest_sources` is a path relative to the plugin data folder
    - if the entry is a directory, all `.json` files under it (recursive) are loaded in lexical order
    - if the entry is a file, it is loaded directly
  - generated_achievements:
    - achievement sources provide either single achievement files or achievement packs
    - single achievements must conform to [domain/achievements/catalog/model.md](../../domain/achievements/catalog/model.md)
    - packs are arrays of achievement objects conforming to the same model
