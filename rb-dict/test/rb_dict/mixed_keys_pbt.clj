(ns rb-dict.mixed-keys-pbt
  (:require [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [rb-dict.cmp :as cmp]
            [rb-dict.core :as dict]
            [rb-dict.api :as api]))

;; Генератор смешанных ключей (числа или строки)
(def gen-mixed-key
  (gen/one-of [gen/int 
               (gen/fmap str gen/int)]))

;; Генератор пары ключ-значение
(def gen-kv
  (gen/tuple gen-mixed-key gen/int))

;; Генератор смешанных ключей с keywords
(def gen-mixed-key-full
  (gen/one-of [gen/int
               gen/string-alphanumeric
               (gen/fmap keyword gen/string-alphanumeric)]))

(def gen-kv-full
  (gen/tuple gen-mixed-key-full gen/int))

;; Property 1: Вставили элемент → можем его найти
(defspec insert-then-lookup-works-on-mixed-keys 200
  (prop/for-all [pairs (gen/vector gen-kv)]
    (let [d0 (dict/empty-dict-with cmp/mixed-cmp)
          d  (reduce (fn [acc [k v]] (dict/insert acc k v)) d0 pairs)]
      (every?
        (fn [[k v]] (= v (dict/lookup d k)))
        pairs))))

;; Property 2: Вставили и удалили → элемента нет
(defspec insert-remove-leaves-nothing 100
  (prop/for-all [k gen-mixed-key
                 v gen/int]
    (let [d0 (dict/empty-dict-with cmp/mixed-cmp)
          d1 (dict/insert d0 k v)
          d2 (dict/remove-key d1 k)]
      (nil? (dict/lookup d2 k)))))

;; Property 3: Моноид identity - e ⋆ a = a, a ⋆ e = a
(defspec mixed-keys-monoid-identity 100
  (prop/for-all [pairs (gen/vector gen-kv)]
    (let [d0 (dict/empty-dict-with cmp/mixed-cmp)
          d  (reduce (fn [acc [k v]] (dict/insert acc k v)) d0 pairs)
          e  (api/dict-mempty d)]
      (and (dict/equal? (dict/mappend d e) d)
           (dict/equal? (dict/mappend e d) d)))))

;; Property 4: Моноид ассоциативность - (a ⋆ b) ⋆ c = a ⋆ (b ⋆ c)
(defspec mixed-keys-monoid-assoc 100
  (prop/for-all [p1 (gen/vector gen-kv 0 10)
                 p2 (gen/vector gen-kv 0 10)
                 p3 (gen/vector gen-kv 0 10)]
    (let [d0 (dict/empty-dict-with cmp/mixed-cmp)
          d1 (reduce (fn [acc [k v]] (dict/insert acc k v)) d0 p1)
          d2 (reduce (fn [acc [k v]] (dict/insert acc k v)) d0 p2)
          d3 (reduce (fn [acc [k v]] (dict/insert acc k v)) d0 p3)]
      (dict/equal? (dict/mappend d1 (dict/mappend d2 d3))
                   (dict/mappend (dict/mappend d1 d2) d3)))))

;; Property 5: Map сохраняет ключи
(defspec mixed-keys-map-preserves-keys 100
  (prop/for-all [pairs (gen/vector gen-kv 1 20)]
    (let [d0 (dict/empty-dict-with cmp/mixed-cmp)
          d  (reduce (fn [acc [k v]] (dict/insert acc k v)) d0 pairs)
          d2 (dict/dict-map d (fn [k v] (+ v 1)))]
      (= (set (seq d))
         (set (seq d2))))))

;; Property 6: Filter уменьшает или сохраняет размер
(defspec mixed-keys-filter-reduces-size 100
  (prop/for-all [pairs (gen/vector gen-kv 0 20)]
    (let [d0 (dict/empty-dict-with cmp/mixed-cmp)
          d  (reduce (fn [acc [k v]] (dict/insert acc k v)) d0 pairs)
          d2 (dict/dict-filter d (fn [k v] (even? v)))]
      (<= (count d2) (count d)))))

;; Property 7: Вставка элемента с тем же ключом перезаписывает значение
(defspec mixed-keys-insert-overwrites 100
  (prop/for-all [k gen-mixed-key
                 v1 gen/int
                 v2 gen/int]
    (let [d0 (dict/empty-dict-with cmp/mixed-cmp)
          d1 (dict/insert d0 k v1)
          d2 (dict/insert d1 k v2)]
      (= v2 (dict/lookup d2 k)))))

;; Property 8: Компаратор сохраняется через операции
(defspec mixed-keys-comparator-preserved-through-ops 100
  (prop/for-all [pairs (gen/vector gen-kv 1 20)]
    (let [d0 (dict/empty-dict-with cmp/mixed-cmp)
          d1 (reduce (fn [acc [k v]] (dict/insert acc k v)) d0 pairs)
          d2 (api/dict-empty d1)
          ;; Теперь пробуем добавить смешанные ключи в d2
          d3 (-> d2
                 (dict/insert 1 "один")
                 (dict/insert "1" "строка-один"))]
      ;; Если компаратор сохранился, мы сможем найти оба элемента
      (and (= "один" (dict/lookup d3 1))
           (= "строка-один" (dict/lookup d3 "1"))))))

;; Property 9: Порядок ключей соответствует компаратору
(defspec mixed-keys-order-matches-comparator 100
  (prop/for-all [pairs (gen/vector gen-kv-full 1 15)]
    (let [d0 (dict/empty-dict-with cmp/mixed-cmp)
          d  (reduce (fn [acc [k v]] (dict/insert acc k v)) d0 pairs)
          keys-from-dict (seq d)
          keys-sorted (sort cmp/mixed-cmp (distinct (map first pairs)))]
      (= keys-from-dict keys-sorted))))

;; Property 10: Foldl обходит все элементы
(defspec mixed-keys-foldl-visits-all 100
  (prop/for-all [pairs (gen/vector gen-kv 0 20)]
    (let [d0 (dict/empty-dict-with cmp/mixed-cmp)
          d  (reduce (fn [acc [k v]] (dict/insert acc k v)) d0 pairs)
          ;; Собираем все ключи через foldl
          keys-via-foldl (dict/foldl d (fn [acc k v] (conj acc k)) [])
          keys-expected (seq d)]
      (= (set keys-via-foldl) (set keys-expected)))))

;; Property 11: Mappend объединяет элементы из обоих словарей
(defspec mixed-keys-mappend-combines 100
  (prop/for-all [p1 (gen/vector gen-kv 1 10)
                 p2 (gen/vector gen-kv 1 10)]
    (let [d0 (dict/empty-dict-with cmp/mixed-cmp)
          d1 (reduce (fn [acc [k v]] (dict/insert acc k v)) d0 p1)
          d2 (reduce (fn [acc [k v]] (dict/insert acc k v)) d0 p2)
          d-merged (dict/mappend d1 d2)
          all-keys (distinct (concat (map first p1) (map first p2)))]
      ;; Все ключи из обоих словарей должны быть в результате
      (every? (fn [k] (some? (dict/lookup d-merged k))) all-keys))))

;; Property 12: Empty словарь имеет размер 0
(defspec mixed-keys-empty-has-zero-size 100
  (prop/for-all [pairs (gen/vector gen-kv 1 10)]
    (let [d0 (dict/empty-dict-with cmp/mixed-cmp)
          d1 (reduce (fn [acc [k v]] (dict/insert acc k v)) d0 pairs)
          d-empty (api/dict-empty d1)]
      (zero? (count d-empty)))))

;; Запуск всех тестов
(comment
  ;; Запустить все property-based тесты
  (clojure.test/run-tests 'rb-dict.mixed-keys-pbt))

