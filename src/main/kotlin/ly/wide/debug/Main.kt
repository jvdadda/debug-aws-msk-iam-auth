package ly.wide.debug

import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.eclipse.microprofile.config.inject.ConfigProperty
import software.amazon.awssdk.services.sts.StsClient
import java.time.Duration
import java.util.*


@ApplicationScoped
class Main(
    @ConfigProperty(name = "app.kafka.bootstrapServers") private var bootstrapServers: String,
    @ConfigProperty(name = "app.kafka.authenticated") private var authenticated: Boolean,
) {
    private val topicName = "test"

    @Startup
    fun startup() {
        if (authenticated) {
            val client = StsClient.builder().build()
            client.callerIdentity.let {
                println("Account: ${it.account()}")
                println("Arn: ${it.arn()}")
                println("UserId: ${it.userId()}")
            }
        }

        adminClient().use {
            it.createTopics(listOf(NewTopic(topicName, 1, 1))).all().get()
        }

        producer().send(ProducerRecord(topicName, "key", "value")).get().let {
            println("Sent record(offset=${it.offset()}, partition=${it.partition()}) to topic ${it.topic()}")
        }
        consumer().let {
            it.subscribe(listOf(topicName))
            it.poll(Duration.ofSeconds(5)).forEach { r ->
                println("Received record: ${r.value()} from topic ${r.topic()}")
            }
        }
    }

    fun adminClient(): AdminClient {
        val properties = Properties()
        properties["bootstrap.servers"] = bootstrapServers
        if (authenticated) {
            fillPropertiesWithIamMskAuthentication(properties)
        }
        return AdminClient.create(properties)
    }

    fun producer(): KafkaProducer<String, String> {
        val properties = Properties()
        properties["bootstrap.servers"] = bootstrapServers
        properties["key.serializer"] = StringSerializer::class.java.name
        properties["value.serializer"] = StringSerializer::class.java.name
        if (authenticated) {
            fillPropertiesWithIamMskAuthentication(properties)
        }
        return KafkaProducer(properties)
    }

    fun consumer(): KafkaConsumer<String, String> {
        val properties = Properties()
        properties["bootstrap.servers"] = bootstrapServers
        properties["key.deserializer"] = StringDeserializer::class.java.name
        properties["value.deserializer"] = StringDeserializer::class.java.name
        if (authenticated) {
            fillPropertiesWithIamMskAuthentication(properties)
        }
        return KafkaConsumer(properties)
    }

    private fun fillPropertiesWithIamMskAuthentication(properties: Properties) {
        properties[AdminClientConfig.SECURITY_PROTOCOL_CONFIG] = "SASL_SSL"
        properties[SaslConfigs.SASL_MECHANISM] = "AWS_MSK_IAM"
        properties[SaslConfigs.SASL_JAAS_CONFIG] =
            "software.amazon.msk.auth.iam.IAMLoginModule required awsDebugCreds=true;"
        properties[SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS] =
            "software.amazon.msk.auth.iam.IAMClientCallbackHandler"
    }
}
