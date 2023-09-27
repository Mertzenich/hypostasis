# Hypostasis

![Hypostasis](./logo.png)

Automatically provision and initialize remote servers through user-defined configuration
files.

## Introduction

> Hypostasis signifies a particular substance as it is in its completeness.
> --Saint Thomas Aquinas, Summa Theologica, Tertia pars q.2 a.3 ad 2

Hypostasis allows you to unify a set of remote resources under a single set of configuration
files. For each server you would like to provision you can provide firewall rules,
environment variables, files to transfer, initialization commands and finally a script
to be executed remotely. You can use built-in server provider drivers, i.e. Digital
Ocean, or you can write your own to support any third-party server provider.

## Usage

Execute the latest jar file using Java 17 without any arguments to generate the default file structure.

``` sh
java -jar hypostasis-0.1.0-standalone.jar
```

## Supported Providers

- [x] Digital Ocean
- [ ] Vultr
- [ ] Linode
- [ ] Hetzner
- [ ] Microsoft Azure
- [ ] Amazon Web Services

## License

Copyright © 2023 Adam Mertzenich
