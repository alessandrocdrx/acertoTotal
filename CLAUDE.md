# acertoTotal

App de contas a receber (antes chamado CelsoPix): cadastrar clientes e contas, dar baixa com o comprovante, ver quem está em atraso, importar e exportar Excel. O protótipo navegável fica em `prototipo/acertototal.html` e é publicado como artefato. O app final será Android nativo.

## Nome e padrão da família

Mesmo padrão do geoTotal e do calcTotal: nome `palavraTotal` em camelCase, repositório com o mesmo nome (`acertoTotal`), ID do app `io.github.alessandrocdrx.acertototal`, `PRIVACIDADE.md` na raiz e ficha da loja em `playstore/FICHA.md`. O nome não pode citar um meio de pagamento (Pix): o app vai crescer além de cobranças. O app de teste se chama `acertoTotal Teste`.

## Antes de qualquer mudança

1. Leia `docs/aprendizados.md`, principalmente a seção "Regras em vigor".
2. Toda mudança segue as regras. Se uma regra atrapalhar, discuta antes de quebrá-la.

## Comandos

- Testes: `bash testes/rodar.sh` roda as suítes app, estatisticas, tema, usabilidade, exportacao, importacao, tarefas, avaliacao, assinatura e android com Playwright e Chromium.
- As capturas de tela ficam em `testes/capturas/`. Elas não vão para o repositório.
- APK de teste: `bash android/preparar-assets.sh && cd android && ./gradlew clean assembleHomologRelease --max-workers=1`. O de produção usa `assembleProducaoRelease`. O SDK fica em `/opt/android-sdk` (`android/local.properties`). O arquivo sai em `android/app/build/outputs/apk/<variante>/release/`.
- A interface do APK vem do protótipo. Nunca edite `android/app/src/*/assets/` à mão.

## Branches, CI e lançamento

- A `main` é a versão aprovada. O trabalho de cada sessão vai numa branch própria e só entra na `main` depois dos testes verdes e da aprovação do Alessandro.
- `.github/workflows/android.yml` roda as 10 suítes e compila o APK de teste a cada push. Na `main`, publica o APK numa Release (link fixo: `https://github.com/alessandrocdrx/acertoTotal/releases/latest/download/acertototal.apk`).
- Lançar na Play Store: aumentar `versionCode`/`versionName` em `android/app/build.gradle.kts` e pôr `[release]` na mensagem do commit na `main`. O workflow gera o AAB assinado com a chave de envio, a partir dos Secrets `ACERTOTOTAL_KEYSTORE_BASE64`, `ACERTOTOTAL_KEYSTORE_PASSWORD` e `ACERTOTOTAL_KEY_ALIAS`. Sem os Secrets, ele para com erro. Passo a passo em `playstore/FICHA.md`.

## Regras de trabalho

- Toda correção ganha um teste em `testes/`.
- Todo problema achado, seja por teste, pelo cliente ou numa revisão, vira uma entrada em `docs/aprendizados.md` com causa, correção e como evitar.
- Publique o artefato só com todas as suítes passando.
- Os textos da interface ficam em português do Brasil simples: uma ação por botão, erro que explica como resolver.
