The following checklist will guide you through the process of setting up the necessary services to host Source++ locally via Docker. It should be noted that the Docker installation does not persist data and everything will be lost between container restarts.

To setup Source++ with Docker you can simply run the following command:

```bash
docker run --name Apache_SkyWalking -p 12800:12800 -p 11800:11800 -d apache/skywalking-oap-server:6.1.0
docker run --name SourcePlusPlus -p 8080:8080 -d sourceplusplus/core:v0.2.1-alpha
```

Note: Source++ will need to be integrated with Apache SkyWalking to enable usage. You can use the Source++ Plugin to enable this integration after running the above commands (see [Manage Environments](./06-configure-source-plugin.md#manage-environments)).

# Source++ Docker Checklist

- [Install Source++ Plugin](./05-install-source-plugin.md)
- [Configure Source++ Plugin](./06-configure-source-plugin.md)
- [Attach Source++ Agent](./07-attach-source-agent.md)
- [Configure Source++ Agent](./08-configure-source-agent.md)
- [Subscribe to Source Artifact](./09-subscribe-to-artifact.md)

# More Information

 - [Docker Hub](https://hub.docker.com/u/sourceplusplus)
