package com.huawei.cangjie.storage;

import com.huawei.cangjie.utils.ExceptionUtilsKt;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LockBasedStorageManager implements StorageManager {

    @NotNull
    public static LockBasedStorageManager createWithExceptionHandling(
            @NotNull String debugText,
            @NotNull ExceptionHandlingStrategy exceptionHandlingStrategy,
            @Nullable Runnable checkCancelled,
            @Nullable Function1<InterruptedException, Unit> interruptedExceptionHandler
    ) {
        return new LockBasedStorageManager(debugText, exceptionHandlingStrategy,
                SimpleLock.Companion.simpleLock(checkCancelled, interruptedExceptionHandler));
    }

    public interface ExceptionHandlingStrategy {
        ExceptionHandlingStrategy THROW = new ExceptionHandlingStrategy() {
            @NotNull
            @Override
            public RuntimeException handleException(@NotNull Throwable throwable) {
                throw ExceptionUtilsKt.rethrow(throwable);
            }
        };

        /*
         * The signature of this method is a trick: it is used as
         *
         *     throw strategy.handleException(...)
         *
         * most implementations of this method throw exceptions themselves, so it does not matter what they return
         */
        @NotNull
        RuntimeException handleException(@NotNull Throwable throwable);
    }

    protected final SimpleLock lock;
    private final ExceptionHandlingStrategy exceptionHandlingStrategy;
    private final String debugText;

    private LockBasedStorageManager(
            @NotNull String debugText,
            @NotNull ExceptionHandlingStrategy exceptionHandlingStrategy,
            @NotNull SimpleLock lock
    ) {
        this.lock = lock;
        this.exceptionHandlingStrategy = exceptionHandlingStrategy;
        this.debugText = debugText;
    }

    public LockBasedStorageManager(String debugText) {
        this(debugText, (Runnable) null, null);
    }

    public LockBasedStorageManager(
            String debugText,
            @Nullable Runnable checkCancelled,
            @Nullable Function1<InterruptedException, Unit> interruptedExceptionHandler
    ) {
        this(debugText, ExceptionHandlingStrategy.THROW, SimpleLock.Companion.simpleLock(checkCancelled, interruptedExceptionHandler));
    }

    @Override
    public <T> T compute(@NotNull Function0<? extends T> computable) {
        lock.lock();
        try {
            return computable.invoke();
        } catch (Throwable throwable) {
            throw exceptionHandlingStrategy.handleException(throwable);
        } finally {
            lock.unlock();
        }
    }
}