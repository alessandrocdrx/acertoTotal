#!/usr/bin/env bash
# Copia o protótipo testado para dentro do APK, pronto para funcionar sem internet:
# - troca o leitor de Excel do CDN por uma cópia local;
# - remove as fontes do Google (o app usa as fontes do sistema);
# - injeta a ponte de download do Android no lugar da permissão do artefato;
# - marca o ambiente (teste ou producao).
set -euo pipefail
cd "$(dirname "$0")"
XLSX=../testes/vendor/xlsx.js
[ -s "$XLSX" ] || { mkdir -p ../testes/vendor; curl -sSf -o "$XLSX" https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js; }

# Um conjunto de páginas por variante do app: homolog (ambiente "teste", mostra a faixa
# de aviso) e producao (sem faixa). O Android não aceita variante com nome começando por "test".
for PAR in homolog:teste producao:producao; do
VARIANTE=${PAR%%:*}; AMB=${PAR##*:}
DEST=app/src/$VARIANTE/assets
mkdir -p "$DEST"
cp "$XLSX" "$DEST/xlsx.full.min.js"
python3 - ../prototipo/celsopix.html "$DEST/index.html" "$AMB" <<'PY'
import re, sys
src, dst, amb = sys.argv[1], sys.argv[2], sys.argv[3]
s = open(src, encoding='utf-8').read()
s, n1 = re.subn(r'<link rel="stylesheet" href="https://fonts\.googleapis\.com[^>]*>\n?', '', s)
s, n2 = re.subn(r'https://cdnjs\.cloudflare\.com/ajax/libs/xlsx/[^"]+', 'xlsx.full.min.js', s)
assert n1 == 1 and n2 == 1, f'protótipo mudou: fontes={n1} xlsx={n2}'
ponte = """<script>
window.CELSOPIX_AMBIENTE = '""" + amb + """';
// Ponte Android: substitui a permissão de download do artefato.
window.claude = { use: function (nome) {
  if (nome !== 'downloads' || !window.CelsoPixAndroid) return Promise.resolve(null);
  return Promise.resolve({ save: function (r) {
    return new Promise(function (ok, falha) {
      var b = new Uint8Array(r.data), s = '';
      for (var i = 0; i < b.length; i += 0x8000) s += String.fromCharCode.apply(null, b.subarray(i, i + 0x8000));
      var st = window.CelsoPixAndroid.salvarArquivo(r.filename, btoa(s));
      if (st === 'saved') ok({ status: 'saved' }); else falha({ code: st || 'unavailable', message: st });
    });
  } });
} };
</script>"""
head = ('<!doctype html><html lang="pt-BR"><head><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">'
        '<style>:root{color-scheme:light}body{margin:0;font-size:14px}img{max-width:100%}[hidden]{display:none!important}</style>'
        + ponte + '</head><body>\n')
open(dst, 'w', encoding='utf-8').write(head + s + '\n</body></html>\n')
PY
done
echo "assets prontos em app/src/homolog/assets e app/src/producao/assets"
