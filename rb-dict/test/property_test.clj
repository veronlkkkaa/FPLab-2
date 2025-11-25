(ns rb-dict.property-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [rb-dict.generators :as gen]
            [rb-dict.core :as dict]))

;; Моноид: e ⋆ a = a, a ⋆ e = a
(defspec monoid-identity 100
  (prop/for-all [pairs gen/gen-pairs]
                (let [d (reduce (fn [d [k v]] (dict/insert d k v))
                                dict/empty-dict
                                pairs)]
                  (and (dict/equal? (dict/mappend d dict/empty-dict) d)
                       (dict/equal? (dict/mappend dict/empty-dict d) d)))))

;; Ассоциативность
(defspec monoid-assoc 100
  (prop/for-all [p1 gen/gen-pairs
                 p2 gen/gen-pairs
                 p3 gen/gen-pairs]
                (let [d1 (reduce dict/insert dict/empty-dict p1)
                      d2 (reduce dict/insert dict/empty-dict p2)
                      d3 (reduce dict/insert dict/empty-dict p3)]
                  (dict/equal? (dict/mappend d1 (dict/mappend d2 d3))
                               (dict/mappend (dict/mappend d1 d2) d3)))))

;; Insert preserves membership
(defspec insert-contains 100
  (prop/for-all [pairs gen/gen-pairs
                 [k v] gen/gen-pair]
                (let [d (reduce dict/insert dict/empty-dict pairs)
                      d2 (dict/insert d k v)]
                  (= (dict/lookup d2 k) v))))

;; Remove removes
(defspec remove-removes 100
  (prop/for-all [pairs gen/gen-pairs
                 [k v] gen/gen-pair]
                (let [d (reduce dict/insert dict/empty-dict pairs)
                      d2 (dict/insert d k v)
                      d3 (dict/remove-key d2 k)]
                  (nil? (dict/lookup d3 k)))))

;; Map keeps keys but changes values
(defspec map-keeps-keys 100
  (prop/for-all [pairs gen/gen-pairs]
                (let [d (reduce dict/insert dict/empty-dict pairs)
                      d2 (dict/dict-map d (fn [k v] (+ v 1)))]
                  (= (set (dict/dict->seq d))
                     (set (dict/dict->seq d2)))))

  ;; Filter removes keys
  (defspec filter-reduces 100
    (prop/for-all [pairs gen/gen-pairs]
                  (let [d (reduce dict/insert dict/empty-dict pairs)
                        d2 (dict/dict-filter d (fn [_ v] (even? v)))]
                    (<= (count d2) (count d))))