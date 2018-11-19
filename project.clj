(defproject org.clojars.drbobbeaty/capn-hook "0.1.0"
  :description "Simple library for reliable webhook generation for apps"
  :url "http://github.com/drbobbeaty/capn-hook"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src"]
  :min-lein-version "2.3.4"
  :dependencies [;; nice utilities (time, memo, etc.)
                 [clj-time "0.12.0"]
                 [org.clojure/core.memoize "0.5.6"]
                 ;; logging with log4j
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [log4j/log4j "1.2.17"]
                 [org.clojure/tools.logging "0.3.1"]
                 [robert/hooke "1.3.0"]
                 ;; JSON parsing library
                 [cheshire "5.5.0"]
                 ;; nice HTTP client library
                 [clj-http "3.7.0"]
                 ;; durable disk-based queues
                 [org.clojars.drbobbeaty/durable-queue "0.1.8" :exclusions [riddley]]
                 ;; simple task scheduling for the retries
                 [overtone/at-at "1.2.0"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]]}})
