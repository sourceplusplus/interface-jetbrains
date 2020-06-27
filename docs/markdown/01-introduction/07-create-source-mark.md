Once the Source++ Plugin has been installed, and the appropriate integrations have been added the final step which may be necessary to is configure any source code artifacts which Source++ should pay special attention to.
The appropriate necessary configuration will depend on which integrations are enabled.

For now, with Apache SkyWalking being the only available integration the only configuration which may be necessary is to designate which methods in your code are to be considered "entry methods".
An "entry method" is a method which receives a request from a user of your application.
For example, if your application offers a REST API the methods which are the first to be called when a user sends a request would be considered an entry method.
For popular frameworks like Spring MVC, Vert.x, etc. these methods will be determined automatically so there may be no configuration necessary.

Methods which have been determined (or configured) to be an entry method will show up as such:

![](../../images/plugin/Entry%20Method%20Artifact.png)

The symbol (![](../../images/plugin/icons/entry_method/active_entry_method.svg)) signifies the method directly to the right has been marked as an entry method.

If instead you see the symbol (![](../../images/plugin/icons/entry_method/inactive_entry_method.svg)) this signifies the method directly to the right has been configured to be an entry method but there is currently no active integration which can utilized this information.
To solve this you would need to make sure to add the Apache SkyWalking integration to Source++ Core.

If you do not see either symbol next to a method which you wish to be considered an entry method you will need to manually configure it to be so.
You can do this by viewing the Source++ Portal and editing the artifact's configuration. Please see the next step for instructions on viewing the Source++ Portal for a particular artifact.

# Next Step

- [View to Source++ Portal](./08-view-source-portal.md)
