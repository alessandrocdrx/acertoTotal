# acertoTotal — ficha da Play Store e passo a passo

Conta: desenvolvedor pessoal (CPF) alessandrocdrx, a mesma do geoTotal e do calcTotal.
Pacote: `io.github.alessandrocdrx.acertototal`. Primeira versão da Play: 0.9.0 (código 10), a primeira com assinatura.

## 1. Gerar o pacote (feito)

```
cd android && rm -rf app/build/outputs && ./gradlew clean bundleProducaoRelease --max-workers=1
```

Sai em `android/app/build/outputs/bundle/producaoRelease/app-producao-release.aab`.

- A variante `producao` não tem faixa de teste nem "Apagar tudo", e só aceita CPF/CNPJ válido. Tem **32 dias de teste grátis** (fase de aprovação da Play) e depois exige assinatura. Na versão final, o teste volta para 7 dias: mude `producao:producao:32` para `producao:producao:7` em `android/preparar-assets.sh` e suba `versionCode`.
- Assinado com a **chave de envio** (`android/celsopix-envio.keystore` + `android/assinatura-play.properties`). Os dois ficam fora do repositório (.gitignore). **Guarde uma cópia dos dois num lugar seguro, fora do computador.** Se perder, dá para pedir a troca da chave de envio ao suporte da Play, mas demora.
- Impressão digital SHA-256 da chave de envio: `F3:13:A3:DC:BA:5C:2F:46:3D:82:80:79:CB:96:E8:EF:5A:87:89:EC:B0:89:24:53:28:3D:A4:F0:0E:13:35:54` (o arquivo continua `android/celsopix-envio.keystore`: o nome antigo foi mantido para não mexer na chave)
- Cada envio novo precisa de `versionCode` maior em `android/app/build.gradle.kts`.

## 2. Criar o app no Play Console

1. Criar app → nome **acertoTotal: Contas a Receber**, idioma português (Brasil), tipo **App**, **Gratuito**.
2. Aceitar a declaração de políticas e as leis de exportação dos EUA.
3. Deixar a **Assinatura de apps pelo Google Play** ligada (padrão). A chave que geramos é só a de envio.

## 3. Painel do app (declarações)

| Tela | O que marcar |
|---|---|
| Política de privacidade | URL do `PRIVACIDADE.md` no GitHub (seção 6). |
| Acesso ao app | Todo o app funciona sem login nem senha. |
| Anúncios | Não tem anúncios. |
| Classificação de conteúdo | Categoria "Utilitário/Produtividade". Sem violência, sexo, drogas, jogos de azar nem conteúdo gerado por usuários compartilhado. Resultado esperado: Livre. |
| Público-alvo | 18 anos ou mais. Não é para crianças. |
| Segurança dos dados | Não coleta nem compartilha dados (seção 5). |
| Recursos financeiros | Marcar que o app **não** oferece empréstimo, pagamento nem carteira. Ele só anota cobranças e não movimenta dinheiro. A assinatura do próprio app é cobrada pela Google Play e não é um recurso financeiro. |
| App de notícias, saúde, governo | Não. |
| Categoria | **Finanças** (ou Negócios). Contato: e-mail e, se quiser, site. |

## 4. Ficha da loja (textos prontos)

**Nome (até 30):** `acertoTotal: Contas a Receber`

**Descrição curta (até 80, esta tem 69):**
Saiba quem te deve, cobre pelo WhatsApp e dê baixa com o comprovante.

**Descrição completa (até 4.000):**
Chega de caderninho de fiado e de planilha bagunçada. O acertoTotal mostra, numa tela só, quem te deve, quanto e desde quando. É feito para quem vende e cobra depois: autônomos, pequenas lojas, academias, escolas, entregadores e prestadores de serviço.

QUEM DEVE, NA HORA
• Veja o total em aberto, o que está em atraso e o que entrou no mês.
• Cada conta diz a situação por escrito: "Atrasada há 5 dias", "Vence em 3 dias", "Paga em 02/11".
• Toque em A vencer, Atrasadas ou Pagas no mês para filtrar, e busque pelo nome do cliente ou pela descrição.

COBRE PELO WHATSAPP
• Toque em "Cobrar no WhatsApp" e a mensagem já vem pronta: o que o cliente deve, o vencimento e há quantos dias está atrasado.
• Cadastre a sua chave Pix uma vez e ela vai em toda cobrança.
• Confira e mude o texto antes de mandar. Quem envia é você, no seu WhatsApp.

CLIENTES PELO CPF OU CNPJ
• Cada cliente é identificado pelo CPF ou CNPJ, sem cadastro repetido.
• Abra o cliente e veja todas as contas dele, quanto deve e o contato. Errou um valor? Edite a conta.

DÊ BAIXA COM O COMPROVANTE
• Marque a conta como paga com um toque.
• Anexe a foto ou o PDF do comprovante (Pix, transferência, dinheiro).
• Salve o comprovante no celular quando precisar mandar ou imprimir.
• Errou? Toque em "Desfazer".

EXCEL DE IDA E DE VOLTA
• Importe clientes e contas de uma planilha e confira tudo antes de gravar.
• Atualize várias contas de uma vez: baixe a planilha, mude no Excel e envie de volta, vendo o antes e o depois.
• Exporte relatórios por situação, cliente e período.
• Faça a cópia de segurança em Excel para guardar ou trocar de celular.

SEUS NÚMEROS
• Quanto entrou, quanto falta, taxa de recebimento e prazo médio de pagamento.
• Recebido mês a mês e quem deve mais.

SEUS DADOS SÃO SEUS
• Funciona sem internet.
• Clientes, contas e comprovantes ficam só no seu aparelho. O app não tem anúncios e não envia seus dados para ninguém.
• Tema claro ou escuro.

TESTE GRÁTIS E ASSINATURA
Use todos os recursos de graça no período de teste. Depois, o app continua aberto para consulta; para voltar a cadastrar, dar baixa, importar e exportar, assine o plano semanal, mensal, semestral ou anual. A cobrança é feita pela Google Play, renova sozinha e você cancela quando quiser em Google Play > Pagamentos e assinaturas > Assinaturas.

O acertoTotal não movimenta dinheiro: ele não faz nem recebe pagamentos. Ele organiza o que você tem a receber.

**Imagens** (em `playstore/`): `icone-512.png`, `grafico-destaque-1024x500.png` e as 8 capturas de `capturas/` (1080×1920), nesta ordem: Contas, Cobrar no WhatsApp, Comprovante, Clientes, Números, Planilha, Importar, Nova conta.

## 5. Segurança dos dados (respostas)

- O app coleta ou compartilha dados do usuário? **Não.**
  - O app não envia nada pela internet. Tudo fica no aparelho. A permissão de internet existe só porque a biblioteca de cobrança da Google Play a inclui.
  - A compra da assinatura é feita e guardada pela Google Play. O app só pergunta se há assinatura ativa e não recebe dados de cartão nem de pagamento.
  - O arquivo de cópia de segurança e os relatórios são salvos pela própria pessoa, e quem decide para onde enviar é ela.
- Os dados são criptografados em trânsito? Não se aplica (nada trafega).
- A pessoa pode pedir para apagar os dados? Sim: desinstalar o app apaga tudo.

## 6. Política de privacidade

Texto em [`PRIVACIDADE.md`](../PRIVACIDADE.md). URL para colar no Play Console (depois de renomear o repositório para `acertoTotal`):
`https://github.com/alessandrocdrx/acertoTotal/blob/main/PRIVACIDADE.md`

## 7. Testes e lançamento

1. **Teste interno** (até 100 pessoas, sem revisão longa): Testes → Teste interno → criar lista de e-mails → enviar o AAB → copiar o link e instalar. Serve para conferir o app instalado pela Play antes de tudo.
2. **Teste fechado:** contas pessoais novas precisam de **12 testadores por 14 dias seguidos** antes de pedir o acesso à produção. Confira no Play Console se a sua conta tem essa exigência. Os testadores entram por uma lista de e-mails ou grupo do Google.
3. **Produção:** depois dos 14 dias, "Solicitar acesso à produção", responder o questionário e enviar para revisão (a primeira costuma levar alguns dias).

## 8. Assinatura (Console › Monetizar › Produtos › Assinaturas)

Só dá para criar depois de enviar um pacote com a cobrança (a partir da 0.9.0).

1. **Criar assinatura:** ID do produto `acertototal_premium`, nome "acertoTotal Premium".
2. **4 planos base** (ID exato, minúsculas), todos de renovação automática:

| ID do plano | Período | Preço (R$) |
|---|---|---|
| `semanal` | 1 semana | 2,99 |
| `mensal` | 1 mês | 9,99 |
| `semestral` | 6 meses | 56,90 |
| `anual` | 1 ano | 107,90 |

3. Preço do Brasil em reais; deixe a Play converter para os outros países (ou ative só o Brasil).
4. **Sem oferta de teste na Play:** o teste grátis é do próprio app (32 dias na aprovação, 7 na versão final).
5. Ative cada plano e a assinatura. Em Configurações › Teste de licença, cadastre os e-mails dos testadores: eles compram sem ser cobrados.
6. Os textos "Cancele quando quiser" e o preço de cada plano já aparecem no app, como a Play exige.

## 9. Pendências antes de enviar

- [ ] Nível de API de destino: o app usa `targetSdk 35`. A Play pode exigir um nível mais novo para apps novos. O Play Console avisa no envio do AAB. Se exigir, é preciso atualizar o Android Gradle Plugin e o SDK para o nível pedido (a exigência exata não foi conferida aqui).
- [x] Repositório renomeado para `acertoTotal` e e-mail de contato no `PRIVACIDADE.md`.
- [ ] Criar a assinatura no Console (seção 9) e conferir a compra no teste interno com uma conta de testador de licença.
- [ ] Quando a Play aprovar, publicar a versão final com teste de 7 dias (seção 1).
- [ ] Guardar uma cópia da chave de envio e da senha.
- [ ] Testar o AAB instalado pela Play (teste interno) num aparelho de verdade: seletor de arquivos, salvar em Downloads e botão Voltar ainda não foram testados em aparelho.
