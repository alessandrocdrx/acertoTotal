// Testes das técnicas de usabilidade aplicadas nas telas.
// Cada teste cita a técnica que protege. Se um falhar, registre em docs/aprendizados.md.
const { chromium } = require('playwright');
const fs = require('fs');
const out = [];
const ok = (tecnica, nome, cond, det = '') => out.push(`${cond ? '✔' : '✖'} [${tecnica}] ${nome}${cond ? '' : ' → ' + det}`);

(async () => {
  const b = await chromium.launch();
  const ctx = await b.newContext({ viewport: { width: 390, height: 844 }, timezoneId: 'America/Sao_Paulo', locale: 'pt-BR' });
  const p = await ctx.newPage();
  p.setDefaultTimeout(5000); // falha rápido se um elemento esperado sumir
  const errs = [];
  p.on('pageerror', e => errs.push(e.message));
  await p.route('**/*', r => {
    const u = r.request().url();
    if (u.includes('xlsx')) return r.fulfill({ contentType: 'application/javascript', body: fs.readFileSync(__dirname + '/vendor/xlsx.js', 'utf8') });
    if (u.startsWith('file://')) return r.continue();
    return r.fulfill({ status: 200, body: '' });
  });
  await p.goto('file://' + __dirname + '/.build/page.html');
  await p.waitForSelector('.crow');
  const n = async label => +(await p.locator('.four .chip', { hasText: label }).innerText()).match(/\d+/)[0];
  const espera = () => p.waitForTimeout(80);

  // Lei de Hick e de Jakob: poucas escolhas, padrões do Android
  ok('Lei de Hick', 'Menu inferior com 4 abas', (await p.locator('.tabs button').count()) === 4);
  ok('Lei de Hick', 'Filtros de contas em 4 botões numa linha só', await p.evaluate(() => { const t = [...document.querySelectorAll('.four .chip')].map(c => c.getBoundingClientRect().top); return t.length === 4 && new Set(t).size === 1; }));
  ok('Lei de Jakob', 'Botão flutuante, menu inferior e janela de baixo, como nos apps Android', (await p.locator('.fab').count()) === 1 && (await p.locator('nav.tabs').count()) === 1);
  ok('Identidade', 'Nome CelsoPix aparece dentro do app', (await p.locator('.brand').innerText()).includes('CelsoPix'));
  ok('Acessibilidade', 'Página declara idioma pt-BR (leitor de tela pronuncia certo)', (await p.evaluate(() => document.documentElement.lang)) === 'pt-BR');

  // Prevenção de erros + mensagem no próprio campo
  await p.locator('.crow.atrasada .btn.primary').first().click();
  ok('Prevenção de erros', 'Calendário da baixa não deixa escolher data futura', (await p.getAttribute('#dtpago', 'max')) === (await p.inputValue('#dtpago')));
  const amanha = await p.evaluate(() => { const d = new Date(); d.setDate(d.getDate() + 1); return d.toISOString().slice(0, 10); });
  await p.evaluate(v => { document.getElementById('dtpago').value = v; }, amanha);
  await p.click('[data-act=baixa]');
  ok('Mensagem de erro', 'Data futura é recusada com aviso embaixo do campo', /depois de hoje/.test(await p.locator('.sheet .ferr').innerText().catch(() => '')) && (await p.locator('.sheet').count()) === 1);
  ok('Mensagem de erro', 'Campo com erro fica marcado para o leitor de tela', (await p.getAttribute('#dtpago', 'aria-invalid')) === 'true' && (await p.getAttribute('#dtpago', 'aria-describedby')) === 'dtpago-err');
  await p.fill('#dtpago', await p.evaluate(() => document.getElementById('dtpago').max));
  ok('Mensagem de erro', 'Erro some assim que a pessoa corrige o campo', (await p.locator('.sheet .ferr').count()) === 0);

  // Controle e liberdade: desfazer em vez de pedir confirmação
  const ab0 = await n('Em aberto');
  await p.click('[data-act=baixa]');
  ok('Visibilidade do status', 'Depois da baixa aparece aviso com o valor', /Baixa registrada: R\$/.test(await p.locator('.toast').innerText()));
  const undo = p.locator('.toast-undo');
  const bx = await undo.boundingBox();
  ok('Controle e liberdade', 'Aviso da baixa oferece Desfazer, com área de toque ≥ 44 px', (await undo.count()) === 1 && bx.height >= 44, JSON.stringify(bx));
  ok('Visibilidade do status', 'Contador "Em aberto" cai 1 na hora', (await n('Em aberto')) === ab0 - 1);
  await undo.click(); await espera();
  ok('Controle e liberdade', 'Desfazer devolve a conta para "Em aberto"', (await n('Em aberto')) === ab0);
  ok('Visibilidade do status', 'Só um aviso na tela por vez', (await p.locator('.toast').count()) <= 1);

  const todas0 = await n('Todas');
  await p.locator('.crow .cmain').first().click();
  await p.click('[data-act=excluir-conta]');
  ok('Controle e liberdade', 'Excluir conta tira da lista e avisa com Desfazer', (await n('Todas')) === todas0 - 1 && /Conta excluída/.test(await p.locator('.toast').innerText()));
  await p.click('.toast-undo'); await espera();
  ok('Controle e liberdade', 'Desfazer a exclusão traz a conta de volta', (await n('Todas')) === todas0);
  ok('Hierarquia visual', 'Excluir é um botão discreto, abaixo de Fechar', await (async () => { await p.locator('.crow .cmain').first().click(); const ex = await p.locator('[data-act=excluir-conta]').boundingBox(); const fe = await p.locator('.sheet [data-act=close]').boundingBox(); const cls = await p.getAttribute('[data-act=excluir-conta]', 'class'); await p.keyboard.press('Escape'); return ex.y > fe.y && /quiet/.test(cls); })());

  // Saída clara em telas vazias
  await p.fill('#q', 'xyzsemresultado');
  ok('Saída clara', 'Busca sem resultado explica e oferece "Limpar busca"', /Nenhuma conta encontrada para “xyzsemresultado”/.test(await p.locator('#screen').innerText()) && (await p.locator('[data-act=limpar-busca]').count()) === 1);
  await p.click('[data-act=limpar-busca]');
  ok('Saída clara', '"Limpar busca" limpa o campo e volta a lista', (await p.inputValue('#q')) === '' && (await p.locator('.crow').count()) > 0);

  // Formulário de nova conta
  await p.click('[data-act=nova-conta]');
  ok('Prevenção de erros', 'Nova conta começa sem cliente escolhido (evita lançar no cliente errado)', (await p.inputValue('#nc-cli')) === '');
  const nomes = await p.$$eval('#nc-cli option:not([disabled])', o => o.map(x => x.textContent));
  ok('Reconhecer em vez de lembrar', 'Lista de clientes em ordem alfabética', JSON.stringify(nomes) === JSON.stringify([...nomes].sort((a, c) => a.localeCompare(c))), nomes.join(', '));
  ok('Clareza', 'Exemplos ficam fora do campo (campo vazio não parece preenchido)', (await p.getAttribute('#nc-desc', 'placeholder')) === null && (await p.getAttribute('#nc-val', 'placeholder')) === null && (await p.locator('.sheet .hint').count()) >= 2);
  ok('Clareza', 'Campo de valor mostra "R$" fixo', (await p.locator('.money span').innerText()) === 'R$');
  await p.click('[data-act=salvar-conta]');
  ok('Mensagem de erro', 'Sem cliente: erro "Escolha o cliente" no próprio campo', /Escolha o cliente/.test(await p.locator('.sheet .ferr').innerText()));
  const opt = await p.$eval('#nc-cli option:not([disabled])', o => o.value);
  await p.selectOption('#nc-cli', opt);
  await p.click('[data-act=salvar-conta]');
  ok('Mensagem de erro', 'Sem valor: erro com exemplo de como preencher', /Informe o valor\. Ex\.: 150,00/.test(await p.locator('.sheet .ferr').innerText()));
  await p.fill('#nc-val', '99,90'); await p.fill('#nc-desc', 'Teste de usabilidade');
  await p.click('[data-act=salvar-conta]');
  ok('Visibilidade do status', 'Conta salva fecha a janela e avisa', (await p.locator('.sheet').count()) === 0 && /Conta salva/.test(await p.locator('.toast').innerText()));

  // Clientes
  await p.click('button[data-tab=clientes]');
  const linhas = await p.locator('#screen .row').allInnerTexts();
  ok('Informação útil primeiro', 'Lista de clientes mostra quanto cada um deve, ou "Em dia"', linhas.every(t => /R\$|Em dia/.test(t)), linhas.join(' | '));
  const primeiro = (await p.locator('#screen .row .t').first().innerText());
  await p.locator('#screen .row').first().click();
  const titulos = await p.locator('.sheet .crow .t').allInnerTexts();
  ok('Sem repetição', 'Na ficha do cliente, as contas mostram a descrição, não o nome repetido', titulos.length > 0 && titulos.every(t => t !== primeiro), titulos.join(' | '));
  await p.click('[data-act=editar-cliente]');
  ok('Controle e liberdade', 'Ficha do cliente permite editar os dados, já preenchidos', (await p.inputValue('#n-nome')) === primeiro);
  await p.fill('#n-tel', '(11) 91234-5678');
  await p.click('[data-act=salvar-cliente]');
  ok('Visibilidade do status', 'Editar salva e mostra o telefone novo na lista', /Dados atualizados/.test(await p.locator('.toast').innerText()) && (await p.locator('#screen').innerText()).includes('(11) 91234-5678'));
  await p.click('[data-act=novo-cliente]');
  await p.fill('#n-nome', primeiro.toUpperCase());
  await p.click('[data-act=salvar-cliente]');
  ok('Prevenção de erros', 'Não deixa cadastrar dois clientes com o mesmo nome', /Já existe um cliente/.test(await p.locator('.sheet .ferr').innerText()));
  await p.keyboard.press('Escape');

  // Rótulos e tamanho de toque em todas as telas e formulários
  const semRotulo = await p.evaluate(() => [...document.querySelectorAll('input:not([type=file]),select,textarea')].filter(i => !(i.labels && i.labels.length) && !i.getAttribute('aria-label')).map(i => i.id));
  ok('Acessibilidade', 'Todo campo tem rótulo', semRotulo.length === 0, semRotulo.join(','));
  for (const abrir of ['nova-conta', 'novo-cliente']) {
    await p.click(abrir === 'nova-conta' ? 'button[data-tab=contas]' : 'button[data-tab=clientes]');
    await p.click(`[data-act=${abrir}]`);
    const pequenos = await p.evaluate(() => [...document.querySelectorAll('.sheet button,.sheet input,.sheet select,.sheet textarea')].filter(e => e.offsetParent && e.type !== 'file').map(e => [e.id || e.textContent.trim(), e.getBoundingClientRect().height]).filter(x => x[1] < 44));
    ok('Lei de Fitts', `Formulário ${abrir}: todo controle com ≥ 44 px de altura`, pequenos.length === 0, JSON.stringify(pequenos));
    const prim = await p.locator('.sheet .btn.primary').boundingBox();
    ok('Lei de Fitts', `Formulário ${abrir}: botão principal largo, ≥ 48 px, na parte de baixo`, prim.height >= 48 && prim.width > 300);
    await p.keyboard.press('Escape');
  }
  ok('Robustez', 'Nenhum erro de JavaScript durante os testes', errs.length === 0, errs.join(' | '));
  await b.close();
  console.log(out.join('\n'));
  if (out.some(l => l.startsWith('✖'))) process.exitCode = 1;
})().catch(e => { console.error('ERRO NO TESTE', e); process.exit(1); });
