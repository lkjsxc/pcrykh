# Lifecycle

- node: docs/runtime/lifecycle.md
	- startup_sequence:
		- ensure default config resources exist in the data folder
		- load `config.json` and validate required fields
		- resolve `category_sources`, `achievement_sources`, `facts_sources`, `npc_sources`, and `quest_sources`
		- load categories, achievements, npc definitions, quest definitions, and fact packs
		- initialize player state store for dialogue, affinity, and quest progression
		- register GUI listeners and hotbar beacon listeners
		- register achievement progress listeners and villager interaction listeners
		- register `/pcrykh`
		- start runtime schedulers (facts, action-bar queue dispatch, autosave)
	- shutdown:
		- flush in-memory dialogue/affinity/quest state to player-state storage
	- runtime_invariants:
		- achievement, npc, and quest catalogs are immutable after load
		- player-state writes are best-effort during autosave and mandatory on player quit
