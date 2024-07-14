public interface Any {

}
public open class Object <: Any {
    public const init(){}
}

public interface CharExtension {

}

public interface Hasher {
    func finish(): Int64
    mut func reset(): Unit
    mut func write(value: Bool): Unit
    mut func write(value: Char): Unit
    mut func write(value: Int8): Unit
    mut func write(value: Int16): Unit
    mut func write(value: Int32): Unit
    mut func write(value: Int64): Unit
    mut func write(value: UInt8): Unit
    mut func write(value: UInt16): Unit
    mut func write(value: UInt32): Unit
    mut func write(value: UInt64): Unit
    mut func write(value: Float16): Unit
    mut func write(value: Float32): Unit
    mut func write(value: Float64): Unit
    mut func write(value: String): Unit
}

func finish(): Int64 {

}

mut func reset(): Unit {

}

mut func write(value: Bool): Unit {

}

mut func write(value: Char): Unit {

}

mut func write(value: Int8): Unit {

}

mut func write(value: Int16): Unit {

}

mut func write(value: Int32): Unit {

}

mut func write(value: Int64): Unit {

}

mut func write(value: UInt8): Unit {

}

mut func write(value: UInt16): Unit {

}

mut func write(value: UInt32): Unit {

}

mut func write(value: UInt64): Unit {

}

mut func write(value: Float16): Unit {

}

mut func write(value: Float32): Unit {

}

mut func write(value: Float64): Unit {

}

mut func write(value: String): Unit {
}


public open class ArithmeticException <: Exception {
    public init(){}
    public init(message: String){}
    protected open override func getClassName(): String {}
}

public class ArrayIterator<T> <: Iterator<T> {
    public init(data: Array<T>){

    }
    public func next(): Option<T> {

    }

    public func iterator(): Iterator<T> {

    }
}



public class Box<T> {
    public var value: T
    public init(v: T){

    }
}


extend Box<T> <: Hashable where T <: Hashable{
public func hashCode(): Int64 {

}

}
extend Box<T> <: Comparable<Box<T>> where T <: Comparable<T>
{
public operator func >(that: Box<T>): Bool {

}

public operator func <(that: Box<T>): Bool {

}

public operator func >=(that: Box<T>): Bool {

}

public operator func <=(that: Box<T>): Bool {}
public operator func == (that: Box<T>): Bool {}
public operator func != (that: Box<T>): Bool {}

public func compare(that: Box<T>): Ordering {}

}

extend Box<T> <: ToString where T <: ToString
{


public func toString(): String {}
}

public class SpawnException <: Exception {
    public init(){}
    public init(message: String){}
}

public open class Error <: ToString {
    public open prop message: String{
        get() {}
    }

    public open func toString(): String {}
    public open func printStackTrace(): Unit {}
    public func getStackTrace(): Array<StackTraceElement> {}
}

public open class Exception <: ToString {
    public init(){}
    public init(message: String){}

    public open prop message: String{
        get() {}
    }

    public open func toString(): String {}
    public func printStackTrace(): Unit {}
    public func getStackTrace(): Array<StackTraceElement> {}
    protected open func getClassName(): String {}

}

public class IllegalArgumentException <: Exception {
    public init(){}
    public init(message: String){}
}

public class IllegalStateException <: Exception {
    public init(){}
    public init(message: String){}
}


public class IndexOutOfBoundsException <: Exception {
    public init(){}
    public init(message: String){}
}


public class NegativeArraySizeException <: Exception {
    public init(){}
    public init(message: String){}
}

public class NoneValueException <: Exception {
    public init(){}
    public init(message: String){}
}

public class UnsupportedException <: Exception {
    public init(){}
    public init(message: String){}
}


public class InternalError <: Error {}
public class OutOfMemoryError <: Error {}

public class OverflowException <: ArithmeticException {
    public init(){}
    public init(message: String){}
}


public class IllegalMemoryException <: Exception {
    public init(){}
    public init(message: String){}
}

public class Future<T> {

    public prop thread: Thread{
        get() {}
    }

    public func get(): T {}
    public func get(ns: Int64): Option<T> {}
    public func tryGet(): Option<T> {}
    public func tryGet(): Option<T> {}

}
public interface ThreadContext {
    func end(): Unit {}
    func hasEnded(): Bool {}
}
public class Thread {

    public static prop currentThread: Thread{}
    public prop id: Int64{
        get() {}
    }

    public mut prop name: String{

        get() {}
        set(value) {}
    }
    public prop hasPendingCancellation: Bool{
        get() {}
    }

    public static func handleUncaughtExceptionBy(exHandler: (Thread, Exception) -> Unit): Unit {}

}
public class ThreadLocal<T>
{
    public func get(): ?T{}
    public func set(value: ?T): Unit{}

}
public struct Range<T> <: Iterable<T> where T <: Countable<T> & Comparable<T> & Equatable<T> {
public let start: T
public let end: T
public let step: Int64
public let hasStart: Bool
public let hasEnd: Bool
public let isClosed: Bool
public const init(start: T, end: T, step: Int64, hasStart: Bool, hasEnd: Bool, isClosed: Bool){}
public func iterator(): Iterator<T>{}
public const func isEmpty(): Bool{}

}
public class RangeIterator<T> <: Iterator<T> where T <: Countable<T> & Comparable<T> & Equatable<T>
{
public func next(): Option<T>{}
public func iterator(): Iterator<T>{}

}

