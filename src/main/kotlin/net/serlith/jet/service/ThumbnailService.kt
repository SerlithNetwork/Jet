package net.serlith.jet.service

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
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

    @Value($$"${jet.cleanup.days}")
    private var cleanupDays: Long = 0

    @Value($$"${jet.thumbnail.worker-threads}")
    private var thumbnailWorkerThreads = 0

    @Value($$"${jet.s3.bucket}")
    private lateinit var bucket: String

    private final lateinit var executor: ExecutorService

    private final val directory = Path.of("pictures")
    private final val prefix = "jet/thumbnails"
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
    final fun retrieveThumbnail(key: String): FileSystemResource {
        return FileSystemResource(this.directory.resolve("$key.png"))
    }


    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun cleanupThumbnails() {

        if (!Files.exists(this.directory)) {
            return
        }

        val instant = Instant.now().minus(this.cleanupDays, ChronoUnit.DAYS)
        Files.list(this.directory).use { files ->
            for (file in files.toList()) {
                if (Files.getLastModifiedTime(file).toInstant().isAfter(instant)) {
                    continue
                }
                Files.deleteIfExists(file)
            }
        }

    }

}