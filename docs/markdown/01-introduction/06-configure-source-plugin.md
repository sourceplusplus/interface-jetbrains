Once you've downloaded and installed the Source++ Plugin, you will need to connect it to your Source++ Core server(s) and create/assign a Source++ application for/to your current project.

# Manage Environments

To connect the Source++ Plugin to a Source++ Core server go to:
 - `File >> Settings >> Source++ >> Manage Environments`

Depending on your setup, these settings may also be located at:
 - `File >> Settings >> Other Settings >> Source++ >> Manage Environments`

Here you will find the ability to connect to an existing Source++ Core server.

![](../../images/plugin/Manage%20Environments.png)

You can test this connection by clicking the "Test Connection" button. If everything worked correctly you should see something similar to the following results:

![](../../images/plugin/Successful%20Connection.png)

Note: Before closing the environment settings make sure to click the "Apply" button to save all changes.

# Add Integration

Once you have an environment created you will need to add integration(s) before you're able to do anything interesting with Source++.
This is done by clicking the "Add Integration" button, which will display several dialogs that must be filled in to establish a connection with an available integration (e.g. Apache SkyWalking). 

After successfully adding an integration and clicking the "Test Connection" button, you should see something similar to the following results:

![](../../images/plugin/Successful%20Integration.png)

# Create/Assign Application

Once you've successfully connected the Source++ Plugin there may be a final step necessary before being able to use Source++.
This is creating or assigning a Source++ application for the current project.

![](../../images/plugin/Status%20Connected.png)

If the project name and structure can be determined automatically this step can be skipped.
If this information cannot be determined automatically you must press the "Create/Assign Application" button.
This will bring up the following dialog:

![](../../images/plugin/Create%20Application.png)

If you are joining an existing application simply select the application from the list of available applications. If you are creating a Source++ application for the first time you may input the `Application Name` and `Application Domain`. The application name is a simple human-readable indicator of the application and the application domain represents the classes/packages which you wish to monitor.

To monitor multiple packages simply separate them with a comma like so:

- `com/company.*,org/othercompany.*`

Once this step is finished you should see a status of "Connected" and you're ready to go.

![](../../images/plugin/Application%20Connected.png)

# Next Step

- [Create Source Mark](./07-create-source-mark.md)
