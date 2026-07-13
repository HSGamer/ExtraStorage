package me.hsgamer.extrastorage.gui.config;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigNode;
import io.github.projectunified.craftconfig.annotation.ConfigPath;

@ConfigNode
@Comment(value = {"These are decorative items, which will make your GUI more beautiful!", "You can add/delete items in this section.", "You can only add 'commands' in this section."})
public interface DecorateItemsConfig {
    @Comment("Border decoration")
    @ConfigPath(value = "border", priority = 0)
    ItemConfig border();

    @Comment("Divider decoration")
    @ConfigPath(value = "divider", priority = 10)
    ItemConfig divider();
}
