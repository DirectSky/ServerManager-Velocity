# ServerManager

A Velocity plugin for managing Minecraft servers through the Pterodactyl API.


## Features

* Start, Stop and Restart Pterodactyl servers via Command
* Scheduled automatic restarts
* Configurable restart times and days of the week
* Configurable messages
* Permission-based commands
* Fully configurable through YAML files

## Requirements

* Velocity
* Java 21 or newer
* A Pterodactyl panel
* A Pterodactyl API token
* Network access from the Velocity proxy to the Pterodactyl API

## Installation

1. Download the latest ServerManager release.
2. Place the plugin JAR into the `plugins` directory of your Velocity proxy.
3. Start the Proxy.
4. Configure everything in the config.
5. Restart the Proxy.

After installation, ServerManager will create its configuration files in:

```text
plugins/ServerManager/
```

## Configuration

ServerManager uses separate configuration files for different parts of the plugin.

### `config.yml`

The main configuration file contains the Pterodactyl connection and server definitions.

Example:

```yaml
pterodactyl:
  url: "https://panel.example.com"
  api-key: "YOUR_API_KEY"

servers:
  lobby:
    id: "SERVER_ID"
  survival:
    id: "SERVER_ID"
```

The exact configuration options depend on the version of ServerManager you are using. Refer to the generated configuration file for the available options.

### `messages.yml`

All messages can be customized in the `messages.yml`.


### `restarttimes.yml`

Scheduled restarts are configured in the `restarttimes.yml`.

Restart schedules support specific times, days of the week, and timezones.

Example:

```yaml
timezone: "Europe/Berlin"

restart-times:
  - "00:00"
  - "08:00"
  - "16:00"
```


## Commands

### `/startserver`

Starts a Server.

```text
/startserver <server>
```


### `/stopserver`

Stops a Server.

```text
/stopserver <server>
```

### `/restartserver`

Restarts a Server.

```text
/restartserver <server> <time>
```

Examples:

```text
/restartserver survival 30s
/restartserver survival 5m
/restartserver survival 2h
/restartserver survival now
```


### `/restartallservers`

Restarts all Servers.

```text
/restartallservers <time>
```

Examples:

```text
/restartallservers 10m
/restartallservers 1h
/restartallservers now
```

## Permissions


| Permission                        | Description                            |
| --------------------------------- | -------------------------------------- |
| `servermanager.startserver`       | Allows the use of `/startserver`       |
| `servermanager.stopserver`        | Allows the use of `/stopserver`        |
| `servermanager.restartserver`     | Allows the use of `/restartserver`     |
| `servermanager.restartallservers` | Allows the use of `/restartallservers` |

## Pterodactyl API

ServerManager communicates with Pterodactyl using its Client API.

### Creating an API Key

1. Log into your Pterodactyl panel.
2. Open your account settings.
3. Navigate to the API credentials section.
4. Create a new API key.
5. Copy the generated key.
6. Add the key to the Config.

The API key should be treated as a secret and must never be committed to a public repository.


## Building from Source

ServerManager uses Gradle as its build system.

Build the plugin:

```bash
./gradlew build
```

On Windows:

```powershell
gradlew.bat build
```

The compiled JAR will be available in:

```text
build/libs/
```


## Security

Never expose your Pterodactyl API key.

Do not commit credentials to Git or include them in public configuration files.

If an API key is accidentally exposed, revoke it immediately through the Pterodactyl panel and generate a new one.

## Compatibility

| Component         | Requirement            |
| ----------------- | ---------------------- |
| Proxy             | Velocity               |
| Java              | 21+                    |
| Server Panel      | Pterodactyl            |

Compatibility may vary depending on the Velocity and Java versions used by your network.


## Issues

If you encounter a bug or have a feature request, open an issue in the GitHub repository.

When reporting a bug, include:

* ServerManager version
* Velocity version
* Java version
* Pterodactyl version
* Relevant configuration
* Console errors or stack traces
* Steps required to reproduce the issue

Remove any API keys, tokens, passwords, or other sensitive information before posting logs or configuration files.

