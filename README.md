# Cap'n Hook - Callbacks Made Simple

<p align="center">
  <img src="doc/img/hook.jpg" width="400" height="357" border="0" />
</p>

The _web callback_ has become q very popular way for applications to notify
clients that some event happened without the client constantly polling for
any status updates. The problem with _adding_ the ability to send callbacks
to a service is that there is nothing technically challenging about doing
them, but getting them _right_ is annoying and takes a lot of time.

* Do you handle retries if the client isn't responding?
* Is there a simple asynchronous queue for sending these out?
* Is there a way to _classify_ the outgoing callbacks so that a client can
  register for one, and not the others?

There are a lot of little details that can make a big difference in the
overall utility of the callbacks you send. _Cap'n Hook_ is all about making
this as simple as possible.

## What Cap'n Hook Is - and Isn't

_Cap'n Hook_ is a simple, asynchronous, unidirectional, messaging system that
uses HTTP POST messages to deliver the messages in the body of the POST. There
is a _simple_ registration capability in the library, but for complex systems
like multi-machine services behind a load-balancer, you're going to need to
come up with a _shared registration system_, and _Cap'n Hook_ allows for you
to easily give it a function to pull the URLs for a given web hook, and it'll
use it just fine.

What _Cap'n Hook_ **isn't** is an app framework that handles the incoming
REST calls to register and de-register the clients for the different web hooks.
That's _one_ way to implement it, but another is to base the web hook URLs on
configuration - which might be stored in a user's config system, or it could
be in a database accessed with a SQL query.

However the URLs are stored, _Cap'n Hook_ handles the messaging to them with
a simple HTTP POST. More than that? Nope.

## Usage

Add the necessary dependency to your project:
```clojure
  [org.clojars.drbobbeaty/capn-hook "0.1.0"]
```

Using _Cap'n Hook_ is really a two-part process: you have to have some
_registration_ of the call-backs to make, and then you have to have a function
to send a call-back to those registered URLs. Since call-backs are only really
useful if they contain data, the firing of a call-back really needs to have
two things: the identifying name of the call-back to make, and the data to
send along with it:
```clojure
  (capn-hook.core/fire! :complete {:id 1234, :name "Jed", :status "OK"})
```

_Cap'n Hook_ then queues that up in a durable queue on the box, and sends it
out as soon as possible to all the registered receivers of the `:complete`
web hook.

In general, there can be different kinds of call-backs serviced. Imagine one
for a successful submission, and another for a successful completion. You
may not want to have both call-backs on the same endpoints in the client, so
you allow the clients to register them differently. Likewise, the payload you
wish to send to the different call-backs is different, so you may want to
have your system keep these separate.

### Simplified Registration

_Cap'n Hook_ has a built-in simplified registration system that handles all
use-cases where the service _sending_ the callbacks exists in a single process
space _and_ the list of registrations doesn't need to be durable. Yes, this
is a pretty small subset of useful cases, but it's also an example of how to
implement this registration no a more realistic scale.

Using this built-in registration system is as simple as calling the `register`
function:
```clojure
  (capn-hook.core/register! :complete "https://panda.dog.com/run")
```
where `:complete` is the _name_ of the web hook, and the URL is the _target_
of the HTTP POST where the body of the post will be the JSON data argument
from the `fire!` function, discussed later.

As long as the service is running, this registration will be persisted, and
registration is idempotent - registering the same URL _twice_ doesn't result
in two calls being made to that URL when `fire!` is invoked.

If there is ever a need to de-register a URL to stop sending the callbacks to,
simply call:
```clojure
  (capn-hook.core/deregister! "https://panda.dog.com/run")
```
The key here is that this will remove the URL from **all** registrations for
**all** web hooks. This is done so that you don't have to remember which
one(s) were registered, all are cleared out.

### Posting a Call-Back

When you want to post a callback to all registered URLs for a specific web
hook you can simply:
```clojure
  (capn-hook.core/fire! :submit payload)
```
where `payload` is a simple map of data that will be put in the body of the
POST that is sent to each URL. Again, this is using the internal registration
scheme because the web hook is is a _key_ - but it doesn't have to be. If
you want to send a callback to a known, specific set of URLs, you can pass
a sequence of strings (URLs) to the function:
```clojure
  (capn-hook.core/fire! ["http://foo.com/webhook", "http://bar.com/hitit"] payload)
```
and if you only have one, just pass in that one:
```clojure
  (capn-hook.core/fire! "http://foo.com/webhook" payload)
```

You can even pass in a funtion that _returns_ a sequence of strings:
```clojure
  (defn listeners
    "Function to return a sequence of URLs as strings that are listening
    to the service for the callback when a user signs up. Pull these from
    the database."
    []
    (db/query ["select distinct url from customers"] :row-fn :url))

  ;; now let's send the callback data to each of those registered URLs
  (capn-hook.core/fire! listeners {:user-id 421, :last "Thumb", :first "Tom"})
```

### Processing the Queued Callbacks

The process of _sending_ the callbacks is really a separate thread working on
a durable queue of the callbacks to send. Once a callback is enqueued, it
will be processed until it's successfully sent. If the process restarts, the
contents of the queue are _durable_ so it'll start up right where it left off.

The simplest way to get the process started is:
```clojure
  (capn-hook.core/start!)
```
and this will stay running until you tell it to stop:
```clojure
  (capn-hook.core/stop!)
```

As long as the thread is running, it will look at the contents of the outgoing
queue _every so often_, and if there are messages to send, it'll send them. If
not, it'll sleep for a bit, and look again. This is something that can be
started in your application's startup, or if you are running Jetty, you can
use the lower-level processing function yourself:
```clojure
  (:require [capn-hook.durable :refer [process!]]
            [overtone.at-at :as aa]
            [ring.adapter.jetty :as jt])

  ;; after processing the CLI args, fire up Jetty...
  (let [hip 1000
        pool (aa/mk-pool)
        ;; handle processing all the queued callbacks
        cron-callbacks (aa/interspaced hip (bound-fn [] (process!)) pool)]
    (try
      (jt/run-jetty app { :port (:port params) })
      (finally
        (if cron-callbacks (aa/stop cron-callbacks)))))
```
where we are using `at-at` to handle the scheduling of calling the `process!`
function from _Cap'n Hook_ to make a pass through the queue and attempt to send
every pending callback to the intended receiver.

## Implementation Details

The actual processing of the enququed callbacks takes place in the
`capn-hook.durable` namespace, in the `process!` function. We are using the
durable queue from [Factual](https://github.com/Factual/durable-queue). There
were a few issues in the latest release, and it needed to be forked and
patched to make a clean use of `/tmp` - and we've sub mitted a pull-request
back to them.

The function pulls off a message, and tries to send it to it's intended
recipient. If it's successful, then the message is removed from the queue.
If not, it stays, and we go to the next. Very simple.

The posting to the registered URLs is done in the same namespace in the `post`
function. Here, it's really just using `clj-http` to POST to the URL with the
provided body, and retrying three times - just in case. If it's successful,
then it's done, and returns `true`. If not, it's `false`.

## License

Copyright © 2018 The Man from S.P.U.D.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.