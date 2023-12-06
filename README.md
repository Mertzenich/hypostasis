# Hypostasis

![Hypostasis](./logo.png)

Automatically provision and initialize remote servers through user-defined configuration
files.

## Introduction

> Hypostasis signifies a particular substance as it is in its completeness.
>
> -- Saint Thomas Aquinas, Summa Theologica, Tertia pars q.2 a.3 ad 2

Hypostasis allows you to unify a set of remote resources under a single set of configuration
files. For each server you would like to provision you can provide firewall rules,
environment variables, files to transfer, initialization commands and finally a script
to be executed remotely. You can use built-in server provider drivers, i.e. Digital
Ocean, or you can write your own to support any third-party server provider.

## Usage

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

## Supported Providers

- [x] Digital Ocean
- [x] Vultr
- [ ] Linode
- [ ] Hetzner
- [ ] Microsoft Azure
- [ ] Amazon Web Services

## License

Copyright © 2023 Adam Mertzenich
