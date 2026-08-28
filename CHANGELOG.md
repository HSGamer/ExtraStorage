> **This is a fairly large update. Make sure to test before uploading to production server**

* **Rework the Block Mining logic to use Drop Item Event**

  Unlike the old way that used Block Break & Item Spawn Event, Drop Item Event is a better way to catch the dropped items 
  from broken blocks since the server provides them as part of the event.

  That also means any plugin that cancels the event to do their own logic by manually dropping their own items would not
  be supported by the plugin. I don't intend to add support for any of them since the plugin is focused on Vanilla. 

* **Remove `BlockedMining` option**

  If the storage is full while mining blocks, the plugin will not block the mining. Instead, it will drop the items as
  how Vanilla does.

* **Add `deposit` command**

  This command allows the player to deposit a specific item to the storage.

* **Add `filter` command with specified item**

  If an item is specified in the command, the filter for that item will be toggled.

* **Allow Console to use Admin Commands**

* **Make some internal changes in the codebase**