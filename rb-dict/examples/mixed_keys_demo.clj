(ns mixed-keys-demo
  (:require [rb-dict.api :as api]
            [rb-dict.impl :as impl]))

(defn mixed-compare [a b]
  "Функция сравнения для смешанных типов: сначала сравниваем типы, потом значения"
  (let [type-a (type a)
        type-b (type b)]
    (if (= type-a type-b)
      (compare a b)
      (compare (.getName type-a) (.getName type-b)))))

(defn dict-size [d]
  (api/dict-foldl d (fn [acc _ _] (inc acc)) 0))

(defn dict-keys [d]
  (api/dict-foldl d (fn [acc k _] (conj acc k)) []))

(defn dict-to-map [d]
  (api/dict-foldl d (fn [acc k v] (assoc acc k v)) {}))

(defn -main []
  (println "=== Демонстрация работы со смешанными ключами ===\n")
  
  ;; 1. Словарь только с числами
  (let [d1 (impl/->RBDict nil compare)]
    (let [d1 (-> d1
                 (api/dict-insert 1 "один")
                 (api/dict-insert 5 "пять")
                 (api/dict-insert 3 "три")
                 (api/dict-insert 10 "десять"))]
      (println "1. Словарь с числовыми ключами:")
      (println "   Размер:" (dict-size d1))
      (println "   Ключи в порядке возрастания:" (dict-keys d1))
      (println "   Поиск ключа 3:" (api/dict-lookup d1 3))
      (println "   Все пары:" (dict-to-map d1))
      (println)))
  
  ;; 2. Словарь только со строками
  (let [d2 (impl/->RBDict nil compare)]
    (let [d2 (-> d2
                 (api/dict-insert "apple" "яблоко")
                 (api/dict-insert "zebra" "зебра")
                 (api/dict-insert "banana" "банан")
                 (api/dict-insert "cherry" "вишня"))]
      (println "2. Словарь со строковыми ключами:")
      (println "   Размер:" (dict-size d2))
      (println "   Ключи в алфавитном порядке:" (dict-keys d2))
      (println "   Поиск \"banana\":" (api/dict-lookup d2 "banana"))
      (println "   Все пары:" (dict-to-map d2))
      (println)))
  
  ;; 3. Словарь со СМЕШАННЫМИ ключами (с особой функцией сравнения)
  (let [d3 (impl/->RBDict nil mixed-compare)]
    (let [d3 (-> d3
                 (api/dict-insert 1 "один")
                 (api/dict-insert "apple" "яблоко")
                 (api/dict-insert 5 "пять")
                 (api/dict-insert "banana" "банан")
                 (api/dict-insert 3 "три")
                 (api/dict-insert "zebra" "зебра"))]
      (println "3. Словарь со СМЕШАННЫМИ ключами (числа + строки):")
      (println "   Размер:" (dict-size d3))
      (println "   Ключи (числа и строки):" (dict-keys d3))
      (println "   Все пары:" (dict-to-map d3))
      (println "   Поиск числа 3:" (api/dict-lookup d3 3))
      (println "   Поиск строки \"banana\":" (api/dict-lookup d3 "banana"))
      (println "   Примечание: числа и строки группируются по типам")
      (println)))
  
  ;; 4. Map - преобразование значений
  (let [d (impl/->RBDict nil mixed-compare)
        d (-> d
              (api/dict-insert 1 "один")
              (api/dict-insert "apple" "яблоко"))
        d2 (api/dict-map d (fn [k v] (str v "!!!")))]
    (println "4. Применили map (добавили !!! к значениям):")
    (println "   До:" (dict-to-map d))
    (println "   После:" (dict-to-map d2))
    (println))
  
  ;; 5. Filter - фильтрация по предикату
  (let [d (impl/->RBDict nil mixed-compare)
        d (-> d
              (api/dict-insert 1 "один")
              (api/dict-insert "apple" "яблоко")
              (api/dict-insert 3 "три")
              (api/dict-insert "banana" "банан"))
        d2 (api/dict-filter d (fn [k v] (string? k)))]
    (println "5. Отфильтровали только строковые ключи:")
    (println "   До:" (dict-to-map d))
    (println "   После фильтра:" (dict-to-map d2))
    (println))
  
  ;; 6. Fold - агрегация
  (let [d (impl/->RBDict nil mixed-compare)
        d (-> d
              (api/dict-insert 1 "aa")
              (api/dict-insert 2 "bbb")
              (api/dict-insert "x" "cccc"))
        total (api/dict-foldl d (fn [acc k v] (+ acc (clojure.core/count v))) 0)]
    (println "6. Посчитали суммарную длину всех значений:")
    (println "   Словарь:" (dict-to-map d))
    (println "   Сумма длин значений:" total)
    (println))
  
  ;; 7. Удаление элементов
  (let [d (impl/->RBDict nil mixed-compare)
        d (-> d
              (api/dict-insert 1 "один")
              (api/dict-insert "apple" "яблоко")
              (api/dict-insert 3 "три"))
        d2 (-> d
               (api/dict-remove 1)
               (api/dict-remove "apple"))]
    (println "7. Удалили ключи 1 и \"apple\":")
    (println "   До:" (dict-to-map d))
    (println "   После удаления:" (dict-to-map d2))
    (println "   Проверка - ключ 1:" (api/dict-lookup d2 1))
    (println "   Проверка - ключ \"apple\":" (api/dict-lookup d2 "apple"))
    (println))
  
  ;; 8. Моноид - объединение словарей
  (let [d1 (impl/->RBDict nil mixed-compare)
        d1 (-> d1
               (api/dict-insert 1 "один")
               (api/dict-insert "apple" "яблоко"))
        d2 (impl/->RBDict nil mixed-compare)
        d2 (-> d2
               (api/dict-insert 10 "десять")
               (api/dict-insert "zebra" "зебра"))
        d3 (api/dict-mappend d1 d2)]
    (println "8. Объединили два словаря (моноидальная операция):")
    (println "   Первый:" (dict-to-map d1))
    (println "   Второй:" (dict-to-map d2))
    (println "   Объединенный:" (dict-to-map d3))
    (println "   Размер:" (dict-size d3))
    (println))
  
  ;; 9. Эквивалентность
  (let [d1 (impl/->RBDict nil mixed-compare)
        d1 (-> d1
               (api/dict-insert "test" "тест")
               (api/dict-insert 42 "ответ"))
        d2 (impl/->RBDict nil mixed-compare)
        d2 (-> d2
               (api/dict-insert 42 "ответ")
               (api/dict-insert "test" "тест"))]
    (println "9. Проверка эквивалентности словарей:")
    (println "   d1 (добавлено: test, 42):" (dict-to-map d1))
    (println "   d2 (добавлено: 42, test):" (dict-to-map d2))
    (println "   Эквивалентны?" (api/dict-equal? d1 d2))
    (println))
  
  (println "=== Заключение ===")
  (println "• Словарь работает с любыми сравнимыми типами ключей")
  (println "• Для смешанных типов можно задать свою функцию сравнения")
  (println "• Ключи всегда хранятся в отсортированном порядке")
  (println "• Все операции иммутабельны - создают новый словарь"))

;; Запускаем
(-main)
