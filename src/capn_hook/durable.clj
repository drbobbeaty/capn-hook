(ns capn-hook.durable
  "This namespace is all about the durable queues for the events streaming in
  the service. This will include the enqueueing, processing, and even the
  Datadog monitoriing of the queue sizes."
  (:require [clojure.core.memoize :as memo]
            [clojure.tools.logging :refer [infof warnf errorf]]
            [durable-queue :refer [queues put! take! complete! retry! stats
                                   delete!]]
            [capn-hook.client :refer [try-times do-post*]]
            [capn-hook.logging :refer [log-execution-time!]]))

(defn post
  "Function to post the message out the url with the body of msg and if
  successful, return 'true'. If not, return 'false', and this will be put
  on the queue and retried."
  [{c :created url :url msg :msg :as arg}]
  (let [resp (try-times 3 (do-post* url msg))]
    (boolean resp)))

(defonce q (queues "/tmp" {}))

(def qn
  "This is the default queue name for all the callbacks that have to be sent
  to the registered callers. It's just here to simplify the code."
  :captain)

(defn enqueue!
  "Function to enqueue the message on the default queue for processing. It will
  return `true` if successful."
  [data]
  (if (map? data)
    (put! q qn data)))

(defn process!
  "Function to process messages on the named queue until there is a timeout
  and then it will return the count of messages processed up to that point in
  time. This can then be called over and over again, as needed."
  []
  (loop [cnt 0]
    (let [msg (take! q qn 1000 nil)]
      (if msg
        (try
          (if (or (empty? @msg) (post @msg))
            (complete! msg)
            (retry! msg))
          (catch Throwable t
            (infof t "Unable to process the message: " @msg))))
      ; if we had something, try again, but up the count
      (if msg
        (recur (inc cnt))
        cnt))))

(log-execution-time! process! {:level :debug, :msg-fn (fn [ret] (format "%s msgs" ret))})

(defn flush!
  "Function to remove all data stored in all queues we're currently managing.
  This will do so without any undo capability."
  []
  (delete! q))
