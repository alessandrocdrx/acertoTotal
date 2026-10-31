#!/usr/bin/env bash
# Roda todos os testes automatizados do protótipo CelsoPix.
# Uso: bash testes/rodar.sh
set -u
cd "$(dirname "$0")"
mkdir -p vendor .build capturas

# Bibliotecas usadas só nos testes (não vão para o repositório)
[ -s vendor/xlsx.js ] || curl -sSf -o vendor/xlsx.js https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js
[ -s vendor/axe.js ] || curl -sSf -o vendor/axe.js https://cdnjs.cloudflare.com/ajax/libs/axe-core/4.10.2/axe.min.js

# Monta a página como o artefato publica: o protótipo dentro de um esqueleto HTML
{
  printf '<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">\n'
  printf '<style>:root{color-scheme:light}body{margin:0;font-size:14px}</style>\n'
  cat ../prototipo/celsopix.html
} > .build/page.html

# Playwright: usa a instalação local ou a global
export NODE_PATH="${NODE_PATH:-}:$(npm root -g 2>/dev/null)"

falhas=0
for t in app estatisticas tema usabilidade; do
  echo
  echo "==================== $t ===================="
  node "$t.test.js" || falhas=$((falhas+1))
done
echo
if [ "$falhas" -eq 0 ]; then echo "RESULTADO: todas as suítes passaram"; else echo "RESULTADO: $falhas suíte(s) com falha"; fi
exit "$falhas"
