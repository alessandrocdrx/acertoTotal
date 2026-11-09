// Põe título e moldura de celular nas capturas brutas do app (testes/capturas/play-bruto/, 1080x1920).
// Uso: node playstore/gerar-molduras.js   (usa o Chrome instalado; a fonte vem de android/app/src/main/res/font)
const fs=require('fs'),path=require('path'),os=require('os'),{execFileSync}=require('child_process');
const RAIZ=path.join(__dirname,'..'),BRUTO=RAIZ+'/testes/capturas/play-bruto/',OUT=__dirname+'/capturas/';
const CHROME=['C:/Program Files/Google/Chrome/Application/chrome.exe','C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'].find(fs.existsSync);
const FONTE=p=>'file:///'+path.join(RAIZ,'android/app/src/main/res/font',p).split(path.sep).join('/');
const L={accent:'#00766c',frame:'#0a2225',sol:'#fbb636'},E={bg:'#061213',accent:'#49d3c3'};
const TIT=[['Quem deve, quanto','e desde quando.'],['Cobre no WhatsApp,','texto já pronto.'],['Dê baixa e guarde','o comprovante.'],['Quanto cada','cliente deve.'],['Quanto entrou','e quanto falta.'],['Excel de ida','e de volta.'],['Confira tudo','antes de gravar.'],['Cadastre uma conta','em poucos toques.']];
const NOMES=['contas','cobrar','comprovante','clientes','numeros','planilha','importar','nova-conta'];
const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'molduras-'));
for(let i=0;i<8;i++){
 const img='data:image/png;base64,'+fs.readFileSync(BRUTO+`captura-${i+1}-${NOMES[i]}.png`).toString('base64');
 const escuro=i%2===1;
 const html=`<!doctype html><meta charset="utf-8"><style>@font-face{font-family:B;font-weight:500;src:url(${FONTE('bricolage_600.ttf')})}@font-face{font-family:B;font-weight:700;src:url(${FONTE('bricolage_700.ttf')})}</style>
 <body style="margin:0;width:1080px;height:1920px;overflow:hidden;background:${escuro?E.bg:L.accent};font-family:B;position:relative">
 <div style="position:absolute;inset:0;background:radial-gradient(900px 700px at 85% 0%, ${L.sol}55, transparent 60%)"></div>
 <div style="position:relative;padding:110px 90px 0;color:#fff"><div style="font-size:42px;font-weight:500;letter-spacing:-.5px">acerto<b style="font-weight:700;color:${L.sol}">Total</b></div>
 <div style="font-size:92px;line-height:1.02;font-weight:700;letter-spacing:-1.5px;margin-top:18px">${TIT[i][0]}<br><span style="color:${escuro?E.accent:'#fff'}">${TIT[i][1]}</span></div></div>
 <div style="position:absolute;left:50%;top:560px;transform:translateX(-50%);width:780px;height:1400px;border-radius:84px;background:${L.frame};padding:22px;box-sizing:border-box;box-shadow:0 40px 120px #0006">
 <img src="${img}" style="width:100%;height:auto;border-radius:64px;display:block"></div></body>`;
 const f=path.join(tmp,`p${i}.html`);fs.writeFileSync(f,html);
 execFileSync(CHROME,['--headless=new','--disable-gpu','--hide-scrollbars','--allow-file-access-from-files','--force-device-scale-factor=1','--window-size=1080,1920','--virtual-time-budget=3000',`--screenshot=${OUT}captura-${i+1}-${NOMES[i]}.png`,'file:///'+f.split(path.sep).join('/')],{stdio:'ignore'});
}
console.log('ok');
