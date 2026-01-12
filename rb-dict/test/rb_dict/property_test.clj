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
                (let [d1 (reduce (fn [d [k v]] (dict/insert d k v)) dict/empty-dict p1)
                      d2 (reduce (fn [d [k v]] (dict/insert d k v)) dict/empty-dict p2)
                      d3 (reduce (fn [d [k v]] (dict/insert d k v)) dict/empty-dict p3)]
                  (dict/equal? (dict/mappend d1 (dict/mappend d2 d3))
                               (dict/mappend (dict/mappend d1 d2) d3)))))

;; Insert preserves membership
(defspec insert-contains 100
  (prop/for-all [pairs gen/gen-pairs
                 [k v] gen/gen-pair]
                (let [d (reduce (fn [d [k v]] (dict/insert d k v)) dict/empty-dict pairs)
                      d2 (dict/insert d k v)]
                  (= (dict/lookup d2 k) v))))

;; Remove removes
(defspec remove-removes 100
  (prop/for-all [pairs gen/gen-pairs
                 [k v] gen/gen-pair]
                (let [d (reduce (fn [d [k v]] (dict/insert d k v)) dict/empty-dict pairs)
                      d2 (dict/insert d k v)
                      d3 (dict/remove-key d2 k)]
                  (nil? (dict/lookup d3 k)))))

;; Map keeps keys but changes values
(defspec map-keeps-keys 100
  (prop/for-all [pairs gen/gen-pairs]
                (let [d (reduce (fn [d [k v]] (dict/insert d k v)) dict/empty-dict pairs)
                      d2 (dict/dict-map d (fn [_ v] (+ v 1)))]
                  (= (set (dict/dict->seq d))
                     (set (dict/dict->seq d2))))))

;; Filter removes keys
(defspec filter-reduces 100
  (prop/for-all [pairs gen/gen-pairs]
                (let [d (reduce (fn [d [k v]] (dict/insert d k v)) dict/empty-dict pairs)
                      d2 (dict/dict-filter d (fn [_ v] (even? v)))]
                  (<= (count d2) (count d)))))

;; === Тесты для стандартных протоколов ===

;; Seqable: seq возвращает ключи в отсортированном порядке
(defspec seqable-sorted 100
  (prop/for-all [pairs gen/gen-pairs]
                (let [d (reduce (fn [d [k v]] (dict/insert d k v))
                                dict/empty-dict
                                pairs)
                      s (seq d)]
                  (or (empty? s)
                      (= (sort s) (vec s))))))

;; Counted: count соответствует количеству уникальных ключей
(defspec counted-correct 100
  (prop/for-all [pairs gen/gen-pairs]
                (let [d (reduce (fn [d [k v]] (dict/insert d k v))
                                dict/empty-dict
                                pairs)
                      unique-keys (count (set (map first pairs)))]
                  (= (count d) unique-keys))))

;; ILookup: get работает так же как dict/lookup
(defspec ilookup-consistent 100
  (prop/for-all [pairs gen/gen-pairs
                 k gen/gen-int]
                (let [d (reduce (fn [d [k v]] (dict/insert d k v))
                                dict/empty-dict
                                pairs)]
                  (= (get d k) (dict/lookup d k)))))

;; Associative: assoc работает как dict/insert
(defspec associative-insert 100
  (prop/for-all [pairs gen/gen-pairs
                 k gen/gen-int
                 v gen/gen-int]
                (let [d (reduce (fn [d [k v]] (dict/insert d k v))
                                dict/empty-dict
                                pairs)
                      d1 (assoc d k v)
                      d2 (dict/insert d k v)]
                  (dict/equal? d1 d2))))

;; IPersistentCollection: conj работает как dict/insert
(defspec persistent-collection-conj 100
  (prop/for-all [pairs gen/gen-pairs
                 k gen/gen-int
                 v gen/gen-int]
                (let [d (reduce (fn [d [k v]] (dict/insert d k v))
                                dict/empty-dict
                                pairs)
                      d1 (conj d [k v])
                      d2 (dict/insert d k v)]
                  (dict/equal? d1 d2))))

;; IFn: вызов словаря как функции работает как lookup
(defspec ifn-lookup 100
  (prop/for-all [pairs gen/gen-pairs
                 k gen/gen-int]
                (let [d (reduce (fn [d [k v]] (dict/insert d k v))
                                dict/empty-dict
                                pairs)]
                  (= (d k) (dict/lookup d k)))))

;; Foldl property: сумма всех значений
(defspec foldl-sum 100
  (prop/for-all [pairs gen/gen-pairs]
                (let [d (reduce (fn [d [k v]] (dict/insert d k v))
                                dict/empty-dict
                                pairs)
                      unique-pairs (into {} pairs)
                      expected-sum (reduce + (vals unique-pairs))
                      actual-sum (dict/foldl d (fn [acc _ v] (+ acc v)) 0)]
                  (= expected-sum actual-sum))))

;; Foldr property: сумма всех значений (должна быть такой же как у foldl)
(defspec foldr-sum 100
  (prop/for-all [pairs gen/gen-pairs]
                (let [d (reduce (fn [d [k v]] (dict/insert d k v))
                                dict/empty-dict
                                pairs)
                      foldl-sum (dict/foldl d (fn [acc _ v] (+ acc v)) 0)
                      foldr-sum (dict/foldr d (fn [_ v acc] (+ v acc)) 0)]
                  (= foldl-sum foldr-sum))))

;; Remove реально удаляет существующий ключ
(defspec remove-existing-key 100
  (prop/for-all [{:keys [dict key has-key]} gen/gen-dict-with-key]
    (if (not has-key)
      true
      (nil? (dict/lookup (dict/remove-key dict key) key)))))

;; Update существующего ключа перезаписывает значение
(defspec update-existing-key 100
  (prop/for-all [{:keys [dict key has-key]} gen/gen-dict-with-key
                 v gen/gen-int]
    (if (not has-key)
      true
      (= (dict/lookup (dict/insert dict key v) key) v))))

;; Вызов словаря как функции работает на существующем ключе
(defspec ifn-lookup-existing 100
  (prop/for-all [{:keys [dict key has-key]} gen/gen-dict-with-key]
    (if (not has-key)
      true
      (= (dict key) (dict/lookup dict key)))))