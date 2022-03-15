# ![](https://github.com/sourceplusplus/live-platform/blob/master/.github/media/sourcepp_logo.svg)

[![License](https://camo.githubusercontent.com/93398bf31ebbfa60f726c4f6a0910291b8156be0708f3160bad60d0d0e1a4c3f/68747470733a2f2f696d672e736869656c64732e696f2f6769746875622f6c6963656e73652f736f75726365706c7573706c75732f6c6976652d706c6174666f726d)](LICENSE)
![GitHub release](https://img.shields.io/github/v/release/sourceplusplus/interface-jetbrains?include_prereleases)
[![Build](https://github.com/sourceplusplus/interface-jetbrains/actions/workflows/build.yml/badge.svg)](https://github.com/sourceplusplus/interface-jetbrains/actions/workflows/build.yml)

# What is this?

<!-- Plugin description -->

This project contains the JetBrains IDE plugin for [Source++](https://github.com/sourceplusplus/live-platform), the open-source live coding platform. This plugin also works for regular [SkyWalking](https://github.com/apache/skywalking) installations, but [Live Instrument](#live-instruments) commands will not be available.

<!-- Plugin description end -->

# Live Views

**Live View** commands utilize existing SkyWalking metrics to bring live data of your code to your IDE.

<details>
  <summary><h3>Show Commands</h3></summary>

  ### Display Quick Stats

  > Inlay hints which indicate an endpoint's current activity.

  <details>
    <summary>Screencast</summary>

  ![screencast](https://user-images.githubusercontent.com/3278877/158376181-7fe597f9-f3c2-4609-bd07-4ea55e10b579.gif)
  </details>

  ### Watch Log

  > Follow specific logging statements in real-time.

  <details>
    <summary>Screencast</summary>

    ![screencast](https://user-images.githubusercontent.com/3278877/158381411-214285ba-7291-4c70-8e1f-8489140fa239.gif)
  </details>

  ### View Portal

  > Contextual popups for displaying live operational data on the code currently in view.

  <details>
    <summary>Screencast</summary>

    ![screencast](https://user-images.githubusercontent.com/3278877/149158868-135568d5-20cc-44d4-886a-2202195b594b.gif)
  </details>
  
</details>

# Live Instruments

**Live Instrument** commands require a [Live Probe](https://github.com/sourceplusplus/probe-jvm) to inject additional metrics into your code for live debugging.

<details>
  <summary><h3>Show Commands</h3></summary>

  ### Add Breakpoint

  > Live Breakpoints (a.k.a. non-breaking breakpoints) are useful debugging instruments for gaining insight into the live variables available in production at a given scope.

  <details>
    <summary>Screencast</summary>

    ![live-breakpoint](https://user-images.githubusercontent.com/3278877/136304451-2c98ad30-032b-4ce0-9f37-f98cd750adb3.gif)
  </details>

  ### Add Log

  > Live Logs (a.k.a. just-in-time logging) are quick and easy debugging instruments for instantly outputting live data from production without redeploying or restarting your application.

  <details>
    <summary>Screencast</summary>

    ![live-log](https://user-images.githubusercontent.com/3278877/136304738-d46c2796-4dd3-45a3-81bb-5692547c1c71.gif)
  </details>

</details>
