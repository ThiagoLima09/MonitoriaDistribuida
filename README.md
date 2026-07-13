# Monitoria Distribuida

Projeto desenvolvido para a materia de **Sistemas Distribuidos**. A ideia e
simular um sistema de monitoria academica com uma arquitetura hibrida:

- um **Servidor Central** para cadastro, login, sessoes e descoberta de monitores;
- comunicacao **P2P** direta entre aluno e monitor para chat, chamada, audio,
  video, compartilhamento de tela e transferencia de arquivos;
- uma interface desktop em **Java Swing** para aluno e monitor.

O servidor central nao trafega as mensagens do chat entre aluno e monitor. Ele
atua como ponto de coordenacao: autentica os usuarios, registra monitores
disponiveis, informa IP/portas e permite que os clientes se conectem
diretamente.

## Tecnologias

- **Java 21**
- **Java Swing** para interface grafica
- **Java Sockets** (`ServerSocket` e `Socket`) para comunicacao central e P2P
- **Threads** para conexoes simultaneas e escuta em tempo real
- **Maven** para build e dependencias
- **MySQL** para persistencia de usuarios
- **JavaCV/FFmpeg** para captura de webcam
- **JDBC MySQL** para acesso ao banco

O arquivo `monitoria.mwb` contem o modelo do banco para abrir no MySQL
Workbench.

## Configuracao do MySQL

Por padrao, o servidor tenta conectar em:

```text
jdbc:mysql://localhost:3306/monitoria
```


## Como executar

### 1. Compilar o projeto

```bash
mvn clean package
```

### 2. Iniciar o servidor central

Em um terminal:

```bash
mvn exec:java -Dexec.mainClass=br.com.monitoriadistribuida.server.ServerMain
```

O servidor central escuta na porta `5001`.

### 3. Abrir o cliente Swing

Em outro terminal:

```bash
mvn exec:java -Dexec.mainClass=br.com.monitoriadistribuida.client.ClientMain
```

Para testar na mesma maquina, abra dois clientes Swing:

```bash
mvn exec:java -Dexec.mainClass=br.com.monitoriadistribuida.client.ClientMain
```

Use um cliente como monitor e outro como aluno.

## Fluxo de teste sugerido

1. Inicie o servidor central.
2. Abra dois clientes Swing.
3. Cadastre um usuario monitor.
4. Cadastre um usuario aluno.
5. Entre no cliente do monitor.
6. Selecione uma disciplina e clique em **Ficar disponivel**.
7. Entre no cliente do aluno.
8. Selecione a mesma disciplina e clique em **Buscar monitores**.
9. Clique no card do monitor disponivel.
10. O chat P2P sera aberto diretamente entre aluno e monitor.
11. No chat, teste:
    - envio de mensagens;
    - envio de arquivos;
    - chamada;
    - audio;
    - webcam;
    - compartilhamento de tela;
    - encerramento da chamada;
    - encerramento do chat.

## Observacoes sobre P2P

O sistema e hibrido porque o servidor central coordena o estado global, mas os
recursos de conversa acontecem diretamente entre clientes:

- chat P2P;
- chamada de audio;
- video/webcam;
- compartilhamento de tela;
- transferencia de arquivos.

O monitor abre portas dinamicas para receber conexoes P2P. O servidor central
repassa essas portas para o aluno durante a solicitacao de atendimento.

```
