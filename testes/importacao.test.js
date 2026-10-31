// Importação em 3 passos (Arquivo → Colunas → Conferir): prévia, atualização de dados, erros com linha,
// coluna e célula, planilha com as linhas com problema, colunas com outros nomes, CSV brasileiro,
// arquivos errados, desfazer (logo depois e pelo histórico) e acessibilidade.
const { chromium } = require('playwright');
const fs = require('fs');
const AXE = fs.readFileSync(__dirname + '/vendor/axe.js', 'utf8');
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
  const resumo = async () => Object.fromEntries((await p.$$eval('.imp-res li', l => l.map(x => [x.querySelector('span').textContent, +x.querySelector('b').textContent]))));
  const botao = () => p.locator('[data-act=import-confirm]');
  const salvo = async () => { await p.waitForFunction(() => window.__salvo); const r = await p.evaluate(() => { const w = XLSX.read(new Uint8Array(window.__salvo.d), { type: 'array' }); return { nome: window.__salvo.nome, abas: w.SheetNames, aoa: XLSX.utils.sheet_to_json(w.Sheets[w.SheetNames[0]], { header: 1, defval: '' }) }; }); await p.evaluate(() => { window.__salvo = null; }); return r; };
  const fechar = () => p.evaluate(() => document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' })));
  const axe = async (rotulo) => {
    for (const tema of ['light', 'dark']) {
      await p.evaluate(t => document.documentElement.setAttribute('data-theme', t), tema);
      await p.waitForTimeout(450);
      await p.addScriptTag({ content: AXE }).catch(() => {});
      const r = await p.evaluate(async () => await axe.run(document.querySelector('.sheet') || document, { runOnly: ['wcag2a', 'wcag2aa', 'wcag21aa'] }));
      ok(`Acessibilidade (${tema === 'light' ? 'claro' : 'escuro'}): ${rotulo}`, r.violations.length === 0, r.violations.map(x => `${x.id}(${x.nodes.length})`).join(' | '));
    }
    await p.evaluate(() => document.documentElement.setAttribute('data-theme', 'light'));
  };

  // ---------- Tela Planilha: três tarefas separadas
  const tela = await p.locator('#screen').innerText();
  ok('Planilha mostra três tarefas: importar, exportar e cópia de segurança', (await p.locator('#screen .task').count()) === 3 && /Importar planilha/.test(tela) && /Exportar contas/.test(tela) && /Cópia de segurança/.test(tela));
  ok('Só a importação tem botão de destaque (uma ação principal por tela)', (await p.locator('#screen .btn.primary').count()) === 1 && /Importar planilha/.test(await p.locator('#screen .btn.primary').innerText()));
  ok('Filtros de exportação não ficam mais soltos na tela', (await p.locator('#screen #xf-cli').count()) === 0);
  ok('Cópia de segurança diz o estado ("Nunca feita")', /Nunca feita/.test(tela));

  // ---------- Modelo
  await p.click('[data-act=import-modelo]');
  const modelo = await salvo();
  ok('Planilha modelo: arquivo com as 7 colunas na ordem do app', modelo.nome === 'modelo-importacao-celsopix.xlsx' && JSON.stringify(modelo.aoa[0]) === JSON.stringify(['Nome', 'Telefone', 'E-mail', 'Descrição', 'Valor', 'Vencimento', 'Pago em']), modelo.nome + ' ' + JSON.stringify(modelo.aoa[0]));
  ok('Planilha modelo traz a aba "Como preencher"', modelo.abas.includes('Como preencher'));

  // ---------- Passo 1
  await p.click('[data-act=import-abrir]');
  const p1 = await p.locator('.sheet').innerText();
  ok('Passo 1 mostra as etapas e em qual a pessoa está', (await p.locator('.passos li[aria-current=step]').innerText()).includes('Arquivo'));
  ok('Passo 1 explica as colunas, o que é obrigatório e dá exemplo', /Nome do cliente\s*obrigatória/.test(p1) && /Vencimento\s*para contas/.test(p1) && /150,00/.test(p1), p1.slice(0, 300));
  ok('Passo 1 sugere fazer cópia de segurança antes (nunca feita)', /faça uma cópia de segurança antes/i.test(p1));
  await axe('passo 1 (escolher arquivo)');

  const S0 = await estado();
  const marina = S0.clientes.find(c => c.nome === 'Marina Alves');
  const contaM = S0.contas.filter(c => c.clienteId === marina.id && !c.pagoEm).sort((a, c) => a.venc < c.venc ? -1 : 1)[0];
  const br = iso => iso.split('-').reverse().join('/');
  const hoje = await p.evaluate(() => { const d = new Date(); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); });
  const amanha = await p.evaluate(() => { const d = new Date(); d.setDate(d.getDate() + 1); return d.toLocaleDateString('pt-BR'); });

  const arq = await xlsx([
    ['Nome', 'Telefone', 'E-mail', 'Descrição', 'Valor', 'Vencimento', 'Pago em'],
    ['marina  alves', '(11) 91111-2222', '', contaM.descricao, '200,00', br(contaM.venc), br(hoje)], // 2: atualiza
    ['Marina Alves', '', '', 'Taxa extra', 75, '15/12/2026', ''],                                      // 3: conta nova
    ['Bruno Costa', '(21) 90000-1234', 'bruno@exemplo.com', 'Mensalidade', 99.9, '20/12/2026', ''],   // 4: cliente + conta novos
    ['', '', '', 'Sem nome', 10, '01/12/2026', ''],                                                    // 5: sem nome
    ['Bruno Costa', '', '', 'Errada', 'abc', '01/12/2026', ''],                                        // 6: valor
    ['Bruno Costa', '', '', 'Errada 2', 10, '32/13/2026', ''],                                         // 7: data
    ['Bruno Costa', '', '', 'Futuro', 10, '01/12/2026', amanha],                                       // 8: pago no futuro
    ['Bruno Costa', '', '', 'Mensalidade', 99.9, '20/12/2026', ''],                                    // 9: repetida
    ['', '', '', '', '', '', ''],                                                                      // 10: em branco
  ]);
  ok('Planilha com títulos certos vai direto para a prévia (passo 3)', /Prévia da importação/.test(await enviar('clientes-novembro.xlsx', arq)) && (await p.locator('.passos li[aria-current=step]').innerText()).includes('Conferir'));
  ok('Prévia: nada é gravado antes de confirmar', JSON.stringify(await estado()) === JSON.stringify(S0));
  ok('Prévia mostra o arquivo e avisa que nada muda até confirmar', /clientes-novembro\.xlsx\. Nada muda até você confirmar/.test(await p.locator('.sheet .sub').innerText()));
  const r = await resumo();
  ok('Resumo: 1 cliente novo, 2 contas novas, 2 atualizações, 5 com problema', r['Clientes novos'] === 1 && r['Contas novas'] === 2 && r['Atualizações'] === 2 && r['Com problema'] === 5, JSON.stringify(r));
  const txt = (await p.locator('.sheet').innerText()).replace(/ /g, ' ');
  ok('Prévia diz quais colunas usou e onde estão os títulos', /Títulos na linha 1\. Colunas usadas: Nome \(A\), Telefone \(B\), E-mail \(C\), Descrição \(D\), Valor \(E\), Vencimento \(F\), Pago em \(G\)/.test(txt));
  ok('Aviso de problemas aparece logo no topo, com "Ver problemas" e "Baixar"', /5 linhas com problema ficam de fora/.test(await p.locator('.sheet .alerta').first().innerText()) && (await p.locator('[data-act=imp-ver-prob]').count()) === 1 && (await p.locator('[data-act=imp-erros]').count()) === 1);
  ok('Atualização de cliente mostra antes e depois do telefone', new RegExp(`Telefone: ${marina.telefone.replace(/[()]/g, '\\$&')}\\s*→?\\s*(para)?\\s*\\(11\\) 91111-2222`).test(txt));
  ok('Atualização de conta mostra valor antigo e novo', /Valor: R\$\s180,00\s*→?\s*(para)?\s*R\$\s200,00/.test(txt));
  ok('Atualização de conta mostra a baixa: em aberto → paga', new RegExp(`Baixa: em aberto\\s*→?\\s*(para)?\\s*paga em ${br(hoje)}`).test(txt));
  ok('Nome escrito diferente ("marina  alves") é reconhecido como a mesma cliente', !/Cliente: marina/.test(txt));
  for (const [linha, motivo] of [
    [5, 'Linha 5\\s+Nome \\(A5\\): está vazio\\. Preencha o nome do cliente'],
    [6, 'Linha 6 · Bruno Costa\\s+Valor \\(E6\\): “abc” não é um valor\\. Use só números, por exemplo 150,00'],
    [7, 'Linha 7 · Bruno Costa\\s+Vencimento \\(F7\\): “32/13/2026” não é uma data válida\\. Use dia/mês/ano'],
    [8, 'Linha 8 · Bruno Costa\\s+Pago em \\(G8\\): \\d\\d/\\d\\d/\\d{4} é depois de hoje\\. Se ainda não foi paga, deixe vazio'],
    [9, 'Linha 9 · Bruno Costa\\s+Conta repetida: igual à linha 4']]) {
    ok(`Linha ${linha}: diz a linha, a coluna, a célula, o porquê e como corrigir`, new RegExp(motivo).test(txt), motivo);
  }
  ok('Linha em branco é ignorada sem virar erro', !/Linha 10/.test(txt));
  await p.click('[data-act=imp-ver-prob]');
  await p.waitForTimeout(150);
  ok('"Ver problemas" leva até a lista e põe o foco nela', await p.evaluate(() => document.activeElement.id === 'imp-prob'));
  const fixo = await botao().boundingBox();
  ok('Botão de importar fica sempre visível na parte de baixo, mesmo com a lista rolada', fixo && fixo.y + fixo.height <= 844 && fixo.y > 600, JSON.stringify(fixo));
  ok('Botão diz quantas mudanças vai gravar', (await botao().innerText()) === 'Importar 5 mudanças', await botao().innerText());
  await axe('prévia com problemas');

  // Baixar as linhas com problema, corrigir e importar de novo
  await p.click('[data-act=imp-erros]');
  const errosArq = await salvo();
  ok('Linhas com problema: arquivo com nome claro', /^linhas-com-problema-clientes-novembro-\d{4}-\d{2}-\d{2}\.xlsx$/.test(errosArq.nome), errosArq.nome);
  ok('Linhas com problema: colunas "Linha no arquivo", "Problema" e as originais', JSON.stringify(errosArq.aoa[0]) === JSON.stringify(['Linha no arquivo', 'Problema', 'Nome', 'Telefone', 'E-mail', 'Descrição', 'Valor', 'Vencimento', 'Pago em']), JSON.stringify(errosArq.aoa[0]));
  ok('Linhas com problema: as 5 linhas, cada uma com o motivo e a célula', errosArq.aoa.length === 6 && errosArq.aoa[2][0] === 6 && /Valor \(E6\)/.test(errosArq.aoa[2][1]), JSON.stringify(errosArq.aoa[2]));

  // Desmarcar "Atualizar estes dados": só entra o que é novo
  await p.uncheck('#imp-atualizar');
  ok('Desmarcar atualizações recalcula o botão', (await botao().innerText()) === 'Importar 3 mudanças', await botao().innerText());
  await botao().click();
  let S1 = await estado();
  ok('Sem atualizar: telefone e valor da Marina continuam iguais', S1.clientes.find(c => c.id === marina.id).telefone === marina.telefone && S1.contas.find(c => c.id === contaM.id).valor === contaM.valor && !S1.contas.find(c => c.id === contaM.id).pagoEm);
  ok('Sem atualizar: o que é novo entrou (Bruno e 2 contas)', S1.clientes.length === S0.clientes.length + 1 && S1.contas.length === S0.contas.length + 2);
  const fim = await p.locator('.sheet').innerText();
  ok('Tela de conclusão: o que foi gravado, o que ficou de fora e os próximos passos', /Importação concluída/.test(fim) && /3 mudanças gravadas/.test(fim) && /5 linhas ficaram de fora/.test(fim) && (await p.locator('.sheet [data-act=imp-desfazer]').count()) === 1 && (await p.locator('.sheet [data-act=go][data-tab=contas]').count()) === 1, fim);
  await axe('importação concluída');

  // Desfazer logo depois
  await p.click('.sheet [data-act=imp-desfazer]');
  const conf = await p.locator('.sheet').innerText();
  ok('Desfazer pede confirmação dizendo o que vai sair', /Desfazer a importação\?/.test(conf) && /Saem as 2 contas que entraram/.test(conf) && /Sai o cliente novo/.test(conf), conf);
  await p.click('[data-act=imp-desfazer-sim]');
  await p.waitForTimeout(100);
  ok('Desfazer volta clientes e contas exatamente como estavam', base(await estado()) === base(S0));
  ok('Histórico guarda a importação marcada como desfeita', /Desfeita · /.test(await p.locator('[data-act=import-log]').first().innerText()));

  // Importar de novo, agora atualizando
  await p.click('[data-act=import-abrir]');
  await enviar('clientes-novembro.xlsx', arq);
  await botao().click();
  await fechar();
  S1 = await estado();
  const contaDepois = S1.contas.find(c => c.id === contaM.id);
  ok('Com atualização: telefone da Marina mudou', S1.clientes.find(c => c.id === marina.id).telefone === '(11) 91111-2222');
  ok('Com atualização: valor e baixa mudaram, sem conta repetida', contaDepois.valor === 200 && contaDepois.pagoEm === hoje && S1.contas.length === S0.contas.length + 2);
  ok('Contato vazio na planilha não apaga o que já existe', S1.clientes.find(c => c.id === marina.id).email === (marina.email || ''));

  // Histórico
  const hist = p.locator('[data-act=import-log]');
  ok('Item do histórico resume o que entrou e mudou', /1 cliente novo · 2 contas novas · 2 atualizações · 5 linhas com problema/.test(await hist.first().innerText()), await hist.first().innerText());
  await hist.first().click();
  const log = await p.locator('.sheet').innerText();
  ok('Registro lista o que mudou, com antes e depois', /O que mudou/i.test(log) && /Telefone:/.test(log) && /Valor:/.test(log));
  ok('Registro lista as linhas de fora com coluna e célula', /Linhas que ficaram de fora/i.test(log) && /Valor \(E6\)/.test(log));
  ok('Registro da importação mais recente oferece "Desfazer esta importação"', (await p.locator('.sheet [data-act=imp-desfazer]').count()) === 1);
  await fechar();
  await hist.nth(1).click();
  ok('Importação já desfeita não oferece desfazer de novo', (await p.locator('.sheet [data-act=imp-desfazer]').count()) === 0 && /Desfeita em/.test(await p.locator('.sheet').innerText()));
  await fechar();

  // Reimportar o mesmo arquivo: nada a mudar
  await p.click('[data-act=import-abrir]');
  await enviar('clientes-novembro.xlsx', arq);
  const r2 = await resumo();
  ok('Reimportar após aplicar: nada novo, nada a atualizar', r2['Clientes novos'] === 0 && r2['Contas novas'] === 0 && r2['Atualizações'] === 0 && r2['Sem mudança'] === 3, JSON.stringify(r2));
  ok('Botão "Nada para importar" fica desativado', await botao().isDisabled());
  ok('Contas iguais ficam recolhidas', (await p.locator('details.imp-iguais').count()) === 1 && !(await p.locator('details.imp-iguais').evaluate(d => d.open)));
  await fechar();

  // Corrigir o arquivo de problemas e importar: entra sem nenhum problema
  const corrigido = errosArq.aoa.map((l, i) => i === 0 ? l : l.slice());
  corrigido[1][2] = 'Lia Souza';           // linha 5: nome
  corrigido[2][6] = '45,00';               // linha 6: valor
  corrigido[3][7] = '13/12/2026';          // linha 7: data
  corrigido[4][8] = '';                    // linha 8: pago em
  corrigido[5][5] = 'Mensalidade 2';       // linha 9: descrição diferente
  await p.click('[data-act=import-abrir]');
  await enviar('linhas-corrigidas.xlsx', await xlsx(corrigido));
  const r3 = await resumo();
  ok('Arquivo de problemas corrigido: importa direto, com 0 problemas', r3['Com problema'] === 0 && r3['Contas novas'] === 5, JSON.stringify(r3));
  ok('Colunas "Linha no arquivo" e "Problema" são ignoradas sozinhas', /Colunas usadas: Nome \(C\)/.test(await p.locator('.imp-cols').innerText()));
  await fechar();

  // ---------- Colunas com outros nomes, títulos na linha 3, valores em vários formatos
  const outro = await xlsx([
    ['Relatório de cobranças · novembro'], [],
    ['Cliente', 'Celular', 'Serviço', 'Valor (R$)', 'Data de vencimento', 'Data de pagamento', 'Observação'],
    ['Paula Reis', '(31) 98888-1111', 'Consultoria', 'R$ 1.234,56', new Date(2026, 11, 5), '', 'x'],
    ['Paula Reis', '', 'Hora extra', '1,234.50', '06-12-2026', '', ''],
    ['Paula Reis', '', 'Ajuste', '12abc', '07/12/26', '', ''],
    ['Paula Reis', '', 'Estorno', '-5', '08/12/2026', '', ''],
    ['Paula Reis', 'abc@', 'Taxa', 50, '2026-12-09', '', ''],
  ]);
  ok('Títulos com outros nomes e na linha 3: reconhece tudo e vai para a prévia', /Prévia/.test(await enviar('cobrancas.xlsx', outro)));
  const t2 = (await p.locator('.sheet').innerText()).replace(/ /g, ' ');
  ok('Diz que os títulos estão na linha 3 e quais colunas usou', /Títulos na linha 3\. Colunas usadas: Nome \(A\), Telefone \(B\), Descrição \(C\), Valor \(D\), Vencimento \(E\), Pago em \(F\)/.test(t2), t2.slice(0, 600));
  ok('Valor "R$ 1.234,56" e data do Excel entram certos', /Consultoria\s+R\$\s1\.234,56 · vence 05\/12\/2026/.test(t2));
  ok('Valor "1,234.50" (formato americano) e data "06-12-2026" entram certos', /Hora extra\s+R\$\s1\.234,50 · vence 06\/12\/2026/.test(t2));
  ok('Valor "12abc" é recusado, com a célula', /Linha 6 · Paula Reis\s+Valor \(D6\): “12abc” não é um valor/.test(t2));
  ok('Valor negativo é recusado e explica', /Valor \(D7\): “-5” precisa ser maior que zero/.test(t2));
  ok('E-mail estranho não barra a linha: vira aviso "Para conferir"', /Para conferir/i.test(t2) && /Linha 8 · Paula Reis\s+Telefone \(B8\): “abc@” tem poucos dígitos/.test(t2));
  ok('Coluna desconhecida ("Observação") não atrapalha', !/Observação/.test(await p.locator('.imp-cols').innerText()));
  await fechar();

  // ---------- Falta uma coluna: passo 2 pede para escolher
  const semVenc = await xlsx([['Nome', 'Valor', 'Quando vence a parcela?'], ['Rui Lima', 80, '10/12/2026']]);
  ok('Título com palavra-chave ("Quando vence a parcela?") é reconhecido como vencimento', /Prévia/.test(await enviar('rui.xlsx', semVenc)) && /Vencimento \(C\)/.test(await p.locator('.imp-cols').innerText()));
  await fechar();
  const ruim = await xlsx([['Cliente', 'Valor', 'Data'], ['Rui Lima', 80, '10/12/2026']]);
  ok('Sem coluna de vencimento reconhecível: passo 2', /Conferir colunas/.test(await enviar('rui2.xlsx', ruim)));
  ok('Passo 2 marca a etapa "Colunas" e explica o que falta', (await p.locator('.passos li[aria-current=step]').innerText()).includes('Colunas') && /Achei a coluna do valor, mas não a do vencimento/.test(await p.locator('.sheet .alerta').innerText()));
  ok('Cada opção mostra a letra, o título e um exemplo da coluna', /C · Data — ex\.: 10\/12\/2026/.test(await p.locator('#map-venc').innerText()));
  await axe('passo 2 (conferir colunas)');
  await p.selectOption('#map-venc', '1');
  await p.click('[data-act=imp-map-ok]');
  ok('Mesma coluna em dois campos: erro embaixo do campo', /Essa coluna já está em “Valor”/.test(await p.locator('#map-venc-err').innerText().catch(() => '')));
  await p.selectOption('#map-venc', '2');
  await p.click('[data-act=imp-map-ok]');
  ok('Depois de escolher a coluna, segue para a prévia com a conta certa', /Prévia/.test(await p.locator('.sheet h2').innerText()) && /Conta: Rui Lima · Conta\s+R\$\s80,00 · vence 10\/12\/2026/.test((await p.locator('.sheet').innerText()).replace(/ /g, ' ')));
  await p.click('[data-act=imp-colunas]');
  ok('"Ajustar colunas" volta ao passo 2 com a escolha feita', (await p.inputValue('#map-venc')) === '2');
  await p.selectOption('#map-valor', ''); await p.selectOption('#map-venc', '');
  await p.click('[data-act=imp-map-ok]');
  ok('Sem valor e vencimento: importa só clientes e avisa', /só os clientes serão importados/.test(await p.locator('.sheet').innerText()));
  await fechar();

  // ---------- Várias abas: usa a que tem os dados
  const abas = await xlsx(null, [['Resumo', [['Total do mês'], [1234]]], ['Contas', [['Nome', 'Valor', 'Vencimento'], ['Ana Dias', 10, '10/12/2026']]]]);
  await enviar('com-abas.xlsx', abas);
  ok('Várias abas: escolhe a que tem a tabela e diz qual', /Aba “Contas”/.test(await p.locator('.imp-cols').innerText()));
  await fechar();

  // ---------- CSV do Excel brasileiro: ponto e vírgula, acentos em Windows-1252, datas dd/mm
  const csv = Buffer.from('Nome;Descrição;Valor;Vencimento\r\nJoão Araújo;Mensalidade;150,00;10/11/2026\r\n', 'latin1');
  await enviar('contas.csv', csv);
  const tc = (await p.locator('.sheet').innerText()).replace(/ /g, ' ');
  ok('CSV com ";" e acentos do Excel (Windows-1252) lido certo', /Cliente: João Araújo/.test(tc), tc.slice(0, 400));
  ok('CSV: 10/11/2026 é 10 de novembro, não 11 de outubro', /vence 10\/11\/2026/.test(tc));
  await fechar();

  // ---------- Arquivos errados: mensagem que diz o que fazer, sem fechar sozinha
  const casos = [
    ['foto.png', Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 13]), 'Esse arquivo não é uma planilha', /É uma imagem/],
    ['comprovante.pdf', Buffer.from('%PDF-1.4 xxx'), 'Esse arquivo não é uma planilha', /É um PDF/],
    ['vazio.xlsx', Buffer.alloc(0), 'O arquivo está vazio', /salve de novo/],
    ['texto.xlsx', Buffer.from('isto não é um excel'), 'Não achei os títulos das colunas', /primeira linha.*Nome, Valor e Vencimento/],
    ['so-titulos.xlsx', Buffer.from(await xlsx([['Nome', 'Valor', 'Vencimento']])), 'A planilha está sem dados', /Achei os títulos na linha 1/],
  ];
  for (const [nome, buf, titulo, re] of casos) {
    const h = await enviar(nome, buf);
    const al = await p.locator('.sheet .alerta').innerText().catch(() => '');
    ok(`Arquivo "${nome}": "${titulo}" e como resolver`, h === titulo && re.test(al) && (await p.locator('.sheet [data-act=pick-xl]').count()) === 1, h + ' / ' + al);
  }
  await fechar();
  ok('Arquivo do WhatsApp sem extensão no nome é aceito', /Prévia/.test(await enviar('DOC-20261031-WA0003', arq)));
  await fechar();

  // ---------- Backup ida e volta: zero mudanças
  await p.click('[data-act=export-backup]'); await p.click('[data-act=xlsx-export]');
  await p.waitForFunction(() => window.__salvo);
  const backup = await p.evaluate(() => window.__salvo.d); await p.evaluate(() => { window.__salvo = null; });
  await fechar();
  ok('Cópia de segurança muda o estado para "Feita hoje"', /Feita hoje/.test(await p.locator('#screen').innerText()));
  await p.click('[data-act=import-abrir]');
  ok('Com cópia recente, o passo 1 não insiste na dica de cópia', !/faça uma cópia de segurança antes/i.test(await p.locator('.sheet').innerText()));
  await enviar('backup.xlsx', backup);
  const r4 = await resumo();
  const S2 = await estado();
  ok('Reimportar a própria cópia de segurança: zero mudanças e zero problemas', r4['Clientes novos'] === 0 && r4['Contas novas'] === 0 && r4['Atualizações'] === 0 && r4['Com problema'] === 0 && r4['Sem mudança'] === S2.contas.length, JSON.stringify(r4) + ' contas=' + S2.contas.length);
  await fechar();

  // ---------- Desfazer pelo histórico, dias depois, preservando o que mudou depois
  await p.click('[data-act=import-abrir]');
  await enviar('lote.xlsx', await xlsx([['Nome', 'Valor', 'Vencimento'], ['Lote Um', 10, '10/12/2026'], ['Lote Dois', 20, '11/12/2026']]));
  await botao().click(); await fechar();
  const S3 = await estado();
  const loteDois = S3.contas.find(c => c.valor === 20 && c.venc === '2026-12-11');
  await p.evaluate(id => { const s = JSON.parse(localStorage.getItem('baixa-pix-v2')); s.contas.find(c => c.id === id).pagoEm = s.contas[0].venc; localStorage.setItem('baixa-pix-v2', JSON.stringify(s)); }, loteDois.id);
  await p.reload(); await p.waitForSelector('.crow');
  await p.click('button[data-tab=planilha]');
  await p.locator('[data-act=import-log]').first().click();
  await p.click('.sheet [data-act=imp-desfazer]');
  await p.click('[data-act=imp-desfazer-sim]');
  const S4 = await estado();
  ok('Desfazer pelo histórico (depois de recarregar o app) tira o que não foi mexido', !S4.clientes.some(c => c.nome === 'Lote Um') && !S4.contas.some(c => c.valor === 10 && c.venc === '2026-12-10'));
  ok('Conta que recebeu baixa depois da importação fica, com o cliente', S4.contas.some(c => c.id === loteDois.id && c.pagoEm) && S4.clientes.some(c => c.nome === 'Lote Dois'));
  ok('Aviso diz que o que foi usado depois ficou', /Importação desfeita\. 2 itens que você usou depois ficaram/.test(await p.locator('.toast').innerText()), await p.locator('.toast').innerText());

  // ---------- Layout em 320 px
  await p.setViewportSize({ width: 320, height: 640 });
  await p.click('[data-act=import-abrir]');
  await enviar('clientes-novembro.xlsx', arq);
  const sobra = await p.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  ok('Prévia sem rolagem horizontal em 320 px', sobra <= 0, `${sobra}px`);
  const toque = await p.evaluate(() => [...document.querySelectorAll('.sheet button, .sheet .toggle, .sheet summary')].filter(e => e.offsetParent).map(e => e.getBoundingClientRect().height).filter(h => h < 44).length);
  ok('Todos os toques da prévia com ≥ 44 px', toque === 0, `${toque} pequenos`);
  await p.click('[data-act=imp-colunas]');
  ok('Passo 2 sem rolagem horizontal em 320 px', (await p.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)) <= 0);

  ok('Sem erros de JavaScript', errs.length === 0, errs.join(' | '));
  await b.close();
  console.log(out.join('\n'));
  if (out.some(l => l.startsWith('✖'))) process.exitCode = 1;
})().catch(e => { console.error('ERRO NO TESTE', e); process.exit(1); });
