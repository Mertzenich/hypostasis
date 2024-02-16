# Development Guide

This page provides information on the structure of the project, how to build from
source, and how to contribute to development.

## Dependencies

Hypostasis is written in Clojure:
> Clojure is a robust, practical, and fast programming language with a set of
> useful features that together form a simple, coherent, and powerful tool
> (clojure.org).

You will need to download and [install](https://clojure.org/guides/install_clojure)
the Clojure language and tools.

We also use Leiningen as our project configuration and automation tool:
> Leiningen is the easiest way to use Clojure. With a focus on project
> automation and declarative configuration, it gets out of your way and
> lets you focus on your code (leiningen.org).

You should follow the installation instructions on the [Leiningen](https://leiningen.org)
website.

## Structure

### Core

The core application login is stored in `src/hypostasis/core.clj`. This is where
all of the logic pertaining to loading the configuration, performing provisioning,
initialization, and execution occurs. All logic pertaining to the actual general
application processes occurs here. It is relatively simple so as to allow plugins
to extend the behavior as they see fit.

### Plugins

Core plugin logic and individual plugin sources can be found in `src/hypostasis/plugins`.
The `plugin.clj` clojure file contains two types of things. First is the Plugin protocol
which plugins must implement to be able to work with Hypostasis. The second class
are utility functions which plugins are able to utilize. This file should only contain
those functions that can be used by all plugins. Some examples are `config-assoc`,
a utility function for modifying configuration atoms, and `fw-filter-abstract`
which aids in filtering out abstract firewall entries.

Each individual plugin is also contained alongside any utility functions.
The format for plugins is `name.clj` and `name_utils.clj`. For example, the Digital
Ocean plugin is found in `digitalocean.clj` and `digitalocean_utils.clj`.

Plugins are required to implement the `Plugin` protocol and a `create-instance` function
that takes a user config and creates the plugin with the config wrapped in an `atom`.

## Building

To build the application run `lein uberjar` in the project root directory.
This will build a standalone jar in the `target/uberjar` directory.
