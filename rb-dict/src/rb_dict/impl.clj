(ns rb-dict.impl
  (:require [rb-dict.api :as api]))

;; Цвета
(def RED :red)
(def BLACK :black)

(defrecord RBNode [color key value left right])

(declare insert-node lookup-value remove-node map-node filter-node foldl-node foldr-node inorder)

;; внутренние утилиты

(defn- make-node [color k v l r]
  (->RBNode color k v l r))

;; Поиск
(defn- lookup-node [cmp node k]
  (when node
    (let [c (cmp k (:key node))]
      (cond
        (neg? c) (lookup-node cmp (:left node) k)
        (pos? c) (lookup-node cmp (:right node) k)
        :else node))))

(defn- lookup-value [cmp root k]
  (some-> (lookup-node cmp root k) :value))

;; Балансировка красно-чёрного дерева 

(defn- balance [color k v left right]
  (cond
    ;; LEFT-LEFT
    (and (= color BLACK)
         (= (:color left) RED)
         (= (:color (:left left)) RED))
    (let [l left
          ll (:left l)]
      (make-node RED (:key l) (:value l)
                 (make-node BLACK (:key ll) (:value ll)
                            (:left ll) (:right ll))
                 (make-node BLACK k v (:right l) right)))

    ;; LEFT-RIGHT
    (and (= color BLACK)
         (= (:color left) RED)
         (= (:color (:right left)) RED))
    (let [l left
          lr (:right l)]
      (make-node RED (:key lr) (:value lr)
                 (make-node BLACK (:key l) (:value l)
                            (:left l) (:left lr))
                 (make-node BLACK k v (:right lr) right)))

    ;; RIGHT-LEFT
    (and (= color BLACK)
         (= (:color right) RED)
         (= (:color (:left right)) RED))
    (let [r right
          rl (:left r)]
      (make-node RED (:key rl) (:value rl)
                 (make-node BLACK k v left (:left rl))
                 (make-node BLACK (:key r) (:value r)
                            (:right rl) (:right r))))

    ;; RIGHT-RIGHT
    (and (= color BLACK)
         (= (:color right) RED)
         (= (:color (:right right)) RED))
    (let [r right
          rr (:right r)]
      (make-node RED (:key r) (:value r)
                 (make-node BLACK k v left (:left r))
                 (make-node BLACK (:key rr) (:value rr)
                            (:left rr) (:right rr))))

    :else
    (make-node color k v left right)))

;; Вставка

(defn- insert-node [cmp root k v]
  (letfn [(ins [node]
            (if (nil? node)
              (make-node RED k v nil nil)
              (let [c (cmp k (:key node))]
                (cond
                  (neg? c) (balance (:color node)
                                    (:key node)
                                    (:value node)
                                    (ins (:left node))
                                    (:right node))

                  (pos? c) (balance (:color node)
                                    (:key node)
                                    (:value node)
                                    (:left node)
                                    (ins (:right node)))

                  :else ;; overwrite
                  (make-node (:color node) (:key node) v
                             (:left node) (:right node))))))]
    (let [new-root (ins root)]
      (assoc new-root :color BLACK))))

;; Удаление

(defn- inorder [node]
  (if (nil? node)
    []
    (concat (inorder (:left node))
            [[(:key node) (:value node)]]
            (inorder (:right node)))))

(defn- from-sorted [pairs]
  (letfn [(build [l r]
            (when (< l r)
              (let [m (quot (+ l r) 2)
                    [k v] (pairs m)]
                (make-node BLACK k v
                           (build l m)
                           (build (inc m) r)))))]
    (build 0 (count pairs))))

(defn- remove-node [cmp root k]
  (let [pairs (vec (inorder root))
        filtered (filterv (fn [[k' _]] (not (zero? (cmp k k'))))
                          pairs)]
    (from-sorted filtered)))

;; FOLD, MAP, FILTER

(defn- foldl-node [f acc node]
  (if (nil? node) acc
      (let [acc1 (foldl-node f acc (:left node))
            acc2 (f acc1 (:key node) (:value node))]
        (foldl-node f acc2 (:right node)))))

(defn- foldr-node [f acc node]
  (if (nil? node) acc
      (let [acc1 (foldr-node f acc (:right node))
            acc2 (f (:key node) (:value node) acc1)]
        (foldr-node f acc2 (:left node)))))

(defn- map-node [f node]
  (when node
    (make-node (:color node)
               (:key node)
               (f (:key node) (:value node))
               (map-node f (:left node))
               (map-node f (:right node)))))

(defn- filter-node [pred node]
  (let [pairs (filter (fn [[k v]] (pred k v))
                      (inorder node))]
    (from-sorted (vec pairs))))

;; Определение типа RBDict с реализацией интерфейсов

(deftype RBDict [root cmp]
  api/IDict
  (dict-empty [_] (RBDict. nil compare))

  (dict-insert [this k v]
    (RBDict. (insert-node cmp root k v) cmp))

  (dict-remove [this k]
    (RBDict. (remove-node cmp root k) cmp))

  (dict-lookup [this k]
    (lookup-value cmp root k))

  (dict-map [this f]
    (RBDict. (map-node f root) cmp))

  (dict-filter [this pred]
    (RBDict. (filter-node pred root) cmp))

  (dict-foldl [this f init]
    (foldl-node f init root))

  (dict-foldr [this f init]
    (foldr-node f init root))

  ;; Моноид
  (dict-mempty [this]
    (RBDict. nil compare))

  (dict-mappend [this other]
    (reduce (fn [d [k v]]
              (api/dict-insert d k v))
            this
            (inorder (.-root other))))

  ;; сравнение словарей
  (dict-equal? [this other]
    (= (inorder root)
       (inorder (.-root other))))

  ;; Реализация Java интерфейсов

  clojure.lang.Seqable
  (seq [this]
    (when root
      (seq (map first (inorder root)))))

  clojure.lang.Counted
  (count [this]
    (count (inorder root)))

  clojure.lang.ILookup
  (valAt [this k]
    (lookup-value cmp root k))
  (valAt [this k not-found]
    (or (lookup-value cmp root k) not-found))

  clojure.lang.Associative
  (containsKey [this k]
    (boolean (lookup-value cmp root k)))
  (entryAt [this k]
    (when-let [v (lookup-value cmp root k)]
      (clojure.lang.MapEntry. k v)))
  (assoc [this k v]
    (RBDict. (insert-node cmp root k v) cmp))

  clojure.lang.IPersistentCollection
  (cons [this o]
    (if (vector? o)
      (let [[k v] o]
        (RBDict. (insert-node cmp root k v) cmp))
      (throw (IllegalArgumentException. "cons expects a vector [k v]"))))
  (empty [_]
    (RBDict. nil compare))
  (equiv [this other]
    (and (instance? RBDict other)
         (= (inorder root) (inorder (.-root other)))))

  clojure.lang.IFn
  (invoke [this k]
    (lookup-value cmp root k)))

;; Создание пустого словаря
(defn create-empty-dict []
  (RBDict. nil compare))