(ns rb-dict.mixed-keys-test
  (:require [clojure.test :refer :all]
            [rb-dict.cmp :as cmp]
            [rb-dict.core :as dict]
            [rb-dict.api :as api]))

(deftest mixed-keys-insert-and-lookup
  (testing "Вставка и поиск смешанных ключей (числа и строки)"
    (let [d  (dict/empty-dict-with cmp/mixed-cmp)
          d2 (-> d
                 (dict/insert 2 :n2)
                 (dict/insert "2" :s2)
                 (dict/insert 1 :n1)
                 (dict/insert "10" :s10))]
      (is (= :n1  (dict/lookup d2 1)))
      (is (= :n2  (dict/lookup d2 2)))
      (is (= :s2  (dict/lookup d2 "2")))
      (is (= :s10 (dict/lookup d2 "10"))))))

(deftest mixed-keys-with-keywords
  (testing "Смешанные ключи: числа, строки и keywords"
    (let [d  (dict/empty-dict-with cmp/mixed-cmp)
          d2 (-> d
                 (dict/insert 10 "число-10")
                 (dict/insert "apple" "строка-apple")
                 (dict/insert :name "keyword-name")
                 (dict/insert 5 "число-5")
                 (dict/insert "zoo" "строка-zoo")
                 (dict/insert :age "keyword-age"))]
      ;; Проверяем поиск по разным типам
      (is (= "число-10" (dict/lookup d2 10)))
      (is (= "число-5" (dict/lookup d2 5)))
      (is (= "строка-apple" (dict/lookup d2 "apple")))
      (is (= "строка-zoo" (dict/lookup d2 "zoo")))
      (is (= "keyword-name" (dict/lookup d2 :name)))
      (is (= "keyword-age" (dict/lookup d2 :age)))
      ;; Проверяем, что несуществующие ключи возвращают nil
      (is (nil? (dict/lookup d2 999)))
      (is (nil? (dict/lookup d2 "nonexistent")))
      (is (nil? (dict/lookup d2 :missing))))))

(deftest mixed-keys-remove
  (testing "Удаление элементов с разными типами ключей"
    (let [d  (dict/empty-dict-with cmp/mixed-cmp)
          d2 (-> d
                 (dict/insert 1 "один")
                 (dict/insert "1" "строка-один")
                 (dict/insert :one "keyword-один"))
          d3 (dict/remove-key d2 1)
          d4 (dict/remove-key d3 "1")
          d5 (dict/remove-key d4 :one)]
      ;; После удаления числа 1
      (is (nil? (dict/lookup d3 1)))
      (is (= "строка-один" (dict/lookup d3 "1")))
      (is (= "keyword-один" (dict/lookup d3 :one)))
      ;; После удаления строки "1"
      (is (nil? (dict/lookup d4 "1")))
      (is (= "keyword-один" (dict/lookup d4 :one)))
      ;; После удаления keyword :one
      (is (nil? (dict/lookup d5 :one))))))

(deftest mixed-keys-comparator-preserved
  (testing "Компаратор сохраняется при создании пустого словаря"
    (let [d1 (dict/empty-dict-with cmp/mixed-cmp)
          d2 (dict/insert d1 1 "один")
          d3 (api/dict-empty d2)] ;; Создаём пустой словарь из d2
      ;; d3 должен сохранить компаратор от d2
      ;; Проверяем, что можем добавлять смешанные ключи
      (let [d4 (-> d3
                   (dict/insert 2 "два")
                   (dict/insert "2" "строка-два"))]
        (is (= "два" (dict/lookup d4 2)))
        (is (= "строка-два" (dict/lookup d4 "2")))))))

(deftest mixed-keys-mempty-preserved
  (testing "Компаратор сохраняется при dict-mempty (моноид)"
    (let [d1 (dict/empty-dict-with cmp/mixed-cmp)
          d2 (dict/insert d1 1 "один")
          d3 (api/dict-mempty d2)] ;; Создаём mempty
      ;; d3 должен сохранить компаратор от d2
      (let [d4 (-> d3
                   (dict/insert 5 "пять")
                   (dict/insert "5" "строка-пять"))]
        (is (= "пять" (dict/lookup d4 5)))
        (is (= "строка-пять" (dict/lookup d4 "5")))))))

(deftest mixed-keys-order
  (testing "Порядок смешанных ключей: numbers < strings < keywords"
    (let [d  (dict/empty-dict-with cmp/mixed-cmp)
          d2 (-> d
                 (dict/insert :z "k-z")
                 (dict/insert "m" "s-m")
                 (dict/insert 10 "n-10")
                 (dict/insert :a "k-a")
                 (dict/insert "b" "s-b")
                 (dict/insert 5 "n-5"))
          ;; Получаем последовательность ключей (должна быть отсортирована)
          keys-seq (seq d2)]
      ;; Ожидаемый порядок: 5, 10 (numbers), "b", "m" (strings), :a, :z (keywords)
      (is (= [5 10 "b" "m" :a :z] keys-seq)))))

(deftest mixed-keys-map-operation
  (testing "Операция map сохраняет ключи и компаратор"
    (let [d  (dict/empty-dict-with cmp/mixed-cmp)
          d2 (-> d
                 (dict/insert 1 10)
                 (dict/insert "1" 20)
                 (dict/insert :one 30))
          d3 (dict/dict-map d2 (fn [k v] (* v 2)))]
      (is (= 20 (dict/lookup d3 1)))
      (is (= 40 (dict/lookup d3 "1")))
      (is (= 60 (dict/lookup d3 :one))))))

(deftest mixed-keys-filter-operation
  (testing "Операция filter работает с разными типами ключей"
    (let [d  (dict/empty-dict-with cmp/mixed-cmp)
          d2 (-> d
                 (dict/insert 1 10)
                 (dict/insert "1" 20)
                 (dict/insert :one 30)
                 (dict/insert 2 40))
          ;; Фильтруем только числовые ключи
          d3 (dict/dict-filter d2 (fn [k v] (number? k)))]
      (is (= 10 (dict/lookup d3 1)))
      (is (= 40 (dict/lookup d3 2)))
      (is (nil? (dict/lookup d3 "1")))
      (is (nil? (dict/lookup d3 :one))))))

(deftest mixed-keys-mappend-operation
  (testing "Операция mappend (моноид) работает со смешанными ключами"
    (let [d1 (dict/empty-dict-with cmp/mixed-cmp)
          d2 (dict/empty-dict-with cmp/mixed-cmp)
          d1-filled (-> d1
                        (dict/insert 1 "один")
                        (dict/insert "a" "строка-а"))
          d2-filled (-> d2
                        (dict/insert 2 "два")
                        (dict/insert "b" "строка-б"))
          d-merged (dict/mappend d1-filled d2-filled)]
      (is (= "один" (dict/lookup d-merged 1)))
      (is (= "два" (dict/lookup d-merged 2)))
      (is (= "строка-а" (dict/lookup d-merged "a")))
      (is (= "строка-б" (dict/lookup d-merged "b"))))))

