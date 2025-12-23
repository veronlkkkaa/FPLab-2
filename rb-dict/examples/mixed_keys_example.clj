(ns rb-dict.examples.mixed-keys-example
  (:require [rb-dict.cmp :as cmp]
            [rb-dict.core :as dict]))

;; ============================================
;; ПРИМЕР ИСПОЛЬЗОВАНИЯ СМЕШАННЫХ КЛЮЧЕЙ
;; ============================================

(comment
  ;; 1. Создаём словарь с компаратором для смешанных ключей
  (def d (dict/empty-dict-with cmp/mixed-cmp))
  
  ;; 2. Добавляем элементы разных типов
  (def d2 (-> d
              (dict/insert 2 "two")
              (dict/insert "10" "ten")
              (dict/insert 1 "one")
              (dict/insert "2" "string-two")
              (dict/insert :keyword "ключевое слово")
              (dict/insert :age 30)))
  
  ;; 3. Поиск элементов
  (dict/lookup d2 1)        ;; => "one"
  (dict/lookup d2 2)        ;; => "two"
  (dict/lookup d2 "2")      ;; => "string-two"
  (dict/lookup d2 "10")     ;; => "ten"
  (dict/lookup d2 :keyword) ;; => "ключевое слово"
  (dict/lookup d2 :age)     ;; => 30
  
  ;; 4. Порядок элементов (numbers < strings < keywords)
  (seq d2)
  ;; => (1 2 "10" "2" :age :keyword)
  
  ;; 5. Операции над словарём
  
  ;; Map - изменяем значения
  (def d3 (dict/dict-map d2 (fn [k v] (str v "!"))))
  (dict/lookup d3 1)  ;; => "one!"
  
  ;; Filter - оставляем только числовые ключи
  (def d4 (dict/dict-filter d2 (fn [k v] (number? k))))
  (seq d4)  ;; => (1 2)
  
  ;; Fold - собираем все значения
  (dict/foldl d2 (fn [acc k v] (conj acc v)) [])
  ;; => ["one" "two" "ten" "string-two" 30 "ключевое слово"]
  
  ;; 6. Моноид - объединение словарей
  (def d5 (dict/empty-dict-with cmp/mixed-cmp))
  (def d6 (-> d5
              (dict/insert 100 "сто")
              (dict/insert "hello" "привет")))
  
  (def d-merged (dict/mappend d2 d6))
  (dict/lookup d-merged 1)       ;; => "one"
  (dict/lookup d-merged 100)     ;; => "сто"
  (dict/lookup d-merged "hello") ;; => "привет"
  
  ;; 7. Компаратор сохраняется при операциях
  (def d-empty (dict/empty-dict d2))  ;; Пустой словарь с тем же компаратором
  (def d7 (-> d-empty
              (dict/insert 5 "пять")
              (dict/insert "5" "строка-пять")))
  
  (dict/lookup d7 5)   ;; => "пять"
  (dict/lookup d7 "5") ;; => "строка-пять"
  
  ;; 8. Использование как обычной Clojure коллекции
  (count d2)  ;; => 6
  
  ;; Как функция
  (d2 :keyword)  ;; => "ключевое слово"
  
  ;; В циклах
  (for [k d2] k)
  ;; => (1 2 "10" "2" :age :keyword)
  
  ;; С assoc (благодаря Associative)
  (def d8 (assoc d2 999 "новый элемент"))
  (dict/lookup d8 999)  ;; => "новый элемент"
  
  )

;; ============================================
;; ПРАКТИЧЕСКИЙ ПРИМЕР: Конфигурация приложения
;; ============================================

(defn create-app-config []
  "Создаёт словарь конфигурации с разными типами ключей"
  (let [config (dict/empty-dict-with cmp/mixed-cmp)]
    (-> config
        ;; Числовые ключи - порты
        (dict/insert 8080 "HTTP порт")
        (dict/insert 8443 "HTTPS порт")
        (dict/insert 5432 "PostgreSQL порт")
        
        ;; Строковые ключи - пути
        (dict/insert "db-host" "localhost")
        (dict/insert "log-path" "/var/log/app.log")
        (dict/insert "config-file" "/etc/app/config.yml")
        
        ;; Keywords - настройки приложения
        (dict/insert :app-name "MyApp")
        (dict/insert :version "1.0.0")
        (dict/insert :debug true)
        (dict/insert :max-connections 100))))

(comment
  (def config (create-app-config))
  
  ;; Получение значений
  (dict/lookup config 8080)           ;; => "HTTP порт"
  (dict/lookup config "db-host")      ;; => "localhost"
  (dict/lookup config :app-name)      ;; => "MyApp"
  
  ;; Все ключи в порядке (numbers < strings < keywords)
  (seq config)
  ;; => (5432 8080 8443 "config-file" "db-host" "log-path" :app-name :debug :max-connections :version)
  
  ;; Фильтрация - только keywords
  (def app-settings (dict/dict-filter config (fn [k v] (keyword? k))))
  (seq app-settings)
  ;; => (:app-name :debug :max-connections :version)
  )

;; ============================================
;; ДЕМОНСТРАЦИЯ: Почему нужен mixed-cmp
;; ============================================

(defn demo-why-mixed-cmp []
  (println "=== ДЕМОНСТРАЦИЯ: Зачем нужен mixed-cmp ===\n")
  
  ;; С обычным compare (НЕ РАБОТАЕТ для смешанных типов)
  (println "1. Попытка использовать обычный словарь:")
  (def d-normal dict/empty-dict)
  (def d1 (dict/insert d-normal 5 "пять"))
  (println "   ✓ Добавили число 5")
  
  (try
    (def d2 (dict/insert d1 "hello" "привет"))
    (println "   ✓ Добавили строку 'hello'")
    (catch Exception e
      (println "   ✗ ОШИБКА:" (.getMessage e))))
  
  (println "\n2. С mixed-cmp всё работает:")
  (def d-mixed (dict/empty-dict-with cmp/mixed-cmp))
  (def d3 (-> d-mixed
              (dict/insert 5 "пять")
              (dict/insert "hello" "привет")
              (dict/insert :keyword "ключ")))
  (println "   ✓ Добавили число 5")
  (println "   ✓ Добавили строку 'hello'")
  (println "   ✓ Добавили keyword :keyword")
  
  (println "\n3. Поиск работает корректно:")
  (println "   Число 5:       " (dict/lookup d3 5))
  (println "   Строка 'hello':" (dict/lookup d3 "hello"))
  (println "   Keyword :keyword:" (dict/lookup d3 :keyword))
  
  (println "\n4. Порядок элементов:")
  (println "   " (seq d3))
  (println "   (сначала числа, потом строки, потом keywords)\n"))

;; Запустите: (demo-why-mixed-cmp)

