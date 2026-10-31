# CelsoPix

App de controle de cobranças Pix: cadastrar clientes e contas, dar baixa com o comprovante, ver quem está em atraso, importar e exportar Excel. O protótipo navegável fica em `prototipo/celsopix.html` e é publicado como artefato. O app final será Android nativo.

## Antes de qualquer mudança

1. Leia `docs/aprendizados.md`, principalmente a seção "Regras em vigor".
2. Toda mudança segue as regras. Se uma regra atrapalhar, discuta antes de quebrá-la.

## Comandos

- Testes: `bash testes/rodar.sh` roda as suítes app, estatisticas, tema e usabilidade com Playwright e Chromium.
- As capturas de tela ficam em `testes/capturas/`. Elas não vão para o repositório.

## Regras de trabalho

- Toda correção ganha um teste em `testes/`.
- Todo problema achado, seja por teste, pelo cliente ou numa revisão, vira uma entrada em `docs/aprendizados.md` com causa, correção e como evitar.
- Publique o artefato só com todas as suítes passando.
- Os textos da interface ficam em português do Brasil simples: uma ação por botão, erro que explica como resolver.
