package com.prezi.gradle.pride.internal

/**
 * Based on Ant's LineOrientedOutputStream.
 */
public class LoggerOutputStream extends OutputStream {

	private static final int CR = 0x0d
	private static final int LF = 0x0a

	private final def buffer = new ByteArrayOutputStream(256)
	private boolean skip = false

	private final Closure logLine

	public LoggerOutputStream(Closure logLine) {
		this.logLine = logLine
	}

	protected void processLine() {
		try {
			logLine(new String(buffer.toByteArray()))
		} finally {
			buffer.reset();
		}
	}

	public void close() {
		if (buffer.size() > 0) {
			processLine()
		}
		super.close()
	}

	public final void write(int cc) {
		final byte c = (byte) cc
		if ((c == LF) || (c == CR)) {
			if (!skip) {
				processLine()
			}
		} else {
			buffer.write(cc)
		}
		skip = (c == CR)
	}

	public final void write(byte[] b, int off, int len) throws IOException {
		// find the line breaks and pass other chars through in blocks
		int offset = off
		int blockStartOffset = offset
		int remaining = len
		while (remaining > 0) {
			while (remaining > 0 && b[offset] != LF && b[offset] != CR) {
				offset++
				remaining--
			}
			// either end of buffer or a line separator char
			int blockLength = offset - blockStartOffset
			if (blockLength > 0) {
				buffer.write(b, blockStartOffset, blockLength)
			}
			while (remaining > 0 && (b[offset] == LF || b[offset] == CR)) {
				write(b[offset])
				offset++
				remaining--
			}
			blockStartOffset = offset
		}
	}
}
