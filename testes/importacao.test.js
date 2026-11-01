// Importação em 3 passos (Arquivo → Colunas → Conferir), com o CPF/CNPJ como chave do cliente e o
// "Código da conta" como chave da conta. Prévia agrupada por cliente, erros com linha, coluna e célula,
// planilha das linhas com problema, colunas com outros nomes, CSV brasileiro, arquivos errados,
// desfazer, cópia de segurança (ida e volta) e acessibilidade.
const { chromium } = require('playwright');
const fs = require('fs');
const AXE = fs.readFileSync(__dirname + '/vendor/axe.js', 'utf8');
const out = [];
const ok = (nome, cond, det = '') => out.push(`${cond ? '✔' : '✖'} ${nome}${cond ? '' : ' → ' + det}`);

// CPF válido a partir de 9 dígitos (para os dados de teste)
const cpf = b => { const d = b.split('').map(Number); for (const n of [9, 10]) { let s = 0; for (let i = 0; i < n; i++) s += d[i] * (n + 1 - i); d.push(s * 10 % 11 % 10); } const x = d.join(''); return `${x.slice(0, 3)}.${x.slice(3, 6)}.${x.slice(6, 9)}-${x.slice(9)}`; };
const CPF_MARINA = '637.129.261-70', CPF_BRUNO = cpf('434259868'), CPF_LIA = cpf('522973638'), CPF_FULANO = cpf('741852963'), CPF_RUI = cpf('159753486');

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
  await p.click('button[data-tab=planilha]');

  const estado = () => p.evaluate(() => JSON.parse(localStorage.getItem('baixa-pix-v2')));
  const base = s => JSON.stringify({ clientes: s.clientes, contas: s.contas });
  const xlsx = (aoa, abas) => p.evaluate(([a, abas]) => {
    const wb = XLSX.utils.book_new();
    (abas || [['S', a]]).forEach(([n, d]) => XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet(d, { cellDates: true }), n));
    return Array.from(new Uint8Array(XLSX.write(wb, { type: 'array', bookType: 'xlsx' })));
  }, [aoa, abas]);
  const enviar = async (nome, bytes) => {
    await p.setInputFiles('#xl', { name: nome, mimeType: 'application/octet-stream', buffer: Buffer.from(bytes) });
    await p.waitForFunction(() => { const h = document.querySelector('.sheet h2'); return h && !/Lendo/.test(h.textContent); });
    await p.waitForTimeout(300);
    return p.locator('.sheet h2').innerText();
  };
  const texto = async () => (await p.locator('.sheet').innerText()).replace(/ /g, ' ');
  const resumo = async () => Object.fromEntries((await p.$$eval('.imp-res li', l => l.map(x => [x.querySelector('span').textContent, +x.querySelector('b').textContent]))));
  const botao = () => p.locator('[data-act=import-confirm]');
  const salvo = async () => { await p.waitForFunction(() => window.__salvo); const r = await p.evaluate(() => { const w = XLSX.read(new Uint8Array(window.__salvo.d), { type: 'array' }); return { nome: window.__salvo.nome, abas: w.SheetNames, aoa: XLSX.utils.sheet_to_json(w.Sheets[w.SheetNames[0]], { header: 1, defval: '' }), d: window.__salvo.d }; }); await p.evaluate(() => { window.__salvo = null; }); return r; };
  const fechar = () => p.evaluate(() => document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' })));
  const abrirImport = async () => { await fechar(); await p.click('button[data-tab=planilha]'); await p.click('[data-act=import-abrir]'); };
  const axe = async (rotulo) => {
    for (const tema of ['light', 'dark']) {
      await p.evaluate(t => document.documentElement.setAttribute('data-theme', t), tema);
      await p.waitForTimeout(450);
      await p.addScriptTag({ content: AXE }).catch(() => {});
      const r = await p.evaluate(async () => await axe.run(document.querySelector('.sheet') || document.getElementById('phone'), { runOnly: ['wcag2a', 'wcag2aa', 'wcag21aa'] }));
      ok(`Acessibilidade (${tema === 'light' ? 'claro' : 'escuro'}): ${rotulo}`, r.violations.length === 0, r.violations.map(x => `${x.id}(${x.nodes.length})`).join(' | '));
    }
    await p.evaluate(() => document.documentElement.setAttribute('data-theme', 'light'));
  };

  // ---------- Tela Planilha: uma tarefa por cartão, com o nome do objetivo
  const tela = await p.locator('#screen').innerText();
  ok('Planilha pergunta "O que você quer fazer?" e lista 4 tarefas pelo objetivo', /O que você quer fazer\?/.test(tela) && (await p.locator('#screen .opcao').count()) === 4 && /Importar planilha/.test(tela) && /Atualizar pelo Excel/.test(tela) && /Exportar relatório/.test(tela) && /Cópia de segurança/.test(tela), tela.slice(0, 300));
  ok('As 4 tarefas cabem na tela sem rolar', await p.evaluate(() => [...document.querySelectorAll('#screen .opcao')].every(e => e.getBoundingClientRect().bottom <= document.getElementById('tabs').getBoundingClientRect().top)));
  ok('Cópia de segurança mostra o estado na própria lista ("Nunca feita")', /Cópia de segurança[\s\S]*Nunca feita/.test(tela));
  await p.click('[data-act=atualizar-abrir]');
  ok('Atualizar pelo Excel: 3 passos numerados e os 2 botões na ordem de uso', (await p.locator('.passo-a-passo li').count()) === 3 && (await p.locator('.sheet .actions .btn').first().getAttribute('data-act')) === 'editar-xl' && (await p.locator('.sheet [data-act=enviar-editada]').count()) === 1);
  ok('Texto dos passos ocupa a largura (não quebra uma palavra por linha)', await p.evaluate(() => [...document.querySelectorAll('.passo-a-passo li')].every(li => li.getBoundingClientRect().height < 90)));
  ok('Nenhum texto da janela fica espremido em coluna estreita', await p.evaluate(() => { const w = document.querySelector('.sheet').clientWidth; return [...document.querySelectorAll('.sheet p, .sheet li > span')].every(e => !e.textContent.trim() || e.getBoundingClientRect().width > w * 0.5); }));
  await axe('atualizar pelo Excel');
  await fechar();
  await axe('aba Planilha');

  // ---------- Modelo (dentro do passo 1)
  await p.click('[data-act=import-abrir]');
  await p.click('.sheet [data-act=import-modelo]');
  const modelo = await salvo();
  ok('Planilha modelo começa pelo CPF/CNPJ', modelo.nome === 'modelo-importacao-acertototal.xlsx' && JSON.stringify(modelo.aoa[0]) === JSON.stringify(['CPF/CNPJ', 'Nome', 'Telefone', 'E-mail', 'Descrição', 'Valor', 'Vencimento', 'Pago em']), JSON.stringify(modelo.aoa[0]));
  await enviar('modelo-importacao-acertototal.xlsx', modelo.d);
  ok('Importar o modelo vazio diz que está sem dados (não lê a aba "Como preencher")', (await p.locator('.sheet h2').innerText()) === 'A planilha está sem dados');

  // ---------- Passo 1
  await abrirImport();
  const p1 = await texto();
  ok('Passo 1: etapas visíveis, na etapa "Arquivo"', (await p.locator('.passos li[aria-current=step]').innerText()).includes('Arquivo'));
  ok('Passo 1: CPF ou CNPJ obrigatório e Nome para cliente novo', /CPF ou CNPJ\s*obrigatória/.test(p1) && /Nome do cliente\s*cliente novo/.test(p1) && /Código da conta\s*para atualizar/.test(p1), p1.slice(0, 500));
  ok('Passo 1 explica "8 contas = 8 linhas com o mesmo CPF/CNPJ"', /um cliente com 8 contas ocupa 8 linhas com o mesmo CPF\/CNPJ/.test(p1));
  ok('Passo 1: ajuda "Um cliente com várias contas", "Atualizar contas" e "Só clientes"', (await p.locator('details.ajuda').count()) === 3);
  await p.locator('details.ajuda summary').first().click();
  ok('Ajuda mostra exemplo com o mesmo CPF em 3 linhas', (await p.locator('details.ajuda[open] tbody tr').count()) === 3 && /1 cliente com 3 contas/.test(await p.locator('details.ajuda[open]').innerText()));
  await axe('passo 1 (escolher arquivo)');

  const S0 = await estado();
  const marina = S0.clientes.find(c => c.nome === 'Marina Alves');
  const contaM = S0.contas.filter(c => c.clienteId === marina.id && !c.pagoEm).sort((a, c) => a.venc < c.venc ? -1 : 1)[0];
  const br = iso => iso.split('-').reverse().join('/');
  const hoje = await p.evaluate(() => { const d = new Date(); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); });
  const amanha = await p.evaluate(() => { const d = new Date(); d.setDate(d.getDate() + 1); return d.toLocaleDateString('pt-BR'); });

  const arq = await xlsx([
    ['CPF/CNPJ', 'Nome', 'Telefone', 'E-mail', 'Descrição', 'Valor', 'Vencimento', 'Pago em'],
    [CPF_MARINA, 'marina  alves', '(11) 91111-2222', '', contaM.descricao, '200,00', br(contaM.venc), br(hoje)], // 2: atualiza
    [CPF_MARINA, 'Marina Alves', '', '', 'Taxa extra', 75, '15/12/2026', ''],                             // 3: conta nova
    [CPF_BRUNO, 'Bruno Costa', '(21) 90000-1234', 'bruno@exemplo.com', 'Mensalidade', 99.9, '20/12/2026', ''], // 4: cliente + conta novos
    ['', '', '', '', 'Sem nome', 10, '01/12/2026', ''],                                                 // 5: sem CPF
    [CPF_BRUNO, 'Bruno Costa', '', '', 'Errada', 'abc', '01/12/2026', ''],                              // 6: valor
    [CPF_BRUNO, 'Bruno Costa', '', '', 'Errada 2', 10, '32/13/2026', ''],                               // 7: data
    [CPF_BRUNO, 'Bruno Costa', '', '', 'Futuro', 10, '01/12/2026', amanha],                             // 8: pago no futuro
    [CPF_BRUNO, '', '', '', 'Mensalidade', 99.9, '20/12/2026', ''],                                     // 9: repetida (sem nome: acha pelo CPF)
    ['111.111.111', 'Fulano', '', '', 'Conta', 10, '01/12/2026', ''],                                  // 10: CPF incompleto
    ['', '', '', '', '', '', '', ''],                                                                   // 11: em branco
  ]);
  ok('Planilha com títulos certos vai direto para a prévia', /Prévia da importação/.test(await enviar('clientes-novembro.xlsx', arq)));
  ok('Prévia: nada é gravado antes de confirmar', JSON.stringify(await estado()) === JSON.stringify(S0));
  const r = await resumo();
  ok('Resumo: 1 cliente novo, 2 contas novas, 2 atualizações, 6 com problema', r['Clientes novos'] === 1 && r['Contas novas'] === 2 && r['Atualizações'] === 2 && r['Com problema'] === 6, JSON.stringify(r));
  const txt = await texto();
  ok('Mudanças agrupadas por cliente, com o CPF no título', new RegExp(`Marina Alves · ${CPF_MARINA.replace(/\./g, '\\.')}\\s+Telefone:`).test(txt), txt.slice(0, 900));
  ok('Mudança na conta: título da conta e depois o antes → depois', new RegExp(`Conta: ${contaM.descricao} · vence ${br(contaM.venc)}\\s+Valor: R\\$\\s180,00\\s*→?\\s*(para)?\\s*R\\$\\s200,00\\s+Baixa: em aberto`).test(txt));
  ok('Nome escrito diferente ("marina  alves") não vira troca de nome', !/Nome: Marina Alves/.test(txt));
  ok('Entradas agrupadas: cliente novo com as contas dele embaixo', new RegExp(`Cliente novo: Bruno Costa · ${CPF_BRUNO.replace(/\./g, '\\.')}\\s+Cliente novo · \\(21\\) 90000-1234 · bruno@exemplo.com\\s+1 conta nova:\\s+\\+ Mensalidade · R\\$\\s99,90`).test(txt));
  ok('Conta nova de cliente que já existe aparece sob o nome dele', /Marina Alves · 637\.129\.261-70\s+1 conta nova:\s+\+ Taxa extra/.test(txt));
  for (const [linha, motivo] of [
    [5, 'Linha 5 · Sem nome|Linha 5\\s+CPF/CNPJ \\(A5\\): está vazio\\. Cada cliente precisa do CPF ou do CNPJ'],
    [6, 'Linha 6 · Bruno Costa\\s+Valor \\(F6\\): “abc” não é um valor\\. Use só números, por exemplo 150,00'],
    [7, 'Linha 7 · Bruno Costa\\s+Vencimento \\(G7\\): “32/13/2026” não é uma data válida'],
    [8, 'Linha 8 · Bruno Costa\\s+Pago em \\(H8\\): \\d\\d/\\d\\d/\\d{4} é depois de hoje'],
    [9, 'Linha 9\\s+Conta repetida: igual à linha 4'],
    [10, 'Linha 10 · Fulano\\s+CPF/CNPJ \\(A10\\): “111\\.111\\.111” não é um CPF nem um CNPJ válido\\. Confira os números']]) {
    ok(`Linha ${linha}: diz a linha, a coluna, a célula, o porquê e como corrigir`, new RegExp(motivo).test(txt), motivo);
  }
  ok('Linha em branco é ignorada sem virar erro', !/Linha 11/.test(txt));
  ok('Botão diz o que vai gravar: "Importar 1 cliente, 2 contas e 2 atualizações"', (await botao().innerText()) === 'Importar 1 cliente, 2 contas e 2 atualizações', await botao().innerText());
  ok('Há as duas escolhas: atualizar o que mudou e cadastrar o que é novo', (await p.locator('#imp-atualizar').count()) === 1 && (await p.locator('#imp-novos').count()) === 1);
  await p.click('[data-act=imp-ver-prob]'); await p.waitForTimeout(150);
  ok('"Ver problemas" leva até a lista e põe o foco nela', await p.evaluate(() => document.activeElement.id === 'imp-prob'));
  // A régua é a própria janela, e não um número fixo de pixels: a altura do texto muda com as fontes de cada máquina.
  const fixo = await p.evaluate(() => { const j = document.querySelector('.sheet'), r = j.getBoundingClientRect(), b = document.querySelector('[data-act=import-confirm]').getBoundingClientRect(), barra = document.querySelector('.actions.fixo').getBoundingClientRect(); return { fundoJanela: r.bottom, fundoBarra: barra.bottom, topoBotao: b.top, fundoBotao: b.bottom, rolada: j.scrollTop > 0, altura: innerHeight }; });
  ok('Botão de importar fica fixo embaixo, mesmo com a lista rolada', fixo.rolada && fixo.fundoBarra >= fixo.fundoJanela - 40 && fixo.fundoBarra <= fixo.fundoJanela + 1 && fixo.fundoBotao <= fixo.fundoBarra && fixo.topoBotao > 0 && fixo.fundoBotao <= fixo.altura, JSON.stringify(fixo));
  await axe('prévia com problemas');

  await p.click('[data-act=imp-erros]');
  const errosArq = await salvo();
  ok('Linhas com problema: "Linha no arquivo", "Problema" e as colunas originais', JSON.stringify(errosArq.aoa[0]) === JSON.stringify(['Linha no arquivo', 'Problema', 'CPF/CNPJ', 'Nome', 'Telefone', 'E-mail', 'Descrição', 'Valor', 'Vencimento', 'Pago em']) && errosArq.aoa.length === 7, JSON.stringify(errosArq.aoa[0]));

  // Só cadastrar o que é novo / só atualizar
  await p.uncheck('#imp-atualizar');
  ok('Desmarcar atualizações: "Importar 1 cliente e 2 contas"', (await botao().innerText()) === 'Importar 1 cliente e 2 contas', await botao().innerText());
  await p.check('#imp-atualizar'); await p.uncheck('#imp-novos');
  ok('Desmarcar o que é novo: "Importar 2 atualizações"', (await botao().innerText()) === 'Importar 2 atualizações', await botao().innerText());
  await p.check('#imp-novos'); await p.uncheck('#imp-atualizar');
  await botao().click();
  let S1 = await estado();
  ok('Sem atualizar: telefone e valor da Marina continuam iguais', S1.clientes.find(c => c.id === marina.id).telefone === marina.telefone && S1.contas.find(c => c.id === contaM.id).valor === contaM.valor);
  ok('O novo entrou: Bruno com CPF guardado só com números, e 2 contas', S1.clientes.some(c => c.nome === 'Bruno Costa' && c.doc === CPF_BRUNO.replace(/\D/g, '')) && S1.contas.length === S0.contas.length + 2);
  const fim = await texto();
  ok('Conclusão: o que foi gravado, o que ficou de fora e os próximos passos', /Importação concluída/.test(fim) && /Gravado: 1 cliente novo · 2 contas novas/.test(fim) && /6 linhas ficaram de fora/.test(fim), fim);
  await p.click('.sheet [data-act=imp-desfazer]');
  ok('Desfazer pede confirmação dizendo o que sai', /Saem as 2 contas que entraram/.test(await texto()) && /Sai o cliente novo/.test(await texto()));
  await p.click('[data-act=imp-desfazer-sim]'); await p.waitForTimeout(100);
  ok('Desfazer volta clientes e contas exatamente como estavam', base(await estado()) === base(S0));

  // Agora com tudo
  await abrirImport(); await enviar('clientes-novembro.xlsx', arq); await botao().click(); await fechar();
  S1 = await estado();
  const contaDepois = S1.contas.find(c => c.id === contaM.id);
  ok('Com atualização: telefone, valor e baixa mudaram, sem conta repetida', S1.clientes.find(c => c.id === marina.id).telefone === '(11) 91111-2222' && contaDepois.valor === 200 && contaDepois.pagoEm === hoje && S1.contas.length === S0.contas.length + 2);
  ok('Célula vazia na planilha não apaga o que existe (e-mail da Marina)', S1.clientes.find(c => c.id === marina.id).email === marina.email);
  await p.locator('[data-act=import-log]').first().click();
  const log = await texto();
  ok('Registro da importação: mudou, entrou e ficou de fora, por cliente', /O que mudou/i.test(log) && /Marina Alves · 637/.test(log) && /O que entrou/i.test(log) && /Valor \(F6\)/.test(log));
  await fechar();
  await abrirImport(); await enviar('clientes-novembro.xlsx', arq);
  const r2 = await resumo();
  ok('Reimportar: nada novo, nada a atualizar, "Nada para importar"', r2['Clientes novos'] === 0 && r2['Contas novas'] === 0 && r2['Atualizações'] === 0 && r2['Sem mudança'] === 3 && await botao().isDisabled(), JSON.stringify(r2));

  // Corrigir o arquivo de problemas e importar
  const cor = errosArq.aoa.map(l => l.slice());
  cor[1][2] = CPF_LIA; cor[1][3] = 'Lia Souza';  // linha 5
  cor[2][7] = '45,00';                           // linha 6
  cor[3][8] = '13/12/2026';                      // linha 7
  cor[4][9] = '';                                // linha 8
  cor[5][6] = 'Mensalidade 2';                   // linha 9
  cor[6][2] = CPF_FULANO;                        // linha 10
  await abrirImport(); await enviar('corrigido.xlsx', await xlsx(cor));
  const r3 = await resumo();
  ok('Arquivo de problemas corrigido: 2 clientes e 6 contas, 0 problemas', r3['Com problema'] === 0 && r3['Contas novas'] === 6 && r3['Clientes novos'] === 2, JSON.stringify(r3));
  await fechar();

  // ---------- Versão de teste: CPF com dígito errado entra, com aviso
  await abrirImport(); await enviar('cpf-teste.xlsx', await xlsx([['CPF/CNPJ', 'Nome', 'Descrição', 'Valor', 'Vencimento'], ['111.111.111-11', 'Teste Inválido', 'Conta', 10, '01/12/2026']]));
  const rT = await resumo(), tT = await texto();
  ok('Versão de teste: CPF com dígito errado entra, com aviso de teste na linha', rT['Clientes novos'] === 1 && rT['Com problema'] === 0 && /CPF\/CNPJ \(A2\): “111\.111\.111-11” não é um número válido\. Foi aceito só porque esta é a versão de teste/.test(tT), JSON.stringify(rT) + tT.slice(0, 600));
  await fechar();

  // ---------- Atualizar pelo código da conta (planilha para editar)
  const S2 = await estado();
  const doBruno = S2.contas.filter(c => S2.clientes.find(k => k.id === c.clienteId).nome === 'Bruno Costa');
  const outra = S2.contas.find(c => c.clienteId === marina.id);
  const ed = await xlsx([
    ['Código da conta', 'CPF/CNPJ', 'Nome', 'Valor', 'Vencimento', 'Pago em', 'Descrição'],
    [doBruno[0].id, CPF_BRUNO, 'Bruno Costa', '120,00', '05/01/2027', '', 'Mensalidade janeiro'], // valor, vencimento e descrição
    [doBruno[0].id, CPF_BRUNO, 'Bruno Costa', '130,00', '', '', ''],                             // código repetido
    [outra.id, CPF_BRUNO, '', '', '', '', ''],                                                  // CPF de outro cliente
    ['zzzzzzz', '', '', 10, '01/12/2026', '', ''],                                              // código que não existe, sem CPF
    [outra.id, '', '', '', '', '', ''],                                                         // só o código: nada muda
  ]);
  await abrirImport(); await enviar('editar.xlsx', ed);
  const te = await texto();
  ok('Pelo código: muda valor, vencimento e descrição da conta', /Bruno Costa · [\d.-]+\s+Conta: Mensalidade · vence 20\/12\/2026\s+Descrição: Mensalidade\s*→?\s*(para)?\s*Mensalidade janeiro\s+Valor: R\$\s99,90\s*→?\s*(para)?\s*R\$\s120,00\s+Vencimento: 20\/12\/2026\s*→?\s*(para)?\s*05\/01\/2027/.test(te), te.slice(0, 900));
  ok('Código repetido no arquivo é barrado', /Linha 3 · Bruno Costa\s+Código repetido: esta conta já está na linha 2/.test(te));
  ok('CPF que não é do dono da conta é barrado e diz de quem é', /Linha 4 · Marina Alves\s+CPF\/CNPJ \(B4\): não é do dono desta conta\. A conta \w+ é de Marina Alves \(637\.129\.261-70\)/.test(te));
  ok('Código inexistente sem CPF: explica o que fazer', /Código da conta \(A5\): “zzzzzzz” não existe no app e a linha não tem CPF\/CNPJ/.test(te));
  ok('Linha só com o código: conta fica igual (célula vazia não apaga)', (await resumo())['Sem mudança'] === 1);
  await botao().click(); await fechar();
  const S3 = await estado(), c3 = S3.contas.find(c => c.id === doBruno[0].id);
  ok('Gravado pelo código: valor 120, vencimento 2027-01-05, descrição nova', c3.valor === 120 && c3.venc === '2027-01-05' && c3.descricao === 'Mensalidade janeiro');

  // ---------- Avisos do CPF: zero tirado pelo Excel e nome diferente
  const zeros = await xlsx([['CPF', 'Nome', 'Valor', 'Vencimento'], [1234567890, 'Zé Zero', 10, '10/12/2026'], [CPF_MARINA, 'Mariana Alvez', '', '']]);
  await abrirImport(); await enviar('zeros.xlsx', zeros);
  const tz = await texto();
  ok('CPF que perdeu o zero no Excel é aceito e o aviso explica', /CPF\/CNPJ \(A2\): o Excel tirou o zero da frente \(“1234567890”\)\. Usamos 012\.345\.678-90/.test(tz) && /Cliente novo: Zé Zero · 012\.345\.678-90/.test(tz), tz.slice(0, 700));
  ok('CPF de um cliente com nome diferente: avisa que o nome vai mudar', /este CPF é de “Marina Alves” no app\. Se você atualizar, o nome muda para “Mariana Alvez”/.test(tz) && /Nome: Marina Alves\s*→?\s*(para)?\s*Mariana Alvez/.test(tz));
  await fechar();

  // ---------- Colunas com outros nomes, títulos na linha 3, CNPJ
  const outro = await xlsx([
    ['Relatório de cobranças · novembro'], [],
    ['CNPJ', 'Cliente', 'Celular', 'Serviço', 'Valor (R$)', 'Data de vencimento', 'Data de pagamento', 'Observação'],
    ['28.145.347/0001-41', 'Paula Reis ME', '(31) 98888-1111', 'Consultoria', 'R$ 1.234,56', new Date(2026, 11, 5), '', 'x'],
    ['28145347000141', 'Paula Reis ME', '', 'Hora extra', '1,234.50', '06-12-2026', '', ''],
    ['28.145.347/0001-41', 'Paula Reis ME', '', 'Ajuste', '12abc', '07/12/26', '', ''],
  ]);
  await abrirImport();
  ok('Títulos com outros nomes e na linha 3: vai direto para a prévia', /Prévia/.test(await enviar('cobrancas.xlsx', outro)));
  const t2 = await texto();
  ok('Reconhece "CNPJ", "Cliente", "Celular", "Serviço", "Valor (R$)" e as datas', /Títulos na linha 3\. Colunas usadas: CPF\/CNPJ \(A\), Nome \(B\), Telefone \(C\), Descrição \(D\), Valor \(E\), Vencimento \(F\), Pago em \(G\)/.test(t2), t2.slice(0, 500));
  ok('CNPJ com e sem pontuação é o mesmo cliente (1 cliente, 2 contas)', /Cliente novo: Paula Reis ME · 28\.145\.347\/0001-41\s+Cliente novo · \(31\) 98888-1111\s+2 contas novas/.test(t2));
  ok('Valores "R$ 1.234,56" e "1,234.50" e data "06-12-2026" entram certos', /Consultoria · R\$\s1\.234,56 · vence 05\/12\/2026/.test(t2) && /Hora extra · R\$\s1\.234,50 · vence 06\/12\/2026/.test(t2));
  ok('Valor "12abc" é recusado, com a célula', /Valor \(E6\): “12abc” não é um valor/.test(t2));
  await fechar();

  // ---------- Falta coluna: passo 2
  const semDoc = await xlsx([['Contribuinte', 'Cliente', 'Valor', 'Data'], [CPF_RUI, 'Rui Lima', 80, '10/12/2026']]);
  await abrirImport();
  ok('Sem coluna de CPF/CNPJ reconhecível: abre o passo 2', /Conferir colunas/.test(await enviar('rui.xlsx', semDoc)));
  ok('Passo 2 explica que falta o CPF/CNPJ e para que serve', /Não achei a coluna de CPF\/CNPJ\. É ela que diz de qual cliente é cada linha/.test(await p.locator('.sheet .alerta').innerText()));
  ok('Cada opção mostra a letra, o título e um exemplo', /A · Contribuinte — ex\.: [\d.-]+/.test(await p.locator('#map-doc').innerText()));
  await axe('passo 2 (conferir colunas)');
  await p.selectOption('#map-doc', '0');
  await p.selectOption('#map-venc', '2');
  await p.click('[data-act=imp-map-ok]');
  ok('Mesma coluna em dois campos: erro embaixo do campo', /Essa coluna já está em “Valor”/.test(await p.locator('#map-venc-err').innerText().catch(() => '')));
  await p.selectOption('#map-venc', '3');
  await p.click('[data-act=imp-map-ok]');
  ok('Depois de escolher, segue para a prévia certa', /Cliente novo: Rui Lima/.test(await texto()) && /R\$\s80,00 · vence 10\/12\/2026/.test(await texto()));
  await p.click('[data-act=imp-colunas]');
  ok('"Ajustar colunas" volta ao passo 2 com as escolhas feitas', (await p.inputValue('#map-doc')) === '0' && (await p.inputValue('#map-venc')) === '3');
  await p.selectOption('#map-valor', ''); await p.selectOption('#map-venc', '');
  await p.click('[data-act=imp-map-ok]');
  ok('Sem valor e vencimento: importa só clientes e avisa', /só os clientes serão importados/.test(await texto()) && (await botao().innerText()) === 'Importar 1 cliente');
  await botao().click();
  ok('Só cliente: a conclusão leva para a aba Clientes', (await p.locator('.sheet [data-act=go]').innerText()) === 'Ver clientes');
  await fechar();

  // ---------- Várias abas, CSV, arquivos errados
  await abrirImport();
  await enviar('com-abas.xlsx', await xlsx(null, [['Resumo', [['Total do mês'], [1234]]], ['Contas', [['CPF', 'Nome', 'Valor', 'Vencimento'], [cpf('852741963'), 'Ana Dias', 10, '10/12/2026']]]]));
  ok('Várias abas: usa a que tem a tabela e diz qual', /Aba “Contas”/.test(await p.locator('.imp-cols').innerText()));
  const csv = Buffer.from(`CPF;Nome;Descrição;Valor;Vencimento\r\n${cpf('963852741')};João Araújo;Mensalidade;150,00;10/11/2026\r\n`, 'latin1');
  await enviar('contas.csv', csv);
  const tc = await texto();
  ok('CSV com ";" e acentos do Excel (Windows-1252) lido certo', /Cliente novo: João Araújo/.test(tc), tc.slice(0, 300));
  ok('CSV: 10/11/2026 é 10 de novembro', /vence 10\/11\/2026/.test(tc));
  const casos = [
    ['foto.png', Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 13]), 'Esse arquivo não é uma planilha', /É uma imagem/],
    ['comprovante.pdf', Buffer.from('%PDF-1.4 xxx'), 'Esse arquivo não é uma planilha', /É um PDF/],
    ['vazio.xlsx', Buffer.alloc(0), 'O arquivo está vazio', /salve de novo/],
    ['texto.xlsx', Buffer.from('isto não é um excel'), 'Não achei os títulos das colunas', /CPF\/CNPJ, Nome, Valor e Vencimento/],
  ];
  for (const [nome, buf, titulo, re] of casos) {
    const h = await enviar(nome, buf);
    const al = await p.locator('.sheet .alerta').innerText().catch(() => '');
    ok(`Arquivo "${nome}": "${titulo}" e como resolver`, h === titulo && re.test(al) && (await p.locator('.sheet [data-act=pick-xl]').count()) === 1, h + ' / ' + al);
  }
  ok('Arquivo do WhatsApp sem extensão no nome é aceito', /Prévia/.test(await enviar('DOC-20261031-WA0003', arq)));
  await fechar();

  // ---------- Cópia de segurança: ida e volta, e restaurar num celular novo
  await p.click('[data-act=export-backup]'); await p.click('[data-act=xlsx-export]');
  const bk = await salvo(); await fechar();
  const S4 = await estado();
  ok('Cópia de segurança tem o código, o CPF/CNPJ e a aba "Como editar"', bk.aoa[0][0] === 'Código da conta' && bk.aoa[0][1] === 'CPF/CNPJ' && bk.abas.includes('Como editar'));
  ok('Cópia inclui cliente sem contas (Rui Lima)', bk.aoa.some(l => l[2] === 'Rui Lima' && l[0] === ''));
  ok('Cópia: uma linha por conta + clientes sem conta', bk.aoa.length - 1 === S4.contas.length + S4.clientes.filter(c => !S4.contas.some(k => k.clienteId === c.id)).length);
  await abrirImport(); await enviar('backup.xlsx', bk.d);
  const r4 = await resumo();
  ok('Reimportar a cópia no mesmo app: zero mudanças e zero problemas', r4['Clientes novos'] === 0 && r4['Contas novas'] === 0 && r4['Atualizações'] === 0 && r4['Com problema'] === 0 && r4['Sem mudança'] === S4.contas.length, JSON.stringify(r4));
  await fechar();
  // Celular novo: app vazio
  await p.evaluate(() => { localStorage.setItem('baixa-pix-v2', JSON.stringify({ clientes: [], contas: [], ultimoBackup: null })); });
  await p.reload(); await p.waitForSelector('.empty, .crow');
  await abrirImport(); await enviar('backup.xlsx', bk.d);
  const r5 = await resumo();
  ok('Celular novo: a cópia restaura todos os clientes e contas, sem problemas', r5['Com problema'] === 0 && r5['Clientes novos'] === S4.clientes.length && r5['Contas novas'] === S4.contas.length, JSON.stringify(r5) + ` esperado ${S4.clientes.length}/${S4.contas.length}`);
  await botao().click(); await fechar();
  const S5 = await estado();
  const chave = s => s.contas.map(c => { const k = s.clientes.find(x => x.id === c.clienteId); return [c.id, k.doc, k.nome, c.descricao, c.valor, c.venc, c.pagoEm || ''].join('|'); }).sort().join('\n');
  ok('Restaurado: mesmas contas, com os mesmos códigos, valores, datas e baixas', chave(S5) === chave(S4));
  ok('Restaurado: a próxima planilha para editar continua valendo (códigos iguais)', S4.contas.every(c => S5.contas.some(k => k.id === c.id)));

  // ---------- Desfazer pelo histórico, preservando o que foi usado depois
  await abrirImport();
  await enviar('lote.xlsx', await xlsx([['CPF', 'Nome', 'Valor', 'Vencimento'], [cpf('111222333'), 'Lote Um', 10, '10/12/2026'], [cpf('444555666'), 'Lote Dois', 20, '11/12/2026']]));
  await botao().click(); await fechar();
  const S6 = await estado();
  const loteDois = S6.contas.find(c => c.valor === 20 && c.venc === '2026-12-11');
  await p.evaluate(id => { const s = JSON.parse(localStorage.getItem('baixa-pix-v2')); s.contas.find(c => c.id === id).pagoEm = '2026-10-01'; localStorage.setItem('baixa-pix-v2', JSON.stringify(s)); }, loteDois.id);
  await p.reload(); await p.waitForSelector('.crow');
  await p.click('button[data-tab=planilha]');
  await p.locator('[data-act=import-log]').first().click();
  await p.click('.sheet [data-act=imp-desfazer]');
  await p.click('[data-act=imp-desfazer-sim]');
  const S7 = await estado();
  ok('Desfazer pelo histórico tira o que não foi mexido e mantém o que foi usado', !S7.clientes.some(c => c.nome === 'Lote Um') && S7.contas.some(c => c.id === loteDois.id) && S7.clientes.some(c => c.nome === 'Lote Dois'));

  // ---------- Layout em 320 px
  await p.setViewportSize({ width: 320, height: 640 });
  await abrirImport(); await enviar('clientes-novembro.xlsx', arq);
  ok('Prévia sem rolagem horizontal em 320 px', (await p.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)) <= 0);
  const toque = await p.evaluate(() => [...document.querySelectorAll('.sheet button, .sheet .toggle, .sheet summary')].filter(e => e.offsetParent).map(e => e.getBoundingClientRect().height).filter(h => h < 44).length);
  ok('Todos os toques da prévia com ≥ 44 px', toque === 0, `${toque} pequenos`);

  ok('Sem erros de JavaScript', errs.length === 0, errs.join(' | '));
  await b.close();
  console.log(out.join('\n'));
  if (out.some(l => l.startsWith('✖'))) process.exitCode = 1;
})().catch(e => { console.error('ERRO NO TESTE', e); process.exit(1); });
