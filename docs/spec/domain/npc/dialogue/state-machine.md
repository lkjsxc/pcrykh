# Dialogue state machine

- node: docs/domain/npc/dialogue/state-machine.md
  - graph:
    - directed graph keyed by `node_id`
    - one `start_node_id` per graph
  - node_types:
    - `line`: NPC text output and implicit next edge
    - `choice`: player options with explicit target edges
    - `accept_quest`: terminal acceptance node
    - `end`: terminal no-op node
  - transition_fields:
    - `to_node_id`: required
    - `affinity_delta`: optional integer
    - `save_checkpoint`: optional boolean (default `false`)
    - `quest_stage_transition`: optional object
  - persistence_contract:
    - runtime MUST persist `last_saved_node_id`
    - if conversation aborts on idle timeout, runtime MUST restore `last_saved_node_id`
    - if transition has `save_checkpoint=true`, runtime MUST update `last_saved_node_id` before rendering next node
