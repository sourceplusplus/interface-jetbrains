Once you've downloaded and installed the Source++ Plugin, you will need to connect it to your Source++ Core server(s). The plugin itself requires at minimum a valid host. An API key may optionally be required if the Source++ Core server has authentication enabled.

# Connect Source++ Plugin

To connect the Source++ Plugin to a Source++ Core server go to: `File >> Settings >> Source++`. Depending on your setup the Source++ settings may also be located at: `File >> Settings >> Other Settings >> Source++`.

![](../../images/plugin/Source++%20Settings.png)

Here you will find the ability to connect to a Source++ Core server. Clicking connect will bring up the following window:

![](../../images/plugin/Connect%20Source++.png)

Input the host and API key (if necessary). You can test this connection by pressing "Test Connection". If everything works correctly you should see something similar to the following output:

![](../../images/plugin/Successful%20Connection.png)

# Create/Assign Application

Once you've successfully connected the Source++ Plugin there is a final step before being able to use the plugin. This is creating or assigning a Source++ application for the current project. Pressing the "Create/Assign Application" button will bring up the following dialog:

![](../../images/plugin/Create%20Application.png)

If you are joining an existing application simply select the application from the list of available applications. If you are creating a Source++ application for the first time you may input the `Application Name` and `Application Domain`. The application name is a simple human-readable indicator of the application and the application domain represents the classes/packages which you wish to monitor. To monitor multiple packages simply separate them with a comma like so: `com/company.*,org/othercompany.*`.

![](../../images/plugin/Application%20Connected.png)

# Next Step

- [Attach Source++ Agent](./07-attach-source-agent.md)
