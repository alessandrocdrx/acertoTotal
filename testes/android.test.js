// Testes do que muda no APK: página sem internet, ponte de download do Android,
// fotos de comprovante reduzidas e aviso de memória cheia.
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const ASSETS = path.join(__dirname, '..', 'android', 'app', 'src', 'main', 'assets');
const out = [];
const ok = (nome, cond, det = '') => out.push(`${cond ? '✔' : '✖'} ${nome}${cond ? '' : ' → ' + det}`);

(async () => {
  const html = fs.readFileSync(path.join(ASSETS, 'index.html'), 'utf8');
  ok('APK não depende de internet: nenhum endereço externo na página', !/https?:\/\//.test(html.replace(/http:\/\/www\.w3\.org\/2000\/svg/g, '')), (html.match(/https?:\/\/[^"' )]+/g) || []).join(', '));
  ok('Leitor de Excel vai dentro do APK', fs.statSync(path.join(ASSETS, 'xlsx.full.min.js')).size > 500000);
  ok('Página do APK declara idioma pt-BR e viewport de celular', /<html lang="pt-BR">/.test(html) && /name="viewport"/.test(html));

  const b = await chromium.launch();
  const nova = async (init) => {
    const ctx = await b.newContext({ viewport: { width: 390, height: 844 }, timezoneId: 'America/Sao_Paulo' });
    const p = await ctx.newPage();
    p.setDefaultTimeout(8000);
    p.errs = [];
    p.on('pageerror', e => p.errs.push(e.message));
    // Bloqueia a rede: o APK não tem permissão de internet
    await p.route('**/*', r => r.request().url().startsWith('file://') ? r.continue() : r.abort());
    if (init) await p.addInitScript(init);
    await p.goto('file://' + path.join(ASSETS, 'index.html'));
    await p.waitForSelector('.crow');
    return { p, ctx };
  };

  // Ponte de download
  {
    const { p, ctx } = await nova(() => {
      window.CelsoPixAndroid = { salvarArquivo: (nome, b64) => { window.__salvo = { nome, b64 }; return 'saved'; } };
    });
    ok('Sem internet: app abre e funciona', (await p.locator('.crow').count()) > 0 && typeof (await p.evaluate(() => typeof XLSX)) === 'string' && (await p.evaluate(() => typeof XLSX)) === 'object');
    await p.click('button[data-tab=planilha]');
    await p.click('[data-act=export-atrasados]');
    await p.click('[data-act=xlsx-export]');
    await p.waitForFunction(() => window.__salvo);
    const r = await p.evaluate(() => {
      const bin = atob(window.__salvo.b64), u = new Uint8Array(bin.length);
      for (let i = 0; i < bin.length; i++) u[i] = bin.charCodeAt(i);
      const wb = XLSX.read(u, { type: 'array' });
      return { nome: window.__salvo.nome, linhas: XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], { header: 1 }) };
    });
    ok('Exportar no Android entrega um .xlsx válido para a ponte', /^atrasados-\d{4}-\d{2}-\d{2}\.xlsx$/.test(r.nome) && r.linhas[0][0] === 'Cliente' && r.linhas.length > 1, JSON.stringify(r.linhas[0]));
    ok('Depois de salvar, o app confirma', /Arquivo Excel salvo/.test(await p.locator('.toast').innerText()));
    await ctx.close();
  }
  {
    const { p, ctx } = await nova(() => { window.CelsoPixAndroid = { salvarArquivo: () => 'unavailable' }; });
    await p.click('button[data-tab=planilha]'); await p.click('[data-act=export-backup]'); await p.click('[data-act=xlsx-export]');
    await p.waitForTimeout(300);
    ok('Se o Android não conseguir salvar, o app avisa e não quebra', /Não foi possível salvar o arquivo/.test(await p.locator('.toast').innerText()) && p.errs.length === 0, p.errs.join('|'));
    await ctx.close();
  }

  // Foto grande de comprovante é reduzida
  {
    const { p, ctx } = await nova();
    const png = await p.evaluate(() => {
      const c = document.createElement('canvas'); c.width = 3000; c.height = 2000;
      const g = c.getContext('2d'), d = g.createImageData(3000, 2000);
      for (let i = 0; i < d.data.length; i += 4) { d.data[i] = (i * 7) % 255; d.data[i + 1] = (i * 13) % 255; d.data[i + 2] = (i / 4) % 255; d.data[i + 3] = 255; }
      g.putImageData(d, 0, 0);
      return c.toDataURL('image/png').split(',')[1];
    });
    const buf = Buffer.from(png, 'base64');
    await p.locator('.crow.atrasada .btn.primary').first().click();
    await p.setInputFiles('#fcomp', { name: 'foto-comprovante.png', mimeType: 'image/png', buffer: buf });
    await p.waitForFunction(() => /Anexado:/.test(document.getElementById('compname').textContent));
    ok('Confirmar fica liberado quando o anexo está pronto', !(await p.locator('[data-act=baixa]').isDisabled()));
    await p.click('[data-act=baixa]');
    const comp = await p.evaluate(async () => {
      const S = JSON.parse(localStorage.getItem('baixa-pix-v2'));
      const c = S.contas.find(x => x.comprovante && x.comprovante.nome === 'foto-comprovante.png').comprovante;
      const img = new Image(); img.src = c.data; await img.decode();
      return { tipo: c.tipo, tam: c.data.length, w: img.width, h: img.height };
    });
    ok(`Foto de ${(buf.length / 1048576).toFixed(1)} MB vira JPEG de até 1280 px`, comp.tipo === 'image/jpeg' && Math.max(comp.w, comp.h) <= 1280 && comp.w === 1280, JSON.stringify(comp));
    ok('Foto reduzida ocupa menos de 1 MB no armazenamento', comp.tam < 1048576, `${comp.tam} bytes`);
    await ctx.close();
  }

  // Memória cheia
  {
    const { p, ctx } = await nova(() => {
      const orig = Storage.prototype.setItem;
      Storage.prototype.setItem = function (k, v) {
        if (window.__cheio && k === 'baixa-pix-v2') throw new DOMException('cheio', 'QuotaExceededError');
        return orig.call(this, k, v);
      };
    });
    await p.evaluate(() => { window.__cheio = true; });
    await p.locator('.crow.atrasada .btn.primary').first().click();
    await p.click('[data-act=baixa]');
    await p.waitForTimeout(250);
    ok('Memória cheia: o app avisa em vez de perder os dados calado', /memória do app está cheia/.test(await p.locator('.toast').innerText()));
    ok('Sem erros de JavaScript', p.errs.length === 0, p.errs.join('|'));
    await ctx.close();
  }
  await b.close();
  console.log(out.join('\n'));
  if (out.some(l => l.startsWith('✖'))) process.exitCode = 1;
})().catch(e => { console.error('ERRO NO TESTE', e); process.exit(1); });
