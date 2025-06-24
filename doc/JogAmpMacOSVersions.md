<style>
table, th, td {
   border: 1px solid black;
}
</style>

# JogAmp's MacOS Version Support

References

- [Mac OS Version History (wiki)](https://en.wikipedia.org/wiki/MacOS_version_history).
- [Xcode Version Comparison Table (wiki)](https://en.wikipedia.org/wiki/Xcode#Version_comparison_table)

## Overview

| MacOS Version | Release Name | Darwin Version | JogAmp Relation                            |
|:--------------|:-------------|:---------------|:-------------------------------------------|
| 10.7          | Lion         | 11             | Min deployment target                      |
|               |              |                |                                            |
| 10.13         | High Sierra  | 17             | Test node 10.13.6, `x86_64` (retired)      |
| 10.14         | Mojave       | 18             |                                            |
| 10.15         | Catalina     | 19             |                                            |
|               |              |                |                                            |
| 11            | Big Sur      | 20             |                                            |
| 12            | Monterey     | 21             | Build node 12.6.5, w/ Xcode 14.2, `x86_64` |
| 13            | Ventura      | 22             | Test node 13.1, `arm64` (retired)          |
| 15            | Sequoia      | 24             | Test node 15.5, `arm64`                    |

Exceptions:
- JOAL's build-in OpenAL-Soft requires MacOS 10.13

## OpenJDK

Available Java(tm) VMs

- [OpenJDK](http://openjdk.java.net/) build @ [Adoptium](https://adoptium.net/temurin/releases/)
  - [Adoptium Supported MacOS Versions](https://adoptium.net/supported-platforms/)
    - MacOS 10.15, 11, 12, 13, 15 for `x86_64` and `arm64`

## JogAmp Build and Test Setup

### MacOS 12.6.5 (Monterey), Darwin 21, `x86_64`

  - Build and main test machine
  - XCode 14.2 w/ SDK 11.3
    - `export SDKROOT=macosx11.3` (*MacOS SDK*)
    - `-mmacosx-version-min=10.7` (*Miniumum deployment target*)
  - OpenJDK Temurin 21.0.7+6-LTS

### MacOS 15.5 (Sequoia), Darwin 24, `arm64`

  - Test machine
  - OpenJDK Temurin 21.0.7+6-LTS

### Retired

Currently not tested anymore, but should still work.

#### MacOS 10.13.6 (High Sierra), Darwin 17, `x86_64`

  - Test machine
  - OpenJDK Temurin 17.0.5+8
  - Retired

#### MacOS 13.1 (Ventura), Darwin 22, `arm64`

  - Test machine
  - OpenJDK Temurin 17.0.5+8
  - Retired

## Change History

| Date       | Note                                     |
|:-----------|:-----------------------------------------|
| 2023-05-06 | Initial Version for JogAmp Release 2.5.0 |
| 2025-06-21 | JogAmp Release 2.6.0                     |
