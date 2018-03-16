package tigase.iot.framework.values;

import org.junit.Assert;
import org.junit.Test;

public class LedMatrixTest {

	@Test
	public void getValue() {
		LedMatrix lm1 = new LedMatrix();

		lm1.setPixel(2, 2, true);
		lm1.setPixel(7, 5, true);
		Assert.assertEquals("00000400008000000000000000000000", lm1.getValue());

		LedMatrix lm2 = new LedMatrix(lm1.getValue());
		Assert.assertEquals("00000400008000000000000000000000", lm2.getValue());

		LedMatrix lm3 = new LedMatrix();
		lm3.setPixel(5, 2, true);
		lm3.setPixel(6, 2, true);
		lm3.setPixel(7, 2, true);
		Assert.assertEquals("0000E000000000000000000000000000", lm3.getValue());

		Assert.assertTrue(lm3.getPixel(5, 2));
		Assert.assertTrue(lm3.getPixel(6, 2));
		Assert.assertTrue(lm3.getPixel(7, 2));
		Assert.assertFalse(lm3.getPixel(5, 3));
		Assert.assertFalse(lm3.getPixel(6, 3));
		Assert.assertFalse(lm3.getPixel(7, 3));

		LedMatrix lm4 = new LedMatrix("0000E000000000000000000000000000");
		Assert.assertEquals("0000E000000000000000000000000000", lm4.getValue());

	}
}