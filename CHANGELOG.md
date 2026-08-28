> **This is a fairly large update. Make sure to test before uploading to production server**

* **Rework the Block Mining logic to use Drop Item Event**

  Unlike the old way that used Block Break & Item Spawn Event, Drop Item Event is a better way to catch the dropped items 
  from broken blocks since the server provides them as part of the event.

  That also means any plugin that cancels the event to do their own logic by manually dropping their own items would not
  be supported by the plugin. I don't intend to add support for any of them since the plugin is focused on Vanilla.

* **Add more placeholders that can be used to make external menus**

  `%exstorage_items%` lists all the items in the storage.

  `%exstorage_items_material%` lists all the items in the storage in alphabetic order of the item material.

  `%exstorage_items_name%` lists all the items in the storage in alphabetic order of the item name.

  `%exstorage_items_quantity%` lists all the items in the storage in highest-first order of the item quantity.

  `%exstorage_item_quantity_<key>%` shows the quantity of the item in the storage, similar to `%exstorage_quantity_<key>%`.
  
  `%exstorage_item_material_<key>%` shows the material of the item.

  `%exstorage_item_name_<key>%` shows the name of the item.

  `%exstorage_item_lore_<key>%` shows the lore of the item.

* **Remove `BlockedMining` option**

  If the storage is full while mining blocks, the plugin will not block the mining. Instead, it will drop the items as
  how Vanilla does.

* **Add `deposit` command**

  This command allows the player to deposit a specific item to the storage.

* **Add `filter` command with specified item**

  If an item is specified in the command, the filter for that item will be toggled.

* **Allow Console to use Admin Commands**

* **Make some internal changes in the codebase**