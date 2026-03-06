# GUI navigation

- node: docs/gui/navigation.md
	- pagination:
		- page_size: 45 achievements
		- page_index: zero-based
		- total_pages: `ceil(count / 45)`
	- navigation_slots:
		- slot `45`: `BARRIER` named `Back`
		- slot `47`: `ARROW` named `Previous` when `page > 0`; otherwise empty
		- slot `49`: `PAPER` named `Page` with page metadata
		- slot `53`: `ARROW` named `Next` when `page < total_pages - 1`; otherwise empty
	- click_behavior:
		- clicking `Previous` opens `page - 1`
		- clicking `Next` opens `page + 1`
		- clicking `Back` opens [menu.md](menu.md)
