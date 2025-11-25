(ns rb-dict.unit-test
  (:require [clojure.test :refer :all]
            [rb-dict.core :as dict]))

(deftest basic-insert-lookup
  (let [d (-> dict/empty-dict
              (dict/insert 1 "a")
              (dict/insert 2 "b"))]
    (is (= "a" (dict/lookup d 1)))
    (is (= "b" (dict/lookup d 2)))
    (is (nil? (dict/lookup d 3)))))

(deftest remove-test
  (let [d (-> dict/empty-dict
              (dict/insert 1 "a")
              (dict/insert 2 "b"))
        d2 (dict/remove-key d 1)]
    (is (nil? (dict/lookup d2 1)))
    (is (= "b" (dict/lookup d2 2)))))

(deftest map-test
  (let [d (-> dict/empty-dict
              (dict/insert 1 10)
              (dict/insert 2 20))
        d2 (dict/dict-map d (fn [_ v] (+ v 5)))]
    (is (= 15 (dict/lookup d2 1)))
    (is (= 25 (dict/lookup d2 2)))))

(deftest filter-test
  (let [d (-> dict/empty-dict
              (dict/insert 1 10)
              (dict/insert 2 21)
              (dict/insert 3 30))
        d2 (dict/dict-filter d (fn [_ v] (even? v)))]
    (is (= #{1 3} (set (dict/dict->seq d2))))))

(deftest monoid-test
  (let [d1 (-> dict/empty-dict
               (dict/insert 1 "a"))
        d2 (-> dict/empty-dict
               (dict/insert 2 "b"))
        m (dict/mappend d1 d2)]
    (is (= "a" (dict/lookup m 1)))
    (is (= "b" (dict/lookup m 2)))
    (is (dict/equal? d1 (dict/mappend d1 dict/empty-dict)))))
