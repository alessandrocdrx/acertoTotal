// Gera as 8 capturas da Play a partir do app atual (versão de produção, sem faixa de teste).
// Uso: bash testes/rodar.sh (monta testes/.build/page.html) e depois node playstore/gerar-capturas.js
// LANG=pt_BR faz o campo de data mostrar 09/11/2026, e não 11/09/2026.
const { chromium } = require('playwright');const fs=require('fs');
const path=require('path'),os=require('os');const RAIZ=path.join(__dirname,'..'),OUT=RAIZ+'/playstore/',S=fs.mkdtempSync(path.join(os.tmpdir(),'capturas-'))+'/';
const C={ "claro": {  "bg": "#f0f8f7",  "surface": "#ffffff",  "surface2": "#ebf5f4",  "fg": "#0a2225",  "muted": "#536869",  "line": "#d5e1e0",  "accent": "#00766c",  "accent_soft": "#dcf7f3",  "frame": "#0a2225",  "sol": "#fbb636",  "ok": "#15733c",  "ok_bg": "#d9f8e0",  "warn": "#8f5d04",  "warn_bg": "#ffecc1",  "bad": "#b6322d",  "bad_bg": "#ffe2dd" }, "escuro": {  "bg": "#061213",  "surface": "#0e1c1e",  "surface2": "#162627",  "fg": "#e7f1f0",  "muted": "#9db3b3",  "line": "#263637",  "accent": "#49d3c3",  "accent_soft": "#063e3a",  "frame": "#020506",  "sol": "#ffc249",  "ok": "#78d694",  "ok_bg": "#193521",  "warn": "#f5c25f",  "warn_bg": "#3e2d10",  "bad": "#f98f84",  "bad_bg": "#47211d" }}; // tokens de docs/marca.md
const L=C.claro,E=C.escuro;
const FONT=`<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:wght@500;700&family=IBM+Plex+Sans:wght@400;500;600&display=block">`;
const VERSAO=(fs.readFileSync(RAIZ+'/android/app/build.gradle.kts','utf8').match(/versionName = "([^"]+)"/)||[])[1]||'';
(async()=>{const b=await chromium.launch({args:['--lang=pt-BR'],env:{...process.env,LANG:'pt_BR.UTF-8',LANGUAGE:'pt_BR'}});
const ctx=await b.newContext({viewport:{width:390,height:780},deviceScaleFactor:2.4,locale:'pt-BR',timezoneId:'America/Sao_Paulo'});
const a=await ctx.newPage();
await a.addInitScript(v=>{window.ACERTO_AMBIENTE='producao';window.ACERTO_LIMITE_DIAS=0;window.ACERTO_VERSAO=v},VERSAO);
await a.route('**/*',r=>{const u=r.request().url();if(u.includes('xlsx'))return r.fulfill({contentType:'application/javascript',body:fs.readFileSync(RAIZ+'/testes/vendor/xlsx.js','utf8')});return r.continue()});
await a.goto('file://'+RAIZ+'/testes/.build/page.html');await a.waitForSelector('.crow');await a.waitForTimeout(2500);
// Comprovante genérico de transferência (ilustração, sem marca de banco)
const jpg=await a.evaluate(()=>{const c=document.createElement('canvas');c.width=600;c.height=420;const g=c.getContext('2d');
 g.fillStyle='#f4f7f6';g.fillRect(0,0,600,420);g.fillStyle='#fff';g.fillRect(30,30,540,360);g.fillStyle='#0a2225';
 g.font='bold 30px sans-serif';g.fillText('Comprovante de transferência',60,90);g.font='24px sans-serif';g.fillStyle='#536869';
 g.fillText('Pix recebido',60,140);g.fillStyle='#15733c';g.font='bold 54px sans-serif';g.fillText('R$ 1.250,00',60,215);
 g.fillStyle='#536869';g.font='24px sans-serif';g.fillText('De: Padaria Bom Pão',60,275);g.fillText('Data: 02/11/2026 · 10:42',60,315);
 return c.toDataURL('image/jpeg',.9).split(',')[1]});
const compId=await a.locator('.crow .btn.primary').first().getAttribute('data-id');
await a.click('.crow .btn.primary');
await a.setInputFiles('#fcomp',{name:'comprovante-pix.jpg',mimeType:'image/jpeg',buffer:Buffer.from(jpg,'base64')});
await a.waitForFunction(()=>/Anexado/.test(document.getElementById('compname').textContent));
await a.click('[data-act=baixa]');await a.waitForTimeout(300);
// Cópia de segurança feita hoje (sem ponto vermelho no ☰ nem "Nunca feita") e "Meu negócio" preenchido (a cobrança mostra o Pix)
await a.evaluate(()=>{const s=JSON.parse(localStorage.getItem('baixa-pix-v2'));s.ultimoBackup=new Date().toISOString().slice(0,10);s.negocio={nome:'Ana Costa · Doces e Bolos',pix:'contato@anacostadoces.com.br'};localStorage.setItem('baixa-pix-v2',JSON.stringify(s))});
await a.reload();await a.waitForSelector('.crow');await a.waitForTimeout(1500);
const raw=[];const tela=async n=>{await a.waitForTimeout(600);const f=S+'raw3-'+n+'.png';await a.screenshot({path:f});raw.push(f)};
const volta=async()=>{await a.evaluate(()=>{while(window.acertoVoltar&&window.acertoVoltar());document.getElementById('screen').scrollTop=0});await a.waitForTimeout(200)};
// A ordem importa: as primeiras capturas são as que mais aparecem na loja.
await tela(1);                                                                                   // 1 Contas
await a.click('.crow .cmain');await a.click('.sheet [data-act=cobrar]');await tela(2);           // 2 Cobrar pelo WhatsApp
await volta();await a.click('.strip [data-f=pagas]');await a.click(`.cmain[data-id="${compId}"]`);await tela(3); // 3 Comprovante
await volta();await a.click('.tabs [data-tab=clientes]');await tela(4);                         // 4 Clientes
await a.click('.tabs [data-tab=numeros]');await a.click('[data-act=periodo][data-p="3"]');await tela(5); // 5 Números, 3 meses
await a.click('.tabs [data-tab=planilha]');await tela(6);                                       // 6 Planilha
await a.click('#screen [data-act=import-abrir]');await a.click('details.como > summary');await a.evaluate(()=>{document.querySelector('.sheet').scrollTop=0});await tela(7); // 7 Importar
await volta();await a.click('.tabs [data-tab=contas]');await a.click('.strip [data-f=pagas]');await a.click('.fab'); // tira o filtro de Pagas
await a.selectOption('#nc-cli',{index:3});await a.fill('#nc-desc','Mensalidade de dezembro');await a.fill('#nc-val','150,00');
await a.click('.sheet h2');await tela(8);                                                       // 8 Nova conta preenchida
const TIT=[['Quem deve, quanto','e desde quando.'],['Cobre no WhatsApp,','texto já pronto.'],['Dê baixa e guarde','o comprovante.'],['Quanto cada','cliente deve.'],['Quanto entrou','e quanto falta.'],['Excel de ida','e de volta.'],['Confira tudo','antes de gravar.'],['Cadastre uma conta','em poucos toques.']];
const NOMES=['contas','cobrar','comprovante','clientes','numeros','planilha','importar','nova-conta'];
fs.readdirSync(OUT+'capturas').forEach(f=>fs.unlinkSync(OUT+'capturas/'+f));
const p=await b.newPage({viewport:{width:1080,height:1920}});
for(let i=0;i<8;i++){
 const img='data:image/png;base64,'+fs.readFileSync(raw[i]).toString('base64');
 const escuro=i%2===1;
 await p.setContent(`<!doctype html>${FONT}<body style="margin:0;width:1080px;height:1920px;overflow:hidden;background:${escuro?E.bg:L.accent};font-family:'Bricolage Grotesque';position:relative">
 <div style="position:absolute;inset:0;background:radial-gradient(900px 700px at 85% 0%, ${L.sol}55, transparent 60%)"></div>
 <div style="position:relative;padding:110px 90px 0;color:#fff"><div style="font-size:42px;font-weight:500;letter-spacing:-.5px;color:#fff">acerto<b style="font-weight:700;color:${L.sol}">Total</b></div>
 <div style="font-size:92px;line-height:1.02;font-weight:700;letter-spacing:-1.5px;margin-top:18px">${TIT[i][0]}<br><span style="color:${escuro?E.accent:'#fff'}">${TIT[i][1]}</span></div></div>
 <div style="position:absolute;left:50%;top:560px;transform:translateX(-50%);width:780px;height:1560px;border-radius:84px;background:${L.frame};padding:22px;box-shadow:0 40px 120px #0006">
 <img src="${img}" style="width:100%;height:100%;object-fit:cover;object-position:top;border-radius:64px;display:block"></div></body>`);
 await p.evaluate(()=>document.fonts.ready);await p.waitForTimeout(300);
 await p.screenshot({path:OUT+`capturas/captura-${i+1}-${NOMES[i]}.png`});
}
await b.close();console.log('ok');})();
