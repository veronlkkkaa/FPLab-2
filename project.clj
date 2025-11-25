(defproject rb-dict "0.1.0-SNAPSHOT"
  :description "Red-Black Tree Dictionary (immutable)"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/test.check "1.1.1"]]
  :plugins [[lein-cljfmt "0.8.2"]
            [lein-kibit "0.1.8"]
            [lein-bikeshed "0.5.2"]]

  :source-paths ["src"]
  :test-paths ["test"])
