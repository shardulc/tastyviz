# TASTyViz

TASTyViz is a web-based visualizer for [TASTy][1], the Scala 3
interchange format. It is currently minimally usable but very much
still a work in progress. Please report issues or request features on
[the GitHub project][2], or open a pull request!

### How to use

After cloning the repository:

 1. Edit the file `src/main/scala/tastyviz/controller/Config.scala` to
    specify the hostname at which you will serve TASTyViz
    (`http://localhost:8080` will be fine in most cases) and URLs for
    the JARs you want to browse. If, as in the example, your JARs are
    also at `localhost` URLs, place them under the `www/` directory.
 1. Run `sbt copyToWWW` from the project root directory. This will
    populate the `www/` directory with all the files needed to serve
    TASTyViz on a web server.
 1. Start your preferred web server in the `www/` directory, e.g. with
    `python -m http.server 8080`, and open the corresponding page in
    your browser.

Note that TASTyViz can take several seconds to start after opening the
page in the browser. Note also that it needs a working Internet
connection to load its JavaScript dependencies (JSZip, jQuery, and
jsTree).


[1]: https://docs.scala-lang.org/scala3/guides/tasty-overview.html
[2]: https://github.com/shardulc/tastyviz/issues
