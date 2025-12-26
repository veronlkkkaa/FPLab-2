(ns rb-dict.generators
  (:require [clojure.test.check.generators :as gen]
            [rb-dict.core :as dict]))

(def gen-int gen/int)

(def gen-pair
  (gen/tuple gen/int gen/int))

(def gen-pairs
  (gen/vector gen-pair 0 20))

(def gen-dict
  (gen/fmap (fn [pairs]
              (reduce (fn [d [k v]] (dict/insert d k v))
                      dict/empty-dict
                      pairs))
            gen-pairs))