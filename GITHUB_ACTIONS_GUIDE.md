# GitHub Actions CI/CD & Подпись Debug ключами

В проекте настроен полноценный пайплайн GitHub Actions (`.github/workflows/android.yml`), генерация Gradle Wrapper (`gradlew`, `gradlew.bat`), а также автоматическая подпись APK файлов debug ключами.

---

## 1. Что настроено

1. **Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`):**
   - Файлы `gradlew` и `gradlew.bat` сгенерированы и готовы к запуску на Linux, macOS и Windows.
   - Скрипт `gradlew` имеет флаг исполняемости (`chmod +x gradlew`).
   - Версия Gradle зафиксирована в `gradle/wrapper/gradle-wrapper.properties` (`9.3.1`).

2. **GitHub Actions Workflow (`.github/workflows/android.yml`):**
   - **Триггеры:**
     - `push` в ветки `main`, `master`, `develop`
     - `pull_request` в ветки `main`, `master`
     - `workflow_dispatch` — ручной запуск из вкладки Actions на GitHub с возможностью выбора сборки release версии.
   - **Окружение:**
     - `ubuntu-latest`
     - JDK 21 (Temurin)
     - `gradle/actions/setup-gradle@v4` с умным кэшированием зависимостей и задач сборки.
   - **Автоматическая подготовка окружения:**
     - Создание `.env` из `.env.example` для плагина `secrets-gradle-plugin`.
     - Поддержка секретов репозитория (`GEMINI_API_KEY` и др.).

3. **Подпись ключами Debug (`debug.keystore`):**
   - В корне проекта находится файл `debug.keystore.base64`. Пайплайн автоматически восстанавливает из него оригинальный `debug.keystore`.
   - Если файла нет, пайплайн автоматически генерирует стандартный Android Debug Keystore через `keytool` (`alias: androiddebugkey`, `pass: android`).
   - Сборка `assembleDebug` подписывается ключом debug автоматически.
   - Сборка `assembleRelease` при отсутствии отдельного релизного ключа также автоматически подписывается с помощью `debug.keystore` и `apksigner`, создавая готовый к установке APK: `app-release-signed-debug.apk`.
   - Проводится автоматическая валидация подписи через `apksigner verify --verbose`.

4. **Артефакты (Artifacts):**
   - `app-debug`: готовый подписанный отладочный APK (`app-debug.apk`).
   - `app-release`: готовые релизные APK / AAB.
   - `test-reports`: отчеты о прохождении тестов при возникновении сбоев.

---

## 2. Локальный запуск через `./gradlew`

Для сборки проекта на локальном компьютере:

```bash
# Сделать gradlew исполняемым (если клонировали на Linux/macOS)
chmod +x gradlew

# Сборка Debug APK (подписывается debug.keystore)
./gradlew assembleDebug

# Запуск тестов
./gradlew testDebugUnitTest

# Сборка Release APK
./gradlew assembleRelease
```

---

## 3. Настройка Production Release ключа (опционально)

Если в будущем потребуется подписывать релизную версию настоящим Google Play ключом, достаточно добавить секреты в настройках репозитория на GitHub (**Settings -> Secrets and variables -> Actions**):

- `RELEASE_KEYSTORE_BASE64`: ваш `.jks` или `.keystore` файл, закодированный в base64 (`base64 -w 0 upload-key.jks`)
- `RELEASE_STORE_PASSWORD`: пароль хранилища
- `RELEASE_KEY_PASSWORD`: пароль ключа

Если эти секреты не заданы, пайплайн автоматически использует debug ключ для подписи.

---

## 4. Решение ошибки `chmod: cannot access 'gradlew': No such file or directory`

Если при запуске GitHub Actions возникла эта ошибка, это означает, что файл `gradlew` не попал в репозиторий Git (не был добавлен в коммит).

### Как зафиксировать файлы Wrapper в Git:

Выполните в корне локального репозитория:

```bash
# Принудительно добавить файлы Gradle Wrapper в Git
git add -f gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties

# Установить права на исполнение для Git
git update-index --chmod=+x gradlew

# Закоммитить и отправить в GitHub
git commit -m "chore: add gradlew and gradle wrapper"
git push
```

### Автоматическая защита в CI:
В `.github/workflows/android.yml` теперь встроена автоматическая проверка:
1. Если `gradlew` находится во вложенной папке — пайплайн найдет и скопирует его в корень.
2. Если `gradlew` вовсе отсутствует в репозитории — пайплайн автоматически сгенерирует Wrapper на раннере GitHub Actions, не прерывая сборку.

