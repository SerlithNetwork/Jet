
# Jet

A backend service for [Flare](https://github.com/TECHNOVE/Flare) and [flare-viewer](https://github.com/SerlithNetwork/flare-viewer).

### Compiling for production

Compile the project by running
````shell
  gradlew build
````

Then run it using
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
