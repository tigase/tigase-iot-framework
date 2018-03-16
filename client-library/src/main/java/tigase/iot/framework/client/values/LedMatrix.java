package tigase.iot.framework.client.values;

import tigase.iot.framework.client.Device;

import java.util.Date;

public class LedMatrix
		extends Device.Value<String> {

	public static final String NAME = "LedMatrix";
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	private final byte[] buffer = new byte[8 * 2];

	private static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return (new String(hexChars)).toUpperCase();
	}

	private static byte[] hexStringToByteArray(String value) {
		String hex = value.toUpperCase();
		int l = hex.length();
		byte[] data = new byte[l / 2];
		for (int i = 0; i < l; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
		}
		return data;
	}

	public LedMatrix(String value, Date timestamp) {
		super(value, timestamp);
		setValue(value);
	}

	public LedMatrix() {
		this(null, new Date());
	}

	public boolean getPixel(int x, int y) {
		assert x >= 0 && x < 16;
		assert y >= 0 && y < 8;
		int $x = x % 8;
		int $y = y + (8 * (x / 8));

		return (buffer[$y] & (1 << $x)) != 0;
	}

	@Override
	public String getValue() {
		return bytesToHex(buffer);
	}

	private void setValue(String encodedBuffer) {
		byte[] tmp = encodedBuffer == null ? null : hexStringToByteArray(encodedBuffer);
		if (tmp != null) {
			for (int i = 0; i < buffer.length; i++) {
				if (tmp != null && i < tmp.length) {
					buffer[i] = tmp[i];
				} else {
					buffer[i] = 0;
				}
			}
		}
	}

	public void setPixel(int x, int y, boolean value) {
		assert x >= 0 && x < 16;
		assert y >= 0 && y < 8;

		int $x = x % 8;
		int $y = y + (8 * (x / 8));

		if (value) {
			buffer[$y] |= (1 << $x);
		} else {
			buffer[$y] &= ~(1 << $x);
		}
	}
}
