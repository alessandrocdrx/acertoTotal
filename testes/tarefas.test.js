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
  const inicio = async () => { await p.evaluate(() => { for (let i = 0; i < 3 && window.acertoVoltar(); i++); }); await p.click('button[data-tab=contas]'); toques = 0; };

  // ===== Tarefa 1: "Como importar um cliente com 8 contas?"
  await inicio();
  const CPF_L = cpf('489768865');
  ok('T1 · A aba Planilha está na barra de baixo, sempre visível', await visivelSemRolar('button[data-tab=planilha]'));
  await tocar('button[data-tab=planilha]');
  ok('T1 · "Importar planilha" aparece sem rolar', await visivelSemRolar('[data-act=import-abrir]'));
  await tocar('[data-act=import-abrir]');
  ok('T1 · A primeira tela da importação já explica "8 contas = 8 linhas com o mesmo CPF/CNPJ"', /8 contas ocupa 8 linhas com o mesmo CPF\/CNPJ/.test(await p.locator('.sheet').evaluate(e => { e.querySelector('details.como').open = true; return e.innerText; })));
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
  ok('T3 · Versão de teste: CPF com dígito errado avisa que só é aceito no teste', /só porque esta é a versão de teste/.test(await p.locator('#n-doc-t').innerText()));
  ok('T3 · O aviso de teste não aparece para CPF válido', await (async () => { await p.fill('#n-doc', cpf('987654321')); return await p.locator('#n-doc-t').count() === 0; })());
  await p.fill('#n-doc', cpf('987654321'));
  await tocar('[data-act=salvar-cliente]');
  ok('T3 · Dois clientes podem ter o mesmo nome; o que não pode é o mesmo CPF/CNPJ', (await estado()).clientes.some(c => c.nome === 'Homônimo Lucas'));
  // Relato de revisão (PDF de UX): o CPF é dado técnico; a lista mostra quanto o cliente deve, e o CPF fica na ficha e na busca.
  ok('T3 · A lista não repete o CPF em cada linha, mas a busca acha o cliente pelo CPF', !/CPF 489\.768/.test(await p.locator('#screen').innerText()) && await (async () => { await p.fill('#qc', '48976886550'); return /Lucas/.test(await p.locator('#screen').innerText()); })());
  await p.fill('#qc', '');
  await p.locator('#screen .row', { hasText: 'Lucas Pereira' }).first().click();
  ok('T3 · A ficha do cliente mostra o CPF', /CPF 489\.768\.865-50/.test(await p.locator('.sheet').innerText()));
  await p.keyboard.press('Escape');

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
  await enviar('backup-acertototal.xlsx', bk.d);
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

  // ===== Tarefa 7: clientes antigos sem CPF/CNPJ (vindos da versão anterior) não podem ficar assim
  const hoje = new Date().toISOString().slice(0, 10);
  await p.evaluate(h => localStorage.setItem('baixa-pix-v2', JSON.stringify({ clientes: [
    { id: 'a1', nome: 'Marina Alves', telefone: '', email: '' }, { id: 'a2', nome: 'Descrição', telefone: '', email: '' }, { id: 'a3', nome: 'E-mail', telefone: '', email: '' },
    { id: 'a4', nome: 'Rui Costa', doc: '52998224725', telefone: '', email: '' }],
    contas: [{ id: 'k1', clienteId: 'a1', descricao: 'Mensalidade', valor: 180, venc: h, pagoEm: null, comprovante: null }], ultimoBackup: null })), hoje);
  await p.reload(); await p.waitForSelector('#screen'); await p.waitForTimeout(300);
  await inicio();
  await tocar('button[data-tab=clientes]');
  ok('T7 · Clientes sem CPF/CNPJ: aviso no topo, sem rolar, com a contagem', await visivelSemRolar('#aviso-semdoc') && /3 clientes estão sem CPF\/CNPJ/.test(await p.locator('#aviso-semdoc').innerText()));
  await p.click('.row:has-text("Marina Alves")');
  ok('T7 · Ficha de cliente sem CPF/CNPJ: não oferece "Nova conta" nem "Atualizar pelo Excel", só "Completar cadastro"', await p.locator('.sheet [data-act=nova-conta-cli]').count() === 0 && await p.locator('.sheet [data-act=editar-xl-cli]').count() === 0 && await p.locator('.sheet button.primary[data-act=editar-cliente]').count() === 1);
  await inicio();
  await p.click('[data-act=nova-conta]');
  ok('T7 · Nova conta: cliente sem CPF/CNPJ aparece desativado na lista', await p.locator('#nc-cli option:has-text("Marina Alves")').isDisabled() && /falta CPF\/CNPJ/.test(await p.locator('#nc-cli option:has-text("Marina Alves")').innerText()) && !(await p.locator('#nc-cli option:has-text("Rui Costa")').isDisabled()));
  await inicio();
  await p.click('button[data-tab=planilha]'); await p.click('[data-act=import-abrir]');
  await enviar('legado.xlsx', await xlsx([['CPF/CNPJ', 'Nome', 'Descrição', 'Valor', 'Vencimento'], ['', 'Marina Alves', 'Extra', 50, '10/12/2026']]));
  ok('T7 · Planilha sem CPF/CNPJ para cliente antigo é recusada e explica como completar', await p.locator('[data-act=import-confirm]').isDisabled() && /“Marina Alves” está no app sem CPF\/CNPJ: preencha o CPF ou CNPJ nesta linha/.test(await p.locator('.sheet').innerText()));
  await inicio();
  await tocar('button[data-tab=clientes]');
  await tocar('[data-act=completar-docs]');
  ok('T7 · "Completar agora" abre o primeiro, com o CPF/CNPJ em foco', /Completar cadastro/.test(await p.locator('.sheet h2').innerText()) && await p.evaluate(() => document.activeElement.id === 'n-doc'));
  ok('T7 · Cliente sem contas (lixo de planilha) pode ser excluído ali mesmo', /Descrição/.test(await p.locator('.sheet .sub').innerText()) && await p.locator('.sheet [data-act=excluir-cliente]').count() === 1);
  await tocar('.sheet [data-act=excluir-cliente]');
  ok('T7 · Ao excluir, já abre o próximo da fila', /E-mail/.test(await p.locator('.sheet .sub').innerText()));
  await tocar('.sheet [data-act=excluir-cliente]');
  ok('T7 · Cliente com contas não oferece excluir', /Marina Alves/.test(await p.locator('.sheet .sub').innerText()) && await p.locator('.sheet [data-act=excluir-cliente]').count() === 0);
  await p.fill('#n-doc', '52998224725'); await tocar('.sheet [data-act=salvar-cliente]');
  ok('T7 · CPF que já é de outro cliente é recusado', /já é de Rui Costa/.test(await p.locator('#n-doc-err').innerText()));
  await p.fill('#n-doc', cpf('123456789')); await tocar('.sheet [data-act=salvar-cliente]');
  const S7 = await estado();
  ok('T7 · No fim, todos os clientes têm CPF/CNPJ e o aviso some', S7.clientes.length === 2 && S7.clientes.every(c => c.doc) && await p.locator('#aviso-semdoc').count() === 0 && await p.locator('.sheet').count() === 0);
  ok(`T7 · 3 cadastros resolvidos em ${toques} toques (meta: até 7)`, toques <= 7, `${toques}`);

  // ===== Tarefa 8: "Apagar tudo" na versão de teste, confirmado digitando APAGAR
  const aval0 = await p.evaluate(() => localStorage.getItem('acertototal-avaliacao'));
  await inicio();
  await tocar('.head [data-act=abrir-menu]');
  ok('T8 · Versão de teste: "Apagar tudo…" fica no fim do Menu, separado das opções (como nos apps Android)', await p.locator('.perigo [data-act=apagar-abrir]').count() === 1 && await p.locator('.opcoes [data-act=apagar-abrir]').count() === 0);
  await tocar('[data-act=apagar-abrir]');
  const tela8 = await p.locator('.sheet').innerText();
  ok('T8 · A confirmação diz o que será apagado, lembra da cópia de segurança e pede para digitar APAGAR (sem cadastrar CPF antes)', /2 clientes/.test(tela8) && /1 conta\b/.test(tela8) && /não pode ser desfeito/.test(tela8) && /digite APAGAR/.test(tela8) && await p.locator('#dono-doc').count() === 0 && await p.locator('.sheet [data-act=export-backup]').count() === 1, tela8);
  ok('T8 · "Apagar tudo" começa desativado', await p.locator('[data-act=apagar-sim]').isDisabled());
  await p.fill('#apagar-txt', 'apaga');
  ok('T8 · Palavra incompleta ("apaga") não libera o botão', await p.locator('[data-act=apagar-sim]').isDisabled());
  await p.fill('#apagar-txt', '987.654.321-00');
  ok('T8 · Um CPF não libera mais o botão (a confirmação agora é a palavra)', await p.locator('[data-act=apagar-sim]').isDisabled());
  await p.fill('#apagar-txt', '  apagar ');
  ok('T8 · "apagar" em minúsculas, com espaços nas pontas, libera o botão', !(await p.locator('[data-act=apagar-sim]').isDisabled()));
  await tocar('[data-act=apagar-sim]');
  const S8 = await estado();
  ok('T8 · Tudo apagado: sem clientes, contas nem histórico', S8.clientes.length === 0 && S8.contas.length === 0 && (S8.importacoes || []).length === 0);
  ok('T8 · Apagar tudo não reinicia a avaliação de 7 dias', await p.evaluate(() => localStorage.getItem('acertototal-avaliacao')) === aval0);
  await p.reload(); await p.waitForSelector('#screen'); await p.waitForTimeout(300);
  ok('T8 · Depois de reabrir, o app continua vazio (não volta o exemplo)', (await estado()).clientes.length === 0 && /Nenhuma|Comece|nenhuma/.test(await p.locator('#screen').innerText()));
  ok(`T8 · Apagar em ${toques} toques (meta: até 4)`, toques <= 4, `${toques}`);
  await inicio();
  await p.click('.head [data-act=abrir-menu]'); await p.click('[data-act=apagar-abrir]');
  ok('T8 · Na segunda vez, a confirmação é a mesma (nada guardado)', await p.locator('#apagar-txt').count() === 1);
  await p.screenshot({ path: __dirname + '/capturas/tarefas-apagar.png' });

  // Produção não tem "Apagar tudo"
  const pp = await ctx.newPage();
  await pp.addInitScript(() => { window.ACERTO_AMBIENTE = 'producao'; window.ACERTO_LIMITE_DIAS = 0; });
  await pp.route('**/*', r => { const u = r.request().url(); if (u.includes('xlsx')) return r.fulfill({ contentType: 'application/javascript', body: fs.readFileSync(__dirname + '/vendor/xlsx.js', 'utf8') }); return u.startsWith('file://') ? r.continue() : r.fulfill({ status: 200, body: '' }); });
  await pp.goto('file://' + __dirname + '/.build/page.html'); await pp.waitForSelector('#screen');
  await pp.click('button[data-tab=planilha]');
  ok('T8 · Produção: não existe "Apagar tudo"', await pp.locator('[data-act=apagar-abrir]').count() === 0);
  await pp.click('button[data-tab=clientes]'); await pp.click('[data-act=novo-cliente]');
  await pp.fill('#n-nome', 'Teste Produção'); await pp.fill('#n-doc', '111.111.111-11'); await pp.click('[data-act=salvar-cliente]');
  ok('T3 · Produção: CPF com dígito errado é recusado', /CPF inválido\. Confira os números/.test(await pp.locator('#n-doc-err').innerText()));
  await pp.evaluate(() => document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' })));
  await pp.click('button[data-tab=planilha]'); await pp.click('[data-act=import-abrir]');
  await pp.setInputFiles('#xl', { name: 'p.xlsx', mimeType: 'application/octet-stream', buffer: Buffer.from(await xlsx([['CPF/CNPJ', 'Nome', 'Valor', 'Vencimento'], ['111.111.111-11', 'X', 10, '10/12/2026']])) });
  await pp.waitForSelector('.sheet h2:has-text("Prévia")');
  ok('T3 · Produção: planilha com CPF de dígito errado é recusada', await pp.locator('[data-act=import-confirm]').isDisabled() && /não é um CPF nem um CNPJ válido/.test(await pp.locator('.sheet').innerText()));
  await pp.close();

  // ===== Tarefa 9: "Quem fez o app?" e o Menu (padrão do geoTotal)
  await inicio();
  ok('T9 · Ícone ☰ do Menu visível no topo, em qualquer aba', await visivelSemRolar('.head [data-act=abrir-menu]'));
  await tocar('.head [data-act=abrir-menu]');
  ok('T9 · O Menu é uma tela própria: título "Menu", seta de voltar e sem o rodapé', /Menu/.test(await p.locator('#title').innerText()) && (await p.locator('.head [data-act=voltar]').count()) === 1 && !(await p.locator('#tabs').isVisible()));
  const menu = await p.locator('#screen').innerText();
  ok('T9 · O Menu mostra a marca, o autor (@alessandrocdrx) e a versão logo no topo', /acertoTotal/.test(menu) && /Criado por @alessandrocdrx · versão protótipo/.test(menu));
  ok('T9 · O Menu tem Assinatura, Tema, Cópia de segurança, Avaliar, Relatar problema, Privacidade e Perguntas frequentes (e um só "Sobre": o cartão da marca)', ['Assinatura', 'Tema', 'Cópia de segurança', 'Restaurar uma cópia', 'Avaliar o acertoTotal', 'Relatar problema ou sugestão', 'Política de privacidade', 'Perguntas frequentes'].every(t => menu.includes(t)), menu);
  ok('T9 · Links: Play, GitHub e a política certa', await p.locator('a.opcao[href="https://play.google.com/store/apps/details?id=io.github.alessandrocdrx.acertototal"]').count() === 1 && await p.locator('a.opcao[href="https://github.com/alessandrocdrx/acertoTotal/issues/new"]').count() === 1 && await p.locator('a.opcao[href="https://github.com/alessandrocdrx/acertoTotal/blob/main/PRIVACIDADE.md"]').count() === 1);
  await tocar('.menu-marca');
  const sobre = await p.locator('.sheet').innerText();
  ok('T9 · "Sobre": nome, assinatura da marca, versão e "Criado por @alessandrocdrx" com link', /Contas a receber, acertadas\./.test(sobre) && /Versão protótipo/.test(sobre) && /Criado por @alessandrocdrx/.test(sobre) && await p.locator('.sheet a[href="https://github.com/alessandrocdrx"]').count() === 1, sobre);
  ok(`T9 · Achar o autor em ${toques} toques (meta: até 2)`, toques <= 2, `${toques}`);
  await p.keyboard.press('Escape');
  await tocar('#screen [data-act=aparencia]');
  ok('T9 · Tema abre pelo Menu', /^Tema/.test(await p.locator('.sheet h2').innerText()));
  await p.keyboard.press('Escape');
  ok('T9 · Voltar (Esc/botão do Android) fecha a janela e continua no Menu', (await p.locator('.sheet').count()) === 0 && /Menu/.test(await p.locator('#title').innerText()));
  await p.keyboard.press('Escape');
  ok('T9 · Voltar de novo sai do Menu e mostra o rodapé', /Contas/.test(await p.locator('#title').innerText()) && await p.locator('#tabs').isVisible());
  await p.click('button[data-tab=clientes]'); await p.click('.head [data-act=abrir-menu]'); await p.click('.head [data-act=voltar]');
  ok('T9 · A seta de voltar devolve à aba de onde a pessoa veio (Clientes)', /Clientes/.test(await p.locator('#title').innerText()));

  ok('Sem erros de JavaScript', errs.length === 0, errs.join(' | '));
  await b.close();
  console.log(out.join('\n'));
  if (out.some(l => l.startsWith('✖'))) process.exitCode = 1;
})().catch(e => { console.error('ERRO NO TESTE', e); process.exit(1); });
