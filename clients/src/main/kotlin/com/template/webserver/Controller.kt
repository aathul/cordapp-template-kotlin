package com.template.webserver

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.slf4j.LoggerFactory
import net.corda.core.identity.CordaX500Name
import com.template.flows.AssetFlow
import com.template.states.AssetState;
import net.corda.client.jackson.JacksonSupport
import net.corda.core.crypto.SecureHash
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.node.services.AttachmentId
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Define your API endpoints here.
 */
/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.

class Controller(rpc: NodeRPCConnection) {



    companion object {

        private val logger = LoggerFactory.getLogger(RestController::class.java)

    }

    private val proxy: CordaRPCOps = rpc.proxy



    @GetMapping(value = "/templateendpoint", produces = arrayOf("text/plain"))

    private fun templateendpoint(): String {

        return "Define an endpoint here."

    }



    @GetMapping(value = "/peers", produces = arrayOf("application/json"))

    private fun otherParties(): String {

        val objectMapper = JacksonSupport.createDefaultMapper(proxy)

        val nodeInfo = proxy.nodeInfo()



        val partiesName = proxy.networkMapSnapshot()

                .map { it.legalIdentities.first() }

                .filter { it.name.toString().contains("Party") && it.name != nodeInfo.legalIdentities.first().name }

        return objectMapper.writeValueAsString(partiesName)

    }





    @RequestMapping(value = "/uploadFile", method = [RequestMethod.POST])

    @ResponseBody

    fun uploadFile(

            @RequestParam("file") file: MultipartFile,

            @RequestParam("txtasset") assetValue: Int,

            @RequestParam("counterparty") counterparty: String

    ): String? {



        try {

            // Handle the received file here

            val filename = file.originalFilename

            println("filename: "+filename)

            val hash: SecureHash = if (!(file.contentType == "application/zip" || file.contentType == "application/jar")) {

                uploadZip(file.inputStream, filename!!)



            } else {

                proxy.uploadAttachmentWithMetadata(

                        jar = file.inputStream,

                        uploader = "Node",

                        filename = filename!!

                )

            }

            println("hash of file "+hash)



            val objectMapper = ObjectMapper().registerModule(KotlinModule())

            val x500Name = CordaX500Name.parse(counterparty)

            val otherParty =  proxy.wellKnownPartyFromX500Name(x500Name)!!



            val flowHandle = proxy.startFlow(::AssetFlow, assetValue, otherParty, hash)



            val transactionID = flowHandle.returnValue.toCompletableFuture().get()



            println("Transaction success. Tn ID:"+transactionID)



            return objectMapper.writeValueAsString("Success" + transactionID)



        } catch (e: Exception) {

            println( ResponseEntity<Any>(HttpStatus.BAD_REQUEST))

        }



        return null



    }



    private fun uploadZip(inputStream: InputStream, filename: String): AttachmentId {

        val zipName = "$filename-${UUID.randomUUID()}.zip"

        FileOutputStream(zipName).use { fileOutputStream ->

            ZipOutputStream(fileOutputStream).use { zipOutputStream ->

                val zipEntry = ZipEntry(filename)

                zipOutputStream.putNextEntry(zipEntry)

                inputStream.copyTo(zipOutputStream, 1024)

            }

        }

        println("uploaded file:"+zipName)

        return FileInputStream(zipName).use { fileInputStream ->

            val hash = proxy.uploadAttachmentWithMetadata(

                    jar = fileInputStream,

                    uploader = "Node",

                    filename = filename

            )

            Files.deleteIfExists(Paths.get(zipName))

            hash

        }

    }



    @GetMapping(value = "/mytransfers", produces = arrayOf("application/json"))

    private fun mytransfers(): String {

        val objectMapper = JacksonSupport.createDefaultMapper(proxy)

        val nodeInfo = proxy.nodeInfo()

        val ious = proxy.vaultQuery(AssetState::class.java)

                .states.map { it.state.data }

        return objectMapper.writeValueAsString(ious)

    }



    @RequestMapping(value = "/download", method = [RequestMethod.GET])

    @ResponseBody

    fun uploadFile(

            @RequestParam("hashv") hash: String

    ): ResponseEntity<Resource>{

        val inputStream = InputStreamResource(proxy.openAttachment(SecureHash.parse(hash)))

        return ResponseEntity.ok().header(

                HttpHeaders.CONTENT_DISPOSITION,

                "attachment; filename=\"$hash.zip\""

        ).body(inputStream)

    }

}