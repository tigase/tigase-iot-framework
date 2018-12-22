/*
 * LedMatrixTest.java
 *
 * Tigase IoT Framework
 * Copyright (C) 2011-2018 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

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