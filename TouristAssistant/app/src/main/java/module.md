# Tourist Assistant - Документация модуля

## Общее описание
Tourist Assistant - это мобильное приложение для туристов, предоставляющее информацию о достопримечательностях, 
ресторанах, отелях и других местах, а также возможность построения маршрутов.

## Архитектура
Приложение построено по принципу чистой архитектуры с использованием:
- **MVVM** для UI слоя
- **Repository pattern** для доступа к данным
- **Dependency Injection** через Hilt
- **Room** для локальной базы данных

## Основные пакеты

### `com.example.touristassistant.ui`
Содержит все экраны и компоненты пользовательского интерфейса:
- `MapScreen` - основной экран с картой
- `PlacesScreen` - список мест
- `RoutesScreen` - список маршрутов

### `com.example.touristassistant.data`
Содержит модели данных, DAO и репозитории:
- `models` - модели Place, Route, RoutePoint
- `dao` - интерфейсы доступа к данным
- `repositories` - реализация репозиториев

### `com.example.touristassistant.di`
Конфигурация Dependency Injection через Hilt

## Как использовать
Для генерации документации выполните:

```bash
# Генерация HTML документации
./gradlew dokkaHtml

# Генерация Javadoc
./gradlew dokkaJavadoc

# Генерация всех видов документации
./gradlew generateDocumentation