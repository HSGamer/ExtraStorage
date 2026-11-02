- **Retain item spawn listener for all versions**
  - A user in my Discord server has requested to keep the break-then-item-spawn logic for their custom plugin which 
    would spawn new items in the broken block. Therefore, I took this time to merge both the drop and item spawn listeners
    into one. Now both listeners work together to handle both Vanilla Drop and Custom Item Spawn when breaking blocks.