package com.example.cardpulse.data.parser

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

class PdfExtractor(private val context: Context) {

    /**
     * Extracts text page by page from the PDF specified by the Uri.
     * If the PDF is password-protected, the password must be provided.
     */
    fun extractText(uri: Uri, password: String? = null): Result<List<String>> {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Could not open input stream for Uri: $uri"))

            inputStream.use { stream ->
                // PDDocument.load can throw IOException or DecryptionException
                val document = if (password != null) {
                    PDDocument.load(stream, password)
                } else {
                    PDDocument.load(stream)
                }

                document.use { doc ->
                    val stripper = PDFTextStripper()
                    val pageTexts = mutableListOf<String>()
                    val totalPages = doc.numberOfPages

                    for (pageNumber in 1..totalPages) {
                        stripper.startPage = pageNumber
                        stripper.endPage = pageNumber
                        val text = stripper.getText(doc)
                        pageTexts.add(text)
                    }

                    Result.success(pageTexts)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if a PDF file is encrypted/password-protected.
     */
    fun isEncrypted(uri: Uri): Result<Boolean> {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Could not open input stream for Uri: $uri"))

            inputStream.use { stream ->
                PDDocument.load(stream).use { doc ->
                    Result.success(doc.isEncrypted)
                }
            }
        } catch (e: Exception) {
            // pdfbox-android often throws an exception (e.g. EncryptedDocumentException or DecryptionException) during load
            // if it is encrypted and we try to load it without a password, or if password checking is needed.
            // Let's assume that if it fails due to cryptography, it is encrypted.
            val message = e.message ?: ""
            if (message.contains("encrypted", ignoreCase = true) || 
                message.contains("password", ignoreCase = true) || 
                e is java.io.IOException) {
                // To be safe, we can try to inspect if the exception is because of encryption
                Result.success(true)
            } else {
                Result.failure(e)
            }
        }
    }
}
