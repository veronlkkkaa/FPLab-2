(ns rb-dict.generators
  (:require [clojure.test.check.generators :as gen]
            [rb-dict.core :as dict]))

(def gen-int gen/int)

(def gen-pair
  (gen/tuple gen/int gen/int))

;; Генерирует случайный словарь напрямую
(def gen-dict
  (gen/fmap
   (fn [pairs]
     (reduce (fn [d [k v]] (dict/insert d k v))
             dict/empty-dict
             pairs))
   (gen/vector gen-pair 0 20)))

;; Создаёт словарь и ключ, который гарантированно в нём есть
(def gen-dict-with-key
  (gen/bind (gen/vector gen-pair 1 20)
            (fn [pairs]
              (let [d (reduce (fn [d [k v]] (dict/insert d k v))
                              dict/empty-dict
                              pairs)
                    keys (map first pairs)]
                (gen/fmap (fn [k] {:dict d :key k :has-key true})
                          (gen/elements keys))))))