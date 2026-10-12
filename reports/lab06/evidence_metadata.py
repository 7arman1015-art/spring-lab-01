"""English captions and measured descriptions for retained application evidence."""
import re
from pathlib import Path

LEGACY_ENGLISH = {
    "Ветка lab06 опубликована в GitHub. Код, доказательства и отчёт сохранены в репозитории.":
        "The lab06 branch is published on GitHub. The code, evidence and report are stored in the repository.",
    "Локальная ветка lab06 подготовлена. Отправка в GitHub ожидает подтверждения.":
        "The local lab06 branch is ready. Publication on GitHub is awaiting confirmation.",
    "Maven 3.9.16: mvn -B package (прямой запуск Maven из кэша Wrapper)":
        "Maven 3.9.16: mvn -B package, run directly from the Maven Wrapper cache.",
    "Отклонённые формы": "Rejected submissions",
    "Шесть некорректных отправок не добавили ни одной книги.":
        "Six invalid submissions did not create any books.",
    "Вариант 11": "Variant 11",
    "libraryName отображается в общей шапке формы, каталога, деталей и страницы ошибки.":
        "libraryName appears in the shared header of the form, catalogue, detail page and error page.",
    "Ответы 404": "404 responses",
    "HTML: 404 text/html; API: 404 application/problem+json. Содержимое и заголовки сохранены отдельно.":
        "HTML: 404 text/html. API: 404 application/problem+json. The response bodies and headers were also saved.",
    "Браузер": "Browser",
    "Реальные формы отправлены через headless Google Chrome. Ошибок pageerror не обнаружено.":
        "The forms were submitted in a real headless Google Chrome session. No pageerror events were detected.",
    "HTML error/not-found; «Книга не найдена», идентификатор 999.":
        "HTML view error/not-found with a localized Book not found message for identifier 999.",
    "JSON ProblemDetail с status=404 и instance=/api/books/999.":
        "JSON ProblemDetail with status=404 and instance=/api/books/999.",
    "Проверено в реальном Chrome: POST /books → HTTP 302 (Location: http://localhost:8080/books) → GET /books. Создана одна книга; после reload flash-сообщение отсутствует и число книг остаётся 4.":
        "Verified in real Chrome: POST /books returned HTTP 302 with Location: http://localhost:8080/books, "
        "followed by GET /books. One book was created. After reloading the page, the flash message disappeared "
        "and the total remained four books.",
}

EVIDENCE_ENGLISH = {
    "01-empty-title-en.png": (
        "An empty title produces an English validation message",
        "POST /books returned HTTP 200 with the same form. The title error appears next to the field "
        "and in the summary. The author, year, genre and availability checkbox retain their values."),
    "02-year-out-of-range-en.png": (
        "The year 1200 is rejected",
        "@Min rejects the year 1200. The submitted values remain in the form, and the year input has a red border."),
    "03-no-genre-en.png": (
        "The genre is not selected",
        "@NotNull requires a genre. The title, author, year and availability flag are retained; "
        "the controller restores the genre options in the model."),
    "04-empty-title-ru.png": (
        "The same form and validation message in Russian",
        "After ?lang=ru, the labels, genres, buttons, shared heading and validation message appear in Russian. "
        "The selected locale remains in the HTTP session for the POST request."),
    "05-custom-constraint-ru.png": (
        "The custom NoDigits constraint rejects an author containing digits",
        "NoDigitsValidator rejects Bloch 2018. The Russian error message is resolved from "
        "book.author.nodigits in the Russian resource bundle."),
    "06-two-errors-ru.png": (
        "Two field errors are shown together",
        "The year 1200 and the missing genre are checked in the same submission. "
        "#fields.allErrors() lists both violations; each message also appears beside its field."),
    "07-success-ru.png": (
        "A valid submission creates a book",
        "The valid POST returned HTTP 302 and redirected to /books. The following GET shows a one-time "
        "flash message and the new Effective Java book with its selected genre and availability."),
    "08-refresh-no-duplicate.png": (
        "Reloading the result does not create a duplicate",
        "After reloading GET /books, the flash message is gone. The book count remains unchanged, "
        "and Effective Java appears exactly once."),
    "09-page-not-found.png": (
        "An HTML error page for a missing book",
        "GET /books/999 returned HTTP 404 with the error/not-found HTML view. "
        "PageExceptionHandler also supplies the shared libraryName attribute."),
    "10-api-problem-detail.png": (
        "A ProblemDetail response for the API",
        "GET /api/books/999 returned HTTP 404 with Content-Type application/problem+json "
        "and the type, title, status, detail and instance fields. ApiExceptionHandler handled the request."),
    "11-variant11-common-model.png": (
        "Variant 11 supplies the shared attribute on the book detail page",
        "The detail page displays libraryName supplied by @ModelAttribute in PageExceptionHandler. "
        "The same attribute is used by the form, catalogue and HTML error page."),
}

def english_text(value) -> str:
    text = str(value)
    translated = LEGACY_ENGLISH.get(text, text)
    if re.search(r"[\u0400-\u04ff]", translated):
        raise ValueError(f"English report metadata is required: {text}")
    return translated

def english_screenshot(record: dict) -> dict:
    result = dict(record)
    caption = str(record.get("caption", ""))
    details = str(record.get("details", ""))
    if re.search(r"[\u0400-\u04ff]", caption + details):
        known = EVIDENCE_ENGLISH.get(Path(record["path"]).name)
        if known is None:
            raise ValueError(f"Translate this screenshot metadata into English: {record['path']}")
        result["caption"], result["details"] = known
        urls = re.findall(r"https?://[^\s]+", details)
        if urls:
            result["details"] += " URL: " + urls[-1]
    else:
        result["caption"] = english_text(caption)
        result["details"] = english_text(details)
    return result
