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
