package net.serlith.jet.configuration

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI
import java.time.Duration

@Configuration
@ConditionalOnProperty(name = ["jet.s3.enabled"], havingValue = "true", matchIfMissing = false)
class AwsS3Configuration {

    private final val logger = LoggerFactory.getLogger(AwsS3Configuration::class.java)

    @Value($$"${jet.s3.enabled}") // Just to not get warnings on application.yml
    private var enabled: Boolean = false

    @Value($$"${jet.s3.endpoint}")
    private lateinit var endpoint: String

    @Value($$"${jet.s3.bucket}")
    private lateinit var bucket: String

    @Value($$"${jet.s3.key.access}")
    private lateinit var accessKey: String

    @Value($$"${jet.s3.key.secret}")
    private lateinit var secretKey: String

    @Value($$"${jet.s3.max-concurrency}")
    private var maxConcurrency: Int = 5


    @Bean
    fun s3Client(credentials: AwsCredentialsProvider): S3AsyncClient {

        val client = NettyNioAsyncHttpClient.builder()
            .writeTimeout(Duration.ZERO)
            .maxConcurrency(this.maxConcurrency)
            .build()

        val config = S3Configuration.builder()
            .chunkedEncodingEnabled(true)
            .build()

        return S3AsyncClient.builder()
            .httpClient(client)
            .endpointOverride(URI.create(this.endpoint))
            .credentialsProvider(credentials)
            .serviceConfiguration(config)
            .build()
    }

    @Bean
    fun credentialsProvider(): AwsCredentialsProvider {
        return AwsCredentialsProvider {
            return@AwsCredentialsProvider AwsBasicCredentials.create(this.accessKey, this.secretKey)
        }
    }

}