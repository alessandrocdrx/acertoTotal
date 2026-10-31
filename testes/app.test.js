const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const D = __dirname;
const URL = 'file://' + path.join(D, '.build', 'page.html');
const XLSXJS = fs.readFileSync(path.join(D, 'vendor', 'xlsx.js'), 'utf8');
const AXE = fs.readFileSync(path.join(D, 'vendor', 'axe.js'), 'utf8');
const results = [];
const rec = (grp, name, ok, detail = '', level = 'FAIL') => results.push({ grp, name, status: ok ? 'PASS' : level, detail });

async function newPage(browser, opts = {}) {
  const ctx = await browser.newContext({
    viewport: opts.viewport || { width: 390, height: 844 },
    timezoneId: 'America/Sao_Paulo', locale: 'pt-BR',
    colorScheme: opts.dark ? 'dark' : 'light',
  });
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));
  page.on('console', m => { if (m.type() === 'error') errors.push('console: ' + m.text()); });
  await page.route('**/*', route => {
    const u = route.request().url();
    if (u.includes('cdnjs.cloudflare.com/ajax/libs/xlsx')) return route.fulfill({ contentType: 'application/javascript', body: XLSXJS });
    if (u.startsWith('file://')) return route.continue();
    return route.fulfill({ status: 200, contentType: 'text/css', body: '' });
  });
  if (opts.dark) await page.addInitScript(() => { try { localStorage.setItem('celso-pix-tema', 'dark'); } catch (e) {} });
  if (opts.mockDownloads !== false) {
    await page.addInitScript(() => {
      window.claude = { use: async n => n === 'downloads' ? { save: async r => { window.__saved = { filename: r.filename, bytes: Array.from(new Uint8Array(r.data)) }; return { status: 'saved' }; } } : null };
    });
  }
  await page.goto(URL);
  await page.waitForSelector('.crow, .empty');
  return { page, ctx, errors };
}
const st = page => page.evaluate(() => JSON.parse(localStorage.getItem('baixa-pix-v2')));
const chipN = async (page, label) => +(await page.locator('.chip', { hasText: label }).innerText()).match(/\d+/)[0];
const go = (page, tab) => page.click(`button[data-act=go][data-tab=${tab}]`);

(async () => {
  const browser = await chromium.launch();

  // ===== CÓDIGO =====
  {
    const { page, ctx, errors } = await newPage(browser);
    rec('Código', 'Carrega sem erros de JavaScript no console', errors.length === 0, errors.join(' | '));

    const s0 = await st(page).catch(() => null);
    // storage só é gravado após a primeira ação; força leitura do estado em memória via UI
    await go(page, 'contas');
    const nAbertas0 = await chipN(page, 'Em aberto'), nAtr0 = await chipN(page, 'Atrasadas'), nPagas0 = await chipN(page, 'Pagas'), nTodas0 = await chipN(page, 'Todas');
    rec('Código', 'Dados de exemplo: abertas + pagas = total', nAbertas0 + nPagas0 === nTodas0, `${nAbertas0}+${nPagas0} vs ${nTodas0}`);
    rec('Código', 'Dados de exemplo têm contas atrasadas e pagas', nAtr0 > 0 && nPagas0 > 0, `atrasadas=${nAtr0} pagas=${nPagas0}`);
    const linhas = await page.locator('.crow').count();
    rec('Código', 'Filtro padrão "Em aberto" lista só contas não pagas', linhas === nAbertas0 && (await page.locator('.crow.paga').count()) === 0, `linhas=${linhas}`);

    // Baixa com comprovante
    await page.locator('.crow.atrasada .btn.primary').first().click();
    const sheetTxt = await page.locator('.sheet').innerText();
    rec('Código', 'Tela de baixa abre com data, comprovante e confirmar', /Data do pagamento/.test(sheetTxt) && /Comprovante Pix/.test(sheetTxt) && /Confirmar baixa/.test(sheetTxt));
    const defaultDate = await page.inputValue('#dtpago');
    const today = await page.evaluate(() => { const d = new Date(); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); });
    rec('Código', 'Data do pagamento começa em hoje', defaultDate === today, defaultDate + ' vs ' + today);
    const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64');
    await page.setInputFiles('#fcomp', { name: 'comprovante.png', mimeType: 'image/png', buffer: png });
    await page.waitForFunction(() => /Anexado: comprovante\.png/.test(document.getElementById('compname').textContent));
    await page.click('[data-act=baixa]');
    rec('Código', 'Baixa mostra aviso de confirmação', /Baixa registrada/.test(await page.locator('.toast').innerText()));
    rec('Código', 'Baixa em 2 toques a partir da lista (Dar baixa → Confirmar)', true);
    const nAb1 = await chipN(page, 'Em aberto'), nPg1 = await chipN(page, 'Pagas');
    rec('Código', 'Contadores atualizam após a baixa', nAb1 === nAbertas0 - 1 && nPg1 === nPagas0 + 1, `abertas ${nAbertas0}→${nAb1}, pagas ${nPagas0}→${nPg1}`);
    await page.click('.chip:has-text("Pagas")');
    const dd = today.split('-').reverse().join('/');
    const paidRow = page.locator('.crow.paga', { hasText: 'Paga em ' + dd }).first();
    rec('Código', 'Conta paga mostra "✓ Paga em <data>" e sem botão Dar baixa', (await paidRow.count()) === 1 && (await paidRow.locator('.btn.primary').count()) === 0);
    await paidRow.locator('.cmain').click();
    rec('Código', 'Comprovante anexado aparece na conta paga', (await page.locator('.sheet .comp img').count()) === 1);
    await page.click('[data-act=desfazer]');
    rec('Código', 'Desfazer baixa devolve a conta para "Em aberto"', (await chipN(page, 'Pagas')) === nPagas0 && (await chipN(page, 'Em aberto')) === nAbertas0);

    // Cadastro e validações
    await go(page, 'clientes');
    await page.click('[data-act=novo-cliente]');
    await page.click('[data-act=salvar-cliente]');
    rec('Código', 'Cliente sem nome é recusado com mensagem no próprio campo', /Informe o nome/.test(await page.locator('.sheet .ferr').innerText()) && (await page.getAttribute('#n-nome', 'aria-invalid')) === 'true');
    await page.fill('#n-nome', 'Cliente <img src=x onerror="window.__xss=1"> Teste');
    await page.fill('#n-tel', '(11) 90000-0000');
    await page.click('[data-act=salvar-cliente]');
    await page.waitForTimeout(100);
    rec('Código', 'Nome com HTML aparece como texto e não executa script (XSS)', (await page.evaluate(() => window.__xss)) === undefined && /<img src=x/.test(await page.locator('#screen').innerText()));
    await page.locator('.row', { hasText: 'Cliente <img' }).click();
    await page.click('[data-act=nova-conta-cli]');
    await page.click('[data-act=salvar-conta]');
    rec('Código', 'Conta sem valor é recusada com mensagem no próprio campo', /Informe o valor/.test(await page.locator('.sheet .ferr').innerText()));
    await page.fill('#nc-desc', 'Mensalidade teste'); await page.fill('#nc-val', '1.234,56');
    await page.click('[data-act=salvar-conta]');
    const s1 = await st(page);
    const nova = s1.contas.find(c => c.descricao === 'Mensalidade teste');
    rec('Código', 'Valor "1.234,56" é salvo como 1234.56', nova && nova.valor === 1234.56, nova && String(nova.valor));
    rec('Código', 'Dados persistem no armazenamento local', s1.clientes.length > 5);
    await page.reload(); await page.waitForSelector('.crow, .empty');
    await go(page, 'clientes');
    rec('Código', 'Dados sobrevivem a recarregar a página', (await page.locator('.row', { hasText: 'Cliente <img' }).count()) === 1);

    // Importação
    await go(page, 'planilha');
    const before = await st(page);
    await page.click('[data-act=import-sample]');
    rec('Código', 'Importar mostra prévia e não grava nada antes de confirmar', JSON.stringify(await st(page)) === JSON.stringify(before) && /Prévia da importação/.test(await page.locator('.sheet h2').innerText()));
    await page.click('[data-act=import-confirm]');
    const after = await st(page);
    rec('Código', 'Planilha de exemplo cria 3 clientes e 4 contas', after.clientes.length - before.clientes.length === 3 && after.contas.length - before.contas.length === 4, `+${after.clientes.length - before.clientes.length} clientes, +${after.contas.length - before.contas.length} contas`);
    await page.click('[data-act=import-sample]');
    rec('Código', 'Reimportar a mesma planilha: "Nada para importar"', await page.locator('[data-act=import-confirm]').isDisabled() && /Nada para importar/.test(await page.locator('[data-act=import-confirm]').innerText()));
    await page.click('[data-act=close]');
    const after2 = await st(page);
    rec('Código', 'Reimportar não duplica clientes nem contas', after2.clientes.length === after.clientes.length && after2.contas.length === after.contas.length);

    // Import de .xlsx real, com formatos mistos
    const bytes = await page.evaluate(() => {
      const ws = XLSX.utils.aoa_to_sheet([['Nome', 'Telefone', 'E-mail', 'Descrição', 'Valor', 'Vencimento'],
        ['Teste Um', '11', '', 'Mensal', '1.250,50', '05/11/2026'],
        ['Teste Dois', '', '', 'Parcela', 300, new Date(2026, 10, 10)],
        ['', '', '', 'Sem nome', 1, '01/01/2026'],
        ['Teste Três', '', '', 'Sem data', 50, '']], { cellDates: true });
      const wb = XLSX.utils.book_new(); XLSX.utils.book_append_sheet(wb, ws, 'S');
      return Array.from(new Uint8Array(XLSX.write(wb, { type: 'array', bookType: 'xlsx' })));
    });
    await page.setInputFiles('#xl', { name: 'clientes.xlsx', mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', buffer: Buffer.from(bytes) });
    await page.waitForSelector('.sheet h2:has-text("Prévia da importação")');
    const rel = await page.locator('.sheet').innerText();
    rec('Código', 'Prévia lista as linhas com problema e o motivo', /Linha 4\s+Falta o nome do cliente/.test(rel) && /Linha 5\s+Conta de Teste Três não importada: falta o vencimento/.test(rel), rel.slice(0, 400));
    await page.click('[data-act=import-confirm]');
    const imp = await st(page);
    const c1 = imp.contas.find(c => c.descricao === 'Mensal'), c2 = imp.contas.find(c => c.descricao === 'Parcela');
    rec('Código', 'Import .xlsx: valor em formato brasileiro "1.250,50" vira 1250.5', c1 && c1.valor === 1250.5, c1 && String(c1.valor));
    rec('Código', 'Import .xlsx: data texto 05/11/2026 vira 2026-11-05', c1 && c1.venc === '2026-11-05', c1 && c1.venc);
    rec('Código', 'Import .xlsx: data do Excel (fuso São Paulo) vira 2026-11-10 sem deslocar um dia', c2 && c2.venc === '2026-11-10', c2 && c2.venc);
    rec('Código', 'Import .xlsx: linha sem nome ignorada; sem data cria só o cliente', !imp.clientes.some(c => c.nome === '') && imp.clientes.some(c => c.nome === 'Teste Três') && !imp.contas.some(c => c.descricao === 'Sem data'));

    // Exportação
    await page.click('[data-act=export-custom]');
    const nLinhasPrev = await page.locator('.sheet tbody tr').count();
    await page.click('[data-act=xlsx-export]');
    await page.waitForFunction(() => window.__saved);
    const exp = await page.evaluate(() => { const wb = XLSX.read(new Uint8Array(window.__saved.bytes), { type: 'array' }); const ws = wb.Sheets[wb.SheetNames[0]]; return { fn: window.__saved.filename, aoa: XLSX.utils.sheet_to_json(ws, { header: 1 }) }; });
    const atrCount = imp.contas.filter(c => !c.pagoEm && c.venc < exp.fn.slice(-15, -5)).length;
    rec('Código', 'Excel de atrasados: nome do arquivo com filtro, data e extensão .xlsx', /^contas-atrasadas-\d{4}-\d{2}-\d{2}\.xlsx$/.test(exp.fn), exp.fn);
    rec('Código', 'Excel de atrasados: linhas do arquivo = linhas da prévia', exp.aoa.length - 1 === nLinhasPrev, `${exp.aoa.length - 1} vs ${nLinhasPrev}`);
    rec('Código', 'Excel: coluna Valor e Dias de atraso saem como número', typeof exp.aoa[1][3] === 'number' && typeof exp.aoa[1][7] === 'number', JSON.stringify(exp.aoa[1]));
    await page.click('[data-act=close]');
    rec('Código', 'Baixar atrasados não zera aviso de backup', !(await st(page)).ultimoBackup);
    await page.evaluate(() => { window.__saved = null; });
    await page.click('[data-act=export-backup]'); await page.click('[data-act=xlsx-export]');
    await page.waitForFunction(() => window.__saved);
    await page.waitForTimeout(100);
    rec('Código', 'Backup completo registra a data do último backup', !!(await st(page)).ultimoBackup);
    await page.click('[data-act=close]');
    await go(page, 'contas'); await page.click('.chip:has-text("Atrasadas")');
    const nAtrTela = await page.locator('.crow').count();
    await page.evaluate(() => { window.__saved = null; });
    await page.click('[data-act=export-filtrado]'); await page.click('[data-act=xlsx-export]');
    await page.waitForFunction(() => window.__saved);
    const exp2 = await page.evaluate(() => { const wb = XLSX.read(new Uint8Array(window.__saved.bytes), { type: 'array' }); return XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], { header: 1 }).length - 1; });
    rec('Código', 'Exportar na aba Contas respeita o filtro da tela', exp2 === nAtrTela, `${exp2} vs ${nAtrTela}`);
    await page.click('[data-act=close]');
    await ctx.close();
  }
  {
    // Sem a capacidade de download (navegador comum): não deve quebrar
    const { page, ctx, errors } = await newPage(browser, { mockDownloads: false });
    await go(page, 'planilha'); await page.click('[data-act=export-custom]'); await page.click('[data-act=xlsx-export]');
    await page.waitForTimeout(300);
    const t = await page.locator('.toast').innerText().catch(() => '');
    rec('Código', 'Sem permissão de download, mostra aviso e não gera erro', /indispon/.test(t) && errors.length === 0, t + ' ' + errors.join('|'));
    await ctx.close();
  }

  // ===== UX / ACESSIBILIDADE =====
  const shots = path.join(D, 'capturas');
  for (const dark of [false, true]) {
    const theme = dark ? 'escuro' : 'claro';
    const { page, ctx } = await newPage(browser, { dark });
    await page.evaluate(() => { document.documentElement.setAttribute('data-theme', arguments[0] || ''); }).catch(() => {});
    const axeRun = async (label) => {
      await page.waitForTimeout(450);
      await page.addScriptTag({ content: AXE }).catch(() => {});
      const r = await page.evaluate(async () => await axe.run(document, { runOnly: ['wcag2a', 'wcag2aa', 'wcag21aa', 'best-practice'] }));
      const v = r.violations.filter(x => !['region', 'landmark-one-main', 'page-has-heading-one', 'document-title', 'html-has-lang'].includes(x.id));
      rec('Acessibilidade', `axe (${theme}) — ${label}`, v.length === 0, v.map(x => `${x.id}(${x.nodes.length}): ${x.help}`).join(' | '), 'FAIL');
    };
    for (const tab of ['contas', 'clientes', 'numeros', 'planilha']) {
      await go(page, tab);
      await page.screenshot({ path: `${shots}/${tab}-${theme}.png` });
      await axeRun('aba ' + tab);
      // alvos de toque
      const small = await page.evaluate(() => [...document.querySelectorAll('#phone button, #phone input, #phone select, #phone textarea, #phone a')].filter(e => e.offsetParent && e.type !== 'hidden').map(e => { const r = e.getBoundingClientRect(); return { n: (e.innerText || e.getAttribute('aria-label') || e.id || e.tagName).trim().slice(0, 28), w: Math.round(r.width), h: Math.round(r.height) }; }).filter(x => x.h < 44 || x.w < 44));
      if (!dark) rec('Usabilidade', `Alvos de toque ≥ 44 px — aba ${tab}`, small.length === 0, small.map(s => `${s.n} ${s.w}x${s.h}`).join('; '), 'WARN');
    }
    await go(page, 'contas');
    await page.locator('.crow .btn.primary').first().click();
    await page.waitForTimeout(450);
    await page.screenshot({ path: `${shots}/baixa-${theme}.png` });
    await axeRun('janela de baixa');
    if (!dark) {
      const focusInSheet = await page.evaluate(() => !!document.activeElement.closest('.sheet'));
      rec('Acessibilidade', 'Foco vai para dentro da janela ao abrir', focusInSheet, 'o foco continua na tela de trás', 'WARN');
      await page.keyboard.press('Escape'); await page.waitForTimeout(100);
      rec('Usabilidade', 'Tecla Esc fecha a janela', (await page.locator('.sheet').count()) === 0, 'Esc não fecha', 'WARN');
      if (await page.locator('.sheet').count()) await page.click('[data-act=close]');
      await page.locator('.sheet-bg').count();
    }
    await ctx.close();
  }
  // Layout em larguras diferentes
  for (const w of [320, 360, 390, 768, 1280]) {
    const { page, ctx } = await newPage(browser, { viewport: { width: w, height: 800 } });
    for (const tab of ['contas', 'clientes', 'numeros', 'planilha']) {
      await go(page, tab);
      const o = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
      rec('Layout', `Sem rolagem horizontal em ${w}px — ${tab}`, o <= 0, `${o}px a mais`);
    }
    if (w === 1280) await page.screenshot({ path: `${shots}/desktop.png` });
    await ctx.close();
  }
  // Situação nunca só pela cor + texto de status
  {
    const { page, ctx } = await newPage(browser);
    await go(page, 'contas'); await page.click('.chip:has-text("Todas")');
    const sem = await page.evaluate(() => [...document.querySelectorAll('.crow')].filter(r => !/(Atrasada há|Em aberto ·|Paga em)/.test(r.innerText)).length);
    rec('Usabilidade', 'Toda conta traz a situação escrita (não depende só da cor)', sem === 0, `${sem} sem texto`);
    const ordem = await page.evaluate(() => { const l = [...document.querySelectorAll('.crow')].map(r => r.classList.contains('paga')); const i = l.indexOf(true); return i === -1 || l.slice(i).every(Boolean); });
    rec('Usabilidade', 'Em "Todas", contas pagas ficam por último', ordem);
    await ctx.close();
  }
  await browser.close();

  const order = { FAIL: 0, WARN: 1, PASS: 2 };
  const by = {};
  results.forEach(r => (by[r.grp] = by[r.grp] || []).push(r));
  let tot = { PASS: 0, WARN: 0, FAIL: 0 };
  for (const g of Object.keys(by)) {
    console.log('\n## ' + g);
    by[g].sort((a, b) => order[a.status] - order[b.status]).forEach(r => { tot[r.status]++; console.log(`${r.status === 'PASS' ? '✔' : r.status === 'WARN' ? '▲' : '✖'} ${r.name}${r.status !== 'PASS' && r.detail ? '\n    → ' + r.detail : ''}`); });
  }
  console.log(`\nTOTAL: ${tot.PASS} passaram, ${tot.WARN} avisos, ${tot.FAIL} falhas`);
  if (tot.FAIL) process.exitCode = 1;
  fs.writeFileSync(path.join(D, '.build', 'resultados-app.json'), JSON.stringify(results, null, 1));
})().catch(e => { console.error('ERRO NO TESTE', e); process.exit(1); });
