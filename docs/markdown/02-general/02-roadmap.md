The Source++ Roadmap is a place for high-to-mid-level ideas around the future of Source++.
The purpose of the roadmap is to present a vision for Source++.
As a community of contributors, we can't work together toward something unless their is a shared vision. There are no dates nor deadlines in this roadmap, only a list of milestones.

# Source++ Vision

> The Path to AI-based Pair Programming

The ultimate vision for Source++ is to become a language-agnostic AI-based pair programing observation tool. In a pair programming scenario, there are two participants: the driver (the one who writes the code) and the observer (the one who ensures the code is acceptable). Source++ wishes to fulfill the observer's role with the advantage of seeing both the source code and the runtime behavior of its execution in much greater detail. These details can then be intelligently fed back to the developer during the development of relevant source code. These details could be things like:

- Actively failing source code in production
- Asynchronous method execution side effects
- Root cause detection
- SLA violation prediction
- etc

# Source++ Milestones

## 0.2.0 (alpha)

> Focus: Seamless local developer environment setup

 - H2 database storage ([#14](https://github.com/sourceplusplus/Assistant/issues/14)) ![](../../images/roadmap/Source++-Core-blue.svg)
 - Source++ Core Docker image on Docker Hub ([#15](https://github.com/sourceplusplus/Assistant/issues/15)) ![](../../images/roadmap/Source++-Core-blue.svg) ![](../../images/roadmap/Source++-Plugin-blue.svg)
 - Application/environment switching ([#16](https://github.com/sourceplusplus/Assistant/issues/16)) ![](../../images/roadmap/Source++-Plugin-blue.svg) ![](../../images/roadmap/Source++-Portal-blue.svg)
 - Min/max response time in quick stats ([#17](https://github.com/sourceplusplus/Assistant/issues/17)) ![](../../images/roadmap/Source++-Plugin-blue.svg) ![](../../images/roadmap/Source++-Portal-blue.svg)
 - Ability to switch overview graph ([#18](https://github.com/sourceplusplus/Assistant/issues/18)) ![](../../images/roadmap/Source++-Plugin-blue.svg) ![](../../images/roadmap/Source++-Portal-blue.svg)
 - View artifact slowest traces ([#19](https://github.com/sourceplusplus/Assistant/issues/19)) ![](../../images/roadmap/Source++-Core-blue.svg) ![](../../images/roadmap/Source++-Plugin-blue.svg) ![](../../images/roadmap/Source++-Portal-blue.svg)
 - Configurable environment setup (via Docker) in Source++ Plugin ([#20](https://github.com/sourceplusplus/Assistant/issues/20)) ![](../../images/roadmap/Source++-Plugin-blue.svg)
 - Responsive Source++ Tooltip UI ([#44](https://github.com/CodeBrig/Source/issues/44)) ![](../../images/roadmap/Source++-Tooltip-blue.svg)
 
## 0.3.0 (alpha)

> Focus: Language-agnostic source code parsing

 - Switch to source code parsing with Babelfish ![](../../images/roadmap/Source++-Core-blue.svg) ![](../../images/roadmap/Source++-Plugin-blue.svg)
 - Artifact view sharing ![](../../images/roadmap/Source++-Plugin-blue.svg) ![](../../images/roadmap/Source++-Portal-blue.svg)
 - Trace service detection and custom tooltip displays ![](../../images/roadmap/Source++-Core-blue.svg) ![](../../images/roadmap/Source++-Plugin-blue.svg) ![](../../images/roadmap/Source++-Portal-blue.svg)
 - Browser-based control panel 
 - Artifact, endpoint, log, trace searching functionality

## 0.4.0 (alpha)

> Focus: Automatic artifact monitoring

 - Automated artifact tracing based on overhead limit ![](../../images/roadmap/Source++-Agent-blue.svg) ![](../../images/roadmap/Source++-Core-blue.svg)
 - Artifact trace comparing ![](../../images/roadmap/Source++-Core-blue.svg) ![](../../images/roadmap/Source++-Plugin-blue.svg) ![](../../images/roadmap/Source++-Tooltip-blue.svg)

## 0.5.0 (alpha)

> Focus: Source code behavior predictions

 - Performance predictions ![](../../images/roadmap/Source++-Core-blue.svg)
 - GitSocratic 0.3+ integration

## 0.6.0 (beta)

> Additional APM/IDE integrations

 - stagemonitor integration ![](../../images/roadmap/Source++-Agent-blue.svg) ![](../../images/roadmap/Source++-Core-blue.svg)
 - Pinpoint integration ![](../../images/roadmap/Source++-Agent-blue.svg) ![](../../images/roadmap/Source++-Core-blue.svg)
 - Apache SkyWalking .NET & JetBrains Rider integration ![](../../images/roadmap/Source++-Agent-blue.svg) ![](../../images/roadmap/Source++-Plugin-blue.svg) ![](../../images/roadmap/Source++-Core-blue.svg)

## 0.7.0+ (beta)

> Offline source code analysis & real-time augmentations

## 1.0.0

> AI-based pair programming solution
