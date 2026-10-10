// Capture a browser source view of the actual committed source files.
// The report uses these PNGs instead of editable code listings.
const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const { chromium } = require('C:/Users/Arman/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');

const root = path.resolve(__dirname, '../..');
const evidence = path.join(__dirname, 'evidence');
const output = path.join(evidence, 'code');
const sources = [
  ['BookForm.java', 'Book form and validation constraints'],
  ['Genre.java', 'Available book genres'],
  ['form.html', 'Thymeleaf form with binding and validation messages'],
  ['post', 'POST handler with BindingResult and Post Redirect Get', 'BookPageController.java', '@PostMapping'],
  ['NoDigits.java', 'Custom NoDigits constraint annotation'],
  ['NoDigitsValidator.java', 'NoDigits validator implementation'],
  ['LocaleConfiguration.java', 'Session locale and language interceptor'],
  ['PageExceptionHandler.java', 'HTML advice and shared libraryName model attribute'],
  ['ApiExceptionHandler.java', 'REST advice with a ProblemDetail response'],
  ['messages.properties', 'English labels and validation messages'],
  ['messages_ru.properties', 'Russian labels and validation messages'],
  ['Book.java', 'Saved book record'],
  ['get_new', 'GET handler that initializes the new book form', 'BookPageController.java', '@GetMapping("/new")'],
];

async function findSource(name, dir = path.join(root, 'src/main')) {
  const found = [];
  for (const entry of await fs.readdir(dir, { withFileTypes: true })) {
    const item = path.join(dir, entry.name);
    if (entry.isDirectory()) found.push(...await findSource(name, item));
    else if (entry.name === name) found.push(item);
  }
  return found;
}

function escapeHtml(text) {
  return text.replaceAll('&', '&amp;').replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;').replaceAll('"', '&quot;');
}

function javaLine(text) {
  const token = /("(?:\\.|[^"\\])*"|\/\/.*|@[A-Za-z]+|\b(?:package|import|public|private|protected|class|interface|record|enum|return|if|else|new|this|void|boolean|long|int|implements|static|final|null|true|false)\b|\b\d+\b)/g;
  let result = '', cursor = 0;
  for (const match of text.matchAll(token)) {
    result += escapeHtml(text.slice(cursor, match.index));
    const value = match[0];
    const style = value.startsWith('"') ? 'string' : value.startsWith('//') ? 'comment' : value.startsWith('@') ? 'annotation' : /^\d/.test(value) ? 'number' : 'keyword';
    result += `<span class="${style}">${escapeHtml(value)}</span>`;
    cursor = match.index + value.length;
  }
  return result + escapeHtml(text.slice(cursor));
}

function codeLine(text, fileName) {
  if (fileName.endsWith('.java')) return javaLine(text);
  if (fileName.endsWith('.properties')) {
    if (text.trim().startsWith('#')) return `<span class="comment">${escapeHtml(text)}</span>`;
    const split = text.indexOf('=');
    if (split >= 0) return `<span class="property">${escapeHtml(text.slice(0, split))}</span>=<span class="string">${escapeHtml(text.slice(split + 1))}</span>`;
  }
  return escapeHtml(text);
}

function methodRange(lines, annotation) {
  const start = lines.findIndex(line => line.trim() === annotation);
  assert(start >= 0, `Cannot find ${annotation}`);
  let depth = 0, opened = false;
  for (let end = start; end < lines.length; end++) {
    for (const char of lines[end]) {
      if (char === '{') { depth++; opened = true; }
      if (char === '}') depth--;
    }
    if (opened && depth === 0) return { start, end: end + 1 };
  }
  throw new Error(`Unclosed method ${annotation}`);
}

(async () => {
  await fs.mkdir(output, { recursive: true });
  const browser = await chromium.launch({
    executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe',
    headless: true,
  });
  const page = await browser.newPage({ viewport: { width: 1340, height: 1300 }, deviceScaleFactor: 1.5 });
  const manifest = {};
  let figure = 0;
  try {
    for (const [key, title, override, annotation] of sources) {
      const fileName = override || key;
      const matches = await findSource(fileName);
      assert.equal(matches.length, 1, `Expected one ${fileName}`);
      const sourceFile = matches[0];
      const original = await fs.readFile(sourceFile, 'utf8');
      const lines = original.replace(/^\uFEFF/, '').replaceAll('\r\n', '\n').replace(/\n$/, '').split('\n');
      const range = annotation ? methodRange(lines, annotation) : { start: 0, end: lines.length };
      const rel = path.relative(root, sourceFile).replaceAll('\\', '/');
      const hash = crypto.createHash('sha256').update(original).digest('hex');
      manifest[key] = [];
      // Split long files at source-line boundaries; preserve every original line.
      const count = range.end - range.start;
      const parts = Math.ceil(count / 27);
      const perPart = Math.ceil(count / parts);
      for (let start = range.start, part = 1; start < range.end; start += perPart, part++) {
        const end = Math.min(start + perPart, range.end);
        const code = lines.slice(start, end).map((line, index) => `<div class="line"><span class="ln">${start + index + 1}</span><code>${codeLine(line, fileName) || ' '}</code></div>`).join('');
        await page.setContent(`<!doctype html><html lang="en"><head><meta charset="UTF-8"><title>${escapeHtml(fileName)}</title><style>
          *{box-sizing:border-box}body{margin:0;padding:20px;background:#edf0f4;color:#17202a}
          #source{width:1300px;background:#fff;border:1px solid #ccd3dc;border-radius:5px;overflow:hidden}
          header{padding:16px 22px 12px;background:#f7f9fc;border-bottom:1px solid #d9e0e8;font-family:Arial,sans-serif}
          h1{font-size:23px;margin:0 0 8px;font-weight:600}header p{font-size:17px;margin:0;line-height:1.4;overflow-wrap:anywhere;color:#475569}
          main{padding:14px 16px 16px 8px}.line{display:grid;grid-template-columns:58px 1fr;font:21.5px/1.32 Consolas,'Courier New',monospace;min-height:28.4px}
          .ln{text-align:right;padding-right:16px;color:#6d7989;user-select:none;border-right:1px solid #e1e6ec}
          code{padding-left:16px;white-space:pre-wrap;overflow-wrap:anywhere;tab-size:4;font:inherit}
          .keyword{color:#7c2693}.string{color:#125d2b}.annotation{color:#6c4b0a}.comment{color:#586c5c}.number{color:#075e91}.property{color:#175ca0}
        </style></head><body><article id="source"><header><h1>${escapeHtml(fileName)} · lines ${start + 1}–${end}</h1><p>${escapeHtml(rel)}</p></header><main>${code}</main></article></body></html>`);
        await page.evaluate(() => document.fonts.ready);
        const sourceText = await page.locator('code').allTextContents();
        assert.deepEqual(sourceText.map(line => line === ' ' ? '' : line), lines.slice(start, end));
        const size = await page.locator('#source').boundingBox();
        assert(size.height <= 1280, `Source section too tall: ${fileName}, ${size.height}`);
        assert.equal(await page.locator('#source').evaluate(el => el.scrollWidth > el.clientWidth), false);
        const name = `${String(++figure).padStart(2, '0')}-${key.replaceAll('.', '-').replaceAll('_', '-')}-${part}.png`;
        await page.locator('#source').screenshot({ path: path.join(output, name) });
        manifest[key].push({
          file: 'code/' + name,
          caption: title + (parts > 1 ? ` part ${part} of ${parts}` : ''),
          source: rel, start_line: start + 1, end_line: end, source_sha256: hash,
        });
      }
    }
    await fs.writeFile(path.join(evidence, 'code_manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
    console.log(JSON.stringify({ screenshots: figure, groups: Object.keys(manifest).length, manifest: path.join(evidence, 'code_manifest.json') }));
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
