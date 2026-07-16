# Invest Sentinel - API de Monitoramento e Alertas de Ativos 

Olá! Este é o repositório da API do **Invest Sentinel**, um microsserviço que desenvolvi em **Spring Boot** voltado para o ecossistema de mercado financeiro. O objetivo principal do projeto é monitorar oscilações de preços de ativos e disparar notificações automatizadas de forma totalmente desacoplada.

Para construir esta API, foquei fortemente em aplicar boas práticas de design de software, os princípios do SOLID e, principalmente, padrões de projeto clássicos (GoF) para garantir que o sistema seja escalável e resiliente.

---

## 📐 Como a Arquitetura Funciona (Design Patterns)

A grande sacada desse projeto foi remover as regras de notificação e o fluxo de negócios de dentro do Controller, dividindo as responsabilidades em duas camadas principais:

### 1. Padrão Strategy (Notificações Dinâmicas)
Em vez de encher o código com blocos de `if/else` para decidir se o alerta vai para o Telegram ou e-mail, isolei os algoritmos de envio no pacote `strategy`. 
* Criei uma interface comum (`NotificationStrategy`) e cada canal implementa sua própria lógica.
* O `NotificationContextService` injeta dinamicamente todas as estratégias disponíveis em um `Map`.
* **Benefício:** Se amanhã eu quiser adicionar alertas via WhatsApp, SMS ou Discord, eu só preciso criar uma nova classe que implemente a interface. O código antigo continua intacto, respeitando o princípio de Aberto/Fechado (OCP).

### 2. Padrão Facade (Fachada Unificada)
O `MarketAlertFacade` serve como um ponto central de entrada para a execução do pipeline de alertas. Ele esconde a complexidade do sistema do meu Controller.
* Ele recebe os dados brutos, executa validações de negócio (como checar se o preço é maior que zero ou aplicar mensagens analíticas) e orquestra o roteamento correto no serviço de contexto.
* **Benefício:** O Controller fica limpo e focado apenas em receber a requisição HTTP.

---

## 🛠️ Tecnologias Utilizadas

* **Java 17** (com suporte pronto para Java 25)
* **Spring Boot 3.3.0** (Spring Web)
* **Maven** (Gerenciador de dependências e build)

---

## ⚙️ Como Executar o Projeto Localmente

1. Certifique-se de ter o Maven e o Java instalados na sua máquina.
2. Clone o repositório e navegue até a pasta raiz (onde está o `pom.xml`).
3. Execute o comando para rodar a aplicação:

```bash
mvn clean install
mvn spring-boot:run
