(ns rb-dict.generators
  (:require [clojure.test.check.generators :as gen]))

(def gen-int gen/int)

(def gen-pair
  (gen/tuple gen/int gen/int))

(def gen-pairs
  (gen/vector gen-pair 0 20))