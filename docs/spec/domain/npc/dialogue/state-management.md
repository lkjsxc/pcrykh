# Dialogue state management

- node: docs/spec/domain/npc/dialogue/state-management.md
  - per_player_session_fields:
    - `npc_id`: currently engaged npc
    - `current_node_id`: node currently being rendered or advanced from
    - `last_saved_node_id`: rollback checkpoint for interruption recovery
    - `last_interaction_epoch_ms`: timeout clock source
    - `affinity`: clamped integer in `[-100, 100]`
    - `active_quest_id`: accepted quest bound to the npc, if any
  - transition_diagram:
    - mermaid:
      ```mermaid
      stateDiagram-v2
        [*] --> Idle
        Idle --> ActiveLine: right-click managed villager
        ActiveLine --> ActiveLine: right-click line node with line target
        ActiveLine --> AcceptQuest: right-click line node targeting accept_quest
        ActiveLine --> Completed: right-click line node targeting end
        ActiveLine --> TimedOut: idle timeout
        AcceptQuest --> Idle: quest accepted and session closed
        Completed --> Idle: end node reached
        TimedOut --> Idle: restore last_saved_node_id
      ```
  - runtime_rules:
    - first interaction starts at `last_saved_node_id` when valid; otherwise use graph `start_node_id`
    - each right-click while active advances exactly one transition
    - `save_checkpoint=true` updates `last_saved_node_id` before rendering the next node
    - `affinity_delta` applies during transition and is clamped immediately
    - timeout clears the active session and restores the checkpoint node
    - reaching `accept_quest` accepts the bound quest and closes the session