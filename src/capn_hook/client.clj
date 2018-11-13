(ns capn-hook.client
  "Namespace for handling all the RESTful calls to the other services with the
  connection pool being passed in to all calls."
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clj-http.conn-mgr :as conn]
            [clj-time.format :as f]
            [clojure.tools.logging :refer [error errorf info infof warnf debugf]]))

;; Definition of the connection manager for all calls in this namespace to
;; the Sulley Service. The point it to make a set of pooled connections that
;; will speed up the hits to the Sulley Service within the simplified context
;; of this namespace.
(defonce cm (conn/make-reusable-conn-manager {:timeout 120
                                              :threads 20
                                              :default-per-route 6}))

;; ## Simplified HTTP requests for the obvious calls

(defn do-get*
  "Function to use the connection manager to save time, but NOT retry
  if we fail on getting the data - we just don't have the time. If we get
  something, then we need to return it to the caller period."
  [url & [opts]]
  (if (and cm url)
    (http/get url (merge {:connection-manager cm
                          :socket-timeout 10000
                          :conn-timeout 2000}
                         opts))))

(defn do-get
  "Function to use the connection manager to save time, but NOT retry
  if we fail on getting the data - we just don't have the time. If we get
  something, then we need to return it as-is to the caller. This version
  catches the exceptions and logs them."
  [url & [opts]]
  (if (and cm url)
    (try
      (do-get* cm url opts)
      (catch Throwable t
        (infof t "Unable to hit '%s' with webhook!" url)))))

(defn do-post*
  "Function to use the connection manager to save time, but NOT retry
  if we fail on getting the data - we just don't have the time. If we get
  something, then we need to return it as-is to the caller."
  [url body & [opts]]
  (if (and cm url)
    (http/post url (merge {:body (if (not-empty body) (json/generate-string body))
                           :content-type (if body :json)
                           :accept :json
                           :connection-manager cm
                           :socket-timeout 10000
                           :conn-timeout 2000}
                          opts))))

(defn do-post
  "Function to use the connection manager to save time, but NOT retry
  if we fail on getting the data - we just don't have the time. If we get
  something, then we need to return it as-is. This version catches the
  exceptions and logs them."
  [url body & [opts]]
  (if (and cm url)
    (try
      (do-post* cm url body opts)
      (catch Throwable t
        (infof t "Unable to post '%s' with webhook!" url)))))

(defn try-times*
  "Executes body. If an exception is thrown, will retry. At most n retries
  are done. If still some exception is thrown a nil is returned - it's the
  best we can do for a reader that's having problems."
  [n func]
  (loop [n (max n 0)]
    (let [tag (Object.)
          result (try
                   [(func)]
                   (catch Exception e
                     (let [msg (.getMessage e)]
                       (cond
                         (pos? (.indexOf msg "status 400"))
                           (do
                             (warnf "Got: '%s' bad request - bailing out" msg)
                             tag)
                         (pos? (.indexOf msg "status 404"))
                           (do
                             (warnf "Got: '%s' not found!" msg)
                             tag)
                         :else
                           (do
                             (warnf e "Exception: %s" msg)
                             (when (zero? n) tag))))))]
      (cond (= result tag) nil
            (nil? result) (recur (dec n))
            :else (first result)))))

(defmacro try-times
  "Executes body. If an exception is thrown, will retry. At most n retries
  are done. If still some exception is thrown a nil is returned - it's the
  best we can do for a reader that's having problems."
  [n & body]
  `(try-times* (dec ~n) (fn [] ~@body)))
