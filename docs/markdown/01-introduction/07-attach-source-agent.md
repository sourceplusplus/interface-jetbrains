# Using Apache SkyWalking

## JetBrains IDE

The Source++ Plugin will attach the Source++ Agent to each application executed from the JetBrains IDE by default.

## Java

To attach the Source++ Agent for use with the Apache SkyWalking APM outside of the IDE you will need to:

-  supply the desired Apache SkyWalking activations and plugins
-  trigger the Source++ Agent by including the `-javaagent` option during application boot

### Supply activations & plugins

By default Apache SkyWalking will not monitor any technologies which are not included in the activations and plugins folders. Both of these folders should be placed in the same directory as the `source-agent-x.x.x.jar` file to activate them.

For more information about the activations and plugins available see: [here](https://github.com/apache/incubator-skywalking/blob/master/docs/en/setup/service-agent/java-agent/README.md)

### Attach Source++ Agent

```bash
java -jar -javaagent:source-agent-0.2.5.jar MyApplication.jar
```

You may additional supply a `SOURCE_CONFIG` system property to point to a custom agent configuration file:

```bash
java -jar -DSOURCE_CONFIG=source-agent.json -javaagent:source-agent-0.2.5.jar MyApplication.jar
```

# Next Step

- [Subscribe to Source Artifact](./09-subscribe-to-artifact.md) or [Configure Source++ Agent](./08-configure-source-agent.md)
