// Exportação com filtros (situação, cliente e período de vencimento).
// O teste recalcula o resultado esperado direto dos dados salvos e compara com a tela e com o arquivo.
const { chromium } = require('playwright');
const fs = require('fs');
const out = [];
const ok = (nome, cond, det = '') => out.push(`${cond ? '✔' : '✖'} ${nome}${cond ? '' : ' → ' + det}`);

(async () => {
  const b = await chromium.launch();
  const ctx = await b.newContext({ viewport: { width: 390, height: 844 }, timezoneId: 'America/Sao_Paulo', locale: 'pt-BR' });
  const p = await ctx.newPage();
  p.setDefaultTimeout(6000);
  const errs = [];
  p.on('pageerror', e => errs.push(e.message));
  await p.addInitScript(() => {
    window.claude = { use: async n => n === 'downloads' ? { save: async r => { window.__salvo = { nome: r.filename, bytes: Array.from(new Uint8Array(r.data)) }; return { status: 'saved' }; } } : null };
  });
  await p.route('**/*', r => {
    const u = r.request().url();
    if (u.includes('xlsx')) return r.fulfill({ contentType: 'application/javascript', body: fs.readFileSync(__dirname + '/vendor/xlsx.js', 'utf8') });
    if (u.startsWith('file://')) return r.continue();
    return r.fulfill({ status: 200, body: '' });
  });
  await p.goto('file://' + __dirname + '/.build/page.html');
  await p.waitForSelector('.crow');
  await p.evaluate(() => document.getElementById('reset').click()); // grava os dados de exemplo
  await p.click('button[data-tab=planilha]');
  await p.click('[data-act=export-abrir]');
  await p.waitForSelector('#xf-count');

  const S = await p.evaluate(() => JSON.parse(localStorage.getItem('baixa-pix-v2')));
  const hoje = await p.evaluate(() => { const d = new Date(); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); });
  const sit = c => c.pagoEm ? 'pagas' : (c.venc < hoje ? 'atrasadas' : 'abertas');
  const esperado = f => S.contas.filter(c => {
    const s = sit(c);
    if (f.status === 'abertas' && s === 'pagas') return false;
    if (f.status === 'atrasadas' && s !== 'atrasadas') return false;
    if (f.status === 'pagas' && s !== 'pagas') return false;
    if (f.cliente && c.clienteId !== f.cliente) return false;
    if (f.de && c.venc < f.de) return false;
    if (f.ate && c.venc > f.ate) return false;
    return true;
  });
  const contagem = async () => (await p.locator('#xf-count').innerText());
  const qtd = async () => { const m = (await contagem()).match(/^(\d+) conta/); return m ? +m[1] : 0; };
  const soma = l => l.reduce((a, c) => a + c.valor, 0);
  const reais = v => v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

  ok('Exportar abre uma janela própria só com os filtros', (await p.locator('.sheet h2').innerText()) === 'Exportar relatório');
  ok('Filtro padrão: Atrasadas', (await p.getAttribute('[data-act=xf-status][data-v=atrasadas]', 'aria-pressed')) === 'true');
  const e0 = esperado({ status: 'atrasadas' });
  ok('Contagem ao vivo mostra quantidade e total do filtro', (await contagem()) === `${e0.length} contas · ${reais(soma(e0))}`.replace(/ /g, ' ') || (await contagem()).replace(/ /g, ' ') === `${e0.length} contas · ${reais(soma(e0))}`.replace(/ /g, ' '), await contagem());

  for (const st of ['todas', 'abertas', 'pagas', 'atrasadas']) {
    await p.click(`[data-act=xf-status][data-v=${st}]`);
    ok(`Situação "${st}": contagem confere com os dados`, (await qtd()) === esperado({ status: st }).length, `${await qtd()} vs ${esperado({ status: st }).length}`);
  }

  // Cliente
  const marina = S.clientes.find(c => c.nome === 'Marina Alves');
  await p.click('[data-act=xf-status][data-v=todas]');
  await p.evaluate(() => { window.__campo = document.getElementById('xf-cli'); });
  await p.selectOption('#xf-cli', marina.id);
  ok('Filtro por cliente: conta só as contas da Marina', (await qtd()) === esperado({ status: 'todas', cliente: marina.id }).length);
  // selectOption não foca o campo; o que importa é a tela não ser redesenhada (o foco e o teclado continuam no lugar)
  ok('Filtrar não redesenha a tela: o mesmo campo continua lá', await p.evaluate(() => window.__campo === document.getElementById('xf-cli')));
  await p.focus('#xf-de'); await p.keyboard.type('01');
  ok('Digitar na data mantém o foco no campo', await p.evaluate(() => document.activeElement.id === 'xf-de'));
  await p.fill('#xf-de', '');

  // Período
  const de = S.contas.map(c => c.venc).sort()[Math.floor(S.contas.length / 3)];
  await p.fill('#xf-de', de);
  ok('Filtro por período (vencimento a partir de)', (await qtd()) === esperado({ status: 'todas', cliente: marina.id, de }).length, `${await qtd()}`);
  await p.fill('#xf-ate', '2000-01-01');
  ok('Período invertido: erro embaixo do campo "Até"', /A data final é antes da inicial/.test(await p.locator('#xf-ate-err').innerText().catch(() => '')) && (await p.getAttribute('#xf-ate', 'aria-invalid')) === 'true');
  ok('Período invertido: botão de exportar fica desativado', await p.locator('[data-act=export-custom]').isDisabled());
  await p.fill('#xf-ate', hoje);
  ok('Corrigir a data tira o erro e libera o botão', (await p.locator('#xf-ate-err').count()) === 0 && !(await p.locator('[data-act=export-custom]').isDisabled()));

  // Arquivo exportado com os filtros
  const filtro = { status: 'todas', cliente: marina.id, de, ate: hoje };
  const exp = esperado(filtro);
  await p.click('[data-act=export-custom]');
  const sub = await p.locator('.sheet .sub').innerText();
  ok('Prévia diz quais filtros foram usados', /Filtros: Todas · Marina Alves · vencimento de/.test(sub), sub);
  ok('Prévia tem as mesmas linhas que o filtro', (await p.locator('.sheet tbody tr').count()) === exp.length);
  await p.click('[data-act=xlsx-export]');
  await p.waitForFunction(() => window.__salvo);
  const arq = await p.evaluate(() => { const wb = XLSX.read(new Uint8Array(window.__salvo.bytes), { type: 'array' }); return { nome: window.__salvo.nome, aoa: XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], { header: 1, defval: '' }) }; });
  ok('Nome do arquivo diz o filtro e o cliente', /^contas-todas-marina-alves-\d{4}-\d{2}-\d{2}\.xlsx$/.test(arq.nome), arq.nome);
  ok('Arquivo tem as colunas Situação, Pago em e Dias de atraso', JSON.stringify(arq.aoa[0]) === JSON.stringify(['Cliente', 'CPF/CNPJ', 'Telefone', 'Descrição', 'Valor', 'Vencimento', 'Situação', 'Pago em', 'Dias de atraso']));
  ok('Arquivo tem só as contas filtradas, todas da Marina', arq.aoa.length - 1 === exp.length && arq.aoa.slice(1).every(r => r[0] === 'Marina Alves'));
  ok('Soma dos valores do arquivo bate com os dados', Math.abs(arq.aoa.slice(1).reduce((a, r) => a + r[4], 0) - soma(exp)) < 0.01);
  await p.click('.sheet [data-act=xf-voltar]');
  ok('"Voltar aos filtros" volta com os filtros que estavam escolhidos', (await p.inputValue('#xf-cli')) === marina.id && (await p.inputValue('#xf-de')) === de);

  // Limpar filtros
  ok('"Limpar filtros" aparece quando algum filtro foi mudado', await p.locator('[data-act=xf-limpar]').isVisible());
  await p.click('[data-act=xf-limpar]');
  ok('"Limpar filtros" volta ao padrão e some', (await p.inputValue('#xf-cli')) === '' && (await p.inputValue('#xf-de')) === '' && (await p.getAttribute('[data-act=xf-status][data-v=atrasadas]', 'aria-pressed')) === 'true' && !(await p.locator('[data-act=xf-limpar]').isVisible()));

  // Filtro sem resultado
  await p.click('[data-act=xf-status][data-v=pagas]');
  await p.fill('#xf-de', '2099-01-01');
  ok('Sem resultado: mensagem clara e botão desativado', /Nenhuma conta com esses filtros/.test(await contagem()) && await p.locator('[data-act=export-custom]').isDisabled());

  // Layout: os campos de data cabem lado a lado até em 320 px
  await p.setViewportSize({ width: 320, height: 700 });
  const sobra = await p.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  ok('Em 320 px os filtros não causam rolagem horizontal', sobra <= 0, `${sobra}px`);
  ok('Sem erros de JavaScript', errs.length === 0, errs.join(' | '));
  await b.close();
  console.log(out.join('\n'));
  if (out.some(l => l.startsWith('✖'))) process.exitCode = 1;
})().catch(e => { console.error('ERRO NO TESTE', e); process.exit(1); });
