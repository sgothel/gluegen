<!---
We convert markdown using pandoc using `markdown+lists_without_preceding_blankline` as source format
and `html5+smart` with a custom template as the target.

Recipe:
```
  ~/pandoc-buttondown-cgit/pandoc_md2html_local.sh GlueGen_Mapping.md > GlueGen_Mapping.html
```

Git repos:
- https://jausoft.com/cgit/users/sgothel/pandoc-buttondown-cgit.git/about/
- https://github.com/sgothel/pandoc-buttondown-cgit
-->

<style>
table, th, td {
   border: 1px solid black;
}
</style>

# JogAmp's Supported Platforms
Jogamp 2.6.x runtime is supported on the described platforms below.

Please ask for contracting options to
have support added for your desired platform.

## Unit Testing
Thorough unit testing is performed on all our [jenkins build nodes](https://jogamp.org/chuck/job/jogl/).

## Bring-up Testing
### Non Android
A quick bring-up testing covers `gluegen`, `joal` and `jogl`
and is performed on non Android platforms:

```
#!/bin/sh

type=archive/rc
version=v2.6.0-rc-20250721
folder=${type}/${version}

mkdir ${version}
cd ${version}

curl --silent --output jogamp-fat.jar  https://jogamp.org/deployment/${folder}/fat/jogamp-fat.jar
curl --silent --output jogl-demos.jar https://jogamp.org/deployment/${folder}/fat/jogl-demos.jar
curl --silent --output jogl-fonts-p0.jar https://jogamp.org/deployment/${folder}/fat/jogl-fonts-p0.jar

echo "Fetched from ${folder} to ${version}"

java -cp jogamp-fat.jar:jogl-demos.jar com.jogamp.opengl.demos.graph.ui.UISceneDemo20
```

However, you can also use the locally produced fat jar file for the
building platform, e.g.

```
cd jogl/build-x86_64/jar
java -cp jogl-fat-linux-amd64.jar:jogl-demos.jar com.jogamp.opengl.demos.graph.ui.UISceneDemo20
```

### Android
For Android, the fat demo APK `jogl-demos-fat-android-${arch}.apk`
inside the jogl build folder can be directly installed and tested
on your device w/ developer mode enabled.

## Java / OpenJDK
Current runtime requirements

- Java 8 (class file 52)
- [OpenJDK](http://openjdk.java.net/) >= 8, tested on [Adoptium Builds](https://adoptium.net/temurin/releases/)
  - OpenJDK 21
  - OpenJDK 17
  - OpenJDK 11
  - Following [OpenJDK](http://openjdk.java.net/) versions are no more tested, but may work
    - OpenJDK 10
    - OpenJDK 9
    - OpenJDK 8
- [Azul's Zulu](https://www.azul.com/downloads/zulu-community/) (untested)
- [Avian](https://github.com/ReadyTalk/avian) (untested)

Future versions may use

- Java 11 (class file 55)
- Android SDK API level 32 (Version 12 Snow Cone, released 2022)
  - Supposed to be [Java 11 API compliant](https://developer.android.com/build/jdks)

*See contracting options above*

## Windows

- Windows >= 10
- Windows 2000 (should work)
- Windows 8 (should work)
- Windows 7 (patched, should work)

### Current Architectures:
- `x86_64`

### Offline Architectures
Currently not tested anymore, code may exist.

- `x86`

### Potential Architectures
- `arm64` (`aarch64`)

*See contracting options above*

## GNU/Linux

### Dependencies
GNU/Linux builds' *GNU libc* dependencies are relaxed
by utilizing lower or no versioning to the few existing versioned library entries used.
Therefor it is expected to be able to run on older and newer distributions.

### Distributions
- Debian [LTS](https://wiki.debian.org/LTS) and [Releases](https://www.debian.org/releases/)
  - Debian 11 *Bullseye* LTS until 2026-08-31
  - Debian 12 *Bookworm* (future LTS on 2026-06-11)
  - Debian 13 *Trixie*
- [Ubuntu](https://www.releases.ubuntu.com/)
  - Ubuntu 24.04.2 LTS (2025-02-20)
  - Ubuntu 22.04.5 LTS (2024-09-12)
  - Ubuntu 20.04.6 LTS (2023-03-22)


### Current Architectures:
- `x86_64`
- `arm64` (`aarch64`)
- `armv6/armv7` (hardfloat)

### Offline Architectures
Currently not tested anymore, code may exist.

- `x86`
- `ia64`
- `riscv64`
- `mipsel`
- `ppc64le`
- `sparcv9`
- `alpha`
- `hppa`

*See contracting options above*

## Android/Linux
- Android Version 8.0 Oreo, API Level 26 or later
  - *minSdkVersion*: 26 (Android 8, Oreo)
  - *targetSdkVersion*: 35 (Android 15), as required as of 2025-08-31

On 2025-07-20 we have tested `jogl-demos-fat-android-${arch}.apk`
using an `x86` and `x86_64` emulator for

- Android  8, API 26
- Android 14, API 34
- Android 15, API 35

### Current Architectures:
- `x86_64`
- `arm64` (`aarch64`)

### Offline Architectures
Currently not tested regularly, code may exist.

- `x86`
- `armv6/armv7` (hardfloat)

*See contracting options above*

## OpenSolaris / Illumus
Currently not tested anymore, code may exist.

### Current Architectures:

### Offline Architectures
Currently not tested anymore, code may exist.

- `x86_64`
- `x86`
- `sparcv9`

*See contracting options above*

## MacOS

References

- [Mac OS Version History (wiki)](https://en.wikipedia.org/wiki/MacOS_version_history).
- [Xcode Version Comparison Table (wiki)](https://en.wikipedia.org/wiki/Xcode#Version_comparison_table)

### Overview

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

### OpenJDK

Available Java(tm) VMs

- [OpenJDK](http://openjdk.java.net/) build @ [Adoptium](https://adoptium.net/temurin/releases/)
  - [Adoptium Supported MacOS Versions](https://adoptium.net/supported-platforms/)
    - MacOS 10.15, 11, 12, 13, 15 for `x86_64` and `arm64`

### JogAmp Build and Test Setup

#### MacOS 12.6.5 (Monterey), Darwin 21, `x86_64`

  - Build and main test machine
  - XCode 14.2 w/ SDK 11.3
    - `export SDKROOT=macosx11.3` (*MacOS SDK*)
    - `-mmacosx-version-min=10.7` (*Miniumum deployment target*)
  - OpenJDK Temurin 21.0.7+6-LTS

#### MacOS 15.5 (Sequoia), Darwin 24, `arm64`

  - Test machine
  - OpenJDK Temurin 21.0.7+6-LTS

#### Retired

Currently not tested anymore, but should still work.

##### MacOS 10.13.6 (High Sierra), Darwin 17, `x86_64`

  - Test machine
  - OpenJDK Temurin 17.0.5+8
  - Retired

##### MacOS 13.1 (Ventura), Darwin 22, `arm64`

  - Test machine
  - OpenJDK Temurin 17.0.5+8
  - Retired

## Change History

| Date       | Note                                     |
|:-----------|:-----------------------------------------|
| 2023-05-06 | Initial Version for JogAmp Release 2.5.0 |
| 2025-06-21 | JogAmp Release 2.6.0                     |
