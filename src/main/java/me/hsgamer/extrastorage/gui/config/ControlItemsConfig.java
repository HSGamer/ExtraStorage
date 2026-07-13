package me.hsgamer.extrastorage.gui.config;

import io.github.projectunified.craftconfig.annotation.Comment;
import io.github.projectunified.craftconfig.annotation.ConfigNode;
import io.github.projectunified.craftconfig.annotation.ConfigPath;

@ConfigNode
@Comment(value = {"Control items for the GUI", "Please do not delete any items in this section.", "If you don't want to display these items on GUI, just set their slot to -1."})
public interface ControlItemsConfig {
    @Comment("This item is used to display the user's storage information")
    @ConfigPath(value = "About", priority = 0)
    ItemConfig about();

    @Comment("Back to previous page")
    @ConfigPath(value = "PreviousPage", priority = 10)
    ItemConfig previousPage();

    @Comment("Go to next page")
    @ConfigPath(value = "NextPage", priority = 20)
    ItemConfig nextPage();

    @Comment("Switching button between GUIs")
    @ConfigPath(value = "SwitchGui", priority = 30)
    ItemConfig switchGui();
}
