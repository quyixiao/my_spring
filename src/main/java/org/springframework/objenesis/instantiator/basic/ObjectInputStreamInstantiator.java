//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.basic;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

public class ObjectInputStreamInstantiator<T> implements ObjectInstantiator<T> {
    private ObjectInputStream inputStream;

    public ObjectInputStreamInstantiator(Class<T> clazz) {
        if (Serializable.class.isAssignableFrom(clazz)) {
            try {
                this.inputStream = new ObjectInputStream(new ObjectInputStreamInstantiator.MockStream(clazz));
            } catch (IOException var3) {
                throw new Error("IOException: " + var3.getMessage());
            }
        } else {
            throw new ObjenesisException(new NotSerializableException(clazz + " not serializable"));
        }
    }

    public T newInstance() {
        try {
            return (T)this.inputStream.readObject();
        } catch (ClassNotFoundException var2) {
            throw new Error("ClassNotFoundException: " + var2.getMessage());
        } catch (Exception var3) {
            throw new ObjenesisException(var3);
        }
    }

    private static class MockStream extends InputStream {
        private int pointer = 0;
        private byte[] data;
        private int sequence = 0;
        private static final int[] NEXT = new int[]{1, 2, 2};
        private byte[][] buffers;
        private final byte[] FIRST_DATA;
        private static byte[] HEADER;
        private static byte[] REPEATING_DATA;

        private static void initialize() {
            try {
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                DataOutputStream dout = new DataOutputStream(byteOut);
                dout.writeShort(-21267);
                dout.writeShort(5);
                HEADER = byteOut.toByteArray();
                byteOut = new ByteArrayOutputStream();
                dout = new DataOutputStream(byteOut);
                dout.writeByte(115);
                dout.writeByte(113);
                dout.writeInt(8257536);
                REPEATING_DATA = byteOut.toByteArray();
            } catch (IOException var2) {
                throw new Error("IOException: " + var2.getMessage());
            }
        }

        public MockStream(Class<?> clazz) {
            this.data = HEADER;
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(byteOut);

            try {
                dout.writeByte(115);
                dout.writeByte(114);
                dout.writeUTF(clazz.getName());
                dout.writeLong(ObjectStreamClass.lookup(clazz).getSerialVersionUID());
                dout.writeByte(2);
                dout.writeShort(0);
                dout.writeByte(120);
                dout.writeByte(112);
            } catch (IOException var5) {
                throw new Error("IOException: " + var5.getMessage());
            }

            this.FIRST_DATA = byteOut.toByteArray();
            this.buffers = new byte[][]{HEADER, this.FIRST_DATA, REPEATING_DATA};
        }

        private void advanceBuffer() {
            this.pointer = 0;
            this.sequence = NEXT[this.sequence];
            this.data = this.buffers[this.sequence];
        }

        public int read() throws IOException {
            int result = this.data[this.pointer++];
            if (this.pointer >= this.data.length) {
                this.advanceBuffer();
            }

            return result;
        }

        public int available() throws IOException {
            return 2147483647;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            int left = len;

            for(int remaining = this.data.length - this.pointer; remaining <= left; remaining = this.data.length - this.pointer) {
                System.arraycopy(this.data, this.pointer, b, off, remaining);
                off += remaining;
                left -= remaining;
                this.advanceBuffer();
            }

            if (left > 0) {
                System.arraycopy(this.data, this.pointer, b, off, left);
                this.pointer += left;
            }

            return len;
        }

        static {
            initialize();
        }
    }
}
