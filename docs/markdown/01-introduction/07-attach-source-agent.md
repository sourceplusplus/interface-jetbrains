# Attach Source++ Agent

## JetBrains IDE

The Source++ Plugin will attach the Source++ Agent to each application executed from the JetBrains IDE by default.

## Java

To attach the agent outside of the IDE simply supply the `-javaagent` option during application boot. Like so:

```bash
java -jar -javaagent:source-agent-0.1.1.jar MyApplication.jar
```

You may additional supply a `SOURCE_CONFIG` environment variable to point to a custom agent configuration file. Like so:

```bash
java -jar -DSOURCE_CONFIG=custom-config.json -javaagent:source-agent-0.1.1.jar MyApplication.jar
```

# Next Step

- [Subscribe to Source Artifact](./09-subscribe-to-artifact.md) or [Configure Source++ Agent](./08-configure-source-agent.md)
