(ns rb-dict.mixed-data-demo
  (:require [rb-dict.core :as dict]
            [rb-dict.impl :as impl]
            [clojure.test.check.generators :as gen]))

;; ДЕМОНСТРАЦИЯ: СМЕШАННЫЕ ДАННЫЕ

(comment
  ;; 1. ПРОБЛЕМА: По умолчанию используется compare, который не работает со смешанными типами
  (def default-dict dict/empty-dict)
  
  (println "Попытка добавить число:")
  (def d1 (dict/insert default-dict 5 "пять"))
  (println (dict/lookup d1 5)) ;; => "пять"
  
  (println "\nПопытка добавить строку:")
  (def d2 (dict/insert d1 "hello" "мир"))
  ;; ОШИБКА! ClassCastException: java.lang.String cannot be cast to java.lang.Number
  ;; Функция compare не может сравнить число 5 и строку "hello"
  
  
  ;; РЕШЕНИЕ 1: Свой компаратор для смешанных типов
  
  (defn mixed-compare [a b]
    "Компаратор, который может сравнивать разные типы"
    (let [type-a (class a)
          type-b (class b)]
      (if (= type-a type-b)
        ;; Одинаковые типы - обычное сравнение
        (compare a b)
        ;; Разные типы - сравниваем имена классов
        (compare (.getName type-a) (.getName type-b)))))
  
  ;; Создаем словарь с кастомным компаратором
  (def mixed-dict (impl/->RBDict nil mixed-compare))
  
  (println "\nСМЕШАННЫЙ СЛОВАРЬ")
  (def md1 (dict/insert mixed-dict 5 "пять"))
  (def md2 (dict/insert md1 "hello" "привет"))
  (def md3 (dict/insert md2 3 "три"))
  (def md4 (dict/insert md3 "world" "мир"))
  (def md5 (dict/insert md4 :keyword "ключевое слово"))
  (def md6 (dict/insert md5 'symbol "символ"))
  
  (println "Поиск числа 5:" (dict/lookup md6 5))          ;; => "пять"
  (println "Поиск строки 'hello':" (dict/lookup md6 "hello"))  ;; => "привет"
  (println "Поиск keyword :keyword:" (dict/lookup md6 :keyword)) ;; => "ключевое слово"
  (println "Поиск symbol 'symbol:" (dict/lookup md6 'symbol))   ;; => "символ"
  
  (println "\nВсе ключи в порядке:")
  (println (dict/dict->seq md6))
  ;; Ключи отсортированы сначала по типу, потом внутри типа
  
  
  ;; РЕШЕНИЕ 2: Преобразовать все к строкам
  
  (defn string-compare [a b]
    (compare (str a) (str b)))
  
  (def string-dict (impl/->RBDict nil string-compare))
  
  (println "\nСТРОКОВЫЙ СЛОВАРЬ")
  (def sd1 (dict/insert string-dict 5 "пять"))
  (def sd2 (dict/insert sd1 "hello" "привет"))
  (def sd3 (dict/insert sd2 :keyword "ключ"))
  
  (println "Поиск 5:" (dict/lookup sd3 5))
  (println "Поиск 'hello':" (dict/lookup sd3 "hello"))
  (println "Поиск :keyword:" (dict/lookup sd3 :keyword))
  
  (println "\nВсе ключи:" (dict/dict->seq sd3))
  ;; Все отсортировано как строки: "5" < ":keyword" < "hello"
  
  
  ;; ГЕНЕРАТОР для смешанных данных
  
  (def gen-mixed-key
    "Генератор, который создает числа, строки или keywords"
    (gen/one-of [gen/int
                 gen/string-alphanumeric
                 (gen/fmap keyword gen/string-alphanumeric)]))
  
  (def gen-mixed-pair
    (gen/tuple gen-mixed-key gen/int))
  
  (def gen-mixed-pairs
    (gen/vector gen-mixed-pair 0 10))
  
  (def gen-mixed-dict
    (gen/fmap (fn [pairs]
                (reduce (fn [d [k v]] 
                          (try
                            (dict/insert d k v)
                            (catch Exception e
                              ;; Если compare не может сравнить, пропускаем
                              d)))
                        mixed-dict
                        pairs))
              gen-mixed-pairs))
  
  ;; Пример генерации
  (println "\nГЕНЕРАЦИЯ СЛУЧАЙНЫХ СМЕШАННЫХ ДАННЫХ")
  (require '[clojure.test.check :as tc])
  (println "Примеры пар:")
  (dotimes [_ 5]
    (println (gen/generate gen-mixed-pair)))
  
  )

;; ДЕМОНСТРАЦИЯ РАБОТЫ

(defn demo []
  (println "ДЕМОНСТРАЦИЯ СМЕШАННЫХ ДАННЫХ В RBDict\n")
  
  ;; Компаратор для смешанных типов
  (defn mixed-compare [a b]
    (let [type-a (class a)
          type-b (class b)]
      (if (= type-a type-b)
        (compare a b)
        (compare (.getName type-a) (.getName type-b)))))
  
  (def mixed-dict (impl/->RBDict nil mixed-compare))
  
  ;; Добавляем разные типы
  (println "📝 Добавляем элементы разных типов:\n")
  
  (def d1 (dict/insert mixed-dict 10 "десять"))
  (println "  ✓ Число 10 -> 'десять'")
  
  (def d2 (dict/insert d1 "apple" "яблоко"))
  (println "  ✓ Строка 'apple' -> 'яблоко'")
  
  (def d3 (dict/insert d2 :name "имя"))
  (println "  ✓ Keyword :name -> 'имя'")
  
  (def d4 (dict/insert d3 5 "пять"))
  (println "  ✓ Число 5 -> 'пять'")
  
  (def d5 (dict/insert d4 "zoo" "зоопарк"))
  (println "  ✓ Строка 'zoo' -> 'зоопарк'")
  
  (def d6 (dict/insert d5 :age "возраст"))
  (println "  ✓ Keyword :age -> 'возраст'\n")
  
  ;; Поиск
  (println "🔍 Поиск элементов:\n")
  (println "  Число 10:     " (dict/lookup d6 10))
  (println "  Строка 'apple':" (dict/lookup d6 "apple"))
  (println "  Keyword :name:" (dict/lookup d6 :name))
  (println "  Число 5:      " (dict/lookup d6 5))
  (println "  Несуществующий:" (dict/lookup d6 "xyz"))
  
  ;; Структура дерева
  (println "\n🌳 Порядок элементов в дереве:")
  (println "  (отсортированы сначала по типу, потом по значению)\n")
  (doseq [k (dict/dict->seq d6)]
    (println (format "    %s [%s] -> %s" 
                     k 
                     (.getSimpleName (class k))
                     (dict/lookup d6 k))))
  
  ;; Количество элементов
  (println (format "\n📊 Всего элементов: %d\n" (count d6)))
  
  ;; Операции
  (println "🔧 Операции над словарем:\n")
  (def d7 (dict/dict-map d6 (fn [k v] (str v "!"))))
  (println "  Map (добавили '!' к значениям):")
  (println "    Число 10 теперь:" (dict/lookup d7 10))
  
  (def d8 (dict/dict-filter d6 (fn [k v] (number? k))))
  (println "\n  Filter (только числовые ключи):")
  (println "    Осталось элементов:" (count d8))
  (println "    Ключи:" (dict/dict->seq d8))
  
  (println "\n✅ Демонстрация завершена!")
  )

;; Запустите (demo) чтобы увидеть все в действии!

