**[Dev Tools](#necessary-tools)** | **[Maven Access](#access-to-the-polaris-maven-private-repo)** | **[Deploy](#deployment)** | **[Messaging](#messaging)** | **[Storage](#storage)** | **[Finance](#finance)**

# Cap'n Hook - Callbacks Made Simple

<p align="center">
  <img src="doc/img/hook.jpg" width="400" height="357" border="0" />
</p>

The _web callback_ has become q very popular way for applications to notify
clients that some event happened without the client constantly polling for
any status updates. The problem with _adding_ callbacks to a service is
that there is nothing technically challenging about doing them, but getting
them _right_ is annoying and takes a lot of time.

* Do you handle retries if the client isn't responding?
* Do you allow the client to indicate the type of call? (GET, POST, etc.)
* Is there a simple asynchronous queue for sending these out?

There are a lot of little details that can make a big difference in the
overall utility of the callbacks you send.

_Cap'n Hook_ is all about making this as simple as possible.

## Usage

Using _Cap'n Hook_ is really a two-part process: you have to have some
_registration_ of the call-backs to make, and then you have to have a function
to fire off a call-back. Since call-backs are only really useful if they
contain data, the firing of a call-back really needs to have two things: the
identifying name of the call-back to make, and the data to send along with
it.

In order to register a new...

## Development

### Necessary Tools

**[Homebrew](http://brew.sh/)** - all of the following tools can be installed with Homebrew. If it's not already installed on you laptop, it's easy enough to go to the website, run the command to install it, and then continue to the next step. If you are on linux, expect to be installing some scripts and RPMs, but everything is available there as well - just not as convenient.

**[JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)** - it might be nice to get the JDK 1.6 from Apple, but it's _essential_ to get the JDK 1.8 from Oracle. This is a download and package install, but it's pretty simple to do and sets up it's own updater for future releases.

**[Leiningen](http://leiningen.org/)** - it is the _swiss army knife_ of clojure development - library version control, build, test, run, deploy tool all in one with the ability to create extensions. On the Mac, this is installed very easily with:
```bash
$ brew install leiningen
```
and on linux you can download the [lein script](https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein) to _somewhere_ in your `$PATH`, and make it executable. Then:
```bash
$ lein
```
and it'll download everything it needs to run properly.
