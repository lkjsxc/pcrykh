# Contradictions Summary

Back: [/docs/reference/README.md](/docs/reference/README.md)

Consolidated status of contradictory specifications discovered during the 2026-03-06 documentation restructure.

## Canonical Contradictions

Source set:
- [/docs/reference/conflicts/index.md](/docs/reference/conflicts/index.md)
- [/docs/reference/conflicts/contradictions.md](/docs/reference/conflicts/contradictions.md)
- [/docs/reference/conflicts/resolutions.md](/docs/reference/conflicts/resolutions.md)

| ID | Topic | Status | Resolution Source |
|---|---|---|---|
| C-006 | `achievement_id` vs `id` naming | resolved | [/docs/reference/conflicts/resolutions.md](/docs/reference/conflicts/resolutions.md) |
| C-007 | criteria type list omitted `block_break` | resolved | [/docs/reference/conflicts/resolutions.md](/docs/reference/conflicts/resolutions.md) |
| C-008 | README authority vs TOC canonicality rule | resolved | [/docs/reference/conflicts/resolutions.md](/docs/reference/conflicts/resolutions.md) |
| C-009 | GUI out-of-scope claim vs GUI menu spec | resolved | [/docs/reference/conflicts/resolutions.md](/docs/reference/conflicts/resolutions.md) |
| C-010 | corrupted JSON schema blocks in config docs | resolved | [/docs/reference/conflicts/resolutions.md](/docs/reference/conflicts/resolutions.md) |
| C-011 | immediate deprecation deletion policy vs retained deprecated docs | resolved | [/docs/reference/conflicts/resolutions.md](/docs/reference/conflicts/resolutions.md) |
| C-012 | lifecycle no-persistence claim vs dialogue resume requirements | resolved | [/docs/reference/conflicts/resolutions.md](/docs/reference/conflicts/resolutions.md) |
| C-013 | quests must be menu-visible vs menu missing quests entrypoint | resolved | [/docs/reference/conflicts/resolutions.md](/docs/reference/conflicts/resolutions.md) |
| C-014 | movement goals required swim but canonical mode list omitted it | resolved | [/docs/reference/conflicts/resolutions.md](/docs/reference/conflicts/resolutions.md) |
| C-015 | achievements back navigation slot conflict (`45` vs `49`) | resolved | [/docs/reference/conflicts/resolutions.md](/docs/reference/conflicts/resolutions.md) |

## Carry-Forward Drift Contradictions

Non-canonical reference set used as migration input:
- [/tmp/docs/reference/DRIFT_MATRIX.md](/tmp/docs/reference/DRIFT_MATRIX.md)

| ID | Contradictory Theme | Status | Action in This Restructure |
|---|---|---|---|
| DRIFT-005 | root/reference status contradiction | carry | canonical docs tree collapsed into one authority root at `/docs/`; contradiction recorded for later runtime rebuild programs |

## Deprecated Content Removed

The following content was removed as deprecated or no longer in use:
- `/docs/domain/achievements/ideas/README.md`
- `/docs/domain/achievements/ideas/ideas-001-100.md`
- `/docs/domain/achievements/ideas/ideas-101-200.md`
- `/docs/domain/achievements/ideas/ideas-201-300.md`
- `/docs/domain/achievements/ideas/ideas-301-400.md`
- `/docs/domain/achievements/ideas/ideas-401-500.md`

## Post-Restructure Contradiction State

- Canonical docs contradictions open: `0`
- Canonical docs contradictions resolved: `10`
- Carry-forward drift contradictions: `1` (`DRIFT-005`)

No unresolved contradictions remain in canonical `/docs` specification files.