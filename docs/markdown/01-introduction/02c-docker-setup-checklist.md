The following checklist will guide you through the process of setting up the necessary services to host Source++ locally via Docker. It should be noted that the Docker installation does not persist data and everything will be lost between container restarts.

To setup Source++ with Docker you can simply run the following command:

```bash
docker run -p 8080:8080 -d sourceplusplus/core-and-apache-skywalking:v0.2.1-alpha
```

Alternatively, the Source++ Plugin is also capable of installing the necessary services via Docker automatically. To access this functionality simply follow through the checklist below.

# Source++ Docker Checklist

- [Install Source++ Plugin](./05-install-source-plugin.md)
- [Configure Source++ Plugin](./06-configure-source-plugin.md)
- [Attach Source++ Agent](./07-attach-source-agent.md)
- [Configure Source++ Agent](./08-configure-source-agent.md)
- [Subscribe to Source Artifact](./09-subscribe-to-artifact.md)

# More Information

 - [Docker Hub](https://hub.docker.com/u/sourceplusplus)
