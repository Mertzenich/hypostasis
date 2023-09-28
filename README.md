# Hypostasis

![Hypostasis](./logo.png)

Automatically provision and initialize remote servers through user-defined configuration
files.

## Introduction

> Hypostasis signifies a particular substance as it is in its completeness.
>
> --Saint Thomas Aquinas, Summa Theologica, Tertia pars q.2 a.3 ad 2

Hypostasis allows you to unify a set of remote resources under a single set of configuration
files. For each server you would like to provision you can provide firewall rules,
environment variables, files to transfer, initialization commands and finally a script
to be executed remotely. You can use built-in server provider drivers, i.e. Digital
Ocean, or you can write your own to support any third-party server provider.

## Usage

Generate an example starting directory by using the `init` argument:

``` sh
java -jar hypostasis*.jar
```

You should now have the default structure:

``` sh
├── config.edn
├── hypostasis.jar
└── servers
    └── default
        ├── server.edn
        ├── toinstall.txt
        └── word.txt
```

After ensuring that you have added your SSH key to your ssh-agent, set the TOKEN
environmental variable and use the `run` argument to launch the server:

``` sh
java -jar hypostasis*.jar run
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
