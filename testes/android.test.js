// Testes do que muda no APK: página sem internet, ponte de download do Android,
// fotos de comprovante reduzidas e aviso de memória cheia.
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const SRC = path.join(__dirname, '..', 'android', 'app', 'src');
const ASSETS = path.join(SRC, 'homolog', 'assets');
const out = [];
const ok = (nome, cond, det = '') => out.push(`${cond ? '✔' : '✖'} ${nome}${cond ? '' : ' → ' + det}`);

(async () => {
  const html = fs.readFileSync(path.join(ASSETS, 'index.html'), 'utf8');
  // Links que a pessoa toca (Play, GitHub do autor e do projeto) abrem no navegador e não são baixados pela página.
  const semLinks = html.replace(/http:\/\/www\.w3\.org\/2000\/svg/g, '').replace(/https:\/\/play\.google\.com\/store\/apps\/details\?id=io\.github\.alessandrocdrx\.acertototal/g, '').replace(/https:\/\/github\.com\/alessandrocdrx(\/acertoTotal)?/g, '').replace(/https:\/\/wa\.me\//g, '');
  ok('Links do Menu só existem como links (não como script, estilo ou imagem)', !/<(script|link|img)[^>]+(github|play\.google|wa\.me)/.test(html));
  ok('APK não depende de internet: nenhum endereço externo na página', !/https?:\/\//.test(semLinks), (semLinks.match(/https?:\/\/[^"' )]+/g) || []).join(', '));
  const kt = fs.readFileSync(path.join(SRC, 'main', 'java', 'io', 'github', 'alessandrocdrx', 'acertototal', 'MainActivity.kt'), 'utf8');
  ok('Botão Voltar do Android usa window.acertoVoltar (fecha janela, sai do Menu, depois sai do app)', /JS_FECHAR_JANELA =\s*"\(function\(\)\{return !!\(window\.acertoVoltar&&window\.acertoVoltar\(\)\)\}\)\(\)"/.test(kt) && /window\.acertoVoltar=function/.test(html));
  const ponte = fs.readFileSync(path.join(SRC, 'main', 'java', 'io', 'github', 'alessandrocdrx', 'acertototal', 'Ponte.kt'), 'utf8');
  ok('Ponte Android salva comprovantes na pasta Downloads (jpg, png e pdf)', ['"jpg" to "image/jpeg"', '"png" to "image/png"', '"pdf" to "application/pdf"'].every(x => ponte.includes(x)));
  const manif = fs.readFileSync(path.join(SRC, 'main', 'AndroidManifest.xml'), 'utf8');
  ok('Comprovante abre e é compartilhado pelos apps do celular (FileProvider, sem permissão de armazenamento)', /fun abrirArquivo\(/.test(ponte) && /fun compartilharArquivo\(/.test(ponte) && /androidx\.core\.content\.FileProvider/.test(manif) && /android:exported="false"/.test(manif.split('<provider')[1] || '') && fs.existsSync(path.join(SRC, 'main', 'res', 'xml', 'arquivos.xml')) && !/STORAGE/.test(manif));
  // Contrato entre a casca Android e a página (a falha de 02/11 passou porque o teste imitava um estado que o Kotlin nunca produz):
  const assin = fs.readFileSync(path.join(SRC, 'main', 'java', 'io', 'github', 'alessandrocdrx', 'acertototal', 'Assinatura.kt'), 'utf8');
  const estadoKt = (assin.match(/fun estado\(\)[\s\S]*?\.toString\(\)/) || [''])[0];
  const camposKt = [...estadoKt.matchAll(/\.put\("(\w+)"/g)].map(m => m[1]).sort();
  const lidos = ['real', 'ativa', 'pronto', 'precos', 'plano', 'motivo'];
  ok('Contrato: o JSON do Kotlin traz os campos que a página lê (ativa, pronto, precos, motivo)', ['real', 'ativa', 'pronto', 'precos', 'motivo'].every(c => camposKt.includes(c)), camposKt.join(','));
  const motivosKt = [...new Set([...assin.matchAll(/motivo = (?:if \(pronto\) "" else )?"(\w*)"/g)].map(m => m[1]))].sort();
  ok('Contrato: os motivos que o Kotlin produz ("", "play", "sem_planos") são todos tratados pela página', JSON.stringify(motivosKt) === JSON.stringify(['', 'play', 'sem_planos']) && /motivo==='sem_planos'/.test(html) && /motivo==='play'/.test(html), motivosKt.join('|'));
  ok('Contrato: só quando não há planos, o Kotlin diz "pronto: false" com motivo "sem_planos" (a página não depende de pronto=true com preços vazios)', /pronto = precos\.isNotEmpty\(\)/.test(assin) && /motivo = if \(pronto\) "" else "sem_planos"/.test(assin) && !/function planosForaDaLoja\(\)\{return ASSIN\.real&&ASSIN\.pronto/.test(html));
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

  // Fontes da marca: dentro do pacote, sem internet (relato do cliente: o app instalado tinha cara diferente do artefato)
  {
    const { p, ctx } = await nova();
    const fontes = await p.evaluate(async () => {
      await Promise.all(['700 28px "Bricolage Grotesque"', '600 16px "Bricolage Grotesque"', '400 15px "IBM Plex Sans"', '500 15px "IBM Plex Sans"', '600 15px "IBM Plex Sans"'].map(f => document.fonts.load(f, 'Menu acertoTotal ação')));
      return [...document.fonts].filter(f => f.status === 'loaded').map(f => `${f.family} ${f.weight}`).sort();
    });
    ok('APK: as fontes da marca carregam do próprio pacote (Bricolage 600/700 e Plex 400/500/600)', JSON.stringify(fontes) === JSON.stringify(['Bricolage Grotesque 600', 'Bricolage Grotesque 700', 'IBM Plex Sans 400', 'IBM Plex Sans 500', 'IBM Plex Sans 600'].map(x => x.replace(/^'|'$/g, '')).sort()), JSON.stringify(fontes));
    ok('APK: título e logotipo usam a fonte da marca, e o texto usa a Plex', await p.evaluate(() => /Bricolage Grotesque/.test(getComputedStyle(document.getElementById('title')).fontFamily) && /IBM Plex Sans/.test(getComputedStyle(document.body).fontFamily)));
    ok('APK: nenhuma fonte vem da internet (arquivos .woff2 dentro de assets/fontes)', fs.readdirSync(path.join(ASSETS, 'fontes')).filter(x => x.endsWith('.woff2')).length === 5 && !/fonts\.(googleapis|gstatic)/.test(html));
    ok('APK: licença das fontes (OFL) registrada', fs.existsSync(path.join(__dirname, '..', 'android', 'fontes', 'LICENCAS.md')));
    await ctx.close();
  }

  // Ponte de download
  {
    const { p, ctx } = await nova(() => {
      window.acertoTotalAndroid = { salvarArquivo: (nome, b64) => { window.__salvo = { nome, b64 }; return 'saved'; } };
    });
    ok('Sem internet: app abre e funciona', (await p.locator('.crow').count()) > 0 && typeof (await p.evaluate(() => typeof XLSX)) === 'string' && (await p.evaluate(() => typeof XLSX)) === 'object');
    await p.click('button[data-tab=planilha]');
    await p.click('[data-act=export-abrir]');
    await p.click('[data-act=export-custom]');
    await p.click('[data-act=xlsx-export]');
    await p.waitForFunction(() => window.__salvo);
    const r = await p.evaluate(() => {
      const bin = atob(window.__salvo.b64), u = new Uint8Array(bin.length);
      for (let i = 0; i < bin.length; i++) u[i] = bin.charCodeAt(i);
      const wb = XLSX.read(u, { type: 'array' });
      return { nome: window.__salvo.nome, linhas: XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]], { header: 1 }) };
    });
    ok('Exportar no Android entrega um .xlsx válido para a ponte', /^contas-atrasadas-\d{4}-\d{2}-\d{2}\.xlsx$/.test(r.nome) && r.linhas[0][0] === 'Cliente' && r.linhas.length > 1, JSON.stringify(r.linhas[0]));
    ok('Depois de salvar, o app confirma', /Arquivo Excel salvo/.test(await p.locator('.toast').innerText()));
    await ctx.close();
  }
  {
    const { p, ctx } = await nova(() => { window.acertoTotalAndroid = { salvarArquivo: () => 'unavailable' }; });
    await p.click('button[data-tab=planilha]'); await p.click('[data-act=export-backup]'); await p.click('[data-act=xlsx-export]');
    await p.waitForTimeout(300);
    ok('Se o Android não conseguir salvar, o app avisa e não quebra', /Não foi possível salvar o arquivo/.test(await p.locator('.toast').innerText()) && p.errs.length === 0, p.errs.join('|'));
    await ctx.close();
  }

  // Ambientes: o APK de teste mostra a faixa, o de produção não
  for (const [amb, deveMostrar] of [['homolog', true], ['producao', false]]) {
    const ctx = await b.newContext({ viewport: { width: 390, height: 844 } });
    const p = await ctx.newPage();
    await p.route('**/*', r => r.request().url().startsWith('file://') ? r.continue() : r.abort());
    await p.goto('file://' + path.join(SRC, amb, 'assets', 'index.html'));
    await p.waitForSelector('.crow');
    const vis = await p.locator('#ambiente').isVisible();
    ok(`APK ${amb}: faixa "Ambiente de teste" ${deveMostrar ? 'aparece' : 'não aparece'}`, vis === deveMostrar, `visível=${vis}`);
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
  // Marca: o mesmo símbolo no app, no ícone do Android e nos arquivos da marca (docs/marca.md)
  {
    const raiz = path.join(__dirname, '..');
    const [anel, visto] = fs.readFileSync(path.join(raiz, 'marca', 'geom.txt'), 'utf8').split('\n');
    const res = path.join(raiz, 'android', 'app', 'src', 'main', 'res');
    const fg = fs.readFileSync(path.join(res, 'drawable', 'ic_launcher_foreground.xml'), 'utf8');
    const mono = fs.readFileSync(path.join(res, 'drawable', 'ic_launcher_monochrome.xml'), 'utf8');
    const adapt = fs.readFileSync(path.join(res, 'mipmap-anydpi', 'ic_launcher.xml'), 'utf8');
    const tema = fs.readFileSync(path.join(res, 'values', 'themes.xml'), 'utf8');
    const html = fs.readFileSync(path.join(raiz, 'prototipo', 'acertototal.html'), 'utf8');
    ok('Marca: ícone do Android usa a geometria do símbolo (anel e visto)', fg.includes(anel) && fg.includes(visto) && mono.includes(anel));
    ok('Marca: ícone adaptativo com fundo Verde Acerto e camada monocromática própria', /verde_acerto/.test(adapt) && /ic_launcher_monochrome/.test(adapt) && /#00766C/i.test(tema));
    ok('Marca: o cabeçalho do app usa o mesmo símbolo, com o visto na cor Sol', html.includes(anel) && /stroke="var\(--sol\)"/.test(html) && /--sol:#fbb636/.test(html));
    ok('Marca: arquivos do símbolo e do logotipo existem', ['simbolo.svg', 'simbolo-mono-tinta.svg', 'logo-horizontal.svg', 'logo-horizontal-negativo.svg', 'assinatura.svg'].every(f => fs.existsSync(path.join(raiz, 'marca', f))));
  }
  await b.close();
  console.log(out.join('\n'));
  if (out.some(l => l.startsWith('✖'))) process.exitCode = 1;
})().catch(e => { console.error('ERRO NO TESTE', e); process.exit(1); });
