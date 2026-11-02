// Assinatura: planos e preços, caminho até "Assinar", liberar o app depois do teste, simulação no app de teste
// e a cobrança real da Google Play (com uma ponte falsa no lugar da casca Android).
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const out = [];
const ok = (nome, cond, det = '') => out.push(`${cond ? '✔' : '✖'} ${nome}${cond ? '' : ' → ' + det}`);
const AXE = fs.readFileSync(path.join(__dirname, 'vendor', 'axe.js'), 'utf8');
const SRC = path.join(__dirname, '..', 'android', 'app', 'src');

(async () => {
  const b = await chromium.launch();
  // diasAtras: quando começou o teste grátis. ponte: script que cria window.acertoTotalAndroid (cobrança real).
  const abrir = async (diasAtras, { url, ponte, largura = 390 } = {}) => {
    const ctx = await b.newContext({ viewport: { width: largura, height: 844 }, timezoneId: 'America/Sao_Paulo', locale: 'pt-BR' });
    const p = await ctx.newPage();
    p.setDefaultTimeout(6000);
    p.errs = [];
    p.on('pageerror', e => p.errs.push(e.message));
    // O protótipo sem variáveis não conta teste grátis (artefato só para olhar); aqui ligamos os 7 dias do app de teste. Nos apps empacotados, o script da página vem depois e decide.
    await p.addInitScript(() => { if (window.ACERTO_LIMITE_DIAS == null) window.ACERTO_LIMITE_DIAS = 7; });
    await p.route('**/*', r => {
      const u = r.request().url();
      if (u.includes('xlsx')) return r.fulfill({ contentType: 'application/javascript', body: fs.readFileSync(__dirname + '/vendor/xlsx.js', 'utf8') });
      if (u.startsWith('file://')) return r.continue();
      return r.fulfill({ status: 200, body: '' });
    });
    if (ponte) await p.addInitScript(ponte);
    await p.addInitScript(d => {
      if (window.__semente) return; window.__semente = 1;
      const agora = Date.now();
      localStorage.setItem('acertototal-avaliacao', JSON.stringify({ inicio: agora - d * 864e5, visto: agora - d * 864e5 }));
    }, diasAtras);
    await p.goto(url || ('file://' + __dirname + '/.build/page.html'));
    await p.waitForSelector('.crow, .empty');
    await p.waitForTimeout(500);
    return { p, ctx };
  };
  const axe = async (p, rotulo) => {
    await p.evaluate(AXE); await p.waitForTimeout(500);
    const r = await p.evaluate(async () => await axe.run(document.querySelector('.sheet'), { runOnly: ['wcag2a', 'wcag2aa', 'wcag21aa'] }));
    ok(`Acessibilidade: ${rotulo}`, r.violations.length === 0, r.violations.map(x => `${x.id}(${x.nodes.length}): ${x.help}`).join(' | '));
  };
  const visivelSemRolar = (p, sel) => p.evaluate(s => { const e = document.querySelector(s); if (!e) return false; const r = e.getBoundingClientRect(); return r.top >= 0 && r.bottom <= innerHeight; }, sel);

  // ===== App de teste (assinatura simulada)
  {
    const { p, ctx } = await abrir(3);
    await p.click('button[data-tab=planilha]');
    ok('A aba Planilha só tem tarefas de planilha (a assinatura fica no Menu)', (await p.locator('#screen .assin').count()) === 0);
    await p.click('[data-act=abrir-menu]');
    ok('O Menu tem o cartão "Assinatura" com os dias do teste', /Assinatura/.test(await p.locator('.assin').innerText()) && /faltam 4 dias/.test(await p.locator('.assin').innerText()));
    ok('O cartão Assinatura aparece no Menu sem rolar', await visivelSemRolar(p, '.assin [data-act=planos]'));
    await p.click('.assin [data-act=planos]');
    const tela = (await p.locator('.sheet').innerText()).replace(/ /g, ' ');
    ok('Janela "Assinar" lista os 4 planos', (await p.locator('.plano').count()) === 4 && /Semanal/.test(tela) && /Mensal/.test(tela) && /Semestral/.test(tela) && /Anual/.test(tela));
    ok('Preços: R$ 2,99, R$ 9,99, R$ 56,90 e R$ 107,90', /R\$\s2,99/.test(tela) && /R\$\s9,99/.test(tela) && /R\$\s56,90/.test(tela) && /R\$\s107,90/.test(tela), tela);
    ok('Cada plano mostra o equivalente por mês (semestral R$ 9,48 e anual R$ 8,99)', /R\$ 9,48 por mês/.test(tela) && /R\$ 8,99 por mês/.test(tela) && /R\$ 9,99 por mês/.test(tela) && /R\$ 12,96 por mês/.test(tela), tela);
    ok('Semestral é 5% e anual 10% mais baratos que o mensal', /Semestral\s*5% mais barato/.test(tela) && /Anual\s*10% mais barato/.test(tela), tela);
    ok('A conta bate: 9,99 × 6 × 0,95 ≈ 56,90 e 9,99 × 12 × 0,90 ≈ 107,90', Math.abs(9.99 * 6 * 0.95 - 56.90) < 0.05 && Math.abs(9.99 * 12 * 0.9 - 107.90) < 0.05);
    ok('Mensal já vem escolhido e o botão diz plano e preço', (await p.locator('.plano[aria-checked=true]').getAttribute('data-p')) === 'mensal' && /Assinar o plano mensal por R\$\s*9,99/.test((await p.locator('#bt-assinar').innerText()).replace(/ /g, ' ')));
    ok('Avisa que na versão de teste é simulado e ninguém é cobrado', /simulada e ninguém é cobrado/.test(tela));
    ok('Diz como cancelar na Google Play', /Cancele quando quiser/.test(tela));
    ok('Os botões ficam fixos embaixo, sem rolar', await visivelSemRolar(p, '#bt-assinar') && await visivelSemRolar(p, '.sheet [data-act=close]'));
    await axe(p, 'janela Assinar');
    await p.click('.plano[data-p=anual]');
    ok('Escolher o anual troca a marcação e o botão, sem fechar a janela', (await p.locator('.plano[aria-checked=true]').getAttribute('data-p')) === 'anual' && /plano anual por R\$\s*107,90/.test((await p.locator('#bt-assinar').innerText()).replace(/ /g, ' ')) && (await p.locator('.sheet').count()) === 1);
    // Espera a confirmação, e não um tempo fixo (150 ms falhou uma vez com a máquina carregada).
    await p.click('#bt-assinar'); await p.waitForSelector('.toast:has-text("simulada ativada")');
    ok('Assinar (simulado): fecha a janela, avisa e o cartão mostra "Ativa"', (await p.locator('.sheet').count()) === 0 && /simulada ativada/.test(await p.locator('.toast').innerText()) && /Ativa/.test(await p.locator('.assin').innerText()));
    ok('Com assinatura, a faixa do teste grátis some', !/Teste grátis/.test(await p.locator('#ambiente').innerText()));
    await p.reload(); await p.waitForSelector('.crow'); await p.click('[data-act=abrir-menu]');
    ok('A assinatura continua depois de reabrir o app', /Ativa/.test(await p.locator('.assin').innerText()));
    await p.click('.assin [data-act=planos]'); await p.waitForSelector('.sheet h2');
    ok('Assinante vê "Sua assinatura" e como gerenciar, sem a lista de planos', /Sua assinatura/.test(await p.locator('.sheet h2').innerText()) && (await p.locator('.plano').count()) === 0 && (await p.locator('[data-act=gerenciar-assin]').count()) === 1);
    await p.click('[data-act=cancelar-sim]');
    ok('Cancelar a assinatura simulada volta à lista de planos', (await p.locator('.plano').count()) === 4 && !/Ativa/.test(await p.locator('.assin').innerText().catch(() => '')));
    ok('Sem erros de JavaScript', p.errs.length === 0, p.errs.join(' | '));
    await ctx.close();
  }

  // ===== Teste terminado: só consulta até assinar
  {
    const { p, ctx } = await abrir(9);
    ok('Teste terminado: faixa vermelha com o botão "Assinar"', /Teste grátis encerrado/.test(await p.locator('#ambiente').innerText()) && (await p.locator('#ambiente [data-act=planos]').count()) === 1);
    ok('Teste terminado: cadastrar e dar baixa bloqueados', (await p.locator('.fab').count()) === 0 && (await p.locator('[data-act=conta]:has-text("Dar baixa")').count()) === 0);
    await p.click('#ambiente [data-act=planos]');
    ok('A janela explica que os dados continuam guardados', /Seus dados continuam guardados/.test(await p.locator('.sheet').innerText()));
    await p.click('.plano[data-p=semestral]'); await p.click('#bt-assinar'); await p.waitForTimeout(200);
    ok('Depois de assinar, o app volta a funcionar por inteiro', (await p.locator('.fab').count()) === 1 && (await p.locator('#ambiente [data-act=planos]').count()) === 0);
    await ctx.close();
  }
  {
    const { p, ctx } = await abrir(9);
    await p.click('.crow >> nth=0');
    ok('Na ficha da conta, o modo só consulta oferece "Ver planos"', (await p.locator('.sheet [data-act=planos]').count()) === 1);
    await p.click('.sheet [data-act=planos]');
    ok('"Ver planos" abre a janela Assinar', /Assinar acertoTotal/.test(await p.locator('.sheet h2').innerText()));
    await p.click('[data-act=restaurar-assin]');
    ok('Restaurar sem ter assinado avisa que não achou nada', /Nenhuma assinatura encontrada/.test(await p.locator('.toast').innerText()));
    await ctx.close();
  }

  // ===== Layout em 320 px
  {
    const { p, ctx } = await abrir(2, { largura: 320 });
    await p.click('[data-act=abrir-menu]'); await p.click('.assin [data-act=planos]');
    ok('Em 320 px: sem rolagem lateral e texto dos planos sem quebrar por palavra', await p.evaluate(() => document.documentElement.scrollWidth <= innerWidth && [...document.querySelectorAll('.plano .pn b, .plano .pv')].every(e => e.getBoundingClientRect().width > 40)));
    await ctx.close();
  }

  // ===== App da Play (cobrança real): ponte falsa no lugar da casca Android
  const PONTE = estado => `
    window.__chamadas = [];
    window.__estado = ${JSON.stringify(estado)};
    window.acertoTotalAndroid = {
      assinaturaEstado: () => JSON.stringify(window.__estado),
      assinar: p => { window.__chamadas.push('assinar:' + p); return 'ok'; },
      atualizarAssinatura: () => { window.__chamadas.push('atualizar'); },
      gerenciarAssinatura: () => { window.__chamadas.push('gerenciar'); },
    };`;
  const PRECOS = { semanal: 'R$ 2,99', mensal: 'R$ 9,99', semestral: 'R$ 56,90', anual: 'R$ 107,90' };
  const PROD = 'file://' + path.join(SRC, 'producao', 'assets', 'index.html');
  {
    const { p, ctx } = await abrir(40, { url: PROD, ponte: PONTE({ real: true, ativa: false, pronto: true, precos: { ...PRECOS, anual: 'R$ 108,00' } }) });
    ok('Play: teste terminado e sem assinatura → só consulta', (await p.locator('.fab').count()) === 0 && /Teste grátis encerrado/.test(await p.locator('#ambiente').innerText()));
    await p.click('#ambiente [data-act=planos]');
    const tela = (await p.locator('.sheet').innerText()).replace(/ /g, ' ');
    ok('Play: os preços mostrados são os que a Play informou', /R\$\s108,00/.test(tela) && !/R\$\s107,90/.test(tela), tela);
    ok('Play: não aparece o aviso de simulação', !/simulada/.test(tela));
    await p.click('.plano[data-p=semestral]'); await p.click('#bt-assinar');
    ok('Play: "Assinar" chama a casca Android com o plano escolhido', JSON.stringify(await p.evaluate(() => window.__chamadas)) === '["assinar:semestral"]');
    // A Play confirma a compra e avisa a página
    await p.evaluate(() => window.acertoAssinatura(JSON.stringify({ real: true, ativa: true, pronto: true, precos: {} })));
    await p.waitForTimeout(200);
    ok('Play: quando a Play avisa que há assinatura, o app é liberado e a janela mostra "Sua assinatura"', (await p.locator('.fab').count()) === 1 && /Sua assinatura/.test(await p.locator('.sheet h2').innerText()));
    await p.click('[data-act=gerenciar-assin]');
    ok('Play: "Gerenciar" abre a Google Play pela casca', (await p.evaluate(() => window.__chamadas)).includes('gerenciar'));
    await p.evaluate(() => window.acertoAssinatura({ real: true, ativa: false, pronto: true, precos: {} }));
    await p.waitForTimeout(200);
    ok('Play: se a assinatura acabar, o app volta ao modo só consulta', (await p.locator('.fab').count()) === 0);
    await p.keyboard.press('Escape'); await p.click('#ambiente [data-act=planos]'); await p.click('[data-act=restaurar-assin]');
    ok('Play: "Restaurar" pede à casca para conferir de novo', (await p.evaluate(() => window.__chamadas)).includes('atualizar'));
    ok('Sem erros de JavaScript', p.errs.length === 0, p.errs.join(' | '));
    await ctx.close();
  }
  {
    // Assinante conhecido do aparelho abre o app já liberado, mesmo antes da Play responder
    const { p, ctx } = await abrir(40, { url: PROD, ponte: PONTE({ real: true, ativa: true, pronto: false, precos: {} }) });
    ok('Play: assinante abre o app liberado, mesmo sem internet', (await p.locator('.fab').count()) === 1 && !(await p.locator('#ambiente').isVisible()));
    await ctx.close();
  }
  {
    // A Play não respondeu a compra
    const { p, ctx } = await abrir(40, { url: PROD, ponte: PONTE({ real: true, ativa: false, pronto: false, precos: {} }).replace("return 'ok'", "return 'indisponivel'") });
    await p.click('#ambiente [data-act=planos]'); await p.click('#bt-assinar');
    ok('Play sem resposta: o aviso pede para conferir a internet', /Não consegui falar com a Google Play\. Confira a internet/.test(await p.locator('.toast').innerText()));
    await ctx.close();
  }
  {
    // Relato do cliente (02/11): a Play respondeu, mas o produto de assinatura ainda não existe no Console.
    // O aviso antigo mandava "conferir a internet", o que estava errado.
    const { p, ctx } = await abrir(3, { url: PROD, ponte: PONTE({ real: true, ativa: false, pronto: true, precos: {} }).replace("return 'ok'", "return 'indisponivel'") });
    await p.click('[data-act=abrir-menu]'); await p.click('.assin [data-act=planos]');
    ok('Play sem planos à venda: a janela avisa e lembra que o teste grátis continua', /ainda não estão à venda na Google Play/.test(await p.locator('#aviso-planos').innerText()) && /continua valendo até/.test(await p.locator('#aviso-planos').innerText()));
    await p.click('#bt-assinar');
    const t = await p.locator('.toast').innerText();
    ok('Play sem planos à venda: "Assinar" explica o motivo certo, sem falar de internet', /ainda não estão à venda/.test(t) && !/internet/.test(t), t);
    await ctx.close();
  }

  // ===== Casca Android: o que o pacote promete
  const kt = fs.readFileSync(path.join(SRC, 'main', 'java', 'io', 'github', 'alessandrocdrx', 'acertototal', 'Assinatura.kt'), 'utf8');
  const gradle = fs.readFileSync(path.join(SRC, '..', 'build.gradle.kts'), 'utf8');
  ok('Android: produto de assinatura "acertototal_premium"', /PRODUTO = "acertototal_premium"/.test(kt));
  ok('Android: a página e a casca usam os mesmos 4 planos', ['semanal', 'mensal', 'semestral', 'anual'].every(id => fs.readFileSync(path.join(__dirname, '..', 'prototipo', 'acertototal.html'), 'utf8').includes(`id:'${id}'`)) && /basePlanId == plano/.test(kt));
  ok('Android: só o app da Play cobra de verdade (o de teste simula)', /"COBRANCA_REAL", "true"/.test(gradle) && /"COBRANCA_REAL", "false"/.test(gradle));
  ok('Android: a compra é confirmada (acknowledge), senão a Play devolve o dinheiro em 3 dias', /acknowledgePurchase/.test(kt));
  ok('Android: Play Billing 8 ou mais novo', /billingclient:billing:(8|9|\d\d)\./.test(gradle));
  ok('Android: o app de teste não chama a Play (assets do homolog sem a ponte de cobrança)', !fs.existsSync(path.join(SRC, 'homolog', 'assets', 'index.html')) || !/assinaturaEstado\(\)\s*{/.test(fs.readFileSync(path.join(SRC, 'homolog', 'assets', 'index.html'), 'utf8')));

  await b.close();
  console.log(out.join('\n'));
  if (out.some(l => l.startsWith('✖'))) process.exitCode = 1;
})().catch(e => { console.error('ERRO NO TESTE', e); process.exit(1); });
