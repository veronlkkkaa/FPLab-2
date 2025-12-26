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

(deftest foldl-test
  (let [d (-> dict/empty-dict
              (dict/insert 1 10)
              (dict/insert 2 20)
              (dict/insert 3 30))]
    (is (= 60 (dict/foldl d (fn [acc _ v] (+ acc v)) 0)))
    (is (= '(3 2 1) (dict/foldl d (fn [acc k _] (conj acc k)) '())))))

(deftest foldr-test
  (let [d (-> dict/empty-dict
              (dict/insert 1 10)
              (dict/insert 2 20)
              (dict/insert 3 30))]
    (is (= 60 (dict/foldr d (fn [_ v acc] (+ v acc)) 0)))
    (is (= '(1 2 3) (dict/foldr d (fn [k _ acc] (conj acc k)) '())))))

(deftest standard-interfaces-test
  (testing "Проверка реализации стандартных коллекционных протоколов"
    (let [d (-> dict/empty-dict
                (dict/insert 3 "c")
                (dict/insert 1 "a")
                (dict/insert 2 "b"))]

      (testing "Seqable"
        (is (= (seq d) '(1 2 3))))

      (testing "Counted"
        (is (= 3 (count d)))
        (is (= 0 (count dict/empty-dict))))

      (testing "ILookup"
        (is (= "a" (get d 1)))
        (is (= "b" (get d 2)))
        (is (= "c" (get d 3)))
        (is (nil? (get d 10)))
        (is (= :missing (get d 10 :missing))))

      (testing "Associative"
        (let [d2 (assoc d 10 "x")]
          (is (= "x" (dict/lookup d2 10)))
          (is (= 4 (count d2))))
        (is (contains? d 1))
        (is (contains? d 2))
        (is (contains? d 3))
        (is (not (contains? d 99))))

      (testing "IPersistentCollection"
        (let [d2 (conj d [10 "x"])]
          (is (= "x" (dict/lookup d2 10)))
          (is (= 4 (count d2))))
        (let [e (empty d)]
          (is (= 0 (count e)))
          (is (nil? (seq e)))))

      (testing "IFn"
        (is (= "a" (d 1)))
        (is (= "b" (d 2)))
        (is (= "c" (d 3)))
        (is (nil? (d 100)))))))
