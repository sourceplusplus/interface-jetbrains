Before being able to augment your coding experience with Source++, there is some setup work to do.

1. At a minimum you must download the Source++ Plugin for your given IDE.
2. Since Source++ relies on Apache SkyWalking for the analysis of source code metrics and traces,
an Apache SkyWalking OAP server must be online and accessible to the Source++ Core server.
3. You will need to set up a Source++ Core server for the Source++ Plugin to connect to. The Source++ Core server acts as a middleman between the Source++ Plugin/Portal and the available source code related integrations.

After you have completed these steps you will be able to view your application's runtime performance in real-time contextualized to the source code displayed in your IDE.

# Setup Checklist

To make things simple, we have provided two setup checklists. The first checklist is for those who wish to self-host Source++ (completely cost-free) and the second checklist is for those who have purchased Source++ Cloud (dedicated instances fully managed by the Source++ team).
The self-hosted setup is more complex, but of course, allows for more customization.

- [Self-Hosted Setup Checklist](02a-self-hosted-setup-checklist.md)
- [Source++ Cloud Setup Checklist](02b-source-cloud-setup-checklist.md)
