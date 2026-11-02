const { chromium } = require('playwright');const fs=require('fs');const path=require('path');
(async()=>{const b=await chromium.launch();const out=[];const ok=(n,c,d='')=>{out.push((c?'✔ ':'✖ ')+n+(c?'':' → '+d))};
for(const os of ['light','dark']){
 const ctx=await b.newContext({viewport:{width:390,height:844},colorScheme:os});const p=await ctx.newPage();const errs=[];p.on('pageerror',e=>errs.push(e.message));
 await p.route('**/*',r=>{const u=r.request().url();if(u.includes('xlsx'))return r.fulfill({contentType:'application/javascript',body:fs.readFileSync(__dirname+'/vendor/xlsx.js','utf8')});if(u.startsWith('file://'))return r.continue();return r.fulfill({status:200,body:''})});
 await p.goto('file://'+__dirname+'/.build/page.html');await p.waitForSelector('.crow');
 const bg=()=>p.evaluate(()=>getComputedStyle(document.body).backgroundColor);
 const initial=await bg();
 ok(`Tema claro é o padrão, mesmo com celular no ${os==='dark'?'escuro':'claro'}`, initial==='rgb(240, 248, 247)',initial);
 await p.click('[data-act=abrir-menu]');await p.click('#screen [data-act=aparencia]');await p.waitForSelector('.sheet .seg');await p.waitForTimeout(400);
 ok(`Menu › Tema abre a janela com 3 opções (${os})`,(await p.locator('.seg button').count())===3);
 ok(`A janela do Tema é só os 3 botões, sem tabela de cores de designer (${os})`,(await p.locator('.sheet .pal, .sheet .sw').count())===0&&/^Tema/.test(await p.locator('.sheet h2').innerText()));
 await p.click('.seg button[data-t=dark]');await p.waitForTimeout(100);
 ok(`Escuro força o tema escuro (${os})`,(await bg())==='rgb(6, 18, 19)'&&(await p.locator('.seg button[data-t=dark]').getAttribute('aria-checked'))==='true',await bg());
 await p.click('.seg button[data-t=light]');await p.waitForTimeout(100);
 ok(`Claro força o tema claro (${os})`,(await bg())==='rgb(240, 248, 247)',await bg());
 await p.screenshot({path:__dirname+`/capturas/paleta-${os}.png`});
 await p.click('.seg button[data-t=auto]');await p.waitForTimeout(100);
 ok(`Automático segue o celular (${os})`,(await bg())===(os==='dark'?'rgb(6, 18, 19)':'rgb(240, 248, 247)'),await bg());
 // contraste dos pares de cor do app (a tabela de cores saiu da tela; a conferência continua aqui e em docs/marca.md)
 const ratios=await p.evaluate(()=>{
  const lum=h=>{const c=[1,3,5].map(i=>{const v=parseInt(h.slice(i,i+2),16)/255;return v<=.03928?v/12.92:Math.pow((v+.055)/1.055,2.4)});return .2126*c[0]+.7152*c[1]+.0722*c[2]};
  const rt=(a,b)=>{const x=lum(a),y=lum(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05)};
  const tema=(os=>{const r=document.documentElement;return r.getAttribute('data-theme')})();
  const v=n=>getComputedStyle(document.documentElement).getPropertyValue('--'+n).trim();
  const pares=[['fg','bg'],['fg','surface'],['muted','bg'],['muted','surface'],['muted','surface2'],['accent-fg','accent'],['accent','surface'],['accent','accent-soft'],['ok','ok-bg'],['warn','warn-bg'],['bad','bad-bg'],['ok','surface2'],['bad','surface2']];
  return pares.map(([a,b])=>({par:a+'/'+b,r:Math.round(rt(v(a),v(b))*10)/10}));
 });
 ok(`Pares de cor do app com contraste ≥ 4,5:1 (${os}, tema ${await p.evaluate(()=>document.documentElement.getAttribute('data-theme')||'automático')})`,ratios.length===13&&ratios.every(x=>x.r>=4.5),JSON.stringify(ratios.filter(x=>x.r<4.5)));
 await p.click('.seg button[data-t=dark]');await p.click('[data-act=close]');
 await p.reload();await p.waitForSelector('.crow');
 ok(`Escolha de tema é lembrada ao recarregar (${os})`,(await bg())==='rgb(6, 18, 19)');
 ok(`Sem erros de JavaScript (${os})`,errs.length===0,errs.join('|'));
 await ctx.close();}
await b.close();console.log(out.join('\n'));if(out.some(l=>l.startsWith('✖')))process.exitCode=1})()
