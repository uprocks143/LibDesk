package com.example.data.backup

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupEncryption {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val SALT_LENGTH_BYTE = 16
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH_BIT = 256

    fun encrypt(inputStream: InputStream, outputStream: OutputStream, password: CharArray) {
        val salt = ByteArray(SALT_LENGTH_BYTE)
        SecureRandom().nextBytes(salt)

        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BIT, iv))

        outputStream.write(salt)
        outputStream.write(iv)

        val cipherOutputStream = CipherOutputStream(outputStream, cipher)
        inputStream.copyTo(cipherOutputStream)
        cipherOutputStream.close()
    }

    fun decrypt(inputStream: InputStream, outputStream: OutputStream, password: CharArray) {
        val salt = ByteArray(SALT_LENGTH_BYTE)
        if (inputStream.read(salt) != SALT_LENGTH_BYTE) throw IllegalArgumentException("Invalid backup file format")

        val iv = ByteArray(IV_LENGTH_BYTE)
        if (inputStream.read(iv) != IV_LENGTH_BYTE) throw IllegalArgumentException("Invalid backup file format")

        val secretKey = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BIT, iv))

        val cipherInputStream = CipherInputStream(inputStream, cipher)
        cipherInputStream.copyTo(outputStream)
        cipherInputStream.close()
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BIT)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }
}
