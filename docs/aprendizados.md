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
- R30. Fase de teste: por decisão do cliente (31/10/2026), a chave pode mudar entre versões, e nesse caso a versão nova pede para desinstalar a anterior. Desinstalar apaga os dados, então exporte a cópia de segurança antes. A chave e a senha nunca vão para o repositório: ficam em `android/celsopix.keystore` e `android/assinatura.properties`, os dois no .gitignore.
- R31. Depois de compilar, confira se a compilação passou ("BUILD SUCCESSFUL" ou código de saída 0), e não só se o APK existe. Apague a saída antiga antes de compilar. Um APK velho na pasta parece sucesso. Ao mover ou renomear recursos (`res/`), compile com `clean`: o cache incremental acusa "resource not found" mesmo com o arquivo no lugar.
- R32. Todo ambiente que não é produção mostra a faixa "Ambiente de teste" no topo, e o app de teste tem outro nome e outro pacote ("CelsoPix Teste", `br.com.celsopix.teste`). Assim nunca se confunde teste com o oficial. [usabilidade, android]
- R33. Variantes Android (productFlavors) não podem ter nome começando com "test". Usamos `homolog` para o ambiente de teste.
- R34. Exportar sempre diz o que vai sair antes de sair: a contagem e o total ao vivo, os filtros escritos na prévia e no nome do arquivo. [exportacao]
- R35. Quando um teste falhar, descubra primeiro se o erro é do app ou do teste. Exemplo: o `selectOption` do Playwright não foca o campo, então "perdeu o foco" era falso alarme. Corrija o lado errado e registre.
- R36. Script que substitui uma função inteira precisa terminar na chave de fechamento dela, e não no início da próxima função citada. Senão apaga tudo o que fica entre as duas. O `assert` antes de gravar salvou o arquivo.
- R37. Importar acontece em duas etapas. A prévia mostra o que entra, o que muda (valor antigo riscado e valor novo em destaque) e as linhas com problema (número da linha e motivo). Nada é gravado até a pessoa confirmar. Depois de confirmar, aparece o "Desfazer", e a importação fica registrada no histórico. [importacao]
- R38. Importar de novo não duplica nada. A mesma conta é o mesmo cliente com a mesma descrição e o mesmo vencimento, sem diferença de maiúsculas, acentos ou espaços. Reimportar a própria cópia de segurança dá zero mudanças. Um campo vazio na planilha não apaga um dado que já existe. [importacao]
- R39. Data só é aceita se existir de verdade. "32/13/2026" e "30/02/2026" são recusadas, não convertidas. [importacao]
- R40. Testes que comparam texto de títulos usam comparação sem maiúsculas: `text-transform: uppercase` muda o que o `innerText` devolve.
- R41. Período de avaliação: 7 dias contados do primeiro uso no aparelho, guardados à parte dos dados (restaurar ou importar não reinicia a contagem). A faixa do topo mostra quantos dias faltam e até quando. Depois do prazo, o app fica só para consulta: ver, buscar, filtrar, ver números e trocar o tema continuam. Cadastrar, editar, dar baixa, excluir, importar e exportar ficam bloqueados, tanto na tela (os botões somem e uma mensagem explica) quanto no código (toda ação de escrita passa por uma trava). Nenhum dado é apagado. [avaliacao]
- R42. Quando uma função fica bloqueada, nenhum aviso pode mandar a pessoa usar essa função. Exemplo: o aviso "Fazer cópia de segurança" some no modo só consulta. [avaliacao]
- R43. Limite controlado só no aparelho pode ser burlado limpando os dados do app. O app detecta o relógio atrasado mais de 1 dia, mas um bloqueio que ninguém contorne exige servidor de licenças. Não prometa mais que isso.
- R44. Toda mensagem de erro de importação diz quatro coisas: a linha, a coluna, a célula (ex.: "Valor (E6)") e como corrigir, com um exemplo. "Arquivo inválido" ou "erro na linha 6" sozinhos não servem. Uma linha com mais de um problema mostra todos de uma vez. [importacao]
- R45. Uma tela, uma tarefa. Importar, exportar e fazer cópia de segurança são cartões separados na aba Planilha, cada um com um botão que abre o seu fluxo. Filtros e formulários não ficam soltos na tela principal. Só a tarefa principal tem botão de destaque. [importacao, exportacao]
- R46. Fluxo de várias etapas mostra as etapas ("1 Arquivo · 2 Colunas · 3 Conferir") e a atual. O botão principal fica fixo embaixo da janela, visível mesmo com a lista rolada. [importacao]
- R47. Importar aceita a planilha como ela vem: títulos com outros nomes ("Cliente", "Data de vencimento", "Valor (R$)"), títulos fora da linha 1, várias abas, CSV do Excel brasileiro (";" e acentos em Windows-1252), valores "R$ 1.234,56" e "1,234.56", datas dd/mm/aa e dd-mm-aaaa. O que o app não reconhece, a pessoa escolhe no passo "Colunas", vendo a letra, o título e um exemplo de cada coluna. [importacao]
- R48. O tipo do arquivo vem do conteúdo, não da extensão. No Android, planilhas do WhatsApp e do Drive chegam sem extensão ou como "application/octet-stream". O seletor do APK aceita esses tipos, e o app confere os primeiros bytes para explicar quando o arquivo é uma imagem, um PDF ou outra coisa. [importacao]
- R49. CSV é lido como texto puro (`raw: true`). Se o leitor converter datas sozinho, "10/11/2026" vira 11 de outubro, no formato americano. [importacao]
- R50. Desfazer uma importação funciona a qualquer momento, pelo histórico, mesmo depois de fechar o app. O app guarda o que entrou e o valor de antes de cada dado mudado. O que a pessoa usou depois (uma baixa, um comprovante, uma conta nova no cliente importado) fica, e o aviso diz quantos itens ficaram. [importacao]
- R51. Linhas com problema podem ser baixadas numa planilha com as colunas originais e mais "Linha no arquivo" e "Problema". A pessoa corrige ali e importa de novo. O que já entrou não se repete, e as duas colunas extras são ignoradas. [importacao]
- R52. O cliente é identificado pelo CPF ou CNPJ. O app confere o dígito verificador e guarda só os números. O CPF/CNPJ é obrigatório no cadastro e não pode repetir, mas dois clientes podem ter o mesmo nome. Na planilha, o CPF/CNPJ é o que liga cada linha ao cliente, com ou sem pontuação. Se o Excel tirou o zero da frente, o app completa e avisa. [importacao, tarefas, usabilidade]
- R53. Nas planilhas, a conta é identificada pelo "Código da conta".
  - **Linha com código:** atualiza aquela conta.
  - **Linha sem código:** o par CPF/CNPJ + descrição + vencimento evita duplicar.
  - **Código que não existe no app, com CPF/CNPJ válido:** cria a conta com o mesmo código. É o que faz a cópia restaurar tudo num celular novo.
  - **Código que é de outro CPF:** a linha é barrada, com a explicação de quem é o dono.
  - [importacao, tarefas]
- R54. Exportar tem dois formatos, com nomes pelo uso:
  - **"Relatório":** para ler ou mandar a alguém. Tem a situação e os dias de atraso.
  - **"Para editar":** volta pela importação. Tem o código, o CPF/CNPJ e a aba "Como editar".
  - **Cópia de segurança:** é a planilha para editar com tudo, inclusive clientes sem contas. [exportacao, importacao]
- R55. A prévia da importação agrupa por cliente: o cliente e, embaixo dele, as contas que entram e as que mudam, com o antes e o depois. O botão diz o que vai gravar, como "Importar 1 cliente e 8 contas" ou "Importar 5 atualizações". Há duas escolhas independentes: cadastrar o que é novo e atualizar o que mudou. [importacao, tarefas]
- R56. Telas de escolha são organizadas pelo objetivo da pessoa ("O que você quer fazer?"): Importar planilha, Atualizar pelo Excel, Exportar relatório e Cópia de segurança. As opções cabem na tela sem rolar. Os filtros abrem limpos e não herdam escolhas de outra tarefa. [importacao, tarefas]
- R57. Toda pergunta do cliente do tipo "como faço X?" vira uma tarefa na suíte `tarefas`. A tarefa tem uma meta de toques, confere que o caminho aparece sem rolar e confere o resultado gravado. [tarefas]
- R58. Depois de mudar o layout, olhe as capturas de tela. Exemplo: grid num `<li>` com negrito e texto solto quebra uma palavra por linha. Envolva o conteúdo num `<span>`. O teste mede a largura dos textos da janela. [importacao]
- R59. Nenhum cliente fica sem CPF/CNPJ. Cadastro, edição e planilha exigem o número. Cliente antigo sem ele aparece num aviso no topo de Clientes com "Completar agora", que abre um por um, e cliente sem contas pode ser excluído ali mesmo. Enquanto não for completado, não recebe conta nova (fica desativado na lista) e a planilha recusa a linha dele sem CPF/CNPJ, explicando que preencher o número completa o cadastro. [tarefas]
- R60. Ação que apaga tudo pede uma confirmação digitada, como o GitHub pede o nome do repositório: a pessoa digita o próprio CPF/CNPJ, cadastrado na primeira vez, e o botão só ativa quando confere. Antes, a tela diz quantos clientes, contas, comprovantes e importações serão apagados e oferece a cópia de segurança. Só existe na versão de teste, fica separado das tarefas comuns e não reinicia a avaliação. [tarefas]
- R61. Na versão de teste, CPF/CNPJ com dígito errado é aceito (desde que tenha 11 ou 14 números), sempre com aviso: no formulário enquanto digita, no aviso ao salvar, na linha da planilha e com o rótulo "inválido (teste)" na lista. Repetido continua barrado. A produção só aceita número válido. [tarefas, importacao, app]
- R62. Um aviso que aparece ou some embaixo de um campo não pode mudar no `blur`. O `blur` acontece no mesmo toque do botão Salvar: a janela muda de altura, o botão sai do lugar e o toque se perde. Atualize o aviso enquanto a pessoa digita. [app, tarefas]
- R63. Teste de layout não usa limite fixo em pixels que dependa da fonte ("y > 600"). Cada máquina tem fontes diferentes, e o texto muda de altura: o que passa no computador falha no GitHub. A régua é a própria janela ou outro elemento da tela (por exemplo, o fundo da barra de botões perto do fundo da janela). [importacao]
- R64. Assinatura: um produto (`acertototal_premium`) com 4 planos base (`semanal`, `mensal`, `semestral`, `anual`). Os preços saem da Play, e o app só calcula o equivalente por mês e a economia (semestral 5%, anual 10%). O ID do produto e dos planos é o mesmo no Console, na página e na casca Android. [assinatura]
- R65. Só o app da Play cobra de verdade (`COBRANCA_REAL`). O protótipo e o app de teste simulam a assinatura, avisam isso na janela e nunca cobram. A última resposta da Play fica guardada no aparelho, para quem já assinou abrir o app liberado sem internet. Sem servidor, essa checagem só vale no aparelho, como o teste grátis (R43). [assinatura]
- R66. Texto de privacidade e ficha da loja prometem só o que o pacote final confere. O Play Billing traz as permissões de internet e de cobrança sozinho: depois de acrescentar uma biblioteca, leia as permissões do pacote montado (`aapt2 dump permissions`) antes de escrever "sem internet". [assinatura]
- R67. Teste grátis: 7 dias no app de teste e, na versão final da Play, 7 dias; na fase de aprovação, 32 dias para a revisão e os 14 dias de teste fechado não travarem. O prazo vem de `android/preparar-assets.sh` (`producao:producao:32`). Depois do prazo, sem assinatura, o app fica só para consulta (R41). [avaliacao, assinatura]

## Registro

As entradas mais recentes ficam no topo.

### A-025 · 01/11/2026 · Assinatura paga pela Google Play
- **Pedido do cliente:** cobrar assinatura: plano semanal, mensal, semestral e anual; mensal R$ 9,99, semestral 5% e anual 10% mais baratos, semanal mais caro por dia; teste grátis de 7 dias, e 32 dias na fase de aprovação da Play.
- **Decisões:** semanal R$ 2,99 (30% mais caro por mês que o mensal), semestral R$ 56,90 (R$ 9,48 por mês), anual R$ 107,90 (R$ 8,99 por mês). O teste grátis é do app, e não uma oferta da Play, para a revisão do Google e os 12 testadores não esbarrarem numa tela de pagamento.
- **O que foi feito:**
  - Casca Android: `Assinatura.kt` (Play Billing 8: conectar, buscar preços, conferir compras, abrir a compra, confirmar a compra) e novos métodos da ponte (`assinaturaEstado`, `assinar`, `atualizarAssinatura`, `gerenciarAssinatura`). `COBRANCA_REAL` liga a cobrança só na produção.
  - Página: janela "Assinar" com os 4 planos, cartão "Assinatura" na aba Planilha (as 4 tarefas continuam cabendo sem rolar), faixa com "Assinar" quando o teste termina (e nos últimos 3 dias no app oficial). Assinante libera o app; sem assinatura, depois do teste, só consulta.
  - Textos: faixa e avisos passaram de "Avaliação" para "Teste grátis".
  - Suíte nova `assinatura` (44 verificações), com uma ponte falsa no lugar da casca Android.
- **Problemas achados:**
  - O Play Billing traz um Fragment antigo e o lint de release barrou o pacote ("Upgrade Fragment version"). Corrigido fixando `androidx.fragment` 1.8.5.
  - O pacote ganhou as permissões `INTERNET`, `ACCESS_NETWORK_STATE` e `com.android.vending.BILLING`, que vêm da biblioteca. A política e a ficha diziam "sem permissão de internet". Os textos foram corrigidos (R66).
- **Limites:** a compra de verdade só dá para testar com o app na Play (teste interno) e uma conta de testador de licença; aqui foi testada com uma ponte falsa. A checagem da assinatura é só no aparelho (R65).

### A-024 · 01/11/2026 · Primeira execução do GitHub Actions falhou num teste
- **O que aconteceu:** as 9 suítes passavam aqui, mas na primeira execução do workflow no GitHub falhou 1 verificação de 400: "Botão de importar fica fixo embaixo, mesmo com a lista rolada", com o botão em `y=598,6` e o teste exigindo `y > 600`.
- **Causa:** o teste usava um número fixo de pixels como régua. O servidor do GitHub tem outras fontes, o texto do botão fica um pouco mais alto e o botão sobe 8 px. O app não tinha defeito.
- **O que foi feito:** o teste agora mede a barra de botões em relação ao fundo da janela (fica colada embaixo, com a lista rolada).
- **Como evitar:** regra R63. Rodar os testes no GitHub, e não só no computador, é o que mostra esse tipo de diferença.

### A-023 · 01/11/2026 · Link da política quebrado: o repositório não tinha `main`
- **O que aconteceu:** depois de renomear o repositório, o link `.../blob/main/PRIVACIDADE.md` não abria. O ambiente bloqueia as páginas do github.com e não deu para abrir o link daqui, mas `git ls-remote` mostrou o motivo: só existia a branch da sessão, sem `main`.
- **Causa:** o trabalho foi todo feito na branch da sessão, sem pull request, e a ficha da loja apontou para uma branch que nunca existiu. Um link escrito antes de existir não foi conferido.
- **O que foi feito:** a `main` foi criada a partir da branch aprovada, e o repositório ganhou o padrão do calcTotal: `LICENSE`, workflow `android.yml` (testes, APK de teste na Release e lançamento na Play com `[release]`) e as regras de branch no `CLAUDE.md`.
- **Como evitar:** todo link que vai para um formulário externo (política, ficha) é conferido com `git ls-remote` ou abrindo numa aba anônima antes de ser entregue. O workflow não pôde ser executado daqui; o primeiro push na `main` é o teste dele.

### A-022 · 01/11/2026 · Mudança de nome: CelsoPix vira acertoTotal
- **Pedido do cliente:** o nome do app e do repositório devem seguir o padrão do geoTotal e do calcTotal, e não podem citar Pix, porque o app vai evoluir. Depois de descartar "caixa" (marca de banco) e "conta" (palavra genérica, some na busca entre os bancos), o cliente escolheu **acertoTotal**.
- **O que mudou:**
  - Nome do app (`acertoTotal`, e `acertoTotal Teste` na versão de teste), ID `io.github.alessandrocdrx.acertototal`, pacote Kotlin, `prototipo/acertototal.html` e nomes dos arquivos de exportação.
  - O texto do app não fala mais de Pix: "dê baixa quando o pagamento chegar", "Comprovante" no lugar de "Comprovante Pix" e a paleta "Verde acerto".
  - A política passou a `PRIVACIDADE.md` e a ficha da loja a `playstore/FICHA.md`, como no calcTotal.
- **O que não mudou, de propósito:** a chave de dados `baixa-pix-v2` (o nome é invisível e mudar exigiria migração), o arquivo da chave de envio `celsopix-envio.keystore` e as capturas antigas de documentação no histórico deste arquivo, que mantêm o nome da época.
- **Como evitar:** o ID do app na Play não muda depois do primeiro envio, então o nome se decide antes de criar o app no Console. Nome de produto não leva marca de banco, termo genérico nem meio de pagamento. Pesquisar o nome na Play e no INPI (classes 9 e 36) antes de usar.

### A-021 · 01/11/2026 · Primeiro pacote para a Google Play
- **Pedido do cliente:** publicar na Play pela conta pessoal alessandrocdrx. O passo a passo está em `docs/play-store.md`.
- **O que foi feito:**
  - A variante `producao` passou a ser assinada pela chave de envio (`celsopix-envio.keystore`, `assinatura-play.properties`), separada da chave dos APKs de teste. As duas variantes agora escolhem a assinatura no próprio `productFlavors`, e o `release` não força mais a chave de debug.
  - `.gitignore` cobre `android/assinatura*.properties`, além de `*.keystore` e `*.jks`.
  - Gerados o AAB (versão 0.8.0, código 9), o ícone 512, o gráfico de destaque e 5 capturas de tela em `entregas/play/`.
- **Como evitar:** a Play só aceita AAB e guarda a primeira chave para sempre. Nunca assine o pacote da Play com a chave de teste, e cada envio precisa de `versionCode` maior. Lição de gradle: um bloco que lê `signingConfigs.findByName` precisa vir depois do bloco que cria a assinatura, senão devolve nulo sem aviso.

### A-020 · 01/11/2026 · Aceitar CPF/CNPJ inválido na versão de teste
- **Pedido do cliente:** "Na versão teste deixa colocar CPF e CNPJ não válido, mas dê o aviso que é teste."
- **O que foi feito:**
  - `docAceito()`: na versão de teste aceita 11 ou 14 números mesmo com o dígito errado. A produção continua só com `docOk()`.
  - O aviso aparece em quatro lugares: embaixo do campo enquanto digita, no aviso ao salvar, como aviso na linha da prévia da planilha e com o rótulo "inválido (teste)" na lista e na ficha do cliente.
  - Os testes que esperavam recusa passaram a usar um número com dígitos faltando. A recusa de número inválido agora é testada numa página de produção.
- **Problema achado no caminho:** o aviso sumia no `blur` do campo. Ao tocar em Salvar, a janela encolhia antes do clique, o botão saía do lugar e o toque se perdia. O teste pegou porque o cadastro não acontecia. Agora o aviso muda só enquanto a pessoa digita (regra R62).


### A-019 · 01/11/2026 · Clientes sem CPF/CNPJ no aparelho; falta "Apagar tudo"
- **Relato do cliente** (captura do APK 0.6.0): a lista de Clientes mostrava 14 clientes "Sem CPF/CNPJ", entre eles "Descrição", "E-mail" e "Pago em", com 0 contas. "Não deve ser possível cadastrar alguém sem CPF ou CNPJ. Como a versão é de teste, deve haver um botão de apagar tudo, estilo GitHub: para limpar todo o sistema, o usuário coloca seu CPF ou CNPJ para confirmar."
- **Causa:**
  - Os dados vieram da versão 0.5.0, quando o CPF/CNPJ não existia. A 0.6.0 passou a exigir o número só para cadastros novos e deixou os antigos como estavam, apenas com o rótulo "Sem CPF/CNPJ". Eles ainda recebiam contas e a planilha ainda os achava pelo nome.
  - "Descrição", "E-mail" e "Pago em" são títulos de coluna: na 0.5.0, a aba de instruções de uma planilha foi importada como se fossem clientes. Hoje essa linha seria recusada, porque não tem CPF/CNPJ, mas os clientes já estavam gravados e não havia como excluir um cliente.
  - A pendência "falta um botão Começar do zero" já estava registrada e não tinha sido feita.
- **O que foi feito:**
  - Aviso no topo de Clientes com a contagem e "Completar agora", que abre um cliente depois do outro, com o campo CPF/CNPJ em foco. Cliente sem contas tem "Excluir este cliente".
  - Cliente sem CPF/CNPJ não recebe conta nova: some o botão na ficha, a opção fica desativada em Nova conta e o salvamento recusa.
  - A planilha não acha mais cliente antigo pelo nome: a linha sem CPF/CNPJ é recusada com a explicação, e a linha com CPF/CNPJ completa o cadastro.
  - "Excluir cliente" para clientes sem contas, com Desfazer.
  - "Apagar tudo…" no fim da aba Planilha, só na versão de teste. Na primeira vez pede o CPF/CNPJ do dono, que fica guardado à parte dos dados. Depois mostra o que será apagado, oferece a cópia de segurança e só ativa o botão quando o CPF/CNPJ digitado confere. Não reinicia a avaliação, e o app reabre vazio, sem voltar o exemplo.
- **Como evitar:** regras R59 e R60; tarefas T7 e T8 na suíte `tarefas`. Quando uma regra nova de cadastro entrar, ela vale também para os dados que já estão no aparelho: planeje a migração junto.


### A-018 · 01/11/2026 · Importar, atualizar e exportar ainda confusos; chave do cliente
- **Relato do cliente:** "Não achei como importar clientes, atualizar e exportar. Como importar um cliente com 8 contas? E como atualizar 5 contas de um cliente? Qual a chave única do cliente? Vamos por CPF ou CNPJ."
- **Análise de sistemas de importação e exportação por planilha:**
  - **Sistemas de cobrança brasileiros:** identificam o cliente pelo CPF/CNPJ.
  - **CRMs e lojas virtuais:** separam "criar novos" de "atualizar existentes", deixam a pessoa escolher, e usam um identificador do registro na planilha exportada para casar as linhas na volta.
  - **Padrões comuns:**
    - uma chave única por objeto;
    - exportar para editar e importar de volta pela mesma chave;
    - célula vazia não apaga;
    - prévia do que muda;
    - arquivo das linhas com erro;
    - modelo para baixar.
- **Causas de verdade no nosso app:**
  - **Sem chave para o cliente:** o app reconhecia o cliente pelo nome. Um homônimo ou uma grafia diferente virava outro cliente.
  - **Sem chave para a conta:** a conta era achada por descrição + vencimento. Por isso não dava para mudar o vencimento pela planilha, e "atualizar" não tinha caminho próprio.
  - **Nomes técnicos nos cartões:** "Importar", "Exportar" e "Cópia" diziam o que o app faz, não o que a pessoa quer fazer. "Atualizar" nem existia como opção.
  - **Prévia em lista solta:** mostrava as contas sem agrupar. 8 contas de um cliente pareciam 8 coisas sem relação.
  - **Cópia incompleta:** não guardava clientes sem contas e não restaurava num celular novo.
- **Feito:**
  - **CPF/CNPJ** no cadastro, na lista, na busca, nas planilhas e na prévia (R52).
  - **"Código da conta"** nas planilhas para editar (R53).
  - **Aba Planilha:** virou "O que você quer fazer?", com 4 opções que cabem na tela (R56). "Atualizar pelo Excel" ensina os 3 passos.
  - **Atalho na ficha do cliente:** "Atualizar pelo Excel" baixa só as contas daquele cliente.
  - **Cabeçalho da aba Clientes:** ganhou o botão "Importar".
  - **Exportar:** em dois formatos, Relatório e Para editar (R54).
  - **Prévia:** agrupada por cliente, com o botão dizendo o que grava (R55).
  - **Cópia de segurança:** passa a restaurar num app vazio, com os mesmos códigos.
- **Achados dos testes de tarefa (R57):**
  - "Exportar relatório" ficava abaixo da dobra.
  - O filtro de cliente usado em "Atualizar pelo Excel" ficava preso no relatório seguinte.
  - Os dois foram corrigidos (R56).
- **Achado da revisão visual (R58):** os passos de "Atualizar pelo Excel" quebravam uma palavra por linha.
- **Achado de acessibilidade:** a regra da descrição cinza também pegava o selo "Nunca feita", que ficava com contraste 4,18. A regra foi restringida.
- **Como evitar:** regras R52 a R58.
  - Suíte nova `testes/tarefas.test.js`: as perguntas do cliente como roteiros, com meta de toques.
  - Suíte `importacao` refeita com CPF/CNPJ e código, incluindo a restauração num celular novo.

### A-017 · 31/10/2026 · Planilha confusa e erros de importação sem explicação
- **Relato do cliente:** "Essa área é muito sensível e está confusa. Também é difícil importar a planilha, e se há erro não diz qual linha, coluna ou por quê."
- **Causas de verdade:**
  - **Tela:** a aba Planilha empilhava três coisas diferentes (filtros de exportação, importação e cópia de segurança) numa tela longa. Os filtros apareciam primeiro e empurravam a importação para baixo.
  - **Erros:** a mensagem dizia a linha, mas não a coluna nem a célula, nem como corrigir. Uma linha com dois problemas mostrava só o primeiro.
  - **Títulos:** só os nomes exatos eram aceitos ("Nome", "Valor", "Vencimento"). Com "Cliente" ou "Data de vencimento", toda linha virava "falta o valor", sem dizer que o problema era o título da coluna.
  - **Formato e arquivo:** títulos fora da linha 1, outra aba ou CSV do Excel brasileiro (";" e acentos) falhavam. No Android, o seletor deixava apagadas as planilhas vindas do WhatsApp ou do Drive (tipo "octet-stream").
  - **Valores:** "12abc" virava 12 sem aviso, e "1,234.56" virava 1,23456.
  - **Desfazer:** só existia por 6 segundos, no aviso que aparece embaixo da tela.
- **Feito:**
  - **Aba Planilha:** três cartões, um por tarefa, cada um com um botão (R45). Os filtros de exportação foram para uma janela própria, com "Voltar aos filtros" na prévia. A cópia de segurança mostra o estado: "Nunca feita", "Feita hoje" ou "Feita há N dias".
  - **Importar em 3 passos com etapas visíveis (R46):**
    - **Passo 1:** explica cada coluna, com exemplo e o que é obrigatório. Tem "Baixar planilha modelo" e "Testar com a planilha de exemplo", e sugere uma cópia de segurança antes.
    - **Passo 2:** só aparece quando falta alguma coluna. A pessoa escolhe a coluna vendo a letra, o título e um exemplo.
    - **Passo 3:** prévia com os problemas no topo, as colunas usadas e "Ajustar colunas".
  - **Erros (R44):** cada erro diz a linha, a coluna, a célula e como corrigir, por exemplo: "Linha 6 · Bruno Costa — Valor (E6): “abc” não é um valor. Use só números, por exemplo 150,00." "Ver problemas" leva até a lista. "Baixar linhas com problema" gera uma planilha para corrigir e importar de novo (R51).
  - **Avisos que não barram a linha:** e-mail estranho e telefone curto aparecem em "Para conferir".
  - **Leitura (R47 a R49):** os títulos são reconhecidos por sinônimos e por palavra-chave, a linha de títulos é procurada nas 15 primeiras linhas e a aba com dados é escolhida sozinha. O CSV é lido como texto, no formato brasileiro. O tipo do arquivo é conferido pelo conteúdo. Arquivo grande demais, vazio, com senha, imagem ou PDF ganha um título e uma instrução próprios.
  - **Tela de conclusão:** mostra o que foi gravado e o que ficou de fora, com "Ver contas" e "Desfazer importação".
  - **Desfazer pelo histórico a qualquer momento (R50).**
  - **APK 0.5.0:** o seletor de arquivos aceita os tipos que chegam pelo WhatsApp e pelo Drive.
- **Decisão:** uma linha com problema fica inteira de fora, inclusive o cliente. Antes, "sem vencimento" criava só o cliente. Era confuso: a linha aparecia como erro, mas metade dela entrava.
- **Como evitar:** Regras R44 a R51. A suíte `testes/importacao.test.js` foi reescrita e tem 94 verificações, incluindo acessibilidade (axe) dos três passos nos dois temas e o layout em 320 px.

### A-016 · 31/10/2026 · Limite de uso de 7 dias (só consulta depois)
- **Pedido do cliente:** "Coloca um limite de uso de 7 dias. Depois, o app não exporta nem importa e, do CRUD, fica somente a leitura."
- **Feito:**
  - **Contagem:** começa no primeiro uso, numa chave própria do armazenamento (`celsopix-avaliacao`).
  - **Faixa do topo:** mostra "Avaliação: faltam N dias (até dd/mm/aaaa)", e "termina hoje" no último dia. Depois do prazo, fica vermelha e explica o que ainda dá para fazer.
  - **Telas no modo só consulta:**
    - "Dar baixa", "+ Nova conta", "+ Novo cliente", "Exportar", "Editar dados", "Excluir" e "Desfazer baixa" somem;
    - as fichas mostram "Avaliação encerrada: o app está só para consulta";
    - a aba Planilha mostra o cartão "Só consulta", que mantém o histórico de importações para leitura;
    - continuam funcionando a busca, os filtros, os números e o tema.
  - **Trava central:** no clique de qualquer ação de escrita, e de novo dentro das funções que gravam ou geram arquivo. Mesmo um botão injetado na página é bloqueado.
  - **Relógio atrasado** mais de 1 dia em relação ao último uso: a avaliação encerra e continua encerrada depois de recarregar.
  - **APK de teste:** limite de 7 dias. **Variante de produção:** sem limite (`CELSOPIX_LIMITE_DIAS = 0`), o que se muda numa linha de `android/preparar-assets.sh`.
- **Encontrado na revisão visual:** no modo só consulta continuava aparecendo "Você ainda não fez cópia de segurança. Fazer agora", que levava a uma função bloqueada. Corrigido (R42).
- **Erro de teste (R35):** o teste tocou no botão de tema do painel de apresentação, que fica escondido no celular, em vez do botão do cabeçalho.
- **Limite conhecido (R43):** limpar os dados do app recomeça a contagem. Para um bloqueio de verdade, a versão final precisa de licença validada num servidor.
- **Como evitar:** Regras R41 a R43. Foi criada a suíte `testes/avaliacao.test.js`, com 35 verificações.

### A-015 · 31/10/2026 · Importar com prévia, atualização e histórico
- **Pedido do cliente:** "O importar tinha que ter uma prévia também, principalmente para dados atualizados, um log do que foi mudado."
- **Como era:** Importar gravava na hora, sem mostrar nada antes. Só criava, nunca atualizava. Importar a mesma planilha duas vezes duplicava todas as contas. Os erros apareciam só como "N linhas ignoradas", sem dizer quais nem por quê.
- **Feito:**
  - **Prévia antes de gravar,** com resumo em números: clientes novos, contas novas, atualizações, sem mudança e com problema.
  - **"O que vai mudar":** mostra, campo a campo, o valor antigo riscado e o novo em destaque. Vale para telefone, e-mail, valor e baixa (coluna "Pago em").
  - **Opção "Atualizar estes dados",** para importar só o que é novo.
  - **"Linhas com problema":** cada uma com o número da linha e o motivo (falta nome, valor inválido, data que não existe, pagamento no futuro, conta repetida no arquivo).
  - **Contas já iguais** ficam recolhidas.
  - **Botão claro:** "Importar N mudanças", ou "Nada para importar", desativado.
  - **Depois de confirmar:** "Desfazer" por 6 segundos e um registro em "Últimas importações" (as 10 mais recentes, guardadas no app). Cada registro mostra o que mudou, o que entrou e o que ficou de fora.
  - **Reconhecimento de nomes:** "marina  alves" é reconhecida como "Marina Alves".
  - **Colunas aceitas:** "Cliente" e "Pago em", para que os arquivos exportados possam voltar ao app.
  - **Mesmo arquivo de novo:** agora pode ser escolhido outra vez (antes, escolher o mesmo arquivo não fazia nada).
- **Defeito antigo encontrado pelos testes:** o leitor de datas aceitava "32/13/2026" e gravava uma data impossível. Corrigido: a data passa a ser conferida (R39).
- **Erro de teste (R35):** um teste não achou "O que mudou" porque o título aparece em maiúsculas na tela (R40).
- **Dados de exemplo:** algumas contas do histórico tinham o mesmo cliente, descrição e vencimento que contas atuais, o que deixava a cópia de segurança com contas repetidas. As datas foram ajustadas.
- **Como evitar:** Regras R37 a R40. Foi criada a suíte `testes/importacao.test.js`, com 36 verificações, que inclui a ida e volta da cópia de segurança, sem nenhuma mudança.

### A-014 · 31/10/2026 · Ambiente de teste, chave de reinstalação e exportação com filtros
- **Pedido do cliente:**
  1. Uma mensagem de ambiente de teste.
  2. Por enquanto, a chave pode ser uma que exige reinstalar.
  3. Filtros para a exportação: por situação, por cliente e outros.
- **Feito:**
  - **Faixa de ambiente:** aparece no topo de todas as telas: "Ambiente de teste: os dados podem ser apagados a qualquer momento."
  - **Variantes do Android:** a `homolog` gera o app "CelsoPix Teste" (`br.com.celsopix.teste`, versão 0.2.0-teste), com faixa. A `producao` gera o "CelsoPix", sem faixa. As duas podem ficar instaladas juntas.
  - **Exportação:** a aba Planilha abre com o cartão "Exportar contas". Ele tem filtros de situação (Todas, Em aberto, Atrasadas, Pagas), cliente e período de vencimento.
  - **Contagem ao vivo:** mostra a quantidade e o total enquanto se ajustam os filtros.
  - **Prévia e arquivo:** a prévia diz quais filtros foram usados, e o nome do arquivo também (`contas-atrasadas-marina-alves-AAAA-MM-DD.xlsx`).
  - **Validação do período:** um período invertido mostra o erro embaixo do campo e desativa o botão. "Limpar filtros" só aparece quando algum filtro foi mudado.
  - O antigo cartão "Atrasados" virou o filtro padrão do cartão novo.
- **Problemas no caminho:**
  - **Script de substituição:** a primeira versão marcava o fim do trecho no início de outra função e ia apagar o `render()` e mais 150 linhas. O `assert` parou antes de gravar (R36).
  - **Nome da variante:** o Gradle recusou a variante "teste" (R33).
  - **Falso alarme de foco:** um teste acusou perda de foco que não existia, porque o Playwright não foca o campo ao escolher uma opção (R35).
- **Como evitar:** Regras R30 e R32 a R36. Foi criada a suíte `testes/exportacao.test.js`, com 25 verificações, que recalcula tudo a partir dos dados. A suíte android ganhou o teste da faixa por variante.

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
| **Planilha** | "O que você quer fazer?" com 4 opções pelo objetivo, todas visíveis sem rolar (lei de Hick, reconhecer em vez de lembrar), prévia agrupada por cliente e botão que diz o que grava (status do sistema), chaves CPF/CNPJ e código da conta (prevenção de erros), uma tarefa por cartão (lei de Hick), etapas visíveis (status do sistema), modelo e exemplo (reconhecer em vez de lembrar), prévia antes de gravar (prevenção de erros), erro com linha, coluna, célula e solução (ajudar a corrigir), desfazer a qualquer momento (controle e liberdade), botão principal fixo embaixo (lei de Fitts). |
| **Tema e paleta** | Claro como padrão (R18). Amostras com contraste numérico. |

## Pendências conhecidas

- **Play Store** (decisão do cliente em 01/11/2026): publicar pela conta de desenvolvedor pessoal (CPF) alessandrocdrx, a mesma do geoTotal. Ainda faltam:
  - decidir o limite de avaliação na versão da Play (tirar ou passar para 30 dias);
  - gerar o AAB, porque a Play não aceita APK;
  - fixar a chave de envio, porque a troca de chave entre versões (R30) deixa de valer na Play;
  - confirmar no Play Console se esta conta precisa do teste fechado com 12 testadores por 14 dias antes da produção.

- A miniatura do link no WhatsApp é gerada pelo Claude e não pode ser trocada pelo app.
- Os testes rodam no navegador (Chromium). A casca Android ainda não foi testada num aparelho: faltam o seletor de arquivos, o salvamento em Downloads e o botão Voltar.
- Lint: três bibliotecas AndroidX têm versões mais novas (activity, core, webkit). Atualizar exige compileSdk 36 e uma versão nova do AGP.
- O app guarda os dados no armazenamento da WebView. Se o app for desinstalado, os dados se perdem. A cópia de segurança em Excel é a proteção.
- Os comprovantes (fotos e PDFs) não vão na cópia de segurança em Excel, porque a planilha não guarda arquivos. Restaurar num celular novo traz as baixas, mas não os comprovantes.
- O APK abre com os dados de exemplo. Na versão de teste, "Apagar tudo" deixa o app vazio. A versão de produção ainda precisa decidir se abre vazia.
- O CPF/CNPJ do dono só serve para confirmar "Apagar tudo". Quem tem o celular desbloqueado e sabe o número consegue apagar; a cópia de segurança continua sendo a proteção.
- Os dados de exemplo dos meses anteriores têm valores parecidos, então as barras do gráfico mensal saem quase iguais.
