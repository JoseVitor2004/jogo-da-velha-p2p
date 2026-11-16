# Jogo da Velha P2P — plugin Java para o Pitiupi

Trabalho acadêmico: plugin com **comunicação individual (unicast) por TCP**. Dois jogadores usam o Pitiupi em computadores diferentes na rede local, trocam um convite, jogam alternadamente e recebem o resultado. Não existe servidor próprio; cada máquina é um peer e utiliza a infraestrutura de rede do Pitiupi.

A implementação foi preparada para a API `pitiupi.models.Plugin` da branch `main` do repositório `https://git.alice.ufsj.edu.br/fls/pitiupi`, consultada em novembro de 2026. Consulte [`PROTOCOLO.md`](PROTOCOLO.md) para o protocolo JVD/1 e [`TESTES.md`](TESTES.md) para a validação.

## Arquivos

```
plugins/jogodavelha/
├── build.xml
├── README.md
├── PROTOCOLO.md
├── TESTES.md
├── registry-entry.json
├── src/
│   ├── META-INF/services/pitiupi.models.Plugin
│   └── pitiupi/jogodavelha/
│       ├── TicTacToePlugin.java           # integração à interface e à rede
│       ├── core/GameSession.java          # regras, estado e validação
│       ├── protocol/GameMessage.java      # formato serializado da mensagem
│       └── ui/GamePanel.java              # painel Swing 3x3
└── test/
    ├── java/pitiupi/jogodavelha/GameSessionTest.java
    └── stubs/                            # API simulada APENAS para testar sem núcleo
```

## Requisitos

- Java JDK 17 ou 21, Apache Ant.
- Cópia do repositório Pitiupi com seu núcleo compilado em `pitiupi/dist/pitiupi.jar`.
- Dois computadores com Pitiupi funcionando, preferencialmente na mesma sub-rede, **mesma porta TCP** (por exemplo, 5000), e firewall liberando essa porta.
- O mesmo JAR do plugin instalado nos dois peers.

## Instalação

1. Clone o projeto do professor (ou baixe o ZIP do repositório):

   ```bash
   git clone https://git.alice.ufsj.edu.br/fls/pitiupi.git
   cd pitiupi
   ```

2. Extraia o diretório **`pitiupi-jogo-da-velha`** deste pacote para **`plugins/jogodavelha`** na raiz do clone (renomeie a pasta). A estrutura final deve conter `plugins/jogodavelha/build.xml`.
3. Compile o núcleo e depois o plugin:

   ```bash
   cd pitiupi
   ant jar
   cd ../plugins/jogodavelha
   ant test
   ant jar
   ```

   Se o `pitiupi.jar` estiver em outro lugar, use `ant jar -Dcore.jar=/caminho/para/pitiupi.jar`.

4. Registre o plugin em `plugins.registry.json` da raiz do clone: insira o objeto de [`registry-entry.json`](registry-entry.json) dentro da lista `"plugins"` existente, mantendo as outras entradas e as vírgulas válidas.
5. Na raiz do clone, faça o link e execute:

   ```bash
   ./scripts/pitiupi.sh link --all
   ./scripts/pitiupi.sh run --all
   ```

   **Alternativa sem editar JSON:** com o Pitiupi aberto, use **Configurações → Instalar JAR…**, selecione `plugins/jogodavelha/dist/pitiupi.jogoDaVelha.jar` e reinicie. A instalação via interface atualiza o catálogo do próprio Pitiupi.

No Windows, use `scripts\pitiupi.cmd run --all` (e o Ant instalado no PATH). O manual da versão atual do Pitiupi está em `docs/README.md` do repositório.

> **Importante:** nunca copie o diretório `test/stubs` para o Pitiupi. As classes falsas existem apenas para executar testes sem o núcleo original. O JAR final contém somente as classes de `src` e o arquivo SPI.

## Como usar

1. Nos dois computadores, abra o Pitiupi, informe nomes distintos e selecione a **mesma porta**, por exemplo **5000**. Verifique em Configurações o IPv4 correto da interface de rede usada.
2. Abra o plugin **Jogo da Velha** pela galeria de plugins.
3. No computador A, preencha o campo `IPv4 do colega` com o endereço exibido pelo computador B e clique em **Convidar**.
4. No computador B, aceite o convite na caixa de diálogo. O autor do convite é `X` e joga primeiro; o convidado é `O`.
5. Clique nas casas do tabuleiro alternadamente. A partida termina quando alguém faz três em linha, quando enche o tabuleiro ou quando alguém clica em **Encerrar partida**.
6. Para uma nova partida, digite o IP do colega e clique novamente em **Convidar**.

### Solução de problemas

- **Plugin não aparece:** veja se o JAR contém `META-INF/services/pitiupi.models.Plugin` com `jar tf dist/pitiupi.jogoDaVelha.jar` e se o catálogo/link apontam para ele. Reinicie o Pitiupi.
- **Convite não chega:** confirme IPv4 do peer, os dois processos na mesma porta, TCP liberado no firewall, mesma rede e mesmo JAR.
- **Falha de conexão:** o Pitiupi usa conexão TCP com prazo de estabelecimento de cerca de 5 segundos; a falha é exibida no status do plugin.
- **Mesmo computador:** a comunicação pública do Pitiupi envia à porta local configurada; testes com duas instâncias na mesma máquina usando portas diferentes não são equivalentes ao cenário de dois computadores. Para esta atividade, priorize dois PCs.
- **Endereço local inválido:** escolha a interface de rede correta em Configurações. A API `LocalNetworkInfo.bestLocalIpv4()` fornece o IPv4 anunciado nas mensagens para resposta.

## Limitações e escopo

- **Um jogo ativo por processo do Pitiupi**, sem histórico persistido ou recuperação automática ao reiniciar.
- **Sem criptografia, autenticação ou proteção contra pares maliciosos.** O protocolo identifica o remetente com um UUID de instância e IP declarado na mensagem, que podem ser falsificados.
- O transporte TCP evita perdas/reordenação de frames enquanto a conexão está funcionando, mas **não implementa recuperação automática de partida quando a conexão cai**.
- Os dois peers precisam instalar **esta mesma versão do plugin**, pois a serialização utiliza classes Java.
- O núcleo Pitiupi real não foi disponibilizado para execução local neste ambiente; os testes automáticos usam uma API simulada e não substituem o teste de ponta a ponta em duas máquinas.
