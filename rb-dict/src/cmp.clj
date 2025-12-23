(ns rb-dict.cmp)

(defn mixed-cmp
  "Тотальный порядок для ключей типа number/string/keyword:
   0: numbers < 1: strings < 2: keywords < 3: other
   Внутри класса — обычный compare."
  [a b]
  (let [ta (cond (number? a) 0 (string? a) 1 (keyword? a) 2 :else 3)
        tb (cond (number? b) 0 (string? b) 1 (keyword? b) 2 :else 3)]
    (if (not= ta tb)
      (compare ta tb)
      (compare a b))))

