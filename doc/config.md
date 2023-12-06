# Configuration

This document will explain the Hypostasis configuration file.

## Config File

The entire `config.edn` file contains a list of individual server configurations.
An example could look something like this:

```edn
[{:name :Primary
  :plugin :DigitalOcean
  :firewall [{:protocol "tcp" :ports "22"} ; No source, assume allow all i.e. 0.0.0.0
             {:protocol "tcp" :ports "80"}
             {:protocol "tcp" :ports "8000" :source :Alt}]
  :env [["DEBIAN_FRONTEND" "noninteractive"]]
  :transfer ["main.py"]
  :init ["apt-get install -y python3-fastapi python3-uvicorn"
         "ufw allow 8000/tcp"]
  :exec "python3 -m uvicorn main:app --host 0.0.0.0"
  :settings {:token "TOKEN HERE"
             :ssh-key "SSH KEY HERE"}}
 {:name :Alt
  :plugin :Vultr
  :firewall [{:protocol "tcp" :ports "22"}
             {:protocol "tcp" :ports "80"}
             {:protocol "tcp" :ports "8000"}]
  :env [["DEBIAN_FRONTEND" "noninteractive"]]
  :transfer ["app.py"]
  :init ["apt-get install -y python3-flask"
         "ufw allow 8000/tcp"]
  :exec "flask run --host 0.0.0.0 --port 8000"
  :settings {:token "TOKEN HERE"
             :ssh-key-id "SSH KEY HERE"}}]
```

An empty configuration, containing no servers, would simply be an empty list `[]`.
Each individual server configuration entry is dictionary/map format `{:key val}`.
Every server in the configuration needs to be in the correct format for the application
to run properly.

## Server Entry

A server entry in the configuration contains following components:

- `:name` the name of a server as a keyword (i.e. `:Primary`)
- `:plugin` the name of the plugin as a keyword (i.e. `:DigitalOcean`)
- `:firewall` a list of firewall entries
- `:env` a list of key-value environment pairs (i.e. `[["DEBIAN_FRONTEND" "noninteractive"]]`)
- `:transfer` a list of files to transfer to the remote server (i.e. `["main.py"]`)
- `:exec` the command string to execute on the server (i.e. `"python main.py"`)
- `:settings` a map containing settings required by each individual plugin

### Firewall Entries

Below is an example list of firewall entries:

```edn
[{:protocol "tcp" :ports "22"}
 {:protocol "tcp" :ports "80"}
 {:protocol "tcp" :ports "25565" :source "192.168.1.1"}]
 {:protocol "tcp" :ports "8000" :source :Alt}]
 ```

Each firewall entry is a map that must contain a `:protocol` and `:ports` value.
Omitting a `:source` value will open the port to the internet.
The `:source` value can either be an IP address string or a keyword referencing
another server defined in the configuration.
