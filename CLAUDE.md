# acertoTotal

App de contas a receber (antes chamado CelsoPix): cadastrar clientes e contas, dar baixa com o comprovante, ver quem está em atraso, importar e exportar Excel. O protótipo navegável fica em `prototipo/acertototal.html` e é publicado como artefato. O app final será Android nativo.

## Nome e padrão da família

Mesmo padrão do geoTotal e do calcTotal: nome `palavraTotal` em camelCase, repositório com o mesmo nome (`acertoTotal`), ID do app `io.github.alessandrocdrx.acertototal`, `PRIVACIDADE.md` na raiz e ficha da loja em `playstore/FICHA.md`. O nome não pode citar um meio de pagamento (Pix): o app vai crescer além de cobranças. O app de teste se chama `acertoTotal Teste`.

## Antes de qualquer mudança

1. Leia `docs/aprendizados.md`, principalmente a seção "Regras em vigor".
2. Toda mudança segue as regras. Se uma regra atrapalhar, discuta antes de quebrá-la.

## Comandos

- Testes: `bash testes/rodar.sh` roda as suítes app, estatisticas, tema, usabilidade, exportacao, importacao, tarefas, avaliacao, assinatura, experiencia e android com Playwright e Chromium. A suíte `experiencia` percorre todas as telas e janelas (360×780 e 320×640, claro e escuro) e guarda as fotos em `testes/capturas/experiencia/`.
- As capturas de tela ficam em `testes/capturas/`. Elas não vão para o repositório.
- APK de teste: `bash android/preparar-assets.sh && cd android && ./gradlew clean assembleHomologRelease --max-workers=1`. O de produção usa `assembleProducaoRelease`. O SDK fica em `/opt/android-sdk` (`android/local.properties`). O arquivo sai em `android/app/build/outputs/apk/<variante>/release/`.
- A interface do APK vem do protótipo. Nunca edite `android/app/src/*/assets/` à mão.

## Branches, CI e lançamento

- **Fluxo de aprovação (decisão do Alessandro):** 1) fazer a mudança e rodar as 11 suítes; 2) publicar o artefato (`prototipo/acertototal.html`, mesma URL) para o Alessandro olhar; 3) enquanto ele não aprova, o trabalho fica **só na branch `aprovacao`** (commit e push nela, não na `main`); 4) quando ele aprova, a `aprovacao` entra na `main`. O artefato é sempre igual à última versão do protótipo, no modo de teste (é só para visualizar). Mudanças que o artefato não mostra (Android, textos da loja, processo) esperam a aprovação do mesmo jeito, e o Alessandro olha o pacote ou o texto.
- A `main` é a versão aprovada.
- `.github/workflows/android.yml` roda as 11 suítes e confere que o APK de teste compila, a cada push. **Nenhum APK é publicado**: o repositório é público e o APK de teste tem a assinatura simulada (libera o app de graça). Quem testa instala pela Play Store.
- Lançar na Play Store (opção pelo GitHub; o jeito usado até agora é gerar o AAB localmente e enviar ao Alessandro): aumentar `versionCode`/`versionName` em `android/app/build.gradle.kts` e pôr `[release]` na mensagem do commit na `main`. O workflow gera o AAB assinado com a chave de envio e o guarda como artefato do workflow (14 dias), a partir dos Secrets `ACERTOTOTAL_KEYSTORE_BASE64`, `ACERTOTOTAL_KEYSTORE_PASSWORD` e `ACERTOTOTAL_KEY_ALIAS`. Sem os Secrets, ele para com erro. Passo a passo em `playstore/FICHA.md`.

## Regras de trabalho

- Com o Alessandro, guie **um passo por vez**: dê só o próximo passo, espere ele responder (muitas vezes com uma captura de tela) e só então dê o seguinte. Mensagens curtas, sem listas longas.

- Toda correção ganha um teste em `testes/`.
- Todo problema achado, seja por teste, pelo cliente ou numa revisão, vira uma entrada em `docs/aprendizados.md` com causa, correção e como evitar.
- Publique o artefato só com todas as suítes passando.
- **O artefato acompanha o protótipo, sempre.** Toda mudança em `prototipo/acertototal.html` é publicada no artefato (https://claude.ai/artifact/AmxvF5aaQtretFKakGHWLA, mesma URL) assim que as suítes passam, **antes** do commit na `main` e sem esperar pedido. Antes de dizer que terminou, confira que a versão publicada é a do protótipo atual.
- Os textos da interface ficam em português do Brasil simples: uma ação por botão, erro que explica como resolver.
