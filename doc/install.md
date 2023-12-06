# Installation

This document will teach you how to download/install Hypostasis.
You will also learn how you can run the default sample project.
You can learn more about the configuration format on the
[configuration documentation](config.md) page.

## Dependencies

Hypostasis has two main dependencies, the Java Runtime and OpenSSH.
It has been only tested on Arch Linux, but should work anywhere you
can use the JVM. Below are links for `JDK17`, you will need to ensure
you have the OpenSSH remote login client installed properly for your
operating system.

### Java Development Kit and Runtime Environment

- [Linux](https://www.oracle.com/java/technologies/downloads/#jdk17-linux)
- [MacOS](https://www.oracle.com/java/technologies/downloads/#jdk17-mac)
- [Windows](https://www.oracle.com/java/technologies/downloads/#jdk17-windows)

## Download Release

Download the latest release `hypostasis.jar` on the GitHub [releases page](https://github.com/mertad01/hypostasis/releases/latest).
and put the jar file into a new directory.

## Initial Setup

Generate an example starting project by using the `init` command:

``` sh
java -jar hypostasis*.jar init
```

You should now have the default structure:

``` sh
├── hypostasis.jar
├── config.edn
├── app.py
└── main.py
```

Edit the `config.edn` file to include the proper settings for each driver.
In the default you will need to provide your API tokens and SSH public key
information for Vultr and Digital Ocean.

After you have configured your remote servers, use the `run` command to launch
your servers:

``` sh
java -jar hypostasis*.jar run
```

This will provision two servers: a web server and an api. The API can only
be accessed by the web server. The web server serves a random number provided
by the API.
