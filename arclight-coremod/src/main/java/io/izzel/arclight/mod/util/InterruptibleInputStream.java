package io.izzel.arclight.mod.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * https://stackoverflow.com/a/49539574
 *
 * @author piegames
 */
public class InterruptibleInputStream extends InputStream {

    public static Thread thread;

    protected final InputStream in;

    public InterruptibleInputStream(InputStream in) {
        this.in = in;
    }

    /**
     * This will read one byte, blocking if needed. If the thread is interrupted while reading, it will stop and throw
     * an {@link IOException}.
     */
    @Override
    public int read() throws IOException {
        while (!Thread.interrupted())
            if (in.available() > 0)
                return in.read();
            else
                Thread.yield();
        throw new IOException("Thread interrupted while reading");
    }

    /**
     * This will read multiple bytes into a buffer. While reading the first byte it will block and wait in an
     * interruptable way until one is available. For the remaining bytes, it will stop reading when none are available
     * anymore. If the thread is interrupted, it will return -1.
     */
    @Override
    public int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }
        int c = -1;
        while (!Thread.interrupted())
            if (in.available() > 0) {
                c = in.read();
                break;
            } else
                Thread.yield();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte) c;

        int i = 1;
        try {
            for (; i < len; i++) {
                c = -1;
                if (in.available() > 0)
                    c = in.read();
                if (c == -1) {
                    break;
                }
                b[off + i] = (byte) c;
            }
        } catch (IOException ee) {
        }
        return i;
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }
}
