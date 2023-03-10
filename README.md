# java-aws-sqs-example

Este projeto é um exemplo de como podemos utilizar o [Spring Cloud AWS](https://docs.awspring.io/spring-cloud-aws/docs/2.4.1/reference/html/index.html) para consumir mensagens de uma fila [SQS](https://aws.amazon.com/pt/sqs/details/). Vou explorar basicamente dois formatos de mensagem: 

1. **String**: onde as informações são enviadas no formato _json_ e você é o responsável por convertê-la para um objeto de negócio; 
2. **Object**: onde as informações já chegam como um objeto, não sendo necessária nenhuma conversão.

Para ser possível a execução local do projeto, sem a necessidade de recursos criados na **AWS**, utilizarei o [localstack](https://github.com/localstack/localstack). Porém, mostrarei também como montar um ambiente híbrido, possibilitando utilizar o _localstack_ durante o desenvolvimento e os recursos da **AWS** durante a execução em produção.

Por último, mostrarei como consumir várias mensagens em simultâneo, aumentando o desempenho do serviço.

As configurações acima citadas, poderão ser observadas nas diferentes versões disponilizadas do projeto, aqui nesse repositório.

## Montagem do ambiente com _localstack_

Para essa configuração utilizarei o **Docker Compose**. Segue abaixo o conteúdo do arquivo:

	# docker-compose.yml
	version: "3"
	services:
	  localstack:
	    container_name: "localstack"
	    image: localstack/localstack
	    ports:
	      - "127.0.0.1:4566:4566"            # LocalStack Gateway
	      - "127.0.0.1:4510-4559:4510-4559"  # external services port range
	    environment:
	      - SERVICES=sqs
	      - DOCKER_HOST=unix:///var/run/docker.sock
	    volumes:
	      - "./.localstack/data:/var/lib/localstack"
	      - "./init:/docker-entrypoint-initaws.d/" # monta um volume com script de inicialização
	      - "/var/run/docker.sock:/var/run/docker.sock"

O _script_ de inicialização é responsável por criar os recursos necessários (**SQS**):

	#!/bin/bash
	awslocal sqs create-queue --queue-name "bills-to-pay" --region "us-east-1"
	
Basicamente ele criará uma fila chamada _bills-to-pay_ na região norte da Virginia (EUA)

O ambiente pode ser iniciado no terminal, através do comando abaixo, no diretório do projeto:

	$ docker-compose -f docker/docker-compose.yml up -d
	
A opção _-f_ só será utilizada se você executar o comando de fora do diretório _docker_.

## Configuração do projeto

### Dependências

Primeiramente é necessário adicionar o repositório das bibliotecas do **Spring Cloud AWS**

	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>io.awspring.cloud</groupId>
				<artifactId>spring-cloud-aws-dependencies</artifactId>
				<version>2.4.2</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>

Depois a dependência das bibliotecas de mensageria:

	<dependency>
		<groupId>io.awspring.cloud</groupId>
		<artifactId>spring-cloud-starter-aws-messaging</artifactId>
	</dependency>

### _Beans_ de configuração

Inicialmente vou criar um _bean_ para me autenticar na **AWS** e que me permita comunicar com as filas **SQS**

	@Configuration
	public class LocalStackConfig {
		
		@Value("${localstack.serviceUrl}")
		private String serviceEndpoint;
		
		@Value("${cloud.aws.region.static}")
		private String region;

		@Primary
		@Bean
		AmazonSQSAsync getAmazonSqsAsync() {
			return AmazonSQSAsyncClientBuilder.standard()
					.withCredentials(new ProfileCredentialsProvider("localstack"))
					.withEndpointConfiguration(new EndpointConfiguration(this.serviceEndpoint, this.region))
					.build();
		}
	}

Nesse momento cabe algumas considerações:

1. Perceba que a autenticação é feita através de um _profile_ "localstack", criado por mim. Dessa forma, precisaremos providenciá-lo.

No seu diretório ```.aws``` crie os arquivos _credentials_ e _config_ com o seguinte conteúdo:

	# credentials
	[localstack]
	aws_access_key_id=test
	aws_secret_access_key=test

	# config
	[localstack]
	region=us-east-1
	output=json

As informações de ```access_key_id``` e ```secret_access_key``` podem ser qualquer uma.

2. _serviceEndpoint_ (```http://localhost:4566```) é uma configuração necessária por conta do _localstack_. Ela é informada ali no ```docker_compose.yml```. 

3. A configuração da _região_ da **AWS** deve ser informada no caminho indicado (```cloud.aws.region.static```) pois as bibliotecas da **AWS** também precisam dessa informação e a procuram nesse caminho.

O próximo _bean_ é o que nos possibilita enviar mensagens para a fila **SQS**:

	@Configuration
	public class SqsConfig {

		@Bean
		QueueMessagingTemplate getQueueMessagingTemplate(AmazonSQSAsync amazonSQSAsync) {
			return new QueueMessagingTemplate(amazonSQSAsync);
		}
	}

Na sua configuração padrão, ```QueueMessagingTemplate``` trata apenas mensagens no formato _string_.

## Produtor e Consumidor com mensagem no formato _string_

Conforme mencionei acima, na versão [1.0.0](https://github.com/tfpolachini/java-aws-sqs-example/releases/tag/v1.0.0) do projeto trabalharei apenas com mensagens no formato _string_.

	@Component
	public class Producer {

		@Value("${queues.bills-to-pay}")
		private String queueName;
		
		@Autowired
		private QueueMessagingTemplate queueMessagingTemplate;
		
		public void produce(String message) {
			queueMessagingTemplate.send(queueName, createMessage(message));
		}
		
		private Message<String> createMessage(String payload) {
			return MessageBuilder.withPayload(payload).build();
		}
	}
	

	@Component
	public class Consumer {

		private static final Logger log = LoggerFactory.getLogger(Consumer.class);

		@SqsListener(value = "${queues.bills-to-pay}")
		public void listen(String message) {
			log.info("A bill of {} was received. I'll pay asap!", message);
		}
	}

### Execução do programa

A ideia aqui é bem simples. Uma _API_ está exposta para fazer o papel do produtor, colocando _contas a pagar_ na fila **SQS**. O consumidor, que está configurado neste mesmo serviço, deverá receber essas _contas_. Um _log_ será mostrado a cada informação recebida. O comando abaixo envia uma _conta a pagar_ para a _API_ produtora.

	curl -d "34.56" -H "Content-Type: application/text" -X POST http://localhost:8080

## Produtor e Consumidor com mensagem no formato _object_

Na versão [2.0.0](https://github.com/tfpolachini/java-aws-sqs-example/releases/tag/v2.0.0) do projeto trabalharei com mensagens no formato _object_. E isso só é possível com as seguintes configurações:

### Criação de uma entidade

A entidade criada é o objeto transportado entre _API_, o produtor e o consumidor **SQS**

    public class Bill {

        private String id;
        private String payee;
        private BigDecimal value;

        // getters, setters e toString
    }

### Beans de configuração do **SQS**

      /**
      * Bean de configuração do consumer. Está sendo inicializado aqui de forma customizada.
      * @param amazonSQSAsync
      * @return
      */
      @Bean
      SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory(AmazonSQSAsync amazonSQSAsync) {
          SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
          factory.setAmazonSqs(amazonSQSAsync);
          factory.setAutoStartup(true);
          factory.setMaxNumberOfMessages(1);
          factory.setResourceIdResolver(getResourceIdResolver(amazonSQSAsync));

          return factory;
      }

      /**
       * Configura um conversor de mensagens, o Jackson
       * @param amazonSQSAsync
       * @return
       */
      @Bean
      QueueMessageHandlerFactory getQueueMessageHandlerFactory(AmazonSQSAsync amazonSQSAsync) {
          var queueMessageHandlerFactory = new QueueMessageHandlerFactory();

          queueMessageHandlerFactory.setAmazonSqs(amazonSQSAsync);
          queueMessageHandlerFactory.setMessageConverters(List.of(new MappingJackson2MessageConverter()));

          return queueMessageHandlerFactory;
      }
        
### Produtor

O produtor passa a enviar um objeto e precisa convertê-lo antes de enviar para a fila. 

    ...
    public void produce(Bill bill) {
		queueMessagingTemplate.convertAndSend(queueName, bill);
	}
    ...

### Consumidor

O consumidor passa a receber um objeto.

    ...
    public void listen(@Payload Bill bill) {
		log.info("A bill of {} was received. I'll pay asap!", bill);
	}
    ...

### Resolvedor de nomes da fila

Uma última configuração interessante feita nesse projeto foi um resolvedor de nomes, não sendo mais necessário informar o nome completo da fila **SQS** no arquivo de propriedades.

    private ResourceIdResolver getResourceIdResolver(AmazonSQSAsync amazonSQSAsync) {
		return new ResourceIdResolver() {
			@Override
			public String resolveToPhysicalResourceId(String logicalResourceId) {
				return amazonSQSAsync.getQueueUrl(logicalResourceId).getQueueUrl();
			}
		};
	}

### Execução do programa

    curl --location --request POST 'localhost:8080/bills/pay' \
        --header 'Content-Type: application/json' \
        --data-raw '{
            "id": "761265f0-518e-4028-8e56-3014127bfa00",
            "payee": "Tulio F. Polachini",
            "value": "13.76"
        }'