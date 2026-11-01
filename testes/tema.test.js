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
 ok(`Menu › Tema e paleta abre a janela com 3 opções (${os})`,(await p.locator('.seg button').count())===3);
 ok(`Paleta lista 9 cores nos dois temas (${os})`,(await p.locator('.pal-r').count())===9);
 await p.click('.seg button[data-t=dark]');await p.waitForTimeout(100);
 ok(`Escuro força o tema escuro (${os})`,(await bg())==='rgb(6, 18, 19)'&&(await p.locator('.seg button[data-t=dark]').getAttribute('aria-checked'))==='true',await bg());
 await p.click('.seg button[data-t=light]');await p.waitForTimeout(100);
 ok(`Claro força o tema claro (${os})`,(await bg())==='rgb(240, 248, 247)',await bg());
 await p.screenshot({path:__dirname+`/capturas/paleta-${os}.png`});
 await p.click('.seg button[data-t=auto]');await p.waitForTimeout(100);
 ok(`Automático segue o celular (${os})`,(await bg())===(os==='dark'?'rgb(6, 18, 19)':'rgb(240, 248, 247)'),await bg());
 // contraste das amostras
 const ratios=await p.evaluate(()=>[...document.querySelectorAll('.sw small')].map(s=>s.textContent).filter(t=>/:1/.test(t)).map(t=>parseFloat(t)));
 ok(`Amostras de texto com contraste ≥ 4,5:1 (${os})`,ratios.length===16&&ratios.every(r=>r>=4.5),JSON.stringify(ratios));
 await p.click('.seg button[data-t=dark]');await p.click('[data-act=close]');
 await p.reload();await p.waitForSelector('.crow');
 ok(`Escolha de tema é lembrada ao recarregar (${os})`,(await bg())==='rgb(6, 18, 19)');
 ok(`Sem erros de JavaScript (${os})`,errs.length===0,errs.join('|'));
 await ctx.close();}
await b.close();console.log(out.join('\n'));if(out.some(l=>l.startsWith('✖')))process.exitCode=1})()
