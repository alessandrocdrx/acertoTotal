# acertoTotal — ficha da Play Store e passo a passo

Conta: desenvolvedor pessoal (CPF) alessandrocdrx, a mesma do geoTotal e do calcTotal.
Pacote: `io.github.alessandrocdrx.acertototal`. Primeira versão da Play: 0.8.0 (código 9).

## 1. Gerar o pacote (feito)

```
bash android/preparar-assets.sh
cd android && rm -rf app/build/outputs && ./gradlew clean bundleProducaoRelease --max-workers=1
```

Sai em `android/app/build/outputs/bundle/producaoRelease/app-producao-release.aab`.

- A variante `producao` não tem faixa de teste, nem "Apagar tudo", nem limite de dias, e só aceita CPF/CNPJ válido.
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
| Recursos financeiros | Marcar que o app **não** oferece empréstimo, pagamento nem carteira. Ele só anota cobranças e não movimenta dinheiro. |
| App de notícias, saúde, governo | Não. |
| Categoria | **Finanças** (ou Negócios). Contato: e-mail e, se quiser, site. |

## 4. Ficha da loja (textos prontos)

**Nome (até 30):** `acertoTotal: Contas a Receber`

**Descrição curta (até 80):**
Anote o que cada cliente deve, dê baixa nos pagamentos e veja quem está atrasado.

**Descrição completa:**
O acertoTotal é um caderno de contas a receber para quem vende e cobra depois: autônomos, pequenas lojas, academias, escolas e prestadores de serviço.

• Cadastre clientes com CPF ou CNPJ e lance as contas a receber.
• Veja na hora quem está em atraso e há quantos dias.
• Dê baixa com um toque e guarde o comprovante em foto ou PDF (Pix, transferência, dinheiro).
• Acompanhe os números: quanto entrou, quanto falta e o prazo médio.
• Importe uma planilha do Excel com clientes e contas, e confira tudo antes de gravar. Um cliente com 8 contas ocupa 8 linhas com o mesmo CPF/CNPJ.
• Atualize várias contas de uma vez: baixe a planilha, mude no Excel e envie de volta, vendo o antes e o depois.
• Exporte relatórios e faça a cópia de segurança em Excel para trocar de celular.
• Funciona sem internet. Seus dados ficam só no seu aparelho.

O acertoTotal não movimenta dinheiro: ele não faz nem recebe pagamentos. Ele só ajuda você a controlar o que tem a receber.

**Imagens** (em `entregas/play/`): `icone-512.png`, `grafico-destaque-1024x500.png` e as capturas de tela 1 a 5 (1080×1920).

## 5. Segurança dos dados (respostas)

- O app coleta ou compartilha dados do usuário? **Não.**
  - O app não pede permissão de internet. Tudo fica no aparelho.
  - O arquivo de cópia de segurança e os relatórios são salvos pela própria pessoa, e quem decide para onde enviar é ela.
- Os dados são criptografados em trânsito? Não se aplica (nada trafega).
- A pessoa pode pedir para apagar os dados? Sim: desinstalar o app apaga tudo.

## 6. Política de privacidade

Texto em [`PRIVACIDADE.md`](../PRIVACIDADE.md). URL para colar no Play Console (depois de renomear o repositório para `acertoTotal`):
`https://github.com/alessandrocdrx/acertoTotal/blob/HEAD/PRIVACIDADE.md`

## 7. Testes e lançamento

1. **Teste interno** (até 100 pessoas, sem revisão longa): Testes → Teste interno → criar lista de e-mails → enviar o AAB → copiar o link e instalar. Serve para conferir o app instalado pela Play antes de tudo.
2. **Teste fechado:** contas pessoais novas precisam de **12 testadores por 14 dias seguidos** antes de pedir o acesso à produção. Confira no Play Console se a sua conta tem essa exigência. Os testadores entram por uma lista de e-mails ou grupo do Google.
3. **Produção:** depois dos 14 dias, "Solicitar acesso à produção", responder o questionário e enviar para revisão (a primeira costuma levar alguns dias).

## 8. Pendências antes de enviar

- [ ] Nível de API de destino: o app usa `targetSdk 35`. A Play pode exigir um nível mais novo para apps novos. O Play Console avisa no envio do AAB. Se exigir, é preciso atualizar o Android Gradle Plugin e o SDK para o nível pedido (a exigência exata não foi conferida aqui).
- [ ] Renomear o repositório `celsao` para `acertoTotal` no GitHub e colocar o e-mail de contato no `PRIVACIDADE.md`.
- [ ] Decidir o limite de avaliação (hoje a Play nasce **sem limite**).
- [ ] Guardar uma cópia da chave de envio e da senha.
- [ ] Testar o AAB instalado pela Play (teste interno) num aparelho de verdade: seletor de arquivos, salvar em Downloads e botão Voltar ainda não foram testados em aparelho.
