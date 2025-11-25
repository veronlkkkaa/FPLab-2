(ns rb-dict.core
  (:require [rb-dict.api :as api]
            [rb-dict.impl :as impl]))

(def empty-dict
  "Пустой словарь"
  (impl/create-empty-dict))

(defn insert [d k v] (api/dict-insert d k v))
(defn remove-key [d k] (api/dict-remove d k))
(defn lookup [d k] (api/dict-lookup d k))
(defn dict-map [d f] (api/dict-map d f))
(defn dict-filter [d pred] (api/dict-filter d pred))
(defn foldl [d f init] (api/dict-foldl d f init))
(defn foldr [d f init] (api/dict-foldr d f init))
(defn mappend [d1 d2] (api/dict-mappend d1 d2))
(defn equal? [d1 d2] (api/dict-equal? d1 d2))
(defn dict->seq [d] (seq d))
