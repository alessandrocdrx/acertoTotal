# CelsoPix: aprendizados

Este arquivo é a memória do projeto. Aqui ficam registrados os erros cometidos, a causa de cada um, a correção e a regra que impede que ele volte.

## Como usar

1. **Antes de mexer no app:** leia as "Regras em vigor".
2. **Quando aparecer um problema** (teste falhando, reclamação do cliente, algo confuso na tela), registre uma entrada nova no topo do "Registro", com:
   - o que aconteceu, nas palavras de quem viu;
   - a causa de verdade, não o sintoma;
   - o que foi feito para corrigir;
   - como evitar: a regra nova e o teste que protege.
3. **Toda correção ganha um teste automatizado** em `testes/`. Sem teste, o erro volta.
4. **Se o mesmo tipo de erro aparecer duas vezes**, ele vira uma regra em "Regras em vigor".
5. **Antes de publicar**, rode `bash testes/rodar.sh`. Só publique com todas as suítes passando.

## Regras em vigor

Cada regra tem um teste que a verifica. A suíte aparece entre colchetes.

### Telas e fluxo

- R1. Cada tela tem uma tarefa só. Duas telas que mostram a mesma coisa confundem. Exemplo: o Início repetia a lista de Contas. [usabilidade]
- R2. A ação principal fica visível na lista. Exemplo: "Dar baixa" aparece em cada conta em aberto, e não escondido num menu. [app]
- R3. A situação vem sempre escrita, e a cor só reforça. Exemplo: "Atrasada há 5 dias", e não apenas uma bolinha vermelha. [app]
- R4. No máximo 4 abas e 4 filtros, numa linha só (Lei de Hick). [usabilidade]
- R5. Desfazer é melhor que perguntar "tem certeza?". Baixa, desfazer baixa e exclusão mostram um aviso com o botão "Desfazer" por 6 segundos. [usabilidade]
- R6. Tela vazia sempre oferece uma saída: "Limpar busca", "Ver todas" ou "Toque em + Nova conta". [usabilidade]
- R7. Mostrar primeiro o que importa para a decisão. Exemplo: a lista de clientes mostra quanto cada um deve, ou "Em dia". [usabilidade]

### Formulários

- R8. O erro aparece embaixo do campo, em vermelho, explicando como corrigir. Não use aviso flutuante longe do campo. O campo fica marcado (`aria-invalid`) e o erro some quando a pessoa corrige. [usabilidade, app]
- R9. O exemplo fica fora do campo, numa dica abaixo do rótulo. Um texto cinza dentro do campo parece valor já preenchido. [usabilidade]
- R10. Impedir o erro antes de avisar. Exemplos: a data de pagamento não aceita dia futuro, a nova conta começa sem cliente escolhido e não se cadastra duas vezes o mesmo nome. [usabilidade]
- R11. Todo campo tem um rótulo visível. [usabilidade]

### Toque, leitura e acessibilidade

- R12. Toda área de toque tem pelo menos 44 px, e o botão principal tem pelo menos 48 px, largo e embaixo, perto do polegar (Lei de Fitts). [app, usabilidade]
- R13. Contraste mínimo de 4,5:1 nos dois temas. Nunca usar `opacity` em texto, porque ela derruba o contraste sem que se perceba. [app (axe), tema]
- R14. Janelas têm nome para o leitor de tela, recebem o foco ao abrir, fecham com Esc e devolvem o foco ao botão de origem. [app]
- R15. A página declara o idioma `pt-BR`. [usabilidade]
- R16. Não pode haver rolagem horizontal de 320 a 1280 px. [app]

### Marca e apresentação

- R17. O nome "CelsoPix" aparece dentro do app, em todas as telas, e no título do artefato. [usabilidade]
- R18. O tema claro é o padrão. O escuro só entra quando a pessoa escolhe. [tema]
- R19. Os dados de exemplo precisam preencher o período padrão de cada tela. Uma estatística com "R$ 0" na primeira tela mata a demonstração. [estatisticas]

### Código e processo

- R20. Ao acrescentar um parâmetro a uma função usada em `.map(funcao)`, lembre que o `map` passa o índice como segundo argumento. Chame com `map(function(x){return funcao(x)})` ou cheque o tipo (`semNome===true`).
- R21. Só um aviso flutuante na tela por vez. Avisos empilhados escondem a mensagem nova e quebram os testes.
- R22. Antes de publicar, rode os testes contra o arquivo local. Publique só com as suítes passando.
- R23. Em parâmetros de texto simples (como a descrição do artefato), escreva os acentos diretamente. Escapes como `ç` só funcionam dentro de JSON.
- R24. Teste de acessibilidade (axe) espera a animação terminar (450 ms) antes de medir contraste. Medir durante a animação dá falso alarme.

### App Android (APK)

- R25. O protótipo é a única fonte da interface. O APK é montado a partir dele por `android/preparar-assets.sh`. Nunca edite `android/app/src/main/assets/` à mão. [android]
- R26. O APK funciona sem internet: a página não pode ter nenhum endereço externo, e o leitor de Excel vai dentro do APK. [android]
- R27. Foto de comprovante é reduzida para JPEG de no máximo 1280 px antes de guardar. O armazenamento do app tem cerca de 5 MB, e uma única foto de câmera pode lotá-lo. [android]
- R28. Falha ao salvar nunca é silenciosa: com a memória cheia, o app avisa e diz o que fazer. [android]
- R29. Um botão que depende de uma tarefa em andamento, como preparar um anexo, fica desativado até a tarefa terminar. Um botão desativado não usa `opacity`: troca de cor por tokens, conforme a regra R13. [android]
- R30. A mesma chave assina todas as versões, para que o APK novo instale por cima do antigo sem apagar os dados. A chave e a senha nunca vão para o repositório: ficam em `android/celsopix.keystore` e `android/assinatura.properties`, os dois no .gitignore.
- R31. Depois de compilar, confira se a compilação passou ("BUILD SUCCESSFUL" ou código de saída 0), e não só se o APK existe. Apague a saída antiga antes de compilar. Um APK velho na pasta parece sucesso. Ao mover ou renomear recursos (`res/`), compile com `clean`: o cache incremental acusa "resource not found" mesmo com o arquivo no lugar.

## Registro

As entradas mais recentes ficam no topo.

### A-013 · 31/10/2026 · Primeiro APK (casca Android do protótipo)
- **O que aconteceu:** O cliente pediu um APK para baixar. Ao preparar o protótipo para rodar no celular, encontrei dois riscos que no navegador passavam despercebidos:
  - uma foto de comprovante, que tem de 3 a 5 MB, pode lotar o armazenamento do app, que é de cerca de 5 MB;
  - quando o armazenamento lota, a função de salvar falhava calada, e os dados se perdiam ao fechar o app.
- **Causa:** O protótipo guardava a foto original em base64 no `localStorage` e engolia o erro de cota.
- **Correção:**
  - as fotos são reduzidas para JPEG de até 1280 px;
  - "Confirmar baixa" fica desativado enquanto a foto é preparada;
  - um aviso aparece quando a memória enche;
  - o APK é uma casca Kotlin com WebView que carrega o protótipo das próprias assets, funciona sem internet e não pede a permissão de internet;
  - a ponte `CelsoPixAndroid.salvarArquivo` salva o Excel na pasta Downloads;
  - o seletor de arquivos aceita fotos, PDF e Excel;
  - o botão Voltar do celular fecha a janela aberta antes de sair do app.
- **Problemas na compilação:**
  - a conexão com o dl.google.com caiu no meio do download; resolvido retomando com `curl -C -`;
  - o Maven Central respondeu 429 (limite de pedidos); resolvido com `--max-workers=1` e novas tentativas.
- **Decisões:**
  - A primeira ideia foi guardar a chave do protótipo no repositório, para as atualizações instalarem por cima. A trava de segurança bloqueou o commit por vazamento de credencial, e com razão: chave e senha são segredos. Agora as duas ficam fora do repositório (R30), e o Gradle lê os dados de `android/assinatura.properties`. A chave precisa ser guardada pelo responsável. Sem ela, a próxima versão só instala depois de desinstalar a anterior, o que apaga os dados.
  - O minSdk é 29 (Android 10 ou superior), o que dispensa a permissão de armazenamento para salvar em Downloads.
- **Limites conhecidos:**
  - O ambiente de compilação não tem virtualização, então a casca nativa não foi executada num emulador. O que foi testado:
    - a página do APK, no Chromium sem rede, com a ponte simulada;
    - o lint do Android: 0 erros;
    - a assinatura (apksigner);
    - o manifesto (aapt2).
  - Seletor de arquivos, Downloads e botão Voltar precisam de um teste num celular real.
- **Erro meu na compilação:** Usei `java.util.Properties` dentro do bloco `android {}` do Gradle, onde `java` é outra coisa, e a compilação falhou. Por alguns minutos achei que tinha passado, porque o APK antigo continuava na pasta. Corrigi com `import java.util.Properties`. Depois, ao mover a pasta de ícones para atender o lint, o `git mv -k` não fez nada (o arquivo ainda não estava no git) e o cache do Gradle acusou recurso inexistente. Resolvido com `git mv` de verdade e `clean`.
- **Como evitar:** Regras R25 a R31. Foi criada a suíte `testes/android.test.js`, com 12 verificações.

### A-012 · 31/10/2026 · Revisão de usabilidade tela por tela
- **O que aconteceu:** Revisei as telas com as 10 heurísticas de Nielsen e as leis de Fitts e Hick. Encontrei:
  - os erros apareciam num aviso flutuante longe do campo;
  - os exemplos dentro dos campos ("150,00") pareciam valores já preenchidos;
  - não havia como desfazer uma baixa ou excluir uma conta;
  - a busca sem resultado não oferecia saída;
  - não dava para editar os dados de um cliente;
  - a ficha do cliente repetia o nome dele em cada conta;
  - a nova conta já vinha com um cliente escolhido, o que facilitava lançar a conta no cliente errado;
  - a data de pagamento aceitava dias futuros.
- **Causa:** O app foi crescendo por pedidos pontuais, sem uma revisão de conjunto.
- **Correção:**
  - erros embaixo do campo;
  - dicas fora do campo e "R$" fixo no valor;
  - "Desfazer" no aviso da baixa e da exclusão;
  - "Limpar busca" e "Ver todas" nas telas vazias;
  - "Editar dados" na ficha do cliente;
  - na ficha do cliente, as contas mostram a descrição em vez do nome;
  - a nova conta começa com "Escolha o cliente", e a lista vem em ordem alfabética;
  - a data de pagamento só vai até hoje;
  - a lista de clientes mostra o valor em aberto de cada um;
  - busca de clientes quando há mais de 5;
  - idioma pt-BR declarado.
- **Como evitar:** Regras R5 a R11, R15 e R20. Foi criada a suíte `testes/usabilidade.test.js`, com 37 verificações. Rodada contra a versão anterior do app, ela falha, o que confirma que detecta esses problemas.

### A-011 · 17/10/2026 · Compartilhamento sem nome nem marca
- **O que aconteceu:** Ao compartilhar pelo WhatsApp, o texto não estava bom, não havia ícone, e dentro do app não aparecia o nome CelsoPix.
- **Causa:** O título era "Celso Pix", com espaço. A descrição tinha sido escrita com escapes (`cobranças`) que apareceram literalmente. Faltava uma marca no cabeçalho.
- **Correção:** Título "CelsoPix", descrição reescrita com acentos, ícone de dinheiro, e logotipo com o nome no topo de todas as telas.
- **Como evitar:** Regras R17 e R23.

### A-010 · 17/10/2026 · Início e Contas confundiam
- **O que aconteceu:** O cliente disse que o Início parecia uma versão simplificada de Contas, sem filtros.
- **Causa:** Duas telas mostravam a mesma lista, com recortes diferentes.
- **Correção:** O Início saiu. Contas virou a tela inicial, com o resumo no topo, a busca e os filtros. O botão do cabeçalho foi encurtado para "Exportar" e os filtros cabem numa linha.
- **Como evitar:** Regras R1 e R4.

### A-009 · 17/10/2026 · Estatísticas com R$ 0 na primeira tela
- **O que aconteceu:** Na aba Números, o período "Este mês" mostrava "Recebido R$ 0" e taxa de 0% com os dados de exemplo.
- **Causa:** O histórico de exemplo terminava no mês anterior.
- **Correção:** Foram incluídas baixas no mês atual nos dados de exemplo.
- **Como evitar:** Regra R19. O teste de estatísticas recalcula tudo de forma independente nos três períodos.

### A-008 · 17/10/2026 · "As cores não foram incluídas"
- **O que aconteceu:** O cliente achou que a paleta não tinha sido aplicada.
- **Causa:** O celular dele estava no modo escuro, que é quase todo cinza. Além disso, a paleta nunca tinha sido mostrada nem aprovada.
- **Correção:** Foi criada a janela "Tema e paleta", com as amostras e o contraste de cada cor, nos temas claro e escuro. Depois, o tema claro passou a ser o padrão.
- **Como evitar:** Regra R18. Uma decisão visual precisa ser mostrada ao cliente antes de ser dada como feita.

### A-007 · 17/10/2026 · Verde suave abaixo do contraste mínimo
- **O que aconteceu:** O teste da paleta mostrou contraste de 4,2:1 no verde sobre o verde suave.
- **Correção:** O fundo foi clareado, e o contraste passou para 4,6:1. O texto verde de "Paga" foi escurecido, e o contraste passou para 5,2:1.
- **Como evitar:** Regra R13.

### A-006 · 17/10/2026 · Acessibilidade das janelas e dos toques
- **O que aconteceu:** O axe e os testes de toque acharam:
  - contraste de 4,11 no cartão verde, causado por `opacity: .85`;
  - janelas sem nome;
  - títulos fora de ordem;
  - botões com 38 px;
  - link "Fazer agora" com 16 px;
  - janelas sem foco e sem fechar com Esc.
- **Correção:** Todos os itens foram corrigidos, e passou a valer o mínimo de 44 px.
- **Como evitar:** Regras R12, R13, R14 e R24. O falso alarme do axe durante a animação foi resolvido com uma espera.

### A-005 · 17/10/2026 · Não ficava claro onde dar baixa
- **O que aconteceu:** O cliente não sabia onde dar baixa nem o que estava em aberto.
- **Causa:** A baixa ficava dentro da ficha da conta, e o rótulo "A vencer" não dizia que a conta estava em aberto.
- **Correção:** Cada conta ganhou o botão "Dar baixa", a situação passou a vir escrita ("Atrasada há N dias", "Em aberto · vence em N dias") e os filtros ganharam contagem.
- **Como evitar:** Regras R2 e R3.

### A-004 · 17/10/2026 · Exportar Excel não baixava arquivo
- **O que aconteceu:** O ambiente do artefato bloqueia downloads iniciados pela página.
- **Correção:** Passei a usar a permissão de download do artefato, que gera um .xlsx real. Copiar e colar no Excel ficou como alternativa. Sem a permissão, o app mostra um aviso e não quebra.
- **Como evitar:** Isso está coberto no teste "Sem permissão de download".

## Técnicas de usabilidade aplicadas por tela

| Tela | Técnicas |
|---|---|
| **Contas (inicial)** | Resumo antes do detalhe. Botão "Dar baixa" em cada conta (R2). Situação escrita mais faixa colorida (R3). 4 filtros com contagem, numa linha (Hick). Busca. Tela vazia com saída (R6). Botão flutuante "+ Nova conta" no alcance do polegar (Fitts). |
| **Ficha da conta e baixa** | Janela de baixo, padrão do Android (Jakob). Data de hoje já preenchida e sem datas futuras (R10). Comprovante opcional, sem conferência, revelado só quando preciso. "Confirmar baixa" largo, embaixo. "Excluir" discreto, abaixo de "Fechar". Desfazer depois de salvar (R5). |
| **Nova conta** | Começa sem cliente escolhido (R10). Clientes em ordem alfabética. Dicas fora do campo, "R$" fixo (R9). Erro no campo, com exemplo (R8). |
| **Clientes** | Valor em aberto ou "Em dia" à direita (R7). Selo de atrasadas. Busca quando há mais de 5 clientes. |
| **Ficha do cliente** | Contas pela descrição, sem repetir o nome. "Nova conta para este cliente" já com o cliente preenchido. "Editar dados". |
| **Números** | Período em 3 opções. Cada gráfico tem o valor escrito ao lado. Frase simples explicando a taxa. Tocar no devedor abre o cliente. Mensagens sem divisão por zero quando não há dados. |
| **Planilha** | Colunas esperadas explicadas, planilha de exemplo, prévia antes de exportar, aviso de backup atrasado. |
| **Tema e paleta** | Claro como padrão (R18). Amostras com contraste numérico. |

## Pendências conhecidas

- A miniatura do link no WhatsApp é gerada pelo Claude e não pode ser trocada pelo app.
- Os testes rodam no navegador (Chromium). A casca Android ainda não foi testada num aparelho: faltam o seletor de arquivos, o salvamento em Downloads e o botão Voltar.
- Lint: três bibliotecas AndroidX têm versões mais novas (activity, core, webkit). Atualizar exige compileSdk 36 e uma versão nova do AGP.
- O app guarda os dados no armazenamento da WebView. Se o app for desinstalado, os dados se perdem. A cópia de segurança em Excel é a proteção.
- O APK abre com os dados de exemplo. Falta um botão "Começar do zero" para uso real.
- Os dados de exemplo dos meses anteriores têm valores parecidos, então as barras do gráfico mensal saem quase iguais.
