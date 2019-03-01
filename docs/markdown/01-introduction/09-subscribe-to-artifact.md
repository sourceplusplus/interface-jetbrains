A central concept in Source++ is the subscription of source code artifact runtime information.

Once subscribed to an artifact you can choose to receive various runtime data about that artifact.
This currently only includes metrics and traces.

Subscribing to an artifact is fairly simply and only requires you to place your keyboard's cursor
in the method which you wish to subscribe to and pressing `ALT + SPACE`. This will bring up the available intentions.
With the Source++ Plugin installed you will see:

![](../../images/plugin/Subscribe%20To%20Artifact.png)

Choosing `Subscribe to this artifact` will inform the Source++ Core that a developer is interested in runtime
information related to the chosen artifact and will instruct the Source++ Agent to collect this information, if necessary.

Once subscribed the S++ mark will show like so:

![](../../images/plugin/Subscribed%20Artifact.png)

The greyed out S++ symbol indicates that the artifact is subscribed to and runtime data will be collected but that there is none available. 
Once the method has been executed and runtime data becomes available you will be able to view collected data like so:

![](../../images/plugin/Active%20Artifact.png)

You can view the differences between these two indicators with the following example code:

```java
public class ActiveVsInactive {
    public static void main(String[] args) throws Exception {
        while (true) {
            for (int i = 1; i <= 10; i++) {
                activeArtifact(100 * i);
            }
        }
    }

    static void activeArtifact(int sleepTime) throws Exception {
        Thread.sleep(sleepTime); //pretend we’re doing something interesting
    }//S++{62905bdc-eeca-4f04-bc81-9a16f5e60d28;ActiveVsInactive.activeArtifact(int);10}

    static void inactiveArtifact() {
        //artifact is subscribed to but never invoked
    }//S--{62905bdc-eeca-4f04-bc81-9a16f5e60d28;ActiveVsInactive.inactiveArtifact();14}
}
```

The `activeArtifact` method shows the active S++ mark because an artifact subscription is active and the method is being executed. These observations will be recorded and the artifact will be marked as active. The `inactiveArtifact` method shows the inactive S++ mark because this method is never executed. If it was it would turn into an active artifact subscription and appear just as `activeArtifact` currently does.

# More Information

- [Source++ Architecture](../02-general/01-architecture.md)
