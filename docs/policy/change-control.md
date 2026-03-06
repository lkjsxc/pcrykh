# Change control

- node: docs/governance/changes/change-control.md
	- rules:
		- changes MUST update both docs and source code
		- documentation updates MUST be committed before source code updates
		- significant and bold changes are permitted without migration
		- backward compatibility is ignored
		- the canonical spec version MUST be updated in `config.json` on any spec change
		- every change MUST be recorded in [change-records.md](change-records.md)
