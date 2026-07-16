# 🛡️ Invest Sentinel AI

O **Invest Sentinel AI** é uma API inteligente desenvolvida em **Spring Boot** que une processamento de linguagem natural (LLM), transcrição de voz e mercado financeiro. O sistema é capaz de interpretar comandos de voz ou texto, identificar a intenção do usuário e executar ações reais (como consultar cotações ou agendar alertas de preço) através de **Function Calling (Tools)** integradas com a **API da Groq (Llama 3.3 / Whisper)** ou **Google Gemini**.

A arquitetura do projeto foi desenhada seguindo as melhores práticas de design de software, utilizando padrões como **Facade, Strategy, DTOs e Services** para garantir baixo acoplamento e alta testabilidade.

---

## 🤖 Interaja Diretamente pelo Telegram!

Para facilitar os testes e a avaliação do projeto, você não precisa rodar a aplicação localmente para ver a IA em ação. Criamos um bot oficial onde você pode realizar consultas de ativos e agendar alertas enviando mensagens de texto ou áudio gravado no próprio celular:

👉 **Acesse o Bot aqui:** [Invest Sentinel no Telegram](https://web.telegram.org/a/#8246885407)

*   **O que enviar para o Bot?**
    *   *Mensagem de texto:* `"Qual o valor atual de VALE3?"` ou `"Crie um alerta se PETR4 chegar a 38.50"`
    *   *Mensagem de voz:* Grave um áudio direto no chat perguntando a cotação de qualquer ativo da B3 ou pedindo para monitorar um preço.

---

## 🚀 Principais Funcionalidades

*   **🎙️ Processamento de Voz Multimodal:** Recebe arquivos de áudio, realiza a transcrição utilizando a inteligência artificial (Groq Whisper) e gera respostas rápidas em texto.
*   **📈 Consulta de Cotações em Tempo Real:** Integrado com APIs financeiras para retornar o preço atual de ativos da B3 (ex: `VALE3`, `PETR4`) sob demanda do usuário.
*   **🔔 Alertas de Preço Automatizados (Function Calling):** A IA identifica quando você deseja monitorar um ativo e dispara um agendamento de alerta real para o canal escolhido (como o próprio **Telegram**).
*   **🛡️ Robustez e Validação:** Filtros integrados que rejeitam ativos vazios, nulos ou preços inconsistentes antes do processamento.

---

## 🛠️ Tecnologias Utilizadas

*   **Java 17 & Spring Boot 3.3.0**
*   **Spring AI** (Integração com ecossistemas Groq e OpenAI)
*   **Groq Cloud (Llama 3.3 & Whisper)** / **Google Gemini API**
*   **Docker & Docker Compose** (Containerização simplificada)
*   **JUnit 5** (Suite de testes automatizados)
*   **Maven** (Gerenciador de dependências)

---

## 🐳 Como Executar com Apenas Um Clique (Opcional)

Se quiser rodar o servidor da API localmente em sua máquina, a forma mais rápida é através do Docker.

### Pré-requisitos:
*   [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado e rodando.

### Passo a Passo:
1.  Na pasta raiz do projeto, renomeie o arquivo `.env.example` para `.env`.
2.  Abra o arquivo `.env` e configure a sua chave da Groq e/ou Gemini:
    ```env
    GROQ_API_KEY=gsk_sua_chave_aqui
    GEMINI_API_KEY=sua_chave_gemini_aqui
    ```
3.  Execute o inicializador automático:
    *   **No Windows:** Dê dois cliques no arquivo `play.bat`.
    *   **No Linux / macOS:** Execute `./play.sh` no terminal.

A API local estará disponível em:  
👉 **`http://localhost:9090`**

---

## 💻 Execução Local via Terminal (Sem Docker)

Caso prefira rodar diretamente no seu ambiente de desenvolvimento:

1.  Entre na pasta da API:
    ```bash
    cd api
    ```
2.  Defina suas chaves de API nas variáveis de ambiente:
    *   **Windows (CMD):**
        ```cmd
        set SPRING_AI_OPENAI_API_KEY=sua_chave_groq_aqui
        set GEMINI_API_KEY=sua_chave_gemini_aqui
        ```
    *   **Linux / macOS:**
        ```bash
        export SPRING_AI_OPENAI_API_KEY="sua_chave_groq_aqui"
        export GEMINI_API_KEY="sua_chave_gemini_aqui"
        ```
3.  Compile e execute o projeto usando o Maven Wrapper:
    ```bash
    ./mvnw.cmd clean spring-boot:run
    ```

---

## 🧪 Como Testar a API Localmente

### 1. Consultar Preço Atual de um Ativo (via IA)
Envie uma pergunta em texto livre para o assistente. A IA vai disparar a Tool interna, buscar a cotação real e te devolver os dados formatados:
```bash
curl -X POST "http://localhost:9090/api/v1/voice/text" \
     -H "Content-Type: text/plain" \
     -d "Qual o valor da Vale atualmente?"
