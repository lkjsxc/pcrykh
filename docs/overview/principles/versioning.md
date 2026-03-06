# Versioning

- node: docs/overview/principles/versioning.md
  - rules:
    - backward compatibility is ignored
    - `spec_version` is required in runtime config
    - only `5.x` is supported by this repository
    - any non-`5.x` version MUST be rejected
    - `spec_version` MUST be a string starting with `5.`
