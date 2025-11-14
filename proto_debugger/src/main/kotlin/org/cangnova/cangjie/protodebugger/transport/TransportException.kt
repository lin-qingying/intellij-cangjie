package org.cangnova.cangjie.protodebugger.transport

/**
 * 传输层异常基类
 */
open class TransportException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 传输超时异常
 */
class TransportTimeoutException(message: String = "Transport operation timed out") : TransportException(message)

/**
 * 连接失败异常
 */
class TransportConnectionException(message: String, cause: Throwable? = null) : TransportException(message, cause)

/**
 * 消息序列化/反序列化异常
 */
class TransportSerializationException(message: String, cause: Throwable? = null) : TransportException(message, cause)
