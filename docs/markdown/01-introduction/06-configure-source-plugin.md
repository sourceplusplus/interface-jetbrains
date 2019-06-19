Once you've downloaded and installed the Source++ Plugin, you will need to connect it to your Source++ Core server(s) and create/assign a Source++ application for/to your current project.

# Manage Environments

To connect the Source++ Plugin to a Source++ Core server go to:
 - `File >> Settings >> Source++ >> Manage Environments`

Depending on your setup, these settings may also be located at:
 - `File >> Settings >> Other Settings >> Source++ >> Manage Environments`

Here you will find the ability to connect to an existing Source++ Core server as well as the ability to setup a new Source++ Core server (via Docker).

![](../../images/plugin/Manage%20Environments.png)

Clicking "Setup via Docker" will bring up the following window and begin downloading (if necessary) and booting (if necessary) a Source++ Core server:

![](../../images/plugin/Setup%20via%20Docker.png)

Alternatively, you can achieve the same thing by running:

```bash
docker run -p 8080:8080 -d sourceplusplus/core-and-apache-skywalking:v0.2.1-alpha
```

After that you can click "Create" and manually input the required connection settings.

![](../../images/plugin/Connect%20Source++.png)

If your Source++ Core server is in secure mode you will also need to input the API token.

You can test this connection by clicking the "Test Connection" button. If everything worked correctly you should see something similar to the following results:

![](../../images/plugin/Successful%20Connection.png)

Note: Before closing the environment settings make sure to click the "Apply" button to save all changes.

# Create/Assign Application

![](../../images/plugin/Status%20Connected.png)

Once you've successfully connected the Source++ Plugin there is a final step required before being able to use Source++. This is creating or assigning a Source++ application for the current project. Pressing the "Create/Assign Application" button will bring up the following dialog:

![](../../images/plugin/Create%20Application.png)

If you are joining an existing application simply select the application from the list of available applications. If you are creating a Source++ application for the first time you may input the `Application Name` and `Application Domain`. The application name is a simple human-readable indicator of the application and the application domain represents the classes/packages which you wish to monitor.

To monitor multiple packages simply separate them with a comma like so:

- `com/company.*,org/othercompany.*`

Once this step is finished you should see a status of "Connected" and you're ready to go.

![](../../images/plugin/Application%20Connected.png)

# Next Step

- [Attach Source++ Agent](./07-attach-source-agent.md)
