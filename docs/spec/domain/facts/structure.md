# Facts catalog structure

- node: docs/domain/facts/structure.md
  - grouping:
    - facts are grouped into packs for scalability
    - packs SHOULD be organized by theme or range
  - sources:
    - packs are loaded from `facts_sources`
    - pack files MUST remain under 300 lines
  - invariants:
    - each pack MUST define a non-empty `facts` list
