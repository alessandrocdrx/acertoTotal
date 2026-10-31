// Período de avaliação de 7 dias: contagem, aviso, modo só consulta depois do prazo e relógio atrasado.
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const out = [];
const ok = (nome, cond, det = '') => out.push(`${cond ? '✔' : '✖'} ${nome}${cond ? '' : ' → ' + det}`);
const DIA = 864e5;

(async () => {
  const b = await chromium.launch();
  // Abre o app como se o primeiro uso tivesse sido há "diasAtras" dias (e o último uso "vistoEm" ms atrás/à frente).
  const abrir = async (diasAtras, opts = {}) => {
    const ctx = await b.newContext({ viewport: { width: 390, height: 844 }, timezoneId: 'America/Sao_Paulo', locale: 'pt-BR' });
    const p = await ctx.newPage();
    p.setDefaultTimeout(6000);
    p.errs = [];
    p.on('pageerror', e => p.errs.push(e.message));
    await p.route('**/*', r => {
      const u = r.request().url();
      if (u.includes('xlsx')) return r.fulfill({ contentType: 'application/javascript', body: fs.readFileSync(__dirname + '/vendor/xlsx.js', 'utf8') });
      if (u.startsWith('file://')) return r.continue();
      return r.fulfill({ status: 200, body: '' });
    });
    if (diasAtras != null) {
      await p.addInitScript(([d, visto]) => {
        if (window.__semente) return; window.__semente = 1;
        const agora = Date.now();
        localStorage.setItem('celsopix-avaliacao', JSON.stringify({ inicio: agora - d * 864e5, visto: visto != null ? agora + visto : agora - d * 864e5 }));
      }, [diasAtras, opts.visto]);
    }
    await p.goto(opts.url || ('file://' + __dirname + '/.build/page.html'));
    await p.waitForSelector('.crow, .empty');
    return { p, ctx };
  };
  const faixa = p => p.locator('#ambiente').innerText();

  // Primeiro uso
  {
    const { p, ctx } = await abrir(null);
    const f = await faixa(p);
    const fim = await p.evaluate(() => { const d = new Date(Date.now() + 7 * 864e5); return d.toLocaleDateString('pt-BR'); });
    ok('Primeiro uso: faixa mostra "faltam 7 dias" e a data final', /Avaliação: faltam 7 dias \(até (\d\d\/\d\d\/\d{4})\)/.test(f) && f.includes(fim), f);
    ok('Primeiro uso: faixa continua avisando que é ambiente de teste', /Ambiente de teste/.test(f));
    ok('Primeiro uso: dá para dar baixa e cadastrar', (await p.locator('.crow .btn.primary').count()) > 0 && (await p.locator('.fab').count()) === 1);
    const ini = await p.evaluate(() => JSON.parse(localStorage.getItem('celsopix-avaliacao')).inicio);
    await p.reload(); await p.waitForSelector('.crow');
    ok('Recarregar não reinicia a contagem', (await p.evaluate(() => JSON.parse(localStorage.getItem('celsopix-avaliacao')).inicio)) === ini);
    await p.evaluate(() => document.getElementById('reset').click());
    ok('"Restaurar dados de exemplo" não reinicia a contagem', (await p.evaluate(() => JSON.parse(localStorage.getItem('celsopix-avaliacao')).inicio)) === ini);
    await ctx.close();
  }
  // Meio do período e último dia
  for (const [d, esperado] of [[3, 'faltam 4 dias'], [6.5, 'termina hoje']]) {
    const { p, ctx } = await abrir(d);
    const f = await faixa(p);
    ok(`Dia ${d}: faixa diz "${esperado}"`, f.includes(esperado), f);
    ok(`Dia ${d}: o app continua completo`, (await p.locator('.fab').count()) === 1 && !(await p.locator('#ambiente').evaluate(e => e.classList.contains('fim'))));
    await ctx.close();
  }

  // Depois do prazo: só consulta
  {
    const { p, ctx } = await abrir(8);
    const antes = await p.evaluate(() => localStorage.getItem('baixa-pix-v2'));
    const f = await faixa(p);
    ok('Prazo vencido: faixa vermelha explica que é só consulta', /Avaliação encerrada em \d\d\/\d\d\/\d{4}\. O app está só para consulta/.test(f) && await p.locator('#ambiente').evaluate(e => e.classList.contains('fim')), f);
    ok('Contas: lista continua visível, com situação escrita', (await p.locator('.crow').count()) > 0 && /Atrasada há/.test(await p.locator('#screen').innerText()));
    ok('Contas: some o aviso de cópia de segurança (exportar está bloqueado)', (await p.locator('#screen .warn').count()) === 0);
    ok('Contas: sem botões "Dar baixa", sem "+ Nova conta" e sem "Exportar"', (await p.locator('.crow .btn.primary').count()) === 0 && (await p.locator('.fab').count()) === 0 && (await p.locator('[data-act=export-filtrado]').count()) === 0);
    await p.click('.chip:has-text("Pagas")');
    ok('Contas: filtros e busca continuam funcionando', (await p.locator('.crow.paga').count()) > 0);
    await p.locator('.crow .cmain').first().click();
    const sh = await p.locator('.sheet').innerText();
    ok('Ficha da conta: mostra os dados e explica que é só consulta', /Valor/.test(sh) && /só para consulta/.test(sh));
    ok('Ficha da conta: sem desfazer baixa e sem excluir', (await p.locator('.sheet [data-act=desfazer], .sheet [data-act=excluir-conta], .sheet [data-act=baixa]').count()) === 0);
    await p.keyboard.press('Escape');
    await p.click('button[data-tab=clientes]');
    ok('Clientes: lista visível, sem "+ Novo cliente"', (await p.locator('#screen .row').count()) > 0 && (await p.locator('.fab').count()) === 0);
    await p.locator('#screen .row').first().click();
    ok('Ficha do cliente: sem "Nova conta" e sem "Editar dados"', (await p.locator('.sheet [data-act=nova-conta-cli], .sheet [data-act=editar-cliente]').count()) === 0 && /só para consulta/.test(await p.locator('.sheet').innerText()));
    await p.keyboard.press('Escape');
    await p.click('button[data-tab=numeros]');
    ok('Números: estatísticas continuam disponíveis', (await p.locator('.statbox').count()) === 1);
    await p.click('button[data-tab=planilha]');
    const pl = await p.locator('#screen').innerText();
    ok('Planilha: cartão "Só consulta" explica, sem importar nem exportar', /Só consulta/i.test(pl) && /versão completa/.test(pl) && (await p.locator('[data-act=pick-xl], [data-act=import-abrir], [data-act=export-abrir], [data-act=export-backup], [data-act=import-modelo]').count()) === 0);
    await p.click('.iconbtn[data-act=aparencia]');
    await p.click('.seg button[data-t=dark]');
    ok('Tema continua podendo ser trocado (não mexe nos dados)', (await p.evaluate(() => document.documentElement.getAttribute('data-theme'))) === 'dark');
    await p.keyboard.press('Escape');

    // Tentativas de burlar pela página: ações de escrita disparadas direto
    for (const act of ['nova-conta', 'novo-cliente', 'import-abrir', 'import-sample', 'import-modelo', 'export-abrir', 'export-backup', 'pick-xl', 'baixa', 'excluir-conta']) {
      await p.evaluate(a => { const b = document.createElement('button'); b.dataset.act = a; b.dataset.id = 'x'; document.body.appendChild(b); b.click(); b.remove(); }, act);
      ok(`Ação "${act}" disparada direto é bloqueada com aviso`, (await p.locator('.sheet').count()) === 0 && /só para consulta/.test(await p.locator('.toast').innerText()));
    }
    await p.evaluate(() => document.getElementById('reset').click());
    ok('"Restaurar dados de exemplo" bloqueado', /só para consulta/.test(await p.locator('.toast').innerText()));
    ok('Nenhum dado foi alterado ou apagado depois do prazo', (await p.evaluate(() => localStorage.getItem('baixa-pix-v2'))) === antes);
    ok('Sem erros de JavaScript', p.errs.length === 0, p.errs.join(' | '));
    await ctx.close();
  }

  // Relógio atrasado: último uso registrado 3 dias "no futuro"
  {
    const { p, ctx } = await abrir(2, { visto: 3 * DIA });
    const f = await faixa(p);
    ok('Relógio atrasado mais de 1 dia: avaliação encerrada', /relógio do aparelho foi atrasado/.test(f) && (await p.locator('.fab').count()) === 0, f);
    await p.reload(); await p.waitForSelector('.crow');
    ok('Relógio atrasado: continua encerrada depois de recarregar', /relógio do aparelho foi atrasado/.test(await faixa(p)));
    await ctx.close();
  }

  // APK: homolog tem 7 dias; produção não tem limite
  const SRC = path.join(__dirname, '..', 'android', 'app', 'src');
  {
    const { p, ctx } = await abrir(8, { url: 'file://' + path.join(SRC, 'homolog', 'assets', 'index.html') });
    ok('APK de teste: limite de 7 dias ligado', /Avaliação encerrada/.test(await faixa(p)));
    await ctx.close();
  }
  {
    const { p, ctx } = await abrir(30, { url: 'file://' + path.join(SRC, 'producao', 'assets', 'index.html') });
    ok('APK de produção: sem limite e sem faixa', !(await p.locator('#ambiente').isVisible()) && (await p.locator('.fab').count()) === 1);
    await ctx.close();
  }

  await b.close();
  console.log(out.join('\n'));
  if (out.some(l => l.startsWith('✖'))) process.exitCode = 1;
})().catch(e => { console.error('ERRO NO TESTE', e); process.exit(1); });
