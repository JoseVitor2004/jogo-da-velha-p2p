# Testes e verificação

## Teste automático (sem núcleo original)

```bash
cd plugins/jogodavelha
ant test
```

Esse alvo compila todos os arquivos Java do plugin contra **stubs mínimos da API do Pitiupi** e executa `GameSessionTest` com `java -ea`. Os stubs estão em `test/stubs` e **não são incluídos no JAR**.

Casos exercitados:

1. Convite, aceitação e prioridade do jogador X.
2. Replicação de jogadas, vitória horizontal e descarte de mensagem duplicada.
3. Rejeição de jogadas fora de ordem, IP divergente, outro `senderId` e turno incorreto.
4. Recusa de convite e rejeição de convite quando já existe partida ativa.
5. Empate com nove jogadas alternadas.
6. Desistência, serialização/desserialização de mensagem Java e validação IPv4.

## Verificação do pacote SPI

```bash
ant jar                    # exige núcleo compilado em ../../pitiupi/dist/pitiupi.jar
jar tf dist/pitiupi.jogoDaVelha.jar
```

A listagem do JAR deve conter `META-INF/services/pitiupi.models.Plugin` e as classes `TicTacToePlugin`, `GameSession`, `GameMessage` e `GamePanel`. Não deve conter nenhuma classe de `test/stubs`.

## Teste manual de ponta a ponta

1. Compile o núcleo do professor, compile o plugin com `ant jar` e instale **o mesmo JAR** nos dois computadores.
2. Configure a mesma porta TCP (por exemplo `5000`) em ambos; confirme os IPs.
3. Inicie uma partida A→B, aceite no B; jogue `X:0`, `O:3`, `X:1`, `O:4`, `X:2`. Os dois lados devem exibir a vitória de X.
4. Inicie outra partida e rejeite o convite: o autor deve visualizar a recusa.
5. Inicie outra e use **Encerrar partida**: o adversário deve receber o aviso.
6. Repita com o firewall bloqueando a porta TCP: deverá aparecer um aviso de falha de envio; não deve travar a interface.

**Estado da validação:** os testes automatizados locais foram executados com API simulada. Não foi possível baixar o código completo nem iniciar duas instâncias reais do Pitiupi no ambiente de geração deste pacote; o teste de rede em duas máquinas deve ser executado no laboratório.
