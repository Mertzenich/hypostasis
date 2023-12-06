# Introduction

Hypostasis allows you to unify a set of remote resources under a single set of configuration
files. For each server you would like to provision you can provide firewall rules,
environment variables, files to transfer, initialization commands and finally a script
to be executed remotely. You can use built-in server provider drivers, i.e. Digital
Ocean, or you can write your own to support any third-party server provider.

## Who This is For

### Who It's For

This project aims to make remote server provisioning as pain-free as possible for
smaller projects. Use this project if you have an application which requires multiple
steps when deploying.

### Who It's NOT For

If you desire auto-scaling, monitoring the status of your remote servers in
real-time through a unified interface (besides seeing the `sout` feed), or
complex inter-server scripting capabilities then this project is not for you.

## Table of Contents

- [Installation](install.md)
- [Configuration](config.md)
- [Development](develop.md)
