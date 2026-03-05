# Villager runtime

- node: docs/runtime/villagers.md
  - interaction:
    - talking to an unemployed villager bound to an npc definition starts dialogue flow
    - while dialogue is active, right-click advances transitions
  - immobilization:
    - if `runtime.dialogue.freeze_villager` is true, villager movement AI MUST be disabled during conversation
    - movement AI MUST be restored when dialogue ends or aborts
  - interruption:
    - if player does not interact for `runtime.dialogue.timeout_seconds`, conversation is interrupted
    - interrupted conversation MUST restore to the most recent saved dialogue node
    - affinity and dialogue state deltas after the last checkpoint MUST be discarded on interruption
  - quest_acceptance:
    - quest is accepted only at `accept_quest` node type
    - acceptance binds quest state to `(player_uuid, npc_id)`
  - persistence:
    - runtime MUST load state on player join
    - runtime MUST save state on autosave and player quit
