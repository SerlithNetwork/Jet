package net.serlith.jet.service

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.util.unit.DataSize
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO

@Service
class ThumbnailService

@Autowired
constructor(
    private val s3Client: S3AsyncClient?,
) {

    @Value($$"${jet.cleanup.hard.days}")
    private var hardCleanupDays: Long = 0

    @Value($$"${jet.thumbnail.worker-threads}")
    private var thumbnailWorkerThreads = 0

    @Value($$"${jet.s3.bucket}")
    private lateinit var bucket: String

    private final val logger = LoggerFactory.getLogger(ThumbnailService::class.java)


    private final val directory = Path.of("pictures")
    private final val prefix = "jet/thumbnails"
    private final val bufferSize = DataSize.ofKilobytes(4).toBytes().toInt()


    private final lateinit var executor: ExecutorService
    private final lateinit var templateResource: ClassPathResource
    private final lateinit var fontMid: Font
    private final lateinit var fontSmall: Font

    @PostConstruct
    fun init() {
        Files.createDirectories(this.directory)
        this.templateResource = ClassPathResource("thumbnail.png")
        ClassPathResource("JetBrainsMono-Regular.ttf").inputStream.use { inputStream ->
            val font = Font.createFont(Font.TRUETYPE_FONT, inputStream)
            this.fontMid = font.deriveFont(32f)
            this.fontSmall = font.deriveFont(24f)
        }

        val counter = AtomicInteger(0)
        this.executor = Executors.newFixedThreadPool(this.thumbnailWorkerThreads) { task ->
            val thread = Thread(task)
            thread.name = "Jet Thumbnail Worker - #${counter.incrementAndGet()}"
            return@newFixedThreadPool thread
        }
    }

    final fun storeThumbnail(key: String, platform: String, version: String, osFamily: String, osVersion: String, jvmName: String, jvmVersion: String): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            this.storeThumbnail0(key, platform, version, osFamily, osVersion, jvmName, jvmVersion)
        }, this.executor)
    }

    @Suppress("DuplicatedCode")
    private final fun storeThumbnail0(key: String, platform: String, version: String, osFamily: String, osVersion: String, jvmName: String, jvmVersion: String) {
        val template = this.templateResource.inputStream.use { inputStream -> ImageIO.read(inputStream) }
        val g2d = template.createGraphics()

        val platformString = "$platform "
        val osFamilyString = "$osFamily "
        val jvmNameString = "$jvmName "

        var x = 120
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.font = this.fontMid



        // Render platform and version line
        g2d.color = Color.WHITE
        g2d.drawString(platformString, x, 350)
        x += g2d.fontMetrics.stringWidth(platformString)

        g2d.color = Color.GRAY
        g2d.drawString("version ", x, 350)
        x += g2d.fontMetrics.stringWidth("version ")

        g2d.color = Color.WHITE
        g2d.drawString(version, x, 350)



        // Render os and version line
        x = 120
        g2d.color = Color.WHITE
        g2d.drawString(osFamilyString, x, 400)
        x += g2d.fontMetrics.stringWidth(osFamilyString)

        g2d.color = Color.GRAY
        g2d.drawString("version ", x, 400)
        x += g2d.fontMetrics.stringWidth("version ")

        g2d.color = Color.WHITE
        g2d.drawString(osVersion.split("build").firstOrNull() ?: "<unknown>", x, 400)



        // Render platform and version line
        x = 120
        g2d.color = Color.WHITE
        g2d.drawString(jvmNameString, x, 450)
        x += g2d.fontMetrics.stringWidth(jvmNameString)

        g2d.color = Color.GRAY
        g2d.drawString("version ", x, 450)
        x += g2d.fontMetrics.stringWidth("version ")

        g2d.color = Color.WHITE
        g2d.drawString(jvmVersion, x, 450)


        g2d.color = Color.GRAY
        g2d.font = this.fontSmall
        g2d.drawString("/$key", 50, 50)

        g2d.dispose()

        if (this.s3Client != null) {
            this.uploadToS3(key, template)
            return
        }

        ImageIO.write(template, "PNG", this.directory.resolve("$key.png").toFile())
    }

    private final fun uploadToS3(key: String, buffer: BufferedImage) {
        if (this.s3Client == null) {
            throw IllegalStateException("S3 client cannot be null!")
        }

        val output = ByteArrayOutputStream()
        ImageIO.write(buffer, "PNG", output)

        val array = output.toByteArray()
        this.s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(this.bucket)
                .key("${this.prefix}/$key.png")
                .contentLength(array.size.toLong())
                .contentType("image/png")
                .metadata(emptyMap())
                .build(),
            AsyncRequestBody.fromByteBuffer(ByteBuffer.wrap(array))
        )
    }

    // Maybe some cache will be a good idea in the future, but for now this is ok
    final fun retrieveThumbnail(key: String): Flux<DataBuffer> {
        if (this.s3Client != null) {
            return this.downloadFromS3(key)
        }

        val resource = FileSystemResource(this.directory.resolve("$key.png"))
        return DataBufferUtils.read(resource, DefaultDataBufferFactory(), this.bufferSize)
            .onErrorMap(FileNotFoundException::class.java) {
                return@onErrorMap ResponseStatusException(HttpStatus.NOT_FOUND)
            }
    }

    private final fun downloadFromS3(key: String): Flux<DataBuffer> {
        if (this.s3Client == null) {
            throw IllegalStateException("S3 client cannot be null!")
        }

        val future = this.s3Client.getObject(
            GetObjectRequest.builder()
                .bucket(this.bucket)
                .key("${this.prefix}/$key.png")
                .build(),
            AsyncResponseTransformer.toPublisher()
        )

        val bufferFactory = DefaultDataBufferFactory()
        return Mono.fromFuture(future)
            .flatMapMany { response ->
                val objectResponse = response.response()
                val sdkResponse = objectResponse.sdkHttpResponse() ?: return@flatMapMany Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))

                if (!sdkResponse.isSuccessful) {
                    this.logger.info("Failed to download thumbnail for [$key]: ${sdkResponse.statusText().orElse("<empty>")}")
                    return@flatMapMany Mono.error(ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR))
                }

                return@flatMapMany Flux.from(response)
            }.map { byteBuffer ->
                return@map bufferFactory.wrap(byteBuffer)
            }
    }


    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun cleanupThumbnails() {

        if (!Files.exists(this.directory)) {
            return
        }

        val hardCleanup = Instant.now().minus(this.hardCleanupDays, ChronoUnit.DAYS)
        Files.list(this.directory).use { files ->
            for (file in files.toList()) {
                val modified = Files.getLastModifiedTime(file).toInstant()
                if (modified.isBefore(hardCleanup)) {
                    continue
                }
                Files.deleteIfExists(file)
            }
        }

    }

}