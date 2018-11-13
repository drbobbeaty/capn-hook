(ns capn-hook.core
  "This namespace is the main user-facing API for the library that will send
  all the webhook messages to the registered listeners. If one is never
  listening, we'll keep trying to send to it for a very long time."
  (:require [capn-hook.durable :refer [enqueue! process!]]
            [clj-time.core :refer [now]]
            [clojure.tools.logging :refer [infof warnf errorf]]
            [capn-hook.logging :refer [log-execution-time!]]
            [overtone.at-at :as aa]))

(defonce registrations (atom {}))

(defn register
  "Function to add the url to the set of all targets for the supplied webhook.
  This will be a set of urls, so there is no chance of duplication within the
  registration."
  [wh url]
  (if (and (keyword? wh) (string? url))
    (if (wh @registrations)
      (swap! registrations update wh conj url)
      (swap! registrations assoc wh #{url}))))

(defn deregister
  "Function to remove a url from *all* webhook registrations. This is a simple
  way to remove a url without having to worry where it might have been
  registered."
  [url]
  (if (string? url)
    (doseq [k (keys @registrations)]
      (swap! registrations update k disj url))))

(defn targets
  "Function to return a sequence of targets for a supplied 'webhook' name. The
  system is capable of having any number of 'nameed' webhooks - imagine 'create',
  'read', 'update', 'delete' for a standard editable service. The return value
  is a sequence of urls to POST to:

    [\"http://foo.com/hit/me\", \"http://bar.com/punch\"]

  ."
  [hook]
  (or (if (keyword? hook) (hook @registrations)) []))

(defn fire!
  "Function to take a representation of a sequence of urls, and a message
  to send to all registered targets of that particular webhook. The first
  arg can be a function that will be expected to return a sequence of urls,
  or it can be a sequence of urls, or a single url, or a keyword, in which
  case, we'll look up the registration from the internal list. This will
  actually just enqueue the complete message onto the durable queue of the
  correct name, and then let the processing of that queue handle the rest."
  [wh msg]
  (let [tgts (cond
               (keyword? wh) (targets wh)
               (fn? wh)      (wh)
               (coll? wh)    wh
               (string? wh)  [wh]
               :else         [])
        base {:created (now)
              :msg     msg}]
    (doseq [u tgts
            :when (string? u)]
      (enqueue! (assoc base :url u)))))

(log-execution-time! fire! {:level :debug})

; (defn flush!
;   "Function to flush the queue of *all* pending callbacks so that anything
;   that was in the queue is now lost. This is a permanent operation!"
;   []
;   )

; (log-execution-time! flush! {:level :debug})

;;
;; Let's now make a way to start a worker process that will send out the
;; callbacks to the registered listeners until the user want to shut it all
;; down.
;;

(defonce pool
  (delay
    (aa/mk-pool)))

(defonce worker (atom nil))

(def chill
  "This is the interval time between restarts on the sending of the callbacks
  to the registered listeners - in msec."
  1000)

(defn start!
  "Function to start a worker thread for sending the callbacks to the
  registered lsteners, and to continue doing so until the process is
  stopped or the `stop!` function is called."
  []
  (if @worker
    (errorf "There is already a callback worker sending messages!")
    (if-let [wt (aa/interspaced chill (bound-fn [] (process!)) @pool)]
      (reset! worker wt))))

(defn stop!
  "Function to stop sending the callbacks to the recipients. This just
  shuts down the sending thread and that's it."
  []
  (when @worker
    (aa/stop @worker)
    (reset! worker nil)))
