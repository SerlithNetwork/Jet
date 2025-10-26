package net.serlith.jet.service

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

@Service
class ThumbnailService {

    @Value($$"${jet.cleanup.days:30}")
    private var cleanupDays: Long = 0

    private final val directory = File("pictures")
    private final lateinit var template: BufferedImage
    private final lateinit var fontMid: Font
    private final lateinit var fontSmall: Font

    @PostConstruct
    fun init() {
        this.directory.mkdirs()
        ClassPathResource("thumbnail.png").inputStream.use { inputStream ->
            this.template = ImageIO.read(inputStream)
        }
        ClassPathResource("JetBrainsMono-Regular.ttf").inputStream.use { inputStream ->
            val font = Font.createFont(Font.TRUETYPE_FONT, inputStream)
            this.fontMid = font.deriveFont(32f)
            this.fontSmall = font.deriveFont(24f)
        }
    }

    @Suppress("DuplicatedCode")
    final fun storeThumbnail(key: String, platform: String, version: String, osFamily: String, osVersion: String, jvmName: String, jvmVersion: String) {
        val g2d = this.template.createGraphics()

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
        g2d.drawString(osVersion, x, 400)



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

        ImageIO.write(this.template, "PNG", File(directory, "$key.png"))
    }

    // Maybe some cache will be a good idea in the future, but for now this is ok
    final fun retrieveThumbnail(key: String): ByteArrayInputStream? {
        val thumbnail = File(directory, "$key.png")
        if (!thumbnail.exists()) {
            return null
        }
        return ByteArrayInputStream(thumbnail.readBytes())
    }


    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun cleanupThumbnails() {

        val date = Date.from(Instant.now().minus(this.cleanupDays, ChronoUnit.DAYS))
        this.directory.listFiles().forEach { file ->
            if (file.lastModified() < date.time) {
                file.delete()
            }
        }

    }

}