
# Jet

A backend service for [Flare](https://github.com/TECHNOVE/Flare) and [flare-viewer](https://github.com/SerlithNetwork/flare-viewer).

### Compiling for production

First, you need a spare database. This database will be cleared up, and used by
Flyway to create the structure in which JOOQ will base of its auto-generated code. \
As a recommendation, use a service database in GitHub actions to create a disposable database.

Add your credentials to `gradle.properties`:
```properties
# Database credentials for code generation
jet.jooq.database.url = jdbc:postgresql://localhost:5432/flare?ssl=require
jet.jooq.database.user = pgactions
jet.jooq.database.password = pgactions
```

Then, compile the project by running the following commands
````shell
  gradlew flywayClean
  gradlew flywayMigrate
  gradlew jooqCodegen
  gradlew generateProto
  gradlew build
````

Finally, find the jar in `/build/libs/` and run it using:
````shell
  java -jar Jet.jar
````

### Supported Frontends
Jet was designed to work on pair with [flare-viewer](https://github.com/SerlithNetwork/flare-viewer). \
Since the Flare protocol expects a single endpoint for both submitting data and using the web-ui, it is
highly recommended to use a reverse proxy like Nginx or Caddy to forward the requests accordingly.

### Client
Jet is compatible with any platform that provides [Flare](https://github.com/TECHNOVE/Flare)
1. As a standalone plugin \
   1.1. [FlarePlugin](https://github.com/TECHNOVE/FlarePlugin) \
   1.2. [FlarePlatform](https://github.com/SerlithNetwork/FlarePlatform) (recommended)
2. Provided by a server software \
   2.1. [Pufferfish](https://github.com/pufferfish-gg/Pufferfish) and forks \
   2.2. [Puffernot](https://github.com/SerlithNetwork/Puffernot)/[Pufferfork](https://github.com/Toffikk/Pufferfork) and forks (recommended)

### License

Jet is free and open source software, released under the AGPL license. \
Refer to [LICENSE](LICENSE) for details.
