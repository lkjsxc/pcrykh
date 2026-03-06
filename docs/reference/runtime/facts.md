# Random facts

- node: docs/runtime/facts.md
  - source:
    - facts are loaded from `config.json` `facts_sources`
    - the canonical fact list is defined in [domain/facts/README.md](../domain/facts/README.md)
    - each source may be a file or directory; see [config/runtime/runtime-config.md](../config/runtime/runtime-config.md)
  - broadcast:
    - if `runtime.chat.facts_enabled` is true, broadcast random facts to global chat
    - interval is `runtime.chat.facts_interval_seconds`
    - prefix is `runtime.chat.prefix`
  - selection:
    - choose a random fact uniformly from the loaded list
    - consecutive repeats SHOULD be avoided when possible
