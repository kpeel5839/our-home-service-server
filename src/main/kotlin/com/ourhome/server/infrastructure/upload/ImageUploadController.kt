package com.ourhome.server.infrastructure.upload

import com.cloudinary.Cloudinary
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.MultipartConfigFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.ResponseEntity
import org.springframework.util.unit.DataSize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import jakarta.servlet.MultipartConfigElement
import java.util.UUID

@Configuration
class CloudinaryConfig(
    @Value("\${cloudinary.cloud-name:}") private val cloudName: String,
    @Value("\${cloudinary.api-key:}") private val apiKey: String,
    @Value("\${cloudinary.api-secret:}") private val apiSecret: String
) {
    @Bean
    fun cloudinary(): Cloudinary = Cloudinary(
        mapOf("cloud_name" to cloudName, "api_key" to apiKey, "api_secret" to apiSecret, "secure" to true)
    )

    @Bean
    fun multipartConfigElement(): MultipartConfigElement {
        val factory = MultipartConfigFactory()
        factory.setMaxFileSize(DataSize.ofMegabytes(200))
        factory.setMaxRequestSize(DataSize.ofMegabytes(200))
        return factory.createMultipartConfig()
    }
}

@RestController
@RequestMapping("/api/upload")
class ImageUploadController(private val cloudinary: Cloudinary) {

    @PostMapping("/image", consumes = ["multipart/form-data"])
    fun uploadImage(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, String>> {
        if (cloudinary.config.cloudName.isNullOrBlank()) {
            throw IllegalStateException("Cloudinary가 설정되지 않았습니다. 관리자에게 문의하세요.")
        }
        if (file.isEmpty) throw IllegalArgumentException("파일이 비어있습니다")
        val contentType = file.contentType ?: ""
        if (!contentType.startsWith("image/")) throw IllegalArgumentException("이미지 파일만 업로드할 수 있습니다")

        val publicId = "family/${UUID.randomUUID()}"
        val result = cloudinary.uploader().upload(
            file.bytes,
            mapOf("public_id" to publicId, "overwrite" to false, "resource_type" to "image")
        )
        val url = result["secure_url"] as String
        return ResponseEntity.ok(mapOf("url" to url))
    }

    @PostMapping("/video", consumes = ["multipart/form-data"])
    fun uploadVideo(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, String>> {
        if (cloudinary.config.cloudName.isNullOrBlank()) {
            throw IllegalStateException("Cloudinary가 설정되지 않았습니다. 관리자에게 문의하세요.")
        }
        if (file.isEmpty) throw IllegalArgumentException("파일이 비어있습니다")
        val contentType = file.contentType ?: ""
        if (!contentType.startsWith("video/")) throw IllegalArgumentException("동영상 파일만 업로드할 수 있습니다")

        val publicId = "family/${UUID.randomUUID()}"
        val result = cloudinary.uploader().upload(
            file.bytes,
            mapOf("public_id" to publicId, "overwrite" to false, "resource_type" to "video")
        )
        val url = result["secure_url"] as String
        return ResponseEntity.ok(mapOf("url" to url))
    }
}
