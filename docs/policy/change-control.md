# Change control

- node: docs/policy/change-control.md
	- rules:
		- changes MUST update both docs and source code
		- documentation updates MUST be committed before source code updates
		- significant and bold changes are permitted without migration
		- backward compatibility is ignored
		- the canonical spec version MUST be updated in `config.json` on any spec change
		- contradictory specifications discovered during an edit MUST be recorded in `docs/reference/conflicts/` before the conflicting docs are rewritten
		- obsolete change history MUST be deleted instead of preserved as a canonical artifact
