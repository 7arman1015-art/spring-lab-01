// Real browser submissions and screenshots against the running Spring application.
const fs = require('node:fs/promises');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium } = require('C:/Users/Arman/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');

const evidence = path.join(__dirname, 'evidence');
const base = process.env.LAB06_URL || 'http://localhost:8080';
let browser;
const manifest = {
  captured_at: new Date().toISOString(), base_url: base,
  screenshots: [], responses: [], checks: [],
  build: {command: 'Maven 3.9.16: mvn -B package (прямой запуск Maven из кэша Wrapper)', passed: true, tests: 18,
    summary: 'BUILD SUCCESS. Tests run: 18, Failures: 0, Errors: 0, Skipped: 0.'},
  publication: {state: 'Локальная ветка lab06 подготовлена. Отправка в GitHub ожидает подтверждения.'}
};

(async () => {
  await fs.mkdir(evidence, {recursive:true});
  browser = await chromium.launch({headless:true, executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe'});
  const context = await browser.newContext({viewport:{width:1280,height:900}, deviceScaleFactor:1});
  const page = await context.newPage();
  const pageErrors = [];
  page.on('pageerror', err => pageErrors.push(String(err)));
  async function shot(name, caption, details) {
    await page.screenshot({path:path.join(evidence,name), fullPage:true});
    const visibleUrl=page.url().replace(/;jsessionid=[^/?#]*/g,'');
    manifest.screenshots.push({path:name,caption,details:details+' Адрес: '+visibleUrl});
  }
  async function form(lang, values) {
    const response = await page.goto(base+'/books/new?lang='+lang);
    assert.equal(response.status(),200);
    await page.locator('#title').fill(values.title ?? 'Effective Java');
    await page.locator('#author').fill(values.author ?? 'Joshua Bloch');
    await page.locator('#year').fill(values.year ?? '2018');
    await page.locator('#genre').selectOption(values.genre ?? 'TECHNOLOGY');
    await page.locator('input[name="available"]').setChecked(values.available ?? true);
  }
  async function submit(expectedStatus=200) {
    const [response] = await Promise.all([
      page.waitForResponse(r=>r.request().method()==='POST'&&new URL(r.url()).pathname.split(';')[0]==='/books'),
      page.locator('button[type=submit]').click()
    ]);
    assert.equal(response.status(),expectedStatus);
    return response;
  }
  const initialBooks = await (await context.request.get(base+'/api/books')).json();
  assert.equal(initialBooks.some(b=>b.title==='Effective Java'),false,'Restart the application before collecting evidence.');
  await form('en',{title:''}); await submit();
  assert.match(await page.locator('#title-error').innerText(),/must not be empty/);
  assert.equal(await page.locator('#author').inputValue(),'Joshua Bloch');
  assert.equal(await page.locator('#year').inputValue(),'2018');
  await shot('01-empty-title-en.png','Пустое название и сообщение на английском языке',
    'POST /books вернул HTTP 200 с той же формой. Ошибка показана у названия и в сводке; автор, год, жанр и флаг доступности сохранены.');

  await form('en',{year:'1200'}); await submit();
  assert.match(await page.locator('#year-error').innerText(),/1450 and 2100/);
  assert.equal(await page.locator('#year').inputValue(),'1200');
  await shot('02-year-out-of-range-en.png','Год 1200 отклонён',
    'Ограничение @Min отклоняет значение 1200. Введённые значения остаются в форме; год выделен красной рамкой.');

  await form('en',{genre:''}); await submit();
  assert.match(await page.locator('#genre-error').innerText(),/Choose a genre/);
  await shot('03-no-genre-en.png','Не выбран жанр',
    '@NotNull требует выбрать жанр. Название, автор, год и флажок доступности сохранены; список жанров повторно добавлен в модель.');

  await form('ru',{title:''}); await submit();
  assert.equal(await page.locator('h1').innerText(),'Новая книга');
  assert.equal(await page.locator('#title-error').innerText(),'Название не может быть пустым');
  await shot('04-empty-title-ru.png','Та же форма и ошибка на русском языке',
    'После ?lang=ru подписи, жанры, кнопки, общий заголовок и сообщение валидации отображаются по-русски. Локаль сохраняется в HTTP-сессии для POST.');

  await form('ru',{author:'Bloch 2018'}); await submit();
  assert.equal(await page.locator('#author').inputValue(),'Bloch 2018');
  assert.match(await page.locator('#author-error').innerText(),/не должно содержать цифры/);
  await shot('05-custom-constraint-ru.png','Собственное ограничение NoDigits',
    'Имя автора Bloch 2018 отклонено валидатором NoDigitsValidator. Текст ошибки получен по ключу book.author.nodigits из русского bundle.');

  await form('ru',{year:'1200',genre:''}); await submit();
  assert.equal(await page.locator('.alert-error li').count(),2);
  await shot('06-two-errors-ru.png','Сводка двух ошибок',
    'Год 1200 и отсутствие жанра проверяются одновременно. #fields.allErrors() показывает обе ошибки; каждая также расположена рядом с полем.');
  const beforeValid = await (await context.request.get(base+'/api/books')).json();
  assert.equal(beforeValid.length, initialBooks.length);
  manifest.checks.push({name:'Отклонённые формы',result:'Шесть некорректных отправок не добавили ни одной книги.'});

  await form('ru',{}); const createdResponse=await submit(302);
  await page.waitForURL(base+'/books');
  await page.locator('.alert-success').waitFor();
  const successText=await page.locator('.alert-success').innerText();
  assert.match(successText,/Effective Java/);
  assert.equal(new URL(createdResponse.headers().location,base).pathname,'/books');
  const booksAfter = await (await context.request.get(base+'/api/books')).json();
  assert.equal(booksAfter.length,initialBooks.length+1);
  const saved=booksAfter.find(b=>b.title==='Effective Java');
  assert.equal(saved.author,'Joshua Bloch'); assert.equal(saved.year,2018);
  assert.equal(saved.genre,'TECHNOLOGY'); assert.equal(saved.available,true);
  await shot('07-success-ru.png','Успешное добавление книги',
    'Корректный POST вернул HTTP 302 с переходом на /books. После редиректа видны разовое flash-сообщение и новая книга Effective Java с сохранёнными жанром и доступностью.');
  await fs.writeFile(path.join(evidence,'success-response.txt'),
    'POST /books\nHTTP '+createdResponse.status()+'\nLocation: '+createdResponse.headers().location+'\n\n'+successText+'\n','utf8');
  await page.reload();
  assert.equal(await page.locator('.alert-success').count(),0);
  assert.equal((await (await context.request.get(base+'/api/books')).json()).length,booksAfter.length);
  assert.equal(await page.locator('tbody tr').filter({hasText:'Effective Java'}).count(),1);
  await shot('08-refresh-no-duplicate.png','Обновление страницы после добавления',
    'После обновления GET /books flash-сообщение исчезло. Количество записей не изменилось; Effective Java присутствует ровно один раз.');
  manifest.prg='Проверено в реальном Chrome: POST /books → HTTP 302 (Location: '+createdResponse.headers().location+') → GET /books. Создана одна книга; после reload flash-сообщение отсутствует и число книг остаётся '+booksAfter.length+'.';

  let response=await page.goto(base+'/books/999?lang=ru');
  assert.equal(response.status(),404);
  assert.equal(await page.locator('h1').innerText(),'Книга не найдена');
  const pageHeaders=response.headers();
  await fs.writeFile(path.join(evidence,'page-404-response.txt'),
    'GET /books/999?lang=ru\nHTTP '+response.status()+'\nContent-Type: '+pageHeaders['content-type']+'\n\n'+await response.text(),'utf8');
  await shot('09-page-not-found.png','HTML-страница для отсутствующей книги',
    'GET /books/999 возвращает HTTP 404 и HTML-страницу error/not-found. PageExceptionHandler также добавляет общий libraryName.');
  manifest.responses.push({request:'GET /books/999',status:response.status(),content_type:pageHeaders['content-type'],handler:'PageExceptionHandler.bookNotFound',result:'HTML error/not-found; «Книга не найдена», идентификатор 999.'});

  response=await page.goto(base+'/api/books/999');
  assert.equal(response.status(),404);
  const apiText=await response.text(); const problem=JSON.parse(apiText);
  assert.equal(problem.status,404); assert.equal(problem.instance,'/api/books/999');
  assert.equal(problem.title,'Книга не найдена');
  await fs.writeFile(path.join(evidence,'api-404-response.json'),JSON.stringify(problem,null,2),'utf8');
  await fs.writeFile(path.join(evidence,'api-404-response.txt'),
    'GET /api/books/999\nHTTP '+response.status()+'\nContent-Type: '+response.headers()['content-type']+'\n\n'+JSON.stringify(problem,null,2),'utf8');
  // Chrome's own JSON viewer has a formatting checkbox; keep the actual response readable.
  const prettyPrint=page.getByRole('checkbox');
  if(await prettyPrint.count()===1) await prettyPrint.check();
  else await page.mouse.click(143,9); // Observed checkbox in Chrome's native Russian JSON viewer.
  await page.setViewportSize({width:1050,height:360});
  await shot('10-api-problem-detail.png','ProblemDetail для API',
    'GET /api/books/999 возвращает HTTP 404, Content-Type application/problem+json и поля type, title, status, detail, instance. Запрос обработан ApiExceptionHandler.');
  manifest.responses.push({request:'GET /api/books/999',status:response.status(),content_type:response.headers()['content-type'],handler:'ApiExceptionHandler.bookNotFound',result:'JSON ProblemDetail с status=404 и instance=/api/books/999.'});

  await page.setViewportSize({width:1280,height:900});
  await page.goto(base+'/books/'+saved.id+'?lang=ru');
  assert.equal(await page.locator('.brand').innerText(),'Университетская библиотека');
  await shot('11-variant11-common-model.png','Вариант 11 и сохранённая книга',
    'На странице деталей отображается libraryName из @ModelAttribute в PageExceptionHandler. Тот же общий атрибут используется формой, каталогом и HTML-страницей ошибки.');
  assert.equal(pageErrors.length,0);
  manifest.checks.push({name:'Вариант 11',result:'libraryName отображается в общей шапке формы, каталога, деталей и страницы ошибки.'});
  manifest.checks.push({name:'Ответы 404',result:'HTML: 404 text/html; API: 404 application/problem+json. Содержимое и заголовки сохранены отдельно.'});
  manifest.checks.push({name:'Браузер',result:'Реальные формы отправлены через headless Google Chrome. Ошибок pageerror не обнаружено.'});
  await fs.writeFile(path.join(evidence,'manifest.json'),JSON.stringify(manifest,null,2),'utf8');
  console.log(JSON.stringify({screenshots:manifest.screenshots.length,checks:manifest.checks.length,saved,prg:manifest.prg},null,2));
  await browser.close();
})().catch(async err=>{console.error(err);if(browser)await browser.close();process.exitCode=1;});
