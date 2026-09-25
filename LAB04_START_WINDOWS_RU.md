# Лабораторная работа 4: запуск на Windows

Это продолжение исходного проекта Lab 3. Внутри архива уже находится локальная ветка `lab04` с кодом. Распакуйте архив **в новую папку**, чтобы не перезаписать свою рабочую копию Lab 3. Старую папку `spring-lab-git` удалять не нужно.

## Сборка и запуск

Откройте PowerShell в распакованной папке `spring-lab-git`:

```powershell
git status -sb
java -version
.\mvnw.cmd package
java -jar .\target\spring-lab-01-0.0.1-SNAPSHOT.jar
```

Для сборки проекта требуется Java 25, как и в Lab 3. Дождитесь сообщения `Started SpringLab01Application`. Если порт 8080 занят, остановите старый процесс Java, прежде чем запускать новый JAR. Не запускайте JAR из старой папки Lab 3.

## Проверки в другом окне PowerShell

```powershell
Invoke-RestMethod http://localhost:8080/api/lab4/item/5
Invoke-RestMethod 'http://localhost:8080/api/lab4/items?limit=5'
Invoke-RestMethod http://localhost:8080/api/lab4/proxy
Invoke-RestMethod http://localhost:8080/api/lab4/remove-twice/5
Invoke-RestMethod http://localhost:8080/api/lab4/remove-twice-fixed/5
$key = [guid]::NewGuid().ToString()
Invoke-RestMethod "http://localhost:8080/api/lab4/retry/$key"
Invoke-RestMethod -Method Delete http://localhost:8080/api/lab4/item/5
Invoke-RestMethod -Method Delete http://localhost:8080/api/lab4/item/0
```

Последний запрос специально вызывает ошибку: PowerShell сообщит об HTTP 500, а консоль приложения покажет `[LOG] !!` и `[AUDIT] ... failure`. Это ожидаемый результат эксперимента.

- `/items`: пять строк лога в порядке `[AUDIT] start`, `[LOG] ->`, `[TIME] SLOW`, `[LOG] <-`, `[AUDIT] ... success`.
- `/item/5`: есть `[LOG]` и `[TIME]`; записей `[AUDIT]` нет.
- `/proxy`: `isAopProxy` и `isCglib` равны `true`.
- `/remove-twice/5`: для внутренних вызовов `remove` отсутствуют записи `[AUDIT]`.
- `/remove-twice-fixed/5`: появляются две пары записей `[AUDIT]` для вызовов через Spring proxy.
- `/retry/<уникальный ключ>`: в логе три попытки `[RETRY]`, ответ сообщает об успехе с третьей попытки. Каждый раз используйте новый ключ.

В качестве индивидуального задания пока реализован **вариант 3** (повтор вызова после исключения): это вариант, который использовался для Lab 3. Если ваш номер в списке группы для Lab 4 другой, сообщите его до подготовки скриншотов и отчета.

После успешной проверки ветку из распакованной папки можно отправить командой `git push -u origin lab04`. Если Lab 3 еще не в `main`, при создании PR выберите в качестве базовой ветки `lab03`, чтобы в PR были только изменения Lab 4.
