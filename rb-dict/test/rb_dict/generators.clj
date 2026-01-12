(ns rb-dict.generators
  (:require [clojure.test.check.generators :as gen]
            [rb-dict.core :as dict]))

(def gen-int gen/int)

(def gen-pair
  (gen/tuple gen/int gen/int))

(def gen-pairs
  (gen/vector gen-pair 0 20))

;; Создаёт словарь и ключ, который гарантированно в нём есть
(def gen-dict-with-key
  (gen/bind gen-pairs
            (fn [pairs]
              (if (empty? pairs)
        ;; Если пар нет, возвращаем пустой словарь и nil
                (gen/return {:dict dict/empty-dict :key nil :has-key false})
        ;; Если пары есть, строим словарь и выбираем случайный существующий ключ
                (let [d (reduce (fn [d [k v]] (dict/insert d k v))
                                dict/empty-dict
                                pairs)
                      keys (map first pairs)]
                  (gen/fmap (fn [k] {:dict d :key k :has-key true})
                            (gen/elements keys)))))))