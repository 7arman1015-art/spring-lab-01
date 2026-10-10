"""Build the Lab 6 report from the current source files and genuine evidence.

Run with the bundled Codex Python after the document-operation marker succeeds.
This script deliberately does not run Maven, change Git state, fabricate pictures,
or render the resulting DOCX. Its output must pass render and visual QA separately.
"""
from __future__ import annotations

import argparse
import json
import re
import textwrap
from pathlib import Path

from docx import Document
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Inches, Pt, RGBColor
from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
REPORT_DIR = ROOT / "reports" / "lab06"
DEFAULT_MANIFEST = REPORT_DIR / "evidence" / "manifest.json"
REPO_LINK = "https://github.com/7arman1015-art/spring-lab-01/tree/lab06"


def source(name: str) -> tuple[str, str]:
    matches = list((ROOT / "src" / "main").rglob(name))
    if len(matches) != 1:
        raise RuntimeError(f"Expected one source file {name}; found {matches}")
    path = matches[0]
    return path.relative_to(ROOT).as_posix(), path.read_text(encoding="utf-8-sig")


def field(paragraph, instruction: str, display: str = "1") -> None:
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    run = paragraph.add_run()
    run._r.append(begin)
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = instruction
    paragraph.add_run()._r.append(instr)
    separate = OxmlElement("w:fldChar")
    separate.set(qn("w:fldCharType"), "separate")
    paragraph.add_run()._r.append(separate)
    paragraph.add_run(display)
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    paragraph.add_run()._r.append(end)


def hyperlink(paragraph, label: str, target: str) -> None:
    from docx.opc.constants import RELATIONSHIP_TYPE
    link = OxmlElement("w:hyperlink")
    link.set(qn("r:id"), paragraph.part.relate_to(target, RELATIONSHIP_TYPE.HYPERLINK, is_external=True))
    run = OxmlElement("w:r")
    prop = OxmlElement("w:rPr")
    color = OxmlElement("w:color")
    color.set(qn("w:val"), "000000")
    prop.append(color)
    underline = OxmlElement("w:u")
    underline.set(qn("w:val"), "single")
    prop.append(underline)
    run.append(prop)
    text = OxmlElement("w:t")
    text.text = label
    run.append(text)
    link.append(run)
    paragraph._p.append(link)


def configure(doc) -> None:
    sec = doc.sections[0]
    sec.page_width, sec.page_height = Cm(21), Cm(29.7)
    sec.top_margin, sec.bottom_margin = Cm(1.9), Cm(1.8)
    sec.left_margin, sec.right_margin = Cm(2.0), Cm(2.0)
    sec.footer_distance = Cm(0.8)
    sec.different_first_page_header_footer = True
    normal = doc.styles["Normal"]
    normal.font.name = "Times New Roman"
    normal.font.size = Pt(11)
    normal.font.color.rgb = RGBColor(0, 0, 0)
    normal.paragraph_format.line_spacing = 1.12
    normal.paragraph_format.space_after = Pt(6)
    for name in ("Title", "Subtitle", "Heading 1", "Heading 2", "Heading 3"):
        style = doc.styles[name]
        style.font.name = "Times New Roman"
        style.font.color.rgb = RGBColor(0, 0, 0)
        style.font.underline = False
        if name.startswith("Heading"):
            style.font.size = Pt(14 if name == "Heading 1" else 12)
            style.font.bold = True
            style.paragraph_format.space_before = Pt(12)
            style.paragraph_format.space_after = Pt(6)
            style.paragraph_format.keep_with_next = True
    doc.styles["Title"].font.size = Pt(21)
    doc.styles["Subtitle"].font.size = Pt(13)
    caption = doc.styles["Caption"]
    caption.font.name = "Times New Roman"
    caption.font.size = Pt(9)
    caption.font.color.rgb = RGBColor(0, 0, 0)
    caption.paragraph_format.space_after = Pt(7)
    code = doc.styles.add_style("Code Listing", 1)
    code.font.name = "Consolas"
    code.font.size = Pt(8)
    code.font.color.rgb = RGBColor(0, 0, 0)
    code.paragraph_format.line_spacing = Pt(9.3)
    code.paragraph_format.space_after = Pt(0)
    code.paragraph_format.space_before = Pt(0)
    code.paragraph_format.keep_with_next = False
    code.paragraph_format.widow_control = False
    # Word's packaged template includes a blue Title border and theme font slots.
    # Remove them rather than letting LibreOffice/Word override the chosen fonts.
    chosen_styles = [normal, code, caption] + [
        doc.styles[name] for name in ("Title", "Subtitle", "Heading 1", "Heading 2", "Heading 3")
    ]
    for st in chosen_styles:
        for border in st.element.xpath("./w:pPr/w:pBdr"):
            border.getparent().remove(border)
        fonts = st.element.get_or_add_rPr().get_or_add_rFonts()
        for theme in ("asciiTheme", "hAnsiTheme", "eastAsiaTheme", "cstheme", "csTheme"):
            fonts.attrib.pop(qn("w:" + theme), None)
        for script in ("ascii", "hAnsi", "eastAsia", "cs"):
            fonts.set(qn("w:" + script), st.font.name)
        for color in st.element.xpath("./w:rPr/w:color"):
            for attr in ("themeColor", "themeTint", "themeShade"):
                color.attrib.pop(qn("w:" + attr), None)
            color.set(qn("w:val"), "000000")
        if st.font.size is not None:
            props = st.element.get_or_add_rPr()
            size_cs = props.find(qn("w:szCs"))
            if size_cs is None:
                size_cs = OxmlElement("w:szCs")
                props.append(size_cs)
            size_cs.set(qn("w:val"), str(round(st.font.size.pt * 2)))
    footer = sec.footer.paragraphs[0]
    footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
    field(footer, "PAGE")
    for run in footer.runs:
        run.font.size = Pt(9)
        run.font.name = "Times New Roman"
    doc.core_properties.title = "Лабораторная работа 6 Формы и валидация"
    doc.core_properties.subject = "RWPSF 3305 Web Application Development with Spring Framework"
    doc.core_properties.author = "Aruzhan Zhaksybekova; Mukhamedjan Arman"
    doc.core_properties.keywords = "Spring MVC, Thymeleaf, Bean Validation, i18n, ControllerAdvice, вариант 11"


def paragraph(doc, text: str, **kwargs):
    p = doc.add_paragraph(text, **kwargs)
    if p.style.name in ("Title", "Subtitle"):
        for border in p._p.xpath("./w:pPr/w:pBdr"):
            border.getparent().remove(border)
    return p


def heading(doc, text: str, level: int = 1):
    return doc.add_heading(text, level=level)


def table(doc, headers: list[str], rows: list[list[str]], widths: list[float]):
    t = doc.add_table(rows=1, cols=len(headers))
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False
    for col, width in zip(t.columns, widths):
        col.width = Cm(width)
    tblpr = t._tbl.tblPr
    borders = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        element = OxmlElement("w:" + edge)
        element.set(qn("w:val"), "single")
        element.set(qn("w:sz"), "4")
        element.set(qn("w:color"), "D9D9D9")
        borders.append(element)
    tblpr.append(borders)
    margins = OxmlElement("w:tblCellMar")
    for edge, value in (("top", "70"), ("bottom", "70"), ("left", "90"), ("right", "90")):
        item = OxmlElement("w:" + edge)
        item.set(qn("w:w"), value)
        item.set(qn("w:type"), "dxa")
        margins.append(item)
    tblpr.append(margins)
    header_repeat = OxmlElement("w:tblHeader")
    t.rows[0]._tr.get_or_add_trPr().append(header_repeat)
    for i, title in enumerate(headers):
        t.rows[0].cells[i].text = title
    for row in rows:
        cells = t.add_row().cells
        for cell, value in zip(cells, row):
            cell.text = str(value)
    for row_index, row in enumerate(t.rows):
        no_split = OxmlElement("w:cantSplit")
        row._tr.get_or_add_trPr().append(no_split)
        for col_index, cell in enumerate(row.cells):
            cell.width = Cm(widths[col_index])
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            shade = OxmlElement("w:shd")
            shade.set(qn("w:fill"), "E7EBF0" if row_index == 0 else "FFFFFF")
            cell._tc.get_or_add_tcPr().append(shade)
            for p in cell.paragraphs:
                p.paragraph_format.space_after = Pt(0)
                p.paragraph_format.line_spacing = 1.05
                for r in p.runs:
                    r.font.size = Pt(9)
                    r.bold = row_index == 0
                    r.font.color.rgb = RGBColor(0, 0, 0)
    paragraph(doc, "").paragraph_format.space_after = Pt(0)
    return t


def listing(doc, title: str, file_name: str, number: int, excerpt: str | None = None):
    rel, text = source(file_name)
    heading(doc, title, level=2)
    cap = paragraph(doc, f"Листинг {number}. {rel}", style="Caption")
    cap.paragraph_format.keep_with_next = True
    if excerpt is not None:
        text = excerpt
    for original in text.expandtabs(4).rstrip().splitlines():
        # Visual wrapping preserves every source character and keeps listings readable.
        if len(original) <= 96:
            pieces = [original]
        else:
            indent = re.match(r"\s*", original).group(0)
            pieces = textwrap.wrap(original, width=96, subsequent_indent=indent + "    ",
                                   replace_whitespace=False, drop_whitespace=False,
                                   break_long_words=True, break_on_hyphens=False)
        for piece in pieces:
            paragraph(doc, piece, style="Code Listing")
    paragraph(doc, "").paragraph_format.space_after = Pt(2)


def mapping_excerpt(text: str, annotation: str) -> str:
    start = text.index("    " + annotation)
    brace = text.index("{", start)
    balance = 1
    pos = brace + 1
    # Method body uses ordinary Java strings; brace balance works for this source.
    while pos < len(text) and balance:
        balance += (text[pos] == "{") - (text[pos] == "}")
        pos += 1
    return text[start:pos]


def post_excerpt(text: str) -> str:
    return mapping_excerpt(text, "@PostMapping")


def evidence_image(doc, record: dict, evidence_dir: Path, number: int):
    image_path = Path(record["path"])
    if not image_path.is_absolute():
        image_path = evidence_dir / image_path
    if not image_path.is_file():
        raise FileNotFoundError(image_path)
    with Image.open(image_path) as im:
        width_px, height_px = im.size
    width = min(6.50, 4.00 * width_px / height_px)
    p = paragraph(doc, "")
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.0
    p.paragraph_format.keep_with_next = True
    pic = p.add_run().add_picture(str(image_path), width=Inches(width))
    description = record.get("details", record.get("caption", image_path.stem))
    pic._inline.docPr.set("descr", str(description))
    cap = paragraph(doc, f"Рисунок {number}. {record.get('caption', image_path.stem)}", style="Caption")
    cap.paragraph_format.keep_with_next = bool(record.get("details"))
    if record.get("details"):
        paragraph(doc, record["details"])


def validate_manifest(manifest: dict):
    screenshots = manifest.get("screenshots", [])
    responses = manifest.get("responses", [])
    if len(screenshots) < 4:
        raise ValueError("The report needs at least four genuine screenshots.")
    if len(responses) < 2:
        raise ValueError("The report needs the measured HTML and API error responses.")
    if not isinstance(manifest.get("build"), dict):
        raise ValueError("Include build/test metadata; do not invent a passed result.")
    return screenshots, responses


def build(manifest_path: Path, output: Path):
    manifest = json.loads(manifest_path.read_text(encoding="utf-8-sig"))
    screenshots, responses = validate_manifest(manifest)
    doc = Document()
    configure(doc)
    for text in ("Международный университет информационных технологий",
                 "Факультет бизнеса медиа и менеджмента",
                 "Кафедра информационных систем"):
        p = paragraph(doc, text)
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    paragraph(doc, "").paragraph_format.space_after = Pt(30)
    p = paragraph(doc, "Лабораторная работа 6", style="Title")
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p = paragraph(doc, "Формы и валидация", style="Subtitle")
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p = paragraph(doc, "Thymeleaf  Bean Validation  Локализация  Обработка исключений")
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    paragraph(doc, "").paragraph_format.space_after = Pt(28)
    for text in ("Курс RWPSF 3305 Web Application Development with Spring Framework",
                 "Образовательная программа 6B06105 Information Systems",
                 "Выполнили Aruzhan Zhaksybekova и Mukhamedjan Arman",
                 "Группа IT1-2404IS",
                 "Индивидуальный вариант 11",
                 "Преподаватель Yemberdiyeva Aknur"):
        p = paragraph(doc, text)
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    paragraph(doc, "").paragraph_format.space_after = Pt(25)
    p = paragraph(doc, "Алматы 2026")
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    doc.add_page_break()

    heading(doc, "Цель и результат работы")
    paragraph(doc, "Мы реализовали форму добавления книги на Spring MVC и Thymeleaf. "
                   "Форма связывает запрос с BookForm, проверяет поля с помощью Jakarta Bean Validation, "
                   "показывает сообщения на английском и русском языках и сохраняет корректные данные "
                   "через Post/Redirect/Get. Для отсутствующей книги предусмотрены HTML страница "
                   "с кодом 404 и JSON ответ ProblemDetail с тем же статусом.")
    paragraph(doc, "Цель работы состоит в том, чтобы объединить привязку формы, декларативную "
                   "валидацию и централизованную обработку ошибок, сохраняя введённые пользователем "
                   "значения при повторном показе формы.")
    paragraph(doc, "Предоставленный проект содержал реализацию лабораторной работы 4. "
                   "Мы восстановили отсутствовавший каталог книг, необходимый для работы 6, "
                   "и разместили его в пространстве имён kz.iitu.springlab.lab6. "
                   "BookService хранит книги в памяти процесса; после перезапуска приложения "
                   "список возвращается к трём начальным записям.")
    p = paragraph(doc, "Ветка репозитория lab06: ")
    hyperlink(p, REPO_LINK, REPO_LINK)
    publication = manifest.get("publication", {})
    if publication.get("state"):
        paragraph(doc, "Состояние репозитория: " + str(publication["state"]))
    if publication.get("commit"):
        paragraph(doc, "Коммит: " + str(publication["commit"]))

    heading(doc, "Порядок выполнения")
    steps = [
        "Добавили зависимости spring-boot-starter-thymeleaf и spring-boot-starter-validation. "
        "Создали BookForm, перечисление Genre, отдельную модель Book и сервис хранения книг.",
        "Добавили GET /books/new с пустым объектом form и перечнем genres. "
        "В templates/books/form.html связали форму через th:object, поля через th:field "
        "и вывели ошибки с th:errors, th:errorclass и #fields.",
        "Реализовали POST /books. Параметр BindingResult стоит сразу после BookForm с @Valid. "
        "При ошибках контроллер возвращает ту же форму с перечнем жанров. "
        "При успехе сохраняет книгу, добавляет flash сообщение и перенаправляет на /books.",
        "Вынесли подписи, сообщения, названия жанров и уведомление об успехе в messages.properties "
        "и messages_ru.properties. Настроили SessionLocaleResolver и LocaleChangeInterceptor "
        "с параметром lang; локаль по умолчанию английская.",
        "Создали @NoDigits и NoDigitsValidator для автора. Ограничение пропускает null, "
        "а обязательность отдельно проверяет @NotBlank. Добавили раздельные advice для страниц и API.",
        "В варианте 11 добавили @ModelAttribute libraryName в PageExceptionHandler. "
        "Название библиотеки автоматически попадает в модель страниц каталога и локализуется.",
        "Проверили обязательные сценарии в браузере, ошибки 404, переключение языка и "
        "повторное открытие списка после сохранения. Результаты и снимки экрана приведены далее.",
    ]
    for i, text in enumerate(steps, 1):
        paragraph(doc, f"{i}. {text}")

    heading(doc, "Проверка сборки и поведения приложения")
    build_meta = manifest["build"]
    if build_meta.get("command"):
        paragraph(doc, "Команда проверки: " + str(build_meta["command"]))
    paragraph(doc, "Результат: " + str(build_meta.get("summary", "Успешно" if build_meta.get("passed") else "Проверка не завершена")))
    if build_meta.get("tests") is not None:
        paragraph(doc, "Количество тестов: " + str(build_meta["tests"]))
    checks = manifest.get("checks", [])
    if checks:
        table(doc, ["Проверка", "Наблюдаемый результат"],
              [[str(x.get("name", x.get("case", ""))), str(x.get("result", x.get("details", "")))] for x in checks], [5.2, 11.8])
    else:
        paragraph(doc, "Подробные наблюдения указаны в подписях к снимкам экрана. "
                       "При неверном вводе проверяли сообщения около поля и сохранение введённых значений; "
                       "при корректном вводе проверяли переход к списку книг.")
    if manifest.get("prg"):
        paragraph(doc, "Проверка Post/Redirect/Get: " + str(manifest["prg"]))

    heading(doc, "Ответы при отсутствии книги")
    paragraph(doc, "Оба запроса обращаются к отсутствующему идентификатору 999. "
                   "Сервис выбрасывает BookNotFoundException, а формат ответа определяется "
                   "контроллером и областью применения advice.")
    table(doc, ["Запрос", "Статус и Content Type", "Обработчик и содержимое"],
          [[str(x["request"]), f"{x['status']}\n{x['content_type']}",
            str(x.get("handler", "")) + "\n" + str(x.get("result", ""))] for x in responses], [4.4, 4.8, 7.8])

    heading(doc, "Снимки экрана и результаты")
    paragraph(doc, "Снимки получены в запущенном приложении. Обязательные случаи показывают "
                   "пустое название, год 1200, отсутствие жанра и успешное сохранение. "
                   "Дополнительные проверки демонстрируют обе локали, @NoDigits и страницы ошибок.")
    for i, record in enumerate(screenshots, 1):
        evidence_image(doc, record, manifest_path.parent, i)

    doc.add_page_break()
    heading(doc, "Объект формы и привязка данных")
    paragraph(doc, "BookForm отделяет входные поля от объекта Book и ограничивает набор данных, "
                   "которые принимает форма. Integer позволяет отличать незаполненный год от числового "
                   "значения. @NotNull проверяет отсутствие года, а @Min и @Max его диапазон.")
    listing(doc, "Объект BookForm", "BookForm.java", 1)
    listing(doc, "Перечисление жанров", "Genre.java", 2)

    heading(doc, "Шаблон формы")
    paragraph(doc, "th:object задаёт объект form для выражений *{...}. th:field формирует name, id "
                   "и текущее значение поля, включая повторный показ после ошибки. "
                   "Ошибки выводятся рядом с полем и в сводном списке; CSS выделяет неверное поле.")
    listing(doc, "Полный шаблон добавления книги", "form.html", 3)

    heading(doc, "Обработка корректного и неверного запроса")
    paragraph(doc, "После @Valid Spring записывает ошибки в BindingResult. Контроллер возвращает "
                   "books/form при наличии ошибок и повторно добавляет genres, поэтому список жанров "
                   "сохраняется. Успешный POST завершается redirect:/books; flash сообщение "
                   "передаётся только следующему запросу и использует выбранную локаль.")
    _, controller = source("BookPageController.java")
    listing(doc, "Метод POST", "BookPageController.java", 4, post_excerpt(controller))

    heading(doc, "Собственное ограничение для автора")
    paragraph(doc, "@NoDigits проверяет, что имя автора не содержит цифр. Аннотация объявляет "
                   "обязательные message, groups и payload и связывается с NoDigitsValidator. "
                   "Ввод Bloch 2018 отклоняется, а текст ошибки берётся по ключу book.author.nodigits.")
    listing(doc, "Аннотация NoDigits", "NoDigits.java", 5)
    listing(doc, "Валидатор NoDigitsValidator", "NoDigitsValidator.java", 6)

    heading(doc, "Локаль и централизованная обработка исключений")
    paragraph(doc, "SessionLocaleResolver хранит выбранный язык в сессии, а LocaleChangeInterceptor "
                   "обрабатывает ?lang=en и ?lang=ru. PageExceptionHandler обслуживает HTML "
                   "контроллер страниц книг, ApiExceptionHandler обслуживает @RestController. "
                   "Так исключение из сервиса превращается в ожидаемый ответ для каждой аудитории.")
    listing(doc, "Настройка локали", "LocaleConfiguration.java", 7)
    listing(doc, "Advice для HTML страниц и вариант 11", "PageExceptionHandler.java", 8)
    listing(doc, "Advice для API", "ApiExceptionHandler.java", 9)

    heading(doc, "Индивидуальный вариант 11")
    paragraph(doc, "Условие варианта требует @ControllerAdvice с @ModelAttribute, общим атрибутом "
                   "для страниц и одним обработчиком исключения. В листинге 8 метод "
                   "libraryName возвращает локализованное название библиотеки для модели. "
                   "Обработчик bookNotFound в том же классе возвращает error/not-found со статусом 404.")
    paragraph(doc, "Результат виден в общей шапке формы, списка книг, страницы книги и страницы "
                   "ошибки: название библиотеки появляется без ручного добавления в каждом обычном "
                   "GET методе. При выборе русского языка меняется и значение libraryName.")

    heading(doc, "Ресурсные файлы сообщений")
    paragraph(doc, "Ключ в фигурных скобках у ограничения передаётся механизму сообщений. "
                   "MessageSource находит текст в bundle выбранной локали, BindingResult сохраняет "
                   "сообщение, а th:errors выводит его у поля. Файл messages.properties служит "
                   "основным набором и резервом для отсутствующих переводов; файлы сохранены в UTF-8.")
    listing(doc, "Английские подписи и сообщения", "messages.properties", 10)
    listing(doc, "Русские подписи и сообщения", "messages_ru.properties", 11)

    heading(doc, "Сравнение формы с моделью каталога")
    paragraph(doc, "В BookForm есть сеттеры для частичной привязки параметров и ограничения "
                   "ввода. Book представляет уже сохранённую книгу и содержит идентификатор, "
                   "который назначает сервис. У record Book нет сеттеров; новые значения "
                   "передаются через конструктор после успешной проверки формы.")
    listing(doc, "Запись Book для сохранённых данных", "Book.java", 12)
    paragraph(doc, "GET /books/new помещает пустой BookForm в модель под именем form "
                   "и добавляет список Genre.values(). Это обеспечивает разрешение th:object "
                   "при первом открытии и заполнение вариантов списка жанров.")
    listing(doc, "Первое открытие формы", "BookPageController.java", 13,
            mapping_excerpt(controller, '@GetMapping("/new")'))

    heading(doc, "Выводы")
    paragraph(doc, "Декларативная валидация переносит правила полей в BookForm, поэтому контроллер "
                   "не повторяет ручные проверки каждого значения. BindingResult позволяет показать "
                   "все нарушения и сохранить введённые данные на той же странице. "
                   "Ресурсные файлы отделяют тексты от кода и согласуют язык подписей с сообщениями "
                   "валидации. Централизованные advice задают единое поведение ошибок для HTML и API, "
                   "а Post/Redirect/Get предотвращает повторный POST при обновлении результата.")

    heading(doc, "Краткие ответы для защиты")
    qa = [
        ("Зачем отдельный объект формы", "Он задаёт разрешённые входные поля и правила ввода; модель хранения может иметь другие поля и ограничения."),
        ("Почему BindingResult стоит сразу после формы", "Spring должен связать результат привязки и валидации именно с этим параметром. Если его переставить, контроллер может не получить ошибки для обычного повторного показа формы."),
        ("Что изменится без @Valid", "Ограничения Bean Validation на объекте не будут запускаться этим обработчиком, хотя ошибки преобразования типов при привязке по-прежнему возможны."),
        ("Чем отличаются проверки обязательности", "@NotNull запрещает null. @NotEmpty дополнительно запрещает пустую строку или коллекцию. @NotBlank проверяет строку и запрещает значение только из пробелов."),
        ("Какие ошибки используются в работе", "Ограничения полей создают field errors. Ошибка преобразования года также относится к полю. Классное ограничение на несколько полей могло бы создать global error."),
        ("Почему null проходит NoDigitsValidator", "Обязательность проверяет @NotBlank, а собственный валидатор отвечает только за отсутствие цифр и не дублирует это правило."),
        ("Как выбирается обработчик исключения", "Spring учитывает область advice и тип исключения; локальный handler контроллера рассматривается раньше advice, а порядок advice и точность соответствия влияют на выбор между несколькими подходящими handler."),
        ("Почему введённый текст выводится через th:text", "th:text экранирует HTML. th:utext выводит непроверенный HTML, поэтому пользовательский ввод через него может привести к XSS."),
    ]
    for question, answer in qa:
        p = paragraph(doc, "")
        p.add_run(question + ". ").bold = True
        p.add_run(answer)

    output.parent.mkdir(parents=True, exist_ok=True)
    doc.save(output)
    print(json.dumps({"output": str(output), "screenshots": len(screenshots),
                      "listings": 13, "responses": len(responses)}, ensure_ascii=False))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--output", type=Path, default=REPORT_DIR / "Laboratory_work_6_report.docx")
    args = parser.parse_args()
    build(args.manifest.resolve(), args.output.resolve())
