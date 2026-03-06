# Commands

- node: docs/runtime/commands.md
	- command:
		- name: `/pcrykh`
		- permission: `pcrykh.use`
		- behavior: opens the GUI menu defined in [gui/menu.md](../gui/menu.md)
		- player_only: true
		- non_player_response: `pcrykh menu is player-only.`
		- success_chat_output: none
	- alternate_entrypoints:
		- hotbar beacon (see [gui/hotbar.md](../gui/hotbar.md))
	- permissions:
		- `pcrykh.use` is required to execute `/pcrykh`
		- no other permissions are defined
