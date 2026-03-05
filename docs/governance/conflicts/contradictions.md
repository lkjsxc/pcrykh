# Contradictions

- node: docs/governance/conflicts/contradictions.md
	- status:
		- no open contradictions remain
	- resolved_contradictions:
		- `achievement_id` vs `id` for achievement identifiers
		- `block_break` used in config examples but missing from criteria type list
		- TOC canonicality rule vs README authority
		- GUI declared out-of-scope vs GUI menu specification
		- corrupted config schema blocks containing duplicated spec text
		- achievement ordering tie-breaker conflict (category name vs id); standardized to category order then id
		- deprecations policy requiring immediate deletion vs deprecated files left in-tree
		- lifecycle `shutdown` declaring no persistence actions vs dialogue state resume requirements
		- quests required to be viewable from the menu vs menu lacking a quests entrypoint
		- swimming achievements required by behavior goals vs movement mode lists omitting `swim`
		- achievements page returning via slot `49` vs shared navigation reserving slot `49` for page metadata and slot `45` for `Back`
