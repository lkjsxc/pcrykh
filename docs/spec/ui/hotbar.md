# GUI hotbar beacon

- node: docs/spec/ui/hotbar.md
  - item:
    - slot: 8 (far right of the hotbar)
    - material: `BEACON`
    - name: `Pcrykh Menu`
    - lore:
      - `Open the catalog`
  - behavior:
    - clicking the beacon opens the GUI menu defined in [menu.md](menu.md)
    - clicks are consumed; no normal item action occurs
    - attempting to drop/throw the beacon opens the GUI menu instead
  - permissions:
    - requires `pcrykh.use`
  - lifecycle:
    - apply on plugin enable for online players, join, respawn, and world change
    - world change includes teleports between worlds
    - re-apply after inventory resets
