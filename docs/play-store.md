# CelsoPix na Google Play: passo a passo

Conta: desenvolvedor pessoal (CPF) alessandrocdrx, a mesma do geoTotal.
Pacote: `br.com.celsopix`. Primeira versão da Play: 0.8.0 (código 9).

## 1. Gerar o pacote (feito)

```
bash android/preparar-assets.sh
cd android && rm -rf app/build/outputs && ./gradlew clean bundleProducaoRelease --max-workers=1
```

Sai em `android/app/build/outputs/bundle/producaoRelease/app-producao-release.aab`.

- A variante `producao` não tem faixa de teste, nem "Apagar tudo", nem limite de dias, e só aceita CPF/CNPJ válido.
- Assinado com a **chave de envio** (`android/celsopix-envio.keystore` + `android/assinatura-play.properties`). Os dois ficam fora do repositório (.gitignore). **Guarde uma cópia dos dois num lugar seguro, fora do computador.** Se perder, dá para pedir a troca da chave de envio ao suporte da Play, mas demora.
- Impressão digital SHA-256 da chave de envio: `F3:13:A3:DC:BA:5C:2F:46:3D:82:80:79:CB:96:E8:EF:5A:87:89:EC:B0:89:24:53:28:3D:A4:F0:0E:13:35:54`
- Cada envio novo precisa de `versionCode` maior em `android/app/build.gradle.kts`.

## 2. Criar o app no Play Console

1. Criar app → nome **CelsoPix**, idioma português (Brasil), tipo **App**, **Gratuito**.
2. Aceitar a declaração de políticas e as leis de exportação dos EUA.
3. Deixar a **Assinatura de apps pelo Google Play** ligada (padrão). A chave que geramos é só a de envio.

## 3. Painel do app (declarações)

| Tela | O que marcar |
|---|---|
| Política de privacidade | Endereço público da página (texto abaixo, seção 6). |
| Acesso ao app | Todo o app funciona sem login nem senha. |
| Anúncios | Não tem anúncios. |
| Classificação de conteúdo | Categoria "Utilitário/Produtividade". Sem violência, sexo, drogas, jogos de azar nem conteúdo gerado por usuários compartilhado. Resultado esperado: Livre. |
| Público-alvo | 18 anos ou mais. Não é para crianças. |
| Segurança dos dados | Não coleta nem compartilha dados (seção 5). |
| Recursos financeiros | Marcar que o app **não** oferece empréstimo, pagamento nem carteira. Ele só anota cobranças e não movimenta dinheiro. |
| App de notícias, saúde, governo | Não. |
| Categoria | **Finanças** (ou Negócios). Contato: e-mail e, se quiser, site. |

## 4. Ficha da loja (textos prontos)

**Nome (até 30):** CelsoPix: cobranças Pix

**Descrição curta (até 80):**
Anote o que cada cliente deve, dê baixa nos Pix e veja quem está atrasado.

**Descrição completa:**
O CelsoPix é um caderno de cobranças para quem recebe por Pix: autônomos, pequenas lojas, academias, escolas e prestadores de serviço.

• Cadastre clientes com CPF ou CNPJ e lance as contas a receber.
• Veja na hora quem está em atraso e há quantos dias.
• Dê baixa com um toque e guarde o comprovante em foto ou PDF.
• Acompanhe os números: quanto entrou, quanto falta e o prazo médio.
• Importe uma planilha do Excel com clientes e contas, e confira tudo antes de gravar. Um cliente com 8 contas ocupa 8 linhas com o mesmo CPF/CNPJ.
• Atualize várias contas de uma vez: baixe a planilha, mude no Excel e envie de volta, vendo o antes e o depois.
• Exporte relatórios e faça a cópia de segurança em Excel para trocar de celular.
• Funciona sem internet. Seus dados ficam só no seu aparelho.

O CelsoPix não movimenta dinheiro: ele não faz nem recebe Pix. Ele só ajuda você a controlar o que tem a receber.

**Imagens** (em `entregas/play/`): `icone-512.png`, `grafico-destaque-1024x500.png` e as capturas de tela 1 a 5 (1080×1920).

## 5. Segurança dos dados (respostas)

- O app coleta ou compartilha dados do usuário? **Não.**
  - O app não pede permissão de internet. Tudo fica no aparelho.
  - O arquivo de cópia de segurança e os relatórios são salvos pela própria pessoa, e quem decide para onde enviar é ela.
- Os dados são criptografados em trânsito? Não se aplica (nada trafega).
- A pessoa pode pedir para apagar os dados? Sim: desinstalar o app apaga tudo.

## 6. Política de privacidade (texto para publicar)

> **Política de privacidade do CelsoPix**
> Atualizada em 01/11/2026.
>
> O CelsoPix é um aplicativo de controle de cobranças que funciona totalmente no seu aparelho.
>
> **Dados que o app guarda:** os clientes, contas, comprovantes e planilhas que você mesmo cadastra ou importa (nomes, CPF/CNPJ, telefone, e-mail, valores e datas). Esses dados ficam apenas no armazenamento do app, no seu aparelho.
>
> **Dados que o app envia:** nenhum. O app não tem permissão de acesso à internet, não usa anúncios nem ferramentas de análise e não compartilha dados com ninguém.
>
> **Arquivos que você escolhe:** o app só lê planilhas e comprovantes que você seleciona, e só grava no seu aparelho os arquivos de relatório e de cópia de segurança que você pede. Para onde enviar esses arquivos é decisão sua.
>
> **Apagar seus dados:** desinstalar o app apaga tudo o que ele guardou.
>
> **Dados de terceiros:** os dados dos seus clientes são de sua responsabilidade. Use o app de acordo com a LGPD.
>
> **Contato:** [e-mail de contato]
>
> Esta página precisa ficar num endereço público (sem login). Pode ser uma página do Google Sites, do GitHub Pages ou um artefato compartilhado com "qualquer pessoa com o link".

## 7. Testes e lançamento

1. **Teste interno** (até 100 pessoas, sem revisão longa): Testes → Teste interno → criar lista de e-mails → enviar o AAB → copiar o link e instalar. Serve para conferir o app instalado pela Play antes de tudo.
2. **Teste fechado:** contas pessoais novas precisam de **12 testadores por 14 dias seguidos** antes de pedir o acesso à produção. Confira no Play Console se a sua conta tem essa exigência. Os testadores entram por uma lista de e-mails ou grupo do Google.
3. **Produção:** depois dos 14 dias, "Solicitar acesso à produção", responder o questionário e enviar para revisão (a primeira costuma levar alguns dias).

## 8. Pendências antes de enviar

- [ ] Nível de API de destino: o app usa `targetSdk 35`. A Play pode exigir um nível mais novo para apps novos. O Play Console avisa no envio do AAB. Se exigir, é preciso atualizar o Android Gradle Plugin e o SDK para o nível pedido (a exigência exata não foi conferida aqui).
- [ ] Endereço público da política de privacidade e e-mail de contato.
- [ ] Decidir o limite de avaliação (hoje a Play nasce **sem limite**).
- [ ] Guardar uma cópia da chave de envio e da senha.
- [ ] Testar o AAB instalado pela Play (teste interno) num aparelho de verdade: seletor de arquivos, salvar em Downloads e botão Voltar ainda não foram testados em aparelho.
