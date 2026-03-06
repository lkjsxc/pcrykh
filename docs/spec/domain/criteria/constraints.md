# Constraints

- node: docs/domain/criteria/constraints.md
	- definition:
		- constraints are opaque objects passed through without validation
	- rules:
		- `constraints` MUST exist in every criteria object, even if empty
		- `constraints` MUST be a JSON object
		- the runtime MUST NOT interpret or validate constraint keys
