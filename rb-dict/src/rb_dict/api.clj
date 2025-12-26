(ns rb-dict.api)

(defprotocol IDict
  (dict-empty [this] "Пустой словарь")
  (dict-insert [this k v] "Добавляет элемент")
  (dict-remove [this k] "Удаляет элемент")
  (dict-lookup [this k] "Возвращает значение или nil")

  (dict-map [this f] "f k v -> v' : отображение")
  (dict-filter [this pred] "pred k v -> bool")

  (dict-foldl [this f init] "Левая свёртка")
  (dict-foldr [this f init] "Правая свёртка")

  (dict-mempty [this])
  (dict-mappend [this other])

  (dict-equal? [this other]))