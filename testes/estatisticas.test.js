const { chromium } = require('playwright');const fs=require('fs');
const out=[];const ok=(n,c,d='')=>out.push((c?'✔ ':'✖ ')+n+(c?'':' → '+d));
(async()=>{const b=await chromium.launch();
const mk=async(pre)=>{const ctx=await b.newContext({viewport:{width:390,height:844},timezoneId:'America/Sao_Paulo',locale:'pt-BR'});const p=await ctx.newPage();p.errs=[];p.on('pageerror',e=>p.errs.push(e.message));
 if(pre)await p.addInitScript(pre);
 await p.route('**/*',r=>{const u=r.request().url();if(u.includes('xlsx'))return r.fulfill({contentType:'application/javascript',body:fs.readFileSync(__dirname+'/vendor/xlsx.js','utf8')});if(u.startsWith('file://'))return r.continue();return r.fulfill({status:200,body:''})});
 await p.goto('file://'+__dirname+'/.build/page.html');await p.waitForSelector('.crow, .empty');return {p,ctx}};
const num=t=>parseFloat(t.replace(/[^\d,]/g,'').replace(',','.'));
// ---- dados de exemplo
{const {p,ctx}=await mk();
 await p.click('button[data-tab=numeros]');await p.waitForSelector('.statbox');
 const st=await p.evaluate(()=>{p=document;return {S:JSON.parse(localStorage.getItem('baixa-pix-v2')||'null')}});
 // força gravação do estado: qualquer ação salva; usa restaurar
 await p.evaluate(()=>document.getElementById('reset').click());await p.waitForTimeout(150);
 await p.click('button[data-tab=numeros]');
 const S=await p.evaluate(()=>JSON.parse(localStorage.getItem('baixa-pix-v2')));
 const D=await p.evaluate(()=>{const d=new Date();const f=x=>x.getFullYear()+'-'+String(x.getMonth()+1).padStart(2,'0')+'-'+String(x.getDate()).padStart(2,'0');return {today:f(d),ini1:f(new Date(d.getFullYear(),d.getMonth(),1)),ini3:f(new Date(d.getFullYear(),d.getMonth()-2,1)),ini12:f(new Date(d.getFullYear(),d.getMonth()-11,1))}});
 const days=(a,b)=>Math.round((new Date(b+'T12:00:00')-new Date(a+'T12:00:00'))/864e5);
 const sum=l=>l.reduce((a,c)=>a+c.valor,0);
 const exp=ini=>{const rec=S.contas.filter(c=>c.pagoEm&&c.pagoEm>=ini&&c.pagoEm<=D.today),due=S.contas.filter(c=>c.venc>=ini&&c.venc<=D.today),ab=S.contas.filter(c=>!c.pagoEm),atr=ab.filter(c=>c.venc<D.today);
  return {rec:sum(rec),ab:sum(ab),atr:sum(atr),pct:due.length?Math.round(sum(due.filter(c=>c.pagoEm))/sum(due)*100):null,prazo:rec.length?Math.round(rec.reduce((a,c)=>a+days(c.venc,c.pagoEm),0)/rec.length):null,comp:rec.length?Math.round(rec.filter(c=>c.comprovante).length/rec.length*100):null,ticket:due.length?sum(due)/due.length:null}};
 for(const [n,ini] of [[1,D.ini1],[3,D.ini3],[12,D.ini12]]){
  await p.click(`.chip[data-p="${n}"]`);
  const e=exp(ini);
  const big=num(await p.locator('.sum .big strong').innerText());
  ok(`Recebido no período (${n} m): R$ ${e.rec}`,Math.abs(big-e.rec)<0.01,`tela ${big}`);
  const mini=await p.locator('.sum .mini strong').allInnerTexts();
  ok(`Em aberto e em atraso batem com os dados (${n} m)`,Math.abs(num(mini[0])-e.ab)<0.01&&Math.abs(num(mini[1])-e.atr)<0.01,mini.join('|'));
  const pctTxt=await p.locator('.pct').count()?await p.locator('.pct').innerText():null;
  ok(`Taxa de recebimento correta (${n} m): ${e.pct}%`,(e.pct===null&&pctTxt===null)||pctTxt===e.pct+'%',pctTxt);
  const tiles=await p.locator('.tile strong').allInnerTexts();
  const pz=e.prazo===null?'—':(e.prazo===0?'No dia':Math.abs(e.prazo)+(Math.abs(e.prazo)===1?' dia':' dias'));
  ok(`Prazo médio correto (${n} m): ${pz}`,tiles[0]===pz,tiles[0]);
  ok(`Pagas com comprovante correto (${n} m)`,tiles[2]===(e.comp===null?'—':e.comp+'%'),tiles[2]);
  ok(`Valor médio por conta correto (${n} m)`,e.ticket===null?tiles[1]==='—':Math.abs(num(tiles[1])-e.ticket)<0.01,tiles[1]+' vs '+e.ticket);
 }
 // atrasos por tempo e devedores
 const atr=S.contas.filter(c=>!c.pagoEm&&c.venc<D.today);
 const fx=[[1,7],[8,30],[31,99999]].map(([a,z])=>atr.filter(c=>{const d=days(c.venc,D.today);return d>=a&&d<=z}));
 const rows=await p.locator('.bar-row').allInnerTexts();
 const aging=rows.slice(6);
 ok('Atrasos por tempo somam o total em atraso',Math.abs(aging.reduce((a,t)=>a+num(t.split('·')[0].split('\n').pop()),0)-sum(atr))<0.01,JSON.stringify(aging));
 ok('Atrasos por tempo: quantidade por faixa correta',aging.every((t,i)=>/·\s*(\d+)/.exec(t)[1]==String(fx[i].length)),JSON.stringify(aging));
 const por={};atr.forEach(c=>por[c.clienteId]=(por[c.clienteId]||0)+c.valor);
 const top=Object.entries(por).sort((a,b)=>b[1]-a[1])[0];
 const topName=S.clientes.find(c=>c.id===top[0]).nome;
 const first=await p.locator('button.row[data-act=cliente]').first().innerText();
 ok('"Quem mais deve" lista primeiro o maior devedor',first.startsWith(topName)&&Math.abs(num(first.split('\n')[1])-top[1])<0.01,first);
 await p.locator('button.row[data-act=cliente]').first().click();
 ok('Tocar no devedor abre o cliente',(await p.locator('.sheet h2').innerText())===topName);
 await p.click('[data-act=close]');
 const meses=await p.locator('.bar-row').allInnerTexts();
 ok('Gráfico de meses tem 6 barras, com o mês atual marcado',meses.slice(0,6).length===6&&/hoje/.test(meses[5]),meses.slice(0,6).join('|'));
 ok('Meses anteriores têm recebimentos no exemplo',meses.slice(0,5).every(t=>num(t.split('\n').pop())>0),meses.slice(0,5).join('|'));
 const txt=await p.locator('#screen').innerText();
 ok('Sem NaN, undefined ou Infinity na tela',!/NaN|undefined|Infinity/.test(txt),'');
 ok('Sem erros de JavaScript',p.errs.length===0,p.errs.join('|'));
 await p.screenshot({path:__dirname+'/capturas/numeros-claro.png'});
 await p.evaluate(()=>document.getElementById('screen').scrollTo(0,9999));await p.screenshot({path:__dirname+'/capturas/numeros-fim.png'});
 await ctx.close();}
// ---- sem dados
{const {p,ctx}=await mk(()=>{try{localStorage.setItem('baixa-pix-v2',JSON.stringify({clientes:[],contas:[],ultimoBackup:null}))}catch(e){}});
 await p.click('button[data-tab=numeros]');
 const txt=await p.locator('#screen').innerText();
 ok('Sem contas: mostra mensagem amigável',/Cadastre contas/.test(txt),txt);
 ok('Sem contas: sem NaN/Infinity',!/NaN|undefined|Infinity/.test(txt),txt);
 ok('Sem contas: sem erros de JavaScript',p.errs.length===0,p.errs.join('|'));
 await ctx.close();}
// ---- só contas futuras (nada vencido, nada pago)
{const {p,ctx}=await mk(()=>{try{const d=new Date();d.setDate(d.getDate()+10);const v=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');localStorage.setItem('baixa-pix-v2',JSON.stringify({clientes:[{id:'a',nome:'A'}],contas:[{id:'1',clienteId:'a',descricao:'x',valor:100,venc:v,pagoEm:null,comprovante:null}],ultimoBackup:null}))}catch(e){}});
 await p.click('button[data-tab=numeros]');
 const txt=await p.locator('#screen').innerText();
 ok('Só contas futuras: taxa mostra aviso, sem divisão por zero',/Ainda não há contas vencidas/.test(txt)&&!/NaN|Infinity/.test(txt),txt);
 ok('Só contas futuras: prazo médio e comprovante mostram traço',(await p.locator('.tile strong').allInnerTexts()).filter(t=>t==='—').length>=2);
 await ctx.close();}
await b.close();console.log(out.join('\n'));if(out.some(l=>l.startsWith('✖')))process.exitCode=1})()
