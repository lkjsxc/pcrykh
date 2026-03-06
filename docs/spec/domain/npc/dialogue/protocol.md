# Dialogue protocol

- node: docs/spec/domain/npc/dialogue/protocol.md
  - start:
    - trigger: right-click unemployed villager bound to an npc definition
    - action: open conversation at `last_saved_node_id` or graph `start_node_id`
  - advance:
    - trigger: right-click same villager while session is active
    - action: execute next transition and render next node
  - freeze:
    - while active, villager AI movement MUST be disabled
  - interruption:
    - if no interaction for `runtime.dialogue.timeout_seconds`, session MUST abort
    - abort MUST restore to `last_saved_node_id`
  - completion:
    - when `accept_quest` node is reached, player quest is accepted and session ends
