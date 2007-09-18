package edu.cmu.cs.bungee.javaExtensions;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class OdometerInputStream extends FilterInputStream {

	private String name;

	private int bytes = 0;

	private int markBytes;

	public OdometerInputStream(InputStream stream, String _name) {
		super(stream);
		name = _name;
	}

	public void close() throws IOException {
		Util.print(name + ": " + bytes + " bytes.");
		super.close();
	}

	public void mark(int readlimit) {
		markBytes = bytes;
		super.mark(readlimit);
	}

	public int read() throws IOException {
		bytes++;
		return super.read();
	}

	public int read(byte[] b) throws IOException {
		int n = super.read(b);
		if (n > 0)
			bytes += n;
		return n;
	}

	public int read(byte[] b, int off, int len) throws IOException {
		int n = super.read(b, off, len);
		if (n > 0)
			bytes += n;
		return n;
	}
	
	public void reset() throws IOException {
		bytes = markBytes;
		super.reset();
	}
	
	public long skip(long n) throws IOException {
		n = super.skip(n);
		bytes += n;
		return n;
	}

}
