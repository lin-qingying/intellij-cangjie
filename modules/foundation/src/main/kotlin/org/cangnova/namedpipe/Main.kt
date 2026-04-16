package org.cangnova.namedpipe

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*

private val log = KotlinLogging.logger {}

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "server" -> runServer()
        "client" -> runClient()
        else     -> runDemo()
    }
}

private fun runServer() {
    log.info { "Server starting on ${detectPlatform()}" }
    val factory = NamedPipeFactory.create()
    factory.createServer(PipeConfig("demo_pipe")).loop { server ->
        val line = server.readLine() ?: return@loop
        log.info { "Server received: $line" }
        server.writeLine("ECHO:$line")
    }
}

private fun runClient() {
    log.info { "Client connecting on ${detectPlatform()}" }
    NamedPipeFactory.create().createClient(PipeConfig("demo_pipe")).call { client ->
        print("Enter message: ")
        val msg = readLine() ?: "hello"
        client.writeLine(msg)
        log.info { "Reply: ${client.readLine()}" }
    }
}

/** 同进程内演示，使用协程替代原先的手动 Thread + CountDownLatch */
private fun runDemo() = runBlocking {
    log.info { "=== Named Pipe Demo (coroutines, ${detectPlatform()}) ===" }

    val factory = NamedPipeFactory.create()
    val config  = PipeConfig("inproc_demo")

    // 服务端协程
    val serverJob = launch(Dispatchers.IO) {
        factory.createServer(config).serveAsync { server ->
            val msg = server.readLine()
            log.info { "[Server] Received: $msg" }
            server.writeLine("ECHO:$msg")
        }
    }

    delay(150)   // 等待 FIFO 就绪

    // 客户端协程
    factory.createClient(config).callAsync { client ->
        client.writeLine("Hello, Named Pipe!")
        log.info { "[Client] Reply: ${client.readLine()}" }
    }

    serverJob.join()
    log.info { "=== Demo complete ===" }
}
