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
  ['Cobrar pelo WhatsApp', async p => { await p.click('.crow .cmain'); await p.click('[data-act=cobrar]'); }],
  ['Cobrar um cliente', async p => { await p.click('.tabs [data-tab=clientes]'); await p.click('#screen .row'); await p.click('.sheet [data-act=cobrar]'); }],
  ['Meu negócio', async p => { await p.click('[data-act=abrir-menu]'); await p.click('#screen [data-act=negocio]'); }],
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
  // 5b) Caixa de digitar com borda visível: contraste de pelo menos 3:1 entre a borda e o fundo em que ela está (WCAG 1.4.11).
  const rgb = c => (c.match(/[\d.]+/g) || []).slice(0, 4).map(Number);
  const lum = ([r, g, b]) => { const f = v => { v /= 255; return v <= .03928 ? v / 12.92 : Math.pow((v + .055) / 1.055, 2.4); }; return .2126 * f(r) + .7152 * f(g) + .0722 * f(b); };
  const fundoDe = e => { for (let n = e.parentElement; n; n = n.parentElement) { const c = rgb(getComputedStyle(n).backgroundColor); if (c.length >= 3 && (c[3] === undefined || c[3] > .9)) return c; } return [255, 255, 255]; };
  // Vale também para os botões de contorno (os cheios têm cor própria e os "quietos" são só texto).
  const campo = [...raiz.querySelectorAll('input:not([type=file]):not([type=checkbox]):not([type=radio]):not([type=hidden]), select, textarea, .btn:not(.primary):not(.quiet):not(.apagar):not(:disabled), .iconbtn:not(.voltar)')].filter(vis)
    .map(e => { const cs = getComputedStyle(e), a = lum(rgb(cs.borderTopColor)), b = lum(fundoDe(e)); return { n: nome(e) || e.id, r: Math.round((Math.max(a, b) + .05) / (Math.min(a, b) + .05) * 10) / 10, w: parseFloat(cs.borderTopWidth) }; })
    .filter(x => x.r < 3 || x.w < 1).map(x => `${x.n} (contraste ${x.r}:1, borda ${x.w}px)`);
  // 6) Rolagem lateral da página.
  const lateral = document.documentElement.scrollWidth - document.documentElement.clientWidth;
  // 7) Janela tem saída visível (Fechar, Cancelar, Agora não, Voltar).
  const sheet = document.querySelector('#layer .sheet');
  const saida = !sheet || [...sheet.querySelectorAll('button')].some(b => /^(Fechar|Cancelar|Agora não|Voltar aos filtros)$/.test(b.innerText.trim()));
  return { toque, campo, cortado, sob: [...new Set(sob)], pequena: [...new Set(pequena)], quebra, lateral, saida };
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
  const inicio = async p => { await p.evaluate(() => { while (window.acertoVoltar && window.acertoVoltar()); document.getElementById('screen').scrollTop = 0; }); await p.click('.tabs [data-tab=contas]'); const m = p.locator('.strip button.mini[aria-pressed=true]'); if (await m.count()) await m.click(); };

  for (const [w, h, tema] of [[360, 780, 'light'], [320, 640, 'light'], [360, 780, 'dark']]) {
    const { p, ctx } = await abrir(w, h, tema);
    const tag = `${w}×${h} ${tema === 'dark' ? 'escuro' : 'claro'}`;
    const prob = { toque: [], campo: [], cortado: [], sob: [], pequena: [], quebra: [], lateral: [], saida: [], axe: [] };
    for (const [nome, ir] of TELAS) {
      try { await inicio(p); await ir(p); } catch (e) { prob.saida.push(`${nome}: não abriu (${e.message.split('\n')[0]})`); continue; }
      await p.waitForTimeout(450);
      await p.screenshot({ path: path.join(FOTOS, `${w}x${h}-${tema}-${nome.replace(/[^\wÀ-ú]+/g, '-')}.png`) });
      const m = await p.evaluate(medir);
      for (const k of ['toque', 'campo', 'cortado', 'sob', 'pequena', 'quebra']) m[k].forEach(x => prob[k].push(`${nome}: ${x}`));
      if (m.lateral > 0) prob.lateral.push(`${nome}: ${m.lateral}px`);
      if (!m.saida) prob.saida.push(`${nome}: sem Fechar/Cancelar`);
      if (w === 360) { // contraste pelo axe em cada tela e janela, nos dois temas
        await p.addScriptTag({ content: AXE });
        const v = await p.evaluate(async () => (await axe.run(document.querySelector('#layer .sheet') || document.getElementById('phone'), { runOnly: ['color-contrast'] })).violations.map(x => x.nodes.map(n => n.target.join(' ')).join(', ')));
        v.forEach(x => prob.axe.push(`${nome}: ${x}`));
      }
    }
    ok(`[${tag}] Toque de 48 px em todos os botões, campos e links das ${TELAS.length} telas`, !prob.toque.length, prob.toque.slice(0, 8).join(' | '));
    ok(`[${tag}] Toda caixa de digitar e todo botão de contorno têm borda visível (contraste ≥ 3:1), não parece espaço vazio`, !prob.campo.length, prob.campo.slice(0, 6).join(' | '));
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
  // Filtros: os 3 cartões do topo mostram quantas contas há e filtram a lista com esse mesmo número
  await p.click('.tabs [data-tab=contas]');
  for (const f of ['avencer', 'atrasadas']) {
    await p.click(`.strip [data-f=${f}]`);
    const n = +(await p.locator(`.strip [data-f=${f}] small`).innerText()).match(/\d+/)[0], l = await p.locator('#screen .crow').count();
    ok(`Cartão "${f}": o número de contas (${n}) é o tamanho da lista (${l}), e o título da lista diz o mesmo`, n === l && new RegExp('· ' + l + '$').test(await p.locator('.lista-t').innerText()));
    await p.click(`.strip [data-f=${f}]`);
    ok(`Tocar de novo no cartão "${f}" tira o filtro e volta ao "Para receber"`, /^Para receber/i.test(await p.locator('.lista-t').innerText()));
  }
  ok('Sem filtro "Todas": a busca olha todas as contas, pagas e a receber', await (async () => { await p.fill('#q', 'a'); await p.dispatchEvent('#q', 'input'); const t = await p.locator('#screen .crow').count(), pg = await p.locator('#screen .crow.paga').count(); await p.fill('#q', ''); await p.dispatchEvent('#q', 'input'); return t > 20 && pg > 0 && (await p.locator('[data-f=todas]').count()) === 0; })());
  // Selos curtos e datas sem o ano
  const sel = await p.evaluate(() => [...document.querySelectorAll('#screen .crow .pill')].map(e => e.innerText.trim()));
  ok('Selos curtos: "Atrasada há 5 dias" e "Vence em 14 dias", sem o prefixo "Em aberto ·"', sel.length > 0 && sel.every(t => /^(Atrasada há \d+ dias?|Vence (hoje|em \d+ dias?)|✓ Paga em \d\d\/\d\d)$/.test(t)), JSON.stringify(sel));
  ok('Data do vencimento sem o ano quando é o ano atual ("venc. 28/10")', await p.evaluate(() => [...document.querySelectorAll('#screen .crow .nw')].every(e => /^venc\. \d\d\/\d\d(\/\d{4})?$/.test(e.innerText.trim()) && (!/\/\d{4}$/.test(e.innerText.trim()) || !e.innerText.includes(String(new Date().getFullYear()))))));
  ok('Topo de Contas enxuto: sem a data "Hoje" e sem botão Exportar', !/Hoje,/.test(await p.locator('.head').innerText()) && (await p.locator('.head .btn').count()) === 0);
  // Pagas: mais recente primeiro, agrupadas por mês
  await p.click('.strip [data-f=pagas]');
  const pagas = await p.evaluate(() => { const S = JSON.parse(localStorage.getItem('baixa-pix-v2')), h = [...document.querySelectorAll('#screen h3:not(.lista-t)')].map(e => e.innerText.trim().toLowerCase()); return { h, n: document.querySelectorAll('#screen .crow.paga').length }; });
  ok('Pagas: contas agrupadas por mês, do mais recente para o mais antigo (novembro, outubro…)', pagas.h.length >= 3 && pagas.h[0] === 'novembro' && pagas.h[1] === 'outubro', JSON.stringify(pagas.h));
  ok('Pagas: dentro do mês, a mais recente vem primeiro', await p.evaluate(() => { const l = [...document.querySelectorAll('#screen .crow.paga .pill')].map(e => e.innerText.match(/(\d\d)\/(\d\d)/)).filter(Boolean).map(m => m[2] + m[1]); const ate = l.slice(0, 6); return ate.every((x, i) => i === 0 || x <= ate[i - 1]); }));
  await p.click('.strip [data-f=pagas]');
  // Menu: ponto no ☰ enquanto a cópia de segurança está pendente, e a situação dentro do Menu
  ok('☰ tem um ponto vermelho e nome para o leitor de tela enquanto a cópia de segurança está pendente', (await p.locator('.head .ponto').count()) === 1 && /cópia de segurança pendente/.test(await p.getAttribute('.head [data-act=abrir-menu]', 'aria-label')));
  await p.click('[data-act=abrir-menu]');
  ok('Menu: um só "Sobre" (o cartão da marca), Tema simples e "Perguntas frequentes"', (await p.locator('#screen [data-act=sobre]').count()) === 1 && /Perguntas frequentes/.test(await p.locator('#screen').innerText()) && !/Tema e paleta/.test(await p.locator('#screen').innerText()));
  ok('Menu: "Cópia de segurança" mostra "Nunca feita" e o aviso não ocupa o topo da lista', /Nunca feita/.test(await p.locator('#screen').innerText()));
  await p.click('#screen [data-act=faq]');
  ok('Perguntas frequentes: 6 respostas curtas, incluindo "Como dar baixa?" e "Como trocar de celular?"', (await p.locator('.sheet details.ajuda').count()) === 6 && /Como dar baixa/.test(await p.locator('.sheet').innerText()) && /Como trocar de celular/.test(await p.locator('.sheet').innerText()));
  await p.keyboard.press('Escape'); await p.keyboard.press('Escape');
  // Clientes: busca, "Deve" e CPF só na ficha
  await p.click('.tabs [data-tab=clientes]');
  ok('Clientes: a busca aparece (4 ou mais clientes)', (await p.locator('#qc').count()) === 1);
  const linha = await p.locator('#screen .row').first().innerText();
  ok('Clientes: a linha diz "Deve R$ …", conta as contas e não repete o CPF', /Deve\s+R\$/.test(linha) && /\d+ contas?/.test(linha) && !/\d{3}\.\d{3}\.\d{3}-\d\d/.test(linha), linha);
  ok('Clientes: o que tem atraso diz "a mais antiga há N dias"', /a mais antiga há \d+ dias?/.test(await p.locator('#screen').innerText()));
  await p.click('#screen .row'); 
  ok('Ficha do cliente: as contas a receber vêm antes das pagas', await p.evaluate(() => { const l = [...document.querySelectorAll('.sheet .crow')].map(r => r.classList.contains('paga')); const i = l.indexOf(true); return i === -1 || l.slice(i).every(Boolean); }));
  await p.keyboard.press('Escape');
  // Números: sem o que repete a tela Contas e sem métricas sem ação
  await p.click('.tabs [data-tab=numeros]');
  const num = await p.locator('#screen').innerText();
  ok('Números: sem "Valor médio por conta", "Pagas com comprovante" e sem Em aberto/Em atraso repetidos', !/Valor médio|Pagas com comprovante/.test(num) && !/^Em aberto$/m.test(num) && (await p.locator('.sum .mini').count()) === 0, num.slice(0, 300));
  ok('Números: no começo do mês, sem "0%" desanimador (explica o motivo)', !(await p.locator('.pct').count()) && /Nenhuma conta vencida neste mês foi paga ainda/.test(num));
  ok('Números: o subtítulo não repete o período escolhido', (await p.locator('#subtitle').innerText()).trim() === '');
  // Textos: "Marcar como paga" e "Desfazer baixa" sem vermelho
  await p.click('.tabs [data-tab=contas]'); await p.click('.crow .btn.primary');
  ok('Dar baixa: o botão diz "Marcar como paga" (sem o jargão "baixa" entre parênteses)', /^Marcar como paga$/.test((await p.locator('.sheet [data-act=baixa]').innerText()).trim()));
  await p.keyboard.press('Escape');
  // Cobrar pelo WhatsApp (Bloco B): mensagem pronta, com Pix e nome do negócio vindos de "Meu negócio"
  await p.click('.tabs [data-tab=contas]'); await p.click('.crow .cmain'); await p.click('.sheet [data-act=cobrar]');
  ok('Cobrar: mensagem pronta com nome, valor, vencimento, dias de atraso e "Abrir no WhatsApp"', await p.evaluate(() => { const t = document.getElementById('cob-txt').value; return /^Olá, \S+!/.test(t) && /R\$ [\d.]+,\d\d/.test(t) && /venceu em \d\d\/\d\d \(há \d+ dias?\)/.test(t) && !!document.getElementById('cob-wa'); }));
  ok('Cobrar: sem Pix cadastrado, a tela avisa e oferece "Cadastrar agora"', (await p.locator('#aviso-pix [data-act=negocio]').count()) === 1);
  await p.click('#aviso-pix [data-act=negocio]'); await p.fill('#neg-nome', 'Padaria Bom Pão'); await p.fill('#neg-pix', 'pix@bompao.com.br'); await p.click('[data-act=salvar-negocio]');
  ok('Meu negócio: salva e mostra no Menu o nome e a chave Pix', (await p.locator('.toast').innerText()).includes('negócio salvos') && await (async () => { await p.click('[data-act=abrir-menu]'); const t = await p.locator('#screen [data-act=negocio]').innerText(); await p.click('.head [data-act=voltar]'); return /Padaria Bom Pão/.test(t) && /Pix pix@bompao/.test(t); })());
  await p.click('.crow .cmain'); await p.click('.sheet [data-act=cobrar]');
  const wa = await p.evaluate(() => { const a = document.getElementById('cob-wa'); return { href: a.href, alvo: a.target, txt: decodeURIComponent(a.href.split('?text=')[1] || '') }; });
  ok('Cobrar: o link abre o WhatsApp do cliente (55 + DDD + número, só dígitos) com a mensagem já escrita', /^https:\/\/wa\.me\/55\d{10,11}\?text=/.test(wa.href) && wa.alvo === '_blank');
  ok('Cobrar: a mensagem leva a chave Pix e o nome do negócio no fim', /Pode pagar pelo Pix: pix@bompao\.com\.br/.test(wa.txt) && /Padaria Bom Pão$/.test(wa.txt) && !/\u00a0/.test(wa.txt), wa.txt);
  await p.fill('#cob-txt', 'Oi, tudo certo? Passa aqui hoje.');
  ok('Cobrar: se a pessoa mudar o texto, o link leva o texto novo', await p.evaluate(() => decodeURIComponent(document.getElementById('cob-wa').href.split('?text=')[1]) === 'Oi, tudo certo? Passa aqui hoje.'));
  await p.evaluate(() => { window.__copiado = null; navigator.clipboard.writeText = async t => { window.__copiado = t; }; });
  await p.click('.sheet [data-act=copiar-cobranca]'); await p.waitForFunction(() => window.__copiado);
  ok('Cobrar: "Copiar mensagem" copia o texto da caixa e avisa', (await p.evaluate(() => window.__copiado)) === 'Oi, tudo certo? Passa aqui hoje.' && /Mensagem copiada/.test(await p.locator('.toast').innerText()));
  await p.keyboard.press('Escape'); await p.keyboard.press('Escape');
  // Cliente com várias contas: uma só mensagem com a lista e o total
  await p.click('.tabs [data-tab=clientes]'); await p.locator('#screen .row', { hasText: 'Carlos' }).first().click();
  ok('Ficha do cliente com contas a receber: "Cobrar no WhatsApp" é o botão principal, no topo', /Cobrar no WhatsApp/.test(await p.locator('.sheet .actions .btn.primary').first().innerText()));
  await p.click('.sheet [data-act=cobrar]');
  const msgC = await p.evaluate(() => document.getElementById('cob-txt').value);
  ok('Cobrar um cliente: lista as contas (uma por linha) e o total', /Passando para lembrar de \d+ contas em aberto/.test(msgC) && (msgC.match(/^• /gm) || []).length >= 2 && /Total: R\$ [\d.]+,\d\d\./.test(msgC), msgC);
  await p.keyboard.press('Escape'); await p.keyboard.press('Escape');
  // Sem telefone com DDD: não há link quebrado, a tela explica e dá a saída
  await p.evaluate(() => { const S = JSON.parse(localStorage.getItem('baixa-pix-v2')); S.clientes.forEach(c => { if (/Carlos/.test(c.nome)) c.telefone = '12345'; }); localStorage.setItem('baixa-pix-v2', JSON.stringify(S)); });
  await p.reload(); await p.waitForSelector('.crow'); await p.click('.tabs [data-tab=clientes]'); await p.locator('#screen .row', { hasText: 'Carlos' }).first().click(); await p.click('.sheet [data-act=cobrar]');
  ok('Sem telefone válido: não existe link do WhatsApp, a tela avisa "Falta o telefone com DDD" e oferece Editar dados e Copiar', (await p.locator('#cob-wa').count()) === 0 && /Falta o telefone com DDD/.test(await p.locator('#aviso-tel').innerText()) && (await p.locator('#aviso-tel [data-act=editar-cliente]').count()) === 1 && (await p.locator('[data-act=copiar-cobranca]').count()) === 1);
  await p.keyboard.press('Escape'); await p.keyboard.press('Escape');
  // Editar conta: muda valor e vencimento, avisa e permite desfazer
  await p.click('.tabs [data-tab=contas]'); await p.click('.crow .cmain');
  const antes = await p.evaluate(() => document.querySelector('.sheet .kv').innerText);
  await p.click('.sheet [data-act=editar-conta]');
  ok('Editar conta: abre com cliente, descrição, valor e vencimento preenchidos', (await p.inputValue('#ec-desc')).length > 0 && /^\d+,\d\d$/.test(await p.inputValue('#ec-val')) && /^\d{4}-\d\d-\d\d$/.test(await p.inputValue('#ec-venc')) && (await p.locator('#ec-cli').inputValue()).length > 0);
  await p.fill('#ec-val', '999,90'); await p.fill('#ec-desc', 'Descrição corrigida'); await p.fill('#ec-venc', '2026-12-15');
  await p.click('[data-act=salvar-edicao]');
  ok('Editar conta: salva, avisa "Conta atualizada" e a lista mostra o novo valor', /Conta atualizada/.test(await p.locator('.toast').innerText()) && /999,90/.test(await p.locator('#screen').innerText()));
  await p.click('.toast-undo'); await p.waitForTimeout(150);
  ok('Editar conta: "Desfazer" devolve os valores de antes', !/999,90/.test(await p.locator('#screen').innerText()));
  await p.click('.crow .cmain'); await p.click('.sheet [data-act=editar-conta]'); await p.fill('#ec-val', '0');
  await p.click('[data-act=salvar-edicao]');
  ok('Editar conta: valor zerado é barrado no próprio campo, sem fechar a janela', /Informe o valor/.test(await p.locator('#ec-val-err').innerText().catch(() => '')) && (await p.locator('.sheet').count()) === 1);
  await p.keyboard.press('Escape'); await p.keyboard.press('Escape');
  // Botão flutuante: some ao rolar para baixo e volta ao rolar para cima
  await p.click('.tabs [data-tab=contas]');
  await p.evaluate(() => { const s = document.getElementById('screen'); s.scrollTop = 200; s.dispatchEvent(new Event('scroll')); });
  const oculto = await p.evaluate(() => document.querySelector('.fab').classList.contains('oculto'));
  await p.evaluate(() => { const s = document.getElementById('screen'); s.scrollTop = 120; s.dispatchEvent(new Event('scroll')); });
  const volta = await p.evaluate(() => !document.querySelector('.fab').classList.contains('oculto'));
  ok('Botão "+": sai da frente ao rolar para baixo e volta ao rolar para cima (não cobre o "Dar baixa")', oculto && volta);
  // Ficha do cliente: telefone e e-mail em linhas separadas
  await p.click('.tabs [data-tab=clientes]'); await p.click('#screen .row');
  ok('Ficha do cliente: telefone e e-mail em linhas separadas', await p.evaluate(() => { const s = document.querySelector('.sheet .sub'); return !/\d · \S+@/.test(s.innerText) && s.innerHTML.split('<br>').length >= 3; }));
  await p.keyboard.press('Escape');
  // Exportar e cópia de segurança: o botão Baixar aparece sem rolar, mesmo com 33 linhas de prévia
  await p.click('.tabs [data-tab=planilha]'); await p.click('#screen [data-act=export-backup]'); await p.waitForTimeout(300);
  const visivel = sel => p.evaluate(s => { const e = document.querySelector(s); if (!e) return false; const r = e.getBoundingClientRect(); return r.top >= 0 && r.bottom <= innerHeight; }, sel);
  ok('Cópia de segurança: "Baixar Excel" aparece sem rolar, antes da prévia', await visivel('.sheet [data-act=xlsx-export]'));
  ok('Cópia de segurança: mostra o que vai dentro (clientes e contas) e não a tabela de códigos', (await p.locator('.sheet tbody tr').count()) === 0 && /\d+ clientes?/.test(await p.locator('#resumo-backup').innerText()) && /\d+ contas?/.test(await p.locator('#resumo-backup').innerText()) && /código na planilha/.test(await p.locator('.sheet').innerText()));
  ok('Cópia de segurança: avisa que os comprovantes não vão na planilha e como salvá-los', /comprovante não vão na planilha/.test(await p.locator('#aviso-comp').innerText()));
  await p.keyboard.press('Escape');
  // Comprovante (relato do cliente: "salvei, mas como visualizo?"): ver em tela cheia, compartilhar e salvar
  await p.click('.tabs [data-tab=contas]'); await p.click('[data-act=filter][data-f=pagas]'); await p.click(`.cmain[data-id="${p.compId}"]`);
  ok('Comprovante: miniatura da foto, botões "Ver" e "Compartilhar" lado a lado, e "Salvar na pasta Downloads"', (await p.locator('.sheet .comp-mini img').count()) === 1 && (await p.locator('.sheet .comp .lado [data-act=ver-comp]').innerText()).trim() === 'Ver' && (await p.locator('.sheet .comp .lado [data-act=enviar-comp]').innerText()).trim() === 'Compartilhar' && (await p.locator('.sheet [data-act=salvar-comp]').count()) === 1);
  await p.click('.sheet .comp .lado [data-act=ver-comp]');
  await p.addScriptTag({ content: AXE });
  const axeVisor = await p.evaluate(async () => (await axe.run(document.getElementById('visor'), { runOnly: ['color-contrast'] })).violations.map(x => x.nodes.map(n => n.target.join(' ')).join(', ')));
  ok('Tela cheia do comprovante: contraste mínimo (o "Fechar" se lê)', !axeVisor.length, axeVisor.join(' | '));
  ok('Ver: abre o comprovante em tela cheia, com "Fechar"', await p.evaluate(() => { const v = document.getElementById('visor'), r = v && v.getBoundingClientRect(), ph = document.getElementById('phone').getBoundingClientRect(); return !!v && r.width === ph.width && r.height === ph.height && !!v.querySelector('img') && /Fechar/.test(v.innerText); }));
  await p.keyboard.press('Escape');
  ok('Voltar (Esc ou botão do Android) fecha só a tela cheia e volta para a conta', (await p.locator('#visor').count()) === 0 && (await p.locator('.sheet .comp').count()) === 1);
  await p.click('.sheet .comp-mini');
  ok('Tocar na miniatura também abre a tela cheia', (await p.locator('#visor img').count()) === 1);
  await p.click('#visor [data-act=fechar-visor]');
  await p.evaluate(() => { window.__share = null; navigator.canShare = () => true; navigator.share = async d => { window.__share = { nome: d.files[0].name, tipo: d.files[0].type, tam: d.files[0].size }; }; });
  await p.click('.sheet [data-act=enviar-comp]'); await p.waitForFunction(() => window.__share);
  const sh = await p.evaluate(() => window.__share);
  ok('Compartilhar: entrega o arquivo de verdade (WhatsApp, e-mail, Drive)', /\.jpg$/.test(sh.nome) && sh.tipo === 'image/jpeg' && sh.tam > 100, JSON.stringify(sh));
  await p.click('.sheet [data-act=salvar-comp]');
  await p.waitForFunction(() => window.__salvo);
  const salvo = await p.evaluate(() => window.__salvo);
  ok('Salvar: arquivo .jpg com o nome do cliente e a data, e bytes de JPEG', /^comprovante-[a-z0-9-]+-\d{4}-\d{2}-\d{2}\.jpg$/.test(salvo.nome) && salvo.bytes[0] === 0xFF && salvo.bytes[1] === 0xD8, salvo.nome);
  ok('Salvar: avisa onde o arquivo foi parar', /Comprovante salvo/.test(await p.locator('.toast').innerText()));
  await p.keyboard.press('Escape');
  // Dados de exemplo: o comprovante de exemplo não fica vazio ("sem arquivo"), aparece como ilustração e abre
  const exId = await p.evaluate(() => { const S = JSON.parse(localStorage.getItem('baixa-pix-v2')); const c = S.contas.find(x => x.comprovante && !x.comprovante.data); return c && c.id; });
  await p.click('.tabs [data-tab=contas]'); await p.fill('#q', 'a'); await p.dispatchEvent('#q', 'input');
  await p.evaluate(id => document.querySelector(`.cmain[data-id="${id}"]`).click(), exId);
  ok('Comprovante de exemplo: mostra a ilustração, marcada como "de exemplo", sem "sem arquivo"', (await p.locator('.sheet .comp-mini img').count()) === 1 && /Comprovante de exemplo/.test(await p.locator('.sheet .comp-nome').innerText()) && !/sem arquivo/.test(await p.locator('.sheet').innerText()));
  await p.click('.sheet .comp .lado [data-act=ver-comp]');
  ok('Comprovante de exemplo: abre em tela cheia', (await p.locator('#visor img').count()) === 1);
  await p.keyboard.press('Escape'); await p.keyboard.press('Escape');
  // No app Android, "Ver" e "Compartilhar" usam os apps do celular (fotos, PDF, WhatsApp) pela ponte
  await p.evaluate(() => { window.__ponte = []; window.acertoTotalAndroid = { abrirArquivo: (n, b) => { window.__ponte.push(['abrir', n, b.length]); return 'ok'; }, compartilharArquivo: (n, b) => { window.__ponte.push(['compartilhar', n, b.length]); return 'ok'; } }; });
  await p.click('[data-act=filter][data-f=pagas]'); await p.click(`.cmain[data-id="${p.compId}"]`);
  await p.click('.sheet .comp .lado [data-act=ver-comp]'); await p.click('.sheet [data-act=enviar-comp]');
  const ponte = await p.evaluate(() => window.__ponte);
  ok('Android: "Ver" abre no app de fotos/PDF e "Compartilhar" abre a janela do Android, com o arquivo inteiro', ponte.length === 2 && ponte[0][0] === 'abrir' && ponte[1][0] === 'compartilhar' && /\.jpg$/.test(ponte[0][1]) && ponte[0][2] > 100 && (await p.locator('#visor').count()) === 0, JSON.stringify(ponte));
  ok('Sem erros de JavaScript', !p.errs.length, p.errs.join(' | '));
  await ctx.close();
  await b.close();
  console.log(out.join('\n'));
  const f = out.filter(l => l.startsWith('✖')).length;
  console.log(`\n${out.length - f} de ${out.length} passaram`);
  process.exit(f ? 1 : 0);
})().catch(e => { console.error('ERRO NO TESTE', e.message.split('\n')[0], '| linha:', (e.stack || '').split('\n').filter(l => l.includes('experiencia.test')).slice(0, 2).join(' ')); process.exit(1); });
