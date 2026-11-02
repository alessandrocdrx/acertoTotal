// Experiência do cliente: percorre TODAS as telas e janelas, em celular normal e pequeno, claro e escuro,
// e confere o que a revisão por foto encontrou: toque, texto cortado ou quebrado, sobreposição, contraste,
// títulos, filtros, ação principal visível e acesso ao comprovante. Fotos em testes/capturas/experiencia/.
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const AXE = fs.readFileSync(path.join(__dirname, 'vendor', 'axe.js'), 'utf8');
const FOTOS = path.join(__dirname, 'capturas', 'experiencia');
fs.mkdirSync(FOTOS, { recursive: true });
const out = [];
const ok = (nome, cond, det = '') => out.push(`${cond ? '✔' : '✖'} ${nome}${cond ? '' : ' → ' + det}`);

// Telas e janelas: [nome, como abrir]. Cada uma começa do zero (sem janela, na aba Contas).
const TELAS = [
  ['Contas', async p => {}],
  ['Contas · atrasadas', async p => p.click('[data-act=filter][data-f=atrasadas]')],
  ['Contas · pagas', async p => p.click('[data-act=filter][data-f=pagas]')],
  ['Dar baixa', async p => p.click('.crow .btn.primary')],
  ['Conta paga com comprovante', async p => { await p.click('[data-act=filter][data-f=pagas]'); await p.click(`.cmain[data-id="${p.compId}"]`); }],
  ['Nova conta', async p => p.click('.fab')],
  ['Clientes', async p => p.click('.tabs [data-tab=clientes]')],
  ['Ficha do cliente', async p => { await p.click('.tabs [data-tab=clientes]'); await p.click('#screen .row'); }],
  ['Novo cliente', async p => { await p.click('.tabs [data-tab=clientes]'); await p.click('.fab'); }],
  ['Números', async p => p.click('.tabs [data-tab=numeros]')],
  ['Planilha', async p => p.click('.tabs [data-tab=planilha]')],
  ['Importar', async p => { await p.click('.tabs [data-tab=planilha]'); await p.click('#screen [data-act=import-abrir]'); }],
  ['Atualizar pelo Excel', async p => { await p.click('.tabs [data-tab=planilha]'); await p.click('#screen [data-act=atualizar-abrir]'); }],
  ['Exportar · filtros', async p => { await p.click('.tabs [data-tab=planilha]'); await p.click('#screen [data-act=export-abrir]'); }],
  ['Exportar · prévia', async p => { await p.click('.tabs [data-tab=planilha]'); await p.click('#screen [data-act=export-abrir]'); await p.click('[data-act=export-custom]'); }],
  ['Cópia de segurança', async p => { await p.click('.tabs [data-tab=planilha]'); await p.click('#screen [data-act=export-backup]'); }],
  ['Menu', async p => p.click('[data-act=abrir-menu]')],
  ['Planos', async p => { await p.click('[data-act=abrir-menu]'); await p.click('.assin [data-act=planos]'); }],
  ['Tema e paleta', async p => { await p.click('[data-act=abrir-menu]'); await p.click('#screen [data-act=aparencia]'); }],
  ['Sobre', async p => { await p.click('[data-act=abrir-menu]'); await p.click('.menu-marca'); }],
  ['Apagar tudo', async p => { await p.click('[data-act=abrir-menu]'); await p.click('[data-act=apagar-abrir]'); }],
];

// Medições feitas dentro da página, na tela aberta no momento.
const medir = () => {
  const vis = e => { const r = e.getBoundingClientRect(); const st = getComputedStyle(e); return r.width > 0 && r.height > 0 && st.visibility !== 'hidden' && !e.closest('[hidden]'); };
  const raiz = document.querySelector('#layer .sheet') || document.getElementById('phone');
  const nome = e => (e.innerText || e.getAttribute('aria-label') || e.id || e.tagName).trim().replace(/\s+/g, ' ').slice(0, 30);
  // 1) Toque: botões, links, campos e chips com pelo menos 48 px (Material 3). Links dentro de texto corrido ficam de fora.
  const toque = [...raiz.querySelectorAll('button, a, input:not([type=file]):not([type=checkbox]):not([type=radio]), select, textarea, summary, .toggle')]
    .filter(vis).filter(e => !(e.tagName === 'A' && e.closest('p, .sobre, .warn')))
    .map(e => ({ n: nome(e), r: e.getBoundingClientRect() }))
    .filter(x => x.r.height < 47.5 || x.r.width < 47.5).map(x => `${x.n} ${Math.round(x.r.width)}x${Math.round(x.r.height)}`);
  // 2) Texto cortado: elemento que esconde o excesso e tem mais conteúdo do que largura.
  const cortado = [...raiz.querySelectorAll('*')].filter(vis).filter(e => {
    const st = getComputedStyle(e); if (e.closest('.tbl') || /INPUT|SELECT|TEXTAREA|svg|path|IMG/i.test(e.tagName)) return false;
    return (st.overflowX === 'hidden' || st.textOverflow === 'ellipsis') && e.scrollWidth > e.clientWidth + 1 && e.innerText.trim();
  }).map(nome);
  // 3) Sobreposição dentro de cada cartão ou linha: textos, selos, valores e botões não podem se encostar.
  const sob = [];
  raiz.querySelectorAll('.crow, .row, .opcao, .plano, .menu-marca, .card.assin').forEach(cx => {
    if (!vis(cx)) return;
    const it = [...cx.querySelectorAll('.t, .s, .pill, .v, .btn, .pv, .pn, .ot b, .chev')].filter(vis);
    for (let i = 0; i < it.length; i++) for (let j = i + 1; j < it.length; j++) {
      if (it[i].contains(it[j]) || it[j].contains(it[i])) continue;
      const a = it[i].getBoundingClientRect(), b = it[j].getBoundingClientRect();
      const x = Math.min(a.right, b.right) - Math.max(a.left, b.left), y = Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top);
      if (x > 1 && y > 1) sob.push(`${nome(it[i])} × ${nome(it[j])}`);
    }
  });
  // 4) Letra pequena: nada abaixo de 11 px (Material 3: menor rótulo é 11 sp).
  const pequena = [...raiz.querySelectorAll('*')].filter(vis).filter(e => [...e.childNodes].some(n => n.nodeType === 3 && n.textContent.trim()) && parseFloat(getComputedStyle(e).fontSize) < 11).map(e => `${nome(e)} ${getComputedStyle(e).fontSize}`);
  // 5) Pedaços que não podem quebrar no meio (data do vencimento, "7 contas").
  const quebra = [...raiz.querySelectorAll('.nw')].filter(vis).filter(e => e.getClientRects().length > 1).map(nome);
  // 6) Rolagem lateral da página.
  const lateral = document.documentElement.scrollWidth - document.documentElement.clientWidth;
  // 7) Janela tem saída visível (Fechar, Cancelar, Agora não, Voltar).
  const sheet = document.querySelector('#layer .sheet');
  const saida = !sheet || [...sheet.querySelectorAll('button')].some(b => /^(Fechar|Cancelar|Agora não|Voltar aos filtros)$/.test(b.innerText.trim()));
  return { toque, cortado, sob: [...new Set(sob)], pequena: [...new Set(pequena)], quebra, lateral, saida };
};

(async () => {
  const b = await chromium.launch();
  const abrir = async (w, h, tema) => {
    const ctx = await b.newContext({ viewport: { width: w, height: h }, timezoneId: 'America/Sao_Paulo', locale: 'pt-BR', colorScheme: tema });
    const p = await ctx.newPage(); p.setDefaultTimeout(6000); p.errs = [];
    p.on('pageerror', e => p.errs.push(e.message));
    await p.addInitScript(() => {
      window.claude = { use: async n => n === 'downloads' ? { save: async r => { window.__salvo = { nome: r.filename, bytes: Array.from(new Uint8Array(r.data)) }; return { status: 'saved' }; } } : null };
    });
    await p.route('**/*', r => { const u = r.request().url(); if (u.includes('xlsx')) return r.fulfill({ contentType: 'application/javascript', body: fs.readFileSync(path.join(__dirname, 'vendor', 'xlsx.js'), 'utf8') }); if (u.startsWith('file://')) return r.continue(); return r.fulfill({ status: 200, body: '' }); });
    await p.goto('file://' + path.join(__dirname, '.build', 'page.html'));
    await p.waitForSelector('.crow');
    // Dá baixa numa conta anexando uma foto de verdade, pelo caminho que a pessoa usa.
    const jpg = await p.evaluate(() => { const cv = document.createElement('canvas'); cv.width = 40; cv.height = 30; const g = cv.getContext('2d'); g.fillStyle = '#00766c'; g.fillRect(0, 0, 40, 30); return cv.toDataURL('image/jpeg', .8).split(',')[1]; });
    p.compId = await p.locator('.crow .btn.primary').first().getAttribute('data-id');
    await p.click('.crow .btn.primary');
    await p.setInputFiles('#fcomp', { name: 'pix.jpg', mimeType: 'image/jpeg', buffer: Buffer.from(jpg, 'base64') });
    await p.waitForFunction(() => /Anexado/.test(document.getElementById('compname').textContent));
    await p.click('[data-act=baixa]'); await p.waitForTimeout(200);
    if (tema === 'dark') { await p.evaluate(() => localStorage.setItem('acerto-total-tema', 'dark')); await p.reload(); await p.waitForSelector('.crow'); }
    return { p, ctx };
  };
  const inicio = async p => { await p.evaluate(() => { while (window.acertoVoltar && window.acertoVoltar()); document.getElementById('screen').scrollTop = 0; }); await p.click('.tabs [data-tab=contas]'); await p.click('[data-act=filter][data-f=abertas]'); };

  for (const [w, h, tema] of [[360, 780, 'light'], [320, 640, 'light'], [360, 780, 'dark']]) {
    const { p, ctx } = await abrir(w, h, tema);
    const tag = `${w}×${h} ${tema === 'dark' ? 'escuro' : 'claro'}`;
    const prob = { toque: [], cortado: [], sob: [], pequena: [], quebra: [], lateral: [], saida: [], axe: [] };
    for (const [nome, ir] of TELAS) {
      try { await inicio(p); await ir(p); } catch (e) { prob.saida.push(`${nome}: não abriu (${e.message.split('\n')[0]})`); continue; }
      await p.waitForTimeout(450);
      await p.screenshot({ path: path.join(FOTOS, `${w}x${h}-${tema}-${nome.replace(/[^\wÀ-ú]+/g, '-')}.png`) });
      const m = await p.evaluate(medir);
      for (const k of ['toque', 'cortado', 'sob', 'pequena', 'quebra']) m[k].forEach(x => prob[k].push(`${nome}: ${x}`));
      if (m.lateral > 0) prob.lateral.push(`${nome}: ${m.lateral}px`);
      if (!m.saida) prob.saida.push(`${nome}: sem Fechar/Cancelar`);
      if (w === 360) { // contraste pelo axe em cada tela e janela, nos dois temas
        await p.addScriptTag({ content: AXE });
        const v = await p.evaluate(async () => (await axe.run(document.querySelector('#layer .sheet') || document.getElementById('phone'), { runOnly: ['color-contrast'] })).violations.map(x => x.nodes.map(n => n.target.join(' ')).join(', ')));
        v.forEach(x => prob.axe.push(`${nome}: ${x}`));
      }
    }
    ok(`[${tag}] Toque de 48 px em todos os botões, campos e links das ${TELAS.length} telas`, !prob.toque.length, prob.toque.slice(0, 8).join(' | '));
    ok(`[${tag}] Nenhum texto cortado`, !prob.cortado.length, prob.cortado.slice(0, 8).join(' | '));
    ok(`[${tag}] Nada se sobrepõe dentro de cartões e linhas (selo × botão, valor × texto)`, !prob.sob.length, prob.sob.slice(0, 8).join(' | '));
    ok(`[${tag}] Nenhuma letra abaixo de 11 px`, !prob.pequena.length, prob.pequena.slice(0, 8).join(' | '));
    ok(`[${tag}] Datas e contagens não quebram no meio ("venc. 23/10/2026", "7 contas")`, !prob.quebra.length, prob.quebra.slice(0, 8).join(' | '));
    ok(`[${tag}] Sem rolagem lateral`, !prob.lateral.length, prob.lateral.join(' | '));
    ok(`[${tag}] Toda janela abre e tem saída (Fechar, Cancelar ou Agora não)`, !prob.saida.length, prob.saida.join(' | '));
    if (w === 360) ok(`[${tag}] Contraste mínimo (axe) em todas as telas e janelas`, !prob.axe.length, prob.axe.slice(0, 6).join(' | '));
    ok(`[${tag}] Sem erros de JavaScript`, !p.errs.length, p.errs.join(' | '));
    await ctx.close();
  }

  // Comportamentos que a revisão pediu, no celular normal
  const { p, ctx } = await abrir(360, 780, 'light');
  // Títulos iguais ao rótulo da aba (a pessoa toca "Números" e vê "Números")
  for (const t of ['contas', 'clientes', 'numeros', 'planilha']) {
    await p.click(`.tabs [data-tab=${t}]`);
    const rot = (await p.locator(`.tabs [data-tab=${t}]`).innerText()).trim(), tit = (await p.locator('#title').innerText()).trim();
    ok(`Título da aba ${rot} é "${rot}"`, rot === tit, tit);
  }
  // Períodos dos Números numa linha só, com nome completo para o leitor de tela
  await p.click('.tabs [data-tab=numeros]');
  ok('Números: os 3 períodos cabem numa linha só', await p.evaluate(() => new Set([...document.querySelectorAll('[data-act=periodo]')].map(e => Math.round(e.getBoundingClientRect().top))).size === 1));
  ok('Números: período tem nome completo para o leitor de tela ("Últimos 3 meses")', (await p.getAttribute('[data-p="3"]', 'aria-label')) === 'Últimos 3 meses');
  // Filtros: o número no botão é o número de contas na lista
  await p.click('.tabs [data-tab=contas]');
  for (const f of ['abertas', 'atrasadas', 'pagas', 'todas']) {
    await p.click(`[data-act=filter][data-f=${f}]`);
    const n = +(await p.locator(`[data-act=filter][data-f=${f}] b`).innerText()), l = await p.locator('#screen .crow').count();
    ok(`Filtro "${f}": o número do botão (${n}) é o tamanho da lista (${l})`, n === l);
  }
  // Ficha do cliente: telefone e e-mail em linhas separadas
  await p.click('.tabs [data-tab=clientes]'); await p.click('#screen .row');
  ok('Ficha do cliente: telefone e e-mail em linhas separadas', await p.evaluate(() => { const s = document.querySelector('.sheet .sub'); return !/\d · \S+@/.test(s.innerText) && s.innerHTML.split('<br>').length >= 3; }));
  await p.keyboard.press('Escape');
  // Exportar e cópia de segurança: o botão Baixar aparece sem rolar, mesmo com 33 linhas de prévia
  await p.click('.tabs [data-tab=planilha]'); await p.click('#screen [data-act=export-backup]'); await p.waitForTimeout(300);
  const visivel = sel => p.evaluate(s => { const e = document.querySelector(s); if (!e) return false; const r = e.getBoundingClientRect(); return r.top >= 0 && r.bottom <= innerHeight; }, sel);
  ok('Cópia de segurança: "Baixar Excel" aparece sem rolar, antes da prévia', await visivel('.sheet [data-act=xlsx-export]'));
  ok('Cópia de segurança: avisa que os comprovantes não vão na planilha e como salvá-los', /comprovante não vão na planilha/.test(await p.locator('#aviso-comp').innerText()));
  await p.keyboard.press('Escape');
  // Comprovante: dá para salvar no celular (foto vira .jpg na pasta Downloads)
  await p.click('.tabs [data-tab=contas]'); await p.click('[data-act=filter][data-f=pagas]'); await p.click(`.cmain[data-id="${p.compId}"]`);
  ok('Conta paga com comprovante: mostra a foto e o botão "Salvar comprovante no celular"', (await p.locator('.sheet .comp img').count()) === 1 && (await p.locator('.sheet [data-act=salvar-comp]').count()) === 1);
  await p.click('.sheet [data-act=salvar-comp]');
  await p.waitForFunction(() => window.__salvo);
  const salvo = await p.evaluate(() => window.__salvo);
  ok('Salvar comprovante: arquivo .jpg com o nome do cliente e a data, e bytes de JPEG', /^comprovante-[a-z0-9-]+-\d{4}-\d{2}-\d{2}\.jpg$/.test(salvo.nome) && salvo.bytes[0] === 0xFF && salvo.bytes[1] === 0xD8, salvo.nome);
  ok('Salvar comprovante: avisa onde o arquivo foi parar', /Comprovante salvo/.test(await p.locator('.toast').innerText()));
  ok('Sem erros de JavaScript', !p.errs.length, p.errs.join(' | '));
  await ctx.close();
  await b.close();
  console.log(out.join('\n'));
  const f = out.filter(l => l.startsWith('✖')).length;
  console.log(`\n${out.length - f} de ${out.length} passaram`);
  process.exit(f ? 1 : 0);
})().catch(e => { console.error('ERRO NO TESTE', e.message); process.exit(1); });
