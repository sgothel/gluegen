# MacOS Versions Related to JogAmp

References

- [Mac OS Version History (wiki)](https://en.wikipedia.org/wiki/MacOS_version_history).
- [Xcode Version Comparison Table (wiki)](https://en.wikipedia.org/wiki/Xcode#Version_comparison_table)

## Overview

| MacOS Version | Release Name | Darwin Version | JogAmp Relation                            |
|:--------------|:-------------|:---------------|:-------------------------------------------|
| 10.7            | Lion       | 11             | Min deployment target                      |
|               |              |                |                                            |
| 10.13         | High Sierra  | 17             | Test node 10.13.6, `x86_64`                |
| 10.14         | Mojave       | 18             |                                            |
| 10.15         | Catalina     | 19             |                                            |
|               |              |                |                                            |
| 11            | Big Sur      | 20             | Build node 12.6.5, w/ Xcode 14.2, `x86_64` |
| 12            | Monterey     | 21             |                                            |
| 13            | Ventura      | 22             | Test node 13.1, `arm64`                    |

## OpenJDK

Available Java(tm) VMs

- [OpenJDK](http://openjdk.java.net/) build @ [Adoptium](https://adoptium.net/temurin/releases/)
  - [Adoptium Supported MacOS Versions](https://adoptium.net/supported-platforms/)
    - MacOS 10.15, 11, 12, 13 for `x86_64` and `arm64`

## JogAmp Build and Test Setup

* MacOS 12.6.5 (Monterey), Darwin 21, `x86_64`
  * Build and main test machine
  * XCode 14.2 w/ SDK 11.3
    * `export SDKROOT=macosx11.3` (*MacOS SDK*)
    * `-mmacosx-version-min=10.7` (*Miniumum deployment target*)
  * OpenJDK Temurin 17.0.5+8
* MacOS 10.13.6 (High Sierra), Darwin 17, `x86_64`
  * Test machine
  * OpenJDK Temurin 17.0.5+8
* MacOS 13.1 (Ventura), Darwin 22, `arm64`
  * Test machine
  * OpenJDK Temurin 17.0.5+8

## Change History

| Date       | Note                                     |
|:-----------|:-----------------------------------------|
| 2023-05-06 | Initial Version for JogAmp Release 2.5.0 |
