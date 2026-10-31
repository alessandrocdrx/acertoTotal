// Importação com prévia, atualização de dados existentes, linhas com problema, histórico e desfazer.
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
    window.claude = { use: async n => n === 'downloads' ? { save: async r => { window.__salvo = Array.from(new Uint8Array(r.data)); return { status: 'saved' }; } } : null };
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
  const xlsx = aoa => p.evaluate(a => {
    const ws = XLSX.utils.aoa_to_sheet(a); const wb = XLSX.utils.book_new(); XLSX.utils.book_append_sheet(wb, ws, 'S');
    return Array.from(new Uint8Array(XLSX.write(wb, { type: 'array', bookType: 'xlsx' })));
  }, aoa);
  const enviar = async (nome, bytes) => {
    await p.setInputFiles('#xl', { name: nome, mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', buffer: Buffer.from(bytes) });
    await p.waitForSelector('.sheet h2:has-text("Prévia da importação")');
    await p.waitForTimeout(450);
  };
  const resumo = async () => Object.fromEntries((await p.$$eval('.imp-res li', l => l.map(x => [x.querySelector('span').textContent, +x.querySelector('b').textContent]))));
  const botao = () => p.locator('[data-act=import-confirm]');

  const S0 = await estado();
  const marina = S0.clientes.find(c => c.nome === 'Marina Alves');
  const contaM = S0.contas.filter(c => c.clienteId === marina.id && !c.pagoEm).sort((a, c) => a.venc < c.venc ? -1 : 1)[0];
  const br = iso => iso.split('-').reverse().join('/');
  const hoje = await p.evaluate(() => { const d = new Date(); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); });
  const amanha = await p.evaluate(() => { const d = new Date(); d.setDate(d.getDate() + 1); return d.toLocaleDateString('pt-BR'); });

  // Planilha com: atualização de telefone, atualização de valor + baixa, conta nova, cliente novo,
  // linhas com problema (sem nome, valor inválido, data inválida, pago no futuro, repetida) e linha em branco.
  const arq = await xlsx([
    ['Nome', 'Telefone', 'E-mail', 'Descrição', 'Valor', 'Vencimento', 'Pago em'],
    ['marina  alves', '(11) 91111-2222', '', contaM.descricao, '200,00', br(contaM.venc), br(hoje)], // linha 2: atualiza
    ['Marina Alves', '', '', 'Taxa extra', 75, '15/12/2026', ''],                                      // linha 3: conta nova
    ['Bruno Costa', '(21) 90000-1234', 'bruno@exemplo.com', 'Mensalidade', 99.9, '20/12/2026', ''],   // linha 4: cliente + conta novos
    ['', '', '', 'Sem nome', 10, '01/12/2026', ''],                                                    // linha 5: erro
    ['Bruno Costa', '', '', 'Errada', 'abc', '01/12/2026', ''],                                        // linha 6: erro valor
    ['Bruno Costa', '', '', 'Errada 2', 10, '32/13/2026', ''],                                         // linha 7: erro data
    ['Bruno Costa', '', '', 'Futuro', 10, '01/12/2026', amanha],                                       // linha 8: erro pago no futuro
    ['Bruno Costa', '', '', 'Mensalidade', 99.9, '20/12/2026', ''],                                    // linha 9: repetida
    ['', '', '', '', '', '', ''],                                                                      // linha 10: em branco, ignorada
  ]);
  await enviar('clientes-novembro.xlsx', arq);

  ok('Prévia abre e nada é gravado antes de confirmar', JSON.stringify(await estado()) === JSON.stringify(S0));
  ok('Prévia mostra o nome do arquivo e avisa que nada muda até confirmar', /clientes-novembro\.xlsx\. Nada muda até você confirmar/.test(await p.locator('.sheet .sub').innerText()));
  const r = await resumo();
  ok('Resumo: 1 cliente novo, 2 contas novas, 2 atualizações, 5 com problema', r['Clientes novos'] === 1 && r['Contas novas'] === 2 && r['Atualizações'] === 2 && r['Com problema'] === 5, JSON.stringify(r));
  const txt = await p.locator('.sheet').innerText();
  ok('Atualização de cliente mostra antes e depois do telefone', new RegExp(`Telefone: ${marina.telefone.replace(/[()]/g, '\\$&')}\\s*→?\\s*(para)?\\s*\\(11\\) 91111-2222`).test(txt), txt.slice(0, 300));
  ok('Atualização de conta mostra valor antigo e novo', /Valor: R\$\s180,00\s*→?\s*(para)?\s*R\$\s200,00/.test(txt.replace(/ /g, ' ')));
  ok('Atualização de conta mostra a baixa: em aberto → paga', new RegExp(`Baixa: em aberto\\s*→?\\s*(para)?\\s*paga em ${br(hoje)}`).test(txt));
  ok('Nome escrito diferente ("marina  alves") é reconhecido como a mesma cliente', !/Cliente: marina/.test(txt));
  ok('Valor antigo riscado (del) e novo destacado (ins)', (await p.locator('.imp-it.atualizado del').count()) >= 3 && (await p.locator('.imp-it.atualizado ins').count()) >= 3);
  for (const [linha, motivo] of [[5, 'Falta o nome do cliente'], [6, 'o valor “abc” não é um número válido'], [7, 'o vencimento “32/13/2026” não é uma data'], [8, '“Pago em” é depois de hoje'], [9, 'Conta repetida no arquivo']]) {
    ok(`Linha ${linha} com problema aparece com o motivo`, new RegExp(`Linha ${linha}\\s+.*${motivo.replace(/[()“”]/g, '.')}`, 'i').test(txt), motivo);
  }
  ok('Linha em branco é ignorada sem virar erro', !/Linha 10/.test(txt));
  ok('Botão diz quantas mudanças vai gravar', (await botao().innerText()) === 'Importar 5 mudanças', await botao().innerText());

  // Desmarcar "Atualizar estes dados": só entra o que é novo
  await p.uncheck('#imp-atualizar');
  ok('Desmarcar atualizações recalcula o botão', (await botao().innerText()) === 'Importar 3 mudanças', await botao().innerText());
  await botao().click();
  let S1 = await estado();
  ok('Sem atualizar: telefone e valor da Marina continuam iguais', S1.clientes.find(c => c.id === marina.id).telefone === marina.telefone && S1.contas.find(c => c.id === contaM.id).valor === contaM.valor && !S1.contas.find(c => c.id === contaM.id).pagoEm);
  ok('Sem atualizar: o que é novo entrou (Bruno e 2 contas)', S1.clientes.length === S0.clientes.length + 1 && S1.contas.length === S0.contas.length + 2);
  ok('Depois de importar aparece aviso com Desfazer', /Importação concluída: 3 mudanças/.test(await p.locator('.toast').innerText()) && (await p.locator('.toast-undo').count()) === 1);

  // Desfazer volta exatamente ao estado anterior
  await p.click('.toast-undo'); await p.waitForTimeout(100);
  ok('Desfazer a importação volta tudo como estava, sem registro', JSON.stringify(await estado()) === JSON.stringify(S0));

  // Importar de novo, agora atualizando
  await enviar('clientes-novembro.xlsx', arq);
  ok('O mesmo arquivo pode ser escolhido de novo', /Prévia da importação/.test(await p.locator('.sheet h2').innerText()));
  await botao().click();
  S1 = await estado();
  const contaDepois = S1.contas.find(c => c.id === contaM.id);
  ok('Com atualização: telefone da Marina mudou', S1.clientes.find(c => c.id === marina.id).telefone === '(11) 91111-2222');
  ok('Com atualização: valor e baixa da conta mudaram, sem criar conta repetida', contaDepois.valor === 200 && contaDepois.pagoEm === hoje && S1.contas.length === S0.contas.length + 2);
  ok('Contato vazio na planilha não apaga o que já existe', S1.clientes.find(c => c.id === marina.id).email === (marina.email || ''));

  // Histórico
  const hist = p.locator('[data-act=import-log]');
  ok('Planilha mostra "Últimas importações" com o arquivo', (await hist.count()) === 1 && /clientes-novembro\.xlsx/.test(await hist.first().innerText()));
  ok('Item do histórico resume o que entrou e mudou', /1 cliente novo · 2 contas novas · 2 atualizações · 5 linhas com problema/.test(await hist.first().innerText()), await hist.first().innerText());
  await hist.first().click();
  const log = await p.locator('.sheet').innerText();
  ok('Registro da importação lista o que mudou, com antes e depois', /O que mudou/i.test(log) && /Telefone:/.test(log) && /Valor:/.test(log));
  ok('Registro lista o que entrou e as linhas que ficaram de fora', /O que entrou/i.test(log) && /Bruno Costa/.test(log) && /Linhas que ficaram de fora/i.test(log) && /Linha 6/.test(log));
  await p.click('[data-act=close]');

  // Reimportar o mesmo arquivo: nada a mudar
  await enviar('clientes-novembro.xlsx', arq);
  const r2 = await resumo();
  ok('Reimportar após aplicar: nada novo, nada a atualizar', r2['Clientes novos'] === 0 && r2['Contas novas'] === 0 && r2['Atualizações'] === 0 && r2['Sem mudança'] === 3, JSON.stringify(r2));
  ok('Botão "Nada para importar" fica desativado', await botao().isDisabled());
  ok('Contas iguais ficam recolhidas, para não poluir a prévia', (await p.locator('details.imp-iguais').count()) === 1 && !(await p.locator('details.imp-iguais').evaluate(d => d.open)));
  await p.click('.sheet [data-act=close]');

  // Ida e volta: exportar a cópia de segurança e importar de volta não muda nada
  await p.click('[data-act=export-backup]'); await p.click('[data-act=xlsx-export]');
  await p.waitForFunction(() => window.__salvo);
  const backup = await p.evaluate(() => window.__salvo);
  await p.click('[data-act=close]').catch(() => {});
  await enviar('backup.xlsx', backup);
  const r3 = await resumo();
  const S2 = await estado();
  ok('Reimportar a própria cópia de segurança: zero mudanças e zero problemas', r3['Clientes novos'] === 0 && r3['Contas novas'] === 0 && r3['Atualizações'] === 0 && r3['Com problema'] === 0 && r3['Sem mudança'] === S2.contas.length, JSON.stringify(r3) + ' contas=' + S2.contas.length);
  await p.click('.sheet [data-act=close]');

  // Arquivo que não é planilha
  await p.setInputFiles('#xl', { name: 'foto.xlsx', mimeType: 'application/octet-stream', buffer: Buffer.from('isto não é um excel') });
  await p.waitForTimeout(300);
  const t = await p.locator('.toast').innerText().catch(() => '');
  ok('Arquivo inválido: aviso diz o que fazer, sem erro de JavaScript', /Não consegui ler esse arquivo|Prévia/.test(t + await p.locator('.sheet h2').innerText().catch(() => '')) && errs.length === 0, t + ' ' + errs.join('|'));

  // Layout da prévia em 320 px
  await p.setViewportSize({ width: 320, height: 700 });
  await enviar('clientes-novembro.xlsx', arq);
  const sobra = await p.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  ok('Prévia sem rolagem horizontal em 320 px', sobra <= 0, `${sobra}px`);
  const toque = await p.evaluate(() => { const e = document.querySelector('.toggle'); return e ? e.getBoundingClientRect().height : 99; });
  ok('Opção "Atualizar estes dados" tem área de toque ≥ 44 px', toque >= 44, `${toque}`);

  ok('Sem erros de JavaScript', errs.length === 0, errs.join(' | '));
  await b.close();
  console.log(out.join('\n'));
  if (out.some(l => l.startsWith('✖'))) process.exitCode = 1;
})().catch(e => { console.error('ERRO NO TESTE', e); process.exit(1); });
