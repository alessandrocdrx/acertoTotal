// Teste de usabilidade por tarefas: as perguntas reais do cliente viram roteiros.
// Para cada tarefa: o caminho é encontrável (o botão está visível sem rolar), quantos toques leva,
// e se o resultado gravado está certo. Limite de toques = o caminho mais curto razoável + folga de 1.
const { chromium } = require('playwright');
const fs = require('fs');
const out = [];
const ok = (nome, cond, det = '') => out.push(`${cond ? '✔' : '✖'} ${nome}${cond ? '' : ' → ' + det}`);
const cpf = b => { const d = b.split('').map(Number); for (const n of [9, 10]) { let s = 0; for (let i = 0; i < n; i++) s += d[i] * (n + 1 - i); d.push(s * 10 % 11 % 10); } const x = d.join(''); return `${x.slice(0, 3)}.${x.slice(3, 6)}.${x.slice(6, 9)}-${x.slice(9)}`; };

(async () => {
  const b = await chromium.launch();
  const ctx = await b.newContext({ viewport: { width: 390, height: 844 }, timezoneId: 'America/Sao_Paulo', locale: 'pt-BR' });
  const p = await ctx.newPage();
  p.setDefaultTimeout(6000);
  const errs = [];
  p.on('pageerror', e => errs.push(e.message));
  await p.addInitScript(() => {
    window.claude = { use: async n => n === 'downloads' ? { save: async r => { window.__salvo = { nome: r.filename, d: Array.from(new Uint8Array(r.data)) }; return { status: 'saved' }; } } : null };
  });
  await p.route('**/*', r => {
    const u = r.request().url();
    if (u.includes('xlsx')) return r.fulfill({ contentType: 'application/javascript', body: fs.readFileSync(__dirname + '/vendor/xlsx.js', 'utf8') });
    if (u.startsWith('file://')) return r.continue();
    return r.fulfill({ status: 200, body: '' });
  });
  await p.goto('file://' + __dirname + '/.build/page.html');
  await p.waitForSelector('.crow');
  await p.evaluate(() => document.getElementById('reset').click());
  await p.waitForTimeout(2600); // deixa o aviso de "dados restaurados" sumir

  const estado = () => p.evaluate(() => JSON.parse(localStorage.getItem('baixa-pix-v2')));
  let toques = 0;
  // Um toque: o elemento precisa estar visível na tela, sem rolar a página principal.
  const tocar = async (sel, rotulo) => {
    const l = p.locator(sel).first();
    await l.waitFor({ state: 'visible' });
    await l.scrollIntoViewIfNeeded(); // dentro de janelas a pessoa rola; conta só o toque
    toques++; await l.click(); await p.waitForTimeout(80);
  };
  const visivelSemRolar = async sel => p.evaluate(s => { const e = document.querySelector(s); if (!e) return false; const r = e.getBoundingClientRect(); return r.top >= 0 && r.bottom <= innerHeight; }, sel);
  const enviar = async (nome, bytes) => { toques++; await p.setInputFiles('#xl', { name: nome, mimeType: 'application/octet-stream', buffer: Buffer.from(bytes) }); await p.waitForSelector('.sheet h2:has-text("Prévia")'); await p.waitForTimeout(250); };
  const xlsx = aoa => p.evaluate(a => { const wb = XLSX.utils.book_new(); XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet(a), 'S'); return Array.from(new Uint8Array(XLSX.write(wb, { type: 'array', bookType: 'xlsx' }))); }, aoa);
  const lerSalvo = async () => { await p.waitForFunction(() => window.__salvo); const r = await p.evaluate(() => { const w = XLSX.read(new Uint8Array(window.__salvo.d), { type: 'array' }); return { nome: window.__salvo.nome, aoa: XLSX.utils.sheet_to_json(w.Sheets[w.SheetNames[0]], { header: 1, defval: '' }), d: window.__salvo.d }; }); await p.evaluate(() => { window.__salvo = null; }); return r; };
  const inicio = async () => { await p.evaluate(() => document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))); await p.click('button[data-tab=contas]'); toques = 0; };

  // ===== Tarefa 1: "Como importar um cliente com 8 contas?"
  await inicio();
  const CPF_L = cpf('489768865');
  ok('T1 · A aba Planilha está na barra de baixo, sempre visível', await visivelSemRolar('button[data-tab=planilha]'));
  await tocar('button[data-tab=planilha]');
  ok('T1 · "Importar planilha" aparece sem rolar', await visivelSemRolar('[data-act=import-abrir]'));
  await tocar('[data-act=import-abrir]');
  ok('T1 · A primeira tela da importação já explica "8 contas = 8 linhas com o mesmo CPF/CNPJ"', /8 contas ocupa 8 linhas com o mesmo CPF\/CNPJ/.test(await p.locator('.sheet .sub').innerText()));
  const linhas8 = [['CPF/CNPJ', 'Nome', 'Telefone', 'Descrição', 'Valor', 'Vencimento']];
  for (let i = 1; i <= 8; i++) linhas8.push([CPF_L, 'Lucas Pereira', i === 1 ? '(11) 95555-0000' : '', `Parcela ${i}/8`, 250, `${String(i).padStart(2, '0')}/12/2026`]);
  await enviar('lucas.xlsx', await xlsx(linhas8));
  const prev = (await p.locator('.sheet').innerText()).replace(/ /g, ' ');
  ok('T1 · A prévia mostra 1 cliente novo com as 8 contas embaixo dele', /Cliente novo: Lucas Pereira · [\d.-]+\s+Cliente novo · \(11\) 95555-0000\s+8 contas novas:/.test(prev) && (prev.match(/\+ Parcela \d\/8/g) || []).length === 8, prev.slice(0, 600));
  ok('T1 · O botão diz exatamente o que vai gravar', (await p.locator('[data-act=import-confirm]').innerText()) === 'Importar 1 cliente e 8 contas');
  await tocar('[data-act=import-confirm]');
  const S1 = await estado(), lucas = S1.clientes.filter(c => c.nome === 'Lucas Pereira');
  ok('T1 · Resultado: 1 cliente (não 8) com 8 contas', lucas.length === 1 && S1.contas.filter(c => c.clienteId === lucas[0].id).length === 8);
  ok(`T1 · Feito em ${toques} toques (meta: até 5)`, toques <= 5, `${toques}`);

  // ===== Tarefa 2: "Como atualizar 5 contas de um cliente?"
  await inicio();
  await tocar('button[data-tab=clientes]');
  ok('T2 · Busca acha o cliente pelo CPF', await (async () => { await p.fill('#qc', CPF_L.replace(/\D/g, '').slice(0, 6)); await p.waitForTimeout(100); const n = await p.locator('#screen .row').count(); await p.fill('#qc', ''); return n === 1; })());
  await tocar('.row:has-text("Lucas Pereira")');
  ok('T2 · Na ficha do cliente, "Atualizar pelo Excel" aparece sem rolar', await visivelSemRolar('.sheet [data-act=editar-xl-cli]'));
  await tocar('.sheet [data-act=editar-xl-cli]');
  ok('T2 · A prévia já vem filtrada no cliente e ensina os próximos passos', /Marina|Lucas Pereira/.test(await p.locator('.sheet .sub').innerText()) && /Não mexa na coluna “Código da conta”/.test(await p.locator('.sheet').innerText()));
  await tocar('[data-act=xlsx-export]');
  const plan = await lerSalvo();
  ok('T2 · Arquivo só com as 8 contas do cliente, com código e CPF', plan.aoa.length === 9 && plan.aoa.slice(1).every(l => l[2] === 'Lucas Pereira' && l[0] && l[1] === CPF_L), JSON.stringify(plan.aoa.slice(0, 2)));
  // "No Excel": muda 5 contas (valor em 3, vencimento em 1, baixa em 1)
  const ed = plan.aoa.map(l => l.slice());
  ed[1][6] = '275,00'; ed[2][6] = '275,00'; ed[3][6] = '275,00'; ed[4][7] = '20/12/2026'; ed[5][8] = '31/10/2026';
  await p.evaluate(() => document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))); toques++;
  await tocar('button[data-tab=planilha]');
  ok('T2 · "Atualizar pelo Excel" aparece sem rolar na aba Planilha', await visivelSemRolar('[data-act=atualizar-abrir]'));
  await tocar('[data-act=atualizar-abrir]');
  await tocar('[data-act=enviar-editada]');
  await enviar('editar-contas-lucas.xlsx', await xlsx(ed));
  const p2 = (await p.locator('.sheet').innerText()).replace(/ /g, ' ');
  ok('T2 · A prévia mostra as 5 contas do cliente com antes → depois', (p2.match(/Conta: Parcela \d\/8 · vence/g) || []).length === 5 && /Valor: R\$\s250,00\s*→?\s*(para)?\s*R\$\s275,00/.test(p2) && /Vencimento: 04\/12\/2026\s*→?\s*(para)?\s*20\/12\/2026/.test(p2) && /Baixa: em aberto\s*→?\s*(para)?\s*paga em 31\/10\/2026/.test(p2), p2.slice(0, 900));
  ok('T2 · As outras 3 contas aparecem como "já estão iguais"', /3 contas já estão iguais no app/.test(p2));
  ok('T2 · O botão diz "Importar 5 atualizações"', (await p.locator('[data-act=import-confirm]').innerText()) === 'Importar 5 atualizações');
  await tocar('[data-act=import-confirm]');
  const S2 = await estado(), cs = S2.contas.filter(c => c.clienteId === lucas[0].id).sort((a, c) => a.venc < c.venc ? -1 : 1);
  ok('T2 · Resultado: 3 valores, 1 vencimento e 1 baixa mudaram; nada foi duplicado', cs.length === 8 && cs.filter(c => c.valor === 275).length === 3 && cs.some(c => c.descricao === 'Parcela 4/8' && c.venc === '2026-12-20') && cs.filter(c => c.pagoEm === '2026-10-31').length === 1);
  ok(`T2 · Feito em ${toques} toques fora do Excel (meta: até 10)`, toques <= 10, `${toques}`);

  // ===== Tarefa 3: "Qual a chave única do cliente?"
  await inicio();
  await tocar('button[data-tab=clientes]');
  await tocar('[data-act=novo-cliente]');
  ok('T3 · O cadastro pede CPF ou CNPJ e explica para que serve', /Identifica o cliente nas planilhas/.test(await p.locator('.sheet').innerText()));
  await p.fill('#n-nome', 'Homônimo Lucas'); await p.fill('#n-doc', CPF_L);
  await tocar('[data-act=salvar-cliente]');
  ok('T3 · CPF repetido é barrado dizendo de quem é', /Esse CPF já é de Lucas Pereira/.test(await p.locator('#n-doc-err').innerText()));
  await p.fill('#n-doc', '48976886551');
  await tocar('[data-act=salvar-cliente]');
  ok('T3 · CPF com dígito errado é barrado', /CPF inválido/.test(await p.locator('#n-doc-err').innerText()));
  await p.fill('#n-doc', cpf('987654321'));
  await tocar('[data-act=salvar-cliente]');
  ok('T3 · Dois clientes podem ter o mesmo nome; o que não pode é o mesmo CPF/CNPJ', (await estado()).clientes.some(c => c.nome === 'Homônimo Lucas'));
  ok('T3 · A lista mostra o CPF de cada cliente', /CPF 489\.768\.865-50/.test(await p.locator('#screen').innerText()));

  // ===== Tarefa 4: "Exportar as contas atrasadas"
  await inicio();
  await tocar('button[data-tab=planilha]');
  ok('T4 · "Exportar relatório" aparece sem rolar', await visivelSemRolar('[data-act=export-abrir]'));
  await tocar('[data-act=export-abrir]');
  ok('T4 · Os filtros abrem limpos (o cliente da tarefa anterior não fica preso)', (await p.inputValue('#xf-cli')) === '' && (await p.getAttribute('[data-act=xf-status][data-v=atrasadas]', 'aria-pressed')) === 'true');
  await tocar('[data-act=export-custom]');
  await tocar('[data-act=xlsx-export]');
  const rel = await lerSalvo();
  ok('T4 · Relatório de atrasadas com situação e dias de atraso', /^contas-atrasadas-/.test(rel.nome) && rel.aoa[0].includes('Dias de atraso') && rel.aoa.slice(1).every(l => l[6] === 'Atrasada'));
  ok(`T4 · Feito em ${toques} toques (meta: até 4)`, toques <= 4, `${toques}`);

  // ===== Tarefa 5: "Trocar de celular sem perder nada"
  await inicio();
  await tocar('button[data-tab=planilha]');
  await tocar('[data-act=export-backup]');
  await tocar('[data-act=xlsx-export]');
  const bk = await lerSalvo(), antes = await estado();
  await p.evaluate(() => localStorage.setItem('baixa-pix-v2', JSON.stringify({ clientes: [], contas: [], ultimoBackup: null })));
  await p.reload(); await p.waitForSelector('#screen');
  toques = 0;
  await tocar('button[data-tab=planilha]');
  await tocar('[data-act=import-abrir]');
  await enviar('backup-celsopix.xlsx', bk.d);
  await tocar('[data-act=import-confirm]');
  const depois = await estado();
  ok('T5 · No celular novo, importar a cópia traz tudo de volta', depois.clientes.length === antes.clientes.length && depois.contas.length === antes.contas.length && depois.contas.every(c => antes.contas.some(a => a.id === c.id && a.valor === c.valor && a.venc === c.venc && (a.pagoEm || null) === (c.pagoEm || null))));
  ok(`T5 · Restaurar em ${toques} toques (meta: até 4)`, toques <= 4, `${toques}`);

  // ===== Tarefa 6: "Errei a planilha" (sair sem estragar nada)
  await inicio();
  await tocar('button[data-tab=planilha]');
  await tocar('[data-act=import-abrir]');
  const ruim = await xlsx([['CPF', 'Nome', 'Valor', 'Vencimento'], ['123', 'X', 'abc', '99/99/9999']]);
  const S6 = await estado();
  await enviar('errada.xlsx', ruim);
  ok('T6 · Planilha toda errada: nada para importar e o motivo de cada célula', await p.locator('[data-act=import-confirm]').isDisabled() && /CPF\/CNPJ \(A2\)/.test(await p.locator('.sheet').innerText()) && /Valor \(C2\)/.test(await p.locator('.sheet').innerText()) && /Vencimento \(D2\)/.test(await p.locator('.sheet').innerText()));
  await tocar('.sheet [data-act=close]');
  ok('T6 · Cancelar não muda nada', JSON.stringify(await estado()) === JSON.stringify(S6));

  ok('Sem erros de JavaScript', errs.length === 0, errs.join(' | '));
  await b.close();
  console.log(out.join('\n'));
  if (out.some(l => l.startsWith('✖'))) process.exitCode = 1;
})().catch(e => { console.error('ERRO NO TESTE', e); process.exit(1); });
